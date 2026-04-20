# GAP-201: Tenant Off-boarding Runbook

**Status:** 🔵 OPEN
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

## Proposed Fix

1. **Runbook doc** — `documents/05-guides/tenant-off-boarding-runbook.md`
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

- [ ] Runbook doc with 6 sections: user-flow, staff-flow, grace-periods, export-bundle-spec, right-to-be-forgotten-api, metrics
- [ ] State machine updated with cancel + purge transitions
- [ ] Export-bundle contract: format, scope, SLA, delivery, expiry
- [ ] Right-to-be-forgotten endpoint designed (POST /tenants/{id}/purge with auth + confirmation)
- [ ] Legal review hook referenced (GAP-174)
- [ ] 3-layer docs under `documents/01-business/kitehub/off-boarding/`

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
