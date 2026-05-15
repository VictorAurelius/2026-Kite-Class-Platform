# GAP-598: P3 grade edit-window (24h) + P2 unlock-P3 recovery path

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (defer Wave 87)
**Domain:** Backend / Frontend
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A persona-outside-in audit cell 4.5)
**Affects:** P3 Manager daily ops error recovery + P2 owner override authority

## Problem

Persona cell 4.5 (anh Tâm P3 failure recovery):
- P3 nhập điểm sai → cần sửa được trong 24h (audit-friendly edit window)
- P3 nếu lock-out (failed login 5×, password forgot) → P2 owner cần unlock được (not require admin escalation)

Wave 86 không cover edit-window / lock-out recovery path.

## Root Cause

Grade entity hiện tại có thể không có `edited_at` + `edit_locked_at` columns. P2 không có authority unlock P3 (chỉ admin).

## Proposed Fix

1. **Grade edit-window** Flyway `V55__grade_edit_window.sql`:
   - ADD COLUMN `grades.entered_at` TIMESTAMP DEFAULT NOW()
   - ADD COLUMN `grades.edit_locked` BOOLEAN DEFAULT FALSE
   - Cron: nightly lock grades where entered_at < NOW() - INTERVAL '24h' AND edit_locked=FALSE
2. **GradeController** `kitehub-classroom/.../GradeController.java`:
   - PUT `/grades/{id}` → check `edit_locked` → if true return 403 "Quá 24h, không thể sửa. Liên hệ chủ trung tâm."
3. **P2 unlock authority**:
   - `POST /api/v1/admin/grades/{id}/unlock` allowed cho P2 owner role (RLS policy check)
   - Audit log every unlock với reason field (required)
   - P2 unlock action visible Bucket H admin audit
4. **P2 unlock-P3 lockout**:
   - User account `locked_at` reset by P2 admin endpoint
   - P3 receives email "Tài khoản của bạn đã được mở khóa bởi [P2 owner name]"

## Acceptance Criteria

- [ ] V55 migration shipped
- [ ] Grade edit blocked after 24h cho P3
- [ ] P2 unlock endpoint working với RLS check
- [ ] Audit log every P2 override action
- [ ] P3 lockout reset by P2
- [ ] Email notifications cho both actions
- [ ] Defer to Wave 87

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.4 cell 4.5 + §6 NEW gap proposal #9
- Wave 87 scope (defer)
