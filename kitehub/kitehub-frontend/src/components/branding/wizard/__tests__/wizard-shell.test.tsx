/**
 * Wave 32 Bucket A — Wizard shell + Step 1-2 tests.
 *
 * Coverage targets (per plan §3 Bucket A "Tests ≥6"):
 *  1. wizardReducer — step transitions
 *  2. wizardReducer — slug actions invalidate prior validation result
 *  3. wizardReducer — APPROVE_RESOURCE / UNAPPROVE_RESOURCE / RESET_APPROVALS (Bucket C compliance)
 *  4. wizardReducer — SET_TEMPLATE clears prior approvals
 *  5. StepIndicator — renders 6 labelled steps with correct aria-current
 *  6. WelcomeStep — slug 'available' enables Continue; conflict shows suggestions
 *  7. WelcomeStep — clicking a suggestion adopts it via SET_SLUG
 *  8. LogoStep — fork toggle dispatches SET_LOGO with aiLogo flag
 *  9. LogoStep — invalid file size emits error banner, never dispatches
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import {
  wizardReducer,
  INITIAL_WIZARD_STATE,
  type WizardState,
} from '../wizard-shared';
import { StepIndicator } from '../StepIndicator';
import { WelcomeStep } from '../WelcomeStep';
import { LogoStep } from '../LogoStep';
import { PortraitStep } from '../PortraitStep';

// Shared mutable mock returns: `assetsReturn` lets LogoStep (GAP-1112 #3 picker)
// + PortraitStep (GAP-1134) tests inject an asset gallery; `uploadMock` asserts
// PortraitStep upload call args. vi.hoisted lets the factory below reference them
// despite vi.mock hoisting.
const hoisted = vi.hoisted(() => ({
  assetsReturn: { data: [] as Array<Record<string, unknown>> },
  uploadMock: vi
    .fn()
    .mockResolvedValue({ id: 'new', type: 'PORTRAIT', url: 'https://cdn.test/new.png' }),
}));

// Mock the branding hooks so LogoStep / PortraitStep tests don't hit the network
vi.mock('@/hooks/use-branding', () => ({
  useUploadAsset: () => ({
    mutateAsync: hoisted.uploadMock,
    isPending: false,
  }),
  useAssets: () => hoisted.assetsReturn,
}));

// Stub sonner toast so LogoStep tests don't depend on the real provider
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('wizardReducer — step transitions', () => {
  it('NEXT_STEP / PREV_STEP cap at boundaries 1..5 (GAP-1216 output-first reorder)', () => {
    let s: WizardState = INITIAL_WIZARD_STATE;
    expect(s.currentStep).toBe(1);

    s = wizardReducer(s, { type: 'PREV_STEP' });
    expect(s.currentStep).toBe(1);

    // TEMPLATE mode walks all 5 steps.
    for (let i = 0; i < 6; i++) {
      s = wizardReducer(s, { type: 'NEXT_STEP' });
    }
    expect(s.currentStep).toBe(5);

    s = wizardReducer(s, { type: 'GO_TO_STEP', step: 3 });
    expect(s.currentStep).toBe(3);
  });

  it('kit v3 — both modes walk the linear 5 steps (Template step removed)', () => {
    // FULL_AI no longer skips step 4: Assets (3) → Tạo&Duyệt (4) → Triển khai (5).
    let s: WizardState = { ...INITIAL_WIZARD_STATE, mode: 'FULL_AI', currentStep: 3 };
    s = wizardReducer(s, { type: 'NEXT_STEP' });
    expect(s.currentStep).toBe(4);
    s = wizardReducer(s, { type: 'NEXT_STEP' });
    expect(s.currentStep).toBe(5);
    s = wizardReducer(s, { type: 'PREV_STEP' });
    expect(s.currentStep).toBe(4);
  });

  it('orgType defaults to SMALL_CENTER (GAP-1231 — card dropped from UI) + SET_ORG_TYPE still persists', () => {
    // GAP-1231: org-type card removed from Step 1; the field defaults to a safe
    // centre-shaped value and stays mutable for the generate request.
    expect(INITIAL_WIZARD_STATE.orgType).toBe('SMALL_CENTER');
    const next = wizardReducer(INITIAL_WIZARD_STATE, {
      type: 'SET_ORG_TYPE',
      orgType: 'LARGE_CENTER',
    });
    expect(next.orgType).toBe('LARGE_CENTER');
  });
});

describe('wizardReducer — slug lifecycle', () => {
  it('SET_SLUG clears prior status + suggestions', () => {
    const prior: WizardState = {
      ...INITIAL_WIZARD_STATE,
      slug: 'old',
      slugStatus: 'conflict',
      conflictSuggestions: ['old-1', 'old-2'],
    };
    const next = wizardReducer(prior, { type: 'SET_SLUG', slug: 'new-slug' });
    expect(next.slug).toBe('new-slug');
    expect(next.slugStatus).toBe('default');
    expect(next.conflictSuggestions).toEqual([]);
  });

  it('SET_SLUG_STATUS persists status + suggestions when provided', () => {
    const next = wizardReducer(INITIAL_WIZARD_STATE, {
      type: 'SET_SLUG_STATUS',
      status: 'conflict',
      suggestions: ['a', 'b'],
    });
    expect(next.slugStatus).toBe('conflict');
    expect(next.conflictSuggestions).toEqual(['a', 'b']);
  });
});

describe('wizardReducer — approvedResources (Bucket C compliance)', () => {
  it('APPROVE / UNAPPROVE / RESET_APPROVALS round-trip', () => {
    let s: WizardState = INITIAL_WIZARD_STATE;
    s = wizardReducer(s, { type: 'APPROVE_RESOURCE', resource: 'logo' });
    s = wizardReducer(s, { type: 'APPROVE_RESOURCE', resource: 'colors' });
    s = wizardReducer(s, { type: 'APPROVE_RESOURCE', resource: 'logo' }); // dedup
    expect(s.approvedResources).toEqual(['logo', 'colors']);

    s = wizardReducer(s, { type: 'UNAPPROVE_RESOURCE', resource: 'logo' });
    expect(s.approvedResources).toEqual(['colors']);

    s = wizardReducer(s, { type: 'RESET_APPROVALS' });
    expect(s.approvedResources).toEqual([]);
  });

  it('SET_TEMPLATE clears any prior approvals (avoids cross-template stale state)', () => {
    const prior: WizardState = {
      ...INITIAL_WIZARD_STATE,
      approvedResources: ['logo', 'colors'],
    };
    const next = wizardReducer(prior, {
      type: 'SET_TEMPLATE',
      templateId: 'tpl-2',
      jobId: 'job-2',
    });
    expect(next.templateId).toBe('tpl-2');
    expect(next.jobId).toBe('job-2');
    expect(next.approvedResources).toEqual([]);
  });
});

describe('StepIndicator', () => {
  it('renders the 5 kit-v3 steps and marks currentStep as aria-current="step"', () => {
    render(<StepIndicator currentStep={3} />);

    const labels = ['Bắt đầu', 'Phong cách', 'Hình ảnh', 'Tạo & Duyệt', 'Triển khai'];
    for (const label of labels) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
    // Template step removed from the flow.
    expect(screen.queryByText('Mẫu thiết kế')).not.toBeInTheDocument();

    const current = screen.getByLabelText(/Bước 3: Hình ảnh \(đang làm\)/);
    expect(current).toHaveAttribute('aria-current', 'step');

    const completed = screen.getByLabelText(/Bước 1: Bắt đầu \(đã xong\)/);
    expect(completed).not.toHaveAttribute('aria-current');
  });

  it('kit v3 — FULL_AI mode shows all 5 steps too (no Template skip)', () => {
    render(<StepIndicator currentStep={3} mode="FULL_AI" />);
    expect(screen.getByText('Tạo & Duyệt')).toBeInTheDocument();
    expect(screen.getByText('Triển khai')).toBeInTheDocument();
    expect(screen.queryByText('Mẫu thiết kế')).not.toBeInTheDocument();
  });
});

describe('WelcomeStep — slug validation', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('available slug enables the Continue button', async () => {
    let state: WizardState = {
      ...INITIAL_WIZARD_STATE,
      tenantName: 'Trung tâm Toán Master',
      slug: 'unique-slug',
      // GAP-1133 — org-type is now a required Step 1 field; seed it so the
      // Continue gating depends solely on the slug-availability path here.
      orgType: 'SOLO_TEACHER',
    };
    const dispatch = vi.fn((action) => {
      state = wizardReducer(state, action);
    });
    const onNext = vi.fn();

    const { rerender } = render(
      <WelcomeStep wizardState={state} dispatch={dispatch} onNext={onNext} />
    );

    // Drive the debounce + stub round-trip
    await act(async () => {
      await vi.advanceTimersByTimeAsync(900);
    });

    // After validation, dispatch should have flipped status to 'available'
    expect(state.slugStatus).toBe('available');

    rerender(<WelcomeStep wizardState={state} dispatch={dispatch} onNext={onNext} />);

    const cta = screen.getByTestId('wizard-step1-continue');
    expect(cta).not.toBeDisabled();
    fireEvent.click(cta);
    expect(onNext).toHaveBeenCalledTimes(1);
  });

  it('org-type card is dropped from the UI; Continue gates on name + slug only (GAP-1231)', async () => {
    let state: WizardState = {
      ...INITIAL_WIZARD_STATE,
      tenantName: 'Trung tâm Toán Master',
      slug: 'unique-slug',
    };
    const dispatch = vi.fn((action) => {
      state = wizardReducer(state, action);
    });

    const { rerender } = render(
      <WelcomeStep wizardState={state} dispatch={dispatch} onNext={vi.fn()} />
    );

    // GAP-1231: org-type card no longer rendered in Step 1.
    expect(screen.queryByTestId('wizard-org-type')).not.toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(900);
    });
    expect(state.slugStatus).toBe('available');

    rerender(<WelcomeStep wizardState={state} dispatch={dispatch} onNext={vi.fn()} />);
    // Name + slug available (no org-type gate) → Continue enabled.
    expect(screen.getByTestId('wizard-step1-continue')).not.toBeDisabled();
  });

  it('conflict slug shows suggestions; clicking one dispatches SET_SLUG', async () => {
    let state: WizardState = {
      ...INITIAL_WIZARD_STATE,
      tenantName: 'Trung tâm Toán Master',
      slug: 'toan-master', // SLUG_STUB_TAKEN
    };
    const dispatch = vi.fn((action) => {
      state = wizardReducer(state, action);
    });

    const { rerender } = render(
      <WelcomeStep wizardState={state} dispatch={dispatch} onNext={vi.fn()} />
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(900);
    });

    expect(state.slugStatus).toBe('conflict');
    expect(state.conflictSuggestions.length).toBeGreaterThan(0);

    rerender(<WelcomeStep wizardState={state} dispatch={dispatch} onNext={vi.fn()} />);

    const suggestion = screen.getByRole('button', { name: 'toan-master-2026' });
    fireEvent.click(suggestion);
    expect(dispatch).toHaveBeenCalledWith({
      type: 'SET_SLUG',
      slug: 'toan-master-2026',
    });

    // Continue is disabled while in conflict state (no name typed elsewhere
    // can flip it without an 'available' slug)
    rerender(<WelcomeStep wizardState={state} dispatch={dispatch} onNext={vi.fn()} />);
    expect(screen.getByTestId('wizard-step1-continue')).toBeDisabled();
  });
});

describe('LogoStep — fork + validation', () => {
  beforeEach(() => {
    // Default: no previously-uploaded assets ⇒ picker section stays hidden.
    hoisted.assetsReturn = { data: [] };
  });

  it('switching to AI-generate fork dispatches SET_LOGO with aiLogo=true', () => {
    let state: WizardState = INITIAL_WIZARD_STATE;
    const dispatch = vi.fn((action) => {
      state = wizardReducer(state, action);
    });

    render(
      <LogoStep
        wizardState={state}
        dispatch={dispatch}
        instanceId="inst-1"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    fireEvent.click(screen.getByTestId('wizard-logo-fork-ai'));
    expect(dispatch).toHaveBeenCalledWith({
      type: 'SET_LOGO',
      url: '',
      aiLogo: true,
    });

    expect(screen.getByTestId('wizard-logo-skip')).toBeInTheDocument();
    expect(screen.getByTestId('wizard-step2-continue')).not.toBeDisabled();
  });

  it('rejects oversized files via error banner without dispatching', async () => {
    const state: WizardState = INITIAL_WIZARD_STATE;
    const dispatch = vi.fn();

    render(
      <LogoStep
        wizardState={state}
        dispatch={dispatch}
        instanceId="inst-1"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    const input = screen.getByTestId('wizard-logo-file-input') as HTMLInputElement;
    const oversized = new File(['x'.repeat(3 * 1024 * 1024)], 'big.png', {
      type: 'image/png',
    });
    Object.defineProperty(input, 'files', { value: [oversized] });
    fireEvent.change(input);

    const banner = await screen.findByTestId('wizard-logo-error');
    expect(banner).toHaveTextContent(/2MB/);
    // No SET_LOGO/SET_LOGO/CLEAR_LOGO from validation reject path
    expect(dispatch).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: 'SET_LOGO' })
    );
  });

  it('asset library lists only LOGO assets; picking one dispatches SET_LOGO (GAP-1112 #3)', () => {
    hoisted.assetsReturn = {
      data: [
        {
          id: 'a1',
          instanceId: 'inst-1',
          type: 'LOGO',
          url: 'https://cdn.test/prev-logo.png',
          s3Key: 'k1',
          createdAt: '2026-06-10T00:00:00Z',
        },
        {
          id: 'a2',
          instanceId: 'inst-1',
          type: 'HERO',
          url: 'https://cdn.test/hero.png',
          s3Key: 'k2',
          createdAt: '2026-06-10T00:00:00Z',
        },
      ],
    };

    let state: WizardState = INITIAL_WIZARD_STATE;
    const dispatch = vi.fn((action) => {
      state = wizardReducer(state, action);
    });

    render(
      <LogoStep
        wizardState={state}
        dispatch={dispatch}
        instanceId="inst-1"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );

    // Only the LOGO asset renders a picker tile; the HERO asset is filtered out.
    const logoTile = screen.getByTestId('wizard-logo-library-a1');
    expect(screen.queryByTestId('wizard-logo-library-a2')).toBeNull();

    fireEvent.click(logoTile);
    expect(dispatch).toHaveBeenCalledWith({
      type: 'SET_LOGO',
      url: 'https://cdn.test/prev-logo.png',
      aiLogo: false,
    });
  });
});

describe('PortraitStep — count hint + upload (GAP-1134)', () => {
  beforeEach(() => {
    hoisted.assetsReturn = { data: [] };
    hoisted.uploadMock.mockClear();
  });

  function renderPortrait(orgType: WizardState['orgType']) {
    const state: WizardState = { ...INITIAL_WIZARD_STATE, orgType };
    return render(
      <PortraitStep
        wizardState={state}
        dispatch={vi.fn()}
        instanceId="inst-1"
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    );
  }

  it('count hint suggests 1 portrait for a solo teacher', () => {
    renderPortrait('SOLO_TEACHER');
    expect(screen.getByTestId('wizard-portrait-hint')).toHaveTextContent(/1 ảnh chân dung/);
  });

  it('count hint suggests multiple portraits for a centre', () => {
    renderPortrait('SMALL_CENTER');
    // SMALL_CENTER portraitHint = 3.
    expect(screen.getByTestId('wizard-portrait-hint')).toHaveTextContent(/khoảng 3 ảnh/);
  });

  it('gallery lists only PORTRAIT assets', () => {
    hoisted.assetsReturn = {
      data: [
        { id: 'p1', type: 'PORTRAIT', url: 'https://cdn.test/p1.png' },
        { id: 'l1', type: 'LOGO', url: 'https://cdn.test/logo.png' },
      ],
    };
    renderPortrait('SMALL_CENTER');

    expect(screen.getByTestId('wizard-portrait-tile-p1')).toBeInTheDocument();
    expect(screen.queryByTestId('wizard-portrait-tile-l1')).toBeNull();
    expect(screen.getByTestId('wizard-portrait-gallery')).toHaveTextContent(/Ảnh đã tải lên \(1\)/);
  });

  it('uploading a valid file calls upload with assetType=PORTRAIT', async () => {
    renderPortrait('SOLO_TEACHER');

    const input = screen.getByTestId('wizard-portrait-file-input') as HTMLInputElement;
    const file = new File(['x'], 'teacher.png', { type: 'image/png' });
    Object.defineProperty(input, 'files', { value: [file], configurable: true });

    await act(async () => {
      fireEvent.change(input);
    });

    expect(hoisted.uploadMock).toHaveBeenCalledWith({
      instanceId: 'inst-1',
      type: 'PORTRAIT',
      file,
    });
  });

  it('rejects oversized files via error banner without uploading', async () => {
    renderPortrait('SOLO_TEACHER');

    const input = screen.getByTestId('wizard-portrait-file-input') as HTMLInputElement;
    const oversized = new File(['x'.repeat(3 * 1024 * 1024)], 'big.png', {
      type: 'image/png',
    });
    Object.defineProperty(input, 'files', { value: [oversized], configurable: true });

    await act(async () => {
      fireEvent.change(input);
    });

    expect(await screen.findByTestId('wizard-portrait-error')).toHaveTextContent(/2MB/);
    expect(hoisted.uploadMock).not.toHaveBeenCalled();
  });
});
