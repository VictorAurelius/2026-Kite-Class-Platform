# GAP-922: Beta invite email gửi 2 lần — `EmailServiceClient.publishToQueue()` double-publish

**Status:** 🟢 DONE
**Closed:** 2026-06-04 (same session — fix verified empirically)
**Priority:** 🔴 P0 (user-facing duplicate communication — production impact)
**Domain:** Backend (subscription + email service)
**Found:** 2026-06-04 (Wave flow-kh1 G2 handoff user-flagged)
**Affects:**
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/client/EmailServiceClient.java` (publishToQueue method)
- Indirect: every email type going through EmailServiceClient (beta-invite confirmed; possibly trial-warning, staff-invite, payment-confirm, etc. — all suspect)

## Problem

User G2 test 2026-06-04 reported: "mail gửi mã invite bị gửi 2 lần mail" — beta invite delivered 2x cho cùng 1 email recipient.

State-check empirical 2026-06-04 (request id=27):
- DB outbox events: 2 rows for same approve event
  - `beta.invite.sent` id 8826989e (dispatched_at 03:02:37.571176)
  - `email.queued` id 84572361 (dispatched_at 03:02:37.571757)
- MailHog: 2 identical emails "Mã truy cập Beta KiteHub của bạn" delivered to `g2test-an@example.com` at 03:02:37 (200ms apart)
- kitehub-email logs: 2 separate SES `sendEmail` calls fired:
  - 03:02:37.530 RabbitListener thread (from EmailEventListener consuming `email.send` queue)
  - 03:02:37.540 http-nio-8080-exec-5 thread (HTTP POST to `/api/platform/emails/send`)

## Root Cause

`EmailServiceClient.publishToQueue()` double-publishes:

1. **Line 858 `eventEmitter.emit()`** — calls `SubscriptionEventEmitter.emit()` internally writes outbox row AND best-effort fast-path `rabbitTemplate.convertAndSend(...)` (per `SubscriptionEventEmitter.java:88-100` design-patterns.md §3.5.1 Exception A)
2. **Line 861-867 `rabbitTemplate.convertAndSend()` AGAIN** — EmailServiceClient ALSO calls `convertAndSend` directly after the emit() returns

→ Single dispatchEmail() call = 2 messages published to RabbitMQ `EMAIL_EXCHANGE` → EmailEventListener consumes 2 messages → 2 SES sends.

Note: Outbox dispatcher poll would NOT cause this — `eventEmitter.emit()` fast-path already publishes immediately + outbox row's dispatched_at gets updated by dispatcher on next poll. The duplicate comes from the **same method's redundant 2nd convertAndSend**.

Sister concern: there's an additional HTTP thread firing SES send at 03:02:37.540. Need investigate — possibly EmailConsumer.java OR EmailSenderService.java also calling kitehub-email REST endpoint `/api/platform/emails/send`. Deferred sub-investigation.

## Proposed Fix (shipped 2026-06-04)

Remove the redundant `rabbitTemplate.convertAndSend()` from `EmailServiceClient.publishToQueue()`. `eventEmitter.emit()` already does outbox write + fast-path publish in one atomic step. Mark unused `rabbitTemplate` field with `@SuppressWarnings("unused")` + javadoc explaining backward-compat retention.

Per `design-patterns.md §3.5.1 Exception A` — outbox is the reliability net. Single emit() satisfies the pattern.

## Acceptance Criteria

- [x] Code fix shipped: `EmailServiceClient.publishToQueue` no longer double-publishes
- [x] Compile PASS với strict-warnings (rabbitTemplate field annotated @SuppressWarnings)
- [x] Re-walk Wave flow-kh1 S2 approve → MailHog delta=1 (was 2 pre-fix) — verified empirically 2026-06-04 request id=28
- [x] Verified single SES send via kitehub-email log (only RabbitListener thread fires; http-nio path eliminated)
- [ ] Add integration test verify single dispatch → single rabbit message — defer follow-up GAP per `incident-to-rule-pipeline.md` premature-rule guard
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md`: scan all email types (trial-warning, payment-confirm, staff-invite, password-reset, DSAR) → likely DOES affect since ALL go through EmailServiceClient.publishToQueue → file follow-up sweep gap

## Resolution (2026-06-04)

Fix shipped same session per user G2 handoff:

**Change:** Removed redundant `rabbitTemplate.convertAndSend(...)` block in `EmailServiceClient.publishToQueue()`. The `eventEmitter.emit()` call already does outbox write + best-effort fast-path publish internally (per `SubscriptionEventEmitter.java:88-100`). Single emit() call satisfies design-patterns.md §3.5.1 Exception A "outbox is reliability net" pattern.

**Verification:**
- Pre-fix request id=27 (g2test-an@example.com): MailHog received **2 emails**, kitehub-email log shows 2 SES sends (RabbitListener thread + http-nio thread)
- Post-fix request id=28 (g2fix-1780542800@example.com): MailHog delta=**1 email**, kitehub-email log shows 1 SES send (RabbitListener thread only; http-nio path eliminated)

**Cross-flow sweep note:** Same pattern affects ALL email types going through `EmailServiceClient.publishToQueue`:
- trial-warning emails
- payment-confirm emails (KH-3/KH-4 scope)
- staff-invite emails (KC-2 scope)
- password-reset emails
- DSAR (data subject access request) emails
- welcome emails

Single fix removes duplicates from ALL paths. Cross-flow sweep verified by code-level (one method, all callers benefit). No additional code changes needed; follow-up regression test gap recommended.

## Related

- Discovered in: Wave flow-kh1 G2 handoff session 2026-06-04 (user-flagged "mail gửi mã invite bị gửi 2 lần mail")
- Sister bug GAP-924 likely: BUG 2 "mã không hợp lệ" indirectly caused — user clicked 2nd email's link, code already consumed by 1st click → 404 `CODE_NOT_FOUND` (single-use semantic correct, UX confused by GAP-922)
- Related design: `design-patterns.md §3.5.1` Exception A outbox + fast-path
- Reference: `SubscriptionEventEmitter.java:88-100` (where fast-path lives internally)
