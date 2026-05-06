/**
 * Wave 32 Bucket D — Tests (GAP-272 ai-branding-wizard v2)
 *
 * Covers:
 *   1. QualityGateWidget — pass variant (score ≥70, all checks green)
 *   2. QualityGateWidget — fail variant (score <70, auto-regen button)
 *   3. RegenerateCounter — decrements on regenerate; disabled at 0
 *   4. RegenerateCounter — quota-empty triggers upsell modal
 *   5. DeployingStep — SSE mock log lines append
 *   6. LifecycleInline — renders all 5 states
 *   7. Settings Advanced page — non-Enterprise shows upgrade prompt
 *   8. AdvancedModeDisclaimer — confirm disabled until checkbox checked
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import React from 'react';

// ─── Mocks ────────────────────────────────────────────────────────────────────

// Mock @kite/shared-ui so InstanceLifecycleStatus renders a predictable structure
vi.mock('@kite/shared-ui', () => ({
  InstanceLifecycleStatus: ({
    state,
    instanceId,
  }: {
    state: string;
    instanceId: string;
  }) => (
    <div data-testid="g9-lifecycle" data-state={state} data-instance={instanceId}>
      G9: {state}
    </div>
  ),
  validTransition: vi.fn(() => true),
}));

// Mock useBrandingTier so tests control tier without env vars
const mockUseBrandingTier = vi.fn();
vi.mock('@/hooks/use-branding-tier', () => ({
  useBrandingTier: () => mockUseBrandingTier(),
}));

// Mock Button (use a simple pass-through to avoid Radix deps)
vi.mock('@/components/ui/button', () => ({
  Button: ({
    children,
    onClick,
    disabled,
    className,
    ...rest
  }: React.ButtonHTMLAttributes<HTMLButtonElement> & { children?: React.ReactNode }) => (
    <button onClick={onClick} disabled={disabled} className={className} {...rest}>
      {children}
    </button>
  ),
}));

// ─── Imports (after mocks) ─────────────────────────────────────────────────────

import { QualityGateWidget } from '../QualityGateWidget';
import { RegenerateCounter } from '../RegenerateCounter';
import { DeployingStep } from '../DeployingStep';
import { LifecycleInline } from '../LifecycleInline';
import { AdvancedModeDisclaimer } from '../AdvancedModeDisclaimer';

// ─── 1. QualityGateWidget — pass variant ─────────────────────────────────────

describe('QualityGateWidget — pass variant (score ≥70)', () => {
  it('shows score, PASS badge and deploy CTA when score ≥70', () => {
    render(
      <QualityGateWidget
        score={95}
        onDeploy={vi.fn()}
      />
    );

    expect(screen.getByText('95')).toBeTruthy();
    expect(screen.getByText('ĐẠT YÊU CẦU')).toBeTruthy();
    expect(screen.getByRole('button', { name: /triển khai/i })).toBeTruthy();
    // Auto-regen button must NOT be visible
    expect(screen.queryByRole('button', { name: /tạo lại tự động/i })).toBeNull();
  });

  it('all 5 check rows pass when score is high', () => {
    render(<QualityGateWidget score={90} />);
    // All checks should show CheckCircle aria-labels (Đạt)
    const passIcons = screen.getAllByLabelText('Đạt');
    expect(passIcons.length).toBe(5);
  });
});

// ─── 2. QualityGateWidget — fail variant ────────────────────────────────────

describe('QualityGateWidget — fail variant (score <70)', () => {
  it('shows CHƯA ĐẠT badge and auto-regen button when score <70', () => {
    const onAutoRegen = vi.fn();
    render(
      <QualityGateWidget
        score={45}
        onAutoRegenerate={onAutoRegen}
      />
    );

    expect(screen.getByText('45')).toBeTruthy();
    expect(screen.getByText('CHƯA ĐẠT')).toBeTruthy();

    const regenBtn = screen.getByRole('button', { name: /tạo lại tự động/i });
    expect(regenBtn).toBeTruthy();

    fireEvent.click(regenBtn);
    expect(onAutoRegen).toHaveBeenCalledTimes(1);
  });

  it('shows threshold warning alert when score <70', () => {
    render(<QualityGateWidget score={55} />);
    // Alert text contains threshold info
    expect(screen.getByRole('alert')).toBeTruthy();
  });
});

// ─── 3. RegenerateCounter — decrement ────────────────────────────────────────

describe('RegenerateCounter — decrement on regenerate', () => {
  it('shows remaining count and calls onRegenerate; button enabled when quota remains', () => {
    const onRegen = vi.fn();
    render(
      <RegenerateCounter
        tier="FREE"
        regenerateQuota={3}
        regenerateUsed={1}
        onRegenerate={onRegen}
      />
    );

    expect(screen.getByText('2/3 lượt còn')).toBeTruthy();

    const btn = screen.getByRole('button', { name: /tạo lại/i });
    expect(btn).not.toBeDisabled();

    fireEvent.click(btn);
    expect(onRegen).toHaveBeenCalledTimes(1);
  });

  it('button shows disabled state text when quota is exhausted', () => {
    render(
      <RegenerateCounter
        tier="FREE"
        regenerateQuota={3}
        regenerateUsed={3}
        onRegenerate={vi.fn()}
      />
    );

    expect(screen.getByText('0/3 lượt còn')).toBeTruthy();
    const btn = screen.getByRole('button');
    // Button text changes to upsell
    expect(btn.textContent).toMatch(/nâng cấp để tạo lại thêm/i);
  });
});

// ─── 4. RegenerateCounter — quota-empty upsell modal ─────────────────────────

describe('RegenerateCounter — quota-empty shows upsell modal', () => {
  it('opens upsell modal when clicking button with 0 remaining', () => {
    render(
      <RegenerateCounter
        tier="FREE"
        regenerateQuota={3}
        regenerateUsed={3}
        onUpgrade={vi.fn()}
      />
    );

    fireEvent.click(screen.getByRole('button'));
    // Upsell dialog should appear
    expect(screen.getByRole('dialog')).toBeTruthy();
    expect(screen.getByText(/hết lượt tạo lại/i)).toBeTruthy();
  });

  it('calls onUpgrade with next tier when upgrading from FREE', () => {
    const onUpgrade = vi.fn();
    render(
      <RegenerateCounter
        tier="FREE"
        regenerateQuota={3}
        regenerateUsed={3}
        onUpgrade={onUpgrade}
      />
    );

    fireEvent.click(screen.getByRole('button'));
    // Click the upgrade button in modal
    const upgradeBtn = screen.getByRole('button', { name: /nâng lên basic/i });
    fireEvent.click(upgradeBtn);
    expect(onUpgrade).toHaveBeenCalledWith('BASIC');
  });
});

// ─── 5. DeployingStep — SSE mock log lines append ────────────────────────────

describe('DeployingStep — mock SSE log lines append', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  it('appends log lines over time using mock SSE', async () => {
    render(
      <DeployingStep
        instanceId="INST-TEST-001"
        lifecycleStatus="GENERATING"
        useMockSse
      />
    );

    // Initially no log lines
    const logRegion = screen.getByRole('log');
    expect(logRegion).toBeTruthy();

    // Advance timers to get first log line (500ms)
    await act(async () => {
      vi.advanceTimersByTime(500);
    });

    await waitFor(() => {
      expect(screen.getByText(/khởi tạo triển khai/i)).toBeTruthy();
    });

    // Advance more to get second line
    await act(async () => {
      vi.advanceTimersByTime(500);
    });

    await waitFor(() => {
      expect(screen.getByText(/MinIO: logo\.png/i)).toBeTruthy();
    });

    vi.useRealTimers();
  });

  it('shows DEPLOYED status in heading when lifecycle is DEPLOYED', () => {
    render(
      <DeployingStep
        instanceId="INST-TEST-002"
        lifecycleStatus="DEPLOYED"
        liveUrl="https://test.kiteclass.vn"
        useMockSse
      />
    );

    expect(screen.getByText(/triển khai hoàn thành/i)).toBeTruthy();
  });
});

// ─── 6. LifecycleInline — renders all 5 states ───────────────────────────────

describe('LifecycleInline — all 5 lifecycle states', () => {
  const states = [
    'NOT_STARTED',
    'GENERATING',
    'DEPLOYED',
    'REGENERATING',
    'FAILED',
  ] as const;

  states.forEach((status) => {
    it(`renders ${status} state via G9`, () => {
      render(
        <LifecycleInline
          status={status}
          instanceId="INST-LIFE-001"
        />
      );

      const g9 = screen.getByTestId('g9-lifecycle');
      expect(g9.getAttribute('data-state')).toBe(status);
    });
  });
});

// ─── 7. Settings Advanced page — non-Enterprise upgrade prompt ───────────────

describe('Settings Advanced page — non-Enterprise user', () => {
  beforeEach(() => {
    mockUseBrandingTier.mockReturnValue({
      tier: 'FREE',
      advancedModeEnabled: false,
      setAdvancedModeEnabled: vi.fn(),
      canUseCustomPrompt: false,
      regenerateQuota: 3,
      regenerateUsed: 0,
      regenerateRemaining: 3,
      isQuotaExhausted: false,
      incrementRegenerate: vi.fn(),
      inputTokenCap: 2000,
    });
  });

  it('shows upgrade prompt for FREE tier user', async () => {
    // Lazy import after mock is configured
    const { default: SettingsBrandingAdvancedPage } = await import(
      '../../../../../../app/(customer)/settings/branding/advanced/page'
    );
    render(<SettingsBrandingAdvancedPage />);

    expect(screen.getByText(/Advanced Mode/)).toBeTruthy();
    expect(screen.getByText(/nâng cấp lên enterprise/i)).toBeTruthy();
    // Toggle should NOT be present for non-ENTERPRISE
    expect(screen.queryByRole('switch')).toBeNull();
  });
});

// ─── 8. AdvancedModeDisclaimer — confirm gated by checkbox ──────────────────

describe('AdvancedModeDisclaimer', () => {
  it('confirm button is disabled until checkbox is checked', () => {
    const onConfirm = vi.fn();
    render(
      <AdvancedModeDisclaimer
        open={true}
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />
    );

    const confirmBtn = screen.getByRole('button', { name: /bật advanced mode/i });
    expect(confirmBtn).toBeDisabled();

    // Clicking while disabled should not call onConfirm
    fireEvent.click(confirmBtn);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('confirm button enables and calls onConfirm after checkbox is checked', () => {
    const onConfirm = vi.fn();
    render(
      <AdvancedModeDisclaimer
        open={true}
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />
    );

    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);

    const confirmBtn = screen.getByRole('button', { name: /bật advanced mode/i });
    expect(confirmBtn).not.toBeDisabled();

    fireEvent.click(confirmBtn);
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('returns null when open=false', () => {
    const { container } = render(
      <AdvancedModeDisclaimer
        open={false}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });
});
