import { describe, it, expect, vi } from 'vitest';
import {
  render as rtlRender,
  screen,
  fireEvent,
  type RenderOptions,
} from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactElement, ReactNode } from 'react';
import { LifecycleInline } from '../LifecycleInline';

// Wave 34 Bucket D: LifecycleInline now consumes `useLifecycleEvents` via
// react-query. Wrap renders in a fresh QueryClient so the provider exists.
function render(ui: ReactElement, options?: RenderOptions) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'TestQueryClientWrapper';
  return rtlRender(ui, { wrapper: Wrapper, ...options });
}

const ALL_5_STATES = [
  'NOT_STARTED',
  'GENERATING',
  'DEPLOYED',
  'REGENERATING',
  'FAILED',
] as const;

describe('LifecycleInline', () => {
  it.each(ALL_5_STATES)('renders %s state correctly', (state) => {
    render(
      <LifecycleInline
        instanceId="inst-test-001"
        instanceName="Test Center"
        stateOverride={state}
      />
    );
    const root = screen.getByTestId('lifecycle-inline');
    expect(root).toHaveAttribute('data-state', state);
  });

  it('forwards onRetry only for FAILED state', () => {
    const onRetry = vi.fn();
    const { rerender } = render(
      <LifecycleInline
        instanceId="inst-test-001"
        stateOverride="FAILED"
        onRetry={onRetry}
      />
    );

    // FAILED — retry button rendered (G9 spec — `Thử lại` button)
    const retryButton = screen.getByRole('button', { name: /Thử lại/ });
    fireEvent.click(retryButton);
    expect(onRetry).toHaveBeenCalledTimes(1);

    // DEPLOYED — no retry button
    rerender(
      <LifecycleInline
        instanceId="inst-test-001"
        stateOverride="DEPLOYED"
        onRetry={onRetry}
      />
    );
    expect(screen.queryByRole('button', { name: /Thử lại/ })).not.toBeInTheDocument();
  });

  it('renders NOT_STARTED with no instanceId (mid-wizard)', () => {
    render(<LifecycleInline instanceId={undefined} stateOverride="NOT_STARTED" />);
    expect(screen.getByTestId('lifecycle-inline')).toHaveAttribute(
      'data-state',
      'NOT_STARTED'
    );
  });
});
