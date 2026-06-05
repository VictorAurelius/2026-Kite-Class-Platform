# GAP-978: `build-all.sh` (và `up.sh --rebuild`) bỏ sót `kiteclass-core` + `kiteclass-frontend` → stale-image walk

**Status:** 🔵 OPEN
**Priority:** P1
**Domain:** DevOps
**Found:** 2026-06-05 (Wave flow-kc1 KC-1 G1 walk — coordinator phát hiện khi rebuild để unify code)
**Affects:** Mọi local walk/test đụng KiteClass (KC-1..KC-12) sau khi có migration/code mới — `--rebuild` chạy nhưng image KiteClass vẫn cũ → DB Flyway kẹt version cũ → walk chạy trên code lỗi thời mà KHÔNG báo lỗi (silent stale-image)

## Problem

`kitehub/scripts/build-all.sh` step "[2/6] Building backend services" chỉ build:
`kitehub-subscription`, `kitehub-branding`, `kitehub-email`, `kitehub-admin`, `kite-gateway` + `kitehub-frontend` (step 3).

**KHÔNG có `kiteclass-core` lẫn `kiteclass-frontend` trong danh sách.** `up.sh --rebuild` gọi `build-all.sh` nên cũng bỏ sót.

Hệ quả empirical (KC-1 walk 2026-06-05):
- Image `kiteclass-core:latest` build 2026-05-29 (7 ngày cũ), repo HEAD đã có migration tới V86.
- Image cũ chỉ chứa migration tới V77 → live `kiteclass_shared` kẹt Flyway V77 → bảng `user_preferences` (tạo ở V79) KHÔNG tồn tại → endpoint preferences sẽ 500.
- Chạy `up.sh --profile full --rebuild --force-recreate` → image `kiteclass-core` **vẫn date 2026-05-29** (không build lại) → người walk tưởng đã unify code nhưng thực tế chưa.
- Phải rebuild thủ công `docker-compose build kiteclass-core kiteclass-frontend` mới fresh.

Đây là bug class nguy hiểm: walk "production-equivalent" (G3 parity per `local-fix-production-parity-check.md`) bị phá ngầm vì stack KiteClass chạy code cũ hơn repo — có thể cho **false G1/G3 PASS** trên code đã lỗi thời.

## Root Cause

`build-all.sh` service list hardcoded chỉ liệt kê kitehub-* services + gateway. KiteClass services (`kiteclass-core`, `kiteclass-frontend`) bị thiếu — có thể từ thời KiteClass tách module sau hoặc copy-paste sót. Không có guard nào cảnh báo "rebuild all nhưng N service không nằm trong danh sách".

## Proposed Fix

1. Thêm `kiteclass-core` vào step 2 backend list + `kiteclass-frontend` vào step 3 (hoặc step frontend riêng) trong `build-all.sh`.
2. (Tùy chọn) Derive service list động từ `docker-compose.kitehub.yml` services có `build:` context thay vì hardcode → tránh sót khi thêm service mới.
3. (Tùy chọn) `up.sh --rebuild` post-build assert: mọi image có `build:` context phải có `Created` ≤ N phút → fail-loud nếu service bị skip.

## Acceptance Criteria

- [ ] `bash kitehub/scripts/build-all.sh` rebuild cả `kiteclass-core` + `kiteclass-frontend` (verify image `Created` date cập nhật)
- [ ] `up.sh --rebuild` → mọi `build:`-context service có image fresh
- [ ] (Nếu làm Option 3) skip service → fail-loud, không silent

## Related

- Discovered in: Wave flow-kc1 KC-1 G1 walk session 2026-06-05
- `local-fix-production-parity-check.md` (G3 parity bị stale-image phá ngầm)
- `feature-ship-runtime-walk-mandate.md` §3.1 (walk phải production-equivalent stack)
