import { describe, it, expect } from 'vitest';
import { buildPaletteVariants } from '../paletteVariants';

const BASE = { primary: '#2563EB', secondary: '#F59E0B', accent: '#10B981' };
const HEX = /^#[0-9a-fA-F]{6}$/;

describe('buildPaletteVariants (GAP-1212 multi-variant)', () => {
  it('returns exactly 3 variants with stable ids + VN labels', () => {
    const v = buildPaletteVariants(BASE);
    expect(v).toHaveLength(3);
    expect(v.map((x) => x.id)).toEqual(['variant-a', 'variant-b', 'variant-c']);
    v.forEach((x) => expect(x.label).toMatch(/^Bản [ABC] · /));
  });

  it('variant A is the deploy-faithful base palette (unchanged)', () => {
    const [a] = buildPaletteVariants(BASE);
    expect(a.primary).toBe('#2563EB'.toLowerCase());
    expect(a.secondary).toBe('#F59E0B'.toLowerCase());
    expect(a.accent).toBe('#10B981'.toLowerCase());
  });

  it('every variant colour is a valid 6-digit hex', () => {
    buildPaletteVariants(BASE).forEach((x) => {
      expect(x.primary).toMatch(HEX);
      expect(x.secondary).toMatch(HEX);
      expect(x.accent).toMatch(HEX);
    });
  });

  it('variants B and C differ from A (real alternatives, not duplicates)', () => {
    const [a, b, c] = buildPaletteVariants(BASE);
    expect(b.primary).not.toBe(a.primary);
    expect(c.primary).not.toBe(a.primary);
    expect(new Set([a.primary, b.primary, c.primary]).size).toBe(3);
  });

  it('is deterministic — same base yields identical variants', () => {
    expect(buildPaletteVariants(BASE)).toEqual(buildPaletteVariants(BASE));
  });

  it('accepts bare hex (no #) + falls back on invalid input', () => {
    const v = buildPaletteVariants({ primary: '2563EB', secondary: 'nope', accent: '' });
    expect(v[0].primary).toBe('#2563eb');
    expect(v[0].secondary).toMatch(HEX); // fallback
    expect(v[0].accent).toMatch(HEX); // fallback
  });
});
