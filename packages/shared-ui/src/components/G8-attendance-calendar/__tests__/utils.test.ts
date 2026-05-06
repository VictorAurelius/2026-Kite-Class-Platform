/**
 * Pure-logic tests for `calculateStreak` — G8 Attendance Calendar (Wave 28 Bucket C).
 *
 * Contract:
 *  - Input: ReadonlyArray<AttendanceDayStatus> (any length; only the LAST 30
 *    elements are considered — "30-day rolling streak").
 *  - Output: { count: number, deferred?: boolean }
 *  - count = length of the LONGEST consecutive PRESENT run in the trailing 30 days.
 *  - Non-PRESENT (ABSENT / LATE / EXCUSED / NO_CLASS / FUTURE) breaks a streak.
 *  - Empty input → { count: 0 }
 *  - Multiple streaks → pick the longest, NOT the most recent.
 *
 * Why this matters: streak indicator is the gamification hook for the teacher
 * (see `streak-highlight.html` — "🏆 Chuỗi 30 ngày hoàn hảo!").
 */

import { describe, expect, it } from 'vitest';
import { calculateStreak } from '../utils';
import type { AttendanceDayStatus } from '../types';

const P: AttendanceDayStatus = 'PRESENT';
const A: AttendanceDayStatus = 'ABSENT';
const L: AttendanceDayStatus = 'LATE';
const E: AttendanceDayStatus = 'EXCUSED';
const N: AttendanceDayStatus = 'NO_CLASS';
const F: AttendanceDayStatus = 'FUTURE';

describe('calculateStreak', () => {
  it('returns 0 for empty input', () => {
    expect(calculateStreak([])).toEqual({ count: 0 });
  });

  it('returns 1 for a single PRESENT day', () => {
    expect(calculateStreak([P])).toEqual({ count: 1 });
  });

  it('returns 0 when no PRESENT days exist', () => {
    expect(calculateStreak([A, L, E, N, F])).toEqual({ count: 0 });
  });

  it('returns 30 for a full perfect 30-day streak', () => {
    const days: AttendanceDayStatus[] = Array.from({ length: 30 }, () => P);
    expect(calculateStreak(days)).toEqual({ count: 30 });
  });

  it('caps streak window at trailing 30 days (older PRESENT runs ignored)', () => {
    // 50-day input: oldest 20 days are P (would otherwise form a 20-streak),
    // but only the trailing 30 are considered. Trailing 30 = [A × 30] → 0.
    const oldP: AttendanceDayStatus[] = Array.from({ length: 20 }, () => P);
    const newA: AttendanceDayStatus[] = Array.from({ length: 30 }, () => A);
    expect(calculateStreak([...oldP, ...newA])).toEqual({ count: 0 });
  });

  it('breaks streak on a single ABSENT', () => {
    expect(calculateStreak([P, P, P, A, P, P])).toEqual({ count: 3 });
  });

  it('breaks streak on LATE (LATE is not PRESENT)', () => {
    expect(calculateStreak([P, P, L, P])).toEqual({ count: 2 });
  });

  it('breaks streak on EXCUSED', () => {
    expect(calculateStreak([P, P, E, P])).toEqual({ count: 2 });
  });

  it('breaks streak on NO_CLASS (gap day still resets the run)', () => {
    expect(calculateStreak([P, P, N, P])).toEqual({ count: 2 });
  });

  it('breaks streak on FUTURE (pending session is not PRESENT)', () => {
    expect(calculateStreak([P, P, P, F, F])).toEqual({ count: 3 });
  });

  it('picks LONGEST streak, not most recent', () => {
    // Two streaks: 5-long (older) and 2-long (recent) — longest wins.
    expect(
      calculateStreak([P, P, P, P, P, A, P, P]),
    ).toEqual({ count: 5 });
  });

  it('handles trailing-only PRESENT run', () => {
    expect(calculateStreak([A, A, P, P, P])).toEqual({ count: 3 });
  });

  it('handles leading-only PRESENT run', () => {
    expect(calculateStreak([P, P, P, P, A, A])).toEqual({ count: 4 });
  });

  it('mixed sequence with multiple equal-length streaks', () => {
    expect(calculateStreak([P, P, A, P, P, A, P, P])).toEqual({ count: 2 });
  });

  it('only counts last 30 of a 60-day mixed input', () => {
    // First 30 days: all PRESENT (would be 30-streak).
    // Last 30 days: 5-PRESENT + 25-ABSENT.
    const head: AttendanceDayStatus[] = Array.from({ length: 30 }, () => P);
    const tail: AttendanceDayStatus[] = [
      ...Array.from({ length: 5 }, () => P),
      ...Array.from({ length: 25 }, () => A),
    ];
    // Trailing 30 = [P×5, A×25] → longest streak = 5.
    expect(calculateStreak([...head, ...tail])).toEqual({ count: 5 });
  });
});
