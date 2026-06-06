# GAP-1033: Admin manual email `/trigger` trả 409 khi resend (idempotency dedup chặn resend hợp lệ)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend (kitehub-subscription)
**Found:** 2026-06-06 (KH-10 G1 walk)
**Affects:** `AdminEmailController.triggerEmail()` + idempotency guard

## Problem

KH-10 G1 walk: admin `POST /api/platform/admin/emails/trigger {instanceId, emailType:"trial-midpoint"}` → **409 Conflict** `RESOURCE_ALREADY_EXISTS` vì email type đó đã gửi cho instance này (dedup). Với admin action "trigger/resend" thủ công, 409 chặn use-case resend hợp lệ (vd email kẹt, user không nhận) và thông báo lỗi mơ hồ ("Tài nguyên đã tồn tại").

Edge — không phải walk-blocker; trigger với type mới hoặc instance khác chạy bình thường. Unknown type → 400 "Unknown email type" (đúng, FM-9 safe).

## Proposed Fix

1. Admin manual trigger nên **bypass content-dedup** (hoặc dùng explicit `Idempotency-Key` do admin cấp) — admin chủ ý resend.
2. Nếu giữ dedup, trả response rõ ràng hơn 409 chung: "Email loại này đã gửi cho instance — dùng force=true để gửi lại" + flag `force`.

## Acceptance Criteria

- [ ] Admin trigger resend same type/instance → 200 (force) HOẶC 409 với message actionable + cách force
- [ ] Không hồi quy idempotency cho automated send path (welcome/trial tự động vẫn dedup)

## Related

- Discovered in: KH-10 G1 walk (Wave flow-kh10)
- Idempotency guard: HTTP idempotency GAP-840 (EmailController content-hash key)
