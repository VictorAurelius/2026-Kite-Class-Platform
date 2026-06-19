# GAP-910: `teacher_courses` M2M không có `instance_id` + RLS

**Status:** 🟢 DONE (wave-gap-audit-p1-1 2026-06-19 — substantive work shipped + CI-verified; residual cosmetic doc-ref/AC-checkbox only per verify pass)
**Priority:** 🟡 P2
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC people-enrollment)
**Affects:** `kiteclass-core` `teacher_courses` table

## Problem

`teacher_courses` M2M không có `instance_id` → không RLS, không audit, không soft-delete. Cô lập tenant gián tiếp qua FK (`teacher_id`/`course_id` trỏ bảng RLS).

Sister GAP-887 (`student_badges`), GAP-908 (academic schedules). Pattern lặp lại — raw query bypass FK → cross-tenant leak risk.

Pattern hợp lý cho bảng nối thuần (chỉ 2 FK PK composite). Tương tự `role_permissions` ở cluster RBAC.

## Proposed Fix

Document accept risk + invariant "không raw query teacher_courses". HOẶC migration denormalize `instance_id` + RLS nếu Phase 1.5 cần cross-tenant query.

## Acceptance Criteria

- [ ] Decision documented (accept vs denormalize)
- [ ] Reference cluster doc 02-people-enrollment narrative + sister GAP-887/908

## Discovered in

`documents/02-architecture/database/kiteclass/02-people-enrollment.md` narrative (teacher_courses M2M)
