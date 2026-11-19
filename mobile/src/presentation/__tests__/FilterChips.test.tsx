import { fireEvent, render, screen } from '@testing-library/react-native';
import { FilterChips } from '../components/FilterChips';

describe('FilterChips', () => {
  it('renders type and level filters with selected state and toggles', async () => {
    const onChange = jest.fn();
    await render(<FilterChips value={{ type: 'FLOOD' }} onChange={onChange} />);
    expect(screen.getByTestId('filter-type-FLOOD').props.accessibilityState).toMatchObject({ selected: true });
    expect(screen.getByTestId('filter-type-all').props.accessibilityState).toMatchObject({ selected: false });

    await fireEvent.press(screen.getByTestId('filter-level-HIGH'));
    expect(onChange).toHaveBeenLastCalledWith({ type: 'FLOOD', level: 'HIGH' });

    await fireEvent.press(screen.getByTestId('filter-type-all'));
    expect(onChange).toHaveBeenLastCalledWith({ type: undefined });
    expect(screen.getByLabelText('Filtrar tipo: Alagamento')).toBeTruthy();
  });
});
