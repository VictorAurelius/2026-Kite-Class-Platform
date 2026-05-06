/**
 * Pure-logic helpers for G8 Attendance Calendar.
 *
 * Contract documented in `__tests__/utils.test.ts`. Kept side-effect-free so
 * caller can run `calculateStreak` either at render time or pre-compute and
 * pass through `AttendanceCalendarProps.streak`.
 */

import type { AttendanceDayStatus, StreakInfo } from './types';

/**
 * Window the streak calculation looks at, in days.
 *
 * "30-day rolling streak" = longest consecutive PRESENT run in the LAST 30
 * elements of the input. Older days are ignored even if they form longer
 * runs — the indicator is intentionally fresh.
 */
const STREAK_WINDOW_DAYS = 30;

/**
 * Return the longest consecutive `PRESENT`-day streak in the trailing
 * `STREAK_WINDOW_DAYS` of `days`.
 *
 * Empty input or no PRESENT days → `{ count: 0 }`.
 *
 * Time complexity O(n), space O(1).
 */
export function calculateStreak(
  days: ReadonlyArray<AttendanceDayStatus>,
): StreakInfo {
  if (days.length === 0) {
    return { count: 0 };
  }

  const window =
    days.length <= STREAK_WINDOW_DAYS
      ? days
      : days.slice(days.length - STREAK_WINDOW_DAYS);

  let longest = 0;
  let current = 0;
  for (const status of window) {
    if (status === 'PRESENT') {
      current += 1;
      if (current > longest) {
        longest = current;
      }
    } else {
      current = 0;
    }
  }

  return { count: longest };
}
