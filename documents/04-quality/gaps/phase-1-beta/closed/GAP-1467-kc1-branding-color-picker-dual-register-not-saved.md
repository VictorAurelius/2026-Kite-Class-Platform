# GAP-1467: KC-1 branding — color picker dual-register → màu pick không được lưu

**Status:** 🟢 DONE
**Priority:** 🟠 P2
**Domain:** Frontend
**Found:** 2026-06-17 (KC-1 G2 walk)
**Affects:** kiteclass-frontend `src/components/settings/branding-settings.tsx`

## Problem

KC-1 G2 walk live: với mỗi field màu thương hiệu (primaryColor, secondaryColor, accentColor), component render HAI `<Input>` cùng spread `{...register('X')}` — 1 ô `type="color"` (swatch picker) + 1 ô text hex. Dual-register react-hook-form phá binding: pick màu mới từ swatch KHÔNG cập nhật giá trị submit (ref ô text bị stale), nên bấm "Lưu thay đổi" → PUT bắn ra nhưng persist màu CŨ. Hai ô cũng không sync hình ảnh với nhau.

Đã confirm live: branding giữ nguyên `#3B82F6` sau khi pick màu mới + Lưu.

## Proposed Fix

Chuyển 3 cặp input màu sang controlled, lấy giá trị form làm single source of truth: thêm `watch` + `setValue` vào `useForm`; mỗi input dùng `value={watch('X')}` + `onChange → setValue('X', e.target.value, { shouldValidate: true, shouldDirty: true })`. Bỏ `register` cho 3 field màu (controlled thay thế); 3 field vẫn nằm trong submit data + zod validation. Các input khác (logo, favicon, banner, displayName, tagline, contact, social) giữ nguyên `register`.

## Acceptance Criteria

- [x] Pick màu từ swatch → ô text hex sync theo (và ngược lại gõ hex → swatch sync)
- [x] Pick/gõ màu mới → "Lưu thay đổi" → `GET /api/v1/settings/branding` trả về màu MỚI (không phải màu cũ)
- [x] zod regex validation màu vẫn fire khi nhập hex không hợp lệ

## Verification

**G2★ human browser walk — FULL PASS 2026-06-17** (tenant `g2walk`, production-accurate nip.io access):

- Server-side confirm: sau khi pick màu mới + "Lưu thay đổi", `GET /api/v1/settings/branding` trả về màu **MỚI** persist — `primary #3bf79f` / `secondary #5ff7d8` / `accent #b71053` (trước fix giữ nguyên `#3B82F6` mặc định → dual-register stale ref). ✅
- PUT submit lúc 08:13:51 → HTTP 200, **không** validation error (zod regex màu pass). ✅
- Swatch ↔ ô text hex sync hai chiều (controlled `watch` + `setValue` single source of truth). ✅
- Docker rebuild production `next build` PASS + container `kiteclass-frontend` recreate **healthy**. ✅

Fix commit `5daea3b4`: 3 cặp input màu (primary/secondary/accent) chuyển từ dual-`register` → controlled (`value={watch('X')}` + `onChange → setValue`). Tất cả AC ✅.

## Related
- Discovered in: 2026-06-17 KC-1 G2 walk (phiên fix branding color picker)
- Flow: KC-1 (tenant settings → branding) — `flow-verification-campaign.md`
