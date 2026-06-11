# GAP-1198: kiteclass-frontend CSP thiếu dev-override cho MinIO logo host

**Status:** 🟢 DONE
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-11 (demo-seed-1 G2 walk — CSP console violation báo logo MinIO)
**Affects:** `kiteclass/kiteclass-frontend/next.config.js` `headers()` CSP `img-src` directive

## Problem

KiteClass tenant landing tải logo branding từ MinIO dev stack qua `http://localhost:9100/kite-branding-assets/...` (presigned URL). CSP `img-src` directive của `kiteclass-frontend` chỉ allow `'self' data: https: blob: https://cdn.kiteclass.com` — KHÔNG có `http://localhost:9100` dev host. Trong G2 walk local Docker, browser console log Content-Security-Policy violation cho logo:

```
Loading the image 'http://localhost:9100/kite-branding-assets/static/.../logo/sky-logo.png?X-Amz-...'
violates the following Content Security Policy directive: "img-src 'self' data: https: blob: https://cdn.kiteclass.com".
The policy is report-only, so the violation has been logged but no further action has been taken.
```

CSP hiện ở mode `Content-Security-Policy-Report-Only` (Wave 86 Bucket E) → violation chỉ log, KHÔNG block ảnh. Nhưng khi CSP flip sang enforce (Wave 86 Bucket E flip plan), logo dev sẽ bị block → vỡ logo preview local.

Đây là sister-flow miss của GAP-1112 (kitehub-frontend đã có `devImg` dev-override cho cùng MinIO `:9100` host); kiteclass-frontend chưa apply cùng pattern (per `cross-flow-bug-class-sweep.md` §1).

## Proposed Fix

Mirror kitehub-frontend `devImg` pattern: thêm `const isDev = process.env.NODE_ENV !== 'production'; const devImg = isDev ? ' http://localhost:9100' : '';` trong `headers()`, đổi `img-src` directive sang template-literal append `${devImg}`. Production (`NODE_ENV=production`) giữ https-only — `devImg` empty, no localhost host leak.

## Acceptance Criteria

- [x] `img-src` directive append `http://localhost:9100` chỉ khi `NODE_ENV !== 'production'`
- [x] Production CSP không chứa `http://localhost:9100` (devImg empty)
- [x] Mirror đúng kitehub-frontend GAP-1112 devImg pattern (verbatim parity)

## Walk evidence

CSP Report-Only → cosmetic dev-noise only, không functional block. Fix = verbatim mirror của GAP-1112 kitehub pattern đã ship + verified. Verify final = local KC FE rebuild → console clean (no img-src violation cho logo `:9100`). Production unaffected (`devImg` empty khi `NODE_ENV=production`).

## Related

- Discovered in: demo-seed-1 G2 walk 2026-06-11 (CSP console paste)
- Fixed + closed via PR #2321 (2026-06-11) — `kiteclass-frontend/next.config.js` devImg + gap archived to `closed/`
- Sister gap: GAP-1112 (kitehub-frontend devImg dev-override — same MinIO `:9100` class)
- Rule: `cross-flow-bug-class-sweep.md` §1 (fix once → sweep sister flow); `small-gap-inline-fix.md` (P3 cosmetic + verify-by-mirror → fix inline)
