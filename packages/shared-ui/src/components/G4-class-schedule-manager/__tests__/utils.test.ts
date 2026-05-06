/**
 * detectConflicts() — pure overlap-detection edge cases.
 *
 * Conflict definition (per kit README + spec):
 *   - Same date AND time-ranges overlap = CONFLICT.
 *   - Back-to-back (end of A === start of B) = NOT a conflict.
 *   - Different dates = NOT a conflict, regardless of time.
 *
 * For recurring slots, this v1 layer compares the FIRST occurrence (`date`)
 * only. Caller can expand recurrence -> per-day slots before calling if they
 * need conflict checks across the recurrence horizon (future scope).
 *
 * VN-specific note: HH:mm strings are compared lexicographically (safe because
 * `09:30` < `10:00` < `14:00` in ASCII). Times use a 24-hour clock per kit.
 */

import { describe, expect, it } from 'vitest';
import { detectConflicts } from '../utils';
import type { ScheduleSlot } from '../types';

function slot(overrides: Partial<ScheduleSlot> & { id: string }): ScheduleSlot {
  return {
    className: 'Toán nâng cao',
    teacherName: 'Cô Lan',
    date: '2026-04-07',
    startTime: '14:00',
    endTime: '15:30',
    recurrence: 'CUSTOM',
    daysOfWeek: [],
    ...overrides,
  };
}

describe('detectConflicts', () => {
  it('returns no conflicts for an empty list', () => {
    expect(detectConflicts([])).toEqual([]);
  });

  it('returns no conflicts for a single slot', () => {
    expect(detectConflicts([slot({ id: 'a' })])).toEqual([]);
  });

  it('flags identical (same-day same-time) slots as conflict', () => {
    const a = slot({ id: 'a' });
    const b = slot({ id: 'b' });
    const conflicts = detectConflicts([a, b]);
    expect(conflicts).toHaveLength(1);
    expect(conflicts[0]?.slotAId).toBe('a');
    expect(conflicts[0]?.slotBId).toBe('b');
    expect(conflicts[0]?.date).toBe('2026-04-07');
  });

  it('flags partial overlap (A 14:00-15:30 vs B 15:00-16:00) as conflict', () => {
    const a = slot({ id: 'a', startTime: '14:00', endTime: '15:30' });
    const b = slot({ id: 'b', startTime: '15:00', endTime: '16:00' });
    expect(detectConflicts([a, b])).toHaveLength(1);
  });

  it('does NOT flag back-to-back slots (A ends 10:00, B starts 10:00) as conflict', () => {
    const a = slot({ id: 'a', startTime: '09:00', endTime: '10:00' });
    const b = slot({ id: 'b', startTime: '10:00', endTime: '11:00' });
    expect(detectConflicts([a, b])).toEqual([]);
  });

  it('does NOT flag slots on different dates even if times overlap exactly', () => {
    const a = slot({ id: 'a', date: '2026-04-07' });
    const b = slot({ id: 'b', date: '2026-04-08' });
    expect(detectConflicts([a, b])).toEqual([]);
  });

  it('flags fully-contained overlap (B 14:30-15:00 inside A 14:00-15:30) as conflict', () => {
    const a = slot({ id: 'a', startTime: '14:00', endTime: '15:30' });
    const b = slot({ id: 'b', startTime: '14:30', endTime: '15:00' });
    expect(detectConflicts([a, b])).toHaveLength(1);
  });

  it('detects multiple pairwise conflicts across N slots', () => {
    const a = slot({ id: 'a', startTime: '14:00', endTime: '15:30' });
    const b = slot({ id: 'b', startTime: '15:00', endTime: '16:00' });
    const c = slot({ id: 'c', startTime: '15:15', endTime: '16:30' });
    // a vs b, a vs c, b vs c → 3 pairs
    expect(detectConflicts([a, b, c])).toHaveLength(3);
  });

  it('builds VN summary copy in the conflict warning', () => {
    const a = slot({
      id: 'a',
      date: '2026-04-11',
      startTime: '14:00',
      endTime: '15:30',
      className: 'Toán nâng cao',
    });
    const b = slot({
      id: 'b',
      date: '2026-04-11',
      startTime: '14:00',
      endTime: '15:30',
      className: 'Văn 8',
      teacherName: 'Cô Lan',
    });
    const conflicts = detectConflicts([a, b]);
    expect(conflicts[0]?.summary).toContain('11/04/2026');
    expect(conflicts[0]?.summary).toContain('14:00');
    expect(conflicts[0]?.summary).toContain('15:30');
    expect(conflicts[0]?.reason).toMatch(/Văn 8|Toán nâng cao/);
  });

  it('does not duplicate (B,A) when (A,B) already flagged — pairwise only', () => {
    // Symmetric pair: detector should emit 1 entry, not 2.
    const a = slot({ id: 'a' });
    const b = slot({ id: 'b' });
    const conflicts = detectConflicts([a, b]);
    expect(conflicts).toHaveLength(1);
  });

  it('handles slots with 1-minute overlap (A ends 10:01, B starts 10:00) as conflict', () => {
    const a = slot({ id: 'a', startTime: '09:00', endTime: '10:01' });
    const b = slot({ id: 'b', startTime: '10:00', endTime: '11:00' });
    expect(detectConflicts([a, b])).toHaveLength(1);
  });
});
