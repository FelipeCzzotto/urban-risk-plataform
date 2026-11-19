import type { DashboardSummary, PageResponse, RiskEvent } from '../domain/types';

export type ApiErrorKind = 'network' | 'timeout' | 'http' | 'parse';

export class ApiError extends Error {
  constructor(
    readonly kind: ApiErrorKind,
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

type FetchLike = (url: string, init?: RequestInit) => Promise<Response>;

export interface ApiClientOptions {
  baseUrl: string;
  fetchImpl?: FetchLike;
  timeoutMs?: number;
}

export type ApiClient = ReturnType<typeof createApiClient>;

export function createApiClient({ baseUrl, fetchImpl = fetch, timeoutMs = 8000 }: ApiClientOptions) {
  async function request(path: string, ms = timeoutMs): Promise<Response> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), ms);
    try {
      return await fetchImpl(`${baseUrl}${path}`, {
        headers: { Accept: 'application/json' },
        signal: controller.signal,
      });
    } catch {
      throw controller.signal.aborted
        ? new ApiError('timeout', `Tempo esgotado após ${ms}ms`)
        : new ApiError('network', 'Falha de rede');
    } finally {
      clearTimeout(timer);
    }
  }

  async function getJson<T>(path: string): Promise<T> {
    const res = await request(path);
    let body: unknown;
    try {
      body = await res.json();
    } catch {
      if (!res.ok) throw new ApiError('http', `HTTP ${res.status}`, res.status);
      throw new ApiError('parse', 'Resposta inválida do servidor', res.status);
    }
    if (!res.ok) {
      // RFC 7807 ProblemDetail
      const title = (body as { title?: unknown } | null)?.title;
      throw new ApiError('http', typeof title === 'string' ? title : `HTTP ${res.status}`, res.status);
    }
    return body as T;
  }

  return {
    // ponytail: first 100 ACTIVE only, filters applied client-side so they also work on cached data. Add paging when >100 active is realistic.
    getEvents: async () =>
      (await getJson<PageResponse<RiskEvent>>('/api/v1/events?status=ACTIVE&page=0&size=100')).content,
    getAlerts: () => getJson<RiskEvent[]>('/api/v1/alerts'),
    getSummary: () => getJson<DashboardSummary>('/api/v1/dashboard/summary'),
    /** Contract v1.1: any non-2xx or 3s timeout means down. Never throws. */
    isHealthy: async () => {
      try {
        return (await request('/actuator/health', 3000)).ok;
      } catch {
        return false;
      }
    },
  };
}
