import { describe, it, expect, vi } from 'vitest';
import {
  render as rtlRender,
  screen,
  fireEvent,
  type RenderOptions,
} from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactElement, ReactNode } from 'react';
import { DeployingStep, type DeployingLogEntry } from '../DeployingStep';

// Wave 34 Bucket D: DeployingStep nests LifecycleInline → useLifecycleEvents
// (react-query). Wrap in QueryClientProvider so the provider exists.
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

const SAMPLE_LOGS: DeployingLogEntry[] = [
  {
    timestamp: '2026-05-07T14:32:08.000Z',
    message: 'Theme.json compiled (24 vars)',
    level: 'success',
  },
  {
    timestamp: '2026-05-07T14:32:14.000Z',
    message: 'Logo SVG uploaded',
    level: 'success',
  },
  {
    timestamp: '2026-05-07T14:32:35.000Z',
    message: 'Pushing to CDN (3/12 assets)…',
    level: 'pending',
  },
];

describe('DeployingStep', () => {
  it('renders provided logs', () => {
    render(
      <DeployingStep
        logs={SAMPLE_LOGS}
        instanceId="inst-test-001"
        progressPercent={62}
        lifecycleStateOverride="GENERATING"
      />
    );
    // Verify each log line rendered (1 line = 1 entry, in order)
    expect(screen.getByTestId('deploying-step-log-line-0')).toHaveTextContent(
      'Theme.json compiled'
    );
    expect(screen.getByTestId('deploying-step-log-line-1')).toHaveTextContent(
      'Logo SVG uploaded'
    );
    expect(screen.getByTestId('deploying-step-log-line-2')).toHaveTextContent(
      'Pushing to CDN'
    );
  });

  it('appends new log entries when re-rendered with extended logs (SSE simulation)', () => {
    const { rerender } = render(
      <DeployingStep
        logs={SAMPLE_LOGS.slice(0, 1)}
        instanceId="inst-test-001"
        lifecycleStateOverride="GENERATING"
      />
    );
    expect(screen.queryAllByTestId(/^deploying-step-log-line-/)).toHaveLength(1);

    rerender(
      <DeployingStep
        logs={SAMPLE_LOGS}
        instanceId="inst-test-001"
        lifecycleStateOverride="GENERATING"
      />
    );
    expect(screen.queryAllByTestId(/^deploying-step-log-line-/)).toHaveLength(3);
  });

  it('shows progress bar when progressPercent provided', () => {
    render(
      <DeployingStep
        logs={SAMPLE_LOGS}
        instanceId="inst-test-001"
        progressPercent={62}
        lifecycleStateOverride="GENERATING"
      />
    );
    const progress = screen.getByTestId('deploying-step-progress');
    expect(progress).toHaveAttribute('aria-valuenow', '62');
  });

  it('renders empty-state placeholder when logs empty', () => {
    render(
      <DeployingStep
        logs={[]}
        instanceId="inst-test-001"
        lifecycleStateOverride="GENERATING"
      />
    );
    // No log lines
    expect(screen.queryByTestId('deploying-step-log-line-0')).not.toBeInTheDocument();
    // Placeholder copy
    expect(screen.getByText('Đang chờ log…')).toBeInTheDocument();
  });
});

describe('DeployingStep — FAILED recovery (GAP-1216)', () => {
  it('renders the FAILED panel with retry + back when errorMessage set', () => {
    const onRetry = vi.fn();
    const onBack = vi.fn();
    render(
      <DeployingStep
        logs={SAMPLE_LOGS}
        instanceId="inst-test-001"
        errorMessage="Tạo banner thất bại"
        errorCode="GENERATION_FAILED"
        onRetry={onRetry}
        onBack={onBack}
      />
    );
    // FAILED panel replaces the in-progress spinner copy.
    expect(screen.getByTestId('deploying-step-failed')).toBeInTheDocument();
    expect(screen.queryByTestId('deploying-step')).toBeNull();
    expect(screen.getByTestId('deploying-step-error-message')).toHaveTextContent(
      'Tạo banner thất bại'
    );
    expect(screen.getByText(/GENERATION_FAILED/)).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('deploying-step-retry'));
    expect(onRetry).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByTestId('deploying-step-back'));
    expect(onBack).toHaveBeenCalledTimes(1);
  });

  it('hides the retry button when the error is not retryable', () => {
    render(
      <DeployingStep
        logs={[]}
        instanceId="inst-test-001"
        errorMessage="Lỗi không thể thử lại"
        errorRetryable={false}
        onRetry={() => {}}
        onBack={() => {}}
      />
    );
    expect(screen.queryByTestId('deploying-step-retry')).toBeNull();
    expect(screen.getByTestId('deploying-step-back')).toBeInTheDocument();
  });
});
