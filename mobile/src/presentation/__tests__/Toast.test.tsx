import { fireEvent, render, screen } from '@testing-library/react-native';
import { Toast } from '../components/Toast';

describe('Toast', () => {
  it('renders message as polite live region and dismisses', async () => {
    const onDismiss = jest.fn();
    await render(<Toast message="Novo alerta: X" onDismiss={onDismiss} />);
    const toast = screen.getByTestId('alert-toast');
    expect(toast.props.accessibilityLiveRegion).toBe('polite');
    expect(screen.getByText('Novo alerta: X')).toBeTruthy();
    await fireEvent.press(screen.getByLabelText('Fechar aviso'));
    expect(onDismiss).toHaveBeenCalled();
  });

  it('renders nothing without message', async () => {
    await render(<Toast message={null} onDismiss={jest.fn()} />);
    expect(screen.queryByTestId('alert-toast')).toBeNull();
  });
});
