# GAP-1054: SubscriptionPendingNullableColumnsIT — FK-parent-instance seed fix

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-07 (Wave p0-2 GAP-942 verify — agent-drafted IT had FK seed bug)
**Affects:** `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/repository/SubscriptionPendingNullableColumnsIT.java`

## Problem

A regression IT for GAP-942 (`SubscriptionPendingNullableColumnsIT`) was drafted to guard V62 (`subscriptions.started_at`/`expires_at` nullable for PENDING state) — Flyway-replay on Testcontainers Postgres proving a PENDING subscription persists with both columns null (the round-trip that previously threw `SQLState 23502`). The IT FAILS with `fk_subscription_instance` violation: it inserts a `Subscription` with a random `instanceId` but no parent row in `instances` (`subscriptions.instance_id` is NOT NULL + FK → `instances`).

```
ERROR: insert or update on table "subscriptions" violates foreign key constraint "fk_subscription_instance"
Detail: Key (instance_id)=(...) is not present in table "instances".
```

## Proposed Fix

Seed a minimal parent `instances` row before the subscription INSERT (fixed `INSTANCE_ID` constant). Required NOT-NULL/no-default `instances` columns: `id, subdomain, organization_name, owner_id, tier, status, database_url, database_username, database_password, created_at, updated_at` (tier/status must satisfy CHECK constraints — use valid enum values, e.g. tier='FREE', status='TRIAL'). Seed via injected `JdbcTemplate` native INSERT in `@BeforeEach` (cleanup with `deleteAll` ordering FK-safe: subscriptions before instances). Then `pendingSubscription()` uses `INSTANCE_ID`.

## Acceptance Criteria

- [ ] IT seeds parent `instances` row → `pendingSubscription_persistsWithNullStartedAtAndExpiresAt` PASS
- [ ] `activatedSubscription_persistsWithNonNullDates` PASS (no V62 regression)
- [ ] `./mvnw -pl kitehub-subscription test -Dtest=SubscriptionPendingNullableColumnsIT` green
- [ ] Closes GAP-942 AC (regression guard for PENDING nullable persist)

## Related

- Parent: GAP-942 (V62 nullable — production fix verified live; this is the automated regression guard)
- Discovered in: Wave p0-2 2026-06-07 (agent-drafted IT FK seed bug)
