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

## UC-DOC-INV-001: Generate Tenant-Branded Invoice PDF (Sub-PR 5.1 — upcoming)

Placeholder — filled by Sub-PR 5.1 (PDF + invoice template).

## UC-DOC-ATT-001: Generate Weekly Attendance Report XLSX (Sub-PR 5.2 — upcoming)

Placeholder — filled by Sub-PR 5.2 (Excel + attendance template).

## UC-DOC-CON-001: Generate Teacher Contract Draft DOCX (Sub-PR 5.3 — upcoming)

Placeholder — filled by Sub-PR 5.3 (Word + contract placeholder).

## Log

- 2026-04-24 — Stub use-cases file with UC-DOC-000 (foundation contract) + 3 placeholders for 5.1/5.2/5.3.
