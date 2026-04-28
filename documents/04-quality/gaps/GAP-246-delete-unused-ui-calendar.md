# GAP-246: Delete unused `kiteclass-frontend/src/components/ui/calendar.tsx`

**Status:** 🔵 OPEN
**Priority:** 🟢 P3 (cleanup; ~1-line PR)
**Domain:** Frontend (KiteClass)
**Detected:** 2026-04-28 (Wave GAP-236 Agent B return finding)
**Affects:** `kiteclass-frontend` bundle hygiene; no runtime impact

## Problem

`kiteclass-frontend/src/components/ui/calendar.tsx` was the original react-day-picker wrapper. Wave GAP-236 Agent B (PR #600) replaced its use in `/attendance` pages with custom SVG-based attendance calendars. State-check 2026-04-28: `grep -rl "from.*ui/calendar" kiteclass-frontend/src` returns 0 hits — no consumers remain.

Keeping a dead component in the tree:
- Wastes maintainer attention during refactors / type-check passes
- Bundles in dev builds (production tree-shakes, but dev still parses)
- Adds noise to dependency audits (react-day-picker pulled in for nothing)

## Root Cause

Cleanup deferred during Wave GAP-236 — agents had hard-rule constraint not to touch out-of-bucket files; Agent B noted the component as unused but couldn't delete (per Hard Rule 4: surface findings, parent files follow-up).

## Proposed Fix

1. Delete `kiteclass-frontend/src/components/ui/calendar.tsx`
2. Verify no imports remain: `grep -rn "ui/calendar" kiteclass/kiteclass-frontend/src` → 0 hits
3. Optionally remove `react-day-picker` from `package.json` if no other consumer (verify with `grep -rn "react-day-picker" kiteclass/kiteclass-frontend/src`)
4. Build + test (should be no-op): `pnpm build && pnpm test`

## Acceptance Criteria

- [ ] `components/ui/calendar.tsx` deleted
- [ ] Build green; tests green; bundle budget unchanged or improved
- [ ] If `react-day-picker` had no other consumer, dependency removed from `package.json` (separate verification step)

## Related

- Parent wave: `documents/03-planning/waves/wave-gap-236-fe-code-split.md`
- PR #600 (Agent B finding)

## Log

- **2026-04-28** — Filed during Wave GAP-236 consolidation. Agent B confirmed component is dead post-Wave 7-Perf attendance migration.
