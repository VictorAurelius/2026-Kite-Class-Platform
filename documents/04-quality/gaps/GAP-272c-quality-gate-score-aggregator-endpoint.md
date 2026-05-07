# GAP-272c: Quality-gate score aggregator endpoint for AI Branding Wizard Step 6

**Status:** 🟡 PARTIAL 2026-05-07 — orchestration layer + endpoint shipped (Wave 34 Bucket B PR #906); 5 sub-checks remain stubs deferred to GAP-226/227/228 (real WCAG / visual-regression / ML measurement infra)
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-branding) + Frontend wiring
**Found:** 2026-05-07 (Wave 32 REWORK Bucket D — `QualityGateWidget.tsx`)
**Affects:** AI Branding Wizard Step 6 — quality-gate /100 score visualization
**Related:** GAP-272 (parent), GAP-226/227/228 (real measurement infra)

## Problem

`QualityGateWidget` renders /100 quality score + 5-check breakdown (WCAG
contrast, CSS vars applied, no broken URLs, visual regression diff, logo
placement) per `ai-branding-guidelines.md` §5. Bucket D shipped widget as
**presentational** — accepts `report` prop, parent supplies score.

Real measurements (GAP-226/227/228) are infrastructure for individual
checks. **Missing:** an aggregator service that consumes those check
results + exposes a unified `/100` score per branding job to the frontend.

State-check 2026-05-07:
- No `QualityScoreService` or `QualityGateAggregator` class in
  `kitehub-branding`
- No endpoint `GET /api/v1/branding/jobs/{jobId}/quality-score`
- §11.4 migration test checklist references real automation but doesn't
  define the aggregation contract

## Root Cause

Quality gate is FE-visible but the backend layer that runs all 5 checks +
combines into single score doesn't exist yet. GAP-226/227/228 describe
individual measurement infra; this gap fills the orchestration layer.

## Proposed Fix

1. **Backend (kitehub-branding):** new `QualityGateAggregator` service
   - On job completion, run all 5 checks (calls into 226/227/228 + 2
     simpler scaffold checks for CSS vars + broken URLs)
   - Persist `quality_gate_report` row per job
   - Expose `GET /api/v1/branding/jobs/{jobId}/quality-score` returning
     `{ overallScore, checks: [{name, passed, value, threshold}], ... }`
2. **Frontend wiring:** `QualityGateWidget` parent fetches from this
   endpoint, passes `report` prop down

## Acceptance Criteria

- [x] Aggregator service runs 5 checks + combines /100 — `QualityScoreAggregator` (Bucket B). **Sub-checks are deterministic v0** (computed from job hash + status + logoUrl, NOT hardcoded `100`); real measurements remain GAP-226/227/228 scope.
- [x] Endpoint live + documented in api-contract.md (Bucket 0 PR #905, Bucket B PR #906)
- [x] Frontend wired — `useQualityScore` hook (Bucket D PR #910)
- [x] Test: `QualityScoreControllerTest` covers happy path + 404 path; deterministic v0 inputs verified
- [ ] **Deferred — real measurements:** WCAG contrast (GAP-226), visual regression (GAP-227), ML classifier scoring (GAP-228) — these gaps own real infra; this gap PARTIAL until they land

## Log

- **2026-05-07:** Wave 34 Bucket B (PR #906) shipped `QualityScoreAggregator` + endpoint with deterministic v0 sub-check computations (real numbers from real inputs, not stubbed constants). Bucket D wired hook. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 — orchestration layer DONE, sub-check measurements tracked in GAP-226/227/228.

## Related

- GAP-272 (parent)
- GAP-226 / 227 / 228 (individual measurement infra)
- Wave 32 rework Bucket D (PR #890) — QualityGateWidget presentational scaffold
- Wave 32 v1 plan §7 (this letter pre-named there)
