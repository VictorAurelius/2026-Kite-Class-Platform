---
gap_id: GAP-704
title: JWT lacks tenantId claim post-beta-signup — onboarding-progress 400
status: OPEN
priority: P0
domain: Backend
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-22
last_updated: 2026-05-22
filed_by: Wave 103 Bucket B live verify
---

# GAP-704 — JWT lacks tenantId claim post-beta-signup

## Problem

After Owner (chị Hằng) completes beta-signup flow (`POST /api/v1/auth/beta-signup`), the issued access JWT for that Owner does NOT include `tenantId` claim, despite the user being bound to a tenant via `instances.owner_id` table.

**Evidence (Wave 103 Bucket B live verify 2026-05-22):**

```
Owner login JWT claims (decoded):
{
  "sub":"ff47940a-...",
  "email":"hong.test+wave103@skyedu.vn",
  "role":"OWNER",
  "type":"access"
  // ❌ MISSING: "tenantId" field
}

Direct consequence:
GET /api/v1/onboarding-progress 
  → 400 Bad Request
  → "X-Tenant-Id header required + JWT cross-check failed"

DB state:
users.tenant_id = NULL          ← Owner row binding not set
instances.owner_id = <user_id>  ← Tenant→Owner binding only stored here
```

**Impact (P0 — blocks owner onboarding):**
- Owner cannot reach `/api/v1/onboarding-progress` (5-step wizard endpoint)
- Owner cannot complete Day-1 setup → tenant stuck in trial-but-not-onboarded state
- All tenant-scoped APIs that read `tenantId` from JWT fail or require extra `X-Tenant-Id` header workaround

## Context

- Wave 33 GAP-372 shipped beta-signup endpoint — schema decision used `instances.owner_id` as source of truth for tenant↔owner binding
- Wave 78 GAP-531 supposedly addressed tenant init handoff — but this aspect missed
- Wave 102.9 Bucket B state-check retained 50% PARTIAL — live verify Wave 103 now reveals THIS is the missing piece
- Sister design issue: `users.tenant_id` is NULL after signup (only `instances.owner_id` populated) — likely intentional Phase 1 BETA multi-tenant design BUT JWT issuance forgot to look up the binding

## Proposed Fix

1. **Diagnose** `AuthService.issueAccessToken(user)` — find where claims are built
2. **Add tenant lookup** in claim builder:
   - For OWNER role: query `instances WHERE owner_id = user.id` → set `tenantId` claim
   - For OTHER tenant-scoped roles: query `tenant_admins` or equivalent
3. **Alternative (cleaner)**: populate `users.tenant_id` at beta-signup time (denormalization for fast claim lookup) — but check if multi-tenant Owner supported (1 user → N tenants)
4. **Add integration test** `AuthServiceIT.ownerJwtIncludesTenantIdAfterSignup()`
5. **Verify** via curl: signup → login → decode JWT → assert tenantId present

## Acceptance Criteria

- [ ] Owner JWT post-signup contains `tenantId` claim matching tenant created during approve
- [ ] `GET /api/v1/onboarding-progress` returns 200 with Bearer JWT (no X-Tenant-Id header needed)
- [ ] IT `AuthServiceIT.ownerJwtIncludesTenantIdAfterSignup()` PASS
- [ ] Live verify: full signup → approve → login → JWT decode shows tenantId
- [ ] GAP-531 status revised to higher % when this lands
- [ ] Decision documented in `business-logic/auth/rules.md`: 1 user → 1 tenant (Phase 1 BETA) vs 1 user → N tenants (Phase 2)

## Related

- [[GAP-531]] Tenant init handoff (this is the root cause)
- [[GAP-538]] Day-1 onboarding wizard (blocked by this)
- [[GAP-702]] Approval email not firing (separate but same flow)
- Wave 103 audit: `documents/04-quality/audits/local-stack/2026-05-22-wave-103-owner-persona-walk.md`
- Wave 33 GAP-372 (BetaAccessController + beta-signup endpoint)
