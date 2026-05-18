# GAP-272e: SSE deploying log streaming endpoint for AI Branding Wizard Step 6

**Status:** 🟡 PARTIAL 2026-05-07 — SSE endpoint + hook shipped (Wave 34 Bucket A PR #907 + Bucket D PR #910); orchestrator wiring of `useDeployStream` events → `DeployingStep` props deferred to GAP-272o; underlying transport is 2s server-poll (RabbitMQ subscription deferred follow-up)
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

- [x] Backend endpoint streams events — `DeployStreamController` `text/event-stream` with event types `log` / `progress` / `state-change` / `complete` / `error` / `heartbeat` (Bucket A PR #907). **Spec deviation:** generic event names instead of literal `deploy-step` — agreed at runtime to match contract pattern across event types
- [x] `useDeployStream` EventSource hook shipped (Bucket D PR #910) — replaces inline mock SSE log
- [ ] **Deferred GAP-272o:** orchestrator-side wiring of `useDeployStream` events into `DeployingStep` `logs` prop (presentational component intact; downstream wiring is wizard-orchestrator scope, separate concern from hook+endpoint contract)
- [ ] **Deferred follow-up:** swap 2s server-poll backing for RabbitMQ `branding.deploy.*` queue subscriber (queue not wired in repo; SSE shape stable across swap — emit surface unchanged when wired)
- [x] api-contract.md entry — Bucket 0 PR #905 documents SSE schema + event types

## Log

- **2026-05-07:** Wave 34 Bucket A (PR #907) shipped SSE endpoint via `SseEmitter` + 2s poll backing. Bucket D wired `useDeployStream` hook. `DeployingStep` remains presentational — orchestrator wiring tracked GAP-272o. RabbitMQ queue subscription (intended source) absent; follow-up: when `branding.deploy.*` queue lands, swap poll for subscription, no contract change needed.

## Related

- GAP-272 (parent track 2 port)
- Wave 32 rework Bucket D (PR #890) — Step 6 sub-states scaffolding
- `ai-branding-guidelines.md` §3.3 — heavy tasks async via RabbitMQ
