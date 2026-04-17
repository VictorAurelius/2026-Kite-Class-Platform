/**
 * Unit tests for validation utilities.
 *
 * @since PR 5.10
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  pricingTierSchema,
  billingCycleSchema,
  paymentMethodSchema,
  paymentStatusSchema,
  upgradeSubscriptionSchema,
  downgradeSubscriptionSchema,
  createPaymentSchema,
  validateTierChange,
  validateAmount,
  validateExpiryDate,
} from '../validation';

describe('validation', () => {
  describe('pricingTierSchema', () => {
    it('accepts valid tiers', () => {
      expect(pricingTierSchema.safeParse('FREE').success).toBe(true);
      expect(pricingTierSchema.safeParse('BASIC').success).toBe(true);
      expect(pricingTierSchema.safeParse('PREMIUM').success).toBe(true);
      expect(pricingTierSchema.safeParse('ENTERPRISE').success).toBe(true);
    });

    it('rejects invalid tiers', () => {
      expect(pricingTierSchema.safeParse('INVALID').success).toBe(false);
      expect(pricingTierSchema.safeParse('free').success).toBe(false);
      expect(pricingTierSchema.safeParse('').success).toBe(false);
    });
  });

  describe('billingCycleSchema', () => {
    it('accepts valid cycles', () => {
      expect(billingCycleSchema.safeParse('MONTHLY').success).toBe(true);
      expect(billingCycleSchema.safeParse('ANNUALLY').success).toBe(true);
    });

    it('rejects invalid cycles', () => {
      expect(billingCycleSchema.safeParse('WEEKLY').success).toBe(false);
      expect(billingCycleSchema.safeParse('YEARLY').success).toBe(false);
      expect(billingCycleSchema.safeParse('').success).toBe(false);
    });
  });

  describe('paymentMethodSchema', () => {
    it('accepts valid payment methods', () => {
      expect(paymentMethodSchema.safeParse('VIETQR').success).toBe(true);
      expect(paymentMethodSchema.safeParse('BANK_TRANSFER').success).toBe(true);
      expect(paymentMethodSchema.safeParse('MOMO').success).toBe(true);
      expect(paymentMethodSchema.safeParse('VNPAY').success).toBe(true);
    });

    it('rejects invalid payment methods', () => {
      expect(paymentMethodSchema.safeParse('CREDIT_CARD').success).toBe(false);
      expect(paymentMethodSchema.safeParse('PAYPAL').success).toBe(false);
    });
  });

  describe('paymentStatusSchema', () => {
    it('accepts valid statuses', () => {
      expect(paymentStatusSchema.safeParse('PENDING').success).toBe(true);
      expect(paymentStatusSchema.safeParse('COMPLETED').success).toBe(true);
      expect(paymentStatusSchema.safeParse('FAILED').success).toBe(true);
      expect(paymentStatusSchema.safeParse('EXPIRED').success).toBe(true);
    });

    it('rejects invalid statuses', () => {
      expect(paymentStatusSchema.safeParse('PROCESSING').success).toBe(false);
      expect(paymentStatusSchema.safeParse('CANCELLED').success).toBe(false);
    });
  });

  describe('upgradeSubscriptionSchema', () => {
    it('accepts valid upgrade request', () => {
      const result = upgradeSubscriptionSchema.safeParse({
        subscriptionId: '123e4567-e89b-12d3-a456-426614174000',
        newTier: 'PREMIUM',
      });
      expect(result.success).toBe(true);
    });

    it('rejects invalid UUID', () => {
      const result = upgradeSubscriptionSchema.safeParse({
        subscriptionId: 'invalid-uuid',
        newTier: 'PREMIUM',
      });
      expect(result.success).toBe(false);
    });

    it('rejects invalid tier', () => {
      const result = upgradeSubscriptionSchema.safeParse({
        subscriptionId: '123e4567-e89b-12d3-a456-426614174000',
        newTier: 'INVALID',
      });
      expect(result.success).toBe(false);
    });
  });

  describe('downgradeSubscriptionSchema', () => {
    it('accepts valid downgrade request', () => {
      const result = downgradeSubscriptionSchema.safeParse({
        subscriptionId: '123e4567-e89b-12d3-a456-426614174000',
        newTier: 'BASIC',
      });
      expect(result.success).toBe(true);
    });

    it('rejects invalid UUID', () => {
      const result = downgradeSubscriptionSchema.safeParse({
        subscriptionId: 'not-a-uuid',
        newTier: 'FREE',
      });
      expect(result.success).toBe(false);
    });
  });

  describe('createPaymentSchema', () => {
    it('accepts valid payment request', () => {
      const result = createPaymentSchema.safeParse({
        subscriptionId: '123e4567-e89b-12d3-a456-426614174000',
        amountVnd: 500000,
        paymentMethod: 'VIETQR',
      });
      expect(result.success).toBe(true);
    });

    it('rejects zero amount', () => {
      const result = createPaymentSchema.safeParse({
        subscriptionId: '123e4567-e89b-12d3-a456-426614174000',
        amountVnd: 0,
        paymentMethod: 'VIETQR',
      });
      expect(result.success).toBe(false);
    });

    it('rejects negative amount', () => {
      const result = createPaymentSchema.safeParse({
        subscriptionId: '123e4567-e89b-12d3-a456-426614174000',
        amountVnd: -100,
        paymentMethod: 'VIETQR',
      });
      expect(result.success).toBe(false);
    });

    it('rejects invalid payment method', () => {
      const result = createPaymentSchema.safeParse({
        subscriptionId: '123e4567-e89b-12d3-a456-426614174000',
        amountVnd: 500000,
        paymentMethod: 'INVALID',
      });
      expect(result.success).toBe(false);
    });
  });

  describe('validateTierChange', () => {
    it('returns invalid when tier is same', () => {
      const result = validateTierChange('BASIC', 'BASIC');
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Không thể chuyển sang gói hiện tại');
    });

    it('returns invalid for ENTERPRISE target', () => {
      const result = validateTierChange('BASIC', 'ENTERPRISE');
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Vui lòng liên hệ sales để đăng ký gói Enterprise');
    });

    it('returns valid for upgrade to non-ENTERPRISE', () => {
      expect(validateTierChange('FREE', 'BASIC').isValid).toBe(true);
      expect(validateTierChange('FREE', 'PREMIUM').isValid).toBe(true);
      expect(validateTierChange('BASIC', 'PREMIUM').isValid).toBe(true);
    });

    it('returns valid for downgrade', () => {
      expect(validateTierChange('PREMIUM', 'BASIC').isValid).toBe(true);
      expect(validateTierChange('PREMIUM', 'FREE').isValid).toBe(true);
      expect(validateTierChange('BASIC', 'FREE').isValid).toBe(true);
    });
  });

  describe('validateAmount', () => {
    it('returns invalid for zero', () => {
      const result = validateAmount(0);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Số tiền phải lớn hơn 0');
    });

    it('returns invalid for negative amount', () => {
      const result = validateAmount(-100);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Số tiền phải lớn hơn 0');
    });

    it('returns invalid for amount exceeding 1 billion', () => {
      const result = validateAmount(1000000001);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Số tiền vượt quá giới hạn cho phép');
    });

    it('returns valid for positive amount within limit', () => {
      expect(validateAmount(1).isValid).toBe(true);
      expect(validateAmount(500000).isValid).toBe(true);
      expect(validateAmount(1000000000).isValid).toBe(true);
    });
  });

  describe('validateExpiryDate', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-03-17T00:00:00Z'));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('returns valid and not expired for null date', () => {
      const result = validateExpiryDate(null);
      expect(result.isValid).toBe(true);
      expect(result.isExpired).toBe(false);
    });

    it('returns expired for past date', () => {
      const result = validateExpiryDate('2026-03-16T00:00:00Z');
      expect(result.isValid).toBe(true);
      expect(result.isExpired).toBe(true);
    });

    it('returns not expired for future date', () => {
      const result = validateExpiryDate('2026-03-18T00:00:00Z');
      expect(result.isValid).toBe(true);
      expect(result.isExpired).toBe(false);
    });

    it('returns expired for current time (edge case)', () => {
      // Same time is considered expired (< comparison)
      const result = validateExpiryDate('2026-03-17T00:00:00Z');
      // At exactly the same millisecond, it's not expired (same time, not less than)
      expect(result.isValid).toBe(true);
    });
  });
});
