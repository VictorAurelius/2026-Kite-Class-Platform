/**
 * Wave 32 Bucket C — TemplateStep + ApprovalStep (G11 ThemePreview integration)
 *
 * Tests:
 *  1. TemplateStep renders 6 template cards
 *  2. Click a card → calls onNext with templateId + jobId
 *  3. Selected card shows aria-pressed=true (ring style)
 *  4. "Quay lại" button calls onBack
 *  5. ApprovalStep renders G11 ThemePreview (data-testid="theme-preview")
 *  6. ApprovalStep renders 4 ResourceToggle rows
 *  7. Deploy button disabled when no resources approved
 *  8. Deploy button enabled after all 4 approved
 *  9. Click ResourceToggle → resource becomes approved
 *
 * @since Wave 32 Bucket C
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';

// ---------------------------------------------------------------------------
// Mock @kite/shared-ui — G11 ThemePreview
// ---------------------------------------------------------------------------
vi.mock('@kite/shared-ui', () => ({
  ThemePreview: () => <div data-testid="theme-preview">ThemePreview</div>,
}));

// ---------------------------------------------------------------------------
// Imports after mocks
// ---------------------------------------------------------------------------
import { TemplateStep } from '../TemplateStep';
import { ApprovalStep } from '../ApprovalStep';
import type { WizardState } from '../wizard-shared';
import { INITIAL_WIZARD_STATE } from '../wizard-shared';

// ---------------------------------------------------------------------------
// Shared fixtures
// ---------------------------------------------------------------------------

/** Minimal WizardState for TemplateStep / ApprovalStep tests. */
function makeState(overrides: Partial<WizardState> = {}): WizardState {
  return { ...INITIAL_WIZARD_STATE, ...overrides };
}

// ---------------------------------------------------------------------------
// TemplateStep tests
// ---------------------------------------------------------------------------

describe('TemplateStep', () => {
  // vi.fn() typed with explicit generic for full mock introspection.
  let onNext: ReturnType<typeof vi.fn<(templateId: string, jobId: string) => void>>;
  let onBack: ReturnType<typeof vi.fn<() => void>>;

  beforeEach(() => {
    onNext = vi.fn<(templateId: string, jobId: string) => void>();
    onBack = vi.fn<() => void>();
  });

  it('1. renders 6 template cards', () => {
    render(
      <TemplateStep
        wizardState={makeState()}
        instanceId="inst-1"
        onNext={onNext}
        onBack={onBack}
      />,
    );
    for (let i = 1; i <= 6; i++) {
      expect(screen.getByRole('button', { name: `Mẫu ${i}` })).toBeInTheDocument();
    }
  });

  it('2. clicking a card calls onNext with templateId and jobId', () => {
    render(
      <TemplateStep
        wizardState={makeState()}
        instanceId="inst-1"
        onNext={onNext}
        onBack={onBack}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Mẫu 1' }));
    expect(onNext).toHaveBeenCalledOnce();
    const [templateId, jobId] = onNext.mock.calls[0] as [string, string];
    expect(templateId).toBe('template-edu-1');
    expect(typeof jobId).toBe('string');
    expect(jobId.length).toBeGreaterThan(0);
  });

  it('3. selected card has aria-pressed=true', () => {
    render(
      <TemplateStep
        wizardState={makeState({ templateId: 'template-edu-3' })}
        instanceId="inst-1"
        onNext={onNext}
        onBack={onBack}
      />,
    );
    const card = screen.getByRole('button', { name: 'Mẫu 3' });
    expect(card).toHaveAttribute('aria-pressed', 'true');
    const card1 = screen.getByRole('button', { name: 'Mẫu 1' });
    expect(card1).toHaveAttribute('aria-pressed', 'false');
  });

  it('4. "Quay lại" calls onBack', () => {
    render(
      <TemplateStep
        wizardState={makeState()}
        instanceId="inst-1"
        onNext={onNext}
        onBack={onBack}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /quay lại/i }));
    expect(onBack).toHaveBeenCalledOnce();
  });
});

// ---------------------------------------------------------------------------
// ApprovalStep tests
// ---------------------------------------------------------------------------

describe('ApprovalStep', () => {
  let onPublish: ReturnType<typeof vi.fn<() => void>>;
  let onBack: ReturnType<typeof vi.fn<() => void>>;

  beforeEach(() => {
    onPublish = vi.fn<() => void>();
    onBack = vi.fn<() => void>();
  });

  function renderApprovalStep(stateOverrides: Partial<WizardState> = {}) {
    const state = makeState({
      jobId: 'job-1',
      templateId: 'template-edu-1',
      ...stateOverrides,
    });
    return render(
      <ApprovalStep
        wizardState={state}
        jobId="job-1"
        onPublish={onPublish}
        onBack={onBack}
      />,
    );
  }

  it('5. renders G11 ThemePreview', () => {
    renderApprovalStep();
    expect(screen.getByTestId('theme-preview')).toBeInTheDocument();
  });

  it('6. renders 4 ResourceToggle rows', () => {
    renderApprovalStep();
    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes).toHaveLength(4);
  });

  it('7. Deploy button disabled when no resources approved', () => {
    renderApprovalStep();
    const deployBtn = screen.getByRole('button', { name: /triển khai/i });
    expect(deployBtn).toBeDisabled();
  });

  it('8. Deploy button enabled after all 4 resources approved', () => {
    renderApprovalStep();
    const checkboxes = screen.getAllByRole('checkbox');
    checkboxes.forEach((cb) => fireEvent.click(cb));
    const deployBtn = screen.getByRole('button', { name: /triển khai/i });
    expect(deployBtn).not.toBeDisabled();
  });

  it('9. clicking ResourceToggle marks resource as approved (aria-checked=true)', () => {
    renderApprovalStep();
    const checkboxes = screen.getAllByRole('checkbox');
    const firstCheckbox = checkboxes[0]!;
    expect(firstCheckbox).toHaveAttribute('aria-checked', 'false');
    fireEvent.click(firstCheckbox);
    expect(firstCheckbox).toHaveAttribute('aria-checked', 'true');
  });
});
