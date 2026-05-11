# Wave 5 — Document Generation Sample Gallery

**Closes:** GAP-047 (Wave 5 success criterion: "Sample gallery in `documents/04-quality/samples/` with 1 golden output per format")
**Origin:** Wave 5 Sub-PRs 5.1 / 5.2 / 5.3 (generators) + Sub-PR 5.5 (branding integration)
**Date:** 2026-04-25

This folder contains one golden output per Wave 5 format. Each file demonstrates the intent of its respective generator + skill, and serves as a visual reference for reviewers and a regression baseline for future waves.

---

## Files

| File | Format | Generator | Template | What it demonstrates |
|------|--------|-----------|----------|---------------------|
| [`invoice-sample.pdf`](./invoice-sample.pdf) | PDF | `PdfGenerator` (OpenHTMLtoPDF + Thymeleaf + DejaVuSans) | `pdf/invoice` (Vietnamese tax invoice / hóa đơn GTGT) | Vietnamese tax invoice layout, VND thousand-separator format (`2.700.000`), full diacritic round-trip (Đ, ễ, ă, ô), branded header structure (logo + accent bar visible when `branding.*` keys present in `DocumentRequest.data`). |
| [`attendance-sample.xlsx`](./attendance-sample.xlsx) | XLSX | `XlsxGenerator` (Apache POI XSSF) | `xlsx/attendance` (weekly per-class attendance) | Formula-first principle (`COUNTIF`, `IFERROR` cells — not pre-computed values), color convention (blue inputs / black formulas / green percent), VN weekday labels (`Thứ 2..Thứ 7`), frozen header pane, branded header row fill (when `branding.primaryColor` provided). |
| [`teacher-contract-sample.docx`](./teacher-contract-sample.docx) | DOCX | `DocxGenerator` (Apache POI XWPF) | `docx/teacher-contract` (placeholder legal layout) | Vietnamese government header, A4 + 2.54 cm margins, Times New Roman 12pt body / 14pt bold title, salary VND formatting, `[sẽ được pháp lý duyệt ở wave sau]` placeholder marking that legal review is deferred. Branded title color (when `branding.primaryColor` provided). |

---

## Notes on branding

The samples here are committed from `kiteclass-core/src/test/resources/document-samples/` — outputs of Sub-PRs 5.1–5.3 `SampleEmitter` test classes which **did not pass `branding.*` keys**. The branding integration shipped in Sub-PR 5.5 + 5.6b adds the `DocumentBrandingAssembler` pipeline that wires tenant theme colors / logo / displayName into all three formats — but the SampleEmitters were not updated to inject branding because regenerating the goldens with hard-coded "fake tenant" branding would create a maintenance signal mismatch (the samples should reflect the *unbranded baseline*; branded outputs are tenant-specific by definition).

To see a branded output for your own tenant: run the controller path (`POST /api/v1/documents/{format}/preview|download`) with your tenant's `BrandingService.getBranding()` populated. The branded behavior is verified end-to-end in `kiteclass-core/src/test/java/com/kiteclass/core/module/document/DocumentBrandingIntegrationTest.java` (3 tests, one per format).

---

## Regenerating samples

The persisted samples come from `*SampleEmitter` test classes in `kiteclass-core/src/test/java/com/kiteclass/core/module/document/{pdf,xlsx,docx}/`. They are `@Disabled` by default so CI does not regenerate them every run. To refresh:

```bash
cd kiteclass/kiteclass-core
mvn test -Dtest='InvoiceSampleEmitter,AttendanceSampleEmitter,TeacherContractSampleEmitter' \
  -DexcludedGroups= \
  -Djunit.jupiter.tests.disabled.disabled=false  # remove @Disabled gate
```

Then `cp kiteclass/kiteclass-core/src/test/resources/document-samples/* documents/04-quality/samples/wave-05/`.

Re-run only when the templates or the generators change in a way that affects visible output (e.g., layout, color, font swap). Trivial code refactors do not warrant a refresh.

---

## Related

- Wave plan: `documents/03-planning/waves/wave-05-document-generation.md`
- Gap: `documents/04-quality/gaps/closed/GAP-047-document-generation-skills.md`
- 3-layer business docs: `documents/01-business/kiteclass/document-generation/{rules,use-cases,api-contract}.md`
- ADR: `documents/02-architecture/adr/ADR-019-document-generation-architecture.md`
