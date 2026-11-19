import { AxeBuilder } from '@axe-core/playwright';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { expect, test, type Page } from '@playwright/test';

// Full-stack demo scenarios. Run with `npm run test:e2e:fullstack` (see playwright.fullstack.config.ts).
const API = 'http://localhost:8080';
const API_KEY = process.env.INGEST_API_KEY ?? 'qa-key';
const CONTROL = path.join(__dirname, 'backend-control.sh');

const backend = (cmd: 'start' | 'stop') => execFileSync(CONTROL, [cmd], { stdio: 'pipe', timeout: 120_000 }).toString().trim();

interface EventDTO {
  id: string;
  title: string;
  source: string;
  sourceRef: string;
  confidence: number;
  priorityScore: number;
  priorityLevel: string;
}

const LEVEL_LABEL: Record<string, string> = { LOW: 'Baixa', MEDIUM: 'Média', HIGH: 'Alta', CRITICAL: 'Crítica' };

let seq = 0;
const nextTitle = () => `QA tempo real ${Date.now()}-${++seq}`;
async function postSensor(title = nextTitle()): Promise<{ ev: EventDTO; t0: number }> {
  const t0 = Date.now();
  const res = await fetch(`${API}/api/v1/events/sensor`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-API-Key': API_KEY },
    body: JSON.stringify({
      type: 'FLOOD',
      title,
      description: 'Evento de teste e2e',
      // Away from the simulator's neighborhoods; repeated posts here corroborate each other.
      latitude: -23.62,
      longitude: -46.55,
      neighborhood: 'QA',
      severity: 5,
      sourceRef: 'qa-sensor',
      occurredAt: new Date().toISOString(),
    }),
  });
  expect(res.status).toBe(201);
  return { ev: (await res.json()) as EventDTO, t0 };
}

/** Resolves with Date.now() (same machine clock as the test) when `text` shows up in the DOM. */
function watchText(page: Page, text: string, timeoutMs = 15_000): Promise<number> {
  return page.evaluate(
    ({ text, timeoutMs }) =>
      new Promise<number>((resolve, reject) => {
        const hit = () => document.body.innerText.includes(text);
        if (hit()) return resolve(Date.now());
        const obs = new MutationObserver(() => {
          if (hit()) {
            obs.disconnect();
            resolve(Date.now());
          }
        });
        obs.observe(document.body, { subtree: true, childList: true, characterData: true });
        setTimeout(() => {
          obs.disconnect();
          reject(new Error(`"${text}" not in DOM after ${timeoutMs}ms`));
        }, timeoutMs);
      }),
    { text, timeoutMs },
  );
}

const tab = (page: Page, name: 'painel' | 'eventos' | 'alertas') => page.getByTestId(`tab-${name}`).click();
const banner = (page: Page) => page.getByTestId('connection-banner');

test.describe.configure({ mode: 'serial' });

let page: Page;
const pageErrors: string[] = [];
const metrics: Record<string, unknown> = {};
const posted: EventDTO[] = [];

test.beforeAll(async ({ browser }) => {
  console.log(`[backend] ${backend('start')}`);
  // Fresh QA DB (local Docker container, never Supabase) so lists stay short and deterministic.
  execFileSync('docker', ['exec', 'urip-qa-db', 'psql', '-U', 'postgres', '-qc', 'TRUNCATE risk_events'], { stdio: 'pipe' });
  page = await (await browser.newContext()).newPage(); // axe needs an explicit context
  page.on('pageerror', (e) => pageErrors.push(e.message));
});

test.afterEach(() => {
  expect(pageErrors, 'uncaught page errors').toEqual([]);
});

test.afterAll(async () => {
  console.log(`[metrics] ${JSON.stringify(metrics)}`);
  await page?.context().close();
  console.log(`[backend] ${backend('stop')}`);
});

test('a. Painel shows summary counts from the backend', async () => {
  // Seed one alert so the summary is non-trivial.
  await postSensor();
  const before = (await (await fetch(`${API}/api/v1/dashboard/summary`)).json()) as { totalActive: number };
  await page.goto('/');
  const total = page.getByTestId('summary-total');
  await expect(total).toBeVisible({ timeout: 15_000 });
  const after = (await (await fetch(`${API}/api/v1/dashboard/summary`)).json()) as {
    totalActive: number;
    byType: Record<string, number>;
  };
  // The simulator may add events between reads; the shown value must be inside that window.
  const shown = Number((await total.getAttribute('aria-label'))?.split(' ')[0]);
  expect(shown).toBeGreaterThanOrEqual(before.totalActive);
  expect(shown).toBeLessThanOrEqual(after.totalActive);
  expect(shown).toBeGreaterThan(0);
  await expect(page.getByLabel(/^Alagamento: \d+$/)).toBeVisible();
  await expect(banner(page)).toHaveCount(0); // connected, no banner
});

test('b. realtime sensor event reaches Eventos, Alertas and the toast without reload', async () => {
  await tab(page, 'eventos');
  await expect(page.getByTestId('events-list')).toBeVisible();
  const latencies: number[] = [];
  for (let i = 0; i < 5; i++) {
    const title = nextTitle();
    const seen = watchText(page, title);
    const { ev, t0 } = await postSensor(title);
    expect(ev.title).toBe(title);
    expect(['HIGH', 'CRITICAL']).toContain(ev.priorityLevel);
    latencies.push((await seen) - t0);
    posted.push(ev);
    await expect(page.getByTestId('alert-toast')).toContainText(`Novo alerta: ${title}`);
  }
  const avg = Math.round(latencies.reduce((a, b) => a + b, 0) / latencies.length);
  metrics.realtimeLatencyMs = { samples: latencies, min: Math.min(...latencies), avg, max: Math.max(...latencies) };
  console.log(`[tempo de atualização] ${JSON.stringify(metrics.realtimeLatencyMs)}`);

  await tab(page, 'alertas');
  for (const ev of posted) await expect(page.getByTestId('alerts-list').getByTestId(`event-card-${ev.id}`)).toBeVisible();
});

test('c. cards show source, confidence and priority; alerts sorted by priority', async () => {
  const list = page.getByTestId('alerts-list');
  for (const ev of posted) {
    const card = list.getByTestId(`event-card-${ev.id}`);
    await expect(card).toContainText(`Sensor · ${ev.sourceRef}`);
    await expect(card).toContainText(`Confiança ${Math.round(ev.confidence * 100)}%`);
    await expect(card).toContainText(LEVEL_LABEL[ev.priorityLevel]!);
  }
  const ids = await list
    .locator('[data-testid^="event-card-"]')
    .evaluateAll((els) => els.map((e) => e.getAttribute('data-testid')!.replace('event-card-', '')));
  expect(ids.length).toBeGreaterThanOrEqual(posted.length);
  const scores = await Promise.all(
    ids.map(async (id) => ((await (await fetch(`${API}/api/v1/events/${id}`)).json()) as EventDTO).priorityScore),
  );
  expect(scores).toEqual([...scores].sort((a, b) => b - a));
});

test('d. backend down: offline banner, cached data kept, no page errors', async () => {
  const t0 = Date.now();
  console.log(`[backend] ${backend('stop')}`);
  const stopReturned = Date.now();
  const b = banner(page);
  await expect(b).toContainText('Sem conexão', { timeout: 60_000 });
  await expect(b).toHaveAttribute('role', 'alert');
  metrics.offlineBannerMs = { fromStopCall: Date.now() - t0, fromProcessGone: Date.now() - stopReturned };
  console.log(`[offline banner] ${JSON.stringify(metrics.offlineBannerMs)}`);

  // Tab screens stay mounted, so scope card lookups to the visible tab's list.
  for (const ev of posted) await expect(page.getByTestId('alerts-list').getByTestId(`event-card-${ev.id}`)).toBeVisible();
  await tab(page, 'eventos');
  await expect(page.getByTestId('events-list').getByTestId(`event-card-${posted[0]!.id}`)).toBeVisible();
  await tab(page, 'painel');
  await expect(page.getByTestId('summary-total')).toBeVisible();
  await expect(page.getByTestId('dashboard-list').getByTestId(`event-card-${posted[0]!.id}`)).toBeVisible();
  await expect(b).toContainText('Sem conexão');
});

test('e. backend back: "Reconectado", refetch, realtime resumes', async () => {
  const t0 = Date.now();
  console.log(`[backend] ${backend('start')}`);
  const healthy = Date.now();
  const refetch = page.waitForResponse((r) => r.url().includes('/api/v1/dashboard/summary') && r.ok(), { timeout: 90_000 });
  await expect(banner(page)).toContainText('Reconectado', { timeout: 90_000 });
  metrics.reconnectedMs = { fromStartCall: Date.now() - t0, fromHealthy: Date.now() - healthy };
  console.log(`[reconectado] ${JSON.stringify(metrics.reconnectedMs)}`);
  await refetch;
  await expect(banner(page)).toHaveCount(0, { timeout: 10_000 });

  await tab(page, 'eventos');
  const title = nextTitle();
  const seen = watchText(page, title);
  const { ev } = await postSensor(title);
  await seen;
  await expect(page.getByTestId('events-list').getByTestId(`event-card-${ev.id}`)).toBeVisible();
  await expect(page.getByTestId('alert-toast')).toContainText(title);
});

test('f. axe: no serious/critical violations on the three tabs', async () => {
  const results: Record<string, string[]> = {};
  for (const name of ['painel', 'eventos', 'alertas'] as const) {
    await tab(page, name);
    await page.waitForTimeout(500);
    const { violations } = await new AxeBuilder({ page }).analyze();
    results[name] = violations.map((v) => `${v.impact}:${v.id}(${v.nodes.length})`);
    const bad = violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
    expect(bad.map((v) => `${v.id}: ${v.help} -> ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`), name).toEqual([]);
  }
  metrics.axe = results;
  console.log(`[axe] ${JSON.stringify(results)}`);
});
