# GAP-1412: Student portal renders inline fixture arrays to real students (mock-in-production)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-15 (hardcode-mock state-check, FE agent)
**Affects:** 9 `kiteclass-frontend/src/app/(dashboard)/student/*` pages

## Problem

9 student-portal pages render inline `const TODAY_CLASSES/PENDING_TASKS/CLASSES = [...]` fixture arrays directly (no API/useQuery). Real students see fabricated classes/grades/payments/attendance. MOCK (unwired), not hardcode — fix = wire to real student-facet API (KC-9 student portal; student-auth shipped per campaign 2026-06-14 RBAC-LMS work). Examples: `(dashboard)/student/today/page.tsx:35,41`, `student/my-classes/page.tsx:22`.

## Proposed Fix

Wire student pages to real student-facet endpoints (today schedule, my-classes, grades, assignments, attendance). Remove inline fixture arrays from render path. Fail-loud empty/error state instead of fake data.

## Acceptance Criteria

- [ ] 9 student pages fetch real data (no inline fixture arrays in render)
- [ ] Student sees own classes/grades/attendance (or proper empty state)
- [ ] Inline `const X = [...]` fake records removed from production student pages

## Related

- Umbrella: GAP-1410 · Audit: `2026-06-15-hardcode-mock-state-check.md`
- KC-9 student portal + GAP-1277 (student-auth BE) + GAP-1285 (student self-enrollment); GAP-269a adjacent
