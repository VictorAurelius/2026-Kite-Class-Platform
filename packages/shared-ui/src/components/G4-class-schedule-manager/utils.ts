/**
 * Pure helpers for G4 Class Schedule Manager.
 *
 * Why hand-rolled instead of a date-fns / dayjs dependency:
 *   The component spec requires zero new deps (per Wave 28 brief). The
 *   conflict-detection layer compares `HH:mm` strings lexicographically,
 *   which is correct for any 24-hour zero-padded format (`"14:00" < "15:30"`
 *   under ASCII ordering — no Date timezone landmines).
 *
 * Conflict semantics (kit README §Use case):
 *   - Same date AND time-ranges overlap → conflict.
 *   - Back-to-back slots (`A.end === B.start`) → NOT a conflict — two
 *     consecutive 90-min sessions are common ("Toán 14:00-15:30, then
 *     Văn 15:30-17:00" is a valid Vietnamese after-school schedule).
 *   - Different dates → never conflict (this v1 layer compares each slot's
 *     declared `date` only; recurrence-expansion is a future-scope concern).
 */

import type { ConflictWarning, ScheduleSlot } from './types';

/**
 * Returns true iff [aStart, aEnd) and [bStart, bEnd) overlap.
 *
 * Using half-open intervals (`end` is exclusive) means:
 *   `9:00-10:00` and `10:00-11:00` → NOT overlapping (back-to-back OK).
 *   `9:00-10:01` and `10:00-11:00` → overlapping (1-min overlap).
 *
 * Inputs MUST be `HH:mm` 24h zero-padded strings. Lexicographic comparison
 * matches numeric comparison for that format.
 */
function timesOverlap(
  aStart: string,
  aEnd: string,
  bStart: string,
  bEnd: string,
): boolean {
  return aStart < bEnd && bStart < aEnd;
}

/**
 * Format `yyyy-MM-dd` → `dd/MM/yyyy` for VN-locale display strings.
 *
 * Defensive: returns the input unchanged if it doesn't parse cleanly so the
 * conflict warning never blows up the whole UI for one malformed slot.
 */
function formatVNDate(iso: string): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!m) return iso;
  return `${m[3]}/${m[2]}/${m[1]}`;
}

/**
 * Pairwise conflict detection over the supplied slots.
 *
 * Algorithm: O(n²) over all unique pairs (`i < j`). For typical class sizes
 * (≤ 50 slots / class / school year), this is < 1ms — virtualization is a
 * future-scope concern (see GAP-273 deferred items).
 *
 * Pairs are emitted exactly once: `(slots[i], slots[j])` where `i < j`. The
 * function never produces both `(A,B)` and `(B,A)`.
 */
export function detectConflicts(
  slots: ReadonlyArray<ScheduleSlot>,
): ConflictWarning[] {
  const conflicts: ConflictWarning[] = [];
  for (let i = 0; i < slots.length; i++) {
    for (let j = i + 1; j < slots.length; j++) {
      const a = slots[i];
      const b = slots[j];
      if (!a || !b) continue;
      if (a.date !== b.date) continue;
      if (!timesOverlap(a.startTime, a.endTime, b.startTime, b.endTime)) continue;

      const dayLabel = formatVNDate(a.date);
      // Use the earliest start + latest end so the summary always describes
      // the "envelope" of the conflict rather than just one slot.
      const start = a.startTime < b.startTime ? a.startTime : b.startTime;
      const end = a.endTime > b.endTime ? a.endTime : b.endTime;
      const teacherSuffix = b.teacherName ? ` (${b.teacherName})` : '';
      conflicts.push({
        slotAId: a.id,
        slotBId: b.id,
        date: a.date,
        summary: `${dayLabel} · ${start} – ${end}`,
        reason: `Trùng giờ với ${b.className}${teacherSuffix}`,
      });
    }
  }
  return conflicts;
}
