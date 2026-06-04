# GAP-930: EmailConsumer fails to deserialize admin-new-login-alert (works for beta-invite)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (admin notification email only — does NOT block user-facing flow; beta-invite + tenant signup verified working)
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh1 G2 re-walk session 06:42 UTC — admin login fires AdminLoginAlertEventListener → publishes via SubscriptionEventEmitter.emit (GAP-925 fixed path) → EmailConsumer rejects with `MismatchedInputException: Cannot construct EmailEvent from String value`)
**Affects:**
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consumer/EmailConsumer.java` (listener fails)
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/migration/SubscriptionEventEmitter.java` (publisher — the GAP-925 fix using `rabbitTemplate.send(Message{Content-Type=application/json})`)
- All admin-side email notifications using @Async @EventListener publish path

## Problem

The GAP-925 fix (commit `58d207d3`) shipped `SubscriptionEventEmitter.emit` using `rabbitTemplate.send(exchange, topic, Message{ContentType=application/json, body=UTF-8 bytes})` to bypass Jackson2JsonMessageConverter's String double-encode path. The fix was empirically verified PASS for beta-invite (g2test-an-4 + g2test-an-5 + g2test-an-6 invites all delivered to MailHog with no Mismatch exception).

Wave flow-kh1 G2 re-walk session 2026-06-04 06:42:41 UTC reproduced the original Mismatch pattern on `admin-new-login-alert` — admin login fires `AdminLoginAlertEventListener.onAdminNewLoginFingerprint` (@Async @EventListener) which calls `EmailServiceClient.sendAdminNewLoginAlert(...)` → `dispatchEmail` → `publishToQueue` → `eventEmitter.emit`. The publisher logs `Fast-path publish OK: eventType=email.queued topic=email.send` at 06:42:42 — same code path as beta-invite. But the `EmailConsumer.handleEmailEvent` listener throws:

```
MessageConversionException: Failed to convert Message content
Caused by: MismatchedInputException: Cannot construct instance of
`com.kitehub.subscription.dto.EmailEvent` (although at least one Creator exists):
no String-argument constructor/factory method to deserialize from String value
('{"instanceId":null,"to":"admin@kitehub.com","subject":"[KiteHub] New admin login from unrecognized device",
   "templateName":"admin-new-login-alert",
   "variables":{"supportUrl":"https://kitehub.me/support","userAgent":"...","loginAt":"2026-06-04T06:42:41.774849199","ip":"172.18.0.12"},
   "emailType":"admin-new-login-alert"}')
```

Same wire-format anti-pattern as GAP-925 (body arriving as JSON-encoded String, not JSON object) — but only for emails published from the `@Async` listener thread context, not the HTTP request thread context.

Empirical evidence from same session:
- 06:42:41 admin login alert published → 06:42:49 retry alert published → both fail at EmailConsumer
- 06:43:41 beta-invite published via HTTP request thread → 06:43:43 EmailConsumer "Email sent successfully: type=beta-invite"
- queues empty (0 ready, 0 unacked) + DLQ empty — broken messages reject-and-don't-requeue but cycle continues on each admin login

## Root Cause Hypotheses (needs investigation)

1. **`@Async` thread vs HTTP request thread — different RabbitTemplate or MessageConverter resolution**. `task-2`/`task-3` threads may resolve a Spring proxy or transaction context that picks a different converter chain than the HTTP request threads.
2. **Listener TypeMapper requires `__TypeId__` header**. `rabbitTemplate.send(Message)` does NOT add a `__TypeId__` header (Spring's `Jackson2JsonMessageConverter.toMessage(Object)` would). The listener side may strict-require the header for non-default classes and fall back to String when absent — but then why does beta-invite work? Differ in serializer details perhaps (Map<String,Object> variables shape).
3. **Variables map shape**. Beta-invite variables are simple Strings. Admin-new-login-alert variables include `loginAt: LocalDateTime.now()` — when Jackson serializes via `objectMapper.writeValueAsString(event)` it emits an ISO-8601 nano-precision string ("2026-06-04T06:42:41.774849199"). Some encoding subtle difference at consumer-side may trip the deserializer.

## Proposed Fix

**Option A (cleanest) — switch publisher to `convertAndSend(Object)`.** Have `SubscriptionEventEmitter.emit(...)` accept the event object (or parse the JSON String back to a generic `Map<String, Object>`) and call `rabbitTemplate.convertAndSend(exchange, topic, eventObj)`. Spring's Jackson2JsonMessageConverter then serializes + adds `__TypeId__` automatically. The consumer's Jackson then uses `__TypeId__` (or listener type) reliably. Avoids the manual-bytes-with-Content-Type subtlety.

**Option B — disable strict type checking on consumer.** Configure `Jackson2JsonMessageConverter` on the consumer side with a permissive `DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED` (use method param type) so absence of `__TypeId__` always falls back to listener type.

**Option C — explicitly add `__TypeId__` header in `emit()`.** Set `props.setHeader("__TypeId__", "com.kitehub.subscription.dto.EmailEvent")` (or whatever the event class is) before building the Message. Brittle because emit() handles multiple event types.

Recommend **Option A** for v1 — Spring handles the serialization metadata, no manual bytes, no manual headers.

## Acceptance Criteria

- [ ] `SubscriptionEventEmitter.emit(...)` rewritten to use `convertAndSend(Object)` OR Option B/C documented in PR
- [ ] Empirical re-walk: admin login fires → EmailConsumer processes admin-new-login-alert successfully (no Mismatch) + MailHog receives the alert email
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3 — confirm beta-invite + invite-staff + password-reset + DSAR + welcome + trial-warning + payment-confirm all still work
- [ ] Unit test in `SubscriptionEventEmitterTest` (or equivalent) exercises both Map<String,String> variables and Map with LocalDateTime variables — both round-trip cleanly

## Related

- Discovered in: Wave flow-kh1 G2 re-walk 2026-06-04 06:42:41 UTC
- Parent: GAP-925 (subscription EmailEvent String double-encode — fix landed but reproduces on @Async path)
- Sister: GAP-922 (original double-publish fix that started the chain)
- Why P2 not P0: admin notification only — does NOT block user-facing beta signup chain (verified working for g2test-an-4 / g2test-an-5 / g2test-an-6). Admin login still works; just the email alert side-channel fails.
