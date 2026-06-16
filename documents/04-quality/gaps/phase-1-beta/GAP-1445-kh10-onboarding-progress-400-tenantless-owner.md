# GAP-1445: Dashboard load trả 400 GET /api/v1/onboarding-progress cho owner không có tenant (KH-1 onboarding scope)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-10)
**Affects:** KH-1 onboarding — `kitehub-frontend` dashboard onboarding hook → `GET /api/v1/onboarding-progress` (`kitehub-subscription`)

## Problem
Discovered Phase-2 browser walk KH-10 (quan sát out-of-scope KH-10, thuộc KH-1/onboarding). `owner.test` không có `tenantId` → `GET /api/v1/onboarding-progress` trả 400 khi load dashboard; có thể liên quan tới việc các surface do `OnboardingCoordinator` điều khiển không gate đúng.

## Proposed Fix
Triage dưới flow KH-1 onboarding: xử lý owner platform không có tenant (trả empty progress hoặc 200 với state phù hợp thay vì 400).

## Acceptance Criteria
- [ ] `GET /api/v1/onboarding-progress` cho owner tenantless không trả 400 (trả empty/200 hợp lý)

## Triage (Phase-3, 2026-06-16) — DEFER, cần design decision (design-first)
Per `design-first-investigation-order.md`: `OnboardingProgressController.resolveTenant()` ném `TenantContextMissingException` → 400 khi `X-Tenant-Id` header missing AND JWT `tenantId` claim absent (`onboarding/controller/OnboardingProgressController.java:107`). Onboarding-progress là resource **tenant-scoped** (checklist per-tenant) → 400 có thể ĐÚNG design, không phải bug. "Bug" thật = FE dashboard gọi endpoint cho owner platform tenantless.

KHÔNG fix mù (đổi BE → 200-empty TRÁI design — `onboarding/api-contract.md` line 88 nói missing tenant → **403 TENANT_CONTEXT_MISSING**, reject là intended).

**Design-first findings (2026-06-16, đọc rules.md + api-contract.md + auth-store):**
1. **Reject-khi-tenantless = đúng design** (BR-ONBOARD-001 onboarding per-tenant 1:1 tenant_id).
2. **Status drift:** code trả **400** nhưng contract nói **403 TENANT_CONTEXT_MISSING** → BE status-code drift (quick design-conformance fix khả thi riêng).
3. **FE-guard không sạch:** `auth-store.ts` `User` type chỉ có `{id,email,role}` — **KHÔNG có tenantId** (tenant context sống trong JWT, gateway inject X-Tenant-Id). FE không biết user có tenant → `enabled: !!tenantId` guard cần thêm tenantId vào auth-store/JWT-decode = **design decision** (where tenant context lives client-side).

**Đề xuất 2 phần (defer KH-1 wave):** (a) BE 400→403 align contract (low-risk conformance); (b) FE tenant-aware: thêm tenantId vào client context → guard fetch + render checklist gracefully cho tenantless owner (hide thay vì error). (b) cần auth-store shape decision.

## Related
- Discovered in: Phase-2 browser walk (flow KH-10), 2026-06-16
- Out-of-scope KH-10; thuộc flow KH-1 onboarding
- Phase-3 verdict: DEFER (design-first — needs KH-1 onboarding scope decision)
