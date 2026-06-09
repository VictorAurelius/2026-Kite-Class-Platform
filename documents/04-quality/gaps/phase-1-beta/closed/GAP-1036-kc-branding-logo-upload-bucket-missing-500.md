# GAP-1036: Logo/favicon upload → 500 NoSuchBucketException (bucket `kiteclass-files` thiếu + no ensure-bucket)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core) + DevOps (MinIO provisioning)
**Found:** 2026-06-06 (KC-10 G1 walk, Bug #3)
**Affects:** `BrandingServiceImpl` uploadLogo/uploadFavicon + S3 bucket `kiteclass-files` (application.yml:161) + MinIO seed + `GlobalExceptionHandler`

## Problem

KC-10 G1 walk: `POST /api/v1/settings/branding/logo` (và `/favicon`) với **bất kỳ ảnh hợp lệ** (PNG) → **HTTP 500** SYSTEM_INTERNAL_ERROR. Logo/favicon upload hỏng hoàn toàn out-of-box.

**Walk evidence (kiteclass-core logs):**
```
POST /api/v1/settings/branding/logo (logo=valid.png) → 500
  software.amazon.awssdk.services.s3.model.NoSuchBucketException:
  The specified bucket does not exist (S3 404)
```

Root cause: kiteclass-core config bucket `STORAGE_S3_BUCKET=kiteclass-files` (application.yml:161) nhưng MinIO **chỉ có bucket `kitehub-assets`** — `kiteclass-files` chưa được tạo. Service không có ensure-bucket logic → upload nào cũng 500.

**Sub-finding:** missing multipart part (gửi sai field name) → **500** `MissingServletRequestPartException` thay vì **400** (client error chưa handle trong `GlobalExceptionHandler`).

## Root Cause

1. **Bucket provisioning gap:** MinIO local init seed `kitehub-assets` nhưng KHÔNG seed `kiteclass-files`. Production S3 cần bucket này tạo bởi terraform.
2. **Code robustness gap:** `BrandingServiceImpl` không ensure-bucket-exists (no `headBucket`/`createBucket`); fail hard 500 thay vì tự tạo hoặc graceful error.
3. **Error mapping gap:** `MissingServletRequestPartException` → 500 thay vì 400.

## Proposed Fix

1. MinIO init script (local) + terraform (prod) seed bucket `kiteclass-files`.
2. (Optional robustness) ensure-bucket on startup trong storage config.
3. `GlobalExceptionHandler` map `MissingServletRequestPartException` + `NoSuchBucketException` → 400/503 với message rõ.

## Acceptance Criteria

- [ ] Bucket `kiteclass-files` tồn tại local MinIO + prod S3 (terraform)
- [ ] `POST /logo` (PNG valid) → 200 + object lưu MinIO + `logo_url` set trong branding row
- [ ] Missing multipart part → 400 (không 500)
- [ ] `/favicon` tương tự

## Related

- Discovered in: KC-10 G1 walk (Wave flow-kc10)
- Blocks: GAP-1037 SVG-XSS runtime verify (upload phải work trước mới test được sanitization)
- Sister provisioning gap class: env/infra parity (per `local-fix-production-parity-check.md`)

## Log
- **2026-06-09 DONE:** Wave landing-100 shipped (bucket 1036) — G1-headless verified (FE build green + curl render 200 + ?tenant= data-binding proven). Full browser-G2 + subdomain resolution gated GAP-811/1077; BE per-tenant fields GAP-1083.
