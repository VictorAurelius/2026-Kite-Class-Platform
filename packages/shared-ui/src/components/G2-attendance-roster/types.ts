/**
 * Type definitions for AttendanceRoster — G2 component (Wave 27 Bucket A).
 *
 * Design source: documents/02-architecture/design-system/ui_kits/components/G2-attendance-roster/spec.md
 * Component gap row: documents/02-architecture/design-system/dossier/04-component-gaps.md §G2
 * Used by screens: KiteClass `/classes/[id]/attendance` (teacher), Parent attendance history (read-only variant)
 *
 * Status codes (Vietnamese-first per CLAUDE.md):
 *   P  green  — Có mặt          (Present)
 *   V  blue   — Vắng có phép    (Excused absence)
 *   M  red    — Vắng không phép (Unexcused absence)
 *   L  amber  — Đi trễ          (Late)
 */

export type AttendanceStatus = 'P' | 'V' | 'M' | 'L';

/**
 * One student row in the roster.
 *
 * - `status` — `null` means the teacher has not yet marked the student.
 * - `currentRate` — running attendance rate Year-to-Date, range `0..1` (0.92 = 92%).
 * - `lateMinutes` — populated only when `status === 'L'`.
 * - `note` — free-form excuse note, populated when `status === 'V'`.
 */
export type StudentRecord = {
  id: string;
  fullName: string;
  studentCode: string;
  avatarUrl?: string;
  currentRate: number;
  status: AttendanceStatus | null;
  lateMinutes?: number;
  note?: string;
};

/**
 * Class session metadata rendered in the roster header.
 *
 * Date format `dd/MM/yyyy`; time `HH:mm-HH:mm` 24h. Caller formats display
 * strings — this component shows ISO-derived data only via the `Date` object.
 */
export type ClassSession = {
  id: string;
  className: string;
  sessionNumber: number;
  date: Date;
  durationMinutes: number;
  teacherName: string;
};

/**
 * Roster lifecycle states. UI representations are documented in
 * `spec.md` §States.
 *
 * - `default`  — loaded, none marked, save bar hidden
 * - `marking`  — at least one row dirty, sticky save bar visible
 * - `saving`   — onSave in flight (controlled via prop)
 * - `saved`    — save complete, rows locked read-only
 * - `error`    — save failed; banner + retry CTA
 * - `empty`    — no students enrolled
 * - `loading`  — initial fetch (skeleton rows)
 */
export type AttendanceRosterState =
  | 'default'
  | 'marking'
  | 'saving'
  | 'saved'
  | 'error'
  | 'empty'
  | 'loading';

/**
 * Optional metadata captured alongside the status toggle.
 */
export type AttendanceChangeOpts = {
  lateMinutes?: number;
  note?: string;
};

/**
 * Public props for `<AttendanceRoster>`.
 *
 * The component is **controlled** — parent owns the `students` array + `state`.
 * Emits `onChange(studentId, status)` per toggle and `onSave()` when the
 * sticky save bar's primary action fires.
 *
 * Wave 27 Bucket A scope = render + emit. Optimistic UI happens in caller
 * via state-update on `onChange`; this matches ConsentBanner's controlled
 * shape and keeps the component free of fetcher / persistence concerns.
 */
export type AttendanceRosterProps = {
  classSession: ClassSession;
  students: ReadonlyArray<StudentRecord>;
  state: AttendanceRosterState;
  onChange: (
    studentId: string,
    status: AttendanceStatus,
    opts?: AttendanceChangeOpts,
  ) => void;
  onSave: () => void | Promise<void>;
  onMarkAllPresent?: () => void;
  /** Save failure message, surfaced when `state === 'error'`. */
  errorMessage?: string;
  /** Override save-bar copy. Defaults to `${count} thay đổi`. */
  dirtyCount?: number;
};
