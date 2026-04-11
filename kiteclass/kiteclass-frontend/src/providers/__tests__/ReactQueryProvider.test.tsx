/**
 * Tests for ReactQueryProvider — ensures devtools only in development.
 *
 * @since 2026-04-11
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';

describe('ReactQueryProvider', () => {
  it('does not render ReactQueryDevtools in production', async () => {
    const originalEnv = process.env.NODE_ENV;
    // @ts-expect-error overriding read-only for test
    process.env.NODE_ENV = 'production';

    const { ReactQueryProvider } = await import('../ReactQueryProvider');
    const { container } = render(
      <ReactQueryProvider>
        <div data-testid="child">content</div>
      </ReactQueryProvider>
    );

    expect(screen.getByTestId('child')).toBeInTheDocument();
    // Devtools button should not be present in production
    const devtoolsButton = container.querySelector('[data-testid="react-query-devtools-toggle-button"]');
    expect(devtoolsButton).toBeNull();

    // @ts-expect-error restoring
    process.env.NODE_ENV = originalEnv;
  });

  it('renders children correctly', async () => {
    const { ReactQueryProvider } = await import('../ReactQueryProvider');
    render(
      <ReactQueryProvider>
        <span data-testid="slot">hello</span>
      </ReactQueryProvider>
    );
    expect(screen.getByTestId('slot')).toBeInTheDocument();
  });
});
