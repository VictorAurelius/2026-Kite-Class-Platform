# GAP-1273: ConfirmPayment split-brain — owner-notification email-log coupled in tier-flip tx + swallowed failure

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (G3 walk wave-kitehub-biz-100 — KH-3 upgrade confirm, Bug P1)
**Affects:** `kitehub-subscription` — `PaymentService.confirmPayment` + `SubscriptionService.applyPendingUpgrade` + `EmailServiceClient` (revenue / paid-upgrade path)
**Wave:** wave-kitehub-biz-100 (fix branch `wave/kitehub-biz-100-p1fix`)

## Problem

G3 production-parity walk (`documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md`, Bug P1) surfaced a revenue-path split-brain on the KH-3 upgrade-confirm flow:

- Admin confirms a PENDING create-flow payment via `POST /admin/payments/{id}/confirm` → HTTP **200** + payment `status=COMPLETED`.
- But `subscriptions` stayed `tier=FREE status=PENDING pending_tier=PREMIUM`; `instances.tier=FREE status=TRIAL`; `GET /instance/{id}/active` → **404**.
- → DB split-brain: money "COMPLETED" but the gói was NOT upgraded, with **no error signal** to admin / FE.

**Root cause (container log `kitehub-subscription` 16:47):**

```
ERROR PaymentService - Failed to apply pending upgrade for subscription: ee32c9c0-…
org.springframework.dao.DataIntegrityViolationException: … insert into email_sent_log
  … null value in column "recipient" of relation "email_sent_log" violates not-null constraint
  at JpaTransactionManager.doCommit(…)   ← fires at COMMIT, OUTSIDE the inner try/catch
```

Two structural defects (latent, independent of the seed trigger):

- **D1 — coupling:** `SubscriptionService.applyPendingUpgrade` (REQUIRES_NEW tier-flip tx, GAP-1062) calls `emailServiceClient.sendSubscriptionCreatedEmail(instance.getId(), instance.getContactEmail()=NULL, …)`. `EmailServiceClient` was class-level `@Transactional` (REQUIRED), so its `recordEmailSent()` → `EmailSentLog(recipient=NULL)` INSERT joined the SAME tier-flip tx. The INSERT is deferred to flush/commit, so the `try/catch` around the email call gave FALSE safety — the NOT-NULL violation fired at the tier-flip tx COMMIT (after the catch), rolling back the entire tier-flip (`subscriptions.tier` + `instances.tier` sync both reverted).
- **D2 — swallow + lie:** the exception bubbled to `PaymentService.confirmPayment`, whose `try/catch` swallowed it (log-only) and still returned 200 + payment COMPLETED → silent split-brain, no retry path, no admin signal.

**Seed vs prod:** the immediate trigger (`contact_email` NULL) is a seed artifact (beta-signup populates `contact_email` via `AuthService.setContactEmail`). But D1+D2 are real production risks — ANY failure in the email-log branch during upgrade-apply (constraint, DB hiccup, etc.) silent-rolls-back the paid upgrade while reporting success.

## Root Cause

Notification/audit side-effect (the `email_sent_log` INSERT) shared the business transaction (D1), and the caller masked a genuine business-write failure as success (D2). Sister-pattern to the 2026-05-16 admin-login 500 (`audit-service-isolation.md` / `design-patterns.md` §3.11) and GAP-1062 (rollback poisoning) — same family, different boundary.

## Proposed Fix

- **D1 (decouple):** run the `EmailSentLog`/notification side-effect in its own boundary so it can NEVER roll back the paid-upgrade tier-flip — `@Transactional(propagation = REQUIRES_NEW)` on `EmailServiceClient.sendSubscriptionCreatedEmail` + `sendSubscriptionActivatedEmail` (aligns with the BE-4 `InAppNotificationChannel` / `EmailNotificationChannel` already on REQUIRES_NEW) + a defensive NULL-recipient guard in `dispatchEmail` so a missing `contact_email` never reaches the NOT-NULL INSERT.
- **D2 (no silent split-brain):** `confirmPayment` (admin-confirm path) re-throws a genuine `applyPendingUpgrade` failure instead of swallowing → its own tx rolls back (payment → PENDING) → admin sees the error + can re-confirm, keeping payment + subscription consistent. Webhook paths (`processSepayWebhook` / `processPaymentWebhook`) KEEP the GAP-1062 swallow (money moved, gateway must not retry forever — `SepayWebhookRollbackIsolationIT`).

## Acceptance Criteria

- [x] D1: `EmailSentLog` side-effect isolated from the tier-flip tx (REQUIRES_NEW on the 2 upgrade-path email methods) + NULL-recipient guard in `dispatchEmail`.
- [x] D2: `confirmPayment` surfaces a genuine tier-flip failure (re-throw) — never returns 200 with payment COMPLETED + sub PENDING.
- [x] Regression test reproducing the scenario — NULL `contact_email` create-flow upgrade confirm → tier-flip COMMITS consistently (payment COMPLETED + sub ACTIVE/PREMIUM + `instances.tier=PREMIUM`); never split-brain (`ConfirmPaymentSplitBrainIT`, real Postgres + queue mode).
- [x] D2 unit test: `confirmPayment` re-throws on genuine `applyPendingUpgrade` failure (`PaymentServiceTest.confirmPayment_tierFlipFailureSurfaced`).
- [x] `cd kitehub && ./mvnw -pl kitehub-subscription -am test` affected classes green (PaymentServiceTest 14, SubscriptionServiceTest 20, EmailServiceClientTest, SepayWebhookRollbackIsolationIT, ConfirmPaymentSplitBrainIT).
- [x] Live re-walk on the up stack — admin upgrade-confirm of a NULL-`contact_email` instance → no split-brain.

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 + pre-handoff-self-test-completeness.md §3)

**Source audit:** G3 walk `2026-06-13-g3-walk-kitehub-biz-100.md` Bug P1 (KH-3 upgrade confirm).
**Affected scope:** revenue path `confirmPayment → applyPendingUpgrade → sendSubscriptionCreatedEmail`.

- Originating symptom resolved — `ConfirmPaymentSplitBrainIT` (real Postgres + Flyway, queue mode) seeds NULL `contact_email` create-flow + admin-confirms → asserts payment COMPLETED + sub ACTIVE/PREMIUM + `instances.tier=PREMIUM` + pendingTier cleared.
- **Live re-walk via gateway :9000 (2026-06-14, rebuilt `kitehub-subscription` w/ fix on the up stack):** seed instance `b8ac75be…` FREE/TRIAL `contact_email=NULL` + create-flow sub PENDING/FREE `pendingTier=PREMIUM` + PENDING payment → `POST /api/platform/admin/payments/{id}/confirm` (ADMIN HS512 JWT, role PLATFORM_ADMIN) → **HTTP 200, payment status COMPLETED**. DB post-confirm: `payment.status=COMPLETED` + `sub.status=ACTIVE tier=PREMIUM pending_tier=NULL` + `instance.tier=PREMIUM`; `GET /api/platform/subscriptions/instance/{id}/active` → **200**. All three sides consistent — NO split-brain (pre-fix would've been payment COMPLETED + sub stuck PENDING + instance FREE + /active 404). Synthetic seed rows cleaned up post-walk.
- Sister scope — `SepayWebhookRollbackIsolationIT` (GAP-1062) still PASS: webhook payment-capture survives `applyPendingUpgrade` failure (swallow preserved).
- D2 sister — `PaymentServiceTest.confirmPayment_tierFlipFailureSurfaced` PASS: genuine tier-flip failure re-thrown, owner-confirm notification NOT fired.

## Related

- Discovered in: G3 walk wave-kitehub-biz-100 (HEAD `9ebf2ca41`); fixed on `wave/kitehub-biz-100-p1fix`.
- Sister rules: `audit-service-isolation.md` (REQUIRES_NEW for side-effects), `design-patterns.md` §3.11, GAP-1062 (`SepayWebhookRollbackIsolationIT`), GAP-1256 (tier-desync sweep).
