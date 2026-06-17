# GAP-1466: KC-6 teacher gradebook 403 — class.teacher_id (actor UUID) chưa gán → owning teacher bị deny enrollments/class

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-16 (Flow Verification Campaign — agent-G1 walk KC-6 grade trên g2walk)
**Affects:** Walk seed (`kitehub/scripts/seed-walk-tenant.sh`) + KC-6 grade flow + class-create teacher assignment

## Problem

Agent-G1 walk KC-6 (teacher gradebook) trên tenant `g2walk`: trang `/teacher/grades/25` (gradebook class#25) render OK nhưng gọi `GET /api/v1/enrollments/class/25?size=100&status=ACTIVE` → **403** cho chính teacher SỞ HỮU class.

Root cause (`AuthorizationBean.hasAccessToClass` line 75-96): `@PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToClass(#classId)")`. Teacher (role TEACHER, không STAFF) → dựa `hasAccessToClass(25)`, so sánh `classes.teacher_id` (= **actor X-User-Id UUID** per V73/GAP-795) `==` authenticated user UUID.

Seed (`seed-walk-tenant.sh`) tạo class#25 qua `POST /api/v1/courses/20/classes` với `course.teacherId=15` (**Long domain id**) nhưng KHÔNG gán `class.teacher_id` (**actor UUID**) → cột NULL/unset → `hasAccessToClass(25)` deny → 403. **Authz đúng-thiết-kế**; lỗ hổng là teacher-actor-UUID chưa được wire vào class.

Hai sub-issue cần phân biệt:
- **(a) Walk-seed completeness** (per `walk-data-committed-seed.md`): seed phải gán teacher actor-UUID vào `class.teacher_id` để gradebook walk được.
- **(b) Class-create teacher-assignment** (cần verify): khi 1 teacher tạo/sở hữu class qua UI thật, `class.teacher_id` (actor UUID) có được gán đúng không? Nếu UI class-create KHÔNG set → real teacher cũng dính 403 → escalate **P1**.

## Proposed Fix

1. Extend `seed-walk-tenant.sh`: sau khi tạo class, gán teacher actor-UUID vào `class.teacher_id` (qua class-edit endpoint hoặc class-create teacher field). Teacher actor UUID = `auth_credentials.user_uuid` where `entity_type='TEACHER' AND entity_id=15`.
2. Verify class-create/edit FE + BE gán `class.teacher_id` (actor UUID) khi assign teacher cho class. Nếu thiếu → escalate P1 + fix BE/FE.

## Acceptance Criteria

- [ ] Walk seed gán teacher → teacher login → `GET /api/v1/enrollments/class/{id}` từ `/teacher/grades/{id}` trả 200 (hết 403)
- [ ] Class-create/edit teacher-assignment verified gán `class.teacher_id` (actor UUID); nếu thiếu → P1 + fix

## Related

- Discovered in: agent-G1 walk KC-6 grade 2026-06-16 (`.claude/g3-walk-scratch/kc6-teacher-walk.mjs` evidence)
- `AuthorizationBean.hasAccessToClass` (GAP-795 actor-UUID + GAP-798 reference-id bridge)
- `walk-data-committed-seed.md` (seed completeness)
- Sibling walk-found: KC-8 parent provisioned end-to-end OK (contrast — parent link via reference-id worked)
