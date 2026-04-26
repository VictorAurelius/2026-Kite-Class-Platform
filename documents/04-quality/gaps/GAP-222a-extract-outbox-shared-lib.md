# GAP-222a: Extract Outbox Infrastructure to Shared Library

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (blocker for GAP-222c — without shared lib, kitehub modules can't migrate without violating module boundaries)
**Domain:** Backend / Architecture / Build
**Found:** 2026-04-26 (during GAP-222 Phase 2 scoping in Sub-PR 6.4)
**Parent gap:** GAP-222 (Outbox Bypass Policy + Migration)
**Predecessor for:** GAP-222c (kitehub-branding + kitehub-subscription migration)

## Current State (verified 2026-04-26)

| Asset | Location | Notes |
|-------|----------|-------|
| Outbox infra (8 classes) | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/outbox/` | `OutboxEvent` / `OutboxStatus` / `OutboxEventRepository` / `OutboxEventWriter` / `OutboxEventPublisher` / `EventDispatcher` / `LoggingEventDispatcher` / `DispatchException` |
| Internal users in kiteclass-core | 4 services | `BrandingEventPublisher`, `RebrandApprovalService`, `InstanceLifecycleService`, `PlanExecutor` — all already use the generic `OutboxEventWriter` |
| Domain-specific outbox in kitehub-subscription | `kitehub/kitehub-subscription/.../outbox/` | `MigrationOutboxEvent` + `MigrationOutboxRepository` — **migration-specific, not generic** |
| Outbox infra in kitehub-branding | None | Module has zero outbox infra → cannot migrate its 3 bypass sites without this gap |
| Existing shared library | `kitehub/kitehub-shared/` (if any) | **TBD** — verify whether shared module exists or needs creation |

## Problem

GAP-222 Phase 2 needs to migrate 5 services to `OutboxEventWriter`, but the only generic implementation lives inside `kiteclass-core`. Three of the five services live in kitehub modules (`kitehub-branding`, `kitehub-subscription`) which cannot legitimately import from `kiteclass-core` (would violate module boundaries — KiteHub manages KiteClass instances, not vice-versa).

Result: GAP-222c is blocked until generic outbox is reachable from kitehub modules.

## Root Cause

When the outbox pattern was introduced (Wave 4, GAP-021 area), it was placed inside `kiteclass-core/common/` because the first user (`BrandingEventPublisher`) was a kiteclass-core consumer. No subsequent reuse forced extraction. Cross-module need only surfaced when GAP-222 audit (Sub-PR 6.1) catalogued bypass sites in kitehub modules.

## Proposed Fix

### Phase 1 — Decide module location (S, ≤30 min)

Two architectural options:

| Option | Description | Pros | Cons |
|--------|-------------|------|------|
| A | Create new `infrastructure/shared-libs/outbox/` Maven module that both `kiteclass-core` and `kitehub-*` depend on | Clean module hierarchy; zero cyclic-dep risk | New module + Maven plumbing (~1h overhead) |
| B | Extract into existing `kitehub-shared` module if one exists | Reuses existing structure | Requires `kitehub-shared` to actually exist (verify first) |
| C | Duplicate the 8 classes inside each module needing it | Zero plumbing | Drift risk; defeats the purpose |

**Recommend Option A or B (B if module exists).** State-check needed: `find . -name "kitehub-shared" -type d`.

### Phase 2 — Move classes (S-M, 1-2h)

Move 8 classes from `kiteclass-core/common/outbox/` to chosen shared location with package rename. Update 4 internal kiteclass-core users + add Maven dep on the new shared lib. JPA entity table name stays unchanged (`outbox_events`) so no DB migration needed.

### Phase 3 — Verify (S, ≤30 min)

Build all modules + run kiteclass-core tests + run any existing outbox-publisher integration tests. No behavior change expected.

## Acceptance Criteria

- [ ] Decision recorded as ADR under `documents/02-architecture/adr/` (Option A vs B vs C)
- [ ] 8 outbox classes live in shared module reachable by both `kiteclass-core` and `kitehub-*`
- [ ] 4 existing internal users still compile + tests pass
- [ ] DB schema unchanged (no migration needed)
- [ ] kitehub-branding can now `import` `OutboxEventWriter` without violating module boundaries

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

- 2026-04-26 — Gap created during Sub-PR 6.4 scope check. State-check confirmed: 8 generic outbox classes already exist in kiteclass-core, 4 internal users; kitehub-branding has zero outbox infra; kitehub-subscription has only domain-specific migration outbox. Extraction is structural-refactor scope (not new infra).
