export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';
export type PaymentMethod = 'VIETQR' | 'BANK_TRANSFER' | 'MOMO';

export interface Payment {
  id: number;
  paymentNumber: string;
  invoiceId: number;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  transactionId: string;
  gatewayTransactionId: string | null;
  qrCodeUrl: string | null;
  createdAt: string;
  completedAt: string | null;
}
