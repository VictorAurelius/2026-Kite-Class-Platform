/**
 * Tests for ReactQueryProvider — ensures children render correctly.
 *
 * @since 2026-04-11
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/utils';
import { ReactQueryProvider } from '../ReactQueryProvider';

describe('ReactQueryProvider', () => {
  it('renders children correctly', () => {
    render(
      <ReactQueryProvider>
        <span data-testid="slot">hello</span>
      </ReactQueryProvider>
    );
    expect(screen.getByTestId('slot')).toBeInTheDocument();
  });

  it('wraps children with QueryClientProvider', () => {
    render(
      <ReactQueryProvider>
        <div data-testid="child">content</div>
      </ReactQueryProvider>
    );
    expect(screen.getByTestId('child')).toBeInTheDocument();
    expect(screen.getByText('content')).toBeInTheDocument();
  });
});
