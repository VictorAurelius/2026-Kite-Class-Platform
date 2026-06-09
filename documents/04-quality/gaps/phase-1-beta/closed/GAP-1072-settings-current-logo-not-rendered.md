# GAP-1072: Settings không render logo hiện tại + presigned URL hết hạn

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend (+ Backend presigned URL TTL)
**Found:** 2026-06-08 (KC-1 G2 Bước 3)
**Affects:** `components/settings/branding-settings.tsx` logo section; MinIO presigned URL TTL

## Problem

`branding.logo_url` CÓ giá trị (sky-logo.png presigned MinIO :9100) nhưng UI Settings ghi "Logo hiện tại / Không có tệp nào được chọn" → **không render preview logo hiện tại**, chỉ hiện control upload trống. Hai vấn đề:
1. **FE:** BrandingSettings không bind/hiển thị `logo_url` hiện tại (img preview).
2. **BE/infra:** presigned URL `X-Amz-Date=20260529 + X-Amz-Expires=604800` (7 ngày) → **hết hạn ~2026-06-05**; render cũng 403. Logo URL nên regenerate khi GET branding HOẶC dùng TTL dài/proxy.

## Proposed Fix

(1) BrandingSettings render `<img src={branding.logoUrl}>` khi có logo_url + fallback "chưa có logo"; (2) BE regenerate presigned URL mỗi lần serve branding (hoặc proxy qua gateway, không trả URL hết hạn).

## Acceptance Criteria

- [x] BE regenerate-on-read shipped (BrandingServiceImpl + storage.renderableUrl); 12/12 test PASS
- [ ] Logo URL không hết hạn lúc render (regenerate on read hoặc proxy)
- [ ] Re-walk browser → logo preview hiện

## Related

- Discovered in: KC-1 G2 Bước 3 walk 2026-06-08
- GAP-1071 (sister — layout shell, cùng /settings walk)


## Update 2026-06-08
BE fix shipped (contained parse-URL regen-on-read, no migration). Verified live: logo URL X-Amz-Date=20260608 fresh sau bust cache. Lưu ý: @Cacheable TTL 1h → sau đổi logo/deploy có thể stale ≤1h rồi self-heal (acceptable). Còn lại: re-walk browser confirm logo preview hiện (pending user F5 per g1-browser-walk-before-flip).

## Log (cập nhật)

- **2026-06-09:** 🟢 DONE — KC-1 G2 human browser-walk PASS (W3 — logo render được sau reload (presigned regen)). Code fix đã ship (PARTIAL trước đó), G2 verify trên browser thật :3000 hoàn tất per `pre-handoff-self-test-completeness.md` §3 + `g1-browser-walk-before-flip.md`. CSV canonical -> DONE; moved closed/. Lưu ý: upload/render cần bucket MinIO `kite-branding-assets` (tạo thủ công G2) — ensure-bucket systemic là GAP-1036 OPEN riêng.
