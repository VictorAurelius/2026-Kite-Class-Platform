# GAP-1085: Subscription email gửi 2 lần — outbox fast-path + dispatcher cùng publish (Bug E)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-09 (Wave landing-tenant-1 — KH-3 G2 SePay walk, bug E)
**Affects:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/migration/SubscriptionEventEmitter.java` (`emit`) + downstream `SubscriptionOutboxDispatcher` + `EmailEventListener` (kitehub-email)

## Problem

KH-3 G2 walk (nâng cấp PREMIUM qua SePay) phát hiện email kích hoạt gửi 2 lần tới người dùng.

`SubscriptionEventEmitter.emit()` thực hiện 2 việc trong cùng transaction:
1. Lưu outbox row (`dispatched_at = NULL`).
2. Fast-path `rabbitTemplate.send(...)` ngay (best-effort, low-latency).

Nhưng fast-path thành công **không stamp `dispatched_at`** → row vẫn `NULL` → `SubscriptionOutboxDispatcher.dispatch()` (poll `findByDispatchedAtIsNullOrderByCreatedAtAsc` mỗi ~10s) tìm thấy row và **publish lại** → `EmailEventListener` nhận 2 message cho cùng 1 email.

Consumer có `EmailIdempotencyGuard` (GAP-580, Redis SETNX / Caffeine fallback, TTL 60 phút) nhưng đó chỉ là *safety net*, không phải barrier chính — khi nó miss (Redis chưa wire / image cũ chưa có guard / TTL hết / key lệch) thì người dùng nhận trùng. Walk thực tế chứng minh barrier-chính-thiếu này.

Đây là cấu trúc double-publish áp dụng cho **mọi** event đi qua `SubscriptionEventEmitter` (email + cross-service), không riêng subscription-created.

## Root Cause

Pattern fast-path + outbox (per `design-patterns.md` §3.5.1 Exception A) thiếu bước "mark dispatched khi fast-path delivered". Dispatcher được thiết kế là reliability-net cho trường hợp fast-path *fail*, nhưng vì fast-path *success* không đánh dấu row → dispatcher coi mọi row là chưa-gửi → luôn republish.

## Fix (shipped session 2026-06-09)

`SubscriptionEventEmitter.emit()` restructure: thử fast-path trước → set `event.setDispatchedAt(now)` **chỉ khi** `rabbitTemplate.send` không ném exception → save **một lần** (carry final state, tránh bẫy Spring Data merge với assigned-ID + giữ `verify(save)` exactly-once của test hiện có).

- Happy path: fast-path gửi + stamp dispatched → dispatcher **skip** → consumer nhận đúng 1 lần.
- Fast-path fail (RMQ down): `dispatched_at` vẫn NULL → dispatcher retry khi broker hồi phục → đúng 1 lần.
- `EmailIdempotencyGuard` giữ vai trò net cho at-least-once redelivery thật (crash-before-ack, DLQ retry).

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** outbox emitter fast-path publish without stamping `dispatched_at` → dispatcher republishes.

**Grep:** `grep -rln "rabbitTemplate.send|convertAndSend" ... | grep -iE "emitter|publisher"`

| # | Site | Verdict | Lý do |
|---|---|---|---|
| 1 | `SubscriptionEventEmitter` (kitehub-subscription) | **FIX** | Bug gốc — đã sửa session này |
| 2 | `kitehub-branding/.../outbox/BrandingEventEmitter` | **DEFER → GAP-1088** | Cùng module-pattern outbox+dispatcher; cần verify có stamp dispatched-on-fast-path không (chưa walk branding flow) |
| 3 | `kiteclass-core/.../branding/events/BrandingEventPublisher` | **DEFER → GAP-1088** | Generic `OutboxEventWriter` path — verify riêng |

## Acceptance Criteria

- [x] `emit()` stamp `dispatched_at` khi fast-path thành công; giữ NULL khi fail
- [x] Unit test: `emit_fast_path_success_marks_dispatched...` + `emit_fast_path_failure_leaves_dispatched_null...` PASS (SubscriptionEventEmitterTest 11/11)
- [ ] **Runtime re-walk (pending — gộp G2 re-walk):** rebuild kitehub-subscription + email, trigger subscription activation, MailHog hiển thị đúng **1** email (per `pre-handoff-self-test-completeness.md` §3)
- [ ] Sweep sister emitters (GAP-1088) verify branding flows không double-publish

## Related

- Discovered in: Wave landing-tenant-1 KH-3 G2 SePay walk (handoff `2026-06-09-wave-landing-tenant-1-kh3-g2-pass.md` Bug E)
- Sister/safety-net: GAP-580 (consumer EmailIdempotencyGuard), GAP-922 (publishToQueue double-convertAndSend, đã đóng — class khác), GAP-925/937 (wire-format)
- Sweep follow-up: GAP-1088
- Sister content bug same walk: GAP-1086
