/**
 * Type definitions for G1 Bulk Import Drop-zone + Job Tracker.
 *
 * Mirrors `ui_kits/components/G1-bulk-import-dropzone/README.md` + 5 state HTML
 * files (`states/{idle,drag-over,parsing,partial-success,done}.html`) and
 * `dossier/04-component-gaps.md` §G1.
 *
 * Use case: P3 Medium Center Admin / P5 K-12 Principal imports up to 500
 * students/day during enrollment week. CSV format
 * `ho_va_ten,ngay_sinh,lop,phu_huynh_phone`. Constraints: ≤ 5 MB, ≤ 10.000
 * rows, batch insert 500/txn.
 *
 * Two abstractions live here:
 *  - `ImportJobStatus` — page-level UI state matching the 5 spec'd HTML states
 *    plus an `'error'` terminal state for upload-time failures.
 *  - `ImportRow` / `ImportError` — per-row data model produced by `parseCSV`
 *    + `validateRow`.
 */

/**
 * Page-level lifecycle state — drives top banner + body content.
 *
 * Maps directly to the 5 spec'd HTML state files (idle / drag-over / parsing /
 * partial-success / done) plus a synthetic `'error'` state for upload-time
 * failures (file too big, wrong format) that have no spec'd HTML but which the
 * caller still needs to render.
 */
export type ImportJobStatus =
  | 'idle'
  | 'drag-over'
  | 'parsing'
  | 'partial-success'
  | 'done'
  | 'error';

/**
 * Schema kind for `validateRow` — currently only `'students'` is implemented;
 * leaves the door open to future schemas (`'teachers'`, `'classes'`).
 */
export type ImportSchema = 'students';

/**
 * A single parsed row from a CSV / Excel file. Keys are the canonical column
 * names per `README.md` §Use case. Values are kept as strings — `validateRow`
 * is the layer that reasons about them.
 */
export type ImportRow = {
  /** Vietnamese full name (e.g. `Nguyễn Văn An`). */
  ho_va_ten: string;
  /** Date of birth in `dd/MM/yyyy` format (e.g. `15/08/2015`). */
  ngay_sinh: string;
  /** Class name (e.g. `Lớp 6A1`). */
  lop: string;
  /** Parent phone — 10–11 digits starting with `0`. */
  phu_huynh_phone: string;
  /**
   * Source row number in the file (1-based, INCLUDING header — so the first
   * data row reports `row: 2`). Used to surface "Dòng N" error labels.
   */
  row: number;
};

/**
 * A validation or parse error attached to one source row.
 *
 * Localized in Vietnamese per `README.md` §VN UX:
 *   `Dòng 23: Số điện thoại không hợp lệ`
 *   `Dòng 47: Ngày sinh sai định dạng`
 */
export type ImportError = {
  /** 1-based source row number (matches `ImportRow.row`). */
  row: number;
  /** Vietnamese error message — caller-ready, no further i18n needed. */
  message: string;
  /**
   * Optional column key the error attaches to (e.g. `phu_huynh_phone`).
   * `undefined` for whole-row errors (e.g. missing column count).
   */
  field?: keyof Omit<ImportRow, 'row'>;
};

/**
 * Live progress for the parsing/committing phase.
 *
 * Drives the `<progressbar>` + ETA copy. Caller updates this as the job runs;
 * component is presentational.
 */
export type JobProgress = {
  /** Rows processed so far. */
  processed: number;
  /** Total rows expected (parsed file row count, excluding header). */
  total: number;
  /** Optional human-readable ETA, e.g. `'12 giây'`. */
  etaLabel?: string;
  /**
   * Optional sub-step caption visible under the progress bar, e.g.
   * `'Đang kiểm tra trùng SĐT phụ huynh'`.
   */
  stepLabel?: string;
};

/**
 * Final aggregate result after parsing + validation. Caller computes this and
 * passes it in for partial-success / done states.
 */
export type ImportSummary = {
  /** Rows that passed validation and will be committed. */
  validCount: number;
  /** Rows with errors (will not be committed unless caller forces). */
  errorCount: number;
  /** Rows skipped because already exist (matched by parent phone, etc.). */
  duplicateCount: number;
  /** Per-row errors (typically truncated to first ~10 in UI; full set in download). */
  errors: ImportError[];
};

/**
 * Optional callbacks the caller hooks into for side-effects.
 *
 * The component is presentational — it does NOT actually upload, parse, or
 * commit. The host app:
 *   1. Listens for `onFileSelect` (drag-drop or click-to-browse).
 *   2. Reads the file, calls `parseCSV` + per-row `validateRow`, computes
 *      `JobProgress` + `ImportSummary`, and passes them back via props.
 *   3. Listens for `onCommit` to fire the actual batch-insert mutation.
 *   4. Listens for `onCancel` / `onClose` to dismiss / back-out.
 *   5. Listens for `onSampleDownload` / `onErrorDownload` for the two
 *      download CTAs.
 */
export type BulkImportDropzoneProps = {
  /** Current page-level status; drives which body to render. */
  status: ImportJobStatus;
  /** Tenant / context label shown in the header (e.g. centre name). */
  tenantLabel?: string;
  /** Sub-context label shown in the header (e.g. `Tuần tuyển sinh tháng 8/2026`). */
  contextLabel?: string;
  /**
   * Filename being processed — required for `parsing` / `partial-success` /
   * `done` states; ignored otherwise.
   */
  fileName?: string;
  /** Live progress data — required for `parsing`, ignored otherwise. */
  progress?: JobProgress;
  /** Aggregate summary — required for `partial-success` and `done`. */
  summary?: ImportSummary;
  /** Free-form error message for `'error'` state. Vietnamese. */
  errorMessage?: string;
  /**
   * Maximum file size in bytes. Defaults to 5 MB (5 × 1024 × 1024) per
   * `README.md` §Use case. Files larger than this are rejected at
   * `onFileSelect` time before the component yields control back to caller.
   */
  maxFileSize?: number;
  /**
   * Maximum row count. Defaults to 10_000 per spec. When `summary.validCount +
   * summary.errorCount + summary.duplicateCount` exceeds this, the partial-
   * success state shows an extra warning banner.
   */
  maxRows?: number;
  /** Required column names for the schema. Defaults to the students schema. */
  requiredColumns?: readonly string[];
  /** Callback when a file is dropped or selected via the file input. */
  onFileSelect?: (file: File) => void;
  /** Callback when the user clicks the "Tiếp tục" / "Đóng" commit CTA. */
  onCommit?: () => void;
  /** Callback for the Cancel button during parsing. */
  onCancel?: () => void;
  /** Callback for the Close button (header X). */
  onClose?: () => void;
  /** Callback for "Tải file mẫu (.xlsx)". */
  onSampleDownload?: () => void;
  /** Callback for "Tải file lỗi (.xlsx)". */
  onErrorDownload?: () => void;
  /** Override `lang` attribute. Defaults to `'vi'`. */
  lang?: 'vi' | 'en';
};
