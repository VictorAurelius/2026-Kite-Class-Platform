# GAP-216: PDF/XLSX/DOCX p95 micro-benchmark + SLO assertion

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — blocks Sub-PR 5.6b per Wave 5 plan §4 "5.6a P0 → block 5.6b" policy
**Domain:** Backend / Testing / Performance
**Found:** 2026-04-25 (Wave 5 audit suite — performance audit finding P0-2)
**Affects:** All 3 generators (PDF/XLSX/DOCX); enforcement of `BR-DOC-PDF-007`

## Problem

`BR-DOC-PDF-007` defines the p95 budget: `<2s for a 1-page invoice`. No code enforces or measures this. `PdfGeneratorTest`, `XlsxGeneratorTest`, `DocxGeneratorTest` measure correctness only (text content, byte streams, format magic numbers). A regression that doubles render time would land silently on main with all tests still green.

Without baseline data, Sub-PR 5.6 cannot honestly state "Wave 5 meets BR-DOC-PDF-007"; the success criterion is unverifiable.

## Root Cause

Wave 5 scope (skill-conventions + generators + branding integration) didn't include benchmarking. Test discipline focused on TDD correctness. Performance assertion was deferred without a tracking gap until this audit surfaced the gap explicitly.

## Proposed Fix

### Option A — JMH micro-benchmark (preferred for repeatability)

Add `kiteclass-core/src/test/java/com/kiteclass/core/module/document/perf/`:
- `InvoiceGenerationBenchmark.java` — JMH `@Benchmark` annotated, generates 1-page invoice, asserts mean + p95 via custom JMH plugin or post-run script.
- `AttendanceGenerationBenchmark.java` — same pattern for XLSX (5 students × 6 days).
- `TeacherContractBenchmark.java` — DOCX (1 contract).

Run via `mvn -Dtest=*Benchmark surefire:test` or dedicated profile. Not in default CI loop (too slow, JMH wants warmup + measurement iterations); run weekly via scheduled workflow.

### Option B — Lightweight assertion in unit tests (faster but noisier)

Add timing assertion to existing `PdfGeneratorTest`:

```java
@Test
void invoice_generation_under_p95_budget() {
    long start = System.nanoTime();
    DocumentResponse resp = generator.generate(req);
    long durationMs = (System.nanoTime() - start) / 1_000_000;
    // Soft budget — fails CI if a single render exceeds 4s (2x SLO with headroom for CI noise)
    assertThat(durationMs).isLessThan(4000);
}
```

Cheap but environmentally noisy on shared CI runners.

**Recommendation:** Ship Option B in Sub-PR 5.6b as a regression canary (`<4s soft cap`). File a follow-up gap for proper JMH suite (Option A) with weekly run cadence — that's a Wave 7 / ops-readiness item, not a Wave 5 closure blocker.

## Acceptance Criteria

- [ ] Soft-cap timing assertion added to `PdfGeneratorTest` (`<4s` first render, including font + template parse — first-run worst case)
- [ ] Same for `XlsxGeneratorTest` (`<2s` first run) and `DocxGeneratorTest` (`<2s`)
- [ ] Document the soft cap rationale in `BR-DOC-PDF-007` rule (it's a regression canary, not the SLO — true SLO requires JMH)
- [ ] File follow-up gap for full JMH suite (Wave 7 ops-readiness scope)

## Related

- Audit: `documents/04-quality/audits/performance/performance-audit-2026-04-25-wave5.md`
- Rule: `documents/01-business/kiteclass/document-generation/rules.md` BR-DOC-PDF-007
- Wave plan: `documents/03-planning/waves/wave-05-document-generation.md` §4 Sub-PR 5.6a/5.6b
- GAP-214: parent audit suite gap

## Log

- **2026-04-25:** Filed from Wave 5 audit suite (performance audit finding P0-2). Blocks Sub-PR 5.6b. Recommended fix is Option B soft-cap canary; full JMH deferred to Wave 7.
