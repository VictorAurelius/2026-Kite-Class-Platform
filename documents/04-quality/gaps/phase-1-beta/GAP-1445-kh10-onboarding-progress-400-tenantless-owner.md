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

KHÔNG fix mù (đổi BE → 200-empty có thể che misconfiguration owner-không-có-tenant). Cần quyết định KH-1 onboarding design: onboarding tenant-scoped hay platform-scoped cho tenantless owner? 2 hướng: (a) FE skip call khi user không có tenantId; (b) BE trả empty progress cho platform owner. Defer tới KH-1 onboarding scope wave.

## Related
- Discovered in: Phase-2 browser walk (flow KH-10), 2026-06-16
- Out-of-scope KH-10; thuộc flow KH-1 onboarding
- Phase-3 verdict: DEFER (design-first — needs KH-1 onboarding scope decision)
