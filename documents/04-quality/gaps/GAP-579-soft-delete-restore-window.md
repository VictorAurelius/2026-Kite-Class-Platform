# GAP-579: Soft-delete + 30-day restore window cho students/classes/grades

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (trước GA Phase 2 — hard-delete = permanent data loss path)
**Domain:** Backend / Database
**Found:** 2026-05-15 (Wave 85 Bucket A persona outside-in audit cell 1.4)
**Affects:** Tenant-scoped tables `students`, `classes`, `grades`, `attendances`, `invoices`

## Problem

Wave 85 inside-out scope không cover soft-delete recovery path. Bucket A persona audit cell 1.4 (cô Mai — P1 Solo Teacher):

- Hiện tại DELETE = `DELETE FROM students WHERE id=?` → hard-delete permanent, no recovery path.
- Expect: nếu xóa nhầm 1 lớp / 1 cột điểm / 1 học sinh, revert qua admin support trong <1h.
- Current "support workflow": admin restore từ RDS daily snapshot → tốn 4-6h (RDS PITR overhead) + impact toàn tenant (snapshot scope = entire DB).

P2 owner cell 2.4: tương tự — nếu staff xóa nhầm hóa đơn / học sinh hàng loạt, mong recover trong <4h. Self-service tier 1 ideal nhưng minimum = soft-delete giúp admin restore single record không cần PITR.

## Root Cause

- Phase 1 BETA scope focus features + security; recovery tier = inside-out blind spot.
- Schema chưa có `deleted_at TIMESTAMP NULL` pattern column.

## Proposed Fix

Wave 86 scope (3 sub-tasks):

1. **Schema migration V53+** — add `deleted_at TIMESTAMP NULL` cho 5 tenant-scoped tables: `students`, `classes`, `grades`, `attendances`, `invoices`.
2. **Soft-delete logic** — repository layer convert DELETE → UPDATE SET `deleted_at=NOW()`; default queries filter `WHERE deleted_at IS NULL` via Hibernate `@SQLDelete` + `@Where`.
3. **30-day restore window** — admin self-service restore endpoint `POST /admin/restore/{table}/{id}` → SET `deleted_at=NULL` + audit log entry; scheduled job sau 30 ngày → hard-delete (TRUE DELETE) cho records `deleted_at < NOW() - INTERVAL '30 days'`.

## Acceptance Criteria

- [ ] V53 migration add `deleted_at` column + index `WHERE deleted_at IS NULL` cho 5 tables
- [ ] Hibernate `@SQLDelete` + `@Where(clause = "deleted_at IS NULL")` applied
- [ ] Admin restore endpoint POST + audit log entry trên restore
- [ ] Scheduled job (cron 02:00 UTC daily) hard-delete records `deleted_at < NOW() - INTERVAL '30 days'`
- [ ] Integration test: DELETE student → UPDATE deleted_at set → default query hides → admin restore → record reappears
- [ ] Integration test: scheduled job purges 31-day-old soft-deleted records
- [ ] FE owner dashboard "Khôi phục" tab listing soft-deleted items < 30 ngày tuổi
- [ ] Pre-handoff verify per `pre-handoff-self-test-completeness.md` §2.4

## Related

- Wave 85 Bucket A persona audit: cell 1.4 (P1 Teacher) + cell 2.4 (P2 Owner)
- Wave 86 scope (planned)
- GAP-257 (existing P0 restore drill — RDS snapshot tier, complementary)

## Log

- **2026-05-15** Filed via Wave 85 Bucket A persona outside-in audit. Defer Wave 86 — Wave 85 scope locked. Status OPEN. Critical trước GA Phase 2 — hard-delete recovery via RDS snapshot only = poor UX + 4-6h TTR vs <1h target.
