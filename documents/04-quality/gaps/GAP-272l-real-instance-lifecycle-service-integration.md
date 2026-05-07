# GAP-272l: Real `InstanceLifecycleService` integration for `LifecycleInline`

**Status:** 🟢 DONE 2026-05-07 (Wave 34 Bucket C — PR #908 + Bucket D PR #910) — §6 compliance hinge enforced
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

- [x] Backend lifecycle events endpoint live — `LifecycleEventsController` + `GET /api/v1/branding/instances/{instanceId}/lifecycle/events` returning `{instanceId, events[], nextCursor}`. Documented in api-contract.md (Bucket 0 PR #905)
- [x] FE hook (`useLifecycleEvents`) calls real endpoint replacing `_lifecycle-mock.ts` and `buildMockEvents()` (Bucket D PR #910)
- [x] Mock service preserved for unit tests via MSW handler
- [x] LifecycleInline component refactored to consume hook (D); §6 compliance verified — zero direct `setStatus(...)` callsites remain in branding state-changing paths
- [x] `InstanceLifecycleService` created from scratch — none existed pre-Wave-34. Audit option α+γ shipped: persistent `BrandingLifecycleEvent` table (V30) + RabbitMQ event publish on transition (no cross-module dependency on kiteclass `AuditLog*`). 4 callsites in `BrandingJobService` refactored to route through service.

## Log

- **2026-05-07:** Wave 34 Bucket C (PR #908) shipped `InstanceLifecycleService` + state machine matching `ai-branding-guidelines.md` §6 + V30 migration + 4 callsite refactors (the §6 compliance hinge). Bucket D (PR #910) wired `useLifecycleEvents` hook. Cross-module audit log (kiteclass-core's `AuditLog*`) deemed too heavy for this scope — chose in-module `BrandingLifecycleEvent` entity + RabbitMQ publish (option α+γ). 11 new tests + 4 regression tests verify hinge holds.

## Related

- GAP-272 (parent)
- Wave 32 rework Bucket D (PR #890) — LifecycleInline + hook abstraction
- `ai-branding-guidelines.md` §6 (InstanceLifecycleService mandate)
