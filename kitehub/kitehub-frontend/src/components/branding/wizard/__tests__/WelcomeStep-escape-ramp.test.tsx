/**
 * GAP-1219(c): escape-ramp "Dùng gợi ý an toàn — thiết lập sau" ngay từ Welcome.
 * Benchmark branding-100: logo không bắt buộc, defaults trước, refine sau.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { WelcomeStep } from '../WelcomeStep';
import { INITIAL_WIZARD_STATE, type WizardState } from '../wizard-shared';

function renderWelcome(state: Partial<WizardState>) {
  const dispatch = vi.fn();
  const onNext = vi.fn();
  render(
    <WelcomeStep
      wizardState={{ ...INITIAL_WIZARD_STATE, ...state } as WizardState}
      dispatch={dispatch}
      onNext={onNext}
    />,
  );
  return { dispatch, onNext };
}

const readyState: Partial<WizardState> = {
  tenantName: 'Trung tâm Tiếng Anh Cô Hà',
  slug: 'co-ha-english',
  slugStatus: 'available',
  orgType: 'SMALL_CENTER',
};

describe('WelcomeStep — escape-ramp (GAP-1219c)', () => {
  it('disabled khi form chưa hợp lệ (giống nút Tiếp tục)', () => {
    renderWelcome({ tenantName: '', slugStatus: 'default', orgType: null });
    expect(screen.getByTestId('wizard-step1-use-defaults')).toBeDisabled();
  });

  it('áp defaults theo orgType center + nhảy tới bước Mẫu (6)', () => {
    const { dispatch } = renderWelcome(readyState);
    fireEvent.click(screen.getByTestId('wizard-step1-use-defaults'));
    expect(dispatch).toHaveBeenCalledWith({ type: 'SET_AUDIENCE', audience: 'english-center' });
    expect(dispatch).toHaveBeenCalledWith({ type: 'SET_TONE', tone: 'professional' });
    expect(dispatch).toHaveBeenCalledWith({ type: 'GO_TO_STEP', step: 6 });
  });

  it('orgType SOLO_TEACHER → audience exam-prep', () => {
    const { dispatch } = renderWelcome({ ...readyState, orgType: 'SOLO_TEACHER' });
    fireEvent.click(screen.getByTestId('wizard-step1-use-defaults'));
    expect(dispatch).toHaveBeenCalledWith({ type: 'SET_AUDIENCE', audience: 'exam-prep' });
  });
});
