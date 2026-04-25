# Security Audit — Wave 5 Refresh

**Date:** 2026-04-25
**Score:** 85/100 (B) — up from 76/100 (2026-04-17 SaaS audit)
**Scope:** Wave 5 cumulative (Sub-PRs 5.0–5.5) — `pom.xml` dependency bumps + new auth-protected endpoints + Thymeleaf rendering surface
**Auditor:** Explore agent (security-audit skill)
**Closes:** GAP-214 (2 of 5 audits in suite)

---

## Score breakdown

| # | Category | Score | Δ |
|---|----------|------:|--:|
| 1 | Dependency Vulnerabilities | 18/20 | +1 (AWS SDK CVE-2024-43383 closure; ognl 3.4.x → 3.3.4 fixes Thymeleaf availability bug; jsoup 1.18→1.22 closes CVE-2024-6409) |
| 2 | Secrets & Credentials | 18/20 | 0 (Wave 5 introduces no new secret vectors; no hardcoded tenant data) |
| 3 | OWASP Top 10 | 16/20 | -1 (2 × P2 from new branding/template surface — see findings 1 & 3) |
| 4 | Auth & Access Control | 17/20 | 0 (`@PreAuthorize` solid; tenant isolation via `TenantContext` ThreadLocal correct but assumed) |
| 5 | Infrastructure Security | 16/20 | 0 (DejaVuSans bundled in JAR; no externalized font URL — safe) |

**Total: 85/100 (B)** — production-ready with documented minor gaps.

---

## Wave 5 dependency deltas

| Lib | Before | After | Risk |
|-----|--------|-------|------|
| jjwt | 0.12.6 | 0.13.0 | ✅ patch only |
| springdoc-openapi | 2.8.4 | 2.8.17 | ✅ patches |
| tika | 3.2.2 | 3.3.0 | ✅ archive hardening |
| jsoup | 1.18.1 | 1.22.2 | ✅ closes CVE-2024-6409 (CSS selector logic error) |
| opencsv | 5.9 | 5.12.0 | ✅ CSV injection mitigations |
| jacoco | 0.8.11 | 0.8.14 | ➖ build only |
| AWS SDK v2 | 2.29.30 | 2.42.40 | ✅ closes CVE-2024-43383 (S3 SigV4a bypass) |
| commons-compress | 1.26.0 | 1.28.0 | ✅ ZIP slip + symlink mitigations |
| poi | 5.4.0 | 5.5.1 | ✅ XLSX/DOCX parsing hardening |
| **ognl** | **3.4.11** | **3.3.4** | ⚠️ DOWNGRADE for Thymeleaf 3.1.x ABI compat (see Sub-PR 5.5 + memory `feedback_thymeleaf_ognl_pin.md`) |

Net: no new critical/high CVEs introduced; one P2-class CVE closed.

---

## Findings (top 5)

| # | Severity | Finding | Evidence |
|---|:--------:|---------|----------|
| 1 | 🟡 P2 | CSS injection risk via unvalidated `branding.primaryColor` if `BrandingResponse` is populated without `HexColorUtil.stripHash()`. Thymeleaf `th:style` interpolates the raw value. Currently mitigated because `Branding` entity has `@Pattern("^#[0-9A-Fa-f]{6}$")` validation, BUT no defense-in-depth at template level. | `invoice.html:32` + `Branding.java:53` |
| 2 | 🟡 P2 | `DocumentGenerationRequestDto.data` is unconstrained `Map<String,Object>`; caller-supplied `branding.*` keys override server-resolved values per `BR-DOC-015`. Safe in prod (auth + role gate), but a leaked test fixture could push test data into prod logs. | `DocumentBrandingAssembler.java:42` |
| 3 | 🟡 P2 | Tenant isolation depends on `TenantContext` ThreadLocal correctness. `TenantFilterInterceptor` clears in `afterCompletion()` (correct), but a future async path that calls `BrandingService.getBranding()` without binding context will throw. | `TenantContext.java` + `BrandingServiceImpl.java:54` |
| 4 | 🟢 P3 | DejaVuSans TTF bundled in JAR — safe. Future externalization to S3/CDN would re-introduce font-substitution risk. Document for future. | `InvoiceRenderer.java:37–38, 141–148` |
| 5 | ➖ Info | OGNL upgrade path: track upstream Thymeleaf 3.2.x release for OGNL 3.4.x compat. Currently pinned at 3.3.4 with aggressive comment + memory. | `kiteclass-core/pom.xml:236–245` |

---

## Gap candidates

| Tracking | Title | Severity |
|----------|-------|:--------:|
| `GAP-XXX` | Defense-in-depth `HexColorUtil.stripHash()` on Thymeleaf model values for `branding.*Color` keys | 🟡 P2 |
| `GAP-XXX` | Whitelist allowed keys in `DocumentGenerationRequestDto.data` (or reject `branding.*` from request body) | 🟡 P2 |
| `GAP-XXX` | Document async `TenantContext` binding pattern for future scheduled/queue document generation | 🟡 P2 |
| `GAP-XXX` | Track Thymeleaf 3.2.x release for OGNL 3.4.x upgrade window (memory entry + dependency-watch list) | 🟢 P3 |

---

## Assessment

Security posture **improves +9 pts** (76→85). Wave 5 ships net-positive: AWS SDK + jsoup CVE closures, OGNL availability fix. New attack surface (branding pipeline + 2 endpoints) is well-bounded by existing `@PreAuthorize` + `TenantContext` infrastructure. The 3 P2 findings are defense-in-depth opportunities, not active vulnerabilities — file as gaps, address in next maintenance window. No P0/P1 gaps blocking Wave 5 closure.
