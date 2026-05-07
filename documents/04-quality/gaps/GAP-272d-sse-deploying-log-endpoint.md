# GAP-272d: SSE deploying log streaming endpoint for AI Branding Wizard Step 6

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-branding) + Frontend wiring
**Found:** 2026-05-07 (Wave 32 REWORK Bucket D — `DeployingStep.tsx`)
**Affects:** `kitehub-frontend` AI Branding Wizard Step 6 deploying state — real-time log streaming UX
**Related:** GAP-272 (parent — Track 2 port for ai-branding-wizard v2)

## Problem

Wave 32 Step 6 deploying state (`step6-deploying.html` kit screen) specifies
real-time SSE log streaming during deploy — user sees each step (initialize,
generate logo, generate banner, deploy, validate) as the backend executes them.

State-check 2026-05-07:
- `kitehub-frontend/src/lib/api/endpoints.ts`: NO SSE/EventSource endpoint
- `grep -rn "EventSource\|/branding.*stream" kitehub/kitehub-branding/src/main/java`: 0 results
- No `Sse*Controller` or `@GetMapping(produces = "text/event-stream")` exists

Result: `DeployingStep.tsx` accepts `logs` as a prop (parent owns transport).
Caller currently passes mock log lines via test fixtures. Real SSE wiring
flagged inline with `TODO(GAP-272d)`.

## Root Cause

Step 6 deploying UX is new in Direction C v2 — legacy 4-step wizard had no
real-time deploy log; just polled `GET /branding/jobs/:id` for terminal
state. Backend never built SSE infrastructure for branding deploy progress.

## Proposed Fix

1. **Backend (kitehub-branding):** new SSE endpoint
   `GET /api/v1/branding/jobs/{jobId}/deploy-stream` (text/event-stream)
   - Emits events as deploy steps complete (logo, colors, banner, etc.)
   - Reuses existing `BrandingJob` lifecycle events from RabbitMQ outbox
   - Heartbeat every 15s to keep connection alive
2. **Frontend wiring:** parent of `DeployingStep` opens EventSource on mount,
   appends incoming events to `logs` array, closes on terminal state.
3. **Auth:** SSE endpoint accepts JWT via query param `?token=...` (browser
   EventSource cannot set headers).

## Acceptance Criteria

- [ ] Backend endpoint streams events with `event: deploy-step` + JSON data
- [ ] Frontend `DeployingStep` consumer wires real EventSource, replaces
      mock log fixture
- [ ] Test: integration test verifies 5 expected events emitted on happy path
- [ ] Documentation: api-contract.md entry for the SSE endpoint

## Related

- GAP-272 (parent track 2 port)
- Wave 32 rework Bucket D (PR #890) — Step 6 sub-states scaffolding
- `ai-branding-guidelines.md` §3.3 — heavy tasks async via RabbitMQ
