# GAP-1044: Stale `*IT` integration tests not auth-migrated (SUB-20 + Wave 101 @PreAuthorize drift)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-06 (Wave security-2 Bucket B IDOR caller-sweep)
**Affects:** `SubscriptionBillingIT` (kitehub-subscription), `BrandingControllerInputCapIT` + `BrandingFlowIT` (kitehub-branding)

## Problem

Discovered while running the caller-sweep for Wave security-2 Bucket B (cross-tenant IDOR fix). Three `*IT` integration tests are stale — broken by **prior waves'** changes, not by the IDOR work:

1. **`SubscriptionBillingIT`** (`@SpringBootTest @ActiveProfiles("test")`):
   - `createSubscriptionForInstance` asserts `status=ACTIVE` + `tier=BASIC`, but `SubscriptionService.createSubscription` was changed to **SUB-20 manual VietQR gate** (commit ac54a419 / flow-kh3 2026-06-04) → now returns `status=PENDING, tier=FREE, pendingTier=BASIC`. Assertions stale.
   - `upgradeSubscriptionTier` calls `/upgrade` after create, but `upgradeSubscription` requires `status=ACTIVE` (create now makes PENDING) → would throw "Can only upgrade active subscriptions".
   - Additionally, the `@SpringBootTest` context fails to load locally ("Failed to load ApplicationContext" — needs RabbitMQ/Redis infra absent in local env).

2. **`BrandingControllerInputCapIT`** + **`BrandingFlowIT`** (`@SpringBootTest @ActiveProfiles("test")`):
   - Hit `@PreAuthorize(OWNER_AUTHZ)` AI-branding endpoints **without** `@WithMockUser` / auth → anonymous → method-security 403. `@PreAuthorize` was added Wave 101 Bucket B (GAP-562); these older GAP-258 input-cap ITs were not auth-migrated → now assert `200` but get `403`.

**Why not caught earlier:** all three are `*IT.java`. The project has **no maven-failsafe plugin**, and CI runs `./mvnw clean test` (surefire). Surefire's Spring-Boot-parent default `<includes>` match `*Test.java` but **not** `*IT.java` → these classes do not run in CI's `mvn test`. They only surface when explicitly named via `-Dtest=`. So they've been silently stale since their respective prior waves.

**Confirmation the IDOR fix did not cause these:** Bucket B's `AIBrandingController` guard uses `requireInstanceOwnershipIfPresent(null, ...)` which early-returns when `X-Instance-Id` is absent (these ITs send none) → zero behavior change; the 403 is purely the Wave-101 `@PreAuthorize`. `SubscriptionBillingIT` edits were reverted (kept identical to main).

## Proposed Fix

1. Decide test strategy for `*IT`: either (a) add maven-failsafe + run `*IT` in CI `verify` phase, OR (b) rename CI-relevant ITs to `*Test.java`, OR (c) keep `*IT` as opt-in local-only integration tests + document.
2. Auth-migrate the 3 ITs: add `@WithMockUser(roles="OWNER")` + `X-Tenant-Id`/`X-Instance-Id` headers (branding) and update `SubscriptionBillingIT` to the SUB-20 PENDING flow (create → admin confirm payment → assert ACTIVE).
3. Fix `SubscriptionBillingIT` `@SpringBootTest` context-load (Testcontainers for infra deps OR `@MockitoBean` the missing beans).

## Acceptance Criteria

- [ ] CI test strategy for `*IT` decided + documented (failsafe wire OR rename OR opt-in)
- [ ] 3 ITs auth-migrated + SUB-20-aligned, pass when run
- [ ] `SubscriptionBillingIT` context loads (or is intentionally excluded with rationale)

## Related

- Discovered in: Wave security-2 Bucket B IDOR PR (GAP-1015/1019/1023)
- Root-cause waves: SUB-20 create gate (flow-kh3, commit ac54a419) + Wave 101 Bucket B @PreAuthorize (GAP-562)
- Pattern sister: per `audit-to-gap-pipeline.md` §2.8 fix-time state-check + `discovery-to-gap-inline-filing.md` §1
