import { useContext } from 'react';
import { AppContext, type AppContextValue } from './AppDataProvider';

function useApp(): AppContextValue {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('Hooks must be used inside <AppDataProvider>');
  return ctx;
}

export const useConnection = () => useApp().connection;

export function useEvents() {
  const { events, fromCache, loading, refresh } = useApp();
  return { data: events, fromCache, loading, refresh };
}

export function useAlerts() {
  const { alerts, fromCache, loading, refresh } = useApp();
  return { data: alerts, fromCache, loading, refresh };
}

export function useSummary() {
  const { summary, fromCache, loading, refresh } = useApp();
  return { data: summary, fromCache, loading, refresh };
}

export function useToast() {
  const { toast, dismissToast } = useApp();
  return { message: toast, dismiss: dismissToast };
}
