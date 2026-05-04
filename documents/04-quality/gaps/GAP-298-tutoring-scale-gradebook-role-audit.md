# GAP-298: P2 tutoring-scale gradebook UX + role-based-financial-visibility audit

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Feature-P2 — verification of likely-existing features at this persona scale)
**Domain:** Frontend / Backend audit
**Found:** 2026-05-04 (P2 Small Center persona review round 1)
**Persona blocked:** P2 Small Tutoring Center
**Wave:** TBD (audit-style sub-task — can pair with GAP-297 fix wave)

## Problem

Two daily-ops ACs scored PARTIAL because schema/role infrastructure exists but persona-scale UX has not been verified end-to-end:

1. **AC-OPS-003 (P2 owner):** Per-class gradebook with weighted columns. `grade` module ships `GradeMapper`, `GradingSummaryResponse`, `GradeComponentResponse` — the schema seems right. But the user-facing flow "Add column 'Kiểm tra 15p' weight 20%, enter 15 scores, see auto-weighted-average" is unverified at the small-center scale (15 students × 3 weighted columns).
2. **AC-OPS-004 (P2 owner):** Role separation — hired teacher must see ONLY own classes, NOT other teachers' classes, NOT student contact details, NOT tuition amounts, NOT commission report. `role` module + `Permission.java` entity exist but a positive-case + negative-case audit at the tutoring-center role mix (Owner / Hired Teacher) hasn't been run.

This is verification work, not new build — likely a 1-2 day audit + small fixes for whatever doesn't pass.

## Root Cause

Features built generically; persona-scale walkthrough never executed for P2's role/scale mix. AC-style verification was created in `documents/00-brd/persona-criteria/P2-small-center.md` (2026-04-30) but no execution against actual code until this gap.

## Proposed Fix

Execute targeted verification:

1. **Gradebook UX walkthrough.** Spin local stack → log in as owner → create test class with 5 students → add 3 weighted columns (20/30/50) → enter scores → screenshot final report. Score against AC fail signals.
2. **Role audit.** Create a hired-teacher account → log in → enumerate every nav item + every API the FE calls → assert no financial endpoint reachable. File child gaps for any leak found.
3. **Document findings** in `documents/04-quality/audits/persona-verification/P2-2026-XX.md` (new sub-folder per persona-verification).
4. **Ship fixes** for any P0/P1 leaks found inline; defer P2/P3 to follow-up gaps.

## Acceptance Criteria

- [ ] Gradebook walkthrough screenshots committed to `documents/04-quality/audits/persona-verification/P2-2026-XX/`
- [ ] Role audit table — for each (owner|teacher) × (route|endpoint) cell — committed
- [ ] Any P0/P1 leak fixed in same wave; remaining items filed as new gaps
- [ ] AC-OPS-003 + AC-OPS-004 (P2 owner) re-scored in next P2 review

## Related

- Audit: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` §2
- Reference AC docs: `documents/00-brd/persona-criteria/P2-small-center.md` §2
- Pipeline: `.claude/rules/audit-to-gap-pipeline.md` — this gap follows the verification side of Step 2.5 state-check
