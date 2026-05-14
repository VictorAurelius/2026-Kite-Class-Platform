# GAP-532: Multi-tenant tenant-switch flow §2.7 coverage gap

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend + Backend
**Found:** 2026-05-14 (Wave 76 — Phase 1 BETA persona audit)
**Affects:** P3 Education Manager (manages multiple centers) — first-hit persona; P5 K-12 also affected long-term
**Phase:** phase-1-beta (P3 falls Phase 1.5 paid but verify Phase 1 path)

## Problem

Phase 1 BETA persona audit surfaced rằng **multi-tenant tenant-switch flow** per `pre-handoff-self-test-completeness.md` §2.7 không được cover bởi bất kỳ gap nào hiện tại. P3 Manager persona manages multiple centers; chuyển tenant context cần đảm bảo:

1. Login as user-with-N-tenants returns tenant picker (vs single-tenant skip picker)
2. Picker selection issues new JWT scoped to chosen tenant
3. Data isolation verified (no cross-tenant leak)
4. Switching tenants doesn't carry stale cache
5. URL reflects tenant context (`/t/<slug>/...` or header-based)
6. Logout clears all tenant tokens

**Symptom:** Existing tenant infrastructure supports multi-tenant (DB schema + auth + gateway routing) but USER FLOW for switching không tested OR documented per `pre-handoff-self-test-completeness.md` §2.7 checklist.

## Root Cause

Wave 72 OWASP A07 hardening + Wave 73-76 governance focused on single-tenant flow (P1+P2 beta). P3 Manager persona (`documents/00-brd/` if exists, OR `release-1-plan-2026.md` §Phase 1.5) requires multi-tenant flow.

`pre-handoff-self-test-completeness.md` §2.7 introduced Wave 72b Bucket E as PROACTIVE class coverage — no incident driven. Persona audit confirms class IS real production concern.

## Proposed Fix

### Phase 1 — Coverage audit (audit-only, Wave 77 candidate)

1. Empirical test: synthetic P3 manager user với 2-3 tenants
2. Walk §2.7 checklist (a)-(f)
3. Document findings per checklist row
4. Identify missing infrastructure (UI tenant picker / JWT swap / cache invalidation)
5. If missing → file P1 subset gaps (UI / BE / cache each)

### Phase 2 — Implementation (Phase 1.5 paid scope)

P3 Manager is **Phase 1.5 paid persona** per release plan — Phase 1 BETA P1+P2 don't strictly need this. Phase 1 BETA may close as PARTIAL với recommendation "P3 onboarding deferred to Phase 1.5 paid".

## Acceptance Criteria

- [ ] Empirical §2.7 (a)-(f) checklist walkthrough
- [ ] Audit artifact `documents/04-quality/audits/persona/2026-XX-XX-multi-tenant-switch-coverage.md`
- [ ] Missing infrastructure inventoried (UI / BE / cache)
- [ ] Decision: ship Phase 1 BETA P3-included OR defer P3 to Phase 1.5
- [ ] Sub-gaps filed per missing component (if any)

## Related

- Phase 1 audit NEW-003: `2026-05-14-phase-1-beta-blockers-re-audit-persona.md`
- Rule: `pre-handoff-self-test-completeness.md` §2.7 Multi-tenant tenant-switch flow
- Release plan: `documents/03-planning/roadmap/release-1-plan-2026.md` §Phase 1.5 P3 scope
- BRD personas: `documents/00-brd/personas/` (if exists)

## Log

- **2026-05-14:** Gap filed Wave 76 Bucket F closure from Phase 1 BETA persona audit (NEW-003 P1). Multi-tenant tenant-switch class chỉ surface via persona simulation (P3 Manager), không inside-out audit naturally find. Validates outside-in-coverage-trigger.md force-multiplier. P3 Manager is Phase 1.5 paid scope — Phase 1 BETA can close as PARTIAL với defer recommendation, but coverage class must be verified before any P3 user signs up.
