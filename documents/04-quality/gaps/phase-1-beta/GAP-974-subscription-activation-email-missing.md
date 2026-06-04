# GAP-974: Subscription activation email "Subscription đã kích hoạt" not emitted on state machine PENDING → ACTIVE

**Status:** 🔵 OPEN
**Priority:** P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KH-3 G1 re-walk production-equivalent — coordinator walk)
**Affects:** Every Owner upgrading FREE/TRIAL → BASIC/PREMIUM via manual VietQR + admin confirm flow (KH-3 chain)

## Problem

KH-3 G1 re-walk 2026-06-04 verified state machine PENDING → ACTIVE works correctly (Subscription `BASIC ACTIVE` + Payment `COMPLETED` + Instance `tier=BASIC, status=ACTIVE`), BUT no email "Subscription đã kích hoạt" sent to Owner.

Empirical evidence:
- `subscription_outbox` ORDER BY created_at DESC LIMIT 3 → all 3 rows = `admin-new-login-alert` (login alerts emit), zero `subscription.activated` events
- `kitehub-email` logs `[SMTP] Email sent` only for `admin-new-login-alert` template, no `subscription-activated`
- `grep -rn "subscription.*activated\|enqueueSubscription\|sendSubscriptionEmail\|SUBSCRIPTION_ACTIVATED" kitehub-subscription/src/main/java` → 0 hits

Wave plan §3 expected outcome + G2 recipe Step 6 expectation both reference "email 'Subscription đã kích hoạt'". Initial G1 walk (per ROADMAP entry) claimed email arrived — likely misidentified admin login alert email.

## Root Cause

`SubscriptionService.applyPendingUpgrade` (called by `PaymentService.confirmPayment` on payment confirm) flips Subscription PENDING → ACTIVE + Instance tier/status, but DOES NOT enqueue email event to `subscription_outbox`. Email pipeline (kitehub-email consumer + template + SMTP MailHog) works correctly — proven by 7 admin login alert emails delivered same session.

Missing component: outbox enqueue call in `applyPendingUpgrade` (or sister event listener) with template `subscription-activated` + variables (tenant name, tier, expires_at, support URL).

## Proposed Fix

1. Add `subscription-activated.hbs` template in `kitehub-email/src/main/resources/templates/` (Vietnamese narrative + English identifiers per `dev-readable-doc-language.md` §4)
2. In `SubscriptionService.applyPendingUpgrade` (or sister `SubscriptionEventEmitter`), enqueue subscription_outbox row:
   ```java
   outbox.enqueue("subscription.activated", "email.send",
     EmailEvent.builder()
       .instanceId(subscription.getInstanceId())
       .to(ownerEmail)
       .subject("[KiteHub] Gói " + tier + " đã kích hoạt")
       .templateName("subscription-activated")
       .variables(Map.of("tenantName", ..., "tier", ..., "expiresAt", ..., "supportUrl", ...))
       .build());
   ```
3. Per `design-patterns.md` §3.5 Outbox pattern: enqueue within same `@Transactional` block as state machine flip
4. Integration test verify email outbox row created + dispatched

## Acceptance Criteria

- [ ] After admin confirm payment, `subscription_outbox` contains row with `event_type=subscription.activated`, `template=subscription-activated`, `to=<owner-email>`
- [ ] MailHog shows email with subject pattern `[KiteHub] Gói * đã kích hoạt` to owner email
- [ ] IT test `SubscriptionServiceTest.applyPendingUpgrade_emitsActivationEmail()` passes
- [ ] G1 re-walk Step 6 PASS without caveat

## Related

- Discovered in: Wave flow-kh3 KH-3 G1 re-walk 2026-06-04 (this session)
- Sister gap: GAP-944 (cross-module payment cache invalidation via RabbitMQ — different scope but same event-emission family)
- Sister event already working: `admin-new-login-alert` template + outbox path proves pipeline functional
- Rule reference: `design-patterns.md` §3.5 Outbox pattern (same-txn enqueue)
- Rule reference: `dev-readable-doc-language.md` §4 (template language convention)
- Flow Verification Campaign §4 row KH-3 (this walk validates rest of flow PASS — email is P1 polish not blocker)

## Log

- **2026-06-04** Gap filed inline during KH-3 G1 re-walk per `discovery-to-gap-inline-filing.md` v1.0.0 §3. KH-3 G1 walk verdict = ✅ PASS production-equivalent on 5/6 mandatory checks (login + sub create PENDING + Payment populated + admin confirm + sub ACTIVE + instance flip); email Step 6 = P1 polish gap, not state-machine blocker. Initial G1 walk ROADMAP claim "email ✅" likely misidentified admin login alert. Flip Campaign §4 KH-3 → `🔄 walk-pass-pending-human` with this caveat documented.
