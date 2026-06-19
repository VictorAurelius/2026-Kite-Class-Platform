# GAP-873: Bulk Un-Skip kiteclass-frontend Tests (Next.js-15 use(params) + jsdom)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 — coverage debt, WARN-only (không chặn merge)
**Domain:** Frontend Testing
**Found:** 2026-06-02 (Wave local-doable-12 Bucket C — split-out từ GAP-346)
**Affects:** kiteclass-frontend test coverage (~206 skipped tests / ~77 skip call-sites)

---

## Problem

GAP-346 ship CI WARN mechanism (`scripts/check-test-skip-ratio.sh` + `quality-code.yml` job `test-skip-ratio`) làm skip ratio kiteclass-frontend VISIBLE (vitest báo 26.7% test-level / ~7% call-site). Phần **bulk un-skip** — phần lớn (~145 của 206 skipped tests nằm trong 14 file `describe.skip`) — được DEFERRED có chủ đích sang gap này, theo `incident-to-rule-pipeline.md` premature-rule guard (WARN-first; HARD STOP chỉ sau khi backlog un-skip đã clear).

Đây KHÔNG phải bug — là test coverage debt. CI hiện WARN-only nên không chặn merge.

## Root Cause (carried từ GAP-346)

1. **Next.js 15 migration** đổi `params` sync object → `Promise`; page dùng `const { id } = use(params)`; RTL `render()` không unwrap promise → toàn bộ `describe.skip` integration suite (teacher/student/course/class detail + edit + list pages).
2. **jsdom limitations** — form validation timing, Radix Select PointerCapture, sonner toast portal → skip thay vì mock đúng layer / chuyển E2E.
3. **React Query mutation timing** — `should update X successfully` hook mutation tests flaky trong jsdom.

## Proposed Fix (defer — multi-wave effort)

- **Phase 1 (Next.js-15 describe.skip):** mock `useParams` từ `next/navigation` thay vì pass-prop pattern → re-enable ~7 file describe.skip integration suites.
- **Phase 2 (jsdom limitation):** convert form-validation skips → schema-level unit (`zod.safeParse`); toast assertions → mock `toast.success/error`; Radix Select → E2E.
- **Phase 3 (mutation timing):** stabilize React Query mutation success assertions hoặc chuyển E2E.
- **Final:** kiteclass-frontend skip ratio ≤5% call-site (target ≤2%); flip CI WARN → HARD STOP.

## Acceptance Criteria

- [ ] Phase 1: 7+ `describe.skip` integration suites re-enabled via `useParams` mock pattern
- [ ] Phase 2: jsdom-limited skips converted to schema-unit OR E2E
- [ ] Phase 3: hook mutation tests stabilized OR moved to E2E
- [ ] Final: kiteclass-frontend call-site skip ratio ≤5% (`bash scripts/check-test-skip-ratio.sh` → OK band)
- [ ] Final: flip `quality-code.yml` job `test-skip-ratio` từ WARN-mode → HARD STOP (exit 1 khi >15%)

## Related

- **Parent:** GAP-346 (PARTIAL — CI WARN mechanism + silent-skip docs shipped Wave local-doable-12 Bucket C)
- **CI mechanism:** `scripts/check-test-skip-ratio.sh` + `quality-code.yml` job `test-skip-ratio`
- **Rule:** `incident-to-rule-pipeline.md` §3.1 (WARN-first premature-rule guard — HARD STOP after backlog clear)

## Out-of-scope

- Changing test framework (vitest stays)
- kitehub-frontend (0% skip ratio, không cần)
- Backend Java tests (0% skipped)

## Log

- **2026-06-02** — Filed Wave local-doable-12 Bucket C, split-out từ GAP-346. GAP-346 Bucket C scope = CI WARN mechanism (bounded, safe) + document silent skips; bulk un-skip (Next.js-15 describe.skip migration + jsdom limitation conversion) là phần lớn effort, defer sang gap này per `gap-done-discipline.md` §3 PARTIAL exit-ramp.
