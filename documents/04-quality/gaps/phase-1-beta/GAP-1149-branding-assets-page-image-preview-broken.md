# GAP-1149: /branding/assets không preview được ảnh

**Status:** 🟡 PARTIAL — state-check + UX degrade shipped PR #2289; runtime root-cause pending G2
**Priority:** 🟡 P2
**Domain:** Frontend (+ presigned URL / CSP)
**Found:** 2026-06-10 (G2 browser-walk — PR #2289)
**Affects:** `kitehub-frontend` trang `/branding/assets`

## Problem

G2 feedback #7: ở `/branding/assets` ảnh không preview (lặp nhiều log CSP `img-src 'self' data: https: blob:` report-only cho URL `http://localhost:9100/...`).

Phân tích:
- CSP là **report-only** → không phải nguyên nhân chặn (chỉ log). Ảnh đáng lẽ vẫn load.
- Nghi: presigned URL **hết hạn** (cùng lớp GAP-1072/GAP-804 — presigned X-Amz-Expires), HOẶC trang `/branding/assets` build URL ảnh sai/không presign-on-read.
- Local presigned host `http://localhost:9100` (http) vi phạm `img-src` (chỉ `https:`); report-only nên load — nhưng prod CSP enforce sẽ chặn nếu host không `https`. Cần thêm MinIO/S3 host vào `img-src` hoặc đảm bảo presigned là `https` ở prod.

## Proposed Fix

1. State-check trang `/branding/assets`: URL ảnh có presign-on-read (regenerate presigned khi load) như GAP-1072 fix không?
2. Verify presigned chưa hết hạn + host reachable từ browser.
3. Cân nhắc thêm asset host vào CSP `img-src` (cross-ref CSP report-only → enforce roadmap).

## Acceptance Criteria

- [ ] `/branding/assets` hiển thị thumbnail ảnh (browser verify).
- [ ] Presigned URL không hết hạn lúc render (presign-on-read).

## Fix (PR #2289, 2026-06-10) — state-check ⇒ gap diagnosis sai một phần

State-check (per `design-first-investigation-order` + `audit-to-gap-pipeline` §2.8) cho thấy **nghi vấn chính của gap sai**:
- `AssetStorageController.getAssets` **ĐÃ presign-on-read** (`presignAssets`, GAP-1112 #1) — re-presign mọi URL chứa `/instances/` mỗi lần load. Không phải "build URL sai/không presign".
- Presigned TTL = **1 giờ** (`S3StorageService` `Duration.ofHours(1)`) — không phải hết hạn lúc render.
- Dev CSP (`next.config.js:48`) **đã allow** `http://localhost:9100`; CSP là `Content-Security-Policy-Report-Only` → log nhưng **không chặn** ảnh. Console log CSP user thấy là red-herring (hoặc do chạy prod-build local → `isDev=false`).
- MinIO host `localhost:9100` reachable (compose `9100:9000`).

→ Root local thật **nghi** là mock-asset 404 (mock provisioning tạo URL `/instances/...` không có object thật trong MinIO → presign 1 URL trỏ object không tồn tại → 404). Cần G2 runtime để xác nhận (network tab status của ảnh).

**Fix UX (chắc chắn, an toàn):** `AssetsGrid` thêm `<img onError>` → fallback placeholder "Không tải được ảnh xem trước" (icon `ImageOff`) thay vì broken-image glyph; Download/"Xem" vẫn expose URL thô. Gallery đọc được bất kể root cause.

FE build PASS (Compiled successfully 90/90). **Pending:** G2 runtime xác nhận root cause (mock-asset vs upload thật) — nếu mock-asset thì là Phase-1 mock limitation, asset upload thật vẫn preview đúng.

## Related

- Discovered in: PR #2289 (wave-wizard-step7 G2 walk 2026-06-10)
- GAP-1072 (settings logo presigned expiry, DONE) · GAP-804 (logo presigned browser-reach, DONE) — same class
