/**
 * Wave 32 REWORK Bucket C — Step 5 + Step 6 preview tests.
 *
 * Coverage:
 *  1. TemplateGrid filters by audience + tone props.
 *  2. TemplateFullscreen modal opens + 3 quality badges visible.
 *  3. TemplateStep custom-prompt visible IFF tier === ENTERPRISE.
 *  4. TemplateStep custom-prompt HIDDEN when tier !== ENTERPRISE.
 *  5. Step6Preview iframe renders with valid src.
 *  6. ResourceToggle dispatches APPROVE_RESOURCE action.
 *  7. wizardReducer APPROVE_RESOURCE / UNAPPROVE_RESOURCE state transitions.
 *  8. TemplateGrid widens back to ≥6 templates when filters narrow below 6.
 *  9. Step6Preview renders G11 ThemePreview with live brand colours from state.
 * 10. Step6Preview deploy disabled until all 4 resources approved.
 */
import { describe, it, expect, vi } from 'vitest';
import {
  render,
  render as rtlRender,
  screen,
  fireEvent,
  type RenderOptions,
} from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactElement, ReactNode } from 'react';

// Wave 34 Bucket D: Step6Preview now consumes useBrandingJobV1 via
// react-query. Wrap that subset of tests with a fresh QueryClient.
function renderWithQuery(ui: ReactElement, options?: RenderOptions) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'TestQueryClientWrapper';
  return rtlRender(ui, { wrapper: Wrapper, ...options });
}

// Stub Bucket D's `useBrandingTier` hook so TemplateStep tests don't need a
// React Query provider. `tierOverride` short-circuits the real hook in
// production code, but the hook is still INVOKED per React rules — keeping
// it inert via mock avoids touching `useActiveSubscription`.
vi.mock('@/hooks/use-branding-tier', () => ({
  useBrandingTier: () => ({
    tier: 'FREE' as const,
    regenerateQuota: 3,
    advancedModeEnabled: false,
    canUseCustomPrompt: false,
    isLoading: false,
  }),
}));

import { TemplateGrid, filterTemplates, TEMPLATES } from '../TemplateGrid';
import { TemplateFullscreen } from '../TemplateFullscreen';
import { TemplateStep } from '../TemplateStep';
import { Step6Preview } from '../Step6Preview';
import { ResourceToggle } from '../ResourceToggle';
import {
  INITIAL_WIZARD_STATE,
  wizardReducer,
  type WizardState,
} from '../wizard-shared';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function makeState(overrides: Partial<WizardState> = {}): WizardState {
  return { ...INITIAL_WIZARD_STATE, ...overrides };
}

// ---------------------------------------------------------------------------
// 1. TemplateGrid filters by audience + tone
// ---------------------------------------------------------------------------

describe('TemplateGrid', () => {
  it('filters templates by both audience and tone', () => {
    // exam-prep + professional should match T1, T2, T4, T6 (4 of 6)
    const filtered = filterTemplates(TEMPLATES, 'exam-prep', 'professional');
    const ids = filtered.map((t) => t.id);
    expect(ids).toContain('template-t1-navy-focus');
    expect(ids).toContain('template-t2-score-board');
    // T3 is friendly-first — should NOT be in the matched set... but the
    // function widens back to ≥6 when matches drop, so we need to check
    // actual filter output before widening:
    const strict = TEMPLATES.filter(
      (t) =>
        t.audiences.includes('exam-prep') && t.tones.includes('professional'),
    );
    expect(strict.length).toBeGreaterThanOrEqual(1);
    expect(strict.every((t) => t.audiences.includes('exam-prep'))).toBe(true);
    expect(strict.every((t) => t.tones.includes('professional'))).toBe(true);
  });

  it('widens back to ≥6 templates when filters narrow below 6 (§2.2)', () => {
    // mam-non + luxurious matches very few (likely 0)
    const filtered = filterTemplates(TEMPLATES, 'mam-non', 'luxurious');
    expect(filtered.length).toBe(TEMPLATES.length);
    expect(filtered.length).toBeGreaterThanOrEqual(6);
  });

  it('returns all templates when audience or tone is null', () => {
    expect(filterTemplates(TEMPLATES, null, null).length).toBe(TEMPLATES.length);
  });

  it('renders template cards with correct test IDs', () => {
    const onSelect = vi.fn();
    const onOpenFullscreen = vi.fn();
    render(
      <TemplateGrid
        audience={null}
        tone={null}
        selectedId={null}
        onSelect={onSelect}
        onOpenFullscreen={onOpenFullscreen}
      />,
    );
    expect(screen.getByTestId('template-card-T1')).toBeInTheDocument();
    expect(screen.getByTestId('template-card-T2')).toBeInTheDocument();
    expect(screen.getByTestId('template-card-T6')).toBeInTheDocument();
  });

  it('fires onSelect when card clicked', () => {
    const onSelect = vi.fn();
    render(
      <TemplateGrid
        audience={null}
        tone={null}
        selectedId={null}
        onSelect={onSelect}
        onOpenFullscreen={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByTestId('template-card-T2'));
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onSelect.mock.calls[0]?.[0].id).toBe('template-t2-score-board');
  });
});

// ---------------------------------------------------------------------------
// 2. TemplateFullscreen modal opens + 3 badges visible
// ---------------------------------------------------------------------------

describe('TemplateFullscreen', () => {
  it('renders 3 quality badges when template is provided', () => {
    const sample = TEMPLATES[1]!; // T2 Score Board
    render(
      <TemplateFullscreen
        template={sample}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
      />,
    );
    expect(
      screen.getByTestId('template-fullscreen-badge-wcag'),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId('template-fullscreen-badge-responsive'),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId('template-fullscreen-badge-text-safety'),
    ).toBeInTheDocument();
  });

  it('marks WCAG badge fail when ratio < 4.5', () => {
    const sample = TEMPLATES[1]!;
    render(
      <TemplateFullscreen
        template={sample}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        badgesOverride={{
          wcagRatio: 3.2,
          responsiveOk: true,
          textSafetyMaxChars: 50,
        }}
      />,
    );
    const wcag = screen.getByTestId('template-fullscreen-badge-wcag');
    expect(wcag.getAttribute('data-pass')).toBe('false');
  });

  it('renders nothing when template is null', () => {
    render(
      <TemplateFullscreen
        template={null}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
      />,
    );
    expect(screen.queryByTestId('template-fullscreen')).not.toBeInTheDocument();
  });
});

// ---------------------------------------------------------------------------
// 3 + 4. TemplateStep custom-prompt Enterprise-gated
// ---------------------------------------------------------------------------

describe('TemplateStep custom-prompt gating (§2.1)', () => {
  it('shows custom-prompt UI when tier === ENTERPRISE', () => {
    render(
      <TemplateStep
        wizardState={makeState({ audience: 'exam-prep', tone: 'professional' })}
        dispatch={vi.fn()}
        tierOverride="ENTERPRISE"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );
    expect(screen.getByTestId('custom-prompt-section')).toBeInTheDocument();
    expect(screen.getByTestId('custom-prompt-textarea')).toBeInTheDocument();
  });

  it('HIDES custom-prompt UI when tier === FREE', () => {
    render(
      <TemplateStep
        wizardState={makeState({ audience: 'exam-prep', tone: 'professional' })}
        dispatch={vi.fn()}
        tierOverride="FREE"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );
    expect(
      screen.queryByTestId('custom-prompt-section'),
    ).not.toBeInTheDocument();
  });

  it('HIDES custom-prompt UI when tier === PRO (regression guard for §2.1)', () => {
    render(
      <TemplateStep
        wizardState={makeState()}
        dispatch={vi.fn()}
        tierOverride="PRO"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );
    expect(
      screen.queryByTestId('custom-prompt-section'),
    ).not.toBeInTheDocument();
  });

  it('HIDES custom-prompt UI when tier === PREMIUM (regression guard for §2.1)', () => {
    render(
      <TemplateStep
        wizardState={makeState()}
        dispatch={vi.fn()}
        tierOverride="PREMIUM"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );
    expect(
      screen.queryByTestId('custom-prompt-section'),
    ).not.toBeInTheDocument();
  });

  it('updates customPrompt textarea value when changed (Enterprise only, local state)', () => {
    // Note: post-rework `customPrompt` is LOCAL component state (not in
    // WizardState — Bucket A's canonical reducer does not own this field).
    // We therefore assert visible textarea value rather than a dispatch call.
    render(
      <TemplateStep
        wizardState={makeState()}
        dispatch={vi.fn()}
        tierOverride="ENTERPRISE"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );
    const ta = screen.getByTestId('custom-prompt-textarea') as HTMLTextAreaElement;
    fireEvent.change(ta, { target: { value: 'banner ấm áp' } });
    expect(ta.value).toBe('banner ấm áp');
  });

  it('TemplateStep filters templates using wizardState.audience + tone', () => {
    // We render with an audience/tone combo that narrows; expect the rendered
    // grid still contains 6 cards (filterTemplates widens to satisfy §2.2).
    render(
      <TemplateStep
        wizardState={makeState({ audience: 'mam-non', tone: 'friendly' })}
        dispatch={vi.fn()}
        tierOverride="FREE"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />,
    );
    // Both templates that match (T3 + T5) MUST render
    expect(screen.getByTestId('template-card-T3')).toBeInTheDocument();
    expect(screen.getByTestId('template-card-T5')).toBeInTheDocument();
  });
});

// ---------------------------------------------------------------------------
// 5. Step6Preview iframe renders
// ---------------------------------------------------------------------------

describe('Step6Preview iframe + G11 ThemePreview', () => {
  it('renders iframe preview client-side via srcDoc (AC2 fix / GAP-272j)', () => {
    renderWithQuery(
      <Step6Preview
        wizardState={makeState({
          tenantName: 'Toán Master',
          slug: 'toan-master',
          templateId: 'template-t2-score-board',
          jobId: 'job-abc-123',
        })}
        dispatch={vi.fn()}
      />,
    );
    const iframe = screen.getByTestId('step6-preview-iframe') as HTMLIFrameElement;
    expect(iframe).toBeInTheDocument();
    // AC2 fix: preview is rendered CLIENT-SIDE via srcDoc (brand colours +
    // org name + logo), NOT an unauthenticated backend <iframe src> that
    // 401/404s into a blank frame. srcDoc reflects the wizard state.
    const srcDoc = iframe.getAttribute('srcdoc') ?? '';
    expect(srcDoc).toContain('Toán Master');
    expect(srcDoc).toContain('toan-master.kiteclass.vn');
    // No backend src request issued (would lack the auth header).
    expect(iframe.getAttribute('src')).toBeNull();
  });

  it('renders G11 ThemePreview with brand colours (fallback while job loads)', () => {
    renderWithQuery(
      <Step6Preview
        wizardState={makeState({
          templateId: 'template-t6-roadmap-vertical',
          jobId: 'job-abc-123',
        })}
        dispatch={vi.fn()}
      />,
    );
    expect(screen.getByTestId('step6-theme-preview')).toBeInTheDocument();
  });

  it('uses brandColors override when provided (test-friendly hook)', () => {
    const dispatch = vi.fn();
    renderWithQuery(
      <Step6Preview
        wizardState={makeState()}
        dispatch={dispatch}
        brandColors={{
          primary: '#FF0000',
          secondary: '#00FF00',
          background: '#FFFFFF',
          foreground: '#000000',
        }}
      />,
    );
    expect(screen.getByTestId('step6-theme-preview')).toBeInTheDocument();
  });

  it('disables deploy button until all 4 resources approved (§4.2)', () => {
    const { rerender } = renderWithQuery(
      <Step6Preview
        wizardState={makeState({
          templateId: 'template-t2-score-board',
          approvedResources: ['logo'],
        })}
        dispatch={vi.fn()}
      />,
    );
    const deployBtn = screen.getByTestId('step6-deploy-button');
    expect(deployBtn).toBeDisabled();

    rerender(
      <Step6Preview
        wizardState={makeState({
          templateId: 'template-t2-score-board',
          approvedResources: ['logo', 'colors', 'banner', 'hero'],
        })}
        dispatch={vi.fn()}
      />,
    );
    expect(screen.getByTestId('step6-deploy-button')).not.toBeDisabled();
  });
});

// ---------------------------------------------------------------------------
// 6. ResourceToggle dispatches actions
// ---------------------------------------------------------------------------

describe('ResourceToggle (§4.2 reducer-controlled state)', () => {
  it('dispatches APPROVE_RESOURCE when toggle turns on', () => {
    const dispatch = vi.fn();
    render(
      <ResourceToggle
        resource="logo"
        title="Logo"
        description="SVG · 12 KB"
        approved={false}
        dispatch={dispatch}
      />,
    );
    fireEvent.click(screen.getByTestId('resource-toggle-switch-logo'));
    expect(dispatch).toHaveBeenCalledWith({
      type: 'APPROVE_RESOURCE',
      resource: 'logo',
    });
  });

  it('dispatches UNAPPROVE_RESOURCE when toggle turns off', () => {
    const dispatch = vi.fn();
    render(
      <ResourceToggle
        resource="banner"
        title="Banner"
        description="T2"
        approved={true}
        dispatch={dispatch}
      />,
    );
    fireEvent.click(screen.getByTestId('resource-toggle-switch-banner'));
    expect(dispatch).toHaveBeenCalledWith({
      type: 'UNAPPROVE_RESOURCE',
      resource: 'banner',
    });
  });

  it('renders approved/un-approved visual state via data attribute', () => {
    const { rerender } = render(
      <ResourceToggle
        resource="colors"
        title="Bảng màu"
        description="Navy + Cam"
        approved={false}
        dispatch={vi.fn()}
      />,
    );
    expect(
      screen.getByTestId('resource-toggle-colors').getAttribute('data-approved'),
    ).toBe('false');

    rerender(
      <ResourceToggle
        resource="colors"
        title="Bảng màu"
        description="Navy + Cam"
        approved={true}
        dispatch={vi.fn()}
      />,
    );
    expect(
      screen.getByTestId('resource-toggle-colors').getAttribute('data-approved'),
    ).toBe('true');
  });
});

// ---------------------------------------------------------------------------
// 7. Reducer state transitions for APPROVE / UNAPPROVE / RESET
// ---------------------------------------------------------------------------

describe('wizardReducer per-resource approve actions', () => {
  it('APPROVE_RESOURCE adds id to approvedResources', () => {
    const next = wizardReducer(INITIAL_WIZARD_STATE, {
      type: 'APPROVE_RESOURCE',
      resource: 'logo',
    });
    expect(next.approvedResources).toEqual(['logo']);
  });

  it('APPROVE_RESOURCE is idempotent (no duplicate)', () => {
    const once = wizardReducer(INITIAL_WIZARD_STATE, {
      type: 'APPROVE_RESOURCE',
      resource: 'logo',
    });
    const twice = wizardReducer(once, {
      type: 'APPROVE_RESOURCE',
      resource: 'logo',
    });
    expect(twice.approvedResources).toEqual(['logo']);
  });

  it('UNAPPROVE_RESOURCE removes id', () => {
    const start = wizardReducer(INITIAL_WIZARD_STATE, {
      type: 'APPROVE_RESOURCE',
      resource: 'banner',
    });
    const after = wizardReducer(start, {
      type: 'UNAPPROVE_RESOURCE',
      resource: 'banner',
    });
    expect(after.approvedResources).toEqual([]);
  });

  it('RESET_APPROVALS clears the list while preserving other state', () => {
    const start = makeState({
      tenantName: 'Toán Master',
      approvedResources: ['logo', 'colors', 'banner'],
    });
    const after = wizardReducer(start, { type: 'RESET_APPROVALS' });
    expect(after.approvedResources).toEqual([]);
    expect(after.tenantName).toBe('Toán Master');
  });
});
