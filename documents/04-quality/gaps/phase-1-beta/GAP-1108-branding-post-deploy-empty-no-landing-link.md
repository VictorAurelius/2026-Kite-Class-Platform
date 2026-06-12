# GAP-1108: Post-deploy /branding trống — thiếu deploy-success summary + link landing + assets 0

**Status:** 🟡 PARTIAL (80%)
**Priority:** 🟠 P1
**Domain:** Mixed (Frontend + Backend)
**Found:** 2026-06-09 (G2 browser-walk — deploy 100% → redirect /branding rỗng)
**Affects:** kitehub-frontend `(customer)/branding/page.tsx`, wizard complete handler `Step6Preview.tsx:686 onDeploy`, kitehub-branding `AssetStorageController.getAssets` + `LifecycleEventsController`

## Problem

Sau khi AI Branding wizard deploy THÀNH CÔNG (deploy-stream 100% + instance DEPLOYED), wizard redirect về `/branding` (`Step6Preview.tsx:686-688` → `onDeploy()`). NHƯNG trang `/branding` **rỗng/không phản ánh lần deploy thành công**:

1. **Dữ liệu lần thành công thiếu** — `(customer)/branding/page.tsx` thuần assets-centric (`useAssets(instanceId)` + `useBrandingTier`). KHÔNG fetch/hiển thị instance lifecycle state (DEPLOYED) hay metadata lần provision.
2. **Assets = 0** — `useAssets` → `GET /instances/{id}/assets` → `AssetStorageController.parseAssetsJson` bug Object-vs-Array (mock provision ghi `assetsGenerated` = metadata object, parser expect `List<BrandingAsset>` → `MismatchedInputException` → 0 assets). Cross-ref **GAP-1107 #2**. → stats `assets?.length||0` toàn 0, không preview.
3. **Không thông báo success** — redirect wizard→/branding không mang toast "Triển khai thành công".
4. **Không link truy cập landing mới** — `frontendUrl` (vd `https://toan-master.kitehub.me`, set bởi MockProvisioningService) KHÔNG hiển thị → user không biết cách xem landing vừa deploy.

## Proposed Fix

- **BE:** fix `parseAssetsJson` (GAP-1107 #2) HOẶC mock provision ghi đúng shape `BrandingAsset[]` → assets hiển thị.
- **FE `/branding`:** thêm deploy-status card — fetch instance lifecycle (DEPLOYED) + `frontendUrl` → hiển thị "Trang web của bạn đã sẵn sàng" + nút/link "Xem landing" (mở `frontendUrl`) + summary lần deploy gần nhất (template, ngày, resources).
- **FE wizard complete:** success toast trước/khi redirect ("Triển khai thành công — xem landing tại ...").

## Acceptance Criteria

- [x] Deploy xong → /branding hiển thị trạng thái DEPLOYED + link landing (`frontendUrl`) clickable — **DONE in code** (deploy-status endpoint + card; vitest render-verified; runtime-walk pending)
- [x] Assets hiển thị (≥1 sau deploy, không 0) — GAP-1107 #2 fix — **DONE**
- [x] Success toast/notification sau deploy — **DONE in code** (`Step6Preview` complete handler; runtime-walk pending)
- [ ] Browser re-walk: deploy 100% → /branding có data + link + thông báo (không rỗng) — **pending coordinator runtime-walk**

## Fix (PR `agent/gap-1107-1108-branding-postdeploy`)

**BE — deploy-status endpoint:**
- `MockProvisioningService.recordDeployMarker` extend marker metadata với `templateId` + `slug` (cạnh `frontendUrl`).
- New `GET /api/v1/branding/instances/{id}/deploy-status` (`LifecycleEventsController`) → `DeployStatusResponse {instanceId, state, deployed, frontendUrl, templateId, slug, brandingVersion, deployedAt}` — đọc `BrandingInstanceState` (state/version) + latest `deploy-completed` marker (frontendUrl/templateId/slug/deployedAt).
- Assets-0 fix: cross-ref GAP-1107 #2 (mock ghi `BrandingAsset[]` + parser array-guard).
- Test: `LifecycleEventsControllerTest` (2 mới) — deployed + frontendUrl; not-deployed empty.

**FE:**
- `useBrandingDeployStatus(instanceId)` hook + endpoint `brandingV1.instanceDeployStatus`.
- `(customer)/branding/page.tsx` — deploy-success card trên cùng (chỉ hiện khi `deployStatus.deployed`): "Trang web của bạn đã sẵn sàng 🎉" + nút "Xem landing" (`<a target=_blank href={frontendUrl}>`) + summary (template + ngày).
- `Step6Preview.tsx` — `toast.success('Triển khai thành công — ...')` trên SSE `complete` trước `onDeploy()` (ref-guarded fire-once).
- Test: `(customer)/branding/__tests__/page.test.tsx` (2 mới) — card hidden khi !deployed; card + landing link khi DEPLOYED.

## Related

- GAP-1107 #2 (assets parse Object-vs-Array — same root cho assets-0)
- GAP-1105 (deploy-stream fixes — deploy giờ chạy 100%)
- GAP-1021 (Agent D deploy pipeline) needs-rework parent
- GAP-1213 (cross-service `branding.deployed` propagation — Wave branding-100 Bucket C)
- Rule: `ai-branding-guidelines.md` §4.2 (preview before commit) + post-deploy UX

## Log

- **2026-06-12** (Wave branding-100 Bucket C — landing-link mở rộng sang event chuỗi): Chuỗi post-deploy "event cuối chứa link landing" giờ phủ thêm cross-service: `branding.deployed` event (GAP-1213) carry `frontendUrl` sang KC-core → đúng landing per-tenant đổi theme thật (không chỉ deploy-status card BE-only như AC #1-#3 đã có). AC #4 (browser re-walk: deploy 100% → /branding có data + link) vẫn pending coordinator runtime-walk — Status giữ PARTIAL.

## Log — 2026-06-12 G1 walk: SSE complete thiếu frontendUrl (Bug #5) + fix

G1 walk: DoneStep render đẹp nhưng landing link = `https://sky-education-2.kitehub.me`
(FE fallback) vì SSE `complete` body chỉ `{jobId, finalStatus, ts}` — KHÔNG mang frontendUrl.
Fix: `DeployStreamController.emitTerminal` đọc marker `deploy-completed` (same source
deploy-status GAP-1108) → thêm `frontendUrl` vào complete event; FE `DoneStep` fallback đổi
env-driven `NEXT_PUBLIC_TENANT_LANDING_URL_TEMPLATE` (local default `http://localhost:3000/?tenant={slug}`
— hết deadlink GAP-803 class). api-contract.md sync cùng PR. Chờ re-walk G1.
