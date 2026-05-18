# GAP-201: Tenant Off-boarding Runbook

**Status:** 🟡 PARTIAL (Phase 1 — design DONE 2026-04-20; Phase 2 — implementation pending)
**Priority:** 🟠 P1 (meta tier — churn-journey coverage)
**Domain:** Meta / Operations / Compliance / SaaS
**Found:** 2026-04-20 (simulation-action-1 Part C — All × Churn × Meta)
**Wave:** Wave 8b (meta)
**Affects:** Any tenant cancelling subscription, data export compliance, off-boarding DX

## Problem

Churn-journey is uncovered. Existing related gaps handle fragments:
- GAP-073 GDPR deletion AI assets — asset-specific
- GAP-184 data retention + deletion policy — retention rule, no runbook
- GAP-034 branding export pack — branding-only

Missing end-to-end off-boarding runbook:
- No canonical "how to cancel" flow documented for user
- No grace-period policy (how long after cancel before purge?)
- No final backup snapshot guarantee
- No data-export bundle contract (what formats, what scope, what SLA)
- No staff runbook for responding to cancellation tickets
- No "right-to-be-forgotten" UX/API endpoint (simulation Part C partial flag)

## Context

Discovered via 3-axis matrix simulation (All × Churn × Meta, simulation-action-1 Part C). Also flagged as partial gap in (All × Post-churn × Compliance).

Existing partial coverage — these gaps own their fragments; this gap stitches them into an end-to-end runbook + fills missing pieces:
- **GAP-073 GDPR deletion AI assets** — 🟢 DONE, but deferred: MinIO streaming export, @Scheduled expiry job, pseudonymization executor. GAP-201 can consume these as sub-tasks.
- **GAP-024 asset lifecycle + storage cleanup** — 🟡 OPEN, tangential (MinIO tiering, orphan cleanup) but relevant to "final backup + purge" step.
- **GAP-184 data retention + deletion policy** — retention rule only, no UX/runbook.
- **GAP-185 billing terms VAT/TCT** — invoice retention (10y tax law) conflicts with purge; runbook must resolve.
- **GAP-034 branding export pack** — branding-only export; GAP-201 wraps with broader data bundle.

## Proposed Fix

1. **Runbook doc** — `documents/05-guides/tenant-lifecycle/tenant-off-boarding-runbook.md`
   - User-facing cancel flow (self-service UI steps)
   - Staff-facing response procedure (support ticket → confirm → schedule purge)
   - Grace period (recommend 30d keep-alive read-only, 60d cold backup, 90d final purge)
   - Data-export bundle spec (formats: JSON + XLSX + PDF style guide, delivery: signed URL)
   - "Right-to-be-forgotten" API endpoint spec
2. **State transitions** (tie into InstanceLifecycleService)
   - `PAID_ACTIVE → CANCEL_REQUESTED → CANCEL_GRACE → ARCHIVED → PURGED`
3. **Legal alignment** — cross-check GAP-184 retention + GAP-182 privacy policy
4. **Export pack reuse** — build on GAP-034 branding export as sub-component
5. **Metrics** — off-boarding funnel (cancel-intent → confirm → export → purge rates)

## Acceptance Criteria

### Phase 1 — Design (DONE 2026-04-20)
- [x] Runbook doc with 6 sections: user-flow, staff-flow, grace-periods, export-bundle-spec, right-to-be-forgotten-api, metrics → `documents/05-guides/tenant-lifecycle/tenant-off-boarding-runbook.md`
- [x] State machine drafted (`PAID_ACTIVE → CANCEL_REQUESTED → CANCEL_GRACE_ACTIVE → CANCEL_GRACE_READONLY → ARCHIVED → PURGED`; RTBF fast-track variant)
- [x] Export-bundle contract: format, scope, SLA, delivery, expiry (api-contract.md §Export Bundle Specification)
- [x] Right-to-be-forgotten endpoint designed (`POST /off-boarding/rtbf` + `/rtbf/confirm` with 6-digit token, 15m TTL)
- [x] Legal review hook referenced (GAP-174 — in Related)
- [x] 3-layer docs under `documents/01-business/kitehub/off-boarding/` (rules.md, use-cases.md, api-contract.md)

### Phase 2 — Implementation (deferred)
- [ ] `OffBoardingController` + `OffBoardingService` + `PurgeScheduler` wired in kitehub-subscription
- [ ] MinIO streaming export (GAP-073 deferred item)
- [ ] `@Scheduled` grace-period expiry job (GAP-073 deferred item)
- [ ] Pseudonymization executor (GAP-073 deferred item)
- [ ] Migration: `offboarding_request` table + `off_boarding_phase` column
- [ ] Contract tests `OffBoardingApiContractTest.java`
- [ ] Email templates (cancel, bundle-ready, undo, RTBF, archive, purge)
- [ ] Subdomain 180d quarantine in DomainRegistryService

## Related

- simulation-action-1-2026-04-20.md Part C (All × Churn × Meta + All × Post-churn × Compliance)
- GAP-034 branding export pack
- GAP-073 GDPR deletion AI assets
- GAP-184 data retention + deletion policy
- GAP-182 privacy policy
- GAP-192 trial→paid migration (sibling lifecycle concern)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P1)

## Log

- 2026-04-20 — Created from simulation Part C.
- 2026-04-20 — Phase 1 closed (Wave 8b-F): 3-layer business docs + runbook landed. State machine, export bundle spec, RTBF endpoint, retention conflict matrix (OFF-08 tax 7y pseudonymization), metrics funnel all drafted. Phase 2 (implementation) scoped as follow-up; consumes GAP-073 deferred items (MinIO streaming export, `@Scheduled` expiry, pseudonymization executor). Files: `documents/05-guides/tenant-lifecycle/tenant-off-boarding-runbook.md`, `documents/01-business/kitehub/off-boarding/{rules,use-cases,api-contract}.md`.
