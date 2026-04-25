# Document Generation — Use Cases

**Domain:** document-generation
**Source:** GAP-047, Wave 5, ADR-019
**Status:** Sub-PR 5.5 SHIPPED — HTTP delivery use cases UC-DOC-PREVIEW-001 + UC-DOC-DOWNLOAD-001 + UC-DOC-BRANDING-001 added below. Sub-PR 5.6 wave-completion still pending.

## UC-DOC-000: Foundation (Sub-PR 5.0 — this PR)

**Actor:** Platform / library consumer (future domain services — invoice, attendance, teacher).
**Goal:** Have a stable facade + strategy contract so per-format generators can ship independently.

**Steps:**
1. Caller builds `DocumentRequest` with format, templateId, tenantId, data map
2. Caller invokes `DocumentGenerationService.generate(request)`
3. Facade routes to the Generator registered for `request.format()`
4. When no generator is registered → `UnsupportedOperationException` with clear message pointing to the responsible Sub-PR

**Errors:**
- `IllegalArgumentException` — invalid DocumentRequest (null format / blank templateId / blank tenantId)
- `UnsupportedOperationException` — no Generator wired for requested format yet (Sub-PR 5.0 baseline)

**FE behavior:** N/A — this UC is library-internal. FE-visible use cases arrive with Sub-PR 5.1+.

## UC-DOC-INV-001: Generate Tenant-Branded Invoice PDF (Sub-PR 5.1)

**Actor:** Billing / payment service running inside `kiteclass-core`. Future: tenant admin user requesting an invoice download via UI.

**Goal:** Produce a Vietnamese-format tax invoice (hóa đơn GTGT) as a PDF byte stream, ready to email the payer or return as an HTTP download response in Sub-PR 5.5.

**Steps:**
1. Caller assembles invoice data (number, issue date, buyer name + tax code + address, line items with qty + unit price, VAT rate).
2. Caller builds `DocumentRequest(format=PDF, templateId="invoice", tenantId=..., data=map)`.
3. Caller invokes `DocumentGenerationService.generate(request)`.
4. Facade routes to `PdfGenerator` (registered for `DocumentFormat.PDF`).
5. `PdfGenerator#generate` validates request (non-null, format matches, templateId whitelisted), delegates to `InvoiceRenderer`.
6. `InvoiceRenderer` formats VND monetary values with `vi-VN` locale, computes VAT percent, runs the Thymeleaf template `pdf/invoice.html`, renders HTML to PDF via OpenHTMLtoPDF with DejaVuSans preloaded.
7. Returns `DocumentResponse(bytes, "application/pdf", "invoice-<number>.pdf")`.

**Errors:**
- `IllegalArgumentException` — null request, format mismatch, blank templateId, unknown templateId.
- `IllegalStateException` — renderer I/O failure (e.g., font resource missing from classpath).

**FE behavior (Wave 5 Sub-PR 5.5):**
- `GET /api/v1/documents/invoice/{id}/preview` → `Content-Disposition: inline; filename="invoice-<id>.pdf"` (browser viewer).
- `GET /api/v1/documents/invoice/{id}/download` → `Content-Disposition: attachment`.

**Acceptance:**
- Generated PDF byte stream begins with `%PDF-` magic number.
- Extracted text (via PDFBox `PDFTextStripper`) contains invoice number, buyer name, formatted VND total (e.g. `2.700.000`), and all line-item descriptions.
- Diacritic characters (Đ, ễ, ă) round-trip through the text layer.

## UC-DOC-ATT-001: Generate Weekly Attendance Report XLSX (Sub-PR 5.2)

**Actor:** Class teacher (via kiteclass-core attendance module) or tenant admin triggering a weekly export. Automated: scheduled job at end of each school week.

**Goal:** Produce a weekly per-class attendance workbook with formula-driven totals + percentages that remain correct when an educator manually corrects a P/A/L/E cell after delivery.

**Steps:**
1. Caller assembles data: `weekStart` (ISO date), `className`, list of `students [{id, name}]`, `attendance` map keyed by student id → map of `Thứ 2..Thứ 7` → `P`/`A`/`L`/`E`.
2. Caller builds `DocumentRequest(format=XLSX, templateId="attendance", tenantId=..., data=map)`.
3. Facade routes to `XlsxGenerator`.
4. `XlsxGenerator#generate` validates request, delegates to `AttendanceReportBuilder`.
5. Builder constructs `XSSFWorkbook` with title row, header row, per-student rows (input cells + COUNTIF/IFERROR formulas), summary row (`Tổng cộng` with SUM + column COUNTIFs), column widths, freeze pane.
6. Returns `DocumentResponse(bytes, xlsx MIME, "attendance-{weekStart}.xlsx")`.

**Errors:**
- `IllegalArgumentException` — null request, format mismatch, blank templateId, unknown templateId.
- `IllegalStateException` — POI I/O failure writing workbook to byte stream.

**FE behavior (Wave 5 Sub-PR 5.5):**
- `GET /api/v1/documents/attendance/{classId}/download?week={weekStart}` → `Content-Disposition: attachment; filename="attendance-<week>.xlsx"`.
- No inline preview — browsers do not reliably render xlsx.

**Acceptance:**
- `xlsx` magic number `PK` at byte offset 0.
- Re-read with POI confirms: title row contains class name + weekStart; header row contains "Học sinh", VN weekdays, "Có mặt/Vắng/Tỷ lệ"; student rows contain VN names round-trip; total + absent + percent cells are `CellType.FORMULA` (not literal values); summary row `Tổng cộng` uses SUM/COUNTIF.
- Empty student list yields a workbook with header + summary rows only (no crash).

## UC-DOC-CON-001: Generate Teacher Contract Draft DOCX (Sub-PR 5.3)

**Actor:** HR / admin at a tenant school preparing a new teacher employment contract draft via kiteclass-core teacher module.

**Goal:** Produce an editable DOCX draft of a teacher employment contract with VN-standard typography + placeholder legal wording, ready for legal review before sign-off.

**Steps:**
1. Caller assembles data: `teacherName`, `teacherIdNumber`, `tenantName`, `tenantAddress`, `startDate`, `endDate`, `salaryVnd` (BigDecimal), `subjects` (comma-separated subject list).
2. Caller builds `DocumentRequest(format=DOCX, templateId="teacher-contract", tenantId=..., data=map)`.
3. Facade routes to `DocxGenerator`.
4. `DocxGenerator#generate` validates request, delegates to `TeacherContractBuilder`.
5. Builder creates `XWPFDocument` with A4 page + 2.54 cm margins, renders VN government header, contract title, party declarations, 4 contract terms (with placeholder for detailed legal wording), two-column signature block. Salary formatted with VN thousand separators.
6. Returns `DocumentResponse(bytes, docx MIME, "teacher-contract-<slug>-<startDate>.docx")`.

**Errors:**
- `IllegalArgumentException` — null request, format mismatch, blank templateId, unknown templateId, missing required data key (e.g. `teacherName`).
- `IllegalStateException` — POI I/O failure writing document.

**FE behavior (Wave 5 Sub-PR 5.5):**
- `GET /api/v1/documents/teacher-contract/{teacherId}/download` → `Content-Disposition: attachment; filename="teacher-contract-<slug>-<date>.docx"`.
- No inline preview.

**Acceptance:**
- `docx` magic number `PK` at byte offset 0.
- Re-read with POI confirms: title `HỢP ĐỒNG GIẢNG DẠY` present; VN government header present; teacher name + tenant name appear in body; diacritics `Đ/ễ/ă` round-trip; salary string `15.000.000` with `.` thousand separator; date strings appear; paragraph count ≥ minimum.
- Missing required data key (`teacherName` etc.) throws with the key name in message.

## UC-DOC-PREVIEW-001: In-browser PDF Preview (Sub-PR 5.5)

**Actor:** Authenticated tenant user with role ADMIN / OWNER / TEACHER. Future: STUDENT for own-doc scoping (follow-up gap).

**Goal:** View a generated invoice (or future PDF template) directly in the browser without forcing a download — useful for quick verification before printing or emailing.

**Steps:**
1. FE POSTs `{templateId, data}` JSON body to `/api/v1/documents/pdf/preview`.
2. `DocumentGenerationController.preview()` parses path variable `format=pdf`, rejects xlsx/docx with 400.
3. Controller reads `TenantContext.getCurrentTenant()` (UUID set by tenant filter from `X-Tenant-Id` header), calls `BrandingService.getBranding()` to fetch the tenant's current `BrandingResponse`.
4. `DocumentBrandingAssembler.enrich()` injects `branding.primaryColor` / `branding.secondaryColor` / `branding.accentColor` / `branding.logoUrl` / `branding.displayName` into `DocumentRequest.data()` (caller-supplied keys take precedence).
5. `DocumentGenerationService.generate()` dispatches to `PdfGenerator`, which renders the branded PDF.
6. Controller emits `Content-Disposition: inline; filename*=UTF-8''<rfc5987-encoded-filename>` + `Content-Type: application/pdf` and returns raw bytes.

**Errors:**
- 400 — non-PDF format requested for preview, malformed JSON body, missing/blank `templateId`, unsupported template id, malformed `format` path variable.
- 401/403 — caller lacks ADMIN/OWNER/TEACHER role (handled by Spring Security).
- 500 — generator I/O failure (font missing, etc.).

**FE behavior:** browser displays the PDF inline (Chrome/Firefox/Safari built-in viewer). No download dialog.

**Acceptance:**
- Response status 200, body bytes start with `%PDF-`.
- `Content-Disposition` header literally contains `inline` and the RFC-5987 `filename*=UTF-8''` form.
- Branded preview shows tenant logo + primary-coloured accent bar + tenant displayName above the title (when branding is set).

## UC-DOC-DOWNLOAD-001: Document Download as Attachment (Sub-PR 5.5)

**Actor:** Same as preview — ADMIN / OWNER / TEACHER. The unified download path covers all 3 Wave 5 formats.

**Goal:** Trigger a browser save dialog for the generated invoice / attendance report / teacher contract, with a proper filename including Vietnamese characters.

**Steps:**
1. FE POSTs `{templateId, data}` to `/api/v1/documents/{format}/download` where `format ∈ {pdf, xlsx, docx}`.
2. Controller resolves tenant + branding identically to preview (UC-DOC-PREVIEW-001 step 3).
3. Branding assembler enriches request, facade dispatches to the matching generator.
4. Controller emits `Content-Disposition: attachment; filename*=UTF-8''<encoded>` + format MIME type + raw bytes.

**Errors:** identical to preview except no PDF-only constraint.

**FE behavior:** browser shows save dialog with the suggested filename — for example `invoice-INV-2026-0001.pdf`, `attendance-2026-04-20.xlsx`, `teacher-contract-nguyễn-văn-đức-2026-05-01.docx`. Vietnamese diacritics in the filename are preserved by RFC-5987.

**Acceptance:**
- Response status 200; format-specific magic number at byte 0 (`%PDF-` for PDF, `PK` for XLSX/DOCX).
- `Content-Disposition` header literally contains `attachment`.
- Filename round-trips through `HexColorUtil.stripHash`/POI/PDFBox extractors and matches the format-specific filename pattern documented in BR-DOC-PDF-005 / BR-DOC-XLSX-008 / BR-DOC-DOCX-008.

## UC-DOC-BRANDING-001: Cross-format Branding Consistency (Sub-PR 5.5)

**Actor:** Tenant admin who has updated their `Branding` record (primary/secondary/accent color, logo URL, displayName) — possibly via the AI-branding wizard.

**Goal:** Confirm that the same branding visually applies across PDF + Excel + Word outputs without manual re-configuration per format.

**Steps:**
1. Tenant updates branding via existing settings flow (`PUT /api/v1/settings/branding`).
2. Cache invalidation event fires (Wave 3 outbox + `CachingBrandingPackageProxy` eviction).
3. Subsequent calls to `/api/v1/documents/*/preview|download` automatically pick up the new branding because both `BrandingService.getBranding()` and the assembler are stateless / per-request.
4. PDF, XLSX, DOCX all render the new primary colour in their format-appropriate position (PDF = branded header accent bar, XLSX = header row fill, DOCX = title run colour).

**Errors:** none specific to this UC — branding read failures bubble up as 500 from the underlying service.

**FE behavior:** N/A — this is a cross-cutting consistency invariant, not a user-facing flow on its own.

**Acceptance:**
- `DocumentBrandingIntegrationTest` exercises all 3 generators with the same `BrandingResponse` and asserts each output surfaces the primary colour: PDF text contains `displayName`, XLSX header cell fill RGB matches, DOCX title run colour hex matches.
- Manual smoke test: update branding → request all 3 formats → spot-check colour matches.

## Log

- 2026-04-25 — Sub-PR 5.5 SHIPPED. Added UC-DOC-PREVIEW-001 (PDF inline preview), UC-DOC-DOWNLOAD-001 (3-format attachment), UC-DOC-BRANDING-001 (cross-format consistency invariant). Status header bumped. Existing FE-behavior placeholders inside UC-DOC-INV-001 / UC-DOC-ATT-001 / UC-DOC-CON-001 superseded by the new dedicated UCs but left in place as descriptive cross-references — they accurately describe what the generators produce, the new UCs describe how callers reach them.
- 2026-04-24 — UC-DOC-CON-001 (teacher contract DOCX) filled (Sub-PR 5.3). 6-step happy path + error + FE behaviour + acceptance.
- 2026-04-24 — UC-DOC-ATT-001 (attendance XLSX) filled (Sub-PR 5.2). 6-step happy path + error + FE behaviour + acceptance.
- 2026-04-24 — UC-DOC-INV-001 (invoice PDF) filled (Sub-PR 5.1). 7-step happy path + error + FE behaviour + acceptance. Excel + Word placeholders untouched.
- 2026-04-24 — Stub use-cases file with UC-DOC-000 (foundation contract) + 3 placeholders for 5.1/5.2/5.3.
