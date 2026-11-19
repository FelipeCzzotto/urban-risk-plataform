import { render, screen } from '@testing-library/react-native';
import { ConnectionBanner } from '../components/ConnectionBanner';
import { initialConnection, type ConnectionState } from '../../domain/connection';

const state = (s: Partial<ConnectionState>): ConnectionState => ({ ...initialConnection, ...s });

describe('ConnectionBanner', () => {
  it('shows offline alert with live region', async () => {
    await render(<ConnectionBanner state={state({ status: 'offline' })} />);
    const banner = screen.getByTestId('connection-banner');
    expect(banner.props.accessibilityRole).toBe('alert');
    expect(banner.props.accessibilityLiveRegion).toBe('assertive');
    expect(screen.getByText(/Sem conexão/)).toBeTruthy();
    expect(screen.getByText(/dados salvos/)).toBeTruthy();
  });

  it('shows reconnecting only after a previous connection', async () => {
    await render(<ConnectionBanner state={state({ status: 'reconnecting', everConnected: true })} />);
    expect(screen.getByText('Reconectando…')).toBeTruthy();
  });

  it('hides while first connecting and while connected', async () => {
    const { rerender } = await render(<ConnectionBanner state={state({ status: 'reconnecting' })} />);
    expect(screen.queryByTestId('connection-banner')).toBeNull();
    await rerender(<ConnectionBanner state={state({ status: 'connected', everConnected: true })} />);
    expect(screen.queryByTestId('connection-banner')).toBeNull();
  });

  it('shows "Reconectado" right after recovering', async () => {
    await render(<ConnectionBanner state={state({ status: 'connected', everConnected: true, reconnected: true })} />);
    expect(screen.getByTestId('connection-banner')).toBeTruthy();
    expect(screen.getByText('Reconectado')).toBeTruthy();
  });
});
