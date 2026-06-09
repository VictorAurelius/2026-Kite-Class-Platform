# GAP-1107: AI Branding mock-provision — rollback-only intermittent + assetsGenerated parse 0-assets

**Status:** 🟡 PARTIAL (80%)
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-09 (G2 browser-walk wizard deploy — instance 7862ab7e)
**Affects:** kitehub-branding `MockProvisioningService` + `BrandingJobService` (lifecycle txn), `AssetStorageController.parseAssetsJson`

## Problem

Browser-walk wizard deploy lộ 2 BE bug (backend mostly succeeds nhưng 2 lỗi thật):

1. **Rollback-only intermittent (P1)** — lifecycle event 2026-06-09 09:57:59 `REGENERATING → FAILED: "Triển khai mock thất bại: Transaction silently rolled back because it has been marked as rollback-only"`. Class `audit-service-isolation.md` §3.11 (`UnexpectedRollbackException`): trong `MockProvisioningService.provisionAsync` flow (job updates + lifecycle transitions), một DB op đánh dấu txn rollback-only (caught local nhưng flag persist) → parent commit throw. INTERMITTENT — đa số REGENERATE attempts thành công (17:56:17 OK); chỉ fail 1 lần. Cần repro + fix: lifecycle-event recording / side-effect write dùng `Propagation.REQUIRES_NEW` HOẶC tách txn boundary trong provisionAsync.

2. **assetsGenerated parse 0-assets (P2)** — `AssetStorageController.parseAssetsJson` (line 257) `readValue(json, List<BrandingAsset>)` nhưng mock provision ghi `assetsGenerated` = JSON OBJECT (`{slug, templateId, approvedResources, frontendUrl, brandColors, mock}`) KHÔNG phải array → `MismatchedInputException: Cannot deserialize ArrayList<BrandingAsset> from Object value` → "Retrieved 0 assets". Assets không hiển thị trong preview/approve. Mock provision (GAP-1021) chưa ghi đúng shape BrandingAsset[]. Fix: mock provision ghi `assetsGenerated` đúng shape array HOẶC parser handle object metadata shape.

## Acceptance Criteria

- [ ] Rollback-only: repro REGENERATE intermittent fail trên Postgres; fix txn isolation (REQUIRES_NEW side-effect OR txn boundary split); IT verify N consecutive REGENERATE no rollback-only — **best-effort: REQUIRES_NEW applied trên `recordMarker`, repro pending (xem §Notes)**
- [x] assetsGenerated: mock provision ghi đúng shape `BrandingAsset[]`; getAssets trả ≥1 asset post-deploy; preview hiển thị assets — **DONE** (writer-fix + parser array-guard + unit round-trip test)
- [ ] Browser-walk REGENERATE × 5 liên tiếp → no FAILED, assets hiển thị — **pending coordinator runtime-walk**

## Fix (PR `agent/gap-1107-1108-branding-postdeploy`)

**#2 assetsGenerated Object-vs-Array (DONE):**
- `MockProvisioningService.persistAssets` (thay `persistTheme`) ghi `assetsGenerated` đúng shape `BrandingAsset[]` qua `buildDeployedAssets(slug, templateId, approvedResources, colours)` — 1 asset/approved-resource (fallback `DEFAULT_RESOURCES` = logo/colors/banner/hero khi rỗng), mock CDN URL giữ segment `/instances/{slug}/` cho delete-path. Theme metadata (frontendUrl/brandColors) chuyển sang `deploy-completed` lifecycle marker, KHÔNG còn nhét vào `assetsGenerated`.
- `AssetStorageController.parseAssetsJson` thêm array-guard: JSON không bắt đầu `[` (legacy theme-object row) → trả empty + debug-log thay vì error-level `MismatchedInputException` stack trace.
- Test: `MockProvisioningServiceTest` (3) — round-trip `buildDeployedAssets → writeValueAsString → readValue(TypeReference<List<BrandingAsset>>)` ≥4 assets; default-fallback; COLORS variant.

**#1 rollback-only (best-effort):**
- `InstanceLifecycleService.recordMarker` → `@Transactional(propagation = REQUIRES_NEW)` per `audit-service-isolation.md` §3.11 — marker INSERT (gồm `deploy-completed`) chạy txn riêng, fail không poison caller. `recordDeployMarker` đã wrap try/catch sẵn.
- Test: `InstanceLifecycleServiceTest.recordMarkerIsolatedInRequiresNewTransaction` (reflection assert propagation).

## Notes

- Rollback-only **best-effort REQUIRES_NEW applied, repro pending**: lỗi `UnexpectedRollbackException` quan sát 2026-06-09 09:57:59 là INTERMITTENT (đa số REGENERATE OK). `BrandingJobService.transitionInstance` đã pre-validate reachability (tránh `IllegalStateException` poison — GAP-1021 fix). Nguồn còn lại: `eventRepo.save`/`outboxEmitter.emit` bên trong `transition` join parent txn của `updateJobProgress`. Cô lập event-insert trong `transition` (vẫn giữ outbox same-txn per `design-patterns.md` §3.5.1) cần refactor sâu state-machine core + repro DB-event-failure dưới concurrent REGENERATE (non-deterministic) → deferred coordinator runtime-walk (REGENERATE × 5).

## Related

- Origin: G2 browser-walk (cùng session GAP-1105 deploy-stream FE fixes)
- GAP-1021 (Agent D deploy pipeline) needs-rework parent
- GAP-1108 (#2 chung root — post-deploy assets-0)
- Rule: `audit-service-isolation.md` §3.11 (rollback-only class) + `postgres-specific-type-testcontainers.md`
