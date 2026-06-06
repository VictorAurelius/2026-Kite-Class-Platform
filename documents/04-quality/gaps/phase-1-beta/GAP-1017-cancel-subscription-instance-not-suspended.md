# GAP-1017: Cancel subscription không suspend/deprovision instance

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (KH-5 subscription cancel G1 walk)
**Affects:** `SubscriptionService.cancelSubscription()` (kitehub-subscription)

## Problem

KH-5 G1 walk (live): `DELETE /api/platform/subscriptions/{id}?immediate=true` → HTTP 204, subscription status CANCELLED + `expiresAt=now` + `autoRenew=false`, NHƯNG **instance vẫn ACTIVE** — không suspend, không deprovision.

Walk evidence: cancel subscription `81cf38cd…` immediate → DB `status=CANCELLED, expires_at=now`, nhưng `SELECT status FROM instances WHERE id='22003e3c…'` vẫn = `ACTIVE`. `cancelSubscription()` (line 257-283) chỉ flip subscription status, không bao giờ chạm tới `Instance`. Scheduler `suspendExpiredSubscription` chỉ xử lý status EXPIRED và `findExpiredSubscriptions` loại trừ CANCELLED → instance của sub đã cancel KHÔNG bao giờ bị suspend.

Hệ quả: Owner huỷ gói (kể cả immediate) → vẫn tiếp tục dùng instance/dịch vụ vô thời hạn miễn phí. P1 (revenue + lifecycle integrity).

## Root Cause

`cancelSubscription()` thiếu side-effect xử lý instance. `immediate=true` lẽ ra phải suspend instance ngay; `immediate=false` (cancel cuối kỳ) phải để scheduler suspend khi tới `expiresAt` — nhưng scheduler bỏ qua CANCELLED.

## Proposed Fix

1. `immediate=true`: suspend instance ngay trong `cancelSubscription()` (set `InstanceStatus.SUSPENDED`).
2. `immediate=false`: đảm bảo scheduler suspend instance khi CANCELLED-sub tới `expiresAt` (mở rộng `findExpiredSubscriptions` hoặc thêm path xử lý CANCELLED-expired).
3. Cân nhắc deprovision/data-retention theo PDPL (link KH-8 off-boarding) — có thể tách gap riêng nếu scope lớn.

## Acceptance Criteria

- [ ] Cancel immediate → instance chuyển SUSPENDED
- [ ] Cancel end-of-cycle → instance SUSPENDED khi tới expiry (scheduler)
- [ ] IT verify instance status thay đổi đúng theo immediate flag

## Related

- Discovered in: KH-5 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh5-subscription-lifecycle.md` (FM-4)
- Related: KH-8 off-boarding + data retention (PDPL) — deprovision scope
