# GAP-1453: KH-10 G2 recipe refresh — Bước 6 trỏ /help 404-by-design + claim admin-2FA sai cho seed admin@kitehub.com

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Docs
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-10)
**Affects:** KH-10 — `documents/05-guides/operations/2026-06-06-g2-recipe-kh10-notification-email-feedback-support.md` §2 Setup + Bước 6

## Problem
Discovered Phase-2 browser walk KH-10. Bước 6 liệt kê `/help` (404 by design — phải dùng route persona); §2 claim admin-2FA setup không chính xác cho seed `admin@kitehub.com`.

## Proposed Fix
- Bước 6: thay `/help` bằng route persona `/help/p2-owner` (owner).
- §2: note `admin@kitehub.com` / `Admin@KiteHub123` đăng nhập trực tiếp không cần 2FA (bước mint-HS512 chỉ cần cho account `admin.test@test.vn` gated 2FA).

## Acceptance Criteria
- [ ] Bước 6 trỏ route persona tồn tại (không /help 404)
- [ ] §2 mô tả đúng account nào cần 2FA / mint-HS512

## Related
- Discovered in: Phase-2 browser walk (flow KH-10), 2026-06-16
