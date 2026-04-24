# ADR-019: Document Generation Architecture

**Status:** PROPOSED
**Date:** 2026-04-24
**Deciders:** @nguyenvankiet (founder / tech lead)
**Reviewers:** — (solo-dev mode; self-review per `rule-change-process.md` §12 emergency / `output-review-mandate.md` §8 exception)
**Related Gap(s):** GAP-047
**Supersedes:** —

---

## Context

Every downstream export feature in the platform needs branded, server-rendered documents — tax invoices, attendance reports, teacher contracts, AI usage billing (GAP-017), branding export packs (GAP-034), future certificates/transcripts/policy docs. Without a unified generation layer each feature would invent its own approach (ad-hoc PDFBox here, string-template HTML there), drifting naming + branding + legal review.

**Forces at play:**

- **Licensing.** iText 7 is AGPL — incompatible with closed-source SaaS without a commercial license. Must pick permissive-licensed libraries.
- **Vietnamese typography.** Diacritics, Đ/đ, VND currency formatting, long composite names. Rendering engine must handle these without font-fallback glitches.
- **Branding injection.** Every generated doc must apply per-tenant colors, logo, fonts from the Branding Package API (ADR-009). Consistent across formats.
- **Format surface.** Invoice needs PDF. Attendance reports need Excel (formulas). Contracts need Word (editable). PPT deferred to Wave 6.
- **Architectural fit.** Core service already hosts domain logic for invoicing, attendance, teacher; pulling generation here keeps one service owning one concern.
- **Scope discipline.** Wave 5 ships foundation + 3 formats × 1 template each. Template library expansion + async queue + legal review are explicit follow-ups (GAP-208/210).

Stakeholder decision points (Wave 5 decision guide Q1-Q6, approved 2026-04-24):

1. PDF library choice (iText vs PDFBox vs OpenHTMLtoPDF)
2. Maven module split (inline vs new `kiteclass-documents` module)
3. Backend vs hybrid vs separate microservice architecture
4. FE preview strategy (inline PDF / download-only for Excel/Word)
5. Sync vs queued generation
6. Sub-PR sequencing (Word stays Wave 5; PPT deferred)

---

## Decision

### 1. Architecture — pure backend, inline in `kiteclass-core`

Generation logic lives as a package (`com.kiteclass.core.module.document`) inside `kiteclass-core`. No new Maven module, no separate microservice.

Rationale:
- **Single caller (kiteclass-core domain services)** in Wave 5. YAGNI rules out a module split until `kitehub-*` services need generation (tracked as follow-up GAP-209).
- **Branding integration** already lives in core via GAP-010 API — cheapest to call in-process.
- **No language split** — everything is Java 17 / Spring Boot 3.5; no need to cross a network.

### 2. Libraries — Apache/MIT-licensed stack

| Concern | Library | License |
|---------|---------|---------|
| PDF: HTML → PDF rendering | **OpenHTMLtoPDF 1.0.10** | LGPL 3 / MIT (no AGPL network clause) |
| PDF: low-level ops (merge/stamp) | **Apache PDFBox 2.0.x** (transitive from openhtmltopdf-pdfbox) | Apache 2.0 |
| PDF: HTML templating | **Thymeleaf** (Spring Boot BOM version) | Apache 2.0 |
| Excel (XSSF) | **Apache POI 5.4.0** (already present, GAP-203 pin) | Apache 2.0 |
| Word (XWPF) | **Apache POI 5.4.0** | Apache 2.0 |

### 3. Pattern — Facade + Strategy (per `.claude/rules/design-patterns.md` §3)

```
DocumentGenerationService  ← Facade (one entry point per domain use case)
    └── Generator           ← Strategy interface (generate(DocumentRequest) → DocumentResponse)
         ├── PdfGenerator   ← Sub-PR 5.1
         ├── XlsxGenerator  ← Sub-PR 5.2
         └── DocxGenerator  ← Sub-PR 5.3
```

Value objects `DocumentRequest` / `DocumentResponse` / `DocumentFormat` isolate callers from format-specific types (no `XWPFDocument` leaking to invoice services — per `design-patterns.md` §3.10 leaky abstraction ban).

### 4. Execution mode — synchronous in Wave 5

Every generate call is a blocking request → byte array. No queue, no polling. Async path (RabbitMQ `doc.generate.{format}`) is a follow-up (GAP-210) triggered when bulk or >10s operations appear.

### 5. Delivery endpoints

- PDF: `/api/v1/documents/{type}/{id}/preview` returns `Content-Disposition: inline` (browser viewer) AND `/download` returns `attachment`
- Excel / Word: `/download` only (browsers don't preview these inline reliably)

### 6. Branding injection

Every generator receives branding via the cached Branding Package facade (ADR-009). Generators NEVER hardcode colors, logos, fonts — they pull per-tenant and fail fast if tenant has no branding package.

---

## Consequences

### Positive

- One Facade, one Strategy interface — adding PPT in Wave 6 is a single new class + deps
- Permissive licenses across the whole stack — no commercial license to procure
- No extra deployment artefact — core service already runs everywhere, generation goes with it
- Branding consistency enforced by single injection point
- Test surface is bounded — each Strategy has isolated unit tests + a Facade integration test

### Negative

- `kiteclass-core` jar grows ~8 MB (PDFBox + OpenHTMLtoPDF + Thymeleaf). Acceptable for v1; revisit if build time or image size becomes an issue
- Any caller outside core (kitehub-*) must either copy this code or wait for module extraction (GAP-209). Wave 5 has no such caller.
- Sync-only blocks the request thread for heavy PDFs (>5s). Mitigation: GAP-210 queue + short per-request timeout; monitor p95 latency before deciding to extract.
- Thymeleaf CSS subset is narrower than full browsers — some design-forward templates may need hand-tuned HTML or fall back to PDFBox manual layout.
- PDFBox 2.0.x (not 3.0) — plan targeted 3.0 but openhtmltopdf 1.0.x line still tracks PDFBox 2.x. Upgrade to 3.x requires openhtmltopdf 1.1+ (still alpha as of 2026-04). Revisit as part of GAP-209 when module extraction happens.

### Neutral

- New package `com.kiteclass.core.module.document/` follows existing convention (alongside `branding`, `invoice`, `teacher`)
- Adds Thymeleaf starter to core (previously only used in gateway) — version aligned via Spring Boot BOM
- Test resources gain `document-samples/` folder for golden outputs, committed under `src/test/resources/` (small PDFs/xlsx, <50 KB each)

---

## Alternatives Considered

### Alternative A: iText 7

- **Pros:** Most mature PDF library on JVM; best text positioning; advanced PDF features (PDF/A, signing, forms)
- **Cons:** **AGPL 3** — requires either open-sourcing the SaaS or buying commercial license (~€4k/yr+). Copyleft network clause triggers for SaaS.
- **Rejected because:** licensing cost + AGPL incompatibility with closed-source commercial SaaS makes this a non-starter regardless of technical merits.

### Alternative B: Hybrid (backend server docs, FE for interactive preview)

- **Pros:** Interactive previews (highlight, annotate) possible with client-side libraries (pdf.js, SheetJS)
- **Cons:** Doubles surface area — two codepaths per format, two sets of tests, branding drift risk, FE bundle bloat from sheet/doc parsers
- **Rejected because:** Wave 5 scope is generation, not interactive editing. FE preview of PDF is already free (browser native viewer via inline `Content-Disposition`); FE-side Excel/Word preview is YAGNI.

### Alternative C: Separate microservice (`kiteclass-document-service`)

- **Pros:** Isolates heavy deps; scales generation independently; polyglot possible (e.g., headless Chrome for PDF)
- **Cons:** Network hop per call; separate deployment + monitoring; adds operational burden for single-caller feature; duplicate branding-package fetch
- **Rejected because:** YAGNI for Wave 5 (single caller). Revisit if ≥2 services need generation and current bottleneck is real. Extraction path via GAP-209 kept open — facade makes the move cheap later.

### Alternative D: Headless browser (Puppeteer / Playwright for PDF)

- **Pros:** Full CSS fidelity; easy design iteration with web tools
- **Cons:** 300+ MB container; cold-start seconds; non-JVM subprocess; Vietnamese font loading is still manual; overkill for invoice-style templates
- **Rejected because:** infrastructure cost grossly disproportionate to Wave 5 needs; reconsider only if templates need modern CSS (grid, flex deep layouts) that OpenHTMLtoPDF can't render.

---

## Implementation Notes

### Sub-PR sequencing (per wave plan §4)

- **5.0** (this ADR + scaffolding) — foundation; inline package + interface + facade stub
- **5.1** PDF (P0) — PdfGenerator + invoice template
- **5.2** Excel (P0) — XlsxGenerator + attendance report template
- **5.3** Word (P1) — DocxGenerator + teacher contract placeholder
- **5.5** Branding integration + quality audit
- **5.6** Wave completion

### Migration / rollback

- No data migration (stateless generation from request + branding)
- Rollback = revert 5.0 commit; no DB schema changes, no config flags

### Monitoring / success criteria

- p95 PDF generation <2s for 1-page invoice
- Zero cross-tenant branding leaks (integration test)
- Test coverage ≥80% on each Generator
- 3-layer docs up-to-date per PR (rules.md + use-cases.md + api-contract.md)

### Font handling (risk mitigation for Vietnamese diacritics)

- Preload NotoSans or DejaVuSans TTF into PDFBox font resolver at service startup
- Per-template test: assert rendered text contains diacritic-perfect characters (Đ đ ă â ê ô ơ ư)

---

## References

- Gap: [`documents/04-quality/gaps/GAP-047-document-generation-skills.md`](../../04-quality/gaps/GAP-047-document-generation-skills.md)
- Wave plan: [`documents/03-planning/waves/wave-05-document-generation.md`](../../03-planning/waves/wave-05-document-generation.md)
- Decision guide: [`documents/03-planning/waves/wave-05-decision-guide.md`](../../03-planning/waves/wave-05-decision-guide.md)
- Design pattern: [`.claude/rules/design-patterns.md`](../../../.claude/rules/design-patterns.md) §3 Facade + Strategy
- Skill convention: [`.claude/rules/skill-conventions.md`](../../../.claude/rules/skill-conventions.md) (governs SKILL.md files added in 5.1–5.3)
- Meta priority: [`.claude/rules/meta-gap-priority.md`](../../../.claude/rules/meta-gap-priority.md) — why Wave 5 is prioritised
- Branding integration target: [ADR-009 Branding Package Composite API](ADR-009-branding-package-api.md)
- Async queue follow-up: ADR-014 (RabbitMQ) + future GAP-210

---

## Log

- 2026-04-24 — Initial proposal (Sub-PR 5.0). PROPOSED.
