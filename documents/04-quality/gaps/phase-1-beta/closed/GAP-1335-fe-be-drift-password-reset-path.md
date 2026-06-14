# GAP-1335: FE→BE drift — kiteclass forgot/reset-password call-site sai path BE

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-14 (API-contract full audit, AUDIT-2026-06-14-api-contract-full)
**Affects:** `kiteclass-frontend/src/lib/api/auth.ts` ↔ kitehub-subscription PasswordResetController

## Problem

`check-fe-be-api-contract.sh` flag (drift thật): kiteclass FE gọi:
- `POST /api/auth/forgot-password` (`auth.ts:117`)
- `POST /api/auth/reset-password` (`auth.ts:124`)

Nhưng BE `PasswordResetController` (`PasswordResetController.java:44,52,62`) expose:
- `POST /api/auth/password-reset-request`
- `POST /api/auth/password-reset-confirm`

Path không khớp → 404. Hoặc FE auth.ts là legacy/chưa-wire (kiteclass tenant-auth thực dùng `/api/v1/tenant-auth/**`), hoặc password-reset flow của kiteclass bị gãy.

## Root Cause

Tên endpoint FE (`forgot-password`/`reset-password`) lệch tên BE (`password-reset-request`/`password-reset-confirm`); không có contract chung. Cũng cần xác định kiteclass password-reset thuộc kitehub PasswordResetController hay có flow riêng.

## Proposed Fix

(1) Xác định owner của kiteclass password-reset flow. (2) Đồng bộ path FE↔BE (sửa 1 trong 2 phía). (3) Document trong `tenant-auth/api-contract.md` hoặc `kitehub/auth/api-contract.md`. (4) Nếu auth.ts là dead code → xóa.

## Acceptance Criteria

- [ ] FE forgot/reset-password resolve đúng BE mapping (không 404) HOẶC dead-code removed
- [ ] Path documented trong api-contract.md tương ứng
- [ ] `check-fe-be-api-contract.sh` không còn flag 2 path này

## Resolution

🟢 DONE (2026-06-15, branch `fix/audit-fixC-apidocs-2026-06-14`). Xác định: kiteclass-core KHÔNG có endpoint password-reset native (chỉ `/api/v1/tenant-auth/login`); FE `authApi.forgotPassword/resetPassword` ĐANG được dùng (`useAuth` + `reset-password-form.tsx`) → KHÔNG dead code. Owner/staff credential ở KH `users` → reset qua kitehub-subscription `PasswordResetController` (`/api/auth/**` qua gateway). Body shape đã khớp sẵn (`{ email }` / `{ token, newPassword }`).

Sửa FE `kiteclass-frontend/src/lib/api/auth.ts`:
- `/api/auth/forgot-password` → `/api/auth/password-reset-request`
- `/api/auth/reset-password` → `/api/auth/password-reset-confirm`
- Cập nhật `__tests__/auth.test.ts` assertions theo path mới.
- Document cross-ref tại `kiteclass/tenant-auth/api-contract.md`.

Verify: `check-fe-be-api-contract.sh` hết flag 2 path; `pnpm --filter kiteclass-frontend build` PASS. AC: cả 3 ✅.

## Related

- Discovered in: `documents/04-quality/audits/api-contract/2026-06-14-api-contract-full-audit.md` B4
- Detector: `scripts/check-fe-be-api-contract.sh`
- Related: GAP-590 (password reset link expiry policy spec)
