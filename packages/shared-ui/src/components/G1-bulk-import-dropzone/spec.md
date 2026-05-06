# G1 Bulk Import Drop-zone — Component Spec (production port)

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G1-bulk-import-dropzone/README.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G1-bulk-import-dropzone/README.md) + 5 state HTML files under `states/`.
**Component gap:** G1 per `dossier/04-component-gaps.md` §G1.
**Tracking gap:** [`GAP-273`](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md) — stays 🟡 PARTIAL after this port; final close at Wave 29 milestone.
**Wave:** 29 Bucket A (8th of 12 G* — first of last 4 G* in Phase 3 final).
**Replaces:** Missing FE entry point at KC `/students` (Nhập danh sách button) — backend complete since Wave 6 per GAP-137 P0; this PR closes the design-system side.
**Persona:** P3 Medium Center Admin / P5 K-12 Principal.

---

## What this PR ships

- `<BulkImportDropzone>` React component covering 6 states (`idle`, `drag-over`, `parsing`, `partial-success`, `done`, `error`) — first 5 mirror the spec'd HTML state files, `error` is a synthetic terminal state for upload-time failures (no spec'd HTML, but caller still needs to render it).
- `parseCSV(text)` — CSV/Excel-export parser that returns `{ rows: ImportRow[], errors: ImportError[] }`. Strips UTF-8 BOM, handles `\r\n`, supports quoted fields with `""` escape. Detects missing required columns and reports them as a single whole-file error.
- `validateRow(row, schema='students')` — per-row validator returning `{ valid: boolean, errors: string[] }`. Vietnamese messages copied verbatim from `partial-success.html`. Phone format `0\d{9,10}` (10–11 digits, leading zero, whitespace tolerant); date `dd/MM/yyyy` with calendar-roundtrip check; name ≥ 2 tokens; class non-empty.
- TypeScript types exported on the public `@kite/shared-ui` API: `BulkImportDropzone`, `BulkImportDropzoneProps`, `ImportJobStatus`, `ImportRow`, `ImportError`, `ImportSummary`, `JobProgress`.
- Vitest coverage: 11 RTL component tests + 17 pure-helper tests (`utils.test.ts`).

## State mapping

`ImportJobStatus` (page-level lifecycle):

| Status | Source | Body content |
|--------|--------|--------------|
| `idle` | `states/idle.html` | Empty drop-zone + sample download CTA + format hint + tip card |
| `drag-over` | `states/drag-over.html` | Primary border + filled bg + filename preview + "Định dạng hợp lệ" badge |
| `parsing` | `states/parsing.html` | File header card + progress bar (`role="progressbar"`) + ETA + cancel CTA |
| `partial-success` | `states/partial-success.html` | Summary banner + 3-stat strip + first 4 errors + "Tải file lỗi" + "Tiếp tục với N dòng" |
| `done` | `states/done.html` | Hero check + 3-stat grid + close CTA |
| `error` | (synthetic) | `role="alert"` panel with VN error message + retry CTA |

The component listens for native HTML5 drag events on the idle drop-zone label so callers don't have to wire `dragenter`/`dragleave` themselves; the host can still drive `drag-over` explicitly via the `status` prop if it has its own drag tracking.

## VN UX (verbatim from README + state HTMLs)

- Phone hint: `0901 234 567` (10 digits starting with `0`).
- Date hint: `15/08/2015` (`dd/MM/yyyy`).
- Class names: `Lớp 6A1`, `Lớp 10A2` (never "Class 1").
- Error messages localized: `Số điện thoại không hợp lệ`, `Ngày sinh sai định dạng`, `Họ tên phải có ít nhất 2 từ`, `Tên lớp không được để trống`.
- Sample download CTA: `Tải file mẫu (.xlsx)`.
- Error download CTA: `Tải file lỗi (.xlsx)`.
- Stepper labels: `Tải lên` → `Kiểm tra` → `Hoàn tất`.
- Constraint hint: `Tối đa 10.000 dòng · Dung lượng ≤ 5 MB · Hỗ trợ .csv, .xlsx, .xls`.

## Accessibility (WCAG AA)

- Drop-zone is a `<label>` wrapping a hidden `<input type="file">` — keyboard-reachable via `Tab` (input gets focus through the label's `focus-within` ring).
- Drag-over banner carries `role="region" aria-live="polite"` so screen readers announce drag activity.
- Parsing progress carries `role="progressbar"` + `aria-valuenow` / `aria-valuemin` / `aria-valuemax` + `aria-label="Tiến độ kiểm tra dữ liệu"`.
- Partial-success summary banner carries `role="status" aria-live="polite"`.
- Error state carries `role="alert"` (assertive, since it's blocking).
- Step icon glyphs are `aria-hidden`; the Vietnamese step name carries the meaning (colour is NOT the only signal).
- Stepper rendered as `<ol aria-label="Tiến trình nhập">`.
- Errors panel uses `role="list"` with one `<li>` per error — each `<li>` has a `data-testid="bulk-import-error-row-{N}"` for tests.
- Header "Đóng" button is disabled during the parsing state (`disabled={status==='parsing'}`) and `aria-label`'d.

## Constraints

- ≤ 5 MB file size (`maxFileSize` prop default `5 * 1024 * 1024`).
- ≤ 10.000 rows (`maxRows` prop default `10_000`). When the parsed total exceeds this cap, the partial-success body shows an extra destructive-toned banner: `File vượt quá 10.000 dòng giới hạn — chỉ 10.000 dòng đầu tiên sẽ được kiểm tra.`
- Batch insert 500/txn — host caller's concern, not the component's. The component's `JobProgress` simply reflects `processed` / `total` and the host chunks however it likes.

## What this PR does NOT ship (deferred)

- Remaining 3 G* components (G9 / G11 / G12) — separate Wave 29 buckets under [GAP-273](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md).
- Wiring into the production route `kiteclass-frontend/src/app/students/page.tsx` — host-app concern in a follow-up PR (the route currently links to a stub).
- Actual XLSX / XLS parsing — `parseCSV` handles plain-text CSV only. Host caller chooses the XLSX library (e.g. `xlsx` or `exceljs`) and feeds the resulting CSV-shaped text into `parseCSV`. This keeps the bundle lean and lets the host decide on streaming vs in-memory.
- Backend duplicate-check (matching on phone number) — server returns `duplicateCount` after the commit; this UI just renders the number passed in via `summary.duplicateCount`.
- Per-row "Sửa và nhập lại" inline editor — out of scope; the partial-success state lets users download an errors `.xlsx`, fix it locally, and re-upload.
- Real-time WebSocket progress for very long jobs — `JobProgress` is a polled snapshot. For 500 rows the spec says ~18 seconds end-to-end; long-job streaming is a future scope.

## Acceptance criteria status (mapping to GAP-273 AC)

- [x] Component ported with TypeScript types
- [x] `spec.md` mirror committed
- [x] Unit tests per state + props edge cases (11 component + 17 helper = 28 tests; covers all 6 states + drag interaction + file selection + parse-success + parse-error + commit-progress)
- [x] Vietnamese-only labels + error messages
- [x] CSV BOM stripping + Vietnamese name handling + comma-in-quoted-field
- [x] Phone format `0\d{9,10}` valid + invalid + whitespace tolerance
- [x] Date format `dd/MM/yyyy` valid + invalid + calendar round-trip (Feb 30 / Apr 31)
- [ ] All 12 components ported — 8/12 with this PR (Wave 27 = 4, Wave 28 = 4 + D1, Wave 29 Bucket A = G1; G9/G11/G12 follow in Bucket B/C/D this wave)
- [ ] Production usage ≥105/128 verification — needs host-app wiring + UI review run after `/students` route ports the component
- [ ] Visual regression baseline — captured separately under post-wave audit

GAP-273 stays 🟡 PARTIAL until milestone Wave 29 closes the cluster.
