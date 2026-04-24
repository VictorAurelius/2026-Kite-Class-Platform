# Document Generation — Use Cases

**Domain:** document-generation
**Source:** GAP-047, Wave 5, ADR-019
**Status:** Sub-PR 5.0 stub — format-specific use cases land in Sub-PRs 5.1–5.3.

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

## Log

- 2026-04-24 — UC-DOC-CON-001 (teacher contract DOCX) filled (Sub-PR 5.3). 6-step happy path + error + FE behaviour + acceptance.
- 2026-04-24 — UC-DOC-ATT-001 (attendance XLSX) filled (Sub-PR 5.2). 6-step happy path + error + FE behaviour + acceptance.
- 2026-04-24 — UC-DOC-INV-001 (invoice PDF) filled (Sub-PR 5.1). 7-step happy path + error + FE behaviour + acceptance. Excel + Word placeholders untouched.
- 2026-04-24 — Stub use-cases file with UC-DOC-000 (foundation contract) + 3 placeholders for 5.1/5.2/5.3.
