/**
 * PaymentMethodSelector tests — TDD-driven coverage for the
 * `method-selecting` state per spec.md G5.
 *
 * Covers (≥7 cases mandated by Wave 27 Bucket C briefing):
 *  1. Renders radiogroup with all options
 *  2. Each option uses Vietnamese label verbatim from spec
 *  3. Selected option is checked; others are not (single-pick semantics)
 *  4. Click on unselected option fires onChange with that id
 *  5. Click on already-selected option does not re-fire onChange
 *  6. Disabled option is non-interactive (click does NOT fire onChange)
 *  7. Disabled option is excluded from keyboard arrow-key cycling
 *  8. Optional badges (popular / redirect) render when flagged
 *  9. ariaLabel + name override props apply
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PaymentMethodSelector } from '../PaymentMethodSelector';
import type { PaymentMethodOption } from '../types';

const VN_OPTIONS: PaymentMethodOption[] = [
  {
    id: 'VNPAY',
    label: 'VNPay',
    description: 'Thanh toán qua thẻ ngân hàng / QR — chuyển hướng tới VNPay',
    redirect: true,
  },
  {
    id: 'MOMO',
    label: 'Ví MoMo',
    description: 'Quét mã QR bằng app MoMo',
    popular: true,
  },
  {
    id: 'ZALOPAY',
    label: 'ZaloPay',
    description: 'Quét mã QR bằng app ZaloPay',
  },
  {
    id: 'BANK',
    label: 'Chuyển khoản ngân hàng',
    description: 'Vietcombank · Techcombank · BIDV · ACB',
  },
  {
    id: 'CASH',
    label: 'Tiền mặt tại trung tâm',
    description: 'Đến nộp tại văn phòng — giờ làm việc 8:00 – 20:00',
  },
];

describe('<PaymentMethodSelector>', () => {
  it('renders a radiogroup with all 5 VN payment methods', () => {
    render(<PaymentMethodSelector options={VN_OPTIONS} onChange={() => {}} />);
    const group = screen.getByRole('radiogroup', { name: /phương thức thanh toán/i });
    expect(group).toBeInTheDocument();
    expect(screen.getAllByRole('radio')).toHaveLength(5);
  });

  it('uses Vietnamese labels copy-pasted verbatim from spec', () => {
    render(<PaymentMethodSelector options={VN_OPTIONS} onChange={() => {}} />);
    expect(screen.getByLabelText(/^VNPay$/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Ví MoMo$/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^ZaloPay$/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Chuyển khoản ngân hàng$/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Tiền mặt tại trung tâm$/)).toBeInTheDocument();
  });

  it('marks only the selectedMethod as checked (single-pick semantics)', () => {
    render(
      <PaymentMethodSelector
        options={VN_OPTIONS}
        selectedMethod="MOMO"
        onChange={() => {}}
      />,
    );
    const radios = screen.getAllByRole('radio') as HTMLInputElement[];
    const checked = radios.filter((r) => r.checked);
    expect(checked).toHaveLength(1);
    expect(checked[0]?.value).toBe('MOMO');
  });

  it('fires onChange with the picked id when user clicks an unselected option', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <PaymentMethodSelector
        options={VN_OPTIONS}
        selectedMethod="VNPAY"
        onChange={onChange}
      />,
    );
    await user.click(screen.getByLabelText(/^Ví MoMo$/));
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith('MOMO');
  });

  it('does not double-fire onChange when re-clicking the already-selected option', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <PaymentMethodSelector
        options={VN_OPTIONS}
        selectedMethod="MOMO"
        onChange={onChange}
      />,
    );
    await user.click(screen.getByLabelText(/^Ví MoMo$/));
    expect(onChange).not.toHaveBeenCalled();
  });

  it('disabled option does NOT fire onChange when clicked', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const optionsWithDisabled: PaymentMethodOption[] = VN_OPTIONS.map((o) =>
      o.id === 'CASH' ? { ...o, disabled: true } : o,
    );
    render(<PaymentMethodSelector options={optionsWithDisabled} onChange={onChange} />);

    const cashRadio = screen.getByLabelText(/^Tiền mặt tại trung tâm$/) as HTMLInputElement;
    expect(cashRadio).toBeDisabled();

    // userEvent.click on a disabled input is a no-op per HTML semantics.
    await user.click(cashRadio);
    expect(onChange).not.toHaveBeenCalled();
  });

  it('disabled option is skipped by keyboard arrow-key cycling', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    // Disable ZaloPay (3rd item). Tabbing in + arrow should jump VNPAY → MOMO → BANK.
    const optionsWithDisabled: PaymentMethodOption[] = VN_OPTIONS.map((o) =>
      o.id === 'ZALOPAY' ? { ...o, disabled: true } : o,
    );
    render(
      <PaymentMethodSelector
        options={optionsWithDisabled}
        selectedMethod="VNPAY"
        onChange={onChange}
      />,
    );

    // Focus first radio (VNPAY) and arrow-down — native radio group jumps over disabled.
    const vnpay = screen.getByLabelText(/^VNPay$/);
    vnpay.focus();
    await user.keyboard('{ArrowDown}');
    expect(onChange).toHaveBeenLastCalledWith('MOMO');

    await user.keyboard('{ArrowDown}');
    // ZALOPAY is disabled → next reachable is BANK.
    expect(onChange).toHaveBeenLastCalledWith('BANK');
  });

  it('renders popular and redirect badges when option flags are set', () => {
    render(<PaymentMethodSelector options={VN_OPTIONS} onChange={() => {}} />);
    // MoMo flagged popular, VNPay flagged redirect — both are user-visible chips.
    // We assert via testid instead of text to avoid clashing with descriptive
    // body text that legitimately contains the same VN words ("chuyển hướng
    // tới VNPay" appears in the description prose for VNPay).
    expect(
      screen.getByTestId('payment-method-badge-popular-MOMO'),
    ).toHaveTextContent(/phổ biến/i);
    expect(
      screen.getByTestId('payment-method-badge-redirect-VNPAY'),
    ).toHaveTextContent(/chuyển hướng/i);
  });

  it('respects custom name and ariaLabel props', () => {
    render(
      <PaymentMethodSelector
        options={VN_OPTIONS}
        name="custom-payment"
        ariaLabel="Chọn cách thanh toán"
        onChange={() => {}}
      />,
    );
    const group = screen.getByRole('radiogroup', { name: /chọn cách thanh toán/i });
    expect(group).toBeInTheDocument();
    const radios = screen.getAllByRole('radio') as HTMLInputElement[];
    radios.forEach((r) => expect(r.name).toBe('custom-payment'));
  });
});
