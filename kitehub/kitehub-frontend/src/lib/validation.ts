import { z } from 'zod';

// Subscription tier validation
export const pricingTierSchema = z.enum(['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE']);

// Billing cycle validation
export const billingCycleSchema = z.enum(['MONTHLY', 'ANNUALLY']);

// Payment method validation
export const paymentMethodSchema = z.enum(['VIETQR', 'BANK_TRANSFER', 'MOMO', 'VNPAY']);

// Payment status validation
export const paymentStatusSchema = z.enum(['PENDING', 'COMPLETED', 'FAILED', 'EXPIRED']);

// Upgrade subscription request schema
export const upgradeSubscriptionSchema = z.object({
  subscriptionId: z.string().uuid('ID gói đăng ký không hợp lệ'),
  newTier: pricingTierSchema,
});

// Downgrade subscription request schema
export const downgradeSubscriptionSchema = z.object({
  subscriptionId: z.string().uuid('ID gói đăng ký không hợp lệ'),
  newTier: pricingTierSchema,
});

// Create payment request schema
export const createPaymentSchema = z.object({
  subscriptionId: z.string().uuid('ID gói đăng ký không hợp lệ'),
  amountVnd: z.number().positive('Số tiền phải lớn hơn 0'),
  paymentMethod: paymentMethodSchema,
});

// Tier change validation
export function validateTierChange(currentTier: string, newTier: string): {
  isValid: boolean;
  error?: string;
} {
  if (currentTier === newTier) {
    return {
      isValid: false,
      error: 'Không thể chuyển sang gói hiện tại',
    };
  }

  if (newTier === 'ENTERPRISE') {
    return {
      isValid: false,
      error: 'Vui lòng liên hệ sales để đăng ký gói Enterprise',
    };
  }

  return { isValid: true };
}

// Amount validation
export function validateAmount(amount: number): {
  isValid: boolean;
  error?: string;
} {
  if (amount <= 0) {
    return {
      isValid: false,
      error: 'Số tiền phải lớn hơn 0',
    };
  }

  if (amount > 1000000000) {
    // 1 billion VND
    return {
      isValid: false,
      error: 'Số tiền vượt quá giới hạn cho phép',
    };
  }

  return { isValid: true };
}

// Date validation
export function validateExpiryDate(expiryDate: string | null): {
  isValid: boolean;
  isExpired: boolean;
} {
  if (!expiryDate) {
    return { isValid: true, isExpired: false };
  }

  const expiry = new Date(expiryDate).getTime();
  const now = new Date().getTime();

  return {
    isValid: true,
    isExpired: expiry < now,
  };
}
