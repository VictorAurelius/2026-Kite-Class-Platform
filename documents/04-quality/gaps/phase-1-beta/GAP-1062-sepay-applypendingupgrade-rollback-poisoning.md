# GAP-1062: `applyPendingUpgrade` failure poisons SePay webhook txn → payment capture rolled back

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (reliability — payment captured nhưng mất do rollback-only poisoning)
**Domain:** Backend
**Found:** 2026-06-08 (SePay Test-Mode logic verify — GAP-1058 execution)
**Affects:** `kitehub-subscription` — `SubscriptionService.applyPendingUpgrade` (line 447) + 3 callers trong `PaymentService` (lines 264, 327, 439)

## Problem

Trong `PaymentService.processSepayWebhook` (`@Transactional`):
1. `payment.complete(sepayId)` + `save()` chạy OK (log "Payment ... completed via SePay transaction ...")
2. `applyPendingUpgrade(subscriptionId, paymentId)` gọi trong try/catch
3. `applyPendingUpgrade` (`@Transactional` default = REQUIRED → **join parent txn**) throw (vd subscription soft-deleted `deleted=true` → `findById` filter → `orElseThrow`)
4. try/catch nuốt exception + log "Failed to apply pending upgrade" — NHƯNG inner đã set **rollback-only** trên parent txn
5. `processSepayWebhook` commit → `UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only` → HTTP 400
6. **payment.complete() bị rollback** → payment vẫn PENDING

Đây CÙNG class incident admin-login 500 (2026-05-16, `audit-service-isolation.md` / `design-patterns.md` §3.11): downstream best-effort step wrapped try/catch nhưng join parent txn → poison.

Code comment line 332 hứa *"Payment captured; subscription update retried by admin/job mechanism"* — implementation KHÔNG deliver: payment capture mất luôn. Production: SePay retry → lại 400 → payment không bao giờ PAID (với mọi applyPendingUpgrade throw: soft-deleted sub, optimistic lock, instance-activation fail, email fail downstream).

**Empirical evidence (recipe verify 2026-06-08):** happy path với subscription soft-deleted → 400 `rollback-only`, payment PENDING. Với subscription ACTIVE (graceful return) → 200 + COMPLETED. Xác nhận poisoning chỉ khi applyPendingUpgrade *throw*, nhưng đó là latent reliability hole.

## Proposed Fix

`SubscriptionService.applyPendingUpgrade` → `@Transactional(propagation = Propagation.REQUIRES_NEW)` (per `design-patterns.md` §3.11 + `audit-service-isolation.md` pattern) → failure isolate, không poison payment-capture txn. Aligns với code intent "payment captured independent of upgrade".

**Caller sweep (per `api-contract-change-caller-sweep.md`):** 3 callers đều ở `PaymentService` (lines 264/327/439), đều payment-confirm flow (payment.complete trước, upgrade best-effort sau) → REQUIRES_NEW consistent cho cả 3. Không có test mock stub (IT-level). Cần chạy full `kitehub-subscription` test suite (`@DataJpaTest` + REQUIRES_NEW interaction) trước merge.

## Acceptance Criteria

- [ ] `applyPendingUpgrade` REQUIRES_NEW (hoặc tách payment-commit khỏi upgrade rõ ràng)
- [ ] Re-verify recipe: happy path với subscription **soft-deleted** → vẫn 200 + payment COMPLETED (upgrade fail isolated)
- [ ] `./mvnw test` kitehub-subscription PASS (REQUIRES_NEW không phá IT)
- [ ] Regression IT: webhook completes payment khi applyPendingUpgrade throws
- [ ] Walk evidence trong closure (per `feature-ship-runtime-walk-mandate.md` §3)

## Related

- Surfaced by: `documents/05-guides/operations/sepay-webhook-local-verify-recipe.md` (GAP-1058)
- Same class: `audit-service-isolation.md` + `design-patterns.md` §3.11 (2026-05-16 admin-login 500)
- Affects logic-verify completeness của GAP-975/976 (PAID flip persistence)
- Discovered in: verify branch `verify/sepay-975-976-test-mode-logic` 2026-06-08
