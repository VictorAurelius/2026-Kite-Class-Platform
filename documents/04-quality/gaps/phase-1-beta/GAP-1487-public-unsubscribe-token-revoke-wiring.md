# GAP-1487: Public unsubscribe page — one-click token-revoke BE wiring

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed (Backend + Frontend)
**Found:** 2026-06-19 (PR #2508 BE→FE URL contract fix — GAP-1414 follow-up)
**Affects:** `kitehub/kitehub-frontend/src/app/(public)/unsubscribe/page.tsx` + `kitehub-subscription` email-preference BE

## Problem

GAP-1414 (PR #2508) chuyển email URLs sang config base `appBaseUrl`, để lộ `unsubscribeUrl = appBaseUrl + "/unsubscribe"` (footer email welcome / tenant-ready / beta-invite) trỏ tới FE route 404 — detector `check-be-fe-url-contract.sh` HARD-STOP.

Fix tạm (PR #2508): thêm trang public `(public)/unsubscribe/page.tsx` **stub** — xác nhận + hướng dẫn gửi yêu cầu thủ công tới `support@kitehub.me`. Link resolve thật (hết 404), đúng chuẩn email footer non-user.

**Còn thiếu:** cơ chế hủy một chạm bằng token. Hiện user phải email thủ công → DPO/support xử lý tay trong 48h.

## Proposed Fix

- BE: endpoint `POST /api/platform/emails/unsubscribe` nhận signed token (HMAC email + list) → set email-preference opt-out, idempotent.
- BE: gắn token vào `unsubscribeUrl` khi build email (`appBaseUrl + "/unsubscribe?token=..."`).
- FE: trang `(public)/unsubscribe` đọc `?token`, gọi endpoint, hiển thị kết quả (đã có scaffold đọc `searchParams.token`).
- Giữ phân biệt email bắt buộc (auth/security/billing/DSAR) vs tiếp thị (opt-out được).

## Acceptance Criteria

- [ ] Token-based one-click unsubscribe hoạt động end-to-end (email link → opt-out persisted)
- [ ] Email bắt buộc vẫn gửi sau khi opt-out (chỉ chặn tiếp thị/không-bắt-buộc)
- [ ] Idempotent (click 2 lần không lỗi)

## Related

- Parent: GAP-1414 (email URL config base) + PR #2508
- Sibling: GAP-1488 (DSAR page BE wiring — cùng class email-link-stub)
- Preferences UI có sẵn: `(customer)/settings/notifications`
