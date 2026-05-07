import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AdvancedModeDisclaimer } from '../AdvancedModeDisclaimer';

function setup(open = true) {
  const onOpenChange = vi.fn();
  const onConfirm = vi.fn();
  const onCancel = vi.fn();
  const utils = render(
    <AdvancedModeDisclaimer
      open={open}
      onOpenChange={onOpenChange}
      onConfirm={onConfirm}
      onCancel={onCancel}
    />
  );
  return { onOpenChange, onConfirm, onCancel, ...utils };
}

describe('AdvancedModeDisclaimer', () => {
  it('renders all 5 disclaimer bullets', () => {
    setup();
    for (let i = 0; i < 5; i++) {
      expect(
        screen.getByTestId(`advanced-mode-disclaimer-bullet-${i}`)
      ).toBeInTheDocument();
    }
  });

  it('confirm button disabled by default (checkbox unchecked)', () => {
    setup();
    const confirm = screen.getByTestId('advanced-mode-disclaimer-confirm-button');
    expect(confirm).toBeDisabled();
  });

  it('confirm enables only after checkbox checked', () => {
    const { onConfirm } = setup();
    const confirm = screen.getByTestId('advanced-mode-disclaimer-confirm-button');
    expect(confirm).toBeDisabled();

    // Tick the checkbox
    fireEvent.click(screen.getByTestId('advanced-mode-disclaimer-checkbox'));
    expect(confirm).not.toBeDisabled();

    // Now confirm fires
    fireEvent.click(confirm);
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('cancel handler fires + does NOT call onConfirm', () => {
    const { onCancel, onConfirm } = setup();
    fireEvent.click(screen.getByTestId('advanced-mode-disclaimer-cancel-button'));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
