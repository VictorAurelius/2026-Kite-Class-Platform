# ADR-044: Owner Notification Channel Abstraction

**Status:** ACCEPTED
**Date:** 2026-06-13
**Deciders:** @nguyenvankiet (solo-dev — acting architect)
**Reviewers:** @nguyenvankiet (solo-dev — transaction-isolation angle)
**Related Gap(s):** GAP-1265 (notification channel abstraction + in-app fallback), GAP-1257 (pending-payment-status + payment-confirmed notify), GAP-1266 (non-VAT receipt bundled), GAP-1263 (win-back/reactivate), GAP-063 (full Zalo Notification Service — Phase 2)

## Context

KiteHub owner notifications (payment-confirmed, win-back, retention warning) trước đây chỉ qua **email** (`EmailServiceClient` → outbox). Hai vấn đề:

1. **Email là kênh duy nhất** — owner không có email hoặc email vào spam → mất hoàn toàn thông báo quan trọng (payment confirm, suspend warning). Văn hóa VN ưa kênh persistent + Zalo, nhưng email-only không có fallback.

2. **Side-effect notify có thể poison payment txn.** Nếu notify chạy trong cùng transaction với `confirmPayment` và notify fail (SMTP down, DB constraint) → Spring set rollback-only → payment capture bị rollback dù tiền đã vào (cùng class lỗi với incident admin-login-500 2026-05-16, `audit-service-isolation.md`).

3. **Thêm kênh mới (Zalo/SMS) = sửa rải rác** mọi notify call-site nếu hard-code email.

**Ràng buộc:** Full Zalo OA Notification Service là scope GAP-063 (Phase 2 — cần Zalo OA registration + ZNS template approval). Phase 1 cần fallback persistent NGAY mà không build Zalo.

## Decision

**Strategy-port `NotificationChannel` + `OwnerNotificationDispatcher` fan-out, mỗi channel chạy `REQUIRES_NEW`:**

### 1. `NotificationChannel` port (Strategy seam)
Interface một method `boolean deliver(OwnerNotification)` + `NotificationChannelType type()`. Contract: **best-effort — MUST NOT throw**; delivery fail trả `false` (dispatcher log). Per `design-patterns.md` §2 "Multiple implementations".

Phase 1 wire 2 adapter:
- `EmailNotificationChannel` (EMAIL, primary — qua `EmailServiceClient` outbox path)
- `InAppNotificationChannel` (IN_APP, durable persistent-banner fallback — bảng `in_app_notifications`)

`NotificationChannelType` enum: `EMAIL, IN_APP, SMS, ZALO, PUSH`. SMS (GAP-063b) / ZALO (GAP-063) / PUSH là **documented stub** — chưa wire adapter Phase 1.

### 2. `OwnerNotificationDispatcher` fan-out + auto-discovery
`@Component` inject `List<NotificationChannel>` → index theo `NotificationChannelType` (`EnumMap`). `notifyOwner(n)` fan: IN_APP trước (reliability net — persist banner), rồi EMAIL (best-effort), rồi ZALO (no-op nếu chưa wire). Thêm Zalo/SMS Phase 2 = drop-in: implement port + register bean → dispatcher tự pick up (không sửa call-site). Dispatcher **never throws** — mỗi channel bọc try/catch.

### 3. `REQUIRES_NEW` isolation (never poison caller txn)
Cả `EmailNotificationChannel.deliver` + `InAppNotificationChannel.deliver` annotate `@Transactional(propagation = REQUIRES_NEW)` → mỗi delivery chạy txn riêng → fail KHÔNG set rollback-only trên payment/business txn của caller. Theo nguyên tắc `audit-service-isolation.md` (notification = best-effort side-effect, caller success KHÔNG phụ thuộc).

### 4. Phase-1 convenience builders
- `sendPaymentConfirmed(instance, receipt)` (GAP-1257/1266) — mọi confirm path (admin confirm / SePay webhook / legacy gateway webhook) dispatch `payment-confirmed` (email template + in-app banner) mang non-VAT receipt summary. Best-effort — notify fail KHÔNG block payment capture.
- `sendWinBack(instance, voluntary)` (GAP-1263) — `winback-reactivate` email + banner, CTA → reactivate endpoint. Seam cho suspend/cancel scheduler ([ADR-043](ADR-043-manual-vietqr-dunning-involuntary-churn-lifecycle.md)) invoke.

```mermaid
flowchart TD
    Caller["Confirm payment / suspend scheduler<br/>(payment txn — must not be poisoned)"]
    Caller -->|notifyOwner(n) — never throws| Disp[OwnerNotificationDispatcher]
    Disp -->|1. reliability net| InApp[InAppNotificationChannel<br/>REQUIRES_NEW — persist in_app_notifications row]
    Disp -->|2. best-effort| Email[EmailNotificationChannel<br/>REQUIRES_NEW — EmailServiceClient outbox]
    Disp -.->|3. no-op Phase 1| Zalo[ZALO / SMS / PUSH<br/>documented stub — GAP-063/063b]
    InApp -->|return true/false| Disp
    Email -->|return true/false| Disp
    InApp --> Banner[FE persistent banner<br/>GET /notifications/in-app]
    Email --> Inbox[Owner inbox / MailHog]
```

## Consequences

### Positive
- **Fallback persistent** — owner luôn thấy banner in-app dù email fail/spam (đóng email-only single-point).
- **Caller txn an toàn** — `REQUIRES_NEW` + never-throw → notify fail không bao giờ rollback payment capture (đóng class admin-login-500).
- **Drop-in extensibility** — Zalo/SMS Phase 2 = implement port + register; dispatcher auto-discover, zero call-site change.
- **Đúng design-pattern** — Strategy (multiple channel impl) + best-effort isolation, không God-service.

### Negative
- **In-app banner chỉ persistent, không push** — owner phải mở app mới thấy (không real-time push như Zalo ZNS). Chấp nhận Phase 1; Zalo deferred GAP-063.
- **2 txn per notify** (IN_APP + EMAIL REQUIRES_NEW) — nhẹ overhead vs 1 txn. Chấp nhận (notify hiếm, không trên hot path).
- **Zalo là stub** — văn hóa VN ưa Zalo nhưng Phase 1 chưa có; documented deferral.

### Neutral
- `in_app_notifications` bảng mới (V74) + endpoint `GET /notifications/in-app/instance/{id}` + `PATCH .../{notificationId}/read` (tenant-bound).
- Email template (`payment-confirmed`, `winback-reactivate`) sở hữu bởi BE-4 / kitehub-email.

## Alternatives Considered

### Alternative A: Build full Zalo OA Notification Service ngay (GAP-063 scope)
- Pros: kênh push văn hóa-VN-phù-hợp ngay.
- Cons: cần Zalo OA registration + ZNS template approval (vendor + legal lead-time) — block beta launch. Phase 1.5 chấp nhận hơn.
- **Rejected → GAP-063 Phase 2:** in-app fallback đủ Phase 1; Zalo lead-time quá dài cho beta.

### Alternative B: `@Async` event listener thay REQUIRES_NEW
- Pros: truly fire-and-forget.
- Cons: thread pool + retry complexity; notify cần atomicity với in-app banner persist (REQUIRES_NEW cho txn riêng đủ, không cần async machinery). `@Async` đúng khi cần off-thread thật.
- **Rejected:** REQUIRES_NEW đơn giản hơn cho nhu cầu "isolate, không cần off-thread".

### Alternative C: Email-only + retry queue
- Pros: ít code.
- Cons: không giải quyết single-channel-point (email spam/missing); retry queue không thay được fallback channel.
- **Rejected:** không đóng root cause (single channel).

## Implementation Notes

- **Code:** `notification/channel/{NotificationChannel, OwnerNotificationDispatcher, OwnerNotification, EmailNotificationChannel, InAppNotificationChannel}`; `notification/enums/NotificationChannelType`; `notification/entity/InAppNotification` + repo/DTO/controller/service.
- **Migration:** V74 `in_app_notifications`.
- **Rollback:** dispatcher fan-out gracefully no-op nếu channel beans vắng; revert = chỉ wire EmailNotificationChannel.
- **Phase 2 (GAP-063):** `ZaloNotificationChannel implements NotificationChannel` + register → auto fan.
- **Test:** dispatcher fan-out + per-channel deliver (best-effort false-on-fail).

## References

- Channel seam doc: [`subscription-billing/api-contract.md`](../../01-business/kitehub/subscription-billing/api-contract.md) §Notification channel seam + in-app endpoints
- Win-back lifecycle seam: [ADR-043](ADR-043-manual-vietqr-dunning-involuntary-churn-lifecycle.md) §3 (involuntary vs voluntary)
- Receipt bundled: [`subscription-billing/api-contract.md`](../../01-business/kitehub/subscription-billing/api-contract.md) GET `/payments/{id}/receipt` (GAP-1266)
- Isolation principle: `.claude/rules/audit-service-isolation.md` (REQUIRES_NEW for best-effort side-effects) + `.claude/rules/design-patterns.md` §3.11
- Strategy pattern: `.claude/rules/design-patterns.md` §2
- Email infra: [ADR-007](ADR-007-outbox-pattern-for-events.md) (outbox) + email-architecture.md
- Related gaps: GAP-1265/1257/1266/1263/063

## Log

- 2026-06-13 — Initial proposal + ACCEPTED same day (solo-dev). Documents NotificationChannel abstraction + in-app fallback + REQUIRES_NEW isolation shipped wave kitehub-biz-100 BE-4 (commit `34ae639ae`). Zalo full build DEFERRED GAP-063 Phase 2. Reviewer: @nguyenvankiet (solo-dev acting architect + txn-isolation scout).
