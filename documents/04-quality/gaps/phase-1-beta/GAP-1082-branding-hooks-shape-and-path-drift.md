# GAP-1082: use-branding.ts 7 hooks shape mismatch (data.data trên bare BE) + path drift /api/platform vs /api/v1

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-09 (KH-3 G2 walk — cross-flow sweep của GAP-1079 shape bug)
**Affects:** `kitehub-frontend` `use-branding.ts` (7 hooks) — KH-6 AI Branding wizard flow

## Problem

Cross-flow sweep (per `cross-flow-bug-class-sweep`) của GAP-1079 (subscription/payment shape bug) phát hiện `use-branding.ts` cùng bug class + thêm 1 drift:

1. **Shape mismatch (same class GAP-1079):** 7 hooks (`useUploadAsset` :34, `useAnalyzeLogo` :52, `useCreateBrandingJob` :71, `useBrandingJob` :87, `useJobAssets` :110, `useAssets` :126, `useGenerateContent` :145) đọc `data.data` (expect `ApiResponse<T>` wrapper) nhưng kitehub-branding BE KHÔNG có `ApiResponse<` nào (bare DTO) → `data.data` = undefined → KH-6 wizard crash/empty.
2. **Path drift (GAP-1069 class):** FE `endpoints.branding.*` dùng base `/api/platform/branding/*` nhưng BE branding controllers `@RequestMapping("/api/v1/branding")` (BrandingWizardController/DeployStreamController/LifecycleEventsController). curl `/api/platform/branding/assets/{id}` → 404. Cần verify gateway có rewrite `/api/platform/branding`→`/api/v1/branding` không, hoặc FE/BE path mismatch thật.

## Why deferred (not fixed in GAP-1079 sweep)

KH-6 (AI Branding) là flow KHÁC, không walk tại KH-3 G2 session. Path drift (#2) cần verify gateway routing thật + KH-6 walk-time investigation (curl-verify shape + path) trước khi đổi 7 hooks (tránh phá KH-6). Per `cross-flow-bug-class-sweep` decision matrix = DEFER (same class, different flow, needs walk verification).

## Proposed Fix (tại KH-6 G2 re-walk)

- Verify gateway route `/api/platform/branding/**` → kitehub-branding `/api/v1/branding/**` rewrite (StripPrefix/RewritePath?). Nếu không có → fix FE endpoints HOẶC BE path.
- Sau khi path đúng + curl-verify branding bare: fix 7 hooks `data.data`→`data` + `ApiResponse<X>`→`X` (như GAP-1079 use-subscriptions/use-payments).

## Acceptance Criteria

- [ ] Gateway branding route verified (rewrite hoặc path align)
- [ ] curl-verify branding endpoint shape (bare vs wrapped)
- [ ] 7 use-branding.ts hooks fixed match BE shape
- [ ] KH-6 G2 browser walk: generate→job→assets→apply không crash/empty

## Related

- Discovered in: KH-3 G2 cross-flow sweep 2026-06-09
- Same shape class: GAP-1079 (subscription/payment data.data→bare, fixed)
- Path drift class: GAP-1069 (FE↔BE contract), GAP-1070 (detector)
- Flow: KH-6 (campaign §4 🔄 walk-pass-pending-human) — fix tại KH-6 G2 re-walk
