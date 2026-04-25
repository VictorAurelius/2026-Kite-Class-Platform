# Performance Audit — Wave 5 Refresh

**Date:** 2026-04-25
**Score:** 63/100 (D) — up from 58/100 (2026-04-19 baseline)
**Scope:** Wave 5 cumulative (Sub-PRs 5.0–5.5) — synchronous PDF/XLSX/DOCX generation + branding cache integration
**Auditor:** Explore agent (performance-audit skill, static analysis only — no load tests)
**Closes:** GAP-214 (3 of 5 audits in suite)

---

## Score breakdown

| # | Category | Score | Δ vs 2026-04-19 |
|---|----------|------:|--:|
| 1 | DB Query Efficiency | 12/20 | +1 (no new N+1 from document path; `getBranding()` undecorated — see P0-1) |
| 2 | API Response Time | 14/20 | +1 (synchronous generation now on critical path; no timeout config — see P0-2) |
| 3 | Frontend Bundle | 8/20 | 0 (Wave 5 backend-only) |
| 4 | Caching Strategy | 15/20 | +2 (`CachingBrandingPackageProxy` with `sync=true` stampede protection in place — but document path bypasses it) |
| 5 | Resource Utilization | 14/20 | +1 (POI try-with-resources + Thymeleaf cache active; minor allocation overhead) |

**Total: 63/100 (D)** — Wave 5 baseline shifted +5pts vs 2026-04-19.

---

## Findings (top 5 — Wave 5 critical)

| # | Severity | Finding | Location | Impact |
|---|:--------:|---------|----------|--------|
| 1 | 🔴 **P0** | `BrandingService.getBranding()` has NO `@Cacheable` decorator — document controller calls it per render (`DocumentGenerationController:85`), hitting PostgreSQL on every request. `CachingBrandingPackageProxy` exists but wraps a different service (`BrandingPackageService.getByInstanceId(Long)`) not used by the document path. | `BrandingServiceImpl.java:51–60` | -300ms per render under 10+ concurrent requests; cache penetration / DB hot path |
| 2 | 🔴 **P0** | `DocumentGenerationController.render()` is synchronous and blocks the Tomcat worker thread for the full PDF/XLSX/DOCX render (2–5s for complex outputs). No `spring.mvc.async.request-timeout`, no `@Async` wrapper, no circuit-breaker. Under sustained POST load (e.g., bulk export), thread pool saturation likely. | `DocumentGenerationController.java` | Tomcat thread pool starvation; cascade timeouts |
| 3 | 🟠 P1 | `InvoiceRenderer` instantiates a fresh `TemplateEngine` in its constructor; every `PdfGenerator` instance allocates one. Spring injects `PdfGenerator` as a singleton (`@Component`), so this is one-time cost in practice — but `new InvoiceRenderer()` defaulting (`PdfGenerator()` no-arg) suggests test instantiation creates engines per test. Static singleton would be cleaner. | `PdfGenerator.java:29–37` | -50ms initialization per fresh instance (negligible in prod, noisy in tests) |
| 4 | 🟠 P1 | DejaVuSans TTF streams opened via lambda on **every** PDF render (`openClasspathFont` in `useFont()` callback). No pre-loaded cache. PDFBox loads the font from the stream every time. | `InvoiceRenderer.java:60–62` | -20–40ms per PDF |
| 5 | 🟡 P2 | No performance test coverage. `PdfGeneratorTest` measures correctness; no p95 baseline against `BR-DOC-PDF-007` budget (`<2s 1-page invoice`). | `PdfGeneratorTest.java` | Regression detection blind spot |

---

## Gap candidates

| Tracking | Title | Severity |
|----------|-------|:--------:|
| `GAP-XXX` | Add `@Cacheable("branding-by-tenant")` to `BrandingServiceImpl.getBranding()` (or extract a Caching proxy mirroring `CachingBrandingPackageProxy`) — eliminates DB hit per document render | 🔴 P0 |
| `GAP-XXX` | PDF/XLSX/DOCX p95 micro-benchmark + SLO assertion vs `BR-DOC-PDF-007` budget — JMH micro-benchmark or dedicated `*PerformanceTest` class | 🔴 P0 |
| `GAP-XXX` | Async/timeout strategy for document endpoints (`spring.mvc.async.request-timeout` + `@Async` or `CompletableFuture` + circuit-breaker) | 🟠 P1 |
| `GAP-XXX` | Static singleton `TemplateEngine` in `InvoiceRenderer` (or extract as Spring bean) | 🟠 P1 |
| `GAP-XXX` | Pre-cache DejaVuSans TTF bytes; document POI XLSX row limits for large attendance reports | 🟡 P2 |

---

## Delta vs 2026-04-19

- 2026-04-19 baseline: 58/100 (F)
- 2026-04-25 refresh: 63/100 (D)
- **+5 from caching scaffolding** (`CachingBrandingPackageProxy` exists with sync=true) — but the document path doesn't use it.
- **Projected after P0 fixes:** ~68/100 (C-) once `getBranding()` is cached and document timeout is configured.

---

## Assessment

Wave 5's sync rendering pipeline is **architecturally sound** but has **two P0 cache/concurrency gaps** that need fixing before Sub-PR 5.6b ships per `post-wave-audit-mandate.md` §4 + the 5.6a "P0 → block 5.6b" policy declared in the wave plan. Specifically:

1. **`BrandingService.getBranding()` must be cached** before serving real document traffic. Current implementation reads PostgreSQL on every render — a load test would expose this immediately.
2. **PDF p95 micro-benchmark missing** — without baseline data, `BR-DOC-PDF-007 <2s` cannot be enforced; regressions go undetected.

Both fixes are small (single annotations + a benchmark class). File as P0 gaps, address in Sub-PR 5.6b-pre-fix or its own dedicated 5.6c if scope grows.
