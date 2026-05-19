import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { UseDefaultsButton } from '../UseDefaultsButton';

describe('UseDefaultsButton (GAP-287 escape ramp)', () => {
  it('renders Vietnamese label by default', () => {
    render(<UseDefaultsButton send={() => {}} />);
    expect(screen.getByRole('button')).toHaveTextContent(/Sử dụng mặc định/i);
  });

  it('dispatches USE_DEFAULTS event on click', async () => {
    const send = vi.fn();
    const user = userEvent.setup();
    render(<UseDefaultsButton send={send} />);

    await user.click(screen.getByTestId('use-defaults-button'));

    expect(send).toHaveBeenCalledTimes(1);
    expect(send).toHaveBeenCalledWith({ type: 'USE_DEFAULTS' });
  });

  it('respects disabled prop — no dispatch on click', async () => {
    const send = vi.fn();
    const user = userEvent.setup();
    render(<UseDefaultsButton send={send} disabled />);

    await user.click(screen.getByTestId('use-defaults-button'));

    expect(send).not.toHaveBeenCalled();
  });

  it('accepts custom Vietnamese label override', () => {
    render(<UseDefaultsButton send={() => {}} label="Bỏ qua, dùng mặc định" />);
    expect(screen.getByRole('button')).toHaveTextContent(/Bỏ qua, dùng mặc định/);
  });
});
