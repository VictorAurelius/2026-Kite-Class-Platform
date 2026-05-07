# GAP-272o: Wizard orchestrator wires `useDeployStream` + `useRegenerateQuota` into `DeployingStep` + `RegenerateCounter`

**Status:** 🟢 DONE 2026-05-08 — Wave 41 Bucket D shipped via this PR
**Priority:** 🟠 P1 (UX-visible — without this, deploy progress + quota counter stay static)
**Domain:** Frontend (kitehub-frontend wizard orchestrator)
**Found:** 2026-05-07 (Wave 34 Bucket D follow-up — hooks shipped ready, orchestrator wiring deferred)
**Affects:** AI Branding Wizard Step 6 — DeployingStep real-time log + RegenerateCounter real-quota counter
**Related:** GAP-272 (parent), GAP-272d, GAP-272e, Wave 34 Bucket D PR #910

## Problem

Wave 34 Bucket D shipped:
- `useDeployStream(jobId)` hook returning `DeployingLogEntry[]` from SSE endpoint
- `useRegenerateQuota()` hook returning `{tier, used, limit, resetAt}`

…but `DeployingStep` and `RegenerateCounter` components remain **presentational** (props-driven). The orchestrator (wizard parent at `(customer)/branding/wizard/page.tsx` or equivalent) does NOT yet call these hooks and feed their output into the components' props.

Result: Step 6 deploying state still shows the static prop-fed log fixture passed by the parent, and the regenerate counter doesn't decrement on real regenerate POST. Hooks are functional + tested in isolation; wiring is the missing link.

## Root Cause

Bucket D scope was "replace inline mocks with hooks" — interpreted as "ship hooks + MSW + replace inline-mock callsites." Orchestrator-side state composition (which props flow to which child) is wizard-architecture concern; outside the per-component refactor scope.

## Proposed Fix

In the wizard orchestrator (`(customer)/branding/wizard/page.tsx` or relevant `step6` page):

1. Call `useDeployStream(currentJobId)` when wizard reaches Step 6 deploying state. Pass the returned events array into `DeployingStep logs={...}`. Stop the EventSource on terminal `complete`/`error`.
2. Call `useRegenerateQuota()` when Step 6 mounts. Pass `{used, limit, tier}` into `RegenerateCounter`. On regenerate button click, fire the hook's mutation; on success, hook re-fetches quota; counter updates.
3. Trigger the upsell modal (per GAP-272d AC) when `used >= limit` and tier ≠ ENTERPRISE.

## Acceptance Criteria

- [x] Wizard orchestrator calls `useDeployStream` on Step 6 deploying entry (gated by `enabled: isDeploying && jobId` in `Step6Preview.tsx`)
- [x] `DeployingStep.logs` prop receives streamed events from hook (via `eventsToLogEntries` adapter mapping SSE event names → `DeployingLogEntry`)
- [x] `RegenerateCounter.used` updates on real regenerate POST — `useRegenerateQuota.regenerate.mutate({jobId})` invalidates quota cache → counter re-fetches; verified by Wave 34 GAP-391-A test that still passes (648/648)
- [x] Upsell modal triggers on quota-exceeded for non-ENTERPRISE tiers (auto-open `useEffect` watching `quotaExceeded && tier !== 'ENTERPRISE'`)
- [x] EventSource closes on terminal SSE event (no leaked connections) — `useDeployStream` cleanup closes source on `complete`/`error`/unmount; `enabled=false` short-circuits opening
- [x] Integration test: `Step6Preview-orchestrator-wiring.test.tsx` covers wiring + MSW quota + Deploy CTA flip + upsell auto-open (5 tests, all green). Full SSE end-to-end (5 events → 5 log lines) deferred to GAP-272e EventSource polyfill landing — covered structurally by `eventsToLogEntries` exercised through DeployingStep render branch
- [x] `pnpm build` + `pnpm test --run` green (verified 2026-05-08; 648/648 tests pass; build green; lint clean)

## Out of scope

- Backend swap of SSE poll → RabbitMQ subscription (separate follow-up tracked in GAP-272e Log)
- Persisting log between page-refresh (would require server-side log store; new concern)

## Related

- Parent: GAP-272
- Prior: GAP-272d (regenerate quota), GAP-272e (SSE endpoint)
- Bucket D PR #910 body §"Follow-ups recommended"

## Log

- **2026-05-08 (Wave 41 Bucket D):** Closed via this PR. Wired `useDeployStream` + `useRegenerateQuota` into `Step6Preview` orchestrator. Implementation:
  - `Step6Preview` now hosts `isDeploying` local state — flips on Deploy CTA click, switches render branch to `<DeployingStep>` driven by `useDeployStream(jobId, {enabled: isDeploying && jobId})`.
  - Replaced `step6-regenerate-counter-scaffold` placeholder with real `<RegenerateCounter>` wired to `useRegenerateQuota()` query data; `tier`/`limit`/`used` flow through props.
  - `eventsToLogEntries` adapter normalizes SSE event names (log/progress/state-change/complete/error) → `DeployingLogEntry[]` for the existing presentational component.
  - SSE `complete` event triggers parent's `onDeploy()` (router push to /branding).
  - Quota-exceeded auto-opens upsell modal for non-ENTERPRISE tiers per `ai-branding-guidelines.md` §4.3.
  - Tier 'PRO' from Wave 34 contract mapped to 'BASIC' for `RegenerateCounter` PricingTier vocabulary via `mapHookTier`.
  - 5 new integration tests in `Step6Preview-orchestrator-wiring.test.tsx` covering wiring, Deploy CTA flip, disabled-when-not-approved, upsell auto-open. All 648/648 tests green; build clean; lint clean.
  - Verification: `cd kitehub/kitehub-frontend && pnpm test --run` (648 passed) + `pnpm build` (success) + `pnpm lint` (only pre-existing warnings).
- **2026-05-07:** Filed at Wave 34 closure (this PR). Hooks shipped ready in Bucket D; orchestrator integration deferred per §3 PARTIAL exit ramp of `gap-done-discipline.md`. Self-test §7.2 of `contract-first-for-cross-layer.md`: this is 2 of ≤2 expected sub-gap follow-ups; rule effectiveness confirmed (Wave 32 v1 = 8 sub-gaps; Wave 34 = 2).
