# GAP-1079: GET /subscriptions/instance/{id}/active trả 400 thay vì 404 cho no-active-subscription

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-09 (KH-3 G2 human walk — user browser test)
**Affects:** `kitehub-subscription` SubscriptionService + GlobalExceptionHandler; chặn toàn bộ KH-3 G2 flow (billing page + upgrade)

## Problem

`GET /api/platform/subscriptions/instance/{instanceId}/active` trả **400 Bad Request** khi instance chưa có subscription ACTIVE. Với tenant TRIAL (chưa nâng cấp paid) → chưa có active subscription = trạng thái **bình thường**, KHÔNG phải client error.

Root cause: `SubscriptionService.java:143` throw `new IllegalArgumentException("No active subscription found for instance: " + instanceId)` → `GlobalExceptionHandler.java:79` `@ExceptionHandler(IllegalArgumentException.class)` map mọi `IllegalArgumentException` → **400** (comment line 37: "validation errors, business logic errors").

Repro (token g2test-an-8 owner, instance TRIAL):
```
GET :9000/api/platform/subscriptions/instance/7862ab7e.../active
→ 400 {"detail":"No active subscription found for instance: 7862ab7e..."}
```

Hệ quả browser (KH-3 G2 2026-06-09): billing page `/billing` gọi `/active` → 400 lặp lại → FE không có state "no subscription / trial" → khi click "Nâng cấp" FE retry create → POST tạo PENDING (201) nhưng GET /active vẫn 400 (PENDING ≠ ACTIVE) → FE crash `TypeError: Cannot read properties of undefined (reading 'pendingPaymentId')`.

## Proposed Fix

`/active` "no active subscription" = not-found semantic → trả **404** (không phải 400), HOẶC **200 với body null/empty** để FE render "trial, chọn gói nâng cấp". Options:
- (A) Đổi `SubscriptionService.getActiveSubscription` throw dedicated `SubscriptionNotFoundException` → `@ExceptionHandler` map 404 (KHÔNG đổi global IllegalArgumentException→400 mapping — tránh ảnh hưởng endpoint khác).
- (B) `/active` controller return `Optional` → `ResponseEntity.notFound()` (404) khi empty.
- FE: hook gọi `/active` PHẢI treat 404 = "no active subscription → tier FREE" (billing/upgrade page §239 đã có logic "treat as FREE" — cần trigger trên 404, không crash trên 400).

## Acceptance Criteria

- [ ] `GET /active` no-active-sub → 404 (hoặc 200-null), KHÔNG 400
- [ ] FE billing page render trạng thái trial + nút nâng cấp khi no active sub (không lỗi console)
- [ ] KH-3 G2 re-walk: login → /billing → /billing/upgrade → tạo subscription → không crash pendingPaymentId
- [ ] Sweep sister: các endpoint khác throw IllegalArgumentException cho "not found" condition → audit semantic 400-vs-404 (per cross-flow-bug-class-sweep)

## Related

- Discovered in: KH-3 G2 human walk 2026-06-09 (campaign §4.5 feedback loop)
- Sister: GAP-1068 class (curl PASS / browser G2 lòi — g1-browser-walk-before-flip §6); GAP-1080 (POST no idempotency, same walk)
- Cross-flow sweep: `cross-flow-bug-class-sweep` — "not-found→IllegalArgumentException→400" có thể lặp ở KC services
