export interface Payment {
  id: number;
  paymentNumber: string;
  transactionId: string;
  invoiceId: number;
  installmentId?: number;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  paymentUrl?: string; // For online payments
  qrCodeUrl?: string; // Future: VietQR
  receiptNumber?: string;
  receiptUrl?: string;
  initiatedAt: string;
  expiresAt?: string;
  completedAt?: string;
  failureReason?: string;
}

export enum PaymentMethod {
  CASH = 'CASH',
  BANK_TRANSFER = 'BANK_TRANSFER',
  MOMO = 'MOMO',
  VNPAY = 'VNPAY',
  ZALOPAY = 'ZALOPAY',
  CREDIT_CARD = 'CREDIT_CARD',
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED',
}

export interface CreatePaymentRequest {
  invoiceId: number;
  amount: number;
  paymentMethod: PaymentMethod;
  ipAddress?: string;
}

export interface PaymentSearchParams {
  status?: PaymentStatus;
  page?: number;
  size?: number;
}
