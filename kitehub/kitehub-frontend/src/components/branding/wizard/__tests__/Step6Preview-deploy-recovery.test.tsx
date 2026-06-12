/**
 * Step6Preview deploy-recovery tests (GAP-1216 / GAP-1217 FE).
 *
 * Verifies the server-side quality gate (422 QUALITY_GATE_FAILED) on approve
 * does NOT dead-end the user: the wizard returns to the preview so they can
 * edit + retry, instead of getting stuck on the deploying spinner.
 *
 * Full SSE-driven DONE / FAILED end-to-end is covered by the DoneStep +
 * DeployingStep unit tests (the deploy-stream is not driven by MSW per the
 * branding handler comment).
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Step6Preview } from '../Step6Preview';
import { INITIAL_WIZARD_STATE, type WizardState } from '../wizard-shared';
import { server } from '@/test/msw/server';

vi.mock('@kite/shared-ui', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@kite/shared-ui')>();
  return { ...actual, ThemePreview: () => <div data-testid="theme-preview-stub" /> };
});

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'TestQueryClientWrapper';
  return Wrapper;
}

function makeState(overrides: Partial<WizardState> = {}): WizardState {
  return {
    ...INITIAL_WIZARD_STATE,
    currentStep: 5,
    tenantName: 'Test Center',
    slug: 'test-center',
    audience: 'high-school',
    tone: 'professional',
    templateId: 'tpl-001',
    jobId: 'job-test-001',
    approvedResources: ['logo', 'colors', 'banner', 'hero'],
    ...overrides,
  };
}

const noop = () => {};

describe('Step6Preview — deploy recovery (GAP-1216 / GAP-1217)', () => {
  it('returns to the preview when approve fails the quality gate (422)', async () => {
    server.use(
      http.post('*/api/v1/branding/jobs/:jobId/approve', () =>
        HttpResponse.json(
          { errorCode: 'QUALITY_GATE_FAILED', qualityScore: 58, message: 'Quality below threshold' },
          { status: 422 },
        ),
      ),
    );

    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <Step6Preview wizardState={makeState()} dispatch={() => {}} onBack={noop} onDeploy={noop} />
      </Wrapper>,
    );

    expect(screen.getByTestId('step6-preview-iframe')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('step6-deploy-button'));

    // After the 422 resolves the wizard returns to the preview (no dead-end),
    // and never shows the terminal DONE screen.
    await waitFor(() => {
      expect(screen.getByTestId('step6-preview-iframe')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('done-step')).toBeNull();
  });
});
