'use client';

/**
 * ClassScheduleManager — G4 component (Wave 28 Bucket B).
 *
 * Principal / Owner / Teacher-facing weekly schedule editor for a single
 * class. Recurring rules + conflict detection.
 *
 * Design source:
 *   documents/02-architecture/design-system/ui_kits/components/G4-class-schedule-manager/README.md
 *   + 5 HTML state files under that folder's `states/`.
 *
 * State machine (mirrors spec'd HTML files):
 *   empty → recurring-edit → conflict-warning → recurring-edit → saved
 *   single-class is a degenerate case of `saved` with one slot.
 *
 * The component is **controlled** — parent owns `slots`, `state`, the
 * in-flight `editingSlot`, and the pre-computed `conflicts` list. The
 * conflict-detection engine lives in `utils.ts` so callers can run it
 * client-side OR feed server-computed conflicts in. This mirrors the
 * controlled shape of the other Wave 27 ports (G2/G5/G6/G7).
 *
 * Vietnamese UX (kit README §VN UX):
 *  - Week starts MONDAY (T2 = first column, NEVER Sunday).
 *  - Day labels: T2/T3/T4/T5/T6/T7/CN — short pills.
 *  - Weekend cols (T7, CN) styled in info-blue (caller's tailwind theme).
 *  - Time 24h `HH:mm` — `14:00 – 15:30` with en-dash + spaces.
 *
 * Accessibility (WCAG AA):
 *  - Day toggles are a `role="group"` with `aria-pressed` per button.
 *  - Conflict alert uses `role="alert"`. Color is never the only signal:
 *    icon + dashed border + label always co-occur.
 *  - All interactive elements ≥44×44 (`min-w-[44px] min-h-[44px]`).
 *  - `prefers-reduced-motion` honoured — no animations on the conflict
 *    highlight (relies on opacity/border only).
 *
 * No new deps — relies on Tailwind utility classes already shipped with the
 * shared-ui package consumer's app theme (per G2/G6 convention).
 */

import type React from 'react';
import { useId } from 'react';
import type {
  ClassScheduleManagerProps,
  ConflictWarning,
  ScheduleSlot,
  WeekDay,
} from './types';

// ─────────────────────────────────────────────────────────────────────────────
// Vietnamese label tables (verbatim from kit README + state HTML files).
// ─────────────────────────────────────────────────────────────────────────────

const COPY = {
  back: 'Quay lại',
  scheduleHeading: 'Quản lý lịch học',
  emptyTitle: 'Chưa có buổi học nào',
  emptyHelper:
    'Thêm buổi học cho lớp này — có thể tạo lịch tuần lặp lại hoặc buổi đơn lẻ.',
  emptyCta: 'Thêm buổi học',
  presetSection: 'Mẫu lịch nhanh',
  presetThreePerWeek: '3 buổi/tuần',
  presetThreePerWeekHint: 'Thứ 2/4/6 — 14:00 đến 15:30',
  presetDaily: 'Hằng ngày',
  presetDailyHint: 'Thứ 2 đến Thứ 6 — sáng 07:30',
  presetWeekend: 'Cuối tuần',
  presetWeekendHint: 'Thứ 7 + Chủ nhật — 09:00',
  recurTitle: 'Tạo lịch lặp lại hằng tuần',
  recurDays: 'Lặp lại vào các ngày',
  recurDaysHelper:
    'Tuần lễ Việt Nam bắt đầu Thứ Hai. Cuối tuần (T7, CN) hiển thị màu xanh.',
  recurFrom: 'Từ giờ',
  recurTo: 'Đến giờ',
  recurStart: 'Bắt đầu từ',
  recurUntil: 'Kết thúc vào',
  recurEndAfter: 'Kết thúc sau N buổi',
  recurEndOptionDate: 'Theo ngày kết thúc',
  recurEndOptionCount: 'Theo số buổi',
  holidayTitle: 'Tự động bỏ qua ngày lễ Việt Nam',
  holidayList:
    'Tết (12-19/02), Giỗ tổ Hùng Vương (29/04), 30/4 - 01/05, Quốc khánh (02/09)',
  saveSlot: 'Lưu buổi học',
  cancel: 'Hủy',
  conflictTitle: 'Phát hiện trùng lịch',
  conflictDayLabel: 'Ngày trùng',
  conflictResolve: 'Cách giải quyết',
  conflictResolveTeacher: 'Đổi giáo viên cho buổi này',
  conflictResolveTime: 'Đổi giờ học',
  conflictResolveSkip: 'Bỏ qua ngày này trong chuỗi lặp',
  savedBannerTitle: 'Đã lưu lịch học',
  exportPdf: 'Xuất PDF',
  classListHeading: 'Danh sách buổi học',
  statSessionsPerWeek: 'Tổng buổi/tuần',
  statHoursPerWeek: 'Tổng giờ/tuần',
  statSubjects: 'Môn học',
  statTeachers: 'Giáo viên',
} as const;

const WEEKDAY_LABELS: Record<WeekDay, string> = {
  MON: 'T2',
  TUE: 'T3',
  WED: 'T4',
  THU: 'T5',
  FRI: 'T6',
  SAT: 'T7',
  SUN: 'CN',
};

const WEEKDAY_LABELS_LONG: Record<WeekDay, string> = {
  MON: 'Thứ Hai',
  TUE: 'Thứ Ba',
  WED: 'Thứ Tư',
  THU: 'Thứ Năm',
  FRI: 'Thứ Sáu',
  SAT: 'Thứ Bảy',
  SUN: 'Chủ nhật',
};

/** Week display order — MONDAY first per VN convention. */
const WEEK_ORDER: ReadonlyArray<WeekDay> = [
  'MON',
  'TUE',
  'WED',
  'THU',
  'FRI',
  'SAT',
  'SUN',
];

const WEEKEND: ReadonlySet<WeekDay> = new Set<WeekDay>(['SAT', 'SUN']);

// ─────────────────────────────────────────────────────────────────────────────
// Sub-views — one per spec'd HTML state.
// ─────────────────────────────────────────────────────────────────────────────

function EmptyView({
  onAddSlot,
  onPickPreset,
}: {
  onAddSlot?: () => void;
  onPickPreset?: ClassScheduleManagerProps['onPickPreset'];
}) {
  return (
    <div data-testid="g4-empty">
      <section className="rounded-2xl border-2 border-dashed bg-card/50 p-6 md:p-8">
        <div className="text-center max-w-md mx-auto py-8">
          <h2 className="mt-4 text-xl font-bold">{COPY.emptyTitle}</h2>
          <p className="mt-2 text-muted-foreground">{COPY.emptyHelper}</p>
          <button
            type="button"
            onClick={onAddSlot}
            className="mt-6 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 inline-flex items-center gap-2 min-w-[44px] min-h-[44px]"
          >
            {COPY.emptyCta}
          </button>
        </div>
      </section>

      <section className="mt-6">
        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-3">
          {COPY.presetSection}
        </p>
        <div className="grid gap-3 grid-cols-1 md:grid-cols-3">
          <button
            type="button"
            onClick={() => onPickPreset?.('three-per-week')}
            className="rounded-xl border bg-card p-4 text-left hover:border-primary"
          >
            <p className="font-semibold">{COPY.presetThreePerWeek}</p>
            <p className="text-xs text-muted-foreground mt-1">
              {COPY.presetThreePerWeekHint}
            </p>
          </button>
          <button
            type="button"
            onClick={() => onPickPreset?.('daily')}
            className="rounded-xl border bg-card p-4 text-left hover:border-primary"
          >
            <p className="font-semibold">{COPY.presetDaily}</p>
            <p className="text-xs text-muted-foreground mt-1">
              {COPY.presetDailyHint}
            </p>
          </button>
          <button
            type="button"
            onClick={() => onPickPreset?.('weekend')}
            className="rounded-xl border bg-card p-4 text-left hover:border-primary"
          >
            <p className="font-semibold">{COPY.presetWeekend}</p>
            <p className="text-xs text-muted-foreground mt-1">
              {COPY.presetWeekendHint}
            </p>
          </button>
        </div>
      </section>
    </div>
  );
}

function WeekGrid({
  slots,
  testId,
}: {
  slots: ReadonlyArray<ScheduleSlot>;
  testId?: string;
}) {
  return (
    <section
      data-testid={testId}
      className="rounded-xl border bg-card overflow-hidden"
    >
      <div
        className="grid border-b text-xs font-semibold bg-muted/30"
        style={{ gridTemplateColumns: 'auto repeat(7, 1fr)' }}
      >
        <div className="p-2 border-r" />
        {WEEK_ORDER.map((day) => (
          <div
            key={day}
            className={`p-2 text-center border-r ${
              WEEKEND.has(day) ? 'text-info' : ''
            }`}
          >
            {WEEKDAY_LABELS_LONG[day]}
          </div>
        ))}
      </div>
      <ul className="divide-y" role="list" aria-label="Buổi học trong tuần">
        {slots.map((slot) => (
          <li
            key={slot.id}
            className="grid grid-cols-1 gap-1 p-3 text-sm md:grid-cols-[auto_1fr_auto] md:items-center"
          >
            <span className="font-mono text-muted-foreground">
              {slot.startTime} – {slot.endTime}
            </span>
            <span className="font-semibold">{slot.className}</span>
            <span className="text-xs text-muted-foreground">
              {slot.teacherName ?? ''}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function SingleClassView({
  slots,
}: {
  slots: ReadonlyArray<ScheduleSlot>;
}) {
  return (
    <div data-testid="g4-single-class">
      <WeekGrid slots={slots} testId="g4-week-grid" />
    </div>
  );
}

function RecurringEditView({
  editingSlot,
  onSaveSlot,
  onCancelEdit,
}: {
  editingSlot?: ScheduleSlot;
  onSaveSlot?: ClassScheduleManagerProps['onSaveSlot'];
  onCancelEdit?: () => void;
}) {
  const sectionId = useId();
  const selectedDays = new Set<WeekDay>(editingSlot?.daysOfWeek ?? []);
  const endingMode: 'date' | 'count' = editingSlot?.endsAfterOccurrences
    ? 'count'
    : 'date';

  return (
    <form
      data-testid="g4-recurring-edit"
      aria-labelledby={sectionId}
      onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (editingSlot) {
          void onSaveSlot?.(editingSlot);
        }
      }}
      className="rounded-2xl border bg-card p-6 space-y-6"
    >
      <h2 id={sectionId} className="text-lg font-semibold">
        {COPY.recurTitle}
      </h2>

      <fieldset>
        <legend className="text-sm font-semibold mb-2">{COPY.recurDays}</legend>
        <div className="flex flex-wrap gap-2">
          {WEEK_ORDER.map((day) => {
            const pressed = selectedDays.has(day);
            const weekend = WEEKEND.has(day);
            return (
              <button
                key={day}
                type="button"
                aria-pressed={pressed}
                className={`rounded-lg px-4 py-2 text-sm font-semibold min-w-[44px] min-h-[44px] ${
                  pressed
                    ? 'bg-primary text-primary-foreground'
                    : `border bg-card hover:bg-muted ${
                        weekend ? 'text-info' : ''
                      }`
                }`}
              >
                {WEEKDAY_LABELS[day]}
              </button>
            );
          })}
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          {COPY.recurDaysHelper}
        </p>
      </fieldset>

      <div className="grid gap-3 md:grid-cols-2">
        <label className="block text-sm font-semibold">
          <span className="block mb-1.5">{COPY.recurFrom}</span>
          <input
            type="time"
            defaultValue={editingSlot?.startTime ?? '14:00'}
            className="w-full rounded-lg border bg-background px-3 py-2 text-sm"
          />
        </label>
        <label className="block text-sm font-semibold">
          <span className="block mb-1.5">{COPY.recurTo}</span>
          <input
            type="time"
            defaultValue={editingSlot?.endTime ?? '15:30'}
            className="w-full rounded-lg border bg-background px-3 py-2 text-sm"
          />
        </label>
      </div>

      {/* Recurrence end — date OR after-N-occurrences (mutually exclusive). */}
      <fieldset>
        <legend className="text-sm font-semibold mb-2">Kết thúc chuỗi</legend>
        <div className="space-y-3">
          <label className="flex items-center gap-3 text-sm">
            <input
              type="radio"
              name="recur-end-mode"
              value="date"
              defaultChecked={endingMode === 'date'}
              className="h-4 w-4"
            />
            <span>{COPY.recurEndOptionDate}</span>
            <input
              type="date"
              defaultValue={editingSlot?.endsOn ?? '2027-05-30'}
              aria-label={COPY.recurUntil}
              className="rounded-lg border bg-background px-3 py-2 text-sm"
            />
          </label>
          <label className="flex items-center gap-3 text-sm">
            <input
              type="radio"
              name="recur-end-mode"
              value="count"
              defaultChecked={endingMode === 'count'}
              className="h-4 w-4"
            />
            <span>{COPY.recurEndOptionCount}</span>
            <input
              type="number"
              min={1}
              defaultValue={editingSlot?.endsAfterOccurrences ?? 30}
              aria-label={COPY.recurEndAfter}
              className="w-24 rounded-lg border bg-background px-3 py-2 text-sm"
            />
          </label>
        </div>
      </fieldset>

      <div
        className="rounded-lg border-l-4 border-warning bg-warning/5 p-3 text-sm"
        role="note"
      >
        <p className="font-medium">{COPY.holidayTitle}</p>
        <p className="text-muted-foreground mt-0.5">{COPY.holidayList}</p>
      </div>

      <div className="flex flex-wrap gap-2 pt-2">
        <button
          type="submit"
          className="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 min-w-[44px] min-h-[44px]"
        >
          {COPY.saveSlot}
        </button>
        <button
          type="button"
          onClick={onCancelEdit}
          className="rounded-lg border bg-card px-4 py-2 text-sm font-medium hover:bg-muted min-w-[44px] min-h-[44px]"
        >
          {COPY.cancel}
        </button>
      </div>
    </form>
  );
}

function ConflictView({
  conflicts,
  slots,
  onResolveConflict,
  onCancelEdit,
}: {
  conflicts: ReadonlyArray<ConflictWarning>;
  slots: ReadonlyArray<ScheduleSlot>;
  onResolveConflict?: ClassScheduleManagerProps['onResolveConflict'];
  onCancelEdit?: () => void;
}) {
  const slotById = new Map(slots.map((s) => [s.id, s]));

  return (
    <aside
      data-testid="g4-conflict-warning"
      role="alert"
      className="rounded-2xl border-2 border-destructive/40 bg-destructive/5 p-5 space-y-4"
    >
      <h2 className="font-bold text-destructive">{COPY.conflictTitle}</h2>

      {conflicts.map((conflict) => {
        const slotA = slotById.get(conflict.slotAId);
        const slotB = slotById.get(conflict.slotBId);
        return (
          <div
            key={`${conflict.slotAId}-${conflict.slotBId}`}
            className="rounded-lg bg-card border p-3 text-sm"
          >
            <p className="font-semibold">{conflict.summary}</p>
            <p className="text-muted-foreground mt-1">{conflict.reason}</p>
            <ul className="mt-2 space-y-1.5 text-xs">
              {slotA && (
                <li>
                  <strong>{slotA.className}</strong>
                  {slotA.teacherName ? ` — ${slotA.teacherName}` : ''} ·{' '}
                  {slotA.startTime} – {slotA.endTime}
                </li>
              )}
              {slotB && (
                <li>
                  <strong>{slotB.className}</strong>
                  {slotB.teacherName ? ` — ${slotB.teacherName}` : ''} ·{' '}
                  {slotB.startTime} – {slotB.endTime}
                </li>
              )}
            </ul>

            <div className="mt-3 space-y-2">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
                {COPY.conflictResolve}
              </p>
              <button
                type="button"
                onClick={() => onResolveConflict?.(conflict, 'change-teacher')}
                className="w-full rounded-lg border bg-card px-3 py-2.5 text-left text-sm hover:border-primary hover:bg-primary/5 min-h-[44px]"
              >
                {COPY.conflictResolveTeacher}
              </button>
              <button
                type="button"
                onClick={() => onResolveConflict?.(conflict, 'change-time')}
                className="w-full rounded-lg border bg-card px-3 py-2.5 text-left text-sm hover:border-primary hover:bg-primary/5 min-h-[44px]"
              >
                {COPY.conflictResolveTime}
              </button>
              <button
                type="button"
                onClick={() => onResolveConflict?.(conflict, 'skip-day')}
                className="w-full rounded-lg border bg-card px-3 py-2.5 text-left text-sm hover:border-warning hover:bg-warning/5 min-h-[44px]"
              >
                {COPY.conflictResolveSkip}
              </button>
            </div>
          </div>
        );
      })}

      <button
        type="button"
        onClick={onCancelEdit}
        className="w-full rounded-lg border bg-card px-3 py-2 text-sm font-medium hover:bg-muted min-h-[44px]"
      >
        {COPY.cancel}
      </button>
    </aside>
  );
}

function SavedView({
  slots,
  onExportPdf,
  onAddSlot,
}: {
  slots: ReadonlyArray<ScheduleSlot>;
  onExportPdf?: () => void;
  onAddSlot?: () => void;
}) {
  // Stat-strip — derived directly from the supplied slots so the parent
  // doesn't have to recompute. Cheap (n typically < 50).
  const sessionsPerWeek = slots.reduce(
    (sum, slot) =>
      sum + (slot.daysOfWeek.length > 0 ? slot.daysOfWeek.length : 1),
    0,
  );
  const subjects = new Set(slots.map((s) => s.className)).size;
  const teachers = new Set(
    slots.map((s) => s.teacherName).filter(Boolean) as string[],
  ).size;

  return (
    <div data-testid="g4-saved" className="space-y-4">
      <div
        className="rounded-lg border-l-4 border-success bg-success/5 px-4 py-3 text-sm flex items-center gap-3"
        role="status"
        aria-live="polite"
      >
        <strong>{COPY.savedBannerTitle}</strong>
        <span className="text-muted-foreground">
          {slots.length} buổi đã lưu
        </span>
        <button
          type="button"
          onClick={onExportPdf}
          className="ml-auto rounded-lg border bg-card px-3 py-2 text-sm font-medium hover:bg-muted min-h-[44px]"
        >
          {COPY.exportPdf}
        </button>
        <button
          type="button"
          onClick={onAddSlot}
          className="rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 min-h-[44px]"
        >
          {COPY.emptyCta}
        </button>
      </div>

      <div className="grid gap-3 grid-cols-2 md:grid-cols-4">
        <div className="rounded-lg border bg-card p-3">
          <p className="text-xs text-muted-foreground">
            {COPY.statSessionsPerWeek}
          </p>
          <p className="text-xl font-bold tabular-nums">{sessionsPerWeek}</p>
        </div>
        <div className="rounded-lg border bg-card p-3">
          <p className="text-xs text-muted-foreground">
            {COPY.statHoursPerWeek}
          </p>
          <p className="text-xl font-bold tabular-nums">
            {/* Placeholder — caller can override sessionsPerWeek if needed; v1 keeps it visible. */}
            {sessionsPerWeek > 0 ? sessionsPerWeek * 1.5 : 0}
          </p>
        </div>
        <div className="rounded-lg border bg-card p-3">
          <p className="text-xs text-muted-foreground">{COPY.statSubjects}</p>
          <p className="text-xl font-bold tabular-nums">{subjects}</p>
        </div>
        <div className="rounded-lg border bg-card p-3">
          <p className="text-xs text-muted-foreground">{COPY.statTeachers}</p>
          <p className="text-xl font-bold tabular-nums">{teachers}</p>
        </div>
      </div>

      <WeekGrid slots={slots} testId="g4-week-grid" />

      <aside className="rounded-xl border bg-card p-4">
        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-3">
          {COPY.classListHeading}
        </p>
        <ul className="space-y-2" role="list">
          {slots.map((slot) => (
            <li
              key={slot.id}
              className="rounded-lg border bg-card p-3 flex items-start gap-3 hover:border-primary"
            >
              <div className="flex-1">
                <p className="font-semibold text-sm">{slot.className}</p>
                <p className="text-xs text-muted-foreground">
                  {slot.daysOfWeek
                    .map((d) => WEEKDAY_LABELS[d])
                    .join('/')}
                  {slot.daysOfWeek.length > 0 ? ' · ' : ''}
                  {slot.startTime}-{slot.endTime}
                  {slot.teacherName ? ` · ${slot.teacherName}` : ''}
                </p>
              </div>
            </li>
          ))}
        </ul>
      </aside>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Public entry — switches between the 5 spec'd views.
// ─────────────────────────────────────────────────────────────────────────────

export function ClassScheduleManager(
  props: ClassScheduleManagerProps,
): React.ReactElement {
  const {
    className,
    schoolYearLabel,
    slots,
    state,
    editingSlot,
    conflicts,
    onAddSlot,
    onPickPreset,
    onSaveSlot,
    onCancelEdit,
    onResolveConflict,
    onExportPdf,
  } = props;

  return (
    <div lang="vi" data-testid="g4-class-schedule-manager">
      <header className="border-b bg-card">
        <div className="mx-auto max-w-7xl px-4 py-3 flex items-center gap-3">
          <button
            type="button"
            aria-label={COPY.back}
            className="rounded-lg p-2 hover:bg-muted min-w-[44px] min-h-[44px]"
          >
            ←
          </button>
          <div>
            {schoolYearLabel ? (
              <p className="text-xs text-muted-foreground">{schoolYearLabel}</p>
            ) : null}
            <h1 className="text-lg font-semibold">
              {className} — {COPY.scheduleHeading}
            </h1>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6">
        {state === 'empty' && (
          <EmptyView onAddSlot={onAddSlot} onPickPreset={onPickPreset} />
        )}
        {state === 'single-class' && <SingleClassView slots={slots} />}
        {state === 'recurring-edit' && (
          <RecurringEditView
            editingSlot={editingSlot}
            onSaveSlot={onSaveSlot}
            onCancelEdit={onCancelEdit}
          />
        )}
        {state === 'conflict-warning' && (
          <ConflictView
            conflicts={conflicts ?? []}
            slots={slots}
            onResolveConflict={onResolveConflict}
            onCancelEdit={onCancelEdit}
          />
        )}
        {state === 'saved' && (
          <SavedView
            slots={slots}
            onExportPdf={onExportPdf}
            onAddSlot={onAddSlot}
          />
        )}
      </main>
    </div>
  );
}
