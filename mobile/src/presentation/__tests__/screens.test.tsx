import { fireEvent, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import { colors } from '../theme';
import { makeEvent } from '../../test-utils/fixtures';
import { renderWithApp } from '../../test-utils/renderWithApp';
import { AlertsScreen } from '../screens/AlertsScreen';
import { DashboardScreen } from '../screens/DashboardScreen';
import { EventsScreen } from '../screens/EventsScreen';

// react-native-maps needs native modules; stub it.
jest.mock('../components/EventsMap', () => ({
  EventsMap: () => {
    const { Text } = jest.requireActual<typeof import('react-native')>('react-native');
    return <Text testID="events-map">mapa</Text>;
  },
}));

const flood = makeEvent({ id: 'f', type: 'FLOOD', priorityLevel: 'HIGH', title: 'Alagamento A' });
const heat = makeEvent({ id: 'h', type: 'HEAT_ISLAND', priorityLevel: 'LOW', title: 'Calor B' });

describe('EventsScreen', () => {
  it('lists events and filters by type', async () => {
    await renderWithApp(<EventsScreen />, { events: [flood, heat] });
    expect(screen.getByTestId('event-card-f')).toBeTruthy();
    expect(screen.getByTestId('event-card-h')).toBeTruthy();
    await fireEvent.press(screen.getByTestId('filter-type-HEAT_ISLAND'));
    expect(screen.queryByTestId('event-card-f')).toBeNull();
    expect(screen.getByTestId('event-card-h')).toBeTruthy();
  });

  it('shows empty state when no events match', async () => {
    await renderWithApp(<EventsScreen />, { events: [] });
    expect(screen.getByText('Nenhum evento encontrado.')).toBeTruthy();
  });

  it('toggles map view', async () => {
    await renderWithApp(<EventsScreen />, { events: [flood] });
    const toggle = screen.getByTestId('toggle-map');
    expect(toggle.props.accessibilityLabel).toBe('Mostrar mapa');
    await fireEvent.press(toggle);
    expect(screen.getByTestId('events-map')).toBeTruthy();
    expect(screen.getByTestId('toggle-map').props.accessibilityLabel).toBe('Mostrar lista');
  });
});

describe('DashboardScreen', () => {
  it('shows summary counts and top alerts', async () => {
    await renderWithApp(<DashboardScreen />, {
      summary: { totalActive: 7, byType: { FLOOD: 4, HEAT_ISLAND: 3 }, byLevel: { CRITICAL: 2 }, lastUpdatedAt: null },
      alerts: [flood],
    });
    expect(screen.getByTestId('summary-total').props.accessibilityLabel).toBe('7 eventos ativos');
    expect(screen.getByLabelText('Alagamento: 4')).toBeTruthy();
    expect(screen.getByLabelText('Crítica: 2')).toBeTruthy();
    expect(screen.getByTestId('event-card-f')).toBeTruthy();
  });

  it('renders the total label in the on-primary color (AA contrast on primary bg)', async () => {
    await renderWithApp(<DashboardScreen />, {
      summary: { totalActive: 1, byType: {}, byLevel: {}, lastUpdatedAt: null },
    });
    expect(StyleSheet.flatten(screen.getByText('eventos ativos').props.style).color).toBe(colors.onPrimary);
  });

  it('shows placeholder when no summary is available', async () => {
    await renderWithApp(<DashboardScreen />);
    expect(screen.getByText('Resumo indisponível.')).toBeTruthy();
  });
});

// axe scrollable-region-focusable: scrollable lists must be reachable by keyboard on web.
it.each([
  ['events-list', <EventsScreen key="e" />],
  ['dashboard-list', <DashboardScreen key="d" />],
  ['alerts-list', <AlertsScreen key="a" />],
])('%s is keyboard focusable', async (id, ui) => {
  await renderWithApp(ui);
  expect(screen.getByTestId(id).props.focusable).toBe(true);
});

describe('AlertsScreen', () => {
  it('lists alerts', async () => {
    await renderWithApp(<AlertsScreen />, { alerts: [flood] });
    expect(screen.getByTestId('event-card-f')).toBeTruthy();
  });

  it('empty state', async () => {
    await renderWithApp(<AlertsScreen />);
    expect(screen.getByText('Nenhum alerta ativo.')).toBeTruthy();
  });
});
