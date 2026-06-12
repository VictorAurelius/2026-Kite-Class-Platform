/**
 * Wave 41 Bucket D (GAP-272o) — Step6Preview orchestrator wiring tests.
 *
 * Verifies the wiring shipped this PR:
 *   1. `<RegenerateCounter>` renders driven by `useRegenerateQuota` (MSW)
 *      with the quota query's `tier`/`limit`/`used` flowed through props.
 *   2. Click Deploy CTA → component flips to `<DeployingStep>` (no longer
 *      renders the iframe preview); waits for `useDeployStream` events.
 *   3. Quota-exceeded surfaces the upsell modal automatically for
 *      non-ENTERPRISE tiers (per `ai-branding-guidelines.md` §4.3).
 *
 * SSE stream is not driven by MSW (per `branding.ts` handler comment); the
 * test verifies presence of `<DeployingStep>` after the flip — full SSE
 * end-to-end is covered by the hook's own unit test once an EventSource
 * polyfill lands (TODO(GAP-272e)).
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Step6Preview } from '../Step6Preview';
import {
  INITIAL_WIZARD_STATE,
  wizardReducer,
  type WizardState,
} from '../wizard-shared';
import { server } from '@/test/msw/server';

// Stub `@kite/shared-ui` ThemePreview — heavy component not relevant to wiring.
// Use importOriginal so other exports (InstanceLifecycleStatus consumed by
// DeployingStep → LifecycleInline) remain real.
vi.mock('@kite/shared-ui', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@kite/shared-ui')>();
  return {
    ...actual,
    ThemePreview: () => <div data-testid="theme-preview-stub" />,
  };
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

describe('Step6Preview — orchestrator wiring (GAP-272o)', () => {
  it('wires RegenerateCounter to useRegenerateQuota hook', async () => {
    const Wrapper = makeWrapper();
    const state = makeState();
    render(
      <Wrapper>
        <Step6Preview wizardState={state} dispatch={() => {}} onBack={noop} onDeploy={noop} />
      </Wrapper>,
    );

    expect(screen.queryByTestId('step6-regenerate-counter-scaffold')).toBeNull();
    expect(screen.getByTestId('step6-regenerate-counter-wired')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId('regenerate-counter-status')).toBeInTheDocument();
    });
    expect(screen.getByTestId('regenerate-counter-active-tier-badge')).toHaveTextContent('FREE');
  });

  it('flips to DeployingStep when user clicks the Deploy CTA', async () => {
    const Wrapper = makeWrapper();
    const state = makeState();
    render(
      <Wrapper>
        <Step6Preview wizardState={state} dispatch={() => {}} onBack={noop} onDeploy={noop} />
      </Wrapper>,
    );

    expect(screen.getByTestId('step6-preview-iframe')).toBeInTheDocument();
    expect(screen.queryByTestId('deploying-step')).toBeNull();

    fireEvent.click(screen.getByTestId('step6-deploy-button'));

    await waitFor(() => {
      expect(screen.queryByTestId('step6-preview-iframe')).toBeNull();
      expect(screen.getByTestId('deploying-step')).toBeInTheDocument();
    });
  });

  it('does NOT flip to DeployingStep when not all resources approved', () => {
    const Wrapper = makeWrapper();
    const state = makeState({ approvedResources: ['logo'] });
    render(
      <Wrapper>
        <Step6Preview wizardState={state} dispatch={() => {}} onBack={noop} onDeploy={noop} />
      </Wrapper>,
    );

    const button = screen.getByTestId('step6-deploy-button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    fireEvent.click(button);
    expect(screen.queryByTestId('deploying-step')).toBeNull();
  });

  it('opens the upsell modal automatically when quota exceeded for non-ENTERPRISE tier', async () => {
    server.use(
      http.get('*/api/v1/branding/regenerate-quota', () =>
        HttpResponse.json({
          tier: 'FREE',
          used: 3,
          limit: 3,
          resetAt: '2026-05-08T00:00:00.000Z',
        }),
      ),
    );

    const Wrapper = makeWrapper();
    const state = makeState();
    render(
      <Wrapper>
        <Step6Preview wizardState={state} dispatch={() => {}} onBack={noop} onDeploy={noop} />
      </Wrapper>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('regenerate-counter-upsell-modal')).toBeInTheDocument();
    });
  });
});

describe('wizardReducer — Preview (Step 5) entry sanity', () => {
  it('NEXT_STEP reaches the Preview step (5) cleanly in TEMPLATE mode', () => {
    let s: WizardState = INITIAL_WIZARD_STATE;
    for (let i = 0; i < 5; i++) s = wizardReducer(s, { type: 'NEXT_STEP' });
    expect(s.currentStep).toBe(5);
  });
});
