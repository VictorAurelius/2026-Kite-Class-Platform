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

## UC-DOC-ATT-001: Generate Weekly Attendance Report XLSX (Sub-PR 5.2 — upcoming)

Placeholder — filled by Sub-PR 5.2 (Excel + attendance template).

## UC-DOC-CON-001: Generate Teacher Contract Draft DOCX (Sub-PR 5.3 — upcoming)

Placeholder — filled by Sub-PR 5.3 (Word + contract placeholder).

## Log

- 2026-04-24 — UC-DOC-INV-001 (invoice PDF) filled (Sub-PR 5.1). 7-step happy path + error + FE behaviour + acceptance. Excel + Word placeholders untouched.
- 2026-04-24 — Stub use-cases file with UC-DOC-000 (foundation contract) + 3 placeholders for 5.1/5.2/5.3.
