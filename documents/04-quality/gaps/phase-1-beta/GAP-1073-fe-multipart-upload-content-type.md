# GAP-1073: FE file upload (logo/favicon) fail — apiClient default Content-Type json phá multipart

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-08 (KC-1 G2 Bước 4+ — user upload avatar/logo "không được" trên browser)
**Affects:** `kiteclass-frontend/src/lib/api-client.ts` — mọi multipart upload (logo, favicon, CSV bulk import)

## Problem

Upload logo từ browser fail; **curl `-F` lại success** (BE + MinIO + gateway OK — verified live HTTP success + presigned fresh). → bug FE-only (curl≠browser, đúng lỗ hổng `g1-browser-walk-before-flip` cảnh báo).

Root cause: `apiClient` axios instance set default `headers: { 'Content-Type': 'application/json' }`. `brandingApi.uploadLogo` gửi `FormData` qua `apiClient.post(url, formData)` — instance-default json content-type đè lên → axios KHÔNG auto-set `multipart/form-data; boundary=...` → BE `@RequestPart("logo")` không parse được body. Comment trong `branding.ts:34` ghi "do NOT hardcode Content-Type, browser sets boundary" — nhưng instance default đã set json, defeat ý định đó.

## Proposed Fix

Request interceptor: nếu `config.data instanceof FormData` → `delete config.headers['Content-Type']` → browser/axios tự set multipart + boundary. Robust (cover mọi upload, không chỉ logo). **APPLIED** PR này.

## Acceptance Criteria

- [x] Interceptor drop Content-Type cho FormData (api-client.ts)
- [ ] Re-walk browser: upload logo từ Settings → success + logo preview cập nhật (pending user F5 per g1-browser-walk)
- [ ] Sweep: kitehub-frontend api-client có cùng bug không (cross-flow)?

## Related

- Discovered in: KC-1 G2 Bước 4 walk 2026-06-08
- GAP-1072 (logo render — sister; giờ upload xong logo mới sẽ render fresh per GAP-1072 regen)
- `g1-browser-walk-before-flip` (curl≠browser — incident thứ N reinforces rule)
- `cross-flow-bug-class-sweep` (sweep kitehub-frontend api-client)
