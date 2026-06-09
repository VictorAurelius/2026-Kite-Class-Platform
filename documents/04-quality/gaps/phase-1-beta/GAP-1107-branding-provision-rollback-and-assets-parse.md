# GAP-1107: AI Branding mock-provision — rollback-only intermittent + assetsGenerated parse 0-assets

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-09 (G2 browser-walk wizard deploy — instance 7862ab7e)
**Affects:** kitehub-branding `MockProvisioningService` + `BrandingJobService` (lifecycle txn), `AssetStorageController.parseAssetsJson`

## Problem

Browser-walk wizard deploy lộ 2 BE bug (backend mostly succeeds nhưng 2 lỗi thật):

1. **Rollback-only intermittent (P1)** — lifecycle event 2026-06-09 09:57:59 `REGENERATING → FAILED: "Triển khai mock thất bại: Transaction silently rolled back because it has been marked as rollback-only"`. Class `audit-service-isolation.md` §3.11 (`UnexpectedRollbackException`): trong `MockProvisioningService.provisionAsync` flow (job updates + lifecycle transitions), một DB op đánh dấu txn rollback-only (caught local nhưng flag persist) → parent commit throw. INTERMITTENT — đa số REGENERATE attempts thành công (17:56:17 OK); chỉ fail 1 lần. Cần repro + fix: lifecycle-event recording / side-effect write dùng `Propagation.REQUIRES_NEW` HOẶC tách txn boundary trong provisionAsync.

2. **assetsGenerated parse 0-assets (P2)** — `AssetStorageController.parseAssetsJson` (line 257) `readValue(json, List<BrandingAsset>)` nhưng mock provision ghi `assetsGenerated` = JSON OBJECT (`{slug, templateId, approvedResources, frontendUrl, brandColors, mock}`) KHÔNG phải array → `MismatchedInputException: Cannot deserialize ArrayList<BrandingAsset> from Object value` → "Retrieved 0 assets". Assets không hiển thị trong preview/approve. Mock provision (GAP-1021) chưa ghi đúng shape BrandingAsset[]. Fix: mock provision ghi `assetsGenerated` đúng shape array HOẶC parser handle object metadata shape.

## Acceptance Criteria

- [ ] Rollback-only: repro REGENERATE intermittent fail trên Postgres; fix txn isolation (REQUIRES_NEW side-effect OR txn boundary split); IT verify N consecutive REGENERATE no rollback-only
- [ ] assetsGenerated: mock provision ghi đúng shape OR parser handle; getAssets trả ≥1 asset post-deploy; preview hiển thị assets
- [ ] Browser-walk REGENERATE × 5 liên tiếp → no FAILED, assets hiển thị

## Related

- Origin: G2 browser-walk (cùng session GAP-1105 deploy-stream FE fixes)
- GAP-1021 (Agent D deploy pipeline) needs-rework parent
- Rule: `audit-service-isolation.md` §3.11 (rollback-only class) + `postgres-specific-type-testcontainers.md`
