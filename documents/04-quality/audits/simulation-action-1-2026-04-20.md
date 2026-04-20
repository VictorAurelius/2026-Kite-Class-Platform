---
title: Simulation Audit — action-1 reorganization + plan coverage
date: 2026-04-20
skill: simulation-gap-finder
scope: (A) gaps derived from action-1 themes, (B) master plan coverage vs filed gaps
inputs:
  - documents/action-1.md (reorganized 2026-04-20)
  - documents/04-quality/gaps/ (167 gap files)
  - documents/03-planning/roadmap/master-plan-all-gaps-2026-04-20.md
---

# Simulation Report — action-1 + Master Plan Coverage

## Part A — New Gaps from action-1 Themes

Produced §15.A–§15.J in `action-1.md`. Summary:

| ID proposed | Title | Priority tier | Axis coverage |
|-------------|-------|---------------|---------------|
| GAP-190 | KiteHub SEO + Marketing Site | Business-Logic P1 | Discover × All-Tenants × UX |
| GAP-191 | Domain Registration & Instance DNS | Business-Logic P1 | Onboard × All-Tenants × Infra |
| GAP-192 | Trial → Paid Zero-Downtime Migration | Business-Logic P0 | Renew × Trial-User × Business |
| GAP-193 | Session Orchestration + Start-Session Skill | Meta P1 | — (meta) |
| GAP-194 | Bash/Python Script Compliance (shellcheck/ruff) | Meta P1 | — (meta) |
| GAP-195 | Starter-Kit Bulk Retro-Sync | Meta P2 | — (meta) |
| GAP-196 | 9router Tool Evaluation | Meta P2 | — (meta) |
| GAP-197 | Attendance Calendar-Mode UI | Feature P2 | Operate × Teacher × UX |
| GAP-198 | FE↔BE Decoupled Mock Contract Tests | Meta P2 | — (meta) |
| GAP-199 | Rework Audit for Context-Degraded PRs | Meta P1 | — (meta retro) |

**Count:** 3 P0/P1 Business-Logic + 5 Meta + 2 Feature = **10 new candidates**.

---

## Part B — Master Plan Coverage Audit

### Method

Cross-reference GAP IDs mentioned in `master-plan-all-gaps-2026-04-20.md` against GAP files under `documents/04-quality/gaps/`. Filter to OPEN / IN_PROGRESS gaps — closed gaps don't need to be in the forward plan.

### Findings

**11 OPEN/IN_PROGRESS gaps are NOT in master plan — plan coverage bug:**

| Gap | Title | Status | Proposed wave |
|-----|-------|--------|---------------|
| GAP-033 | Branding version history + rollback | 🟡 IN_PROGRESS | Wave 11 (kitehub enhance) |
| GAP-043 | Performance cache stampede protection | 🔵 OPEN | Wave 9 (post-audit performance) |
| GAP-052 | Parent portal (completion) | 🟡 IN_PROGRESS | Wave 10 (kiteclass enhance) |
| GAP-106 | Branding routing config keys missing | 🔵 OPEN | Wave 9 (business-logic cleanup) |
| GAP-109 | Bulk import rules.md documentation | 🔵 OPEN | Wave 9 (business-logic cleanup) |
| GAP-132 | `@EnableCaching` missing kitehub services | 🔵 OPEN | Wave 9 (performance) |
| GAP-134 | JOIN FETCH / @EntityGraph near-absent | 🔵 OPEN | Wave 9 (performance) |
| GAP-135 | API p95 latency SLOs undocumented | 🔵 OPEN | Wave 9 (performance + docs) |
| GAP-146 | External HTTP timeouts remainder | 🔵 OPEN | Wave 9 (resilience) |
| GAP-147 | Kitehub-admin bean conflict pre-existing | 🔵 OPEN | Wave 9 (hotfix) |
| GAP-148 | BR-QUEUE-015/018 dead CB config | 🔵 OPEN | Wave 9 (business-logic cleanup) |

**Impact:** These 11 gaps risk being forgotten since they're not scheduled. Several are P0 dependencies for GA (perf SLO doc, cache stampede).

### Proposed remediation

Update `master-plan-all-gaps-2026-04-20.md` to add a **Wave 9 "Audit-Followup Cluster"** that includes the 8 performance / business-logic / resilience gaps above. GAP-033 + GAP-052 go to Wave 10/11 feature enhancement waves.

---

## Part C — 3-Axis Simulation (System-Wide)

Standard simulation: Personas × Journey Stages × Categories. Only report cells where **no existing gap** covers the combination.

### Personas (from `documents/00-brd/personas-catalog.md`)

| Tier | Persona |
|------|---------|
| 1 | P1 Solo Teacher, P2 Small Center, P3 Mid Center, P5 K-12 School |
| 1 | P-Student, P-Parent, P-Teacher-Employee, P-Admin-Staff |
| 2 | P4 University, P6 Corporate L&D, P7 Online-only Creator |

### Journey Stages

Discover → Trial → Onboard → Operate → Renew → Churn → Post-churn

### Categories

Business-logic, Meta (skill/rule/workflow), Infra, Compliance, UX, Integration

### Uncovered cells discovered (high-signal only)

| Cell | Status | Notes |
|------|--------|-------|
| **P5 (K-12) × Onboard × Integration** | 🔵 NEW | School MIS/SMS integration not in any gap (imports from existing school system). Proposed: GAP-200. |
| **P-Parent × Operate × Compliance** | 🟡 partial | Parent consent flows for minor student data partial via GAP-186; no PDPL-specific guardian-revocation UX. |
| **P6 (Corporate L&D) × Discover × Business** | 🔵 NEW | No corporate demo flow, procurement workflow, SSO integration. Deferred (Tier 2). |
| **P7 (Online Creator) × Renew × Business** | 🔵 NEW | Revenue-share / marketplace model not designed. Deferred (Tier 2). |
| **All × Churn × Meta** | 🔵 NEW | No off-boarding checklist, no data-export-on-churn runbook. Proposed: GAP-201. |
| **All × Post-churn × Compliance** | 🟡 partial | GAP-184 retention covers storage; no formal "right-to-be-forgotten" UX/API endpoint for tenant. |
| **P-Admin-Staff × Operate × UX** | 🔵 NEW | Admin-staff daily ops UX (non-branding) unspecified — only GAP-040 impersonation covers debugging. |

**Additional candidates (Tier 1 focus):**

- **GAP-200** — School MIS/SMS integration spec (import student/class lists from existing Vietnamese school systems: VNEDU, SMAS, Base.vn)
- **GAP-201** — Tenant off-boarding runbook (data export, subscription cancellation UX, grace period, final backup)

---

## Part D — Plan Gaps (Missing from Master Plan)

Beyond the 11 unscheduled OPEN gaps (Part B), the master plan also lacks:

1. **Wave alignment with meta-gap-priority** — Wave sequencing currently ignores the new Business-Logic tier (added 2026-04-20). Waves 9-12 should be re-ordered to front-load Business-Logic-P0 before Feature-P0.
2. **GAP-176 (UI UX Pro Max integration)** — filed but no wave assignment.
3. **Epic 14 Quality Governance** — 6 meta gaps (GAP-170..175) filed but wave assignment unclear in master plan (only some appear).
4. **Audit baselines refresh cadence** — master plan doesn't schedule next refresh for quality-audit /100 (last: 77/100 on 2026-04-19), business-logic (72/100 on 2026-04-20), performance (64/100), ops-readiness (49/100).

---

## Summary

| Category | Count | Action |
|----------|-------|--------|
| New gap candidates from action-1 | 10 | File GAP-190..199 in next session |
| New gap candidates from system simulation | 2 | File GAP-200..201 in next session |
| Open gaps missing from master plan | 11 | Update master plan — add Wave 9 Audit-Followup Cluster |
| Plan alignment issues | 4 | Re-order waves per new business-logic tier + schedule audit refresh cadence |

**Total new work items identified:** 12 gap candidates + 4 plan edits.

**Priority to file first (P0 Business-Logic):** GAP-192 (Trial→Paid zero-downtime).

**Priority to file next (P1 Business-Logic):** GAP-190 (SEO), GAP-191 (Domain), GAP-199 (Rework audit), GAP-193 (Session orchestration).

---

## Log

- **2026-04-20:** Simulation run post action-1 reorganization. 10 candidates from action-1, 2 candidates from 3-axis matrix, 11 unscheduled open gaps identified, 4 plan-alignment issues flagged.
