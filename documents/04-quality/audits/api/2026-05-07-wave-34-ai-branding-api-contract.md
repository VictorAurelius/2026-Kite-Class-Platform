# API Contract Audit — Wave 34 AI Branding (7 endpoints)

**Date:** 2026-05-07
**Auditor:** Background agent ad28b70c (Sonnet, Explore subagent)
**Scope:** Bucket 0 Foundation contract + Buckets A/B/C/D implementations

---

## Score: 72/100 — C

| Category | Score | Notes |
|----------|:-----:|-------|
| Endpoint Coverage | 18/20 | 7/7 implemented |
| Request/Response Match | 14/20 | POST /regenerate returns entity; tenantId hardcoded null |
| Error Code Consistency | 18/20 | Codes match; 404 vs 409 in preview slight inconsistency |
| Versioning/Deprecation | 20/20 | Clean v1 segregation; no breaking |
| IT Coverage | 16/20 | 7 endpoints tested; SSE event payload assertions missing |

**Subtotal 86 → Final 72/100** (deduction for blocking drift)

---

## Endpoint Audit Table

| # | Endpoint | Contract | Controller | DTO | IT | Drift |
|---|----------|:---:|:---:|:---:|:---:|-------|
| 1 | GET /slug-availability | ✓ | ✓ | ✓ | ✓ | None |
| 2 | GET /regenerate-quota | ✓ | ✓ | ✓ | ✓ | None |
| 3 | POST /jobs/{id}/regenerate | ✓ | ✓ | **✗** | ✓ | **Response shape — entity vs DTO** |
| 4 | GET /jobs/{id}/deploy-stream | ✓ | ✓ | ✓ | △ | SSE payload assertions missing |
| 5 | GET /jobs/{id}/quality-score | ✓ | ✓ | ✓ | ✓ | None |
| 6 | GET /jobs/{id}/preview | ✓ | ✓ | ✓ | ✓ | 404 vs 409 status inconsistent |
| 7 | GET /instances/{id}/lifecycle/events | ✓ | ✓ | ✓ | ✓ | None |

---

## Findings

| # | Sev | Issue |
|---|:---:|-------|
| 1 | 🔴 BLOCKING | POST /regenerate returns raw `BrandingJob` instead of `BrandingJobResponse` — `BrandingWizardController.java:109` (already tracked **GAP-272n**) |
| 2 | 🟠 P1 | `BrandingJobResponse.from()` line 54 sets `tenantId=null` — blocked on Bucket C FrontendInstance integration |
| 3 | 🟠 P1 | SSE event assertions missing trong `DeployStreamControllerTest` (no validation of `state-change`/`progress`/`complete` payloads) |
| 4 | 🟡 P2 | Path param type mismatch — contract shows numeric `instanceId` (12345) but impl uses UUID. Reconcile docs hoặc convert |

---

## Gap Recommendations

- **EXISTING**: GAP-272n (POST /regenerate response shape) — confirmed bởi 2 audits độc lập
- **NEW P1**: Wire tenantId lookup từ FrontendInstance trong DTO factory (Bucket C dependency)
- **NEW P1**: Add SSE event assertions (Wave 35 follow-up)
- **NEW P2 doc**: Reconcile api-contract.md numeric example IDs với UUID actual type

---

## Strengths

- All 7 endpoints exist với correct method/path
- Headers properly validated (Idempotency-Key, X-Instance-Id, X-Subscription-Tier)
- Error envelopes match contract (INVALID_SLUG_FORMAT, AI_REGENERATE_QUOTA_EXCEEDED)
- Controller segregation clean: `/api/v1/branding/` (Wave 34) vs `/api/platform/` (legacy)

## 1-line summary

Contract functionally complete (7/7 endpoints) but POST /regenerate returns entity instead of DTO + tenantId unpopulated — fix before merge per existing GAP-272n; SSE assertions + tenantId wire as P1 follow-up.
