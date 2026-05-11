# GAP-084: CI Failure Triage Skill

**Status:** ✅ DONE
**Priority:** 🟠 P1
**Domain:** DevOps / CI-CD
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** All PRs hitting CI

## Problem

CI fail → dev waste time vì không biết: flaky test? Code lỗi? Infrastructure issue? Dependency conflict?

`/check-pr` skill monitors CI nhưng không classify failure type. Dev phải đọc raw logs → chậm và error-prone.

## Proposed Fix

1. Extend `/check-pr` skill hoặc tạo section mới: "CI Failure Classification"
   - **Infrastructure**: timeout, OOM, runner unavailable → retry
   - **Flaky test**: same test fails intermittently → mark `@Flaky`, skip, create fix ticket
   - **My code**: new test fails, compilation error → dev fixes
   - **Dependency**: version conflict, breaking upstream → investigate
2. Script `scripts/ci-triage.sh`:
   - Parse CI log → extract failed step + error message
   - Compare with known flaky patterns (maintain list)
   - Output: classification + suggested action
3. Flaky test registry: `documents/04-quality/flaky-tests.md` — track known flaky tests + status

## Acceptance Criteria

- [ ] CI failure logs parsed and classified automatically
- [ ] Flaky test registry exists and maintained
- [ ] Dev knows within 30s if failure is their code or infrastructure
