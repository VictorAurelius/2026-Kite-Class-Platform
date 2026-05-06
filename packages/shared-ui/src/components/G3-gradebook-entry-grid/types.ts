/**
 * Type definitions for G3 Gradebook Entry Grid.
 *
 * Mirrors `ui_kits/components/G3-gradebook-entry-grid/README.md` + the 6
 * HTML state files. See also: `dossier/04-component-gaps.md` §G3.
 *
 * VN 10-point scale (Thông tư 22/2021/TT-BGDĐT):
 *   - 0.0 .. 10.0 with **max 1 decimal place** (per `validation-error.html`
 *     rules card: "8.5 hợp lệ; 9.25 không cho phép").
 *   - VN convention also accepts comma as decimal separator: "7,5".
 */

/**
 * Validated VN grade (0..10, max 1 decimal). `undefined` means "not yet
 * entered" (cell renders as `—`). Component consumers store this shape per
 * cell.
 */
export type GradeValue = number | undefined;

/** Lifecycle state for a single grade cell. */
export type GradebookCellState =
  | 'idle'
  | 'editing'
  | 'dirty'
  | 'saving'
  | 'saved'
  | 'error';

/** A single grade cell coordinate in the grid. */
export type GradebookCell = {
  /** Student MST (mã số học sinh, e.g. `HS-10A2-001`). */
  studentCode: string;
  /** Raw clipboard value (still a string — caller validates via `validateGrade`). */
  rawValue: string;
};

/** Class session metadata rendered in the gradebook header. */
export type GradebookSession = {
  /** Display title, e.g. `Lớp 10A2 — Toán nâng cao`. */
  className: string;
  /** Term + year line, e.g. `Học kỳ I · 2026-2027`. */
  term: string;
  /** Teacher's full name (Vietnamese honorific included), e.g. `Cô Nguyễn Thị Lan`. */
  teacherName: string;
};

/**
 * Per-MoET grade-column "Hệ số" weight.
 *
 * - 1 = KT 15 phút (max 4 columns)
 * - 2 = KT 1 tiết / Giữa kỳ (max 3 columns)
 * - 3 = Cuối kỳ (1 column only)
 */
export type GradeWeight = 1 | 2 | 3;

/** A grade-column definition per MoET regulation. */
export type GradeColumn = {
  /** Column id, stable across renders. */
  id: string;
  /** Header label, e.g. `KT 15'`, `Cuối kỳ`. */
  label: string;
  /** "Hệ số" weight per Thông tư 22/2021/TT-BGDĐT. */
  weight: GradeWeight;
};

/** A student row in the grid. */
export type GradebookStudent = {
  studentCode: string;
  fullName: string;
  /**
   * Map of column id → grade. `undefined` means the cell hasn't been entered.
   * (Use `Record<string, GradeValue>` so callers can spread-assign cleanly.)
   */
  grades: Record<string, GradeValue>;
};

/**
 * Per-(student × column) cell save indicator.
 *
 * The grid is **controlled** — caller owns dirty-tracking + persistence and
 * tells the grid which cells are saving / saved / error so the per-cell
 * indicator can render. Same shape as G2 AttendanceRoster's controlled props.
 */
export type GradebookCellStatus = {
  studentCode: string;
  columnId: string;
  state: GradebookCellState;
  /** Vietnamese error message, surfaced when `state === 'error'`. */
  error?: string;
};

/** Top-level lifecycle of the whole gradebook grid (mirrors AttendanceRoster). */
export type GradebookGridState =
  | 'loading'
  | 'empty'
  | 'default'
  | 'editing'
  | 'saving'
  | 'saved'
  | 'error';

export type GradebookEntryGridProps = {
  session: GradebookSession;
  columns: ReadonlyArray<GradeColumn>;
  students: ReadonlyArray<GradebookStudent>;
  /** Top-level state of the grid (loading / empty / default / saving / ...). */
  state: GradebookGridState;
  /** Per-cell save indicators. Default: all cells `idle`. */
  cellStatuses?: ReadonlyArray<GradebookCellStatus>;
  /**
   * Fired when a cell finishes editing with a (possibly invalid) raw value.
   * Caller validates via `validateGrade` and decides whether to persist.
   */
  onCellChange: (
    studentCode: string,
    columnId: string,
    rawValue: string,
  ) => void;
  /** Fired when teacher hits the Save action (Ctrl+S or footer button). */
  onSave: () => void | Promise<void>;
  /**
   * Fired when teacher pastes Excel TSV. Caller decides matching strategy
   * (by MST, by row index, etc.).
   */
  onBulkPaste?: (cells: ReadonlyArray<GradebookCell>) => void;
  /** Total dirty cells; rendered in the save bar. */
  dirtyCount?: number;
  /** Save-failure message rendered when `state === 'error'`. */
  errorMessage?: string;
  /**
   * "Đã lưu lúc HH:MM bởi <Teacher>" timestamp. Caller-formatted (component
   * stays presentation-only — no `Date` formatting beyond `padStart`).
   */
  savedAt?: string;
};

/** Result shape returned by `validateGrade`. */
export type ValidateGradeResult = {
  valid: boolean;
  value?: number;
  error?: string;
};
