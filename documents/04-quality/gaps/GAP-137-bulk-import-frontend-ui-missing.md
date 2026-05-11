# GAP-137: Bulk Import Frontend UI Missing (Wave 1 Backend Inaccessible)

**Status:** 🟢 DONE 2026-05-11 (Wave 60 Bucket B — admin/bulk-import page shipped)
**Priority:** 🔴 P0
**Domain:** Frontend / Feature Completeness
**Found:** 2026-04-19 (UI audit catch-up — ui-review-2026-04-19.md §Top Findings #2)
**Affects:** `kiteclass-frontend` → `(dashboard)/students/page.tsx` — Wave 1 GAP-051 feature user-inaccessible

## Problem

Wave 1 (#332) shipped a **backend-only** bulk import for students:
- `POST /api/v1/students/bulk-import/preview`
- `POST /api/v1/students/bulk-import/commit`
- `POST /api/v1/students/bulk-import/jobs/{id}/errors`
- `BulkImportJob` audit entity + Flyway V41 migration
- Apache POI xlsx parsing, per-chunk transactions, row-level error reporting

**But** `kiteclass/kiteclass-frontend/src/app/(dashboard)/students/page.tsx` (verified 2026-04-19) offers only:
- Search input
- Table pagination
- Single "Thêm học viên" button → `/students/new` (one-at-a-time form)

No upload button, no dropzone, no template download, no progress indicator, no error report UI. The feature built to "unblock the #1 K-12 school onboarding bottleneck" (quoted from PR #332 description) is currently inaccessible to end users.

**Evidence:**
```
$ grep -l "bulk\|import" kiteclass/kiteclass-frontend/src/app/(dashboard)/students/
(no matches in page.tsx)

$ find kiteclass/kiteclass-frontend/src -name "*bulk*"
(empty)
```

## Root Cause

Wave 1 plan scoped backend-only MVP. No follow-up PR added the frontend. Hook didn't block because no business-doc rule enforced frontend-for-every-endpoint.

## Proposed Fix

Add a bulk-import UI block on students list page:

1. **Entry point:** "Nhập hàng loạt" button next to "Thêm học viên" on `/students`.
2. **Upload dialog/drawer:**
   - File input (accept `.xlsx`)
   - Template download link (generate sample xlsx with required columns)
   - "Xem trước" button → calls `/bulk-import/preview`
   - Preview table shows first N rows + per-row validation status
   - "Xác nhận nhập" button → calls `/bulk-import/commit`
3. **Post-commit state:**
   - Show jobId + success count + error count
   - If errors > 0 → "Tải báo cáo lỗi (xlsx)" button → calls `/jobs/{id}/errors`
4. **Accessibility:** Vietnamese labels, keyboard support, progress state while uploading.

Suggested component path: `kiteclass-frontend/src/components/students/BulkImportDialog.tsx`.

## Acceptance Criteria

- [x] "Nhập hàng loạt" button visible on `/students` for admin users (entry link → `/admin/bulk-import`)
- [x] Dedicated page opens with file picker (page chosen over dialog per Wave 60 plan §3 Bucket B; template download deferred to follow-up — column spec embedded inline in the page description)
- [x] Preview shows parsed rows + validation results before commit (table with row-level errors)
- [x] Commit triggers `/bulk-import/commit`, shows job result (success toast + result card)
- [x] Error report download works (`/jobs/{id}/errors`) — XLSX blob downloaded via object URL
- [x] Component test exercises happy path + 5 error/edge paths (7 tests total, MSW-mocked BE)
- [x] Vietnamese copy + error messages throughout

### Out of scope (follow-up gaps)

- Downloadable XLSX template (column header sample file) — embed inline column spec for now; tracked in a future enhancement gap if a center owner needs it
- E2E Playwright test against live stack — component test (Vitest + MSW) covers happy + 5 error paths; E2E deferred to Wave 51 e2e sweep follow-up
- RBAC gate ("admin users only") — admin page is reachable via `/admin/bulk-import`; role-based redirect handled by existing dashboard guard (no new gate added)

## Related

- Audit: `documents/04-quality/audits/ui/ui-review-2026-04-19.md` §Top Findings #2, §New Issues U-2
- Backend PR: #332 (Wave 1 — GAP-051 bulk-import students xlsx MVP)
- Business rule gap: GAP-109 (bulk-import rules.md undocumented) — documentation, not UI
- Documentation follow-up: may need to create `documents/01-business/bulk-import/use-cases.md` for UI interaction flow

## Log

- **2026-05-11** — Wave 60 Bucket B shipped FE consumer:
  - `kiteclass/kiteclass-frontend/src/app/(dashboard)/admin/bulk-import/page.tsx` (admin page; state machine idle → selected → previewing → previewed → committing → committed)
  - `kiteclass/kiteclass-frontend/src/lib/api/bulk-import.ts` (preview / commit / downloadErrorReport with `X-File-Name` header workaround for jsdom + MSW)
  - `kiteclass/kiteclass-frontend/src/types/bulk-import.ts` (BulkImportResult + RowError + BulkImportPhase)
  - MSW handlers added to `src/mocks/handlers.ts` (3 endpoints, 5 trigger patterns by filename)
  - 7 component tests passing (`__tests__/bulk-import.test.tsx`): renders, rejects non-xlsx, rejects oversized, happy path, errors path, BE 500, reset
  - Entry link "Nhập hàng loạt" added to `/students` page
  - Verification: `pnpm test --run` 728/728 pass, `pnpm build` clean (`/admin/bulk-import 4.78 kB`), `pnpm lint` no new warnings
- 2026-04-19 — Identified during Audit 4. Wave 1 merged without FE surface; gap 3 waves later.
