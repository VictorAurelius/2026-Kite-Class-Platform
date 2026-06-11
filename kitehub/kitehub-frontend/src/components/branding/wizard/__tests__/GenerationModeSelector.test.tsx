import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import {
  GenerationModeSelector,
  type GenerationModeSelectorProps,
} from '../GenerationModeSelector';

function setup(overrides: Partial<GenerationModeSelectorProps> = {}) {
  const props: GenerationModeSelectorProps = {
    tier: 'FREE',
    value: 'TEMPLATE',
    onChange: vi.fn(),
    onUpgradeClick: vi.fn(),
    ...overrides,
  };
  return { props, ...render(<GenerationModeSelector {...props} />) };
}

describe('GenerationModeSelector — FREE tier', () => {
  it('FULL_AI disabled, lock + upgrade CTA visible, onUpgradeClick fires', () => {
    const { props } = setup({ tier: 'FREE' });

    const fullAi = screen.getByTestId('mode-option-full-ai');
    expect(fullAi).toBeDisabled();
    expect(fullAi).toHaveAttribute('aria-disabled', 'true');
    expect(fullAi).toHaveAttribute('data-locked', 'true');
    expect(screen.getByTestId('mode-full-ai-lock')).toBeInTheDocument();

    // TEMPLATE is the only usable choice.
    expect(screen.getByTestId('mode-option-template')).not.toBeDisabled();

    const cta = screen.getByTestId('mode-upgrade-cta');
    expect(cta).toBeInTheDocument();
    fireEvent.click(cta);
    expect(props.onUpgradeClick).toHaveBeenCalledTimes(1);

    // Clicking the disabled FULL_AI card must NOT change selection.
    fireEvent.click(fullAi);
    expect(props.onChange).not.toHaveBeenCalled();
  });
});

describe('GenerationModeSelector — BASIC tier', () => {
  it('FULL_AI disabled + upgrade CTA visible', () => {
    const { props } = setup({ tier: 'BASIC' });

    const fullAi = screen.getByTestId('mode-option-full-ai');
    expect(fullAi).toBeDisabled();
    expect(fullAi).toHaveAttribute('data-locked', 'true');
    expect(screen.getByTestId('mode-upgrade-cta')).toBeInTheDocument();

    fireEvent.click(fullAi);
    expect(props.onChange).not.toHaveBeenCalled();
  });
});

describe('GenerationModeSelector — PREMIUM tier', () => {
  it('FULL_AI enabled, shows "Còn N/limit", onChange fires, no CTA', () => {
    const { props } = setup({
      tier: 'PREMIUM',
      value: 'TEMPLATE',
      fullAiRemaining: 3,
      fullAiLimit: 5,
    });

    const fullAi = screen.getByTestId('mode-option-full-ai');
    expect(fullAi).not.toBeDisabled();
    expect(fullAi).toHaveAttribute('data-locked', 'false');
    expect(screen.getByTestId('mode-full-ai-sublabel')).toHaveTextContent(
      'Còn 3/5 lượt tháng này',
    );
    // Upgrade CTA is FREE/BASIC-only.
    expect(screen.queryByTestId('mode-upgrade-cta')).not.toBeInTheDocument();
    // No lock icon for eligible tier.
    expect(screen.queryByTestId('mode-full-ai-lock')).not.toBeInTheDocument();

    fireEvent.click(fullAi);
    expect(props.onChange).toHaveBeenCalledWith('FULL_AI');
  });

  it('quota=0 → FULL_AI disabled + exhausted note', () => {
    const { props } = setup({
      tier: 'PREMIUM',
      value: 'TEMPLATE',
      fullAiRemaining: 0,
      fullAiLimit: 5,
    });

    const fullAi = screen.getByTestId('mode-option-full-ai');
    expect(fullAi).toBeDisabled();
    expect(fullAi).toHaveAttribute('data-quota-exhausted', 'true');
    expect(screen.getByTestId('mode-full-ai-sublabel')).toHaveTextContent(
      'Đã hết lượt AI cao cấp tháng này — dùng Mẫu hoặc nâng cấp',
    );

    fireEvent.click(fullAi);
    expect(props.onChange).not.toHaveBeenCalled();
  });
});

describe('GenerationModeSelector — ENTERPRISE tier', () => {
  it('FULL_AI enabled + "Không giới hạn"', () => {
    const { props } = setup({
      tier: 'ENTERPRISE',
      value: 'TEMPLATE',
      fullAiRemaining: null,
    });

    const fullAi = screen.getByTestId('mode-option-full-ai');
    expect(fullAi).not.toBeDisabled();
    expect(screen.getByTestId('mode-full-ai-sublabel')).toHaveTextContent(
      'Không giới hạn',
    );
    expect(screen.queryByTestId('mode-upgrade-cta')).not.toBeInTheDocument();

    fireEvent.click(fullAi);
    expect(props.onChange).toHaveBeenCalledWith('FULL_AI');
  });
});

describe('GenerationModeSelector — TEMPLATE selection', () => {
  it('onChange to TEMPLATE fires from a FULL_AI-selected state', () => {
    const { props } = setup({
      tier: 'ENTERPRISE',
      value: 'FULL_AI',
      fullAiRemaining: null,
    });

    const template = screen.getByTestId('mode-option-template');
    expect(template).toHaveAttribute('aria-checked', 'false');
    fireEvent.click(template);
    expect(props.onChange).toHaveBeenCalledWith('TEMPLATE');
  });

  it('TEMPLATE always reflects selected state via aria-checked', () => {
    setup({ tier: 'FREE', value: 'TEMPLATE' });
    expect(screen.getByTestId('mode-option-template')).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });
});

describe('GenerationModeSelector — disabled prop', () => {
  it('disables both options + suppresses onChange', () => {
    const { props } = setup({
      tier: 'ENTERPRISE',
      value: 'TEMPLATE',
      fullAiRemaining: null,
      disabled: true,
    });

    const template = screen.getByTestId('mode-option-template');
    const fullAi = screen.getByTestId('mode-option-full-ai');
    expect(template).toBeDisabled();
    expect(fullAi).toBeDisabled();

    fireEvent.click(template);
    fireEvent.click(fullAi);
    expect(props.onChange).not.toHaveBeenCalled();
  });
});
