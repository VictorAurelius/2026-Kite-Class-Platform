'use client';

/**
 * GradebookEntryGrid — G3 component (Wave 28 Bucket A).
 *
 * Subject teacher entry grid for VN 10-point grades. Vertical sticky header
 * row, sticky-left student column, per-cell save indicator, Excel-paste
 * import via clipboard TSV.
 *
 * Design source:
 *   documents/02-architecture/design-system/ui_kits/components/G3-gradebook-entry-grid/README.md
 *   documents/02-architecture/design-system/ui_kits/components/G3-gradebook-entry-grid/states/*.html
 *
 * State machine (top-level grid):
 *   loading → empty | default
 *   default → editing → saving → saved | error
 *   saved   → editing (re-edit)
 *
 * The component is **controlled** — caller owns `students[].grades`,
 * `cellStatuses`, `state`. Callbacks: `onCellChange`, `onSave`, `onBulkPaste`.
 *
 * Accessibility (WCAG AA):
 *  - `<table>` markup with sticky `<thead data-sticky="true">` so screen
 *    readers + scroll-aware browsers can pin the header.
 *  - Each grade cell is an `<input type="number">` with `aria-label`
 *    `"<column> cho <học sinh>"` so audio users hear context.
 *  - Cell-level errors set `aria-invalid="true"` + `aria-describedby` linking
 *    the inline error message (matches `validation-error.html` markup).
 *  - In-flight cells set `aria-busy="true"` for assistive tech.
 *  - Touch targets follow VN tablet primary surface (min 44×44 via padding).
 *  - Color-not-only: error has destructive red background AND text "Tối đa 10".
 *
 * Vietnamese-first per CLAUDE.md. All copy verbatim from spec HTML protos.
 */

import type React from 'react';
import { useCallback, useId, useMemo, useState } from 'react';
import type {
  GradebookEntryGridProps,
  GradebookCellStatus,
  GradebookStudent,
  GradeColumn,
} from './types';
import { parseExcelPaste } from './utils';

const COPY_VI = {
  studentCol: 'Học sinh',
  tbmCol: 'TBM',
  tbmHint: 'Tự tính',
  weightLabel: (w: 1 | 2 | 3): string => `Hệ số ${w}`,
  rangeHint: 'Thang điểm 0–10',
  emptyTitle: 'Chưa có cột điểm nào cho lớp này',
  emptyHint:
    'Tạo cột điểm để bắt đầu nhập. Ví dụ: Kiểm tra 15 phút, Kiểm tra 1 tiết, Cuối kỳ.',
  emptyCta: 'Thêm cột điểm',
  changeCount: (n: number): string => `${n} thay đổi`,
  save: 'Lưu',
  saveShortcut: 'Lưu (Ctrl+S)',
  saving: 'Đang lưu',
  cancel: 'Hủy',
  retry: 'Thử lại',
  savedBanner: (n: number): string =>
    `Đã lưu thành công ${n} điểm. Phụ huynh sẽ nhận thông báo qua Zalo OA trong 5 phút tới.`,
  savedBannerGeneric:
    'Đã lưu thành công. Phụ huynh sẽ nhận thông báo qua Zalo OA trong 5 phút tới.',
  savedTimestamp: (time: string, teacher: string): string =>
    `Đã lưu lúc ${time} bởi ${teacher}`,
  errorTitle: 'Không lưu được sổ điểm',
  cellErrorFallback: 'Điểm không hợp lệ',
};

export function GradebookEntryGrid(
  props: GradebookEntryGridProps,
): React.JSX.Element {
  const {
    session,
    columns,
    students,
    state,
    cellStatuses,
    onCellChange,
    onSave,
    onBulkPaste,
    dirtyCount,
    errorMessage,
    savedAt,
  } = props;

  // Index cell statuses by `${studentCode}::${columnId}` for O(1) lookup.
  const statusIndex = useMemo(() => {
    const map = new Map<string, GradebookCellStatus>();
    for (const s of cellStatuses ?? []) {
      map.set(`${s.studentCode}::${s.columnId}`, s);
    }
    return map;
  }, [cellStatuses]);

  // Loading skeleton
  if (state === 'loading') {
    return (
      <div
        data-testid="gradebook-loading"
        role="status"
        aria-live="polite"
        aria-label="Đang tải sổ điểm"
        className="flex flex-col gap-2 p-4"
      >
        <div className="h-12 w-full animate-pulse rounded-lg bg-muted/60" />
        {Array.from({ length: 6 }, (_, i) => (
          <div
            key={i}
            className="h-14 w-full animate-pulse rounded-lg bg-muted/40"
          />
        ))}
      </div>
    );
  }

  // Empty state — no columns OR no students
  if (state === 'empty' || columns.length === 0 || students.length === 0) {
    return (
      <div
        data-testid="gradebook-empty"
        className="flex flex-col items-center justify-center gap-4 rounded-2xl border-2 border-dashed bg-card/60 p-12 text-center"
      >
        <h2 className="text-xl font-bold">{COPY_VI.emptyTitle}</h2>
        <p className="max-w-md text-sm text-muted-foreground">
          {COPY_VI.emptyHint}
        </p>
        <button
          type="button"
          className="inline-flex items-center justify-center rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
        >
          {COPY_VI.emptyCta}
        </button>
      </div>
    );
  }

  const isSaving = state === 'saving';
  const isSaved = state === 'saved';
  const isError = state === 'error';
  const showSaveBar =
    state === 'editing' || state === 'saving' || state === 'error';

  return (
    <div
      className="flex flex-col"
      data-testid="gradebook-entry-grid"
      data-state={state}
    >
      <GradebookHeader session={session} />

      {isSaved && (
        <SavedBanner
          countSaved={dirtyCount ?? 0}
          savedAt={savedAt}
          teacherName={session.teacherName}
        />
      )}

      {isError && (
        <ErrorBanner
          message={errorMessage ?? COPY_VI.errorTitle}
          onRetry={onSave}
        />
      )}

      <div className="overflow-x-auto rounded-xl border bg-card shadow-sm">
        <table className="w-full text-sm" role="table">
          <thead
            data-testid="gradebook-thead"
            data-sticky="true"
            className="sticky top-0 z-10 bg-muted/40"
          >
            <tr className="border-b text-left">
              <th
                scope="col"
                className="sticky left-0 z-20 bg-muted/40 px-4 py-3 font-semibold w-64"
              >
                {COPY_VI.studentCol}
              </th>
              {columns.map((col) => (
                <th
                  key={col.id}
                  scope="col"
                  className="px-3 py-3 text-center font-semibold"
                >
                  <span className="block">{col.label}</span>
                  <span className="block text-[10px] font-normal text-muted-foreground">
                    {COPY_VI.weightLabel(col.weight)}
                  </span>
                </th>
              ))}
              <th
                scope="col"
                className="border-l bg-primary/5 px-3 py-3 text-center font-semibold"
              >
                <span className="block">{COPY_VI.tbmCol}</span>
                <span className="block text-[10px] font-normal text-muted-foreground">
                  {COPY_VI.tbmHint}
                </span>
              </th>
            </tr>
          </thead>
          <tbody>
            {students.map((stu) => (
              <StudentGradeRow
                key={stu.studentCode}
                student={stu}
                columns={columns}
                statusIndex={statusIndex}
                locked={isSaving || isSaved}
                onCellChange={onCellChange}
                onBulkPaste={onBulkPaste}
              />
            ))}
          </tbody>
        </table>
      </div>

      {showSaveBar && (
        <SaveBar
          dirtyCount={dirtyCount ?? 0}
          isSaving={isSaving}
          onSave={onSave}
        />
      )}
    </div>
  );
}

type GradebookHeaderProps = {
  session: GradebookEntryGridProps['session'];
};

function GradebookHeader({ session }: GradebookHeaderProps): React.JSX.Element {
  return (
    <header className="flex flex-wrap items-start justify-between gap-3 border-b bg-card px-4 py-3">
      <div className="min-w-0 flex-1">
        <p className="text-xs text-muted-foreground">{session.term}</p>
        <h1 className="truncate text-lg font-semibold">{session.className}</h1>
      </div>
      <p className="text-xs text-muted-foreground">{session.teacherName}</p>
    </header>
  );
}

type StudentGradeRowProps = {
  student: GradebookStudent;
  columns: ReadonlyArray<GradeColumn>;
  statusIndex: Map<string, GradebookCellStatus>;
  locked: boolean;
  onCellChange: GradebookEntryGridProps['onCellChange'];
  onBulkPaste: GradebookEntryGridProps['onBulkPaste'];
};

function StudentGradeRow({
  student,
  columns,
  statusIndex,
  locked,
  onCellChange,
  onBulkPaste,
}: StudentGradeRowProps): React.JSX.Element {
  return (
    <tr
      data-testid={`gradebook-row-${student.studentCode}`}
      className="border-b last:border-0"
    >
      <th
        scope="row"
        className="sticky left-0 z-10 bg-card px-4 py-3 text-left font-normal"
      >
        <p className="truncate font-medium">{student.fullName}</p>
        <p className="font-mono text-xs text-muted-foreground">
          {student.studentCode}
        </p>
      </th>
      {columns.map((col) => (
        <GradeCell
          key={col.id}
          studentCode={student.studentCode}
          studentName={student.fullName}
          column={col}
          value={student.grades[col.id]}
          status={statusIndex.get(`${student.studentCode}::${col.id}`)}
          locked={locked}
          onCellChange={onCellChange}
          onBulkPaste={onBulkPaste}
        />
      ))}
      <td className="border-l bg-primary/5 px-3 py-3 text-center font-semibold">
        <TbmCell student={student} columns={columns} />
      </td>
    </tr>
  );
}

type GradeCellProps = {
  studentCode: string;
  studentName: string;
  column: GradeColumn;
  value: number | undefined;
  status: GradebookCellStatus | undefined;
  locked: boolean;
  onCellChange: GradebookEntryGridProps['onCellChange'];
  onBulkPaste: GradebookEntryGridProps['onBulkPaste'];
};

function GradeCell({
  studentCode,
  studentName,
  column,
  value,
  status,
  locked,
  onCellChange,
  onBulkPaste,
}: GradeCellProps): React.JSX.Element {
  const errorId = useId();
  const [focused, setFocused] = useState(false);
  const cellState = status?.state ?? 'idle';
  const isError = cellState === 'error';
  const isSaving = cellState === 'saving';

  const handleBlur = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      setFocused(false);
      onCellChange(studentCode, column.id, e.currentTarget.value);
    },
    [onCellChange, studentCode, column.id],
  );

  const handlePaste = useCallback(
    (e: React.ClipboardEvent<HTMLInputElement>) => {
      if (!onBulkPaste) return;
      const text = e.clipboardData.getData('text/plain');
      // Only intercept multi-cell paste (TSV with tab OR multiple lines).
      // Single-cell paste continues to the input as normal.
      if (text.includes('\t') || /\r?\n/.test(text)) {
        e.preventDefault();
        const cells = parseExcelPaste(text);
        if (cells.length > 0) {
          onBulkPaste(cells);
        }
      }
    },
    [onBulkPaste],
  );

  return (
    <td
      data-cell-state={cellState}
      className="px-3 py-2 text-center"
    >
      <input
        data-testid={`gradebook-cell-${studentCode}-${column.id}`}
        type="text"
        inputMode="decimal"
        aria-label={`${column.label} cho ${studentName}`}
        aria-invalid={isError ? 'true' : undefined}
        aria-describedby={isError ? errorId : undefined}
        aria-busy={isSaving ? 'true' : undefined}
        defaultValue={value === undefined ? '' : String(value)}
        disabled={locked}
        onFocus={() => setFocused(true)}
        onBlur={handleBlur}
        onPaste={handlePaste}
        className={`grade-cell w-16 rounded-md border px-2 py-1.5 text-center font-semibold tabular-nums focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-70 ${
          isError
            ? 'border-destructive bg-destructive/5 text-destructive'
            : 'border-border bg-background'
        }`}
      />
      {focused && !isError && (
        <p className="mt-1 text-[10px] text-muted-foreground">
          {COPY_VI.rangeHint}
        </p>
      )}
      {isError && (
        <p
          id={errorId}
          className="mt-1 text-[10px] font-medium text-destructive"
        >
          {status?.error ?? COPY_VI.cellErrorFallback}
        </p>
      )}
    </td>
  );
}

type TbmCellProps = {
  student: GradebookStudent;
  columns: ReadonlyArray<GradeColumn>;
};

/**
 * Compute TBM (trung bình môn học) per Thông tư 22/2021/TT-BGDĐT
 * weighted-average rule:
 *   TBM = Σ(điểm_i × hệ_số_i) / Σ(hệ_số_i)
 * across cells that have a value. Cells with `undefined` are excluded from
 * both numerator and denominator.
 */
function TbmCell({ student, columns }: TbmCellProps): React.JSX.Element {
  let weightedSum = 0;
  let weightTotal = 0;
  for (const col of columns) {
    const v = student.grades[col.id];
    if (v === undefined) continue;
    weightedSum += v * col.weight;
    weightTotal += col.weight;
  }
  if (weightTotal === 0) {
    return <span className="text-muted-foreground">—</span>;
  }
  const tbm = weightedSum / weightTotal;
  return <span>{tbm.toFixed(1)}</span>;
}

type SavedBannerProps = {
  countSaved: number;
  savedAt: string | undefined;
  teacherName: string;
};

function SavedBanner({
  countSaved,
  savedAt,
  teacherName,
}: SavedBannerProps): React.JSX.Element {
  return (
    <div
      data-testid="gradebook-saved-banner"
      role="status"
      aria-live="polite"
      className="mb-3 flex flex-wrap items-center justify-between gap-3 rounded-lg border-l-4 border-emerald-600 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"
    >
      <p>
        <strong>
          {countSaved > 0
            ? COPY_VI.savedBanner(countSaved)
            : COPY_VI.savedBannerGeneric}
        </strong>
      </p>
      {savedAt && (
        <p className="text-xs text-muted-foreground">
          {COPY_VI.savedTimestamp(savedAt, teacherName)}
        </p>
      )}
    </div>
  );
}

type ErrorBannerProps = {
  message: string;
  onRetry: GradebookEntryGridProps['onSave'];
};

function ErrorBanner({ message, onRetry }: ErrorBannerProps): React.JSX.Element {
  return (
    <div
      data-testid="gradebook-error-banner"
      role="alert"
      className="mb-3 flex flex-wrap items-center justify-between gap-3 rounded-lg border-l-4 border-destructive bg-destructive/10 px-4 py-3 text-sm text-destructive"
    >
      <span>{message}</span>
      <button
        type="button"
        onClick={() => void onRetry()}
        className="inline-flex items-center justify-center rounded-md border border-destructive/40 bg-background px-3 py-1.5 text-sm font-medium text-destructive hover:bg-destructive/10 focus:outline-none focus:ring-2 focus:ring-destructive focus:ring-offset-2"
      >
        {COPY_VI.retry}
      </button>
    </div>
  );
}

type SaveBarProps = {
  dirtyCount: number;
  isSaving: boolean;
  onSave: GradebookEntryGridProps['onSave'];
};

function SaveBar({
  dirtyCount,
  isSaving,
  onSave,
}: SaveBarProps): React.JSX.Element {
  return (
    <div
      data-testid="gradebook-save-bar"
      role="region"
      aria-label="Thanh thao tác lưu sổ điểm"
      className="sticky bottom-0 z-30 mt-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border bg-card px-4 py-3 shadow-md backdrop-blur"
    >
      <p className="text-sm" data-testid="gradebook-dirty-count">
        <strong>{COPY_VI.changeCount(dirtyCount)}</strong>
      </p>
      <button
        type="button"
        onClick={() => void onSave()}
        disabled={isSaving}
        className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {isSaving ? COPY_VI.saving : COPY_VI.saveShortcut}
      </button>
    </div>
  );
}

export default GradebookEntryGrid;
