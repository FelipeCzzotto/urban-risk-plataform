import { createContext, useCallback, useEffect, useMemo, useReducer, useRef, useState, type ReactNode } from 'react';
import { ApiError, type ApiClient } from '../data/apiClient';
import { fetchWithCache, type CacheRepository } from '../data/cacheRepository';
import type { RealtimeHandlers } from '../data/realtimeClient';
import { connectionReducer, initialConnection, type ConnectionState } from '../domain/connection';
import { applyAlertUpdate, applyEventUpdate } from '../domain/events';
import type { DashboardSummary, RiskEvent } from '../domain/types';

export interface AppDeps {
  api: Pick<ApiClient, 'getEvents' | 'getAlerts' | 'getSummary'>;
  cache: CacheRepository;
  createRealtime: (h: RealtimeHandlers) => { start(): void; stop(): Promise<void> };
  notify: (alert: RiskEvent) => void;
}

interface Data {
  events: RiskEvent[];
  alerts: RiskEvent[];
  summary: DashboardSummary | null;
  fromCache: boolean;
  loading: boolean;
}

export interface AppContextValue extends Data {
  connection: ConnectionState;
  refresh: () => Promise<void>;
  toast: string | null;
  dismissToast: () => void;
}

export const AppContext = createContext<AppContextValue | null>(null);

const RECONNECTED_VISIBLE_MS = 3000;
const TOAST_VISIBLE_MS = 5000;
const isOfflineError = (e: unknown) => e instanceof ApiError && (e.kind === 'network' || e.kind === 'timeout');

export function AppDataProvider({ deps, children }: { deps: AppDeps; children: ReactNode }) {
  const [connection, dispatch] = useReducer(connectionReducer, initialConnection);
  const [data, setData] = useState<Data>({ events: [], alerts: [], summary: null, fromCache: false, loading: true });
  const [toast, setToast] = useState<string | null>(null);
  const depsRef = useRef(deps);

  const refresh = useCallback(async () => {
    const { api, cache } = depsRef.current;
    const [events, alerts, summary] = await Promise.all([
      fetchWithCache(cache, 'events', api.getEvents),
      fetchWithCache(cache, 'alerts', api.getAlerts),
      fetchWithCache(cache, 'summary', api.getSummary),
    ]);
    if ([events, alerts, summary].some((r) => isOfflineError(r.error))) dispatch({ type: 'NETWORK_ERROR' });
    setData({
      events: events.data ?? [],
      alerts: alerts.data ?? [],
      summary: summary.data,
      fromCache: events.fromCache || alerts.fromCache || summary.fromCache,
      loading: false,
    });
  }, []);

  useEffect(() => {
    refresh();
    const rt = depsRef.current.createRealtime({
      onStatus: (up) => dispatch({ type: up ? 'CONNECTED' : 'DISCONNECTED' }),
      onEvent: (ev) =>
        setData((d) => ({ ...d, events: applyEventUpdate(d.events, ev), alerts: applyAlertUpdate(d.alerts, ev) })),
      onAlert: (ev) => {
        setData((d) => ({ ...d, alerts: applyAlertUpdate(d.alerts, ev) }));
        depsRef.current.notify(ev);
        setToast(`Novo alerta: ${ev.title}`);
      },
    });
    rt.start();
    return () => {
      rt.stop();
    };
  }, [refresh]);

  // Contract: on reconnect refetch REST (subscriptions are renewed by the realtime client).
  useEffect(() => {
    if (!connection.reconnected) return;
    refresh();
    const t = setTimeout(() => dispatch({ type: 'ACK_RECONNECTED' }), RECONNECTED_VISIBLE_MS);
    return () => clearTimeout(t);
  }, [connection.reconnected, refresh]);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), TOAST_VISIBLE_MS);
    return () => clearTimeout(t);
  }, [toast]);

  const dismissToast = useCallback(() => setToast(null), []);
  const value = useMemo(
    () => ({ ...data, connection, refresh, toast, dismissToast }),
    [data, connection, refresh, toast, dismissToast],
  );
  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}
