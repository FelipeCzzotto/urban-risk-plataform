import { ApiError, createApiClient } from '../apiClient';
import { makeEvent } from '../../test-utils/fixtures';

const json = (body: unknown, status = 200) =>
  Promise.resolve(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }));

describe('apiClient', () => {
  it('GET /events with ACTIVE filter and returns content', async () => {
    const ev = makeEvent();
    const fetchImpl = jest.fn(() => json({ content: [ev], page: 0, size: 100, totalElements: 1, totalPages: 1 }));
    const api = createApiClient({ baseUrl: 'http://h:8080', fetchImpl });
    await expect(api.getEvents()).resolves.toEqual([ev]);
    const [url, init] = fetchImpl.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('http://h:8080/api/v1/events?status=ACTIVE&page=0&size=100');
    expect(init.signal).toBeDefined();
  });

  it('GET /alerts and /dashboard/summary', async () => {
    const summary = { totalActive: 3, byType: { FLOOD: 3 }, byLevel: { HIGH: 3 }, lastUpdatedAt: null };
    const fetchImpl = jest.fn((url: string) => json(url.endsWith('/alerts') ? [makeEvent()] : summary));
    const api = createApiClient({ baseUrl: 'http://h', fetchImpl });
    await expect(api.getAlerts()).resolves.toHaveLength(1);
    await expect(api.getSummary()).resolves.toEqual(summary);
    expect(fetchImpl.mock.calls.map((c) => c[0])).toEqual([
      'http://h/api/v1/alerts',
      'http://h/api/v1/dashboard/summary',
    ]);
  });

  it('maps non-2xx to http ApiError with ProblemDetail title', async () => {
    const fetchImpl = jest.fn(() => json({ title: 'Bad Request', status: 400 }, 400));
    const api = createApiClient({ baseUrl: 'http://h', fetchImpl });
    const err = await api.getAlerts().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err).toMatchObject({ kind: 'http', status: 400, message: 'Bad Request' });
  });

  it('maps fetch rejection to network ApiError', async () => {
    const fetchImpl = jest.fn(() => Promise.reject(new TypeError('Network request failed')));
    const api = createApiClient({ baseUrl: 'http://h', fetchImpl });
    await expect(api.getAlerts()).rejects.toMatchObject({ kind: 'network' });
  });

  it('maps invalid JSON to parse ApiError', async () => {
    const fetchImpl = jest.fn(() => Promise.resolve(new Response('<html>', { status: 200 })));
    const api = createApiClient({ baseUrl: 'http://h', fetchImpl });
    await expect(api.getAlerts()).rejects.toMatchObject({ kind: 'parse' });
  });

  it('aborts after timeout and maps to timeout ApiError', async () => {
    jest.useFakeTimers();
    try {
      let signal: AbortSignal | undefined;
      const fetchImpl = jest.fn(
        (_url: string, init?: RequestInit) =>
          new Promise<Response>((_res, rej) => {
            signal = init?.signal ?? undefined;
            signal?.addEventListener('abort', () => rej(new DOMException('Aborted', 'AbortError')));
          }),
      );
      const api = createApiClient({ baseUrl: 'http://h', fetchImpl, timeoutMs: 1000 });
      const p = api.getAlerts();
      jest.advanceTimersByTime(1000);
      await expect(p).rejects.toMatchObject({ kind: 'timeout' });
      expect(signal?.aborted).toBe(true);
    } finally {
      jest.useRealTimers();
    }
  });
});
