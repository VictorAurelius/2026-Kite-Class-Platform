import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { LifecycleInline } from '../LifecycleInline';

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
