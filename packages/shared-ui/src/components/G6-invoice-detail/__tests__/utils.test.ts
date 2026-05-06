/**
 * Pure-logic tests for VN currency + tax formatting helpers used by G6 Invoice Detail.
 *
 * Format contract (per `ui_kits/components/G6-invoice-detail/spec.md` + HTML protos):
 *  - Currency uses period as thousands separator and lowercase `đ` suffix:
 *      1500000  → "1.500.000đ"
 *      0        → "0đ"
 *  - Negative amounts use the minus sign U+2212 to match the HTML protos:
 *      -200000  → "−200.000đ"
 *  - Decimals are NOT expected for VND; we round to nearest integer just in case.
 *  - VAT rate is exposed as either decimal (0.08) or whole number (8); formatter
 *    accepts both and emits `8%` / `10%` / `0%`.
 */

import { describe, expect, it } from 'vitest';
import { formatVNCurrency, formatVNTax } from '../utils';

describe('formatVNCurrency', () => {
  it('formats zero as "0đ"', () => {
    expect(formatVNCurrency(0)).toBe('0đ');
  });

  it('formats 1.500.000 with period separators', () => {
    expect(formatVNCurrency(1500000)).toBe('1.500.000đ');
  });

  it('formats large numbers (15B+) with multiple period separators', () => {
    expect(formatVNCurrency(15000000000)).toBe('15.000.000.000đ');
  });

  it('formats small amounts without separators', () => {
    expect(formatVNCurrency(500)).toBe('500đ');
    expect(formatVNCurrency(50000)).toBe('50.000đ');
  });

  it('formats negative amounts with U+2212 minus sign', () => {
    // Discount rows in spec render "−200.000đ" with Unicode minus.
    expect(formatVNCurrency(-200000)).toBe('−200.000đ');
    expect(formatVNCurrency(-1)).toBe('−1đ');
  });

  it('rounds non-integer amounts to the nearest VND', () => {
    // Defense-in-depth: VND has no fractional unit; if any caller passes a float,
    // we don't want "1.500.000,5đ" leaking into the UI.
    expect(formatVNCurrency(1500000.4)).toBe('1.500.000đ');
    expect(formatVNCurrency(1500000.6)).toBe('1.500.001đ');
    // JS Math.round rounds half toward +∞: Math.round(-200000.5) === -200000.
    expect(formatVNCurrency(-200000.5)).toBe('−200.000đ');
    expect(formatVNCurrency(-200000.6)).toBe('−200.001đ');
  });

  it('handles 4.500.000đ (spec example)', () => {
    expect(formatVNCurrency(4500000)).toBe('4.500.000đ');
  });

  it('handles 4.700.000đ subtotal example', () => {
    expect(formatVNCurrency(4700000)).toBe('4.700.000đ');
  });

  it('does not throw on NaN — returns "0đ" defensively', () => {
    expect(formatVNCurrency(Number.NaN)).toBe('0đ');
  });
});

describe('formatVNTax', () => {
  it('formats decimal rate 0.08 as "8%"', () => {
    expect(formatVNTax(0.08)).toBe('8%');
  });

  it('formats decimal rate 0.1 as "10%"', () => {
    expect(formatVNTax(0.1)).toBe('10%');
  });

  it('formats integer rate 8 as "8%" (already-percent input)', () => {
    expect(formatVNTax(8)).toBe('8%');
  });

  it('formats integer rate 10 as "10%"', () => {
    expect(formatVNTax(10)).toBe('10%');
  });

  it('formats 0 as "0%"', () => {
    expect(formatVNTax(0)).toBe('0%');
  });

  it('rounds 0.083 to "8%"', () => {
    expect(formatVNTax(0.083)).toBe('8%');
  });
});
