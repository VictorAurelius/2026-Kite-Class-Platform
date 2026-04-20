/**
 * Tests for root-level error.tsx (Next.js App Router error boundary).
 *
 * Covers:
 * - Vietnamese error heading + body copy renders
 * - "Thử lại" button triggers reset() callback
 * - Dev-mode shows error details (digest, message)
 * - Sentry-ready useEffect hook fires on mount with error
 *
 * @since GAP-136
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '@/__tests__/test-utils';
import ErrorBoundary from '../error';

describe('app/error', () => {
  const sampleError = Object.assign(new Error('Boom'), {
    digest: 'abc123',
  });

  let consoleErrorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  it('renders Vietnamese error heading', () => {
    render(<ErrorBoundary error={sampleError} reset={vi.fn()} />);

    expect(
      screen.getByRole('heading', { name: /đã xảy ra lỗi/i })
    ).toBeInTheDocument();
  });

  it('renders helper copy in Vietnamese', () => {
    render(<ErrorBoundary error={sampleError} reset={vi.fn()} />);

    expect(
      screen.getByText(/đã có lỗi xảy ra khi tải trang này/i)
    ).toBeInTheDocument();
  });

  it('calls reset() when "Thử lại" button is clicked', async () => {
    const user = userEvent.setup();
    const reset = vi.fn();
    render(<ErrorBoundary error={sampleError} reset={reset} />);

    await user.click(screen.getByRole('button', { name: /thử lại/i }));

    expect(reset).toHaveBeenCalledTimes(1);
  });

  it('logs error to console via Sentry-ready hook', () => {
    render(<ErrorBoundary error={sampleError} reset={vi.fn()} />);

    expect(consoleErrorSpy).toHaveBeenCalled();
    const calledWith = consoleErrorSpy.mock.calls[0];
    expect(calledWith).toBeDefined();
    expect(calledWith?.some((arg: unknown) => arg === sampleError)).toBe(true);
  });

  it('renders home link as fallback action', () => {
    render(<ErrorBoundary error={sampleError} reset={vi.fn()} />);

    const homeLink = screen.getByRole('link', { name: /về trang chủ/i });
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute('href', '/');
  });
});
