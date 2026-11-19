import { render } from '@testing-library/react-native';
import type { ReactElement } from 'react';
import { initialConnection } from '../domain/connection';
import { AppContext, type AppContextValue } from '../presentation/AppDataProvider';

export function renderWithApp(ui: ReactElement, over: Partial<AppContextValue> = {}) {
  const value: AppContextValue = {
    events: [],
    alerts: [],
    summary: null,
    fromCache: false,
    loading: false,
    connection: { ...initialConnection, status: 'connected', everConnected: true },
    refresh: jest.fn(() => Promise.resolve()),
    toast: null,
    dismissToast: jest.fn(),
    ...over,
  };
  return render(<AppContext.Provider value={value}>{ui}</AppContext.Provider>);
}
