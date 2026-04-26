# GAP-222a: Per-Module Domain Outbox for kitehub-branding (re-scoped 2026-04-26 — see ADR-021)

**Status:** 🟡 PARTIAL — ADR-021 PROPOSED 2026-04-26 redirects scope from "extract shared library" to "ratify per-module pattern + apply to kitehub-branding"
**Priority:** 🟠 P1 (still unblocks GAP-222c, now via per-module outbox precedent rather than shared lib)
**Domain:** Backend / Architecture
**Found:** 2026-04-26 (during GAP-222 Phase 2 scoping in Sub-PR 6.4)
**Re-scoped:** 2026-04-26 (Phase 1 state-check — see ADR-021)
**Parent gap:** GAP-222 (Outbox Bypass Policy + Migration)
**Predecessor for:** GAP-222c (kitehub-branding + kitehub-subscription migration)
**Decision record:** [ADR-021](../../02-architecture/adr/ADR-021-per-module-outbox-vs-shared-lib.md)

## Current State (verified 2026-04-26)

| Asset | Location | Notes |
|-------|----------|-------|
| Outbox infra (8 classes) | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/outbox/` | `OutboxEvent` / `OutboxStatus` / `OutboxEventRepository` / `OutboxEventWriter` / `OutboxEventPublisher` / `EventDispatcher` / `LoggingEventDispatcher` / `DispatchException` |
| Internal users in kiteclass-core | 4 services | `BrandingEventPublisher`, `RebrandApprovalService`, `InstanceLifecycleService`, `PlanExecutor` — all already use the generic `OutboxEventWriter` |
| Domain-specific outbox in kitehub-subscription | `kitehub/kitehub-subscription/.../outbox/` | `MigrationOutboxEvent` + `MigrationOutboxRepository` — **migration-specific, not generic** |
| Outbox infra in kitehub-branding | None | Module has zero outbox infra → cannot migrate its 3 bypass sites without this gap |
| Existing shared library | `kitehub/kitehub-shared/` (if any) | **TBD** — verify whether shared module exists or needs creation |

## Problem

GAP-222 Phase 2 needs to migrate 5 services to outbox, but the only generic implementation lives inside `kiteclass-core`. Three of the five services live in kitehub modules (`kitehub-branding`, `kitehub-subscription`) which cannot legitimately import from `kiteclass-core` (would violate module boundaries — KiteHub manages KiteClass instances, not vice-versa).

**Original framing:** extract a cross-product shared library so all modules can `import OutboxEventWriter`.

**Revised framing (ADR-021):** there is no need for a cross-product shared library. The codebase already runs a `MigrationOutboxRepository` precedent in `kitehub-subscription`, and `design-patterns.md` §3.5.1 explicitly endorses domain-specific outbox. Each kitehub module that needs outbox owns one — same pattern, separate physical classes. Result: GAP-222c is unblocked by this re-scope without adding shared infrastructure.

## Root Cause

When the outbox pattern was introduced (Wave 4, GAP-021 area), it was placed inside `kiteclass-core/common/` because the first user (`BrandingEventPublisher`) was a kiteclass-core consumer. No subsequent reuse forced extraction. Cross-module need only surfaced when GAP-222 audit (Sub-PR 6.1) catalogued bypass sites in kitehub modules.

## Proposed Fix (revised — see ADR-021)

### Phase 1 — Decision recorded (DONE 2026-04-26)

State-check found:
- `kitehub-shared` does not exist
- `kitehub-platform` is kitehub-only (cross-product use would invert dependency direction)
- `kitehub/` and `kiteclass/` have separate Maven roots with no top-level aggregator
- `kitehub-subscription` already runs domain-specific `MigrationOutboxRepository` — established precedent

Decision: **per-module domain outbox**, not extraction. Documented in ADR-021. Original Options A (new shared module) + B (existing kitehub-shared) rejected; new Option D (per-module pattern, copying `MigrationOutboxRepository` precedent) accepted.

### Phase 2 — Apply per-module pattern to kitehub-branding (S, ~30min)

1. Copy 8-class outbox structure from `kiteclass-core/common/outbox/` into `kitehub-branding/.../outbox/` with package rename `com.kitehub.branding.outbox`
2. Add Flyway migration `V<N>__create_branding_outbox.sql` creating `branding_outbox_events` table (same shape as `outbox_events`)
3. Wire Spring `@ComponentScan` to pick up the new package
4. Migrate 3 known `kitehub-branding` direct-publish bypass sites (catalogued by design-pattern audit Cat 5) to use `BrandingOutboxEventWriter`
5. Verify by re-running design-pattern audit Cat 5 — bypass count should drop to zero in `kitehub-branding`

### Phase 3 — Rule clarification (S, ≤15 min)

Extend `design-patterns.md` §3.5.1 example list to call out per-module domain outbox as the primary cross-product path (currently mentioned only as parenthetical exception). Tracked here, not as a separate gap — small text change.

### Phase 4 — Verify (S, ≤30 min)

Build `kitehub-branding` + run tests + design-pattern audit. No behavior change to existing `kiteclass-core` consumers.

## Acceptance Criteria

- [x] Decision recorded as ADR-021 (PROPOSED 2026-04-26; ratified by next reviewer pass per `rule-change-process.md` §5)
- [ ] `kitehub-branding` has its own outbox infra (entity + repository + writer + table) following `MigrationOutboxRepository` precedent
- [ ] 3 direct-publish bypass sites in `kitehub-branding` migrated to `BrandingOutboxEventWriter`
- [ ] Existing 4 `kiteclass-core` outbox consumers unchanged + still compile + tests pass
- [ ] `design-patterns.md` §3.5.1 example list extended to mention per-module pattern as primary
- [ ] Design-pattern audit Cat 5 re-run shows 0 bypass sites in `kitehub-branding`

## Dependencies

- ADR review (architect / lead) for module-location decision
- No DB or behavior changes — pure module reorganization

## Risk / Tradeoffs

- **Cyclic dependency** if shared module accidentally depends on a domain module → guard via Maven `<scope>` review during PR
- **Bean wiring** — Spring `@Component` scan must include the new package in each consumer's `@SpringBootApplication`/`@ComponentScan`
- **Migration entity uniqueness** — `kitehub-subscription`'s `MigrationOutboxEvent` (domain-specific) stays put; only generic outbox moves. No conflict.

## Related

- Parent: GAP-222 (Outbox Bypass Policy + Migration)
- Blocks: GAP-222c (kitehub-* migration cannot start without this)
- Sibling: GAP-222b (ParentInvitationServiceImpl — kiteclass-core internal, not blocked by this gap)
- Rule: `.claude/rules/design-patterns.md` §3.5 + §3.5.1 (this gap eliminates the geographic excuse for bypass)
- Audit: `documents/04-quality/audits/design-patterns/audit-2026-04-26.md` Cat 5

## Log

- **2026-04-26 (later, scope correction):** Status 🔵 OPEN → 🟡 PARTIAL. Phase 1 state-check found `kitehub-shared` does not exist, `kitehub-platform` is kitehub-only (cross-product use would invert direction), `kitehub/` and `kiteclass/` have separate Maven roots with no aggregator. Existing `MigrationOutboxRepository` in `kitehub-subscription` is precedent for per-module domain outbox; `design-patterns.md` §3.5.1 explicitly endorses the pattern. Original "extract shared library" framing rejected — disproportionate infra cost vs duplicating ~8 stable Spring Boot classes per module. ADR-021 PROPOSED documents the decision. Scope re-shaped: Phase 2 = copy pattern into `kitehub-branding` (~30min), Phase 3 = small `design-patterns.md` §3.5.1 clarification. Acceptance criteria updated. Filename retained for git history continuity but title changed to reflect new scope.
- 2026-04-26 — Gap created during Sub-PR 6.4 scope check. State-check confirmed: 8 generic outbox classes already exist in kiteclass-core, 4 internal users; kitehub-branding has zero outbox infra; kitehub-subscription has only domain-specific migration outbox. Extraction is structural-refactor scope (not new infra).
