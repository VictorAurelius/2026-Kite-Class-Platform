# GAP-293: Persona/tier-based feature/menu gating

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend nav + dashboard) + Backend (feature-flag service)
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** ALL Tier-1 personas — cross-cutting force multiplier

## Problem

P1 AC-FIN-005 explicit: "Settings/menu KHÔNG hiển thị 'Payroll / Teacher commission / Staff' cho FREE/PRO solo persona — feature gated by tier hoặc role."

State-check:
- `kiteclass-core/src/main/java/com/kiteclass/core/module/role/entity/Permission.java:18` lists categories: STUDENT, TEACHER, GRADE, **PAYROLL**, BRANDING, USER, ROLE
- `TeacherContractBuilder.java` exists (docx generator for staff contracts)
- `Permission` is granular, but **no UI logic hides menus per persona/tier** — a solo teacher with `tier = FREE` sees the same nav as a school admin

Result: solo persona overwhelmed by enterprise menu items (Payroll, Teacher commission, MOET report card, Academic year/semester) → cognitive overhead → violates P1 AC §0 critical concern #1 (≤30 min onboarding).

This is a **meta-feature** — fixing it once unblocks AC-FIN-005, AC-OPS-004 (gradebook complexity), AC-ONBOARD-004 (tour relevance), and equivalents in P2/P3/P5 reviews. **Per `meta-gap-priority.md` §3 — gated as Meta-tier P1 boost.**

## Root Cause

V1 design assumed all features visible by default; feature flags / persona routing not in scope. Tier definitions (`PricingTier.java`) include limits (max students/teachers/storage) but no feature visibility flags.

## Proposed Fix

1. Define `FeatureFlag` enum: `PAYROLL`, `MULTI_TENANT_BRANDING`, `K12_ACADEMIC_YEAR`, `MOET_REPORT_CARD`, `STAFF_MANAGEMENT`, `RUBRIC_GRADING`, etc.
2. Define `PersonaProfile` (read from instance config): SOLO / SMALL_CENTER / MEDIUM_CENTER / K12_SCHOOL.
3. Mapping matrix: `PersonaProfile × FeatureFlag → boolean` (default visible/hidden).
4. Frontend `useFeature(flag: FeatureFlag)` hook reads matrix + tier from auth profile.
5. Wrap nav menu items + dashboard tiles in `<Feature flag={...}>` HOC.
6. Settings page allows admin to override defaults (advanced — defer to v2).
7. Backend: `/api/v1/instance/features` endpoint returning resolved flags.

## Acceptance Criteria

- [ ] `FeatureFlag` enum + matrix defined per persona × tier
- [ ] `PersonaProfile` field on Instance entity (defaults to SOLO if unset)
- [ ] `useFeature` hook + `<Feature>` HOC in frontend
- [ ] All nav items + dashboard tiles wrapped — verified by snapshot tests per persona
- [ ] Settings menu hides Payroll/Staff for SOLO persona
- [ ] Backend endpoint + caching
- [ ] AC-FIN-005 PASS in re-test

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §3 + Critical Finding 4
- AC: AC-FIN-005, AC-OPS-004, AC-ONBOARD-004 (indirect)
- Existing: GAP-053 (academic year — exact use-case for hide-from-solo)
- Existing: GAP-055 (MOET report card — same)
- Existing: GAP-057 (payroll/commission — same)
- Foundation for: GAP-289 (in-app tour — needs persona context)
- Meta priority per: `meta-gap-priority.md` §3

## Log

- 2026-05-04 — Created from Wave 17 Bucket A. State-check: Permission.java has PAYROLL category but no UI gating logic; PricingTier.java has limits but no feature flags.
