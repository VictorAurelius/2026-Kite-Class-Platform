'use client';

/**
 * AttendanceRoster — G2 component (Wave 27 Bucket A).
 *
 * Mark daily attendance for a class session. Teacher toggles per-student
 * status: P (Có mặt) / V (Vắng có phép) / M (Vắng không phép) / L (Đi trễ).
 *
 * Design source:
 *   documents/02-architecture/design-system/ui_kits/components/G2-attendance-roster/spec.md
 *
 * State machine:
 *   loading → empty | default
 *   default → marking → saving → saved | error
 *   saved   → marking (re-edit, future scope)
 *
 * The component is **controlled** — parent owns the `students` array and the
 * `state` enum. Per-toggle and per-save callbacks let the parent host
 * optimistic updates / persistence concerns. Mirrors the controlled shape of
 * `<ConsentBanner>` in this same package (no fetch / persistence here).
 *
 * Accessibility (WCAG AA):
 *  - Each row's 4-button toggle is a `role="radiogroup"` with the student's
 *    name as the group label, and each button is a `role="radio"` with
 *    `aria-checked`. Buttons combine letter glyph + Vietnamese sr-only label
 *    so colour is never the only signal.
 *  - Touch targets ≥ 44×44 (`min-w-[44px] min-h-[44px]`). Tablet is the
 *    primary surface per spec.
 *  - Save bar is a `role="region"` sticky-bottom landmark.
 *  - `prefers-reduced-motion` honoured by relying on opacity/border-only
 *    transitions (no slide).
 *
 * Vietnamese-first per CLAUDE.md. Strings copied verbatim from spec.md.
 */

import type React from 'react';
import { useCallback, useId } from 'react';
import type {
  AttendanceRosterProps,
  AttendanceStatus,
  StudentRecord,
} from './types';

const COPY_VI = {
  markAllPresent: 'Tất cả có mặt',
  save: 'Lưu',
  saving: 'Đang lưu...',
  retry: 'Thử lại',
  changeCount: (n: number): string => `${n} thay đổi`,
  empty: 'Chưa có học sinh nào trong lớp này.',
  emptyCta: 'Thêm học sinh',
  saved: 'Đã lưu',
  error: 'Không lưu được danh sách điểm danh.',
  status: {
    P: { glyph: 'P', label: 'Có mặt' },
    V: { glyph: 'V', label: 'Vắng có phép' },
    M: { glyph: 'M', label: 'Vắng không phép' },
    L: { glyph: 'L', label: 'Đi trễ' },
  } satisfies Record<
    AttendanceStatus,
    { glyph: string; label: string }
  >,
  rate: (rate: number): string => `${Math.round(rate * 100)}%`,
};

const STATUS_ORDER: ReadonlyArray<AttendanceStatus> = ['P', 'V', 'M', 'L'];

// Theme-token utility classes (same approach as ConsentBanner — Tailwind tokens
// resolved by consumer's tailwind.config). Per-status background uses semantic
// theme tokens so consumer apps can re-skin without forking this component.
const STATUS_ACTIVE_CLASS: Record<AttendanceStatus, string> = {
  P: 'bg-emerald-600 text-white border-emerald-600',
  V: 'bg-sky-600 text-white border-sky-600',
  M: 'bg-rose-600 text-white border-rose-600',
  L: 'bg-amber-500 text-black border-amber-500',
};

const STATUS_INACTIVE_CLASS =
  'bg-background text-foreground border-border hover:bg-muted/50';

export function AttendanceRoster(
  props: AttendanceRosterProps,
): React.JSX.Element {
  const {
    classSession,
    students,
    state,
    onChange,
    onSave,
    onMarkAllPresent,
    errorMessage,
    dirtyCount,
  } = props;

  const handleToggle = useCallback(
    (studentId: string, status: AttendanceStatus) => {
      if (state === 'saved' || state === 'saving') return;
      onChange(studentId, status);
    },
    [onChange, state],
  );

  const handleSave = useCallback(() => {
    if (state === 'saving') return;
    void onSave();
  }, [onSave, state]);

  // Loading skeleton
  if (state === 'loading') {
    return (
      <div
        data-testid="attendance-roster-loading"
        className="flex flex-col gap-2 p-4"
        role="status"
        aria-live="polite"
        aria-label="Đang tải danh sách điểm danh"
      >
        <div className="h-12 w-full animate-pulse rounded-lg bg-muted/60" />
        {Array.from({ length: 5 }, (_, i) => (
          <div
            key={i}
            className="h-16 w-full animate-pulse rounded-lg bg-muted/40"
          />
        ))}
      </div>
    );
  }

  // Empty state
  if (state === 'empty' || students.length === 0) {
    return (
      <div
        data-testid="attendance-roster-empty"
        className="flex flex-col items-center justify-center gap-3 rounded-lg border bg-card p-12 text-center"
      >
        <p className="text-sm text-muted-foreground">{COPY_VI.empty}</p>
        <button
          type="button"
          className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
        >
          {COPY_VI.emptyCta}
        </button>
      </div>
    );
  }

  const isLocked = state === 'saved';
  const isSaving = state === 'saving';
  const showSaveBar =
    state === 'marking' || state === 'saving' || state === 'error';

  return (
    <div
      className="flex flex-col"
      data-testid="attendance-roster"
      data-state={state}
    >
      <RosterHeader
        session={classSession}
        onMarkAllPresent={onMarkAllPresent}
        disabled={isLocked || isSaving}
      />

      {state === 'saved' && (
        <div
          data-testid="attendance-saved-badge"
          className="flex items-center gap-2 border-b bg-emerald-50 px-4 py-2 text-sm text-emerald-900"
          role="status"
        >
          <span aria-hidden="true">✓</span>
          <span>
            {COPY_VI.saved} — {classSession.teacherName}
          </span>
        </div>
      )}

      {state === 'error' && (
        <div
          data-testid="attendance-error-banner"
          role="alert"
          className="flex flex-wrap items-center justify-between gap-3 border-b border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive"
        >
          <span>{errorMessage ?? COPY_VI.error}</span>
          <button
            type="button"
            onClick={handleSave}
            className="inline-flex items-center justify-center rounded-md border border-destructive/40 bg-background px-3 py-1.5 text-sm font-medium text-destructive hover:bg-destructive/10 focus:outline-none focus:ring-2 focus:ring-destructive focus:ring-offset-2"
          >
            {COPY_VI.retry}
          </button>
        </div>
      )}

      <ul className="flex flex-col divide-y border-x border-b" role="list">
        {students.map((student) => (
          <StudentRow
            key={student.id}
            student={student}
            locked={isLocked || isSaving}
            onToggle={handleToggle}
          />
        ))}
      </ul>

      {showSaveBar && (
        <div
          data-testid="attendance-save-bar"
          role="region"
          aria-label="Thanh thao tác lưu điểm danh"
          className="sticky bottom-0 z-30 flex flex-wrap items-center justify-between gap-3 border-t bg-background/95 px-4 py-3 backdrop-blur"
        >
          <p className="text-sm text-muted-foreground" data-testid="attendance-dirty-count">
            {COPY_VI.changeCount(dirtyCount ?? 0)}
          </p>
          <button
            type="button"
            onClick={handleSave}
            disabled={isSaving}
            className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSaving ? COPY_VI.saving : COPY_VI.save}
          </button>
        </div>
      )}
    </div>
  );
}

type RosterHeaderProps = {
  session: AttendanceRosterProps['classSession'];
  onMarkAllPresent: (() => void) | undefined;
  disabled: boolean;
};

function RosterHeader({
  session,
  onMarkAllPresent,
  disabled,
}: RosterHeaderProps): React.JSX.Element {
  return (
    <header className="flex flex-wrap items-start justify-between gap-3 border-x border-t bg-card px-4 py-3">
      <div className="min-w-0 flex-1">
        <h2 className="truncate text-base font-semibold">
          {session.className}
        </h2>
        <p className="text-xs text-muted-foreground">
          Buổi #{session.sessionNumber} · {formatVietnameseDate(session.date)} ·{' '}
          {session.durationMinutes} phút
        </p>
      </div>
      {onMarkAllPresent && (
        <button
          type="button"
          onClick={onMarkAllPresent}
          disabled={disabled}
          className="inline-flex items-center justify-center rounded-md border bg-background px-3 py-2 text-sm font-medium hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {COPY_VI.markAllPresent}
        </button>
      )}
    </header>
  );
}

type StudentRowProps = {
  student: StudentRecord;
  locked: boolean;
  onToggle: (studentId: string, status: AttendanceStatus) => void;
};

function StudentRow({
  student,
  locked,
  onToggle,
}: StudentRowProps): React.JSX.Element {
  const groupLabelId = useId();
  return (
    <li
      data-testid={`attendance-row-${student.id}`}
      className="flex flex-wrap items-center justify-between gap-3 bg-background px-4 py-3"
    >
      <div className="min-w-0 flex-1">
        <p id={groupLabelId} className="truncate text-sm font-medium">
          {student.fullName}
        </p>
        <p className="text-xs text-muted-foreground">
          {student.studentCode} · Tỷ lệ chuyên cần {COPY_VI.rate(student.currentRate)}
        </p>
      </div>
      <div
        role="radiogroup"
        aria-labelledby={groupLabelId}
        aria-label={`Trạng thái cho ${student.fullName}`}
        className="flex items-center gap-1"
      >
        {STATUS_ORDER.map((status) => {
          const meta = COPY_VI.status[status];
          const active = student.status === status;
          return (
            <button
              key={status}
              type="button"
              role="radio"
              aria-checked={active}
              aria-label={meta.label}
              disabled={locked}
              onClick={() => onToggle(student.id, status)}
              className={`inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-md border px-3 py-2 text-sm font-semibold transition-opacity focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-70 ${
                active ? STATUS_ACTIVE_CLASS[status] : STATUS_INACTIVE_CLASS
              }`}
            >
              <span aria-hidden="true">{meta.glyph}</span>
              <span className="sr-only">{meta.label}</span>
            </button>
          );
        })}
      </div>
    </li>
  );
}

/**
 * Format date as `dd/MM/yyyy HH:mm` (Vietnamese convention, 24h).
 *
 * Avoids `Intl.DateTimeFormat` differences across runtimes by emitting the
 * expected pattern directly. Caller-provided `Date` is treated as local — the
 * component is presentation-only.
 */
function formatVietnameseDate(d: Date): string {
  const pad = (n: number): string => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default AttendanceRoster;
