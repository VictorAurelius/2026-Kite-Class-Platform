export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';
export type PaymentMethod = 'VIETQR' | 'BANK_TRANSFER' | 'MOMO';

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
