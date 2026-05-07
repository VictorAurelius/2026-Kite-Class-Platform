# GAP-272l: Real `InstanceLifecycleService` integration for `LifecycleInline`

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (kitehub-branding lifecycle service) + Frontend hook wiring
**Found:** 2026-05-07 (Wave 32 REWORK Bucket D — `LifecycleInline.tsx`)
**Affects:** AI Branding Wizard Step 6 — lifecycle state transitions must use real `InstanceLifecycleService` per `ai-branding-guidelines.md` §6
**Related:** GAP-272 (parent — Track 2 port for ai-branding-wizard v2)

## Problem

`ai-branding-guidelines.md` §6 mandates: "State transitions via
`InstanceLifecycleService` ONLY". Wave 32 v1 (PR #883) violated this by
having `LifecycleInline` render-only stateless with hardcoded
`buildMockEvents()` progression.

Wave 32 REWORK Bucket D corrected the violation: `LifecycleInline.tsx`
calls `useInstanceLifecycle(instanceId)` hook which sources state from
`(customer)/instances/_lifecycle-mock.ts` — a mock service with proper
typed events.

State-check 2026-05-07:
- Real backend service for lifecycle events: NO equivalent of
  `useInstanceLifecycle` calling actual `InstanceLifecycleService` REST or
  SSE endpoint
- `_lifecycle-mock.ts` is a typed mock returning hardcoded event sequences
  for development; production needs real service backing

Inline TODO: `TODO(GAP-272l): wire to real InstanceLifecycleService`.

## Root Cause

`InstanceLifecycleService` exists in backend per AI Branding architecture
docs but the FE-consumable hook + endpoint aren't yet wired. Bucket D's
mock keeps the API surface stable (same hook signature, same event shape)
so backend swap is hook-internal — no component changes needed.

## Proposed Fix

1. **Backend:** expose lifecycle events via SSE or REST polling endpoint
   `GET /api/v1/branding/instances/{instanceId}/lifecycle/events` returning
   chronological event list since timestamp param
2. **Frontend:** replace `_lifecycle-mock.ts` implementation with real
   service call (preserving hook signature `useInstanceLifecycle(instanceId)`)
3. **Migration:** keep mock available as test fixture under
   `__mocks__/instance-lifecycle.ts` for unit tests

## Acceptance Criteria

- [ ] Backend lifecycle events endpoint live + documented in api-contract.md
- [ ] `useInstanceLifecycle` calls real endpoint
- [ ] Mock service preserved for tests
- [ ] No `LifecycleInline` component changes required (proves Bucket D's
      hook abstraction worked)

## Related

- GAP-272 (parent)
- Wave 32 rework Bucket D (PR #890) — LifecycleInline + hook abstraction
- `ai-branding-guidelines.md` §6 (InstanceLifecycleService mandate)
