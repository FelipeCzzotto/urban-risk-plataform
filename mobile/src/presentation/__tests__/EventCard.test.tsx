import { render, screen } from '@testing-library/react-native';
import { EventCard } from '../components/EventCard';
import { makeEvent } from '../../test-utils/fixtures';

describe('EventCard', () => {
  it('renders source, confidence percent and priority level', async () => {
    const ev = makeEvent({ id: 'e1', source: 'COMMUNITY', sourceRef: 'community', confidence: 0.6, priorityLevel: 'HIGH' });
    await render(<EventCard event={ev} />);
    const card = screen.getByTestId('event-card-e1');
    expect(screen.getByText(/Comunidade/)).toBeTruthy();
    expect(screen.getByText('Confiança 60%')).toBeTruthy();
    expect(screen.getByText('Alta')).toBeTruthy();
    expect(screen.getByText(ev.title)).toBeTruthy();
    expect(card.props.accessibilityLabel).toEqual(expect.stringContaining('prioridade Alta'));
    expect(card.props.accessibilityLabel).toEqual(expect.stringContaining('fonte Comunidade'));
    expect(card.props.accessibilityLabel).toEqual(expect.stringContaining('confiança 60%'));
  });

  it('shows sensor ref for sensor events', async () => {
    await render(<EventCard event={makeEvent({ id: 'e2', source: 'SENSOR', sourceRef: 'sensor-42', confidence: 0.9 })} />);
    expect(screen.getByText('Sensor · sensor-42')).toBeTruthy();
    expect(screen.getByText('Confiança 90%')).toBeTruthy();
    expect(screen.getByText('Crítica')).toBeTruthy();
  });
});
