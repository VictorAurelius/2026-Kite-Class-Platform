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
import { renderHook, act, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useWizardDeploy } from '../hooks/useWizardDeploy';
import { INITIAL_WIZARD_STATE, type WizardState } from '../wizard-shared';
import { server } from '@/test/msw/server';

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


describe('useWizardDeploy — deploy recovery (GAP-1216 / GAP-1217, kit v3)', () => {
  it('advances to step 5 then returns to step 4 when approve fails the quality gate (422)', async () => {
    server.use(
      http.post('*/api/v1/branding/jobs/:jobId/approve', () =>
        HttpResponse.json(
          { errorCode: 'QUALITY_GATE_FAILED', qualityScore: 58, message: 'Quality below threshold' },
          { status: 422 },
        ),
      ),
    );

    const Wrapper = makeWrapper();
    const dispatch = vi.fn();
    const { result } = renderHook(
      () => useWizardDeploy({ wizardState: makeState(), dispatch }),
      { wrapper: Wrapper },
    );

    act(() => {
      result.current.start();
    });

    // start() advances to the explicit "Triển khai" step (5) before approve.
    expect(dispatch).toHaveBeenCalledWith({ type: 'GO_TO_STEP', step: 5 });

    // After the 422 resolves the controller returns the user to step 4 (review)
    // so they can edit + retry — no dead-end — and never marks the deploy done.
    await waitFor(() => {
      expect(dispatch).toHaveBeenCalledWith({ type: 'GO_TO_STEP', step: 4 });
    });
    expect(result.current.deployDone).toBe(false);
  });
});
