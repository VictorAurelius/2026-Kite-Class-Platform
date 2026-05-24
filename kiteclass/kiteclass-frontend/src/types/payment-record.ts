/**
 * PaymentRecord domain types — Wave beta-readiness-4 Bucket C / GAP-292b.
 *
 * Matches backend PaymentRecord entity + RecordPaymentRequest/PaymentRecordResponse DTOs.
 * Distinct from gateway Payment (VNPAY/MoMo redirect handled by ../payment.ts).
 */

/**
 * Manual payment methods recorded by teachers/admins at trung tâm.
 * VN edu market: CASH > BANK_TRANSFER > VIETQR > MOMO.
 */
export enum PaymentRecordMethod {
  /** Tiền mặt — most common at TT dạy thêm. */
  CASH = 'CASH',
  /** Chuyển khoản ngân hàng (Vietcombank, Techcombank, MB, ACB). */
  BANK_TRANSFER = 'BANK_TRANSFER',
  /** VietQR scan-to-pay — bank-agnostic QR code. */
  VIETQR = 'VIETQR',
  /** Ví MoMo — digital wallet. */
  MOMO = 'MOMO',
}

export const PAYMENT_RECORD_METHOD_LABELS: Record<PaymentRecordMethod, string> = {
  [PaymentRecordMethod.CASH]: 'Tiền mặt',
  [PaymentRecordMethod.BANK_TRANSFER]: 'Chuyển khoản ngân hàng',
  [PaymentRecordMethod.VIETQR]: 'VietQR (quét mã)',
  [PaymentRecordMethod.MOMO]: 'Ví MoMo',
};

export interface RecordPaymentRequest {
  method: PaymentRecordMethod;
  amount: number;
  /** ISO 8601 timestamp; defaults to now() server-side if omitted. */
  paidAt?: string;
  /** Optional teacher note, max 500 chars. */
  note?: string;
}

export interface PaymentRecord {
  id: number;
  invoiceId: number;
  method: PaymentRecordMethod;
  amount: number;
  paidAt: string;
  note?: string;
  recordedBy: number;
  createdAt: string;
}
