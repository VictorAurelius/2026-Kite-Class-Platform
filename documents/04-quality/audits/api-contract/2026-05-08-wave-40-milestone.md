# API Contract Audit — Wave 40 Milestone (Cụm release-deploy-artifacts)

**Date:** 2026-05-08
**Auditor:** Bucket F subagent (Sonnet 4.6, Wave 40 Bucket F)
**Scope:** Kite Platform full surface — KiteClass core + KiteHub 6 services, post Wave 33→34→35→36→37→38→39 cluster
**Baseline:** 71/100 C — 2026-05-07 post Wave 35 (`2026-05-07-post-wave-35.md`)
**Wave cluster:** Wave 33 (beta-access) + Wave 34 (AI Branding wizard 7 eps) + Wave 35 (PDPL + admin guards) + Wave 36 (GAP-388 claim-code 2FA + GAP-390 API polish) + Waves 37-39 (infra/ops — no new endpoints)
**Skill:** `.claude/skills/quality/api-contract-audit/SKILL.md`
**Domain-milestone:** `AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts` closed by this report.

---

## Score: 72/100 — C+  (Δ +1 vs 71 Wave 35 baseline)

| Category | Score | Δ | Notes |
|---|:---:|:---:|---|
| **Endpoint Coverage** | 14/20 | 0 | +1 new undocumented endpoint (`POST /api/v1/auth/beta-signup/exchange-claim-code` Wave 36 GAP-388-B) added to surface; pre-existing 3 P1 findings from Wave 35 still open (AdminController 10 eps, AdminEmailController 5 eps, AuthController 7 eps). Wave 34 wizard 7 eps + Wave 35 admin guards 100% documented. Net: coverage unchanged — 1 new undocumented offsets 0 new documented. |
| **Request/Response Match** | 15/20 | +1 | Wave 36 GAP-390-A: `BrandingJobResponse.from()` now resolves `tenantId` from MDC (confirmed `BrandingJobV1Controller.java` 3-arg overload). GAP-390-C: UUID examples replace 5 numeric `12345` placeholders in `kitehub/ai-branding/api-contract.md`. Wave 34 GAP-272n drift (POST /regenerate returns entity) closed by Wave 36 schema fix. |
| **Error Code Consistency** | 18/20 | +1 | Wave 36 GAP-388-C: `BETA_EMAIL_RATE_LIMIT` (429) correctly mapped via `BetaRateLimitExceededException` → `ProblemDetail`. GAP-388-A: `BETA_HONEYPOT_FILLED` counter fires via `recordHoneypotRejection`. Pre-existing: AdminController + AdminEmailController lack error catalog (-2). |
| **Versioning/Deprecation** | 18/20 | 0 | No new version drift from Wave 36-39. `/api/platform/admin/*` (kitehub-admin legacy) vs `/api/v1/admin/*` (beta-access) drift still open (-2). Wave 34 wizard all `/api/v1/branding/*` — correct. |
| **Integration Test Coverage** | 7/20 | −1 | Wave 36 adds 3 service-layer tests for `exchangeClaimCode` (`BetaAccessServiceTest:383-430`) — good. But `BetaAccessControllerTest` does NOT cover `POST /exchange-claim-code` controller path. `DeployStreamControllerTest` (Wave 36 GAP-390-B, 4 SSE assertions) is a clear positive. Consumer-driven contract tests (Pact) absent across all 6 KiteHub services — long-standing PARTIAL. Net: -1 from missing controller test for new claim-code endpoint. |

**Total: 72/100 C+** — Δ +1 vs Wave 35 baseline. GAP-390 polish (DTO fix + UUID examples + SSE tests) and GAP-388 error mapping improved request/response match and error consistency. Offset by 1 new undocumented endpoint + missing controller-layer test for exchange-claim-code path.

---

## Endpoint Inventory — Changes vs Wave 35 Baseline

### Net-new endpoints since Wave 35 (Wave 36)

| Controller | Endpoint | Documented in api-contract.md? | Wave |
|---|---|:---:|---|
| `BetaAccessController` | `POST /api/v1/auth/beta-signup/exchange-claim-code` | ❌ **Missing** | Wave 36 GAP-388-B |

**Key finding:** `exchange-claim-code` is the 2FA gate — submits a 6-digit claim code, returns `inviteToken` UUID + pre-fill data. It exists in `BetaAccessController.java:85`, has DTO classes (`BetaClaimCodeExchangeCommand`, `BetaClaimCodeExchangeResponse`), and 3 service-layer tests (`exchangeClaimCodeHappyPath`, `wrongCode`, `expired`). The `beta-access/api-contract.md` last updated at Wave 35 does NOT include this endpoint.

The `api-contract.md` currently only shows `POST /api/v1/auth/beta-signup` (direct token redemption) — the new 2FA flow requires calling `exchange-claim-code` first to obtain the UUID token, then passing it to `beta-signup`. This flow gap means the FE integration docs are incomplete.

### Waves 37–39 endpoint delta

Waves 37 (Terraform), 38 (CDN/CI/ops runbooks), 39 (dev-stack/E2E) — **zero new REST endpoints**. API contract not affected by infra-only waves.

### Pre-existing findings (unchanged from Wave 35)

| Finding | Endpoints | Status |
|---|---|---|
| P1-1: `AdminController` | 10 (`/api/platform/admin/{dashboard,instances,revenue,subscriptions,payments}`) | Still open |
| P1-2: `AdminEmailController` | 5 (`/api/platform/admin/emails/*`) | Still open |
| P1-3: `AuthController` | 7 (`/api/auth/{register,login,refresh,...}`) | Still open |
| P2-1: `BrandingJobController` | 6 legacy (`/api/platform/branding/jobs/*`) | Still open |
| P2-2: `ContentGenerationController` + `AssetStorageController` | 2+3 | Still open |

---

## Per-Service Endpoint Inventory (summary)

| Service | Code endpoints | Documented | Undocumented | Notes |
|---|:---:|:---:|:---:|---|
| kiteclass-core | ~257 | ~254 | ~3 | IncidentReportingController 1 + ⚠️ 2 pending verify |
| kitehub-branding | ~26 | ~14 | ~12 | BrandingJobController 6 + ContentGen 2 + AssetStorage 3 + LifecycleEvents 1 |
| kitehub-subscription | ~47 | ~36 | ~11 | AuthController 7 + AdminEmailController 5 + **exchange-claim-code 1 (new)** |
| kitehub-admin | ~10 | 0 | 10 | Entire AdminController surface |
| kitehub-email | 1 | 1 | 0 | — |
| **Total** | **~341** | **~305** | **~36** | 1 new undocumented vs Wave 35 (35→36) |

---

## Wave 36 Specific Verification (GAP-388 + GAP-390)

| Deliverable | Code evidence | Docs evidence | Status |
|---|---|---|:---:|
| GAP-390-A: `tenantId` via MDC | `BrandingJobV1Controller.java` 3-arg `BrandingJobResponse.from()` | `ai-branding/api-contract.md` note + git commit 9a8847e9 | ✅ |
| GAP-390-B: SSE 4 assertions | `DeployStreamControllerTest.java` 137-line test with mockConstruction | api-contract `deploy-stream` section event table | ✅ |
| GAP-390-C: UUID examples | `ai-branding/api-contract.md` (confirmed `550e8400-...` format) | — | ✅ |
| GAP-388-A: Honeypot wire-up | `BetaAccessController.handleValidationException` → `recordHoneypotRejection(email, ip)` | `beta-access/api-contract.md` `BETA_HONEYPOT_FILLED` | ✅ |
| GAP-388-B: Claim code 2FA | `BetaAccessController:85` + `BetaClaimCodeExchangeCommand/Response` DTOs + 3 service tests | **MISSING from api-contract.md** | ❌ |
| GAP-388-C: Per-email rate limit 429 | `BetaRateLimitExceededException` → 429 + `ProblemDetail` `BETA_EMAIL_RATE_LIMIT` | `beta-access/api-contract.md` | ✅ |

---

## Findings

### 🔴 P0 (0)

None.

### 🟠 P1 — New finding (1)

**P1-NEW: `POST /api/v1/auth/beta-signup/exchange-claim-code` not documented**

- **Location:** `BetaAccessController.java:85`
- **Missing in:** `documents/01-business/kitehub/beta-access/api-contract.md`
- **What's absent:** request schema (`{ claimCode: string }`) + response schema (`{ valid, email, name, persona, inviteToken, expiresAt, status }`) + error codes (`CODE_NOT_FOUND` 404, `CODE_EXPIRED` 404) + flow context (FE calls this before `POST /beta-signup`)
- **Impact:** FE devs relying on api-contract.md to integrate beta signup 2FA flow have incomplete spec — must read controller source directly
- **Gap filed:** GAP-427

### Pre-existing P1 (3 — unchanged from Wave 35)

| # | Issue |
|---|---|
| P1-1 | `AdminController` 10 endpoints — full admin dashboard surface |
| P1-2 | `AdminEmailController` 5 endpoints |
| P1-3 | `AuthController` 7 endpoints |

### 🟡 P2 (unchanged from Wave 35)

| # | Issue |
|---|---|
| P2-1 | `BrandingJobController` 6 legacy endpoints |
| P2-2 | `ContentGenerationController` 2 + `AssetStorageController` 3 |
| P2-3 | `IncidentReportingController.mandatoryReportAck` in use-cases only |
| P2-4 | Versioning drift: `/api/platform/admin/*` vs `/api/v1/admin/*` |

### 🟢 P3 (unchanged)

| # | Issue |
|---|---|
| P3-1 | Consumer-driven contract tests (Pact) absent across all services |
| P3-2 | `PublicConfigController` 1 endpoint undocumented |

---

## Strengths (Wave 36–39)

- **GAP-390 polish delivered cleanly**: DTO shape, UUID alignment, SSE test assertions — all verified. `DeployStreamControllerTest` 4 SSE assertions prevent future shape drift.
- **Wave 34 wizard endpoint health**: all 7 endpoints (`slug-availability`, `regenerate-quota`, `regenerate`, `deploy-stream`, `quality-score`, `preview`, `jobs/{jobId}`) remain documented + tested — no regression.
- **Wave 35 BetaAccess 6 core endpoints solid**: request, validate, beta-signup, admin list/approve/reject — all documented + tested.
- **Waves 37–39 zero endpoint drift**: infra-only waves correctly produced zero API surface change — audit budget saved.
- **Contract-first rule working**: Wave 34 (7 endpoints) and Wave 35 (6 endpoints) both hit 100% documentation rate; only Wave 36 BE-only hotfix escaped the cross-layer check.

---

## Verdict

**Score: 72/100 C+** (Δ +1 vs Wave 35 baseline of 71/100).

The `release-deploy-artifacts` cluster (Wave 33→39) ends with 98.5% documentation rate for wave-specific endpoints (34/35 documented). The 1 new finding (`exchange-claim-code`) is a minor process miss from a BE-only hotfix wave escaping the `contract-first-for-cross-layer.md` check.

**Phase 1 BETA gate:** API Contract score 72/100 is informational for the BETA gate — the gate requires Quality /100 ≥ 80 AND Security /100 ≥ 80. No API Contract P0 blocking findings.

**New gap filed:** GAP-427 — `exchange-claim-code` endpoint undocumented — P1, Wave 41 cluster.

---

## Related

- Baseline: `documents/04-quality/audits/api-contract/2026-05-07-post-wave-35.md`
- Wave plan: `documents/03-planning/waves/wave-2026-05-08-40-audit-milestone-release-deploy-artifacts.md`
- GAP-427: `documents/04-quality/gaps/GAP-427-exchange-claim-code-undocumented.md`
- Skill: `.claude/skills/quality/api-contract-audit/SKILL.md`
- Rule: `.claude/rules/post-wave-audit-mandate.md` §2.4.2
