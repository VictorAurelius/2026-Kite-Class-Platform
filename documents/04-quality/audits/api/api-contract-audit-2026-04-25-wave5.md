# API Contract Audit — Wave 5 Refresh

**Date:** 2026-04-25
**Score:** 95/100 (A) — up from 82/100 (2026-04-17 SaaS audit)
**Scope:** Wave 5 cumulative (Sub-PRs 5.0–5.5) — `kiteclass-core/module/document/**` + `documents/01-business/kiteclass/document-generation/{rules,use-cases,api-contract}.md`
**Auditor:** Explore agent (api-contract-audit skill)
**Closes:** GAP-214 (1 of 5 audits in suite)

---

## Score breakdown

| # | Category | Score | Notes |
|---|----------|------:|-------|
| 1 | Endpoint Coverage | 20/20 | All 2 HTTP endpoints + 8 internal types fully documented; bidirectional code↔doc match. |
| 2 | Request/Response Match | 19/20 | DTO fields + DocumentResponse + DocumentRequest 100% match `api-contract.md`. -1 for blank/null format error case underdocumented. |
| 3 | Error Code Consistency | 18/20 | 400/401/403/500 matrix correct. -2 for `UnsupportedOperationException` not mapped in `GlobalExceptionHandler` (returns generic 500). |
| 4 | Versioning & Deprecation | 20/20 | All types are net-new under `/api/v1/`; no breaking changes; status flagged "Sub-PR 5.5 SHIPPED" in api-contract.md. |
| 5 | Integration Test Coverage | 18/20 | `DocumentGenerationControllerTest` (10 @Test) covers preview/download dispositions, format gating, validation, branding capture. -2 for no auth-rejection IT (401/403 paths not exercised). |

**Total: 95/100 (A)**

---

## Findings (top 5)

| # | Severity | Finding | Location |
|---|:--------:|---------|----------|
| 1 | 🟠 P1 | No 401/403 auth-rejection integration tests for `DocumentGenerationController`; `@PreAuthorize` enforcement is not verified at IT layer. | `DocumentGenerationControllerTest.java` |
| 2 | 🟡 P2 | `UnsupportedOperationException` thrown by `DocumentGenerationService` when no Generator is wired falls through to generic 500 in `GlobalExceptionHandler` — clients get opaque error. Wave 5 not affected (all 3 wired); future-proofing concern. | `GlobalExceptionHandler.java` |
| 3 | 🟡 P2 | Blank/null `format` path-variable error path conflated with "unknown format" in `api-contract.md` error matrix; controller distinguishes them but doc doesn't. | `api-contract.md` §error codes |
| 4 | 🟡 P2 | `BR-DOC-015` caller-key precedence rule documented but no concrete request example showing `data:{"branding.primaryColor":"#FF0000",...}` override — client devs can miss the rule. | `api-contract.md` |
| 5 | 🟡 P2 | Rate-limiting deferral note references `GAP-210` without a hyperlink; consumers don't know what limit will arrive. | `api-contract.md` §rate limits |

---

## Gap candidates

| Tracking | Title | Severity |
|----------|-------|:--------:|
| `GAP-XXX` | Add `@WithMockUser` / `@WithAnonymousUser` integration tests for `DocumentGenerationController` auth rejection | 🟠 P1 |
| `GAP-XXX` | Map `UnsupportedOperationException` in `GlobalExceptionHandler` (501 with detail) | 🟡 P2 |
| `GAP-XXX` | Split blank/null format error from unknown-format error in `api-contract.md` | 🟡 P2 |
| `GAP-XXX` | Add concrete caller-precedence example to `api-contract.md` for `branding.*` keys | 🟡 P2 |

(Final gap IDs assigned by parent during state-check + filing.)

---

## Delta vs 2026-04-17

- Prior score 82/100 (SaaS email endpoints). Document-generation surface is **net-new** — first audit of this domain.
- IT discipline jumped Category 5 from 12/20 → 18/20 thanks to `DocumentGenerationControllerTest` WebMvc suite (10 cases including RFC-5987 filename + branding capture via ArgumentCaptor).
- 3-layer business docs (rules / use-cases / api-contract) are tightly synced — gap is doc-level polish, not contract drift.

---

## Assessment

Document-generation APIs are **release-ready**. Three-layer docs precisely mirror code; HTTP endpoints are auth-guarded; tests cover happy + most error paths. The single P1 finding (auth-rejection IT) is recommended for closure inside Sub-PR 5.6b. Three P2 findings are doc-polish + future-proofing — file as gaps and address in next maintenance sweep.
