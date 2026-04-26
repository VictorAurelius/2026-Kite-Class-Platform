# GAP-230: design-patterns.md §3.5.1 — add Exception D for dedicated dispatcher infrastructure (AIQueueDispatcher)

**Status:** 🟢 DONE — Exception D rule landed (`design-patterns.md` v1.3.0); AIQueueDispatcher marker applied; EmailServiceClient + InstancePurgeService triaged out (both fail criterion 3 — see Triage table)
**Priority:** 🟠 P1 Meta (rule clarification — blocks closing one design-pattern audit Cat 5 finding)
**Domain:** Rules / Governance
**Found:** 2026-04-26 (during GAP-222a Phase 2 scoping)
**Parent gap:** GAP-222 (Outbox Bypass Policy + Migration)
**Related:** GAP-222a (re-scoped + Phase 2 shipped — this gap continues from §AIQueueDispatcher case)
**Decision record:** ADR-021 (per-module domain outbox)

## Current State (verified 2026-04-26)

`design-patterns.md` §3.5.1 lists 3 exceptions to the "must use outbox" rule:
- **Exception A** — Fast-path with outbox backup (best-effort direct publish + outbox row reliability net)
- **Exception B** — Bean wiring / Config code
- **Exception C** — Test fixtures

Rule wording: *"Default: every cross-service event MUST flow through `OutboxEventWriter` (or per-module domain outbox). Direct `rabbitTemplate.convertAndSend(...)` is BANNED unless one of the documented exceptions below applies."*

The 2026-04-26 design-pattern audit (Cat 5) flagged 5 bypass sites. GAP-222a Phase 2 closed 1 (`BrandingJobService` via Exception A). Remaining `kitehub-branding` site:

| File | Line | Nature |
|------|------|--------|
| `kitehub-branding/.../queue/AIQueueDispatcher.java` | 65, 70 | The class IS the AI-job dispatcher. Its single purpose is `convertAndSend` to a tier queue. Wrapping it in outbox = wrap-the-wrapper. |

`AIQueueDispatcher` is not "a service that publishes a domain event as a side-effect of business logic" (the rule's target). It is dedicated dispatch infrastructure invoked synchronously by callers that already wrote their domain rows in their own transactions.

## Problem

Three of the five existing exceptions (A/B/C) don't fit:
- **A** doesn't fit: there's no domain transaction co-located with the dispatch — caller transactions ended before the dispatch is invoked
- **B** doesn't fit: this isn't config/wiring code; it runs at request time
- **C** doesn't fit: this is production code

So the rule currently classifies `AIQueueDispatcher` as a violation, but forcing it through outbox would either:
1. Add latency to AI job dispatch (the queue's whole purpose is fast handoff)
2. Move the responsibility to callers (which is what AIQueueDispatcher exists to centralize)

Either resolution requires a rule decision.

## Root Cause

The exception list grew from a single `BrandingEventPublisher` use-case (Exception A). Cases of "dedicated dispatcher infrastructure" weren't anticipated when §3.5.1 was authored 2026-04-26 morning.

## Proposed Fix

Add **Exception D — Dedicated dispatcher infrastructure** to `design-patterns.md` §3.5.1:

> **Exception D — Dedicated dispatcher infrastructure**
>
> A class whose single purpose is to publish events to RabbitMQ on behalf of callers (e.g. `AIQueueDispatcher` routes by tier, `EmailDispatcher` formats + sends) is exempt when:
>
> 1. The class is suffixed with `Dispatcher`, `Publisher`, or equivalent infrastructure naming
> 2. Callers MUST have already persisted their domain change before invoking it (verified by code review, not enforced)
> 3. The dispatcher itself contains no business logic — only routing + serialization + send
> 4. A class-level javadoc states the marker phrase `dedicated dispatcher infrastructure`
>
> Rationale: wrapping a dispatcher in outbox creates wrap-the-wrapper and adds latency to the operation the dispatcher exists to make fast. The reliability concern moves to callers, which write domain row + outbox row before invoking the dispatcher.

### Then in implementation:

1. Update `AIQueueDispatcher.java` class-level javadoc to include the marker phrase
2. (Optional, future) Update audit detector to recognize the marker — currently it would still flag; reviewer manually accepts under Exception D until detector updated

## Acceptance Criteria

- [x] `design-patterns.md` §3.5.1 v1.2.0 → v1.3.0 with new Exception D documented
- [x] `AIQueueDispatcher` javadoc updated with marker phrase
- [ ] Next design-pattern audit explicitly checks each Exception D claim against the 4-criterion test (deferred — happens at next audit run, not part of this gap)
- [x] Decision logged in this gap for `EmailServiceClient` + `InstancePurgeService` — see Triage table below

## Triage decision (audit Cat 5 hits)

Applied 4-criterion test of Exception D to all 5 audit hits:

| File | Naming (1) | Caller persists first (2) | No business logic (3) | Verdict |
|------|:----------:|:------------------------:|:---------------------:|---------|
| `AIQueueDispatcher` | ✅ `Dispatcher` | ✅ `BrandingJobService.createJob` writes job row before invoke | ✅ pure routing + metrics + send | **✅ Exception D — marker applied this PR** |
| `EmailServiceClient` | ✅ `Client` | ✅ callers pass formed event | ❌ has `useQueue` config toggle, writes email_sent_log row, hybrid HTTP/queue dispatch | **❌ FAIL crit 3 — needs refactor (extract pure dispatcher) OR Exception A migration** |
| `InstancePurgeService` | ❌ `Service` | n/a | ❌ multi-repo orchestration (instance + backup + email log) + domain logic | **❌ FAIL crit 1 + 3 — needs Exception A migration (write outbox row + best-effort publish)** |
| `BrandingJobService` (already migrated) | n/a | n/a | n/a | ✅ Closed by GAP-222a Phase 2 (Exception A) |
| `BrandingEventPublisher` (already documented) | n/a | n/a | n/a | ✅ Already Exception A with marker |

**Net audit-cat-5 status after this PR:**
- 5 raw hits → 1 documented under Exception A (BrandingEventPublisher), 1 documented under Exception A (BrandingJobService via GAP-222a), 1 documented under Exception D (AIQueueDispatcher this PR), 2 unresolved (EmailServiceClient + InstancePurgeService)
- Unresolved 2 → tracked under **GAP-222c** (existing) which is being kept open and re-scoped. Effort: M (mostly Exception A migration pattern repetition + consider extract-pure-dispatcher refactor for EmailServiceClient)

## Dependencies

- ADR-021 PROPOSED 2026-04-26 (governs the per-module outbox direction)
- GAP-222a Phase 2 SHIPPED 2026-04-26 (this gap continues from there)
- Reviewer approval per `rule-change-process.md` §5 for MINOR rule bump

## Risk / Tradeoffs

- **Risk of escape hatch abuse** — adding exceptions weakens the rule. Mitigated by (a) 4-criterion test, (b) marker phrase requirement enforced at audit time, (c) reviewer must accept each claim explicitly per `audit-to-gap-pipeline.md`
- **Detector update lag** — until `quality/design-pattern-audit/SKILL.md` recognizes Exception D, every audit will keep flagging `AIQueueDispatcher`. Acceptable short-term; reviewer accepts manually

## Related

- Parent: GAP-222 (Outbox Bypass Policy + Migration)
- Predecessor: GAP-222a (this gap continues from there)
- Rule: `.claude/rules/design-patterns.md` §3.5.1 (target of the change)
- ADR: `documents/02-architecture/adr/ADR-021-per-module-outbox-vs-shared-lib.md`
- Audit: `documents/04-quality/audits/design-patterns/audit-2026-04-26.md` Cat 5 (catalogues the unresolved hits)

## Log

- **2026-04-26 (later — SHIPPED):** Status 🔵 OPEN → 🟢 DONE. `design-patterns.md` v1.2.0 → v1.3.0 with Exception D + 4-criterion test + AIQueueDispatcher example. Marker applied to `AIQueueDispatcher` class-level javadoc. Triage table populated: only `AIQueueDispatcher` qualifies under D; `EmailServiceClient` (hybrid HTTP/queue + email log) and `InstancePurgeService` (multi-repo orchestrator) both fail criterion 3 (no business logic) — both need Exception A migration tracked under existing GAP-222c. Net audit Cat 5 status: 3/5 documented (1 D, 2 A), 2/5 still need migration via GAP-222c.
- 2026-04-26 — Filed during GAP-222a Phase 2 implementation. Phase 2 closed `BrandingJobService` under Exception A; `AIQueueDispatcher` did not fit any of A/B/C, so a 4th exception is needed. EmailServiceClient + InstancePurgeService in `kitehub-subscription` may also qualify — to be triaged when this rule lands.
