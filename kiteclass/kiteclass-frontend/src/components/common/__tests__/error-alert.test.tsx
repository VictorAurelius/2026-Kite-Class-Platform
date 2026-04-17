/**
 * ErrorAlert Component Tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import { ErrorAlert } from '../error-alert';

describe('ErrorAlert', () => {
  it('should render error message', () => {
    render(<ErrorAlert message="Something went wrong" />);
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('should render default title when not provided', () => {
    render(<ErrorAlert message="Error message" />);
    expect(screen.getByText('Lỗi')).toBeInTheDocument();
  });

  it('should render custom title when provided', () => {
    render(<ErrorAlert title="Custom Error" message="Error message" />);
    expect(screen.getByText('Custom Error')).toBeInTheDocument();
  });

  it('should show dismiss button when onDismiss is provided', () => {
    const onDismiss = vi.fn();
    render(<ErrorAlert message="Error message" onDismiss={onDismiss} />);

    const dismissButton = screen.getByRole('button', { name: /bỏ qua/i });
    expect(dismissButton).toBeInTheDocument();
  });

  it('should call onDismiss when dismiss button clicked', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    render(<ErrorAlert message="Error message" onDismiss={onDismiss} />);

    const dismissButton = screen.getByRole('button', { name: /bỏ qua/i });
    await user.click(dismissButton);

    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('should not show dismiss button when onDismiss is not provided', () => {
    render(<ErrorAlert message="Error message" />);

    expect(screen.queryByRole('button', { name: /bỏ qua/i })).not.toBeInTheDocument();
  });

  it('should show retry button when onRetry is provided', () => {
    const onRetry = vi.fn();
    render(<ErrorAlert message="Error message" onRetry={onRetry} />);

    const retryButton = screen.getByRole('button', { name: /thử lại/i });
    expect(retryButton).toBeInTheDocument();
  });

  it('should call onRetry when retry button clicked', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    render(<ErrorAlert message="Error message" onRetry={onRetry} />);

    const retryButton = screen.getByRole('button', { name: /thử lại/i });
    await user.click(retryButton);

    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('should not show retry button when onRetry is not provided', () => {
    render(<ErrorAlert message="Error message" />);

    expect(screen.queryByRole('button', { name: /thử lại/i })).not.toBeInTheDocument();
  });
});
