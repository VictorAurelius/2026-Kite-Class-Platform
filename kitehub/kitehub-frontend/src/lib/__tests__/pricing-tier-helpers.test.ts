import { describe, it, expect } from 'vitest';
import {
  recommendTierByStudents,
  computeDowngradeImpact,
  allowsCustomDomain,
  formatVnd,
} from '../pricing';

describe('recommendTierByStudents (GAP-1269)', () => {
  it('recommends FREE for 0–10 students', () => {
    expect(recommendTierByStudents(0).tier).toBe('FREE');
    expect(recommendTierByStudents(10).tier).toBe('FREE');
    expect(recommendTierByStudents(10).isEnterprise).toBe(false);
  });

  it('recommends BASIC for 11–50 students', () => {
    expect(recommendTierByStudents(11).tier).toBe('BASIC');
    expect(recommendTierByStudents(50).tier).toBe('BASIC');
  });

  it('recommends PREMIUM for 51–200 students', () => {
    expect(recommendTierByStudents(51).tier).toBe('PREMIUM');
    expect(recommendTierByStudents(200).tier).toBe('PREMIUM');
  });

  it('recommends ENTERPRISE (sales-assisted) for >200 students', () => {
    const rec = recommendTierByStudents(201);
    expect(rec.tier).toBe('ENTERPRISE');
    expect(rec.isEnterprise).toBe(true);
  });

  it('clamps negative / NaN input to 0 → FREE', () => {
    expect(recommendTierByStudents(-5).tier).toBe('FREE');
    expect(recommendTierByStudents(Number.NaN).tier).toBe('FREE');
  });
});

describe('allowsCustomDomain (SUB-22)', () => {
  it('only PREMIUM + ENTERPRISE allow custom domain', () => {
    expect(allowsCustomDomain('FREE')).toBe(false);
    expect(allowsCustomDomain('BASIC')).toBe(false);
    expect(allowsCustomDomain('PREMIUM')).toBe(true);
    expect(allowsCustomDomain('ENTERPRISE')).toBe(true);
  });
});

describe('computeDowngradeImpact (GAP-1261)', () => {
  it('PREMIUM → BASIC flags custom-domain loss + cap reductions', () => {
    const impact = computeDowngradeImpact('PREMIUM', 'BASIC');
    expect(impact.hasImpact).toBe(true);
    expect(impact.losesCustomDomain).toBe(true);
    expect(impact.studentCapFrom).toBe(200);
    expect(impact.studentCapTo).toBe(50);
    expect(impact.losesAiBranding).toBe(false);
  });

  it('BASIC → FREE flags AI-branding loss', () => {
    const impact = computeDowngradeImpact('BASIC', 'FREE');
    expect(impact.hasImpact).toBe(true);
    expect(impact.losesAiBranding).toBe(true);
    expect(impact.losesCustomDomain).toBe(false); // BASIC already had no custom domain
  });

  it('ENTERPRISE (unlimited) → PREMIUM is a real cap reduction', () => {
    const impact = computeDowngradeImpact('ENTERPRISE', 'PREMIUM');
    expect(impact.hasImpact).toBe(true);
    expect(impact.studentCapFrom).toBe(-1); // unlimited
    expect(impact.studentCapTo).toBe(200);
    expect(impact.losesCustomDomain).toBe(false); // both allow custom domain
  });
});

describe('formatVnd', () => {
  it('formats VND amount and handles custom (-1)', () => {
    expect(formatVnd(-1)).toBe('Liên hệ');
    expect(formatVnd(500000)).toContain('500.000');
  });
});
