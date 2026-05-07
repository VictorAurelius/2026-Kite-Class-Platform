# GAP-272c: Quality-gate score aggregator endpoint for AI Branding Wizard Step 6

**Status:** 🔵 OPEN
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

- [ ] Aggregator service runs 5 checks + combines /100
- [ ] Endpoint live + documented in api-contract.md
- [ ] Frontend wired to real endpoint
- [ ] Test: integration test happy path 95/100 + fail path 65/100

## Related

- GAP-272 (parent)
- GAP-226 / 227 / 228 (individual measurement infra)
- Wave 32 rework Bucket D (PR #890) — QualityGateWidget presentational scaffold
- Wave 32 v1 plan §7 (this letter pre-named there)
