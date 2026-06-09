# GAP-1108: Post-deploy /branding trống — thiếu deploy-success summary + link landing + assets 0

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Frontend + Backend)
**Found:** 2026-06-09 (G2 browser-walk — deploy 100% → redirect /branding rỗng)
**Affects:** kitehub-frontend `(customer)/branding/page.tsx`, wizard complete handler `Step6Preview.tsx:686 onDeploy`, kitehub-branding `AssetStorageController.getAssets`

## Problem

Sau khi AI Branding wizard deploy THÀNH CÔNG (deploy-stream 100% + instance DEPLOYED), wizard redirect về `/branding` (`Step6Preview.tsx:686-688` → `onDeploy()`). NHƯNG trang `/branding` **rỗng/không phản ánh lần deploy thành công**:

1. **Dữ liệu lần thành công thiếu** — `(customer)/branding/page.tsx` thuần assets-centric (`useAssets(instanceId)` + `useBrandingTier`). KHÔNG fetch/hiển thị instance lifecycle state (DEPLOYED) hay metadata lần provision.
2. **Assets = 0** — `useAssets` → `GET /instances/{id}/assets` → `AssetStorageController.parseAssetsJson` bug Object-vs-Array (mock provision ghi `assetsGenerated` = metadata object, parser expect `List<BrandingAsset>` → `MismatchedInputException` → 0 assets). Cross-ref **GAP-1107 #2**. → stats `assets?.length||0` toàn 0, không preview.
3. **Không thông báo success** — redirect wizard→/branding không mang toast "Triển khai thành công".
4. **Không link truy cập landing mới** — `frontendUrl` (vd `https://toan-master.kiteclass.vn`, set bởi MockProvisioningService) KHÔNG hiển thị → user không biết cách xem landing vừa deploy.

## Proposed Fix

- **BE:** fix `parseAssetsJson` (GAP-1107 #2) HOẶC mock provision ghi đúng shape `BrandingAsset[]` → assets hiển thị.
- **FE `/branding`:** thêm deploy-status card — fetch instance lifecycle (DEPLOYED) + `frontendUrl` → hiển thị "Trang web của bạn đã sẵn sàng" + nút/link "Xem landing" (mở `frontendUrl`) + summary lần deploy gần nhất (template, ngày, resources).
- **FE wizard complete:** success toast trước/khi redirect ("Triển khai thành công — xem landing tại ...").

## Acceptance Criteria

- [ ] Deploy xong → /branding hiển thị trạng thái DEPLOYED + link landing (`frontendUrl`) clickable
- [ ] Assets hiển thị (≥1 sau deploy, không 0) — GAP-1107 #2 fix
- [ ] Success toast/notification sau deploy
- [ ] Browser re-walk: deploy 100% → /branding có data + link + thông báo (không rỗng)

## Related

- GAP-1107 #2 (assets parse Object-vs-Array — same root cho assets-0)
- GAP-1105 (deploy-stream fixes — deploy giờ chạy 100%)
- GAP-1021 (Agent D deploy pipeline) needs-rework parent
- Rule: `ai-branding-guidelines.md` §4.2 (preview before commit) + post-deploy UX
