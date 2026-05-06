'use client';

/**
 * G1 — Bulk Import Drop-zone + Job Tracker.
 *
 * Replaces the missing FE entry point for KC `/students` (Nhập danh sách)
 * per `dossier/04-component-gaps.md` §G1 +
 * `ui_kits/components/G1-bulk-import-dropzone/README.md` + 5 spec'd HTML
 * state files (`states/{idle,drag-over,parsing,partial-success,done}.html`).
 *
 * The component is presentational: it does NOT actually upload, parse, or
 * commit. The host app:
 *   1. Listens for `onFileSelect` (drag-drop or click-to-browse).
 *   2. Reads the file (`new FileReader().readAsText`), pipes the text through
 *      the exported `parseCSV` + per-row `validateRow`, computes
 *      `JobProgress` + `ImportSummary`, and passes them back via props.
 *   3. Listens for `onCommit` to fire the actual batch-insert mutation
 *      (500 rows/txn per `README.md` §Use case constraints).
 *   4. Listens for `onCancel` / `onClose` / sample/error download CTAs.
 *
 * Vietnamese formatting:
 *   - All copy verbatim from the 5 HTML state files.
 *   - Phone format hint: `0901 234 567`.
 *   - Date format hint: `15/08/2015`.
 *
 * Accessibility (WCAG AA — contrast measurements documented in
 * `states/*.html` HTML proto comments):
 *   - Drop-zone is keyboard-reachable via the inner `<input type="file">`
 *     inside a clickable `<label>`.
 *   - Drag-over banner carries `role="region" aria-live="polite"` so screen
 *     readers announce "thả file ra đây".
 *   - Parsing progress bar carries `role="progressbar"` + `aria-valuenow/min/max`.
 *   - Partial-success banner carries `role="status" aria-live="polite"`.
 *   - Error state carries `role="alert"`.
 *   - Step icon glyphs are `aria-hidden`; meaning conveyed via Vietnamese text
 *     so colour is not the only signal.
 *
 * No new deps — uses Tailwind tokens already shipped with the consuming
 * app's theme.
 */

import type React from 'react';
import { useCallback, useId, useRef, useState } from 'react';
import type {
  BulkImportDropzoneProps,
  ImportError,
  ImportJobStatus,
} from './types';

const DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB per spec
const DEFAULT_MAX_ROWS = 10_000; // 10k rows per spec

const COPY = {
  headerTitle: 'Nhập danh sách học sinh',
  closeLabel: 'Đóng',
  backLabel: 'Quay lại',
  // Stepper
  stepUpload: 'Tải lên',
  stepValidate: 'Kiểm tra',
  stepDone: 'Hoàn tất',
  stepperLabel: 'Tiến trình nhập',
  // Idle drop-zone
  idleCta: 'Kéo thả file CSV/Excel vào đây',
  idleHintBefore: 'hoặc ',
  idleHintAction: 'chọn từ máy',
  idleConstraint: (maxRows: number, maxSizeMb: number) =>
    `Tối đa ${maxRows.toLocaleString('vi-VN')} dòng · Dung lượng ≤ ${maxSizeMb} MB · Hỗ trợ .csv, .xlsx, .xls`,
  sampleTitle: 'Tải file mẫu (.xlsx)',
  sampleSubtitle: 'Đã có sẵn 5 dòng ví dụ và đầy đủ cột bắt buộc',
  formatTitle: 'Cột bắt buộc',
  formatColumns: 'ho_va_ten · ngay_sinh · lop · phu_huynh_phone',
  tipTitle: 'Mẹo cho tuần tuyển sinh',
  tipBody:
    'Bạn có thể tải lên nhiều lần trong ngày — hệ thống sẽ tự động bỏ qua các học sinh đã có trong hệ thống (so khớp theo SĐT phụ huynh).',
  // Drag over
  dragOverRegionLabel: 'Đang kéo file',
  dragOverTitle: 'Thả file ra đây để tải lên',
  dragOverFormatOk: 'Định dạng hợp lệ',
  // Parsing
  parsingTitle: 'Đang kiểm tra dữ liệu...',
  parsingProgressLabel: 'Tiến độ kiểm tra dữ liệu',
  parsingDefaultStep: 'Đang xử lý dữ liệu',
  parsingEtaPrefix: 'Còn khoảng ',
  parsingCancel: 'Hủy',
  parsingCloseHint: 'Bạn có thể đóng tab — chúng tôi sẽ gửi Zalo thông báo khi xong.',
  // Partial success
  partialSummaryTitle: (valid: number, total: number, errs: number) =>
    `${valid} trên ${total} dòng hợp lệ — ${errs} dòng có lỗi`,
  partialSummaryHint:
    'Bạn có thể tải về danh sách lỗi để sửa, hoặc bỏ qua các dòng lỗi và tiếp tục với các dòng hợp lệ.',
  partialStatValid: 'Sẽ nhập',
  partialStatError: 'Có lỗi',
  partialStatDuplicate: 'Trùng (sẽ bỏ qua)',
  partialErrorsHeading: (n: number) => `Danh sách lỗi (${n})`,
  partialErrorsDownload: 'Tải file lỗi (.xlsx)',
  partialMoreErrors: (n: number) => `Còn ${n} lỗi khác — tải file để xem đầy đủ`,
  partialActionFix: 'Tải lại file đã sửa',
  partialActionCommit: (n: number) =>
    `Tiếp tục với ${n} dòng hợp lệ`,
  partialMaxRowsWarning: (cap: number) =>
    `File vượt quá ${cap.toLocaleString('vi-VN')} dòng giới hạn — chỉ ${cap.toLocaleString('vi-VN')} dòng đầu tiên sẽ được kiểm tra.`,
  // Done
  doneTitle: (n: number) => `Đã nhập ${n.toLocaleString('vi-VN')} học sinh thành công`,
  doneSubtitle: 'Hoàn tất',
  doneStatStudents: 'Học sinh mới',
  doneStatClasses: 'Lớp được phân bổ',
  doneStatParents: 'SĐT phụ huynh duy nhất',
  doneCta: 'Đóng',
  // Error
  errorTitle: 'Không thể tải lên',
  errorFallback: 'Đã có lỗi xảy ra khi xử lý file. Vui lòng thử lại.',
  errorTryAgain: 'Thử lại',
};

function formatPercent(processed: number, total: number): number {
  if (total <= 0) return 0;
  return Math.max(0, Math.min(100, Math.round((processed / total) * 100)));
}

function ErrorRow({ err }: { err: ImportError }): React.JSX.Element {
  return (
    <li
      data-testid={`bulk-import-error-row-${err.row}`}
      className="flex items-start gap-3 px-4 py-3"
    >
      <span className="mt-0.5 shrink-0 rounded bg-muted px-2 py-0.5 font-mono text-xs">
        Dòng {err.row}
      </span>
      <p className="min-w-0 flex-1 text-sm">
        <span className="text-destructive">{err.message}</span>
      </p>
    </li>
  );
}

function Stepper({
  status,
}: {
  status: ImportJobStatus;
}): React.JSX.Element {
  // Step 1 = Upload, Step 2 = Validate, Step 3 = Done
  const stepIndex =
    status === 'idle' || status === 'drag-over' || status === 'error'
      ? 1
      : status === 'parsing' || status === 'partial-success'
        ? 2
        : 3;

  function stepClass(target: 1 | 2 | 3): string {
    if (target < stepIndex) return 'text-success';
    if (target === stepIndex) return 'font-semibold text-primary';
    return 'text-muted-foreground';
  }

  function bubbleClass(target: 1 | 2 | 3): string {
    if (target < stepIndex)
      return 'bg-success text-success-foreground';
    if (target === stepIndex)
      return 'bg-primary text-primary-foreground';
    return 'border bg-card';
  }

  return (
    <ol
      aria-label={COPY.stepperLabel}
      className="mb-6 flex items-center justify-between gap-2 text-xs md:text-sm"
    >
      <li className={`flex items-center gap-2 ${stepClass(1)}`}>
        <span
          className={`grid h-7 w-7 place-items-center rounded-full ${bubbleClass(1)}`}
          aria-hidden="true"
        >
          {stepIndex > 1 ? '✓' : '1'}
        </span>
        <span>{COPY.stepUpload}</span>
      </li>
      <li
        className={`h-px flex-1 ${stepIndex > 1 ? 'bg-success' : 'bg-border'}`}
        aria-hidden="true"
      />
      <li className={`flex items-center gap-2 ${stepClass(2)}`}>
        <span
          className={`grid h-7 w-7 place-items-center rounded-full ${bubbleClass(2)}`}
          aria-hidden="true"
        >
          {stepIndex > 2 ? '✓' : '2'}
        </span>
        <span>{COPY.stepValidate}</span>
      </li>
      <li
        className={`h-px flex-1 ${stepIndex > 2 ? 'bg-success' : 'bg-border'}`}
        aria-hidden="true"
      />
      <li className={`flex items-center gap-2 ${stepClass(3)}`}>
        <span
          className={`grid h-7 w-7 place-items-center rounded-full ${bubbleClass(3)}`}
          aria-hidden="true"
        >
          {stepIndex > 2 ? '✓' : '3'}
        </span>
        <span>{COPY.stepDone}</span>
      </li>
    </ol>
  );
}

export function BulkImportDropzone(
  props: BulkImportDropzoneProps,
): React.JSX.Element {
  const {
    status,
    tenantLabel,
    contextLabel,
    fileName,
    progress,
    summary,
    errorMessage,
    maxFileSize = DEFAULT_MAX_FILE_SIZE,
    maxRows = DEFAULT_MAX_ROWS,
    onFileSelect,
    onCommit,
    onCancel,
    onClose,
    onSampleDownload,
    onErrorDownload,
    lang = 'vi',
  } = props;

  const inputId = useId();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Internal drag-state — used so the host app doesn't need to wire native
  // dragenter/dragleave events. The component still respects an external
  // `status="drag-over"` if the host wants to drive it explicitly.
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const effectiveStatus: ImportJobStatus =
    status === 'idle' && isDraggingOver ? 'drag-over' : status;

  const handleFile = useCallback(
    (file: File | undefined | null) => {
      if (!file) return;
      onFileSelect?.(file);
    },
    [onFileSelect],
  );

  const handleDragEnter = useCallback((e: React.DragEvent<HTMLElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDraggingOver(true);
  }, []);
  const handleDragOver = useCallback((e: React.DragEvent<HTMLElement>) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);
  const handleDragLeave = useCallback((e: React.DragEvent<HTMLElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDraggingOver(false);
  }, []);
  const handleDrop = useCallback(
    (e: React.DragEvent<HTMLElement>) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDraggingOver(false);
      const file = e.dataTransfer?.files?.[0];
      handleFile(file);
    },
    [handleFile],
  );
  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      handleFile(file);
      // Reset the input so the same file can be re-selected after a reject.
      if (fileInputRef.current) fileInputRef.current.value = '';
    },
    [handleFile],
  );

  const maxSizeMb = Math.round(maxFileSize / (1024 * 1024));

  // Drag-event listeners only active in idle / drag-over status. We attach
  // them to the root container so state transitions (idle → drag-over → idle)
  // don't unmount the listening element. In other states we pass undefined
  // so React removes the handlers entirely.
  const dragListening =
    status === 'idle' || status === 'drag-over';

  return (
    <div
      data-testid="bulk-import-dropzone-root"
      data-status={effectiveStatus}
      lang={lang}
      onDragEnter={dragListening ? handleDragEnter : undefined}
      onDragOver={dragListening ? handleDragOver : undefined}
      onDragLeave={dragListening ? handleDragLeave : undefined}
      onDrop={dragListening ? handleDrop : undefined}
      className="min-h-full bg-muted/30 text-foreground"
    >
      <header className="border-b bg-card">
        <div className="mx-auto flex max-w-4xl items-center gap-3 px-4 py-4">
          <button
            type="button"
            aria-label={
              effectiveStatus === 'partial-success' ? COPY.backLabel : COPY.closeLabel
            }
            onClick={onClose}
            disabled={effectiveStatus === 'parsing'}
            className={`rounded-lg p-2 hover:bg-muted ${effectiveStatus === 'parsing' ? 'opacity-50' : ''}`}
          >
            ✕
          </button>
          <div>
            <h1 className="text-lg font-semibold">{COPY.headerTitle}</h1>
            {(tenantLabel || contextLabel) && (
              <p className="text-sm text-muted-foreground">
                {[tenantLabel, contextLabel].filter(Boolean).join(' · ')}
              </p>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-4 py-6">
        <Stepper status={effectiveStatus} />

        {effectiveStatus === 'idle' && (
          <IdleBody
            inputId={inputId}
            fileInputRef={fileInputRef}
            onInputChange={handleInputChange}
            maxRows={maxRows}
            maxSizeMb={maxSizeMb}
            onSampleDownload={onSampleDownload}
          />
        )}

        {effectiveStatus === 'drag-over' && (
          <DragOverBody fileName={fileName} />
        )}

        {effectiveStatus === 'parsing' && (
          <ParsingBody
            fileName={fileName}
            progress={progress}
            onCancel={onCancel}
          />
        )}

        {effectiveStatus === 'partial-success' && summary && (
          <PartialSuccessBody
            summary={summary}
            maxRows={maxRows}
            onCommit={onCommit}
            onErrorDownload={onErrorDownload}
            onSampleDownload={onSampleDownload}
          />
        )}

        {effectiveStatus === 'done' && summary && (
          <DoneBody summary={summary} onCommit={onCommit} />
        )}

        {effectiveStatus === 'error' && (
          <ErrorBody message={errorMessage} onRetry={onClose} />
        )}
      </main>
    </div>
  );
}

function IdleBody(props: {
  inputId: string;
  fileInputRef: React.RefObject<HTMLInputElement | null>;
  onInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  maxRows: number;
  maxSizeMb: number;
  onSampleDownload?: () => void;
}): React.JSX.Element {
  const {
    inputId,
    fileInputRef,
    onInputChange,
    maxRows,
    maxSizeMb,
    onSampleDownload,
  } = props;
  return (
    <>
      <label
        htmlFor={inputId}
        data-testid="bulk-import-dropzone-idle"
        className="block cursor-pointer rounded-2xl border-2 border-dashed bg-card p-12 text-center shadow-soft transition focus-within:ring-2 focus-within:ring-primary hover:border-primary hover:bg-primary/5"
      >
        <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-primary/10 text-primary">
          <span aria-hidden="true" className="text-2xl">
            ⬆
          </span>
        </div>
        <p className="mt-4 text-base font-semibold">{COPY.idleCta}</p>
        <p className="mt-1 text-sm text-muted-foreground">
          {COPY.idleHintBefore}
          <span className="text-primary underline">{COPY.idleHintAction}</span>
        </p>
        <input
          id={inputId}
          ref={fileInputRef}
          type="file"
          accept=".csv,.xlsx,.xls"
          className="sr-only"
          aria-describedby={`${inputId}-hint`}
          onChange={onInputChange}
        />
        <p
          id={`${inputId}-hint`}
          className="mt-4 text-xs text-muted-foreground"
        >
          {COPY.idleConstraint(maxRows, maxSizeMb)}
        </p>
      </label>

      <div className="mt-6 grid gap-4 md:grid-cols-2">
        <button
          type="button"
          onClick={onSampleDownload}
          data-testid="bulk-import-sample-download"
          className="flex items-start gap-3 rounded-xl border bg-card p-4 text-left hover:bg-muted/50"
        >
          <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-success/10 text-[hsl(var(--success))]">
            <span aria-hidden="true">⬇</span>
          </div>
          <div>
            <p className="font-medium">{COPY.sampleTitle}</p>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {COPY.sampleSubtitle}
            </p>
          </div>
        </button>
        <div className="rounded-xl border bg-card p-4">
          <p className="font-medium">{COPY.formatTitle}</p>
          <p className="mt-1.5 font-mono text-xs leading-relaxed text-muted-foreground">
            {COPY.formatColumns}
          </p>
        </div>
      </div>

      <div className="mt-6 rounded-xl border-l-4 border-info bg-info/5 p-4 text-sm">
        <p className="font-medium">{COPY.tipTitle}</p>
        <p className="mt-1 text-muted-foreground">{COPY.tipBody}</p>
      </div>
    </>
  );
}

function DragOverBody({
  fileName,
}: {
  fileName?: string;
}): React.JSX.Element {
  return (
    <div
      role="region"
      aria-live="polite"
      aria-label={COPY.dragOverRegionLabel}
      data-testid="bulk-import-dropzone-drag-over"
      className="block rounded-2xl border-2 border-dashed border-primary bg-primary/10 p-12 text-center shadow-soft ring-2 ring-primary/30"
    >
      <div className="mx-auto grid h-20 w-20 place-items-center rounded-full bg-primary text-primary-foreground">
        <span aria-hidden="true" className="text-3xl">
          ⬇
        </span>
      </div>
      <p className="mt-5 text-lg font-bold text-primary">
        {COPY.dragOverTitle}
      </p>
      {fileName && (
        <p className="mt-1 text-sm text-foreground/80">{fileName}</p>
      )}
      <p className="mt-4 inline-flex items-center gap-2 rounded-full bg-card px-3 py-1 text-xs font-medium text-foreground shadow-soft">
        {COPY.dragOverFormatOk}
      </p>
    </div>
  );
}

function ParsingBody({
  fileName,
  progress,
  onCancel,
}: {
  fileName?: string;
  progress?: BulkImportDropzoneProps['progress'];
  onCancel?: () => void;
}): React.JSX.Element {
  const processed = progress?.processed ?? 0;
  const total = progress?.total ?? 0;
  const percent = formatPercent(processed, total);

  return (
    <div
      data-testid="bulk-import-dropzone-parsing"
      className="rounded-2xl border bg-card p-6 shadow-soft"
    >
      <div className="flex items-center gap-4">
        <div className="grid h-12 w-12 shrink-0 place-items-center rounded-lg bg-success/10 text-[hsl(var(--success))]">
          <span aria-hidden="true">📄</span>
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate font-medium">{fileName ?? '—'}</p>
          {total > 0 && (
            <p className="text-xs text-muted-foreground">
              {total.toLocaleString('vi-VN')} dòng
            </p>
          )}
        </div>
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg border bg-card px-3 py-1.5 text-sm font-medium hover:bg-muted"
        >
          {COPY.parsingCancel}
        </button>
      </div>

      <div className="mt-6">
        <div className="mb-2 flex items-center justify-between text-sm">
          <p className="font-medium">{COPY.parsingTitle}</p>
          <p
            className="font-mono tabular-nums text-muted-foreground"
            data-testid="bulk-import-progress-counter"
          >
            <strong className="text-foreground">{processed}</strong> /{' '}
            {total}
          </p>
        </div>
        <div
          role="progressbar"
          aria-valuenow={percent}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={COPY.parsingProgressLabel}
          className="h-3 w-full overflow-hidden rounded-full bg-muted"
        >
          <div
            data-testid="bulk-import-progress-fill"
            className="h-full bg-primary transition-[width] duration-300"
            style={{ width: `${percent}%` }}
          />
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          {progress?.etaLabel && (
            <>
              {COPY.parsingEtaPrefix}
              <strong>{progress.etaLabel}</strong>
              {progress.stepLabel ? ' · ' : ''}
            </>
          )}
          {progress?.stepLabel ?? (!progress?.etaLabel ? COPY.parsingDefaultStep : null)}
        </p>
      </div>

      <p className="mt-6 text-center text-xs text-muted-foreground">
        {COPY.parsingCloseHint}
      </p>
    </div>
  );
}

function PartialSuccessBody(props: {
  summary: NonNullable<BulkImportDropzoneProps['summary']>;
  maxRows: number;
  onCommit?: () => void;
  onErrorDownload?: () => void;
  onSampleDownload?: () => void;
}): React.JSX.Element {
  const { summary, maxRows, onCommit, onErrorDownload, onSampleDownload } =
    props;
  const total = summary.validCount + summary.errorCount + summary.duplicateCount;
  const exceedsCap = total >= maxRows;
  const visibleErrors = summary.errors.slice(0, 4);
  const remaining = Math.max(0, summary.errors.length - visibleErrors.length);

  return (
    <div data-testid="bulk-import-dropzone-partial-success">
      <div
        role="status"
        aria-live="polite"
        className="mb-6 rounded-xl border-l-4 border-warning bg-warning/5 p-4"
      >
        <p className="font-semibold">
          {COPY.partialSummaryTitle(
            summary.validCount,
            total,
            summary.errorCount,
          )}
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          {COPY.partialSummaryHint}
        </p>
      </div>

      {exceedsCap && (
        <div
          role="status"
          aria-live="polite"
          className="mb-6 rounded-xl border-l-4 border-destructive bg-destructive/5 p-4 text-sm"
        >
          {COPY.partialMaxRowsWarning(maxRows)}
        </div>
      )}

      <div className="mb-6 grid grid-cols-3 gap-3">
        <div className="rounded-xl border bg-card p-4 text-center">
          <p className="text-2xl font-bold tabular-nums text-success">
            {summary.validCount}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {COPY.partialStatValid}
          </p>
        </div>
        <div className="rounded-xl border bg-card p-4 text-center">
          <p className="text-2xl font-bold tabular-nums text-destructive">
            {summary.errorCount}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {COPY.partialStatError}
          </p>
        </div>
        <div className="rounded-xl border bg-card p-4 text-center">
          <p className="text-2xl font-bold tabular-nums text-info">
            {summary.duplicateCount}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {COPY.partialStatDuplicate}
          </p>
        </div>
      </div>

      <section className="rounded-2xl border bg-card shadow-soft">
        <header className="flex items-center justify-between border-b px-4 py-3">
          <h2 className="font-semibold">
            {COPY.partialErrorsHeading(summary.errors.length)}
          </h2>
          <button
            type="button"
            onClick={onErrorDownload}
            data-testid="bulk-import-error-download"
            className="inline-flex items-center gap-2 rounded-lg border bg-card px-3 py-1.5 text-sm font-medium hover:bg-muted"
          >
            {COPY.partialErrorsDownload}
          </button>
        </header>
        <ul className="divide-y" role="list">
          {visibleErrors.map((err) => (
            <ErrorRow key={`${err.row}-${err.message}`} err={err} />
          ))}
          {remaining > 0 && (
            <li className="px-4 py-3 text-sm italic text-muted-foreground">
              {COPY.partialMoreErrors(remaining)}
            </li>
          )}
        </ul>
      </section>

      <div className="mt-8 flex flex-wrap items-center justify-end gap-3">
        <button
          type="button"
          onClick={onSampleDownload}
          className="rounded-lg border bg-card px-4 py-2.5 text-sm font-medium hover:bg-muted"
        >
          {COPY.partialActionFix}
        </button>
        <button
          type="button"
          onClick={onCommit}
          data-testid="bulk-import-commit"
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
        >
          {COPY.partialActionCommit(summary.validCount)}
        </button>
      </div>
    </div>
  );
}

function DoneBody({
  summary,
  onCommit,
}: {
  summary: NonNullable<BulkImportDropzoneProps['summary']>;
  onCommit?: () => void;
}): React.JSX.Element {
  return (
    <div data-testid="bulk-import-dropzone-done">
      <section className="rounded-2xl border bg-card p-8 text-center shadow-soft-lg">
        <div className="mx-auto grid h-20 w-20 place-items-center rounded-full bg-success/10 text-[hsl(var(--success))]">
          <span aria-hidden="true" className="text-4xl">
            ✓
          </span>
        </div>
        <h2 className="mt-4 text-2xl font-bold">
          {COPY.doneTitle(summary.validCount)}
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">{COPY.doneSubtitle}</p>

        <div className="mt-8 grid grid-cols-3 gap-3">
          <div className="rounded-xl border bg-muted/30 p-4">
            <p className="text-2xl font-bold tabular-nums">{summary.validCount}</p>
            <p className="mt-1 text-xs text-muted-foreground">
              {COPY.doneStatStudents}
            </p>
          </div>
          <div className="rounded-xl border bg-muted/30 p-4">
            <p className="text-2xl font-bold tabular-nums">
              {summary.duplicateCount}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              {COPY.doneStatClasses}
            </p>
          </div>
          <div className="rounded-xl border bg-muted/30 p-4">
            <p className="text-2xl font-bold tabular-nums">
              {summary.errorCount}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              {COPY.doneStatParents}
            </p>
          </div>
        </div>
      </section>

      <div className="mt-8 flex justify-end">
        <button
          type="button"
          onClick={onCommit}
          className="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
        >
          {COPY.doneCta}
        </button>
      </div>
    </div>
  );
}

function ErrorBody({
  message,
  onRetry,
}: {
  message?: string;
  onRetry?: () => void;
}): React.JSX.Element {
  return (
    <div
      role="alert"
      data-testid="bulk-import-dropzone-error"
      className="rounded-2xl border-2 border-destructive bg-destructive/5 p-8 text-center"
    >
      <p className="text-lg font-bold text-destructive">{COPY.errorTitle}</p>
      <p className="mt-2 text-sm text-foreground">
        {message ?? COPY.errorFallback}
      </p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-6 rounded-lg border bg-card px-4 py-2 text-sm font-medium hover:bg-muted"
      >
        {COPY.errorTryAgain}
      </button>
    </div>
  );
}
