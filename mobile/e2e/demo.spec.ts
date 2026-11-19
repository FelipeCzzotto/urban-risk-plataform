import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { expect, test, type Page } from '@playwright/test';

// Narrated walkthrough of every front-end feature, recorded to video. Run: npm run demo:video
const API = 'http://localhost:8080';
// USE_ENV_DB=1 runs against the real DB from the repo-root .env; otherwise the local QA container.
const REAL_DB = process.env.USE_ENV_DB === '1';
const envKey = () => /^INGEST_API_KEY=(.*)$/m.exec(readFileSync(path.join(__dirname, '../../.env'), 'utf8'))?.[1]?.trim();
const KEY = process.env.INGEST_API_KEY ?? (REAL_DB ? envKey() : undefined) ?? 'qa-key';
const backend = (cmd: 'start' | 'stop') =>
  execFileSync(path.join(__dirname, 'backend-control.sh'), [cmd], { stdio: 'pipe', timeout: 120_000 });

async function post(kind: 'sensor' | 'public' | 'community', body: Record<string, unknown>) {
  const res = await fetch(`${API}/api/v1/events/${kind}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(kind === 'community' ? {} : { 'X-API-Key': KEY }) },
    body: JSON.stringify({ occurredAt: new Date().toISOString(), description: 'Demonstração', ...body }),
  });
  expect(res.status).toBe(201);
  return (await res.json()) as { id: string };
}

async function caption(page: Page, text: string, ms = 2500) {
  await page.evaluate((t) => {
    let el = document.getElementById('demo-caption');
    if (!el) {
      el = document.createElement('div');
      el.id = 'demo-caption';
      el.setAttribute('aria-hidden', 'true');
      Object.assign(el.style, {
        position: 'fixed', left: '8px', right: '8px', bottom: '72px', zIndex: '99999', padding: '10px 14px',
        background: 'rgba(17,24,39,.92)', color: '#fff', font: '600 16px system-ui', borderRadius: '10px',
        textAlign: 'center', pointerEvents: 'none',
      });
      document.body.appendChild(el);
    }
    el.textContent = t;
  }, text);
  await page.waitForTimeout(ms);
}

const tab = (page: Page, n: 'painel' | 'eventos' | 'alertas') => page.getByTestId(`tab-${n}`).click();

test('demo: todas as funcionalidades do app', async ({ page }) => {
  backend('start');
  // Never wipe the real DB: only the throwaway local container is truncated.
  if (!REAL_DB) execFileSync('docker', ['exec', 'urip-qa-db', 'psql', '-U', 'postgres', '-qc', 'TRUNCATE risk_events'], { stdio: 'pipe' });
  try {
    await post('sensor', { type: 'FLOOD', title: 'Alagamento na Av. do Estado', latitude: -23.55, longitude: -46.62, neighborhood: 'Cambuci', severity: 4, sourceRef: 'pluviometro-07' });
    await post('public', { type: 'HEAT_ISLAND', title: 'Ilha de calor no centro', latitude: -23.54, longitude: -46.63, neighborhood: 'Sé', severity: 3, sourceRef: 'inmet-dataset' });
    await post('community', { type: 'SERVICE_OUTAGE', title: 'Falta de energia na rua', latitude: -23.58, longitude: -46.68, neighborhood: 'Pinheiros', severity: 2 });

    await page.goto('/');
    await expect(page.getByTestId('summary-total')).toBeVisible({ timeout: 15_000 });
    await caption(page, 'Painel: total de riscos ativos, por tipo e por prioridade (dados do backend)', 4000);
    await caption(page, 'Principais alertas listados no próprio painel');

    await tab(page, 'eventos');
    await caption(page, 'Eventos: cada card mostra origem, confiança e nível de prioridade', 3500);
    await page.getByTestId('filter-type-FLOOD').click();
    await caption(page, 'Filtro por tipo: Alagamento');
    await page.getByTestId('filter-type-all').click();
    await page.getByTestId('filter-level-HIGH').click();
    await caption(page, 'Filtro por prioridade: Alta');
    await page.getByTestId('filter-level-all').click();
    await page.getByTestId('toggle-map').click();
    await caption(page, 'Alternar para mapa (na web, cai para a lista)');
    await page.getByTestId('toggle-map').click();

    await caption(page, 'Tempo real: um sensor envia um evento crítico agora...', 2000);
    const critical = await post('sensor', { type: 'FLOOD', title: 'Rio transbordando na Marginal', latitude: -23.52, longitude: -46.65, neighborhood: 'Barra Funda', severity: 5, sourceRef: 'pluviometro-12' });
    await expect(page.getByTestId('alert-toast')).toBeVisible({ timeout: 10_000 });
    await caption(page, 'Chegou via WebSocket sem recarregar + notificação de alerta', 3500);

    await caption(page, 'Relato da comunidade (endpoint público, contato guardado só como hash)', 1500);
    await post('community', { type: 'FLOOD', title: 'Rua alagada perto da escola', latitude: -23.521, longitude: -46.651, neighborhood: 'Barra Funda', severity: 4, contact: 'morador@example.com' });
    await page.waitForTimeout(2500);

    await tab(page, 'alertas');
    await caption(page, 'Alertas: só prioridade alta/crítica, ordenados por prioridade', 3500);

    await fetch(`${API}/api/v1/events/${critical.id}/resolve`, { method: 'PATCH', headers: { 'X-API-Key': KEY } });
    await expect(page.getByTestId('alerts-list').getByTestId(`event-card-${critical.id}`)).toHaveCount(0, { timeout: 10_000 });
    await caption(page, 'Evento resolvido no backend: some da lista em tempo real', 3000);

    await caption(page, 'Resiliência: derrubando o backend...', 1500);
    backend('stop');
    await expect(page.getByTestId('connection-banner')).toContainText('Sem conexão', { timeout: 60_000 });
    await caption(page, 'Banner "Sem conexão"; o app segue mostrando os dados em cache', 3000);
    await tab(page, 'painel');
    await caption(page, 'Painel continua navegável offline', 2500);

    await caption(page, 'Religando o backend...', 1500);
    backend('start');
    await expect(page.getByTestId('connection-banner')).toContainText('Reconectado', { timeout: 90_000 });
    await caption(page, 'Reconectado: dados atualizados e tempo real retomado', 3000);
    await tab(page, 'eventos');
    await post('sensor', { type: 'HEAT_ISLAND', title: 'Calor extremo no Brás', latitude: -23.545, longitude: -46.61, neighborhood: 'Brás', severity: 5, sourceRef: 'termometro-03' });
    await expect(page.getByTestId('alert-toast')).toBeVisible({ timeout: 10_000 });
    await caption(page, 'Novo evento em tempo real após a reconexão. Fim da demonstração.', 4000);
  } finally {
    backend('stop');
  }
});
