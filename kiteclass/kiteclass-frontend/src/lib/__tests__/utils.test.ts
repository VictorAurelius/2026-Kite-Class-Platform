/**
 * Utility functions tests
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { describe, it, expect } from 'vitest';
import { formatDate, formatDateTime, cn } from '../utils';

describe('Utils', () => {
  describe('cn (className merger)', () => {
    it('should merge class names correctly', () => {
      expect(cn('foo', 'bar')).toBe('foo bar');
    });

    it('should handle conditional classes', () => {
      expect(cn('foo', false && 'bar', 'baz')).toBe('foo baz');
    });

    it('should merge Tailwind classes without conflicts', () => {
      // twMerge should handle conflicting classes
      expect(cn('p-4', 'p-2')).toBe('p-2');
    });

    it('should handle empty inputs', () => {
      expect(cn()).toBe('');
    });

    it('should handle null and undefined', () => {
      expect(cn('foo', null, 'bar', undefined, 'baz')).toBe('foo bar baz');
    });

    it('should handle arrays', () => {
      expect(cn(['foo', 'bar'], 'baz')).toBe('foo bar baz');
    });

    it('should handle objects', () => {
      expect(cn({ foo: true, bar: false, baz: true })).toBe('foo baz');
    });
  });

  describe('formatDate', () => {
    it('should format ISO date to Vietnamese locale (DD/MM/YYYY)', () => {
      const result = formatDate('2024-01-15');
      expect(result).toBe('15/01/2024');
    });

    it('should format ISO datetime to Vietnamese date (DD/MM/YYYY)', () => {
      const result = formatDate('2024-03-20T10:30:00Z');
      expect(result).toBe('20/03/2024');
    });

    it('should handle different date formats', () => {
      const result = formatDate('2024-12-31T23:59:59Z');
      expect(result).toBe('31/12/2024');
    });

    it('should handle invalid date gracefully', () => {
      const result = formatDate('invalid-date');
      expect(result).toBe('—');
    });
  });

  describe('formatDateTime', () => {
    it('should format ISO datetime to Vietnamese locale with time', () => {
      const result = formatDateTime('2024-01-15T10:30:00Z');
      // Vietnamese locale uses 24-hour format with time before date
      expect(result).toMatch(/10:30.*15\/01\/2024/);
    });

    it('should include hours and minutes', () => {
      const result = formatDateTime('2024-03-20T14:45:00Z');
      expect(result).toMatch(/14:45.*20\/03\/2024/);
    });

    it('should handle midnight correctly', () => {
      const result = formatDateTime('2024-06-01T00:00:00Z');
      expect(result).toMatch(/00:00.*01\/06\/2024/);
    });

    it('should handle invalid datetime gracefully', () => {
      const result = formatDateTime('invalid-datetime');
      expect(result).toBe('—');
    });
  });
});
