/**
 * Test utilities and custom render functions
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from '@/components/ui/toaster';

// Create a custom render function that includes providers
function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false, // Disable retries in tests
        gcTime: Infinity, // Keep cache forever in tests
      },
      mutations: {
        retry: false,
      },
    },
    // Note: logger option removed in React Query v5
  });
}

interface AllTheProvidersProps {
  children: React.ReactNode;
}

export function AllTheProviders({ children }: AllTheProvidersProps) {
  const testQueryClient = createTestQueryClient();

  return (
    <QueryClientProvider client={testQueryClient}>
      {children}
      <Toaster />
    </QueryClientProvider>
  );
}

export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>
) {
  return render(ui, { wrapper: AllTheProviders, ...options });
}

// Re-export everything from RTL
export * from '@testing-library/react';

// Override render with our custom version
export { renderWithProviders as render };
