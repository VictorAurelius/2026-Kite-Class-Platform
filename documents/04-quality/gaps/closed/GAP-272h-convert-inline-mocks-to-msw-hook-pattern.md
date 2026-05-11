# GAP-272h: Convert Wave 32 inline mocks → MSW handlers + hook abstraction

**Status:** 🟢 DONE 2026-05-07 (Wave 34 Bucket D — PR #910)
**Priority:** 🟠 P1 (technical debt — pattern miss caught at Wave 32 closure)
**Domain:** Frontend (kitehub-frontend) — refactor only, no behavior change
**Found:** 2026-05-07 (Wave 32 REWORK closure — user-flagged miss)
**Affects:** Bucket A (`WelcomeStep.tsx`) + Bucket C (`Step6Preview.tsx`, `TemplateGrid.tsx`)
**Related:** GAP-272 (parent — Track 2 port for ai-branding-wizard v2)

## Problem

Wave 32 REWORK Bucket A + C shipped với **inline hardcoded mocks** trong production component code thay vì project's standard MSW handler + hook abstraction pattern. User-flagged miss 2026-05-07 sau closure audit:

> "việc code UI trước thì có dùng mock như tiêu chuẩn mock của dự án không?"

State-check 2026-05-07 (post-rework merge):

### Bucket A — WelcomeStep.tsx (PR #887)
- `MOCK_TAKEN_SLUGS = new Set(['toan-master', 'hoc-vien-abc', 'trung-tam-anh-ngu'])` hardcoded ở module-level
- `checkSlugStub(slug, signal)` deterministic client function
- `// TODO(GAP-272i)` comment ở định nghĩa
- **Issue**: khi backend slug-availability endpoint ship (GAP-272i), phải edit `WelcomeStep.tsx` xóa MOCK constants + thay logic. Project standard sẽ chỉ cần update MSW handler.

### Bucket C — Step6Preview.tsx (PR #888)
- `data:text/html,...` URI sentinel cho iframe preview
- `TEMPLATE_TO_COLORS` map hardcoded ở module-level cho ThemePreview brand colors
- `// TODO(GAP-272j)` + `// TODO(GAP-272k)` comments
- **Issue**: khi backend preview render endpoint ship + colors API extend, phải edit `Step6Preview.tsx` cleanup. Standard pattern: hook `useBrandingPreview(jobId)` returns `{ previewUrl, colors }` → component KHÔNG biết về mock.

### Bucket C — TemplateGrid.tsx (PR #888)
- 6 hardcoded templates **inline** trong component (NOT loaded from API)
- Template filtering by audience+tone done client-side over hardcoded array
- **Issue**: khi backend template list endpoint ship, FE refactor required to call hook `useTemplateList({ audience, tone })`.

### Bucket D — clean (reference pattern)
- `LifecycleInline.tsx` calls `useInstanceLifecycle(instanceId)` hook
- `_lifecycle-mock.ts` separate file with typed mock service
- Backend swap = update hook implementation only — NO component changes
- ✅ This IS the project-standard pattern — A and C should match.

## Project mock standard (per CLAUDE.md + memory)

1. **MSW handlers** at `kitehub-frontend/src/__mocks__/handlers.ts` (or `tests/mocks/`) for HTTP layer mocking
2. **Service abstraction (hook layer)** — components import hooks (e.g., `useSlugAvailability(slug)`), hook implementation calls real endpoint OR is replaced via `vi.mock()` in tests
3. **Mock data files** separate from production — e.g., `_mock-templates.ts` co-located but explicitly marked as mock

References:
- `feedback_screenshot_mock_data.md` (memory) — "Capture cần MSW mock data"
- Bucket D's `_lifecycle-mock.ts` + `useInstanceLifecycle` (Wave 32 REWORK reference pattern)

## Root Cause

Wave 32 v1 plan + rework brief did NOT explicitly mandate MSW + hook abstraction pattern. Agents defaulted to inline constants for "scaffold-only" justification. Pattern correctness was implicitly assumed but not enforced. Post-merge audit reveals A + C diverged.

## Proposed Fix

### A. Refactor Bucket A — slug availability
1. Create `kitehub-frontend/src/hooks/use-slug-availability.ts` — hook signature `(slug: string) => { status: SlugStatus, suggestions: string[] }` calling real endpoint OR returning deterministic stub during dev
2. MSW handler `kitehub-frontend/src/__mocks__/handlers/branding.ts` — POST `/api/v1/branding/slug-availability` returns conflict for predefined slugs
3. `WelcomeStep.tsx` imports `useSlugAvailability` only — remove `MOCK_TAKEN_SLUGS` + `checkSlugStub`
4. Tests: `vi.mock('@/hooks/use-slug-availability')`

### B. Refactor Bucket C — preview + colors + templates
1. Create `kitehub-frontend/src/hooks/use-branding-preview.ts` — returns `{ previewUrl, colors }` for jobId
2. Create `kitehub-frontend/src/hooks/use-template-list.ts` — returns filtered templates by `{audience, tone}` props
3. MSW handlers for both endpoints
4. `Step6Preview.tsx` + `TemplateGrid.tsx` import hooks only — remove inline constants
5. Tests: vi.mock the hooks

### C. Documentation
1. Add example pattern reference in `documents/05-guides/development/frontend-mocking-pattern.md`
2. Reference in `wave-pack-planner` SKILL update (post-Wave-32 retro meta-update)

## Acceptance Criteria

- [x] `WelcomeStep.tsx` no longer references `MOCK_TAKEN_SLUGS` / `checkSlugStub` — refactored to `useSlugAvailability` hook
- [x] `Step6Preview.tsx` no longer references `TEMPLATE_TO_COLORS` or hardcoded preview URI — refactored to `usePreviewBrandColors` + `usePreview` hooks
- [x] Hooks ship for slug/quota/preview/quality/lifecycle/SSE — 6 hook files in `components/branding/wizard/hooks/`
- [x] MSW handlers populate `src/test/msw/handlers/branding.ts` covering all 7 endpoints (happy + error variants); Bucket 0's setup.ts gating activates lifecycle hooks correctly
- [x] Test count parity preserved — 632/632 (was 615 → +17 new for hook smoke + integration)
- [x] Backend swap pattern proven — A/B/C endpoints landed first, Bucket D consumed via hooks with zero further component changes
- [x] All verification gates green — `tsc --noEmit` clean, `pnpm test --run` 632/632, `pnpm build` clean

## Log

- **2026-05-07:** Wave 34 Bucket D (PR #910) shipped 6 hooks + MSW handlers + 3 component refactors (WelcomeStep / Step6Preview / LifecycleInline). Used MSW (not `vi.mock`) per Bucket 0 contract — better isolation, server-resilience pattern. Existing `use-theme-generation.test.ts` `global.fetch = vi.fn()` conflicted với MSW activation; fix: re-assign in `beforeEach` (documented in test). 3 wizard tests required `QueryClientProvider` wrapper (refactored components use react-query). DeployingStep + RegenerateCounter remain presentational; orchestrator-side wiring tracked GAP-272o.

## Related

- GAP-272 (parent — track 2 port)
- GAP-272i (slug-availability backend) — consumed by hook from this gap
- GAP-272j (iframe preview render) — consumed by hook from this gap
- GAP-272k (live brand colors) — consumed by hook from this gap
- Wave 32 REWORK PRs #887 (Bucket A) + #888 (Bucket C) — sources of inline mocks
- Wave 32 REWORK PR #890 (Bucket D) — reference pattern (LifecycleInline + useInstanceLifecycle + _lifecycle-mock.ts)
- Memory: `feedback_screenshot_mock_data.md` (project MSW standard)
- Future memory: `feedback_fe_first_endpoint_proliferation.md` (lessons from Wave 32 REWORK)

## Effort estimate

~1-2 hours single agent. Pure refactor — no new endpoints, no new tests beyond `vi.mock` swap. Can be coordinator-applied if scope stays ≤30 LOC per file.
