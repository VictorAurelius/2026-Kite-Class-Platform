/**
 * Type definitions for ClassScheduleManager — G4 component (Wave 28 Bucket B).
 *
 * Design source: documents/02-architecture/design-system/ui_kits/components/G4-class-schedule-manager/README.md
 * Component gap row: documents/02-architecture/design-system/dossier/04-component-gaps.md §G4
 * Used by screens: KiteClass `/classes/[id]/schedule` (Principal / Owner / Teacher)
 *
 * Vietnamese-first per CLAUDE.md. Week starts Monday (T2) — VN convention.
 */

/**
 * Recurrence pattern for a slot. `CUSTOM` = caller-defined day picks.
 *
 * - `WEEKLY`   — same `daysOfWeek` every week
 * - `BIWEEKLY` — same `daysOfWeek` every other week
 * - `MONTHLY`  — same nth weekday of the month (caller resolves)
 * - `CUSTOM`   — explicit day list, no auto-recur engine in v1
 */
export type RecurrenceRule = 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'CUSTOM';

/**
 * Vietnamese week-day codes. Week starts MONDAY (T2) per VN convention —
 * NEVER Sunday-first. Display labels per kit README:
 *   T2 / T3 / T4 / T5 / T6 / T7 / CN
 */
export type WeekDay = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN';

/**
 * One scheduled slot. Times are stored as `HH:mm` 24-hour strings to avoid
 * Date timezone landmines in the conflict-detection layer.
 *
 * For recurring slots, `date` represents the FIRST occurrence; the engine
 * (caller) projects subsequent occurrences using `recurrence` + `daysOfWeek`.
 *
 * `endsOn` and `endsAfterOccurrences` are mutually exclusive (radio in UI):
 * exactly one of them is set on a recurring slot. Single (CUSTOM, one-day)
 * slots leave both unset.
 */
export type ScheduleSlot = {
  id: string;
  /** Display title — e.g. "Toán nâng cao". */
  className: string;
  /** Optional teacher display name — used in conflict-warning copy. */
  teacherName?: string;
  /** ISO `yyyy-MM-dd` date of the FIRST occurrence (or the only occurrence). */
  date: string;
  /** `HH:mm` 24h start. */
  startTime: string;
  /** `HH:mm` 24h end. Must be lexicographically > startTime. */
  endTime: string;
  /** Recurrence rule (`CUSTOM` = single day or caller-resolved). */
  recurrence: RecurrenceRule;
  /**
   * Days of week this slot repeats on (for WEEKLY / BIWEEKLY). For MONTHLY +
   * CUSTOM, caller decides; we don't reinterpret.
   */
  daysOfWeek: ReadonlyArray<WeekDay>;
  /** Recurrence end date — `yyyy-MM-dd`. Mutually exclusive with `endsAfterOccurrences`. */
  endsOn?: string;
  /** Total occurrences before stopping. Mutually exclusive with `endsOn`. */
  endsAfterOccurrences?: number;
};

/**
 * One conflict surfaced by `detectConflicts`. Always references TWO slot ids
 * (the new + the existing) and the human-readable explanation strings the
 * UI can render verbatim.
 */
export type ConflictWarning = {
  slotAId: string;
  slotBId: string;
  /** ISO `yyyy-MM-dd` of the conflicting day. */
  date: string;
  /** Pre-rendered VN summary, e.g. `"Thứ Sáu, 11/04/2026 · 14:00 – 15:30"`. */
  summary: string;
  /** Explanation copy — e.g. `"Trùng giờ với Văn 8 (Cô Lan)"`. */
  reason: string;
};

/**
 * UI lifecycle states. Mirrors the 5 spec'd HTML state files.
 *
 * - `empty`             — no slots, render preset CTA grid
 * - `single-class`      — exactly one one-time slot rendered
 * - `recurring-edit`    — form open: subject + days toggle + time + date range
 * - `conflict-warning`  — at least one entry in `conflicts`; UI surfaces alert
 * - `saved`             — read-only week view + class list + stat strip
 */
export type ScheduleManagerState =
  | 'empty'
  | 'single-class'
  | 'recurring-edit'
  | 'conflict-warning'
  | 'saved';

/**
 * Public props for `<ClassScheduleManager>`.
 *
 * The component is **controlled** — parent owns `slots`, `state`, and (when
 * relevant) the in-flight `editingSlot` plus the `conflicts` list. The
 * component does NOT compute conflicts itself; callers run `detectConflicts`
 * (or their server) and feed the result back. This matches the controlled
 * shape of the other Wave 27 ports (G2/G5/G6/G7).
 */
export type ClassScheduleManagerProps = {
  /** Class display name shown in the page header — e.g. `"Lớp 6A1"`. */
  className: string;
  /** Optional school year label, e.g. `"Năm học 2026-2027"`. */
  schoolYearLabel?: string;
  /** All scheduled slots (existing + the in-flight edit, if any). */
  slots: ReadonlyArray<ScheduleSlot>;
  state: ScheduleManagerState;
  /**
   * Slot currently being edited (recurring-edit / conflict-warning state).
   * When unset, the recurring-edit form is in `create-new` mode.
   */
  editingSlot?: ScheduleSlot;
  /**
   * Active conflicts — caller-supplied. Required when state ===
   * `conflict-warning`; ignored otherwise.
   */
  conflicts?: ReadonlyArray<ConflictWarning>;
  /** Called when user clicks "Thêm buổi học". */
  onAddSlot?: () => void;
  /** Called when user picks a preset (3-buổi/tuần, Hằng ngày, Cuối tuần). */
  onPickPreset?: (presetId: 'three-per-week' | 'daily' | 'weekend') => void;
  /** Called when the recurring-edit form submits. */
  onSaveSlot?: (slot: ScheduleSlot) => void | Promise<void>;
  /** Called when user cancels the edit form OR the conflict resolution panel. */
  onCancelEdit?: () => void;
  /** Called when user picks a conflict resolution option. */
  onResolveConflict?: (
    conflict: ConflictWarning,
    resolution: 'change-teacher' | 'change-time' | 'skip-day',
  ) => void;
  /** Saved-state actions. */
  onExportPdf?: () => void;
};
