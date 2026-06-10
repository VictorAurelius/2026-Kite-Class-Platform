# GAP-1149: /branding/assets không preview được ảnh

**Status:** 🔵 OPEN
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

## Related

- Discovered in: PR #2289 (wave-wizard-step7 G2 walk 2026-06-10)
- GAP-1072 (settings logo presigned expiry, DONE) · GAP-804 (logo presigned browser-reach, DONE) — same class
