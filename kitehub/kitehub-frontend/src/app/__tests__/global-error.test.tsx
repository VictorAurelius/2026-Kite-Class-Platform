/**
 * Tests for global-error.tsx (Next.js root-level fatal error boundary).
 *
 * global-error.tsx replaces the entire <html>/<body> tree, so it renders
 * its own <html lang="vi"><body>...</body></html>. Tests verify the
 * body content (heading, reset button, console logging) rather than
 * the html shell (jsdom already has its own document).
 *
 * @since GAP-136
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '@/__tests__/test-utils';
import GlobalError from '../global-error';

describe('app/global-error', () => {
  const sampleError = Object.assign(new Error('Fatal boom'), {
    digest: 'fatal-xyz',
  });

  let consoleErrorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  it('renders Vietnamese fatal error heading', () => {
    render(<GlobalError error={sampleError} reset={vi.fn()} />);

    expect(
      screen.getByRole('heading', { name: /lỗi hệ thống/i })
    ).toBeInTheDocument();
  });

  it('renders Vietnamese helper copy', () => {
    render(<GlobalError error={sampleError} reset={vi.fn()} />);

    expect(
      screen.getByText(/hệ thống gặp sự cố/i)
    ).toBeInTheDocument();
  });

  it('triggers reset() when retry button is clicked', async () => {
    const user = userEvent.setup();
    const reset = vi.fn();
    render(<GlobalError error={sampleError} reset={reset} />);

    await user.click(screen.getByRole('button', { name: /thử lại/i }));

    expect(reset).toHaveBeenCalledTimes(1);
  });

  it('logs fatal error via Sentry-ready hook', () => {
    render(<GlobalError error={sampleError} reset={vi.fn()} />);

    expect(consoleErrorSpy).toHaveBeenCalled();
    const calledWith = consoleErrorSpy.mock.calls[0];
    expect(calledWith).toBeDefined();
    expect(calledWith?.some((arg: unknown) => arg === sampleError)).toBe(true);
  });
});
