import AsyncStorage from '@react-native-async-storage/async-storage';
import { act, renderHook, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';
import { ApiError } from '../../data/apiClient';
import { createCacheRepository } from '../../data/cacheRepository';
import type { RealtimeHandlers } from '../../data/realtimeClient';
import type { RiskEvent } from '../../domain/types';
import { makeEvent } from '../../test-utils/fixtures';
import { AppDataProvider, type AppDeps } from '../AppDataProvider';
import { useAlerts, useConnection, useEvents, useSummary, useToast } from '../hooks';

const summary = { totalActive: 1, byType: { FLOOD: 1 }, byLevel: { CRITICAL: 1 }, lastUpdatedAt: null };

function setup(over: Partial<{ [K in keyof AppDeps['api']]: jest.Mock }> = {}) {
  let handlers!: RealtimeHandlers;
  const realtime = { start: jest.fn(), stop: jest.fn(() => Promise.resolve()) };
  const api = {
    getEvents: jest.fn<Promise<RiskEvent[]>, []>(() => Promise.resolve([makeEvent({ id: 'live' })])),
    getAlerts: jest.fn(() => Promise.resolve([makeEvent({ id: 'live' })])),
    getSummary: jest.fn(() => Promise.resolve(summary)),
    ...over,
  };
  const notify = jest.fn();
  const deps: AppDeps = {
    api,
    cache: createCacheRepository(AsyncStorage),
    createRealtime: (h) => {
      handlers = h;
      return realtime;
    },
    notify,
  };
  const wrapper = ({ children }: { children: ReactNode }) => <AppDataProvider deps={deps}>{children}</AppDataProvider>;
  const hook = () => ({
    conn: useConnection(),
    events: useEvents(),
    alerts: useAlerts(),
    summary: useSummary(),
    toast: useToast(),
  });
  return { api, realtime, notify, wrapper, hook, handlers: () => handlers };
}

describe('AppDataProvider', () => {
  beforeEach(() => AsyncStorage.clear());

  it('loads REST data and starts realtime', async () => {
    const s = setup();
    const { result } = await renderHook(s.hook, { wrapper: s.wrapper });
    await waitFor(() => expect(result.current.events.data.map((e) => e.id)).toEqual(['live']));
    expect(result.current.summary.data).toEqual(summary);
    expect(result.current.events.fromCache).toBe(false);
    expect(s.realtime.start).toHaveBeenCalledTimes(1);
  });

  it('on network failure goes offline and shows cached data without crashing', async () => {
    await AsyncStorage.setItem('cache:events', JSON.stringify([makeEvent({ id: 'cached' })]));
    const fail = () => Promise.reject(new ApiError('network', 'Falha de rede'));
    const s = setup({ getEvents: jest.fn(fail), getAlerts: jest.fn(fail), getSummary: jest.fn(fail) });
    const { result } = await renderHook(s.hook, { wrapper: s.wrapper });
    await waitFor(() => expect(result.current.conn.status).toBe('offline'));
    await waitFor(() => expect(result.current.events.data.map((e) => e.id)).toEqual(['cached']));
    expect(result.current.events.fromCache).toBe(true);
    expect(result.current.alerts.data).toEqual([]);
    expect(result.current.summary.data).toBeNull();
  });

  it('on reconnect refetches REST and flags reconnected', async () => {
    const s = setup();
    const { result } = await renderHook(s.hook, { wrapper: s.wrapper });
    await waitFor(() => expect(s.api.getEvents).toHaveBeenCalledTimes(1));

    await act(() => s.handlers().onStatus(true));
    expect(result.current.conn.status).toBe('connected');
    expect(result.current.conn.reconnected).toBe(false);

    await act(() => s.handlers().onStatus(false));
    expect(result.current.conn.status).toBe('reconnecting');

    s.api.getEvents.mockResolvedValueOnce([makeEvent({ id: 'fresh' })]);
    await act(() => s.handlers().onStatus(true));
    expect(result.current.conn.reconnected).toBe(true);
    await waitFor(() => expect(result.current.events.data.map((e) => e.id)).toEqual(['fresh']));
    expect(s.api.getEvents).toHaveBeenCalledTimes(2);
    expect(s.api.getAlerts).toHaveBeenCalledTimes(2);
    expect(s.api.getSummary).toHaveBeenCalledTimes(2);
  });

  it('clears "reconnected" flag after a few seconds', async () => {
    jest.useFakeTimers();
    try {
      const s = setup();
      const { result } = await renderHook(s.hook, { wrapper: s.wrapper });
      await act(() => s.handlers().onStatus(true));
      await act(() => s.handlers().onStatus(false));
      await act(() => s.handlers().onStatus(true));
      expect(result.current.conn.reconnected).toBe(true);
      await act(() => jest.advanceTimersByTime(4000));
      expect(result.current.conn.reconnected).toBe(false);
    } finally {
      jest.useRealTimers();
    }
  });

  it('applies realtime events and notifies on alerts', async () => {
    const s = setup();
    const { result } = await renderHook(s.hook, { wrapper: s.wrapper });
    await waitFor(() => expect(result.current.events.data).toHaveLength(1));

    const alert = makeEvent({ id: 'a1', priorityLevel: 'CRITICAL', priorityScore: 99 });
    await act(() => s.handlers().onAlert(alert));
    expect(result.current.alerts.data.map((e) => e.id)).toEqual(['a1', 'live']);
    expect(s.notify).toHaveBeenCalledWith(alert);
    expect(result.current.toast.message).toEqual(expect.stringContaining(alert.title));

    await act(() => s.handlers().onEvent(makeEvent({ id: 'live', status: 'RESOLVED' })));
    expect(result.current.events.data).toEqual([]);
    expect(result.current.alerts.data.map((e) => e.id)).toEqual(['a1']);
  });

  it('hooks throw outside provider', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    await expect(renderHook(() => useConnection())).rejects.toThrow(/AppDataProvider/);
  });
});
