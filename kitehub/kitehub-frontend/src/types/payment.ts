export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';

/**
 * KiteHub subscription billing payment methods.
 *
 * Canonical source: `com.kitehub.platform.domain.enums.PaymentMethod` (Java).
 * GAP-739 (Wave beta-readiness-8 Bucket C): synced FE union ↔ BE enum, eliminated drift.
 *
 * Note: KiteClass (school payment domain) uses a SEPARATE PaymentMethod enum
 * defined in `kiteclass-frontend/src/types/payment.ts` — domain boundary is intentional
 * (subscription billing vs school invoice are distinct business contexts).
 */
export type PaymentMethod =
  | 'VIETQR'
  | 'MOMO'
  | 'VNPAY'
  | 'BANK_TRANSFER'
  | 'MANUAL';

export interface Payment {
  id: string;                     // UUID
  subscriptionId: string;         // UUID
  amountVnd: number;
  currency: string;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  qrCodeUrl: string | null;
  transactionId: string | null;
  bankCode: string | null;
  accountNumber: string | null;
  accountName: string | null;
  paymentContent: string | null;
  // GAP-1472: BE PaymentResponse exposes txnRef (gateway reference). Nullable —
  // not every payment method populates it.
  txnRef: string | null;
  paidAt: string | null;
  expiresAt: string | null;       // QR code expiry
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentRequest {
  subscriptionId: string;
  amountVnd: number;
  paymentMethod: PaymentMethod;
}

export interface QRCodeResponse {
  qrCodeUrl: string;
  expiresAt: string;
}
