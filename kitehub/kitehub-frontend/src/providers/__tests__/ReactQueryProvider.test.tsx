/**
 * Component tests for ReactQueryProvider.
 *
 * @since PR 5.10
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { useQuery } from '@tanstack/react-query';
import { ReactQueryProvider } from '../ReactQueryProvider';

// Test component that uses React Query
function TestQueryComponent() {
  const { data, isLoading } = useQuery({
    queryKey: ['test'],
    queryFn: () => Promise.resolve('test data'),
  });

  if (isLoading) return <div>Loading...</div>;
  return <div data-testid="query-result">{data}</div>;
}

describe('ReactQueryProvider', () => {
  it('renders children', () => {
    render(
      <ReactQueryProvider>
        <div data-testid="child">Child content</div>
      </ReactQueryProvider>
    );
    expect(screen.getByTestId('child')).toBeInTheDocument();
  });

  it('provides QueryClient to children', async () => {
    render(
      <ReactQueryProvider>
        <TestQueryComponent />
      </ReactQueryProvider>
    );

    // Should show loading initially
    expect(screen.getByText('Loading...')).toBeInTheDocument();

    // Wait for query to resolve
    const result = await screen.findByTestId('query-result');
    expect(result).toHaveTextContent('test data');
  });

  it('renders multiple children', () => {
    render(
      <ReactQueryProvider>
        <div data-testid="child1">Child 1</div>
        <div data-testid="child2">Child 2</div>
      </ReactQueryProvider>
    );
    expect(screen.getByTestId('child1')).toBeInTheDocument();
    expect(screen.getByTestId('child2')).toBeInTheDocument();
  });
});
