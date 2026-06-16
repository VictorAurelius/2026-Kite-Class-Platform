/**
 * Unit tests for utility functions.
 *
 * @since PR 5.9
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { cn, formatDate, formatDateTime, formatCurrency, getDaysRemaining } from '../utils';

describe('utils', () => {
  describe('cn (classnames merger)', () => {
    it('merges multiple class names', () => {
      expect(cn('foo', 'bar')).toBe('foo bar');
    });

    it('handles conditional classes', () => {
      expect(cn('base', true && 'active', false && 'hidden')).toBe('base active');
    });

    it('merges tailwind classes correctly', () => {
      expect(cn('px-2 py-1', 'px-4')).toBe('py-1 px-4');
    });

    it('handles undefined and null', () => {
      expect(cn('base', undefined, null, 'end')).toBe('base end');
    });

    it('handles empty inputs', () => {
      expect(cn()).toBe('');
    });

    it('handles array of classes', () => {
      expect(cn(['foo', 'bar'])).toBe('foo bar');
    });
  });

  describe('formatDate', () => {
    it('formats ISO date string to Vietnamese format', () => {
      const result = formatDate('2026-03-15T00:00:00.000Z');
      expect(result).toBe('15/03/2026');
    });

    it('formats date without time', () => {
      const result = formatDate('2026-01-01');
      expect(result).toBe('01/01/2026');
    });

    it('returns placeholder for invalid input', () => {
      expect(formatDate('not-a-date')).toBe('—');
    });

    it('returns placeholder for empty string', () => {
      expect(formatDate('')).toBe('—');
    });

    it('handles leap year date', () => {
      const result = formatDate('2024-02-29');
      expect(result).toBe('29/02/2024');
    });
  });

  describe('formatDateTime', () => {
    it('formats ISO datetime to Vietnamese format with time', () => {
      const result = formatDateTime('2026-03-15T14:30:00.000Z');
      // Vietnamese locale formats as "HH:mm DD/MM/YYYY"
      expect(result).toBe('14:30 15/03/2026');
    });

    it('formats midnight correctly', () => {
      const result = formatDateTime('2026-03-15T00:00:00.000Z');
      expect(result).toBe('00:00 15/03/2026');
    });

    it('returns placeholder for invalid input', () => {
      expect(formatDateTime('invalid')).toBe('—');
    });

    it('handles end of day time', () => {
      const result = formatDateTime('2026-03-15T23:59:00.000Z');
      expect(result).toBe('23:59 15/03/2026');
    });
  });

  describe('formatCurrency', () => {
    it('formats positive amount in VND', () => {
      const result = formatCurrency(1500000);
      // Vietnamese locale formats with dot separator
      expect(result).toMatch(/1[.,]500[.,]000/);
      expect(result).toContain('₫');
    });

    it('formats zero amount', () => {
      const result = formatCurrency(0);
      expect(result).toMatch(/0/);
      expect(result).toContain('₫');
    });

    it('formats large amount correctly', () => {
      const result = formatCurrency(999999999);
      expect(result).toMatch(/999[.,]999[.,]999/);
    });

    it('handles decimal amounts (rounds to whole number)', () => {
      const result = formatCurrency(1500000.5);
      // VND doesn't have decimal places
      expect(result).toMatch(/1[.,]500[.,]00[01]/);
    });
  });

  describe('getDaysRemaining', () => {
    beforeEach(() => {
      // Mock current date to 2026-03-15
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-03-15T12:00:00.000Z'));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('returns positive days for future date', () => {
      const result = getDaysRemaining('2026-03-20T00:00:00.000Z');
      expect(result).toBe(5);
    });

    it('returns negative days for past date', () => {
      const result = getDaysRemaining('2026-03-10T00:00:00.000Z');
      expect(result).toBe(-5);
    });

    it('returns 0 or 1 for same day', () => {
      const result = getDaysRemaining('2026-03-15T23:59:59.000Z');
      expect(result).toBeGreaterThanOrEqual(0);
      expect(result).toBeLessThanOrEqual(1);
    });

    it('handles date string without time', () => {
      const result = getDaysRemaining('2026-03-25');
      expect(result).toBeGreaterThan(0);
    });

    it('returns large number for far future date', () => {
      const result = getDaysRemaining('2027-03-15T00:00:00.000Z');
      expect(result).toBe(365);
    });
  });
});
