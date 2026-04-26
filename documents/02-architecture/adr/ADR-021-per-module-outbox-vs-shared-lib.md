# ADR-021: Per-Module Domain Outbox over Cross-Product Shared Library

**Status:** PROPOSED
**Date:** 2026-04-26
**Deciders:** @nguyenvankiet (solo-dev)
**Related Gap(s):** GAP-222a (re-scoped by this ADR), GAP-222c (unblocked by it)

## Context

`ADR-007` accepted the Transactional Outbox pattern for reliable event publishing. Implementation landed in `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/outbox/` (8 classes: `OutboxEvent`, `OutboxStatus`, `OutboxEventRepository`, `OutboxEventWriter`, `OutboxEventPublisher`, `EventDispatcher`, `LoggingEventDispatcher`, `DispatchException`). Four `kiteclass-core` services consume it via `OutboxEventWriter`.

The 2026-04-26 design-pattern audit (Sub-PR 6.1, score 70/100) catalogued **direct broker-publish bypass sites in `kitehub-branding` and `kitehub-subscription`**. The audit-derived rule §3.5.1 ("Outbox Bypass Policy") permits only three exceptions; the catalogued sites match none.

GAP-222a was filed to "extract the outbox infrastructure to a shared library reachable by both kiteclass-core and kitehub-* modules" so the bypasses could migrate without crossing module boundaries.

State-check during scoping found:
- `kitehub-shared` does not exist
- `kitehub-platform` is a kitehub-only shared module (5 kitehub-* services depend on it; cross-product use would invert dependency direction)
- `kitehub/` and `kiteclass/` have **separate Maven roots** with no top-level aggregator POM
- `kitehub-subscription` already runs a **domain-specific outbox** (`MigrationOutboxEvent` + `MigrationOutboxRepository`) — established precedent
- Rule `design-patterns.md` §3.5.1 explicitly endorses domain-specific outbox: *"Default: every cross-service event MUST flow through `OutboxEventWriter` (or **domain-specific outbox like `MigrationOutboxRepository`**)"*

The forces in tension:
- **DRY pull:** 8 infrastructure classes shouldn't be copy-pasted
- **Microservices independence:** KiteHub and KiteClass must deploy independently; cross-product Maven coupling fights this
- **Solo-dev capacity:** new top-level aggregator + artifact registry is high-overhead infra for a single maintainer
- **Established precedent:** `MigrationOutboxRepository` already shows the pattern works inside this codebase

## Decision

**We will NOT extract a cross-product outbox shared library. Each module that needs an outbox will own a domain-specific outbox (entity + repository + writer), following the `MigrationOutboxRepository` precedent.**

Concretely:
- `kiteclass-core` keeps its current generic `OutboxEvent` / `OutboxEventWriter` for consumers inside `kiteclass-core`
- `kitehub-subscription` keeps its existing `MigrationOutboxEvent` (already domain-specific)
- `kitehub-branding` will introduce its own `BrandingOutboxEvent` + `BrandingOutboxRepository` + writer (Phase 2 of re-scoped GAP-222a)
- Any future kitehub-* module needing outbox follows the same per-module pattern
- `design-patterns.md` §3.5.1 examples list will be extended to call out this pattern as the default path for cross-product modules

This re-scopes GAP-222a from "extract shared library" to "ratify per-module pattern + apply to kitehub-branding."

## Consequences

### Positive
- **Module independence preserved** — no cross-product Maven coupling; KiteHub and KiteClass remain independently buildable + deployable
- **Zero new infrastructure** — no top-level aggregator POM, no artifact registry, no relative-path Maven hacks
- **Pattern already proven in this codebase** — `MigrationOutboxRepository` validates the approach
- **Rule alignment** — directly endorsed by `design-patterns.md` §3.5.1
- **Lower bar to migrate bypass sites** — `kitehub-branding` Phase 2 effort drops from "extract + plumb shared lib" (M) to "copy pattern" (S, ~30min)
- **Matches Sam Newman's microservices guidance** — duplication across service boundaries is preferable to coupling for infrastructure code

### Negative
- **Code duplication of ~8 classes per module** — accepted; classes are stable Spring Boot boilerplate, not domain logic
- **Drift risk over time** — if `OutboxEventPublisher` poll logic evolves, each module must update separately
- **Per-module audit burden** — design-pattern audit must enumerate each module's outbox; already handled because audit is filesystem-walking

### Neutral
- Each consuming module gets its own `outbox_events` table (or `<domain>_outbox_events` like `migration_outbox_events`) — same schema shape
- Each module's outbox is a separate Flyway migration timeline — no cross-module migration ordering concerns
- `design-patterns.md` §3.5.1 needs a small clarification to reference per-module pattern as primary, not exception

## Alternatives Considered

### Alternative A: New cross-product Maven module `infrastructure/shared-libs/outbox/`
Pros:
- True DRY — single source for the 8 classes
- Future cross-product infrastructure code has a home

Cons:
- Requires top-level aggregator POM (or relative-path module references) — fragile in CI
- Spring `@ComponentScan` boundaries get complicated; consumers must explicitly include the new package
- Bumps coupling: bug in shared lib forces both products to upgrade in lockstep
- Maven plumbing alone is 1-2h of overhead before any code moves

Rejected because: the infrastructure cost is disproportionate for 8 stable classes when a precedent already exists. Solo-dev mode amplifies the cost.

### Alternative B: Publish outbox as a versioned artifact (Nexus / GitHub Packages)
Pros:
- Standard JVM enterprise practice
- Versioned upgrades give consumers control

Cons:
- Requires hosting an artifact registry (or paying for one)
- CI must publish on every change to shared lib
- Solo-dev: registry is one more system to maintain + secrets to manage
- Slower iteration: every shared-lib change is a publish-then-upgrade dance

Rejected because: registry overhead is unjustified at current team + traffic scale.

### Alternative C: Move outbox into `kitehub-platform` and have `kiteclass-core` depend on it
Pros:
- Reuses existing kitehub shared module

Cons:
- **Inverts dependency direction** — `kiteclass-core` would depend on `kitehub-platform`, but KiteHub manages KiteClass instances, not vice-versa
- Pollutes `kitehub-platform` with infrastructure concerns it didn't ask for
- Still doesn't solve the problem cleanly (kitehub modules already depend on kitehub-platform; that's fine — it's the kiteclass side that breaks)

Rejected because: violates the architectural direction documented in CLAUDE.md.

### Alternative D: Duplicate the 8 classes into each module needing outbox (this ADR)
**Selected.** See Decision section.

## Implementation Notes

### Phase 2 (re-scoped GAP-222a, ~30min)
1. In `kitehub-branding`, copy the 8-class outbox structure from `kiteclass-core` with package rename (`com.kitehub.branding.outbox`)
2. Add Flyway migration `V<N>__create_branding_outbox.sql` creating `branding_outbox_events` table (same shape as `outbox_events`)
3. Wire Spring `@ComponentScan` so the new package is picked up
4. Migrate the 3 known bypass sites (catalogued by design-pattern audit Cat 5) to use the new `BrandingOutboxEventWriter`
5. Verify by running design-pattern audit Cat 5 — bypass count should drop to zero in `kitehub-branding`

### Rollback plan
If the per-module pattern proves problematic (e.g., drift hurts more than infrastructure cost would), revisit with Alternative A. The duplicated classes are ≤200 LOC each — re-extraction is straightforward.

### Success criteria
- `kitehub-branding` direct-publish bypass count = 0 in next design-pattern audit
- Build time delta for `kitehub-branding` adding outbox infra: ≤10s
- No Spring bean wiring conflicts in integration tests

### Rule update (separate concern)
`design-patterns.md` §3.5.1 example list will be extended in a small follow-up edit to call out per-module domain outbox as the primary cross-product path. Tracked under GAP-222a re-scope; not part of this ADR.

## References

- ADR-007 (parent — established the outbox pattern itself)
- Rule: `.claude/rules/design-patterns.md` §3.5 + §3.5.1 (Outbox Bypass Policy)
- Gap: `documents/04-quality/gaps/GAP-222a-extract-outbox-shared-lib.md` (re-scoped by this ADR)
- Audit: `documents/04-quality/audits/design-patterns/audit-2026-04-26.md` Cat 5
- Sam Newman, *Building Microservices* 2nd ed., Ch.5 ("Don't share code across services")
- Existing precedent: `kitehub/kitehub-subscription/.../outbox/MigrationOutboxRepository.java`

## Log

- 2026-04-26 — Initial proposal. Written after Phase 1 state-check found that GAP-222a's "extract shared library" framing carried disproportionate infrastructure cost given the existing `MigrationOutboxRepository` precedent and the rule's explicit endorsement of domain-specific outbox.
