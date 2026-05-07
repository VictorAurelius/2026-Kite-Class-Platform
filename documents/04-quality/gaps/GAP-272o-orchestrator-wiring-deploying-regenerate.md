# GAP-272o: Wizard orchestrator wires `useDeployStream` + `useRegenerateQuota` into `DeployingStep` + `RegenerateCounter`

**Status:** 🔵 OPEN
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

- [ ] Wizard orchestrator calls `useDeployStream` on Step 6 deploying entry
- [ ] `DeployingStep.logs` prop receives streamed events from hook
- [ ] `RegenerateCounter.used` updates on real regenerate POST
- [ ] Upsell modal triggers on quota-exceeded for non-ENTERPRISE tiers
- [ ] EventSource closes on terminal SSE event (no leaked connections)
- [ ] Integration test: simulate Step 6 mount → MSW emits 5 SSE events → DeployingStep renders 5 log lines
- [ ] `pnpm build` + `pnpm test --run` green

## Out of scope

- Backend swap of SSE poll → RabbitMQ subscription (separate follow-up tracked in GAP-272e Log)
- Persisting log between page-refresh (would require server-side log store; new concern)

## Related

- Parent: GAP-272
- Prior: GAP-272d (regenerate quota), GAP-272e (SSE endpoint)
- Bucket D PR #910 body §"Follow-ups recommended"

## Log

- **2026-05-07:** Filed at Wave 34 closure (this PR). Hooks shipped ready in Bucket D; orchestrator integration deferred per §3 PARTIAL exit ramp of `gap-done-discipline.md`. Self-test §7.2 of `contract-first-for-cross-layer.md`: this is 2 of ≤2 expected sub-gap follow-ups; rule effectiveness confirmed (Wave 32 v1 = 8 sub-gaps; Wave 34 = 2).
