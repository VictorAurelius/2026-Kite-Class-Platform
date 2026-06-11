# GAP-1112: AI Branding logo upload UX cluster — preview / dedup / reuse (G2 walk)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-10 (G2 walk recipe 1 — user-flagged 3 logo-upload issues)
**Affects:** `kitehub-branding` `AssetStorageController` + `S3StorageService` + MinIO; `kitehub-frontend` branding wizard

## Problem

G2 walk AI Branding deploy (recipe 1) phát hiện 3 vấn đề UX khi upload logo. Tách 3 sub-issue (có thể split thành gap riêng khi fix):

### #1 — Logo upload không preview được (MinIO access, KHÔNG phải CSP)

Ảnh upload trả URL `http://localhost:9100/instances/{id}/branding/LOGO/xxx.png` (MinIO) → không preview. User thấy CSP report-only violation `img-src 'self' data: https: blob:` — NHƯNG report-only KHÔNG block, nên đó là noise, không phải nguyên nhân. Preview fail thực do MinIO object không serve được tới browser (bucket không public-read / cần presigned URL / hoặc URL scheme).
- **Đã làm cùng PR (cosmetic)**: `next.config.js` CSP dev-aware thêm `http://localhost:9100` vào `img-src` + `http://localhost:9000`/`ws://` vào `connect-src` (giảm noise + chuẩn bị enforce).
- **Còn lại**: verify MinIO bucket public-read HOẶC chuyển sang presigned GET URL trong `S3StorageService` để browser load được ảnh.

### #2 — Re-upload logo tích lũy asset (4 upload = 4 asset)

`AssetStorageController.uploadAsset` (POST `/{instanceId}/{assetType}`) lưu 1 `BrandingAsset` row + 1 S3 object MỖI lần upload, KHÔNG replace-by-assetType. User xóa+upload lại 4 lần → job lưu cả 4 ảnh LOGO. **Nên: 1 LOGO per instance** (replace ảnh cũ khi upload mới cùng assetType).
- **Fix**: `uploadAsset` xóa asset cũ (S3 object + DB row) cùng `assetType` cho `instanceId` TRƯỚC khi lưu mới; HOẶC dedup keep-latest lúc tạo job. + IT.

### #3 — Không reuse được asset đã upload trong wizard 6 bước

Logo đã vào assets rồi nhưng wizard (6 bước) không có bước chọn/dùng lại asset có sẵn (chỉ có upload mới ở Step 2). **Feature gap**: thêm asset-library/picker để chọn logo đã upload trước.
- **Fix**: wizard Step 2 (upload logo) thêm tab/section "Chọn từ đã upload" → list assets `GET /api/v1/branding/.../{instanceId}` → pick.

## Acceptance Criteria

- [ ] #1: logo preview load được trên browser (MinIO public-read hoặc presigned URL) + CSP dev-host (đã làm)
- [ ] #2: re-upload cùng assetType replace ảnh cũ (1 LOGO per instance) + IT verify
- [ ] #3: wizard cho phép chọn/reuse asset đã upload (asset picker Step 2)

## Related

- Discovered in: G2 walk recipe 1 (`2026-06-10-g2-recipe-kh-branding-deploy.md`), PR #2279
- #1 CSP fix: `next.config.js` (this session)
- #4 (env-separated landing URL) — đã fix riêng (MockProvisioningService env-aware + compose override); local landing render vẫn gated GAP-811/1077
- `ai-branding-guidelines.md` §4.1 wizard 6-step, §4.2 preview-before-commit

## Log

- **2026-06-10 (PARTIAL 70%):** #2 dedup DONE — `AssetStorageController` replace-by-assetType (xóa S3 object + DB row cùng assetType trước khi lưu mới) + `AssetStorageControllerDedupTest` 3/3. #1 presigned code DONE — `S3StorageService.getPresignedAssetUrl` (1h time-limited, mock-aware, fallback) + 11/11 unit; browser load = G2. #3 asset picker code DONE — `LogoStep` "chọn logo đã upload" grid + test; browser = G2. Còn lại = browser G2 walk verify #1 (MinIO preview) + #3 (picker e2e). Wave branding-fix-2026-06-10 (agent af9cb327 BE + abc8207d FE).
- **2026-06-10:** Filed từ G2 walk recipe 1 (user-flagged 4 logo issue). #1 CSP cosmetic-fixed cùng PR; #1 preview-real + #2 dedup + #3 reuse defer wave branding-fix (cần logic + S3 + feature + IT). #4 env-URL fixed riêng. Per `discovery-to-gap-inline-filing.md` + `small-gap-inline-fix.md` (large → defer).
