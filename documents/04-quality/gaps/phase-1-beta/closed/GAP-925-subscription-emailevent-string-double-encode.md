# GAP-925: SubscriptionEventEmitter publishes JSON-string-encoded EmailEvent (consumer cannot deserialize)

**Status:** 🟢 DONE 2026-06-04 — empirical re-walk PASS (g2test-an-4 invite email delivered to MailHog 04:38:58, no Mismatch exception)
**Priority:** 🔴 P0 (production-blocker — every email queued via subscription outbox fails consumer deserialization → no email reaches recipient)
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh1 G2 walk — user g2test-an-2 approved but no MailHog message; subscription consumer reflexive failure)
**Affects:**
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/migration/SubscriptionEventEmitter.java` (line 88-100 fast-path)
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/SubscriptionOutboxDispatcher.java` (line 131-136 reliability-net dispatcher)
- All email flows publishing through `EmailServiceClient.publishToQueue` → `eventEmitter.emit` (beta-invite, trial-warning, payment-confirm, staff-invite, password-reset, DSAR, welcome — per GAP-922 cross-flow note)

## Problem

GAP-922 fix (commit 9122ffc3, 2026-06-04 03:14 UTC) removed the duplicate `rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event)` from `EmailServiceClient.publishToQueue` and now relies solely on `eventEmitter.emit(instanceId, eventType, topic, payload)` (where `payload = objectMapper.writeValueAsString(event)`). Inside `SubscriptionEventEmitter.emit` the fast-path calls `rabbitTemplate.convertAndSend(EXCHANGE, topic, payload)` with the same JSON STRING.

Spring AMQP's `Jackson2JsonMessageConverter` detects `payload` is a `String` and **wraps the already-JSON string in another JSON string** (escaping every internal `"` to `\"`). The consumer (`kitehub-email`) then reads the message body as a quoted JSON-of-JSON and Jackson rejects it with:

```
MismatchedInputException: Cannot construct instance of `com.kitehub.subscription.dto.EmailEvent`
(although at least one Creator exists): no String-argument constructor/factory method to
deserialize from String value ('{"instanceId":null,"to":"g2test-an-2@example.com",...}')
```

Empirical reproduction (2026-06-04 04:25 UTC, Wave flow-kh1 G2 walk):
- Admin approved `g2test-an-2@example.com / Nguyễn Văn G2 / G2 Test Center / P2_CENTER_OWNER` at 11:25:44 4/6/2026
- MailHog has 30 messages, none for `g2test-an-2@example.com` (compare: `g2test-an-1@example.com` got beta-invite pre-GAP-922 fix)
- `docker logs kitehub-subscription` shows the deserialization stack trace repeating every retry — bug visible on the publisher side because the same EmailEvent class is registered as a listener via the shared queue binding

The `SubscriptionOutboxDispatcher` reliability-net poll uses identical `rabbitTemplate.convertAndSend(EXCHANGE, topic, payload-as-String)`, so the dispatcher catch-up path has the same defect — RMQ replay does not heal the bad encoding.

## Root Cause

When GAP-922 removed the redundant 2nd publish, the surviving fast-path code in `SubscriptionEventEmitter.emit` keeps using `convertAndSend(exchange, topic, String)`. `Jackson2JsonMessageConverter.toMessage(...)` re-encodes any String body as JSON — adding outer quotes + escaping — so the wire format becomes `"\"{...payload-json...}\""` instead of `{...payload-json...}`.

The previous redundant 2nd publish passed the **`event` object** (not the JSON string), which Jackson serialized correctly. That hid the bug in the emit-path because both publishes reached the consumer, but the object-path message was the one that consumers actually deserialized; the string-path message also arrived but consumers (and now subscription's own listener) silently rejected it. After GAP-922 the object-path is gone and only the broken string-path remains.

## Proposed Fix

Replace `rabbitTemplate.convertAndSend(exchange, topic, payload-as-String)` with an explicit `Message` build:

```java
MessageProperties props = new MessageProperties();
props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
props.setContentEncoding(StandardCharsets.UTF_8.name());
Message msg = new Message(payload.getBytes(StandardCharsets.UTF_8), props);
rabbitTemplate.send(EmailQueueConfig.EMAIL_EXCHANGE, topic, msg);
```

`rabbitTemplate.send(...)` bypasses `Jackson2JsonMessageConverter` — the body bytes go on the wire unchanged, with `Content-Type: application/json` so consumers' converter still parses the JSON correctly.

Apply the same fix to `SubscriptionOutboxDispatcher.dispatchPending()` (reliability-net path uses the identical pattern).

## Acceptance Criteria

- [x] `SubscriptionEventEmitter.emit` builds explicit `Message` with `Content-Type: application/json` instead of `convertAndSend(String)`
- [x] `SubscriptionOutboxDispatcher.dispatchPending` mirrors the same fix (cross-flow sweep per `cross-flow-bug-class-sweep.md` §3)
- [x] `InstancePurgeService.publishCleanupEvent` verified EXEMPT — passes `event` Object, Jackson serializes correctly
- [x] Empirical re-walk: approve a fresh beta access request → MailHog receives the beta-invite email + no subscription listener exception — VERIFIED 2026-06-04 04:38:58 (request id=31 → `g2test-an-4@example.com`, log `Fast-path publish OK: eventType=email.queued topic=email.send`, MailHog total 30→31)
- [x] Rebuild `kitehub-subscription` Docker image and confirm the new code lands (avoids GAP-866-style stale-image regression) — `kitehub-subscription:latest` rebuilt 2026-06-04 04:32

## Log

- **2026-06-04 (DONE):** Wave flow-kh1 bundled fix. 2 code edits + Docker rebuild:
  - `SubscriptionEventEmitter.java` — fast-path `convertAndSend(..., String)` → `send(..., Message{Content-Type=application/json, body=UTF-8 bytes})`. Bypasses `Jackson2JsonMessageConverter` String double-encode path.
  - `SubscriptionOutboxDispatcher.java` — same fix for reliability-net catch-up path (cross-flow sweep per `cross-flow-bug-class-sweep.md` §3).
  - `kitehub-subscription:latest` Docker image rebuilt + container restarted (image age 2026-06-04 04:32).
  - Empirical re-walk: admin coordinator approved beta access request id=31 (`g2test-an-4@example.com`) at 04:38:58 → subscription log shows `Fast-path publish OK: eventType=email.queued topic=email.send` (no `MismatchedInputException`) → MailHog received message with subject `Mã truy cập Beta KiteHub của bạn` (UTF-8 quoted-printable). Counterfactual pre-fix (g2test-an-2 approved at 04:25): same flow, MailHog received NO message, subscription log shows repeating `MismatchedInputException`.
  - Cross-flow sweep evidence table inline in §Related.
  - DONE per `gap-done-discipline.md` §2 — every AC checkbox `[x]`, no banned phrase in Log, no deferred sub-tasks.

## Related

- Triggered by: GAP-922 fix (commit 9122ffc3) — necessary fix for double-publish, but exposed the latent String-body-encoding bug
- Companion of: GAP-924 (FE 2FA silent 401) — both surfaced during the same Wave flow-kh1 G2 walk session
- Cross-flow sweep evidence per `.claude/rules/cross-flow-bug-class-sweep.md` §3:

| Site | Verdict | Reason |
|---|---|---|
| `SubscriptionEventEmitter.java:90` fast-path | **FIX** | Originating bug — String body double-encoded |
| `SubscriptionOutboxDispatcher.java:132` reliability-net | **FIX** (sister) | Same String body, same broken pattern |
| `InstancePurgeService.java:201` purge event | **EXEMPT** | Passes `event` Object, Jackson serializes correctly |
| `EmailServiceClient.java:838,865` | **EXEMPT** | Javadoc + comment references only, no actual call site |

- Per `release-fix-retry-budget.md` §3.5 Investigation phase — empirical log + diff read identified the cause first attempt
