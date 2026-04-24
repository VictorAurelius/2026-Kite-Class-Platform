---
name: document-generation-pdf
description: "Use when user asks to generate a PDF — invoice, certificate, transcript, letter, report, 'hóa đơn', 'xuất PDF', 'render PDF'. Wave 5 ships Vietnamese tax invoice only; additional templates land in later waves per GAP-047 roadmap."
---

# Document Generation — PDF

PDF strategy for `com.kiteclass.core.module.document.DocumentGenerationService` (ADR-019 Facade + Strategy). Auto-wired via Spring's `List<Generator>` injection — no registration needed.

## When to use

- User needs a server-rendered PDF with branded layout + Vietnamese typography
- Template-driven output, not free-form document editing
- Wave 5 supports `templateId = "invoice"` (Vietnamese tax invoice / hóa đơn GTGT) only

## 3-pipeline routing

| Pipeline | Use case | Wave 5 status |
|----------|----------|:-------------:|
| Create | Render fresh PDF from template + data | ✅ shipped |
| Edit-Fill | Overlay data onto existing PDF form | ⬜ later wave |
| Reformat | Restructure an existing PDF | ⬜ later wave |

Stay in **Create** route. If you need Edit-Fill or Reformat, file a gap first.

## How it works

1. Caller hands a `DocumentRequest` (format=PDF, templateId, tenantId, data map) to the facade.
2. `PdfGenerator.generate(request)` delegates to the template-specific renderer (`InvoiceRenderer` for now).
3. Renderer:
   - Applies VN locale number formatting (`#.##0` for VND) + VAT percent.
   - Processes the Thymeleaf template at `resources/templates/pdf/{templateId}.html`.
   - Feeds HTML to **OpenHTMLtoPDF** (`PdfRendererBuilder#useFastMode`) with preloaded DejaVuSans + DejaVuSans-Bold so Đ/đ/ễ/ă render correctly.
4. Output `DocumentResponse(bytes, "application/pdf", "invoice-<number>.pdf")`.

## Vietnamese-specific gotchas

- **Fonts:** DejaVuSans TTFs live in `kiteclass-core/src/main/resources/fonts/`. PDFBox's built-in Helvetica drops Đ and most diacritics. Always register a TTF via `builder.useFont(InputStream supplier, "family")`.
- **VND format:** group separator is `.` (not `,`) — e.g. `2.500.000`. Use `Locale.forLanguageTag("vi-VN")` + `DecimalFormatSymbols.setGroupingSeparator('.')`.
- **HTML entities:** OpenHTMLtoPDF uses a strict XML parser. Entities beyond XHTML core (`&middot;`, `&nbsp;`, etc.) throw `SAXParseException`. Use Unicode characters directly (`·`, ` `).
- **A4 + 2cm margins:** default Vietnamese legal doc layout — set via `@page` CSS in the template, not Java.
- **Long composite names:** lines like `Nguyễn Văn Đức Anh Tuấn` can overflow single-cell table layouts. Use `word-break: break-word` on narrow columns.

## Reference

- `reference/pdf-cover-styles.md` — cover/header style catalog (15 patterns)
- `reference/pdf-block-types.md` — block type catalog (20 reusable layout blocks)

## Out of scope (Wave 5)

- Branding Package API integration (Sub-PR 5.5 wires color + logo from ADR-009 API).
- Async queue for slow renders (GAP-210).
- PDF/A compliance, signing, form fields, encryption.
- Template library beyond invoice (GAP-208, Wave 7).
