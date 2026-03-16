/**
 * Unit tests for pricing utilities.
 *
 * @since PR 5.9
 */

import { describe, it, expect } from 'vitest';
import {
  PLAN_DETAILS,
  getTierRank,
  isUpgrade,
  isDowngrade,
  calculateProration,
  formatPrice,
} from '../pricing';

describe('pricing', () => {
  describe('PLAN_DETAILS', () => {
    it('has all four tiers defined', () => {
      expect(PLAN_DETAILS).toHaveProperty('FREE');
      expect(PLAN_DETAILS).toHaveProperty('BASIC');
      expect(PLAN_DETAILS).toHaveProperty('PREMIUM');
      expect(PLAN_DETAILS).toHaveProperty('ENTERPRISE');
    });

    it('FREE tier has zero prices', () => {
      expect(PLAN_DETAILS.FREE.monthlyPrice).toBe(0);
      expect(PLAN_DETAILS.FREE.yearlyPrice).toBe(0);
    });

    it('BASIC tier has correct pricing', () => {
      expect(PLAN_DETAILS.BASIC.monthlyPrice).toBe(500000);
      expect(PLAN_DETAILS.BASIC.yearlyPrice).toBe(5400000);
    });

    it('PREMIUM tier has correct pricing', () => {
      expect(PLAN_DETAILS.PREMIUM.monthlyPrice).toBe(1500000);
      expect(PLAN_DETAILS.PREMIUM.yearlyPrice).toBe(16200000);
    });

    it('ENTERPRISE tier has custom pricing (-1)', () => {
      expect(PLAN_DETAILS.ENTERPRISE.monthlyPrice).toBe(-1);
      expect(PLAN_DETAILS.ENTERPRISE.yearlyPrice).toBe(-1);
    });

    it('each tier has required properties', () => {
      const requiredProps = ['tier', 'name', 'monthlyPrice', 'yearlyPrice', 'maxStudents', 'features'];
      Object.values(PLAN_DETAILS).forEach(plan => {
        requiredProps.forEach(prop => {
          expect(plan).toHaveProperty(prop);
        });
      });
    });
  });

  describe('getTierRank', () => {
    it('returns 0 for FREE', () => {
      expect(getTierRank('FREE')).toBe(0);
    });

    it('returns 1 for BASIC', () => {
      expect(getTierRank('BASIC')).toBe(1);
    });

    it('returns 2 for PREMIUM', () => {
      expect(getTierRank('PREMIUM')).toBe(2);
    });

    it('returns 3 for ENTERPRISE', () => {
      expect(getTierRank('ENTERPRISE')).toBe(3);
    });

    it('ranks are in ascending order', () => {
      expect(getTierRank('FREE')).toBeLessThan(getTierRank('BASIC'));
      expect(getTierRank('BASIC')).toBeLessThan(getTierRank('PREMIUM'));
      expect(getTierRank('PREMIUM')).toBeLessThan(getTierRank('ENTERPRISE'));
    });
  });

  describe('isUpgrade', () => {
    it('returns true when upgrading from FREE to BASIC', () => {
      expect(isUpgrade('FREE', 'BASIC')).toBe(true);
    });

    it('returns true when upgrading from BASIC to PREMIUM', () => {
      expect(isUpgrade('BASIC', 'PREMIUM')).toBe(true);
    });

    it('returns true when upgrading from FREE to ENTERPRISE', () => {
      expect(isUpgrade('FREE', 'ENTERPRISE')).toBe(true);
    });

    it('returns false when same tier', () => {
      expect(isUpgrade('BASIC', 'BASIC')).toBe(false);
    });

    it('returns false when downgrading', () => {
      expect(isUpgrade('PREMIUM', 'BASIC')).toBe(false);
    });
  });

  describe('isDowngrade', () => {
    it('returns true when downgrading from PREMIUM to BASIC', () => {
      expect(isDowngrade('PREMIUM', 'BASIC')).toBe(true);
    });

    it('returns true when downgrading from BASIC to FREE', () => {
      expect(isDowngrade('BASIC', 'FREE')).toBe(true);
    });

    it('returns true when downgrading from ENTERPRISE to FREE', () => {
      expect(isDowngrade('ENTERPRISE', 'FREE')).toBe(true);
    });

    it('returns false when same tier', () => {
      expect(isDowngrade('BASIC', 'BASIC')).toBe(false);
    });

    it('returns false when upgrading', () => {
      expect(isDowngrade('BASIC', 'PREMIUM')).toBe(false);
    });
  });

  describe('calculateProration', () => {
    it('calculates correct proration for upgrade with 15 days remaining', () => {
      // FREE (0) to BASIC (500,000) monthly, 15 days remaining
      // Daily diff = 500,000 / 30 = 16,666.67
      // Proration = 16,666.67 * 15 = 250,000
      const result = calculateProration('FREE', 'BASIC', 15, 'MONTHLY');
      expect(result).toBe(250000);
    });

    it('calculates correct proration for BASIC to PREMIUM', () => {
      // BASIC (500,000) to PREMIUM (1,500,000) monthly, 10 days remaining
      // Daily diff = (1,500,000 - 500,000) / 30 = 33,333.33
      // Proration = 33,333.33 * 10 = 333,333
      const result = calculateProration('BASIC', 'PREMIUM', 10, 'MONTHLY');
      expect(result).toBe(333333);
    });

    it('returns 0 for downgrade (negative proration capped at 0)', () => {
      const result = calculateProration('PREMIUM', 'BASIC', 15, 'MONTHLY');
      expect(result).toBe(0);
    });

    it('returns 0 for same tier', () => {
      const result = calculateProration('BASIC', 'BASIC', 15, 'MONTHLY');
      expect(result).toBe(0);
    });

    it('calculates yearly proration correctly', () => {
      // FREE to BASIC yearly: 5,400,000 / 365 * 100 days
      const result = calculateProration('FREE', 'BASIC', 100, 'YEARLY');
      const expected = Math.round((5400000 / 365) * 100);
      expect(result).toBe(expected);
    });

    it('handles 0 days remaining', () => {
      const result = calculateProration('FREE', 'BASIC', 0, 'MONTHLY');
      expect(result).toBe(0);
    });

    it('handles full cycle (30 days monthly)', () => {
      const result = calculateProration('FREE', 'BASIC', 30, 'MONTHLY');
      expect(result).toBe(500000);
    });
  });

  describe('formatPrice', () => {
    it('formats monthly price with suffix', () => {
      const result = formatPrice(500000, 'MONTHLY');
      expect(result).toMatch(/500[.,]000/);
      expect(result).toContain('/tháng');
    });

    it('formats yearly price with suffix', () => {
      const result = formatPrice(5400000, 'YEARLY');
      expect(result).toMatch(/5[.,]400[.,]000/);
      expect(result).toContain('/năm');
    });

    it('returns "Liên hệ" for -1 (custom pricing)', () => {
      expect(formatPrice(-1, 'MONTHLY')).toBe('Liên hệ');
      expect(formatPrice(-1, 'YEARLY')).toBe('Liên hệ');
    });

    it('returns "Miễn phí" for 0', () => {
      expect(formatPrice(0, 'MONTHLY')).toBe('Miễn phí');
      expect(formatPrice(0, 'YEARLY')).toBe('Miễn phí');
    });

    it('defaults to MONTHLY if cycle not specified', () => {
      const result = formatPrice(500000);
      expect(result).toContain('/tháng');
    });

    it('formats large numbers correctly', () => {
      const result = formatPrice(16200000, 'YEARLY');
      expect(result).toMatch(/16[.,]200[.,]000/);
    });
  });
});
