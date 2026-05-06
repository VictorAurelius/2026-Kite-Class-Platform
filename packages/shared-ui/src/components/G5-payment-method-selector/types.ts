/**
 * Type definitions for PaymentMethodSelector — VN multi-gateway payment picker.
 *
 * Per `documents/02-architecture/design-system/ui_kits/components/G5-payment-method-selector/spec.md`,
 * VN payment is wallet-first, not card-first. The five gateway IDs below are
 * verbatim from the spec.
 */

/**
 * Vietnamese payment gateway identifiers.
 *
 * - `VNPAY` — bank-card / QR via VNPay redirect (most-trusted institutional rail)
 * - `MOMO` — MoMo wallet QR (highest market-share consumer wallet ~35%)
 * - `ZALOPAY` — ZaloPay wallet QR
 * - `BANK` — direct bank transfer (Vietcombank / Techcombank / BIDV / ACB)
 * - `CASH` — pay at center (offline-first centers; admin confirms receipt)
 *
 * Note: the dossier's §G5 spec lists `QR` as a state (`qr-displayed`) reached
 * via MoMo/ZaloPay selection — NOT as a separate method. We preserve spec verbatim.
 */
export type PaymentMethod = 'VNPAY' | 'MOMO' | 'ZALOPAY' | 'BANK' | 'CASH';

/**
 * One option in the radio-group method picker.
 *
 * - `id` — stable enum value matching `PaymentMethod`.
 * - `label` — Vietnamese display label, copy-pasted verbatim from
 *   `ui_kits/components/G5-payment-method-selector/default.html`.
 * - `description` — optional sub-text shown under label (e.g. "Quét mã QR bằng app MoMo").
 * - `disabled` — when true, the option renders muted + non-interactive and is
 *   skipped by keyboard arrow-key navigation. Rationale: some tenants disable
 *   CASH for online-only flows, or temporarily disable a gateway during outage.
 * - `popular` — optional badge marker (e.g. "Phổ biến" on MoMo).
 * - `redirect` — optional badge marker (e.g. "Chuyển hướng" on VNPay).
 */
export type PaymentMethodOption = {
  id: PaymentMethod;
  label: string;
  description?: string;
  disabled?: boolean;
  popular?: boolean;
  redirect?: boolean;
};

/**
 * Props for `<PaymentMethodSelector>`.
 *
 * Wave 27 Bucket C scope = the `method-selecting` state of the spec's state
 * machine: render radio-group, capture pick, fire onChange. Other states
 * (loading-qr, qr-displayed, expired, success, failure-retry) defer to
 * follow-up bucket — see spec.md state machine for full lifecycle.
 *
 * - `options` — ordered list of methods to display (typical order:
 *   VNPay, MoMo, ZaloPay, Bank, Cash per `dossier/02-vietnamese-ux-musts.md` §4).
 * - `selectedMethod` — currently picked id (controlled). `undefined` = none picked yet.
 * - `onChange` — fired when user picks a non-disabled option.
 * - `name` — radio-group form name (default `'payment-method'`).
 * - `ariaLabel` — accessible group label (default `'Phương thức thanh toán'`).
 * - `id` — optional id for the group container (auto-generated otherwise).
 */
export type PaymentMethodSelectorProps = {
  options: PaymentMethodOption[];
  selectedMethod?: PaymentMethod;
  onChange: (method: PaymentMethod) => void;
  name?: string;
  ariaLabel?: string;
  id?: string;
};
