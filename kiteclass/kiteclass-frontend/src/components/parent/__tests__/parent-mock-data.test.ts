/**
 * Mock-data unit tests — formatVN + buildMockAttendance shape contracts.
 *
 * Wave 49 Bucket A (GAP-267).
 */

import { describe, expect, it } from 'vitest';
import {
  buildMockAttendance,
  formatVN,
  MOCK_GRADES,
  MOCK_INVOICES,
} from '../parent-mock-data';

describe('formatVN', () => {
  it('formats integer amounts with VN thousand separators + đ suffix', () => {
    expect(formatVN(4_500_000)).toBe('4.500.000đ');
  });

  it('handles zero', () => {
    expect(formatVN(0)).toBe('0đ');
  });
});

describe('buildMockAttendance', () => {
  it('returns 30 cells with valid status codes', () => {
    const cells = buildMockAttendance();
    expect(cells).toHaveLength(30);
    const valid = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED', 'NO_CLASS'];
    for (const c of cells) {
      expect(valid).toContain(c.status);
      expect(c.date).toMatch(/^2026-04-\d{2}$/);
    }
  });
});

describe('MOCK_INVOICES', () => {
  it('has at least one outstanding + one paid invoice', () => {
    expect(MOCK_INVOICES.some((i) => i.status === 'PAID')).toBe(true);
    expect(
      MOCK_INVOICES.some((i) => i.status === 'PENDING_PAYMENT' || i.status === 'OVERDUE'),
    ).toBe(true);
  });
});

describe('MOCK_GRADES', () => {
  it('uses VN 10-pt scale', () => {
    for (const g of MOCK_GRADES) {
      expect(g.score).toBeGreaterThanOrEqual(0);
      expect(g.score).toBeLessThanOrEqual(10);
    }
  });
});
