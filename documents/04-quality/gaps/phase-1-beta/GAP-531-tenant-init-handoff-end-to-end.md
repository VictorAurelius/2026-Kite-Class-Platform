# GAP-531: Tenant init handoff post admin-approve walked end-to-end

**Status:** 🟡 PARTIAL (Wave 78 Bucket E — runbook shipped covering 6-step flow + 4 failure modes; live walkthrough deferred Plan 1 invite)
**Priority:** 🟠 P1
**Domain:** Backend + Frontend
**Found:** 2026-05-14 (Wave 76 — Phase 1 BETA persona audit)
**Affects:** P1 Trial Free, P2 Center Owner — first-time tenant provision flow
**Phase:** phase-1-beta

## Problem

Phase 1 BETA persona audit surfaced rằng **post-admin-approve tenant init handoff** chưa có end-to-end empirical walk-through. After admin approves a beta request, the flow includes:

1. Admin click "Approve" trên `/admin/beta-requests` (GAP-526 verified UI)
2. Backend triggers tenant provision (V35+ migrations, default config, branding stub)
3. Invite email sent với signup token (GAP-530 cover email side)
4. User clicks token link → signup form with token validation
5. Signup completes → tenant flag flipped beta=true
6. Tenant dashboard loads with default state

**Symptom:** Each step verified individually trong Wave 33 Bucket C (BetaAccessRequest flow) + Wave 73 (signup) + Wave 72 (admin role guard), but NOT walked end-to-end on production with real persona.

## Root Cause

Like email flow, post-approve handoff = multi-step state machine. Per `pre-handoff-self-test-completeness.md` §2.1 auth-gated user-flow gap checklist, EACH step needs verification:
- (a) Admin approve UI advances state (verified — GAP-526)
- (b) Backend provisioning succeeds (Flyway migrations apply, default tenant config)
- (c) Invite email sent to correct address với valid token (GAP-530 cover)
- (d) Token link validates client-side
- (e) Signup completes, tenant beta-flag flipped
- (f) Dashboard renders với expected default state (no errors, no missing data)

Steps (b), (d), (e), (f) chưa empirically tested end-to-end production-side.

## Proposed Fix

### Phase 1 — End-to-end walkthrough (Wave 77 candidate)

1. Run synthetic beta-request → admin approve → signup → dashboard cycle on production
2. Verify each step (b)-(f) produces expected state in DB + UI
3. Ship audit artifact documenting walkthrough
4. Cross-reference findings against `pre-handoff-self-test-completeness.md` §2.1 checklist

### Phase 2 — Automation

E2E Playwright spec covering full handoff (Wave 37 Bucket C `signup-with-claim-code.spec.ts` extends to cover post-handoff dashboard load).

## Acceptance Criteria

- [x] Runbook `documents/05-guides/operations/tenant-init-handoff-runbook.md` shipped — 6-step happy path + 4 failure modes + verify commands per bước
- [x] Cross-link to GAP-530 + GAP-526 trong runbook §Related
- [x] Runbook walks `pre-handoff-self-test-completeness.md` §2.1 (auth-gated) + §2.4 (admin-flow) verification per bước
- [ ] Synthetic walkthrough completed production-side — defer Plan 1 invite ship (real persona = empirical walkthrough)
- [ ] Each step (b)-(f) verified per §2.1 checklist — defer Plan 1 invite ship
- [ ] Audit artifact filed `documents/04-quality/audits/persona/2026-XX-XX-tenant-init-handoff-walkthrough.md` — defer post-walkthrough
- [ ] Any blockers discovered → file follow-up gap — defer post-walkthrough

## Related

- Phase 1 audit NEW-002: `2026-05-14-phase-1-beta-blockers-re-audit-persona.md`
- Sibling gaps: GAP-526 admin UI verify, GAP-530 email e2e, GAP-518 admin role match, GAP-372 beta invite mechanism
- Rule: `pre-handoff-self-test-completeness.md` §2.1 Auth-gated user-flow checklist

## Log

- **2026-05-14:** Gap filed Wave 76 Bucket F closure from Phase 1 BETA persona audit (NEW-002 P1). Each handoff step verified individually in prior waves but never walked end-to-end. Defer to Wave 77 after Plan 1 tightly-controlled handful invite ships (handful invite IS the empirical walkthrough — close gap post-invite).
- **2026-05-14 (Wave 78 Bucket E):** PARTIAL — runbook ship covering 6 bước + 4 failure modes + verify commands per bước. Live walkthrough vẫn pending Plan 1 invite (real persona = empirical walkthrough). Per `gap-done-discipline.md` §3 PARTIAL exit ramp — close DONE sau khi Plan 1 ship + audit artifact `persona/<date>-tenant-init-handoff-walkthrough.md` filed.
- **2026-05-21 (Wave 102.9 Bucket B):** PARTIAL 50% confirmed. State-check (per `audit-to-gap-pipeline.md` §2.8): canonical post-admin-approve entry point is `POST /api/v1/admin/beta-requests/{id}/approve` + signup token flow — NOT `POST /api/v1/tenants` (latter does not exist; `TenantInitController` not in codebase, only `TenantSlugNormalizer` helper). Runbook + 6-step flow + 4 failure modes already shipped. Live walkthrough remains blocked on GAP-612 (AWS account 906286017800 suspended) — agent cannot reach production. Evidence audit: `documents/04-quality/audits/persona-review/2026-05-21-wave-102.9-bucket-b-tenant-init-onboarding-state-check.md`. Per `pre-handoff-self-test-completeness.md` §5.4 override mechanism: blocker tracked via GAP-612 (no new follow-up gap required — pre-existing dependency).
