# GAP-1017: Cancel subscription không suspend/deprovision instance

**Status:** 🟡 PARTIAL
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

## Log

- **2026-06-07** (Wave g2-blockers-1 Bucket C, inline): `cancelSubscription()` giờ propagate xuống Instance. (1) `immediate=true` → suspend Instance ngay (set `InstanceStatus.SUSPENDED`). (2) `immediate=false` → thêm `SubscriptionRepository.findCancelledExpiredSubscriptions()` + `SubscriptionRenewalService.suspendCancelledExpired()` + loop trong `SubscriptionExpirationChecker.processExpiredSubscriptions()` → scheduler suspend instance khi CANCELLED-sub tới `expiresAt` (trước đây `findExpiredSubscriptions` loại trừ CANCELLED). AC#1 + AC#2 met. **Status 🟡 PARTIAL ~85%** — code fix + compile PASS; **residual:** (a) IT verify (immediate→SUSPENDED + end-of-cycle scheduler→SUSPENDED); (b) deprovision/PDPL data-retention = scope KH-8 (GAP-1026), không trong gap này; (c) G3 gateway :9000 re-walk pending coordinator trước DONE flip.
