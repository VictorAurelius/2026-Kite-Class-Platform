/**
 * G11 utils — WCAG contrast calculator + auto-fix engine.
 *
 * Spec sources:
 *   - https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
 *   - `.claude/rules/ai-branding-guidelines.md` §5 (≥4.5:1 AA threshold)
 */

import { describe, expect, it } from 'vitest';
import { WCAG_AA_NORMAL, calculateContrast, suggestFix } from '../utils';

const EPSILON = 0.05;

describe('calculateContrast', () => {
  it('white-on-white ≈ 1', () => {
    const r = calculateContrast('#ffffff', '#ffffff');
    expect(Math.abs(r - 1)).toBeLessThan(EPSILON);
  });

  it('black-on-white = 21', () => {
    const r = calculateContrast('#000000', '#ffffff');
    expect(Math.abs(r - 21)).toBeLessThan(EPSILON);
  });

  it('mid-gray on white sits near AA threshold', () => {
    // #777777 on #ffffff is the canonical "just-below AA" sample (~4.48:1).
    const r = calculateContrast('#777777', '#ffffff');
    expect(r).toBeGreaterThan(4);
    expect(r).toBeLessThan(5);
  });

  it('order-independent (fg/bg vs bg/fg yield same result)', () => {
    const a = calculateContrast('#2563eb', '#ffffff');
    const b = calculateContrast('#ffffff', '#2563eb');
    expect(Math.abs(a - b)).toBeLessThan(0.001);
  });

  it('matches WCAG warning state spec (yellow-on-yellow fails AA)', () => {
    // From `states/wcag-warning.html` — failing combo demo.
    const r = calculateContrast('#fbbf24', '#fef3c7');
    expect(r).toBeGreaterThan(1);
    expect(r).toBeLessThan(WCAG_AA_NORMAL);
  });
});

describe('suggestFix', () => {
  it('failing pair → output passes AA', () => {
    const fix = suggestFix({
      foreground: '#fbbf24', // yellow
      background: '#fef3c7', // light yellow → fails ~3.2:1
    });
    const ratio = calculateContrast(fix.fg, fix.bg);
    expect(ratio).toBeGreaterThanOrEqual(WCAG_AA_NORMAL);
  });

  it('preserves background colour by default', () => {
    const fix = suggestFix({
      foreground: '#fbbf24',
      background: '#fef3c7',
    });
    expect(fix.bg).toBe('#fef3c7');
    expect(fix.fg).not.toBe('#fbbf24');
  });

  it('returns reason explaining the fix', () => {
    const fix = suggestFix({
      foreground: '#fbbf24',
      background: '#fef3c7',
    });
    expect(fix.reason).toMatch(/AA/);
  });

  it('already-passing pair returned as-is', () => {
    const fix = suggestFix({
      foreground: '#000000',
      background: '#ffffff',
    });
    expect(fix.fg).toBe('#000000');
    expect(fix.bg).toBe('#ffffff');
    expect(fix.reason).toMatch(/đạt AA/i);
  });

  it('handles dark-on-dark failing pair', () => {
    // Dark grey on darker grey — fails AA, fix should lighten foreground.
    const fix = suggestFix({
      foreground: '#444444',
      background: '#222222',
    });
    const ratio = calculateContrast(fix.fg, fix.bg);
    expect(ratio).toBeGreaterThanOrEqual(WCAG_AA_NORMAL);
  });
});
