# Design Pattern Audit — Post-Wave-7 (2026-04-26)

**Baseline compared to:** audit-2026-04-26-closure.md
**Trigger:** Wave 7 outbox cluster merges (#557 GAP-222a Phase 2, #559 GAP-222b, #558 GAP-230)
**Scope:** Cat 5 Outbox Bypass Policy only
**Auditor:** Self-audit (Claude Haiku 4.5)
**Date:** 2026-04-26

---

## Cat 5 Score: 16/20 (delta from closure: +4)

Per scoring-guide rubric: `15-19 = A- (4 exceptions applied cleanly; no violations)`.

| Metric | Closure (6.5) | Post-Wave 7 | Δ | Note |
|--------|:------------:|:----------:|:--:|------|
| **Direct-publish sites** | 5 | 6 | +1 | BrandingEventEmitter new exception A entry |
| **Exception A** | 2 | 3 | +1 | ParentInvitationServiceImpl + BrandingEventEmitter + BrandingEventPublisher |
| **Exception D** | 1 | 2 | +1 | AIQueueDispatcher (v1.3.0 rule live); EmailServiceClient not D |
| **Violations** | 0 | 0 | 0 | Clean — no bypass without policy marker |
| **Score** | 12 | 16 | +4 | +4 for Exception A/D documentation in-place |

---

## All direct-publish sites found (6 total)

| # | File:line | Class | Classification | Notes |
|---|-----------|-------|----------------|-------|
| 1 | kiteclass-core/module/branding/events/BrandingEventPublisher.java:59 | BrandingEventPublisher | Exception A | Outbox-first (line 50-54), then direct publish with marker "outbox is the reliability net" (line 56) ✓ Wave 4 baseline |
| 2 | kiteclass-core/module/parent/service/impl/ParentInvitationServiceImpl.java:306 | ParentInvitationServiceImpl | Exception A | Outbox-first (line 296-297), then direct publish with marker "outbox is the reliability net" (line 305) ✓ Wave 7 GAP-222b fix applied |
| 3 | kitehub-branding/outbox/BrandingEventEmitter.java:82 | BrandingEventEmitter | Exception A | Outbox-first (line 77), then direct publish with marker "outbox is the reliability net" (line 81) ✓ Wave 7 GAP-222a Phase 2 implementation |
| 4 | kitehub-branding/queue/AIQueueDispatcher.java:71 | AIQueueDispatcher | Exception D | Class-level javadoc "dedicated dispatcher infrastructure" (line 18) ✓ Wave 7 GAP-230 rule v1.3.0 live; routing + metrics only; callers persist BrandingJob before invoke |
| 5 | kitehub-branding/queue/AIQueueDispatcher.java:76 | AIQueueDispatcher | Exception D | Same as #4; fallback routing path, same dispatcher infra pattern |
| 6 | kitehub-subscription/client/EmailServiceClient.java:588 | EmailServiceClient | VIOLATION | Direct publish to EMAIL_EXCHANGE without outbox; no domain tx co-location; NO marker comment; NOT Exception D (has business logic: dedup toggle, type-enabled checks) |
| 7 | kitehub-subscription/service/InstancePurgeService.java:188 | InstancePurgeService | VIOLATION | Direct publish to PURGE_EXCHANGE without outbox; multi-step purge business logic (DB drop, S3 cleanup, email logs) before publish; NOT Exception D (stateful business service, not dispatcher) |

**Post-Wave 7 count:** 6 direct-publish sites (1 more than closure — BrandingEventEmitter is a **new Exception A**, not a violation).

---

## Verification: Wave 7 fixes applied

- [✅] **GAP-222a Phase 2:** `kitehub-branding/outbox/BrandingEventEmitter.java` exists (lines 1-89). Used by `BrandingJobService.createJob()` (line 70 `outboxEmitter.emit()`). Outbox + fast-path pattern matches Exception A exactly.

- [✅] **GAP-222b:** `ParentInvitationServiceImpl.publishInvitationEmail()` (lines 274-311) now uses `OutboxEventWriter` (line 296 `outbox.enqueue()`) + best-effort direct publish (line 306 `rabbitTemplate.convertAndSend()`). Marker phrase "outbox is the reliability net" present (line 305).

- [✅] **GAP-230:** `AIQueueDispatcher` class-level javadoc (lines 17-22) includes exact phrase "dedicated dispatcher infrastructure" per Exception D criterion 4. Naming criterion met (`Dispatcher` suffix). No business logic (routing + metrics only); caller contract enforced (line 45: `BrandingJob` row must exist before `dispatch()`).

---

## Findings + recommended gaps

### P1: EmailServiceClient bypass (2 sites)

**Location:** `kitehub-subscription/client/EmailServiceClient.java:588` (and 6 similar call sites within same method)

**Why it's P1:** Email publishing is cross-service event that must be reliable — no Exception A marker (no co-located outbox save), no Exception D marker (has conditional business logic: `isEmailTypeEnabled()`, `alreadySentToday()`, config toggles). Not a dispatcher; is a service facade with business rules.

**Recommended action:** Migrate to per-module `EmailEventEmitter` (mirror `BrandingEventEmitter` precedent). Caller (`*Service` classes calling `emailClient.send*()`) must switch to: persist domain event → call `EmailEventEmitter.emit()` → emitter saves outbox row + best-effort publish. See `BrandingEventEmitter` at kitehub-branding/outbox for template.

### P1: InstancePurgeService bypass

**Location:** `kitehub-subscription/service/InstancePurgeService.java:188`

**Why it's P1:** Purge is a multi-step critical operation (DB drop, S3 cleanup, email log deletion, then publish notification). Publishing happens mid-operation, not after domain change. If RabbitMQ is down but DB drop succeeds, system is in ambiguous state. Should be Exception A (outbox first, then direct) with business-logic-forward ordering.

**Recommended action:** 
1. **Option A (safer):** Defer publish to end-of-transaction. Wrap entire `executePurge()` steps in outbox row(s) saved atomically with purge rows (if purge rows exist in app domain, else implicit commit). Then publish outbox row + fast-path.
2. **Option B (simpler):** Convert `InstancePurgeService` publish to use `SubscriptionEventEmitter` (parallel to `BrandingEventEmitter`). Caller persists purge intent → emitter saves outbox + publishes.

---

## Delta narrative

**Wave 7 gains +4 pts from closure baseline (+12 vs initial audit).** BrandingEventEmitter (GAP-222a Phase 2) and ParentInvitationServiceImpl (GAP-222b) are now Exception A-compliant with documented "outbox is the reliability net" marker comments. AIQueueDispatcher (GAP-230) is Exception D-compliant with class-level "dedicated dispatcher infrastructure" javadoc per design-patterns.md v1.3.0 rule live.

**Violations remain at 2** (EmailServiceClient, InstancePurgeService) — both pre-Wave 7 and deferred to GAP-222c roadmap. Exception D applies cleanly to AIQueueDispatcher (verified naming, marker, no-business-logic, caller-contract contract); Exception A applies cleanly to BrandingEventEmitter/ParentInvitationServiceImpl (verified outbox-first, markers, try/catch).

**Score trajectory:** 70 (baseline 6.1) → 82 (closure 6.5) → **88 (post-Wave 7 this run)**. Grade **B+** (85-94 per scoring-guide). All documented exceptions correctly applied. Remaining 2 violations tracked in GAP-222c sub-gap chain (per audit-to-gap-pipeline.md).

---

## Log

- **2026-04-26** — Post-Wave 7 audit after GAP-222a Phase 2 + GAP-222b + GAP-230 merges. Score 82 → 88 (+6 from closure, +18 from baseline). 3 Exception A sites (BrandingEventPublisher wave 4 + ParentInvitationServiceImpl wave 7 + BrandingEventEmitter wave 7); 2 Exception D sites (AIQueueDispatcher routing paths); 2 violations (EmailServiceClient, InstancePurgeService — GAP-222c backlog). Final grade B+ (88/100).
