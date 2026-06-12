import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { RegenerateCounter } from '../RegenerateCounter';

function setup(overrides: Partial<Parameters<typeof RegenerateCounter>[0]> = {}) {
  const props = {
    tier: 'FREE' as const,
    regenerateQuota: 3,
    regeneratesUsed: 0,
    upsellModalOpen: false,
    onRegenerate: vi.fn(),
    onUpgradeClick: vi.fn(),
    onContinueWithCurrent: vi.fn(),
    onUpsellModalOpenChange: vi.fn(),
    ...overrides,
  };
  return { props, ...render(<RegenerateCounter {...props} />) };
}

describe('RegenerateCounter — FREE tier', () => {
  it('decrements display when regenerate clicked', () => {
    const { props, rerender } = setup({ regeneratesUsed: 0 });
    expect(screen.getByTestId('regenerate-counter-status')).toHaveTextContent(
      '3/3 lượt còn'
    );
    fireEvent.click(screen.getByTestId('regenerate-counter-button'));
    expect(props.onRegenerate).toHaveBeenCalledTimes(1);

    // Caller updates regeneratesUsed
    rerender(<RegenerateCounter {...{ ...props, regeneratesUsed: 1 }} />);
    expect(screen.getByTestId('regenerate-counter-status')).toHaveTextContent(
      '2/3 lượt còn'
    );
  });

  it('quota empty → button disabled', () => {
    setup({ regeneratesUsed: 3 });
    const button = screen.getByTestId('regenerate-counter-button');
    expect(button).toBeDisabled();
    expect(screen.getByTestId('regenerate-counter-bar')).toHaveAttribute(
      'data-quota-empty',
      'true'
    );
  });

  it('quota empty modal-open triggers onUpgradeClick from modal', () => {
    const { props } = setup({ regeneratesUsed: 3, upsellModalOpen: true });
    expect(screen.getByTestId('regenerate-counter-upsell-modal')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('regenerate-counter-modal-upgrade-button'));
    expect(props.onUpgradeClick).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByTestId('regenerate-counter-continue-button'));
    expect(props.onContinueWithCurrent).toHaveBeenCalledTimes(1);
  });
});

describe('RegenerateCounter — comparison rows', () => {
  it('highlights active tier row', () => {
    setup({ tier: 'PREMIUM', regenerateQuota: 30, regeneratesUsed: 0 });
    const premiumRow = screen.getByTestId('regenerate-counter-row-premium');
    expect(premiumRow).toHaveAttribute('data-active', 'true');
    const freeRow = screen.getByTestId('regenerate-counter-row-free');
    expect(freeRow).toHaveAttribute('data-active', 'false');
  });
});

describe('RegenerateCounter — ENTERPRISE tier', () => {
  it('shows unlimited + no upgrade button', () => {
    const { props } = setup({
      tier: 'ENTERPRISE',
      regenerateQuota: -1,
      regeneratesUsed: 5,
    });
    expect(screen.getByTestId('regenerate-counter-status')).toHaveTextContent(
      'Không giới hạn'
    );
    expect(
      screen.queryByTestId('regenerate-counter-upgrade-button')
    ).not.toBeInTheDocument();
    // Button always enabled
    fireEvent.click(screen.getByTestId('regenerate-counter-button'));
    expect(props.onRegenerate).toHaveBeenCalledTimes(1);
  });
});

// GAP-1219(a): "Tạo lại" là no-op khi CHƯA generate lần đầu — gate bằng hasGenerated.
describe('RegenerateCounter — hasGenerated gating (GAP-1219)', () => {
  const baseProps = {
    tier: 'FREE' as const,
    regenerateQuota: 3,
    regeneratesUsed: 0,
    upsellModalOpen: false,
    onRegenerate: vi.fn(),
    onUpgradeClick: vi.fn(),
    onContinueWithCurrent: vi.fn(),
    onUpsellModalOpenChange: vi.fn(),
  };

  it('disables the regenerate button + shows hint when hasGenerated=false', () => {
    render(<RegenerateCounter {...baseProps} hasGenerated={false} />);
    const btn = screen.getByTestId('regenerate-counter-button');
    expect(btn).toBeDisabled();
    expect(screen.getByTestId('regenerate-counter-status').textContent).toContain(
      'Khả dụng sau khi tạo bản đầu tiên',
    );
  });

  it('does not call onRegenerate when hasGenerated=false', () => {
    const onRegenerate = vi.fn();
    render(<RegenerateCounter {...baseProps} hasGenerated={false} onRegenerate={onRegenerate} />);
    fireEvent.click(screen.getByTestId('regenerate-counter-button'));
    expect(onRegenerate).not.toHaveBeenCalled();
  });

  it('keeps default behavior (enabled) when hasGenerated omitted', () => {
    render(<RegenerateCounter {...baseProps} />);
    expect(screen.getByTestId('regenerate-counter-button')).toBeEnabled();
  });
});

