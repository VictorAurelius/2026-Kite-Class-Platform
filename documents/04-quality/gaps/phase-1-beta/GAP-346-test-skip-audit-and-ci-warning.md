# GAP-346: Test Skip Audit — kiteclass-frontend 26.7% Skip Ratio + CI Warning Mechanism

**Status:** 🟡 PARTIAL — CI WARN mechanism + silent-skip docs shipped (Wave local-doable-12 Bucket C); bulk un-skip deferred → GAP-873
**Priority:** 🟠 P1 — quality debt with hidden coverage erosion
**Domain:** Frontend Testing + CI Quality Gates
**Detected:** 2026-05-04 (user flag during Wave 18a CI review — 11/11 skip in `teacher-edit.integration.test.tsx`)
**Affects:** kiteclass-frontend test suite quality + CI signal reliability

---

## Problem

User flagged surprise during Wave 18a Bucket A CI: `teacher-edit.integration.test.tsx (11 tests | 11 skipped)`. Comprehensive audit reveals **the entire kiteclass-frontend test suite has 26.7% skip ratio** — far above industry healthy threshold (<5%) and degrades CI signal-to-noise.

Skipped tests pass CI without executing → **false confidence**. Teams reading "565 passed" trust coverage that doesn't exist for 206 tests.

## Current State (verified 2026-05-04 via `pnpm test --run`)

### Authoritative test counts

| Project | Test Files (passed/skipped/total) | Tests (passed/skipped/total) | Skip ratio |
|---------|-----------------------------------|------------------------------|------------|
| **kiteclass-frontend** | 57 / **14** / 71 | 565 / **206** / 771 | **26.7%** 🔴 |
| kitehub-frontend | 40 / 0 / 40 | 484 / 0 / 484 | 0% ✅ |
| All Java modules (`*Test.java`, `*IT.java`) | — | 0 actual @Disabled / @Ignore | 0% ✅ |
| Java sample-emitters (`*SampleEmitter.java`) | 3 utilities @Disabled by design | — | N/A (intentional) |

### Reason categorization (kiteclass-frontend, 79 grep occurrences)

Of 79 `.skip(` call-sites in source code:

| Reason | Occurrences | Severity |
|--------|:-----------:|:--------:|
| `Next.js 15 use(params) incompatible with RTL` (whole `describe.skip` blocks) | 7 files × ~15 tests each = **~100+ tests** | 🔴 HIGH — critical pages no integration test |
| `jsdom validation timing` | 11 | 🟡 MEDIUM |
| `Toast not rendered in jsdom` | 8 | 🟡 MEDIUM |
| `Radix Select requires browser APIs` | 6 | 🟡 MEDIUM |
| `flaky button selector` | 4 | 🟡 MEDIUM |
| `Form submission timing in jsdom` | 3 | 🟡 MEDIUM |
| `flaky in CI` | 2 | 🟠 LOW |
| `React Query retry complexity` | 1 | 🟠 LOW |
| **Silent skip — no `[SKIP: reason]` comment** | **~37** | 🔴 HIGH — undocumented tech debt |

**Key finding:** 47% of `.skip(` calls have NO inline reason comment. Silent skips concentrate in `use-auth.test.tsx` (6 silent), `use-classes.test.tsx` (3 silent), `use-teachers/students/courses.test.tsx` (1 each, all "should update successfully" mutations).

### Files with full `describe.skip` (entire suites disabled)

| File | Reason | Estimated tests skipped |
|------|--------|:----------------------:|
| `(dashboard)/teachers/[id]/edit/__tests__/teacher-edit.integration.test.tsx` | Next.js 15 `use(params)` | 11 |
| `(dashboard)/teachers/[id]/__tests__/teacher-detail.integration.test.tsx` | Next.js 15 `use(params)` | ~15 |
| `(dashboard)/teachers/__tests__/teachers-list.integration.test.tsx` | (no comment on describe.skip) | ~15 |
| `(dashboard)/courses/__tests__/courses-list.integration.test.tsx` | (no comment) | ~12 |
| `(dashboard)/students/__tests__/students-list.integration.test.tsx` | (no comment) | ~13 |
| `(dashboard)/courses/[id]/__tests__/course-detail.integration.test.tsx` | Next.js 15 `use(params)` | ~10 |
| `(dashboard)/students/[id]/__tests__/student-detail.integration.test.tsx` | Next.js 15 `use(params)` | ~10 |
| `(dashboard)/courses/[id]/edit/__tests__/course-edit.integration.test.tsx` | Next.js 15 `use(params)` | ~10 |
| `(dashboard)/students/[id]/edit/__tests__/student-edit.integration.test.tsx` | Next.js 15 `use(params)` | ~10 |
| `(dashboard)/classes/[id]/__tests__/class-detail.integration.test.tsx` | Next.js 15 `use(params)` | ~10 |
| `(dashboard)/classes/__tests__/classes-list.integration.test.tsx` | Radix UI Select / JSDOM | ~10 |
| `(dashboard)/teachers/new/__tests__/teachers-new.integration.test.tsx` | individual `it.skip` x8 | 8 |
| `(dashboard)/courses/new/__tests__/courses-new.integration.test.tsx` | individual `it.skip` x9 | 9 |
| `(dashboard)/courses/[id]/classes/new/__tests__/classes-new.integration.test.tsx` | individual `it.skip` x11 | 11 |

**Total: 14 files contributing ~145 of the 206 skipped tests.** Remaining ~60 = individual `it.skip` calls scattered.

### Coverage gap analysis

Critical user flows lacking integration test (because describe.skip):
- ❌ All entity detail pages (teacher/student/course/class) — view by ID
- ❌ All entity edit pages — edit/update flows
- ❌ All entity list pages — pagination, search, filtering
- ❌ All entity creation forms — validation + error handling
- ❌ Authentication flows (use-auth has 6 silent skips on login/logout/forgotPassword)
- ❌ All hook update mutations (use-teachers/students/courses/classes silent skip on `should update successfully`)

→ **Net coverage gap: CRUD operations for 4 main entities (Teacher/Student/Course/Class) have zero integration test coverage.** E2E (Playwright) is the only safety net.

## Root Cause Analysis

### Cause 1 — Next.js 15 migration (Q1 2026) broke RTL pattern

When project upgraded Next.js 14 → 15, `params` changed from sync object to `Promise<{}>`. Pages now use `const { id } = use(params)`. RTL's `render()` doesn't unwrap promises → tests crash. Pattern fix: mock `useParams` from `next/navigation` instead of relying on `params` prop. Not applied at upgrade time → tests skipped wholesale.

### Cause 2 — jsdom limitations vs production browser

jsdom doesn't fully implement:
- Form validation timing (HTML5 Constraint Validation API)
- Portal rendering (Radix UI dropdowns/selects need PointerCapture API)
- Toast portal injection from sonner

→ Tests punted to skip rather than: (a) mocking at appropriate layer, (b) moving to E2E.

### Cause 3 — Silent skips = no governance

47% of `.skip()` calls have no comment. Author intent lost. Reading code, can't tell if:
- Test broken temporarily and forgotten
- Behavior intentionally moved elsewhere (E2E?)
- Test was wrong (testing wrong thing)
- Just lazy WIP

No CI mechanism flags new silent skips → regression keeps growing.

### Cause 4 — CI doesn't surface skip count visibly

Current CI workflow (`frontend-ci.yml`):
- Runs `pnpm test --run`
- Reports pass/fail in PR check
- **Does not warn on skip count, skip ratio, or skip increase vs base**

→ "565 passed" looks healthy in PR badge; user sees real number only when manually reading vitest output mid-stream.

## Proposed Fix

### Phase 1 — CI warning mechanism (low-risk, immediate)

Add 3 layers of CI signal:

#### 1.1 Skip-budget script (`scripts/check-skip-budget.sh`)

```bash
# Hard cap: total skip count must not exceed budget
# Soft cap: ratio must not exceed 5%
KITECLASS_SKIP_BUDGET=210  # current 206 + small headroom
KITEHUB_SKIP_BUDGET=10     # current 0 + headroom
SKIP_RATIO_THRESHOLD=5     # %
```

CI fails if any budget exceeded. Forces conscious decision on every new `.skip()`.

#### 1.2 Mandatory inline reason comment (lint rule)

Custom ESLint rule (or simple grep-based check in CI):
```
# fail if any .skip() call lacks `// SKIP: <reason>` comment OR string literal containing "SKIP:"
```

Forces author to document WHY. Catches silent skips at PR time, not 6 months later.

#### 1.3 PR comment with skip diff

GitHub Action posts comment on PR:
```
Skip diff vs base:
- Added: 0 new skipped tests
- Removed: 2 (un-skipped, see commits abc123, def456)
- Total: 204 skipped (was 206)
```

Visible signal — adding skips becomes social cost, removing becomes credit.

### Phase 2 — Un-skip silent skips (medium-effort, weeks)

Pass through 37 silent-skip occurrences. For each:
- (a) Document reason (add inline `// SKIP: <reason>` comment) — accept skip with rationale
- (b) Fix and un-skip — preferred for mutation tests
- (c) Delete — if test is genuinely obsolete

Subject to PR review with quality reviewer.

### Phase 3 — Address Next.js 15 RTL pattern (large effort)

Deep fix for 7+ describe.skip files using `use(params)` pattern:

Option A (preferred): Mock `useParams` instead of pass-prop pattern
```typescript
vi.mock('next/navigation', () => ({
  useParams: vi.fn(() => ({ id: '1' })),
  // ... rest
}));
```
Refactor pages to call `useParams()` directly (or wrap `use(params)` behind a hook that's easier to mock).

Option B (fallback): Move whole-page integration tests to Playwright E2E (real browser unwraps `use()` correctly).

### Phase 4 — Address jsdom limitations (medium effort)

For jsdom timing / portal limitations:
- Form validation: test schema-level (`zod.safeParse`) in unit tests, render-level in E2E
- Toast assertions: mock `toast.success` / `toast.error` calls instead of asserting DOM render
- Radix Select: use `userEvent.selectOptions` shim or move to E2E

### Phase 5 — E2E coverage backfill

For tests genuinely impossible in unit layer, ensure Playwright E2E covers the gap. Audit current `e2e/*.spec.ts` for coverage of CRUD edit/detail/list flows.

## Acceptance Criteria

- [ ] **Phase 1:** `scripts/check-skip-budget.sh` shipped + wired into CI as required check
- [ ] **Phase 1:** Mandatory `[SKIP: reason]` comment lint rule shipped + wired into CI
- [ ] **Phase 1:** PR comment showing skip diff vs base shipped
- [ ] **Phase 2:** Document or fix 37 silent skips → 0 silent skips
- [ ] **Phase 3:** Next.js 15 `use(params)` pattern migrated → 7+ describe.skip files re-enabled
- [ ] **Phase 4:** jsdom limitation skips converted to schema-level unit OR E2E
- [ ] **Phase 5:** E2E coverage audit confirms CRUD flows covered
- [ ] **Final:** kiteclass-frontend skip ratio ≤5% (target ≤2%)
- [ ] **Final:** Skip count budget enforced in CI; new skips require PR-description justification

## Related

- **Detected via:** Wave 18a CI run for PR #760 (Bucket A) — user inspected verbose output and noticed `11 tests | 11 skipped` line
- **Cross-cuts:** GAP-194 (lefthook pre-commit gate — could include skip-budget check)
- **Cross-cuts:** GAP-122 (CI alert standards — could surface skip-ratio metric)
- **Memory:** None applicable; this is repo-specific quality debt
- **Wave 18a context:** All 3 agents shipped clean tests (including 9 new RecurrenceServiceTest, 13 PayrollServiceTest, 5+5 NotificationChannel/Preference) — Wave 18a did NOT add new skips. Skips are pre-existing legacy.

## Out-of-scope

- Changing test framework (vitest stays)
- Switching from jsdom to happy-dom (separate evaluation)
- Backend Java tests (already 0% skipped)
- E2E test framework choice (Playwright stays)

## Log

- **2026-06-02** — Wave local-doable-12 Bucket C: shipped Phase 1 CI WARN mechanism (bounded, safe) + documented silent skips. Status OPEN → 🟡 PARTIAL (completion ~50%).
  - **CI skip-ratio script:** `scripts/check-test-skip-ratio.sh` — counts `.skip(` call-sites per frontend project, computes call-site skip ratio = skip / (active + skip), 3 bands (>5% WARN / >15% HIGH-WARN / ≤5% OK). WARN-mode (always exit 0). `--self-test` (4 synthetic fixtures PASS) + `--json` modes. shellcheck clean (via npx). Real-repo: kiteclass-frontend skip=77 active=933 ratio=7% WARN; kitehub-frontend 0% OK. (Call-site ratio < vitest 26.7% test-level because `describe.skip` blocks under-count statically — documented in script header.)
  - **CI wired:** `quality-code.yml` job `test-skip-ratio` (WARN-mode + self-test step) + path triggers for `*.test.tsx`/`*.spec.tsx` in both frontends.
  - **Silent skips documented:** added `// [SKIP: <reason>]` markers to all 13 hook-level silent skips (use-auth 6 login-flow + use-teachers/students/courses update + use-classes update/generate/schedule). Comment-only — skip count unchanged (verified). useFeatureDetection already had inline SKIP marker.
  - **Bulk un-skip DEFERRED** → **GAP-873** (Next.js-15 `use(params)` describe.skip migration + jsdom limitation conversion — ~145 of 206 skipped tests in 14 describe.skip integration suites). Per `incident-to-rule-pipeline.md` WARN-first premature-rule guard; HARD STOP flip only after backlog clear.
  - AC Phase 1 partially met: skip-budget script (1.1) ✅ via skip-ratio WARN; `[SKIP: reason]` documentation (1.2) ✅ for hook silent skips; PR comment diff (1.3) deferred. Phases 2-5 (bulk un-skip) → GAP-873.
- **2026-05-04** — Filed during Wave 18a CI review. User flagged 11/11 skip on `teacher-edit.integration.test.tsx`. Comprehensive audit found 26.7% skip ratio in kiteclass-frontend, 0% in kitehub-frontend, 0% in Java. Phase 1 CI warning mechanism is force-multiplier — prevents recurrence + catches existing silent skips at PR time. Per `meta-gap-priority.md` Meta-tier (CI workflow) ranks above Feature-tier.
