# GAP-877: V73 UUID sweep bỏ sót actor columns — BIGINT/VARCHAR drift cross-cluster

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC attendance/finance/rbac/gamification/compliance/branding + KH branding)
**Affects:** Mọi cluster — actor user-id columns BIGINT/VARCHAR còn lại sau V73 sweep

## Problem

V73 (GAP-795) chỉ convert `created_by`/`updated_by` (+ hardcoded `classes.teacher_id`, `classes.rescheduled_by_user_id`, `parent_invitations.invited_by_user_id`) sang UUID. Vì `X-User-Id` JWT là UUID, các cột actor BIGINT/VARCHAR còn lại sẽ KHÔNG nhận được user-id thật (parse fail) — cùng lớp lỗi V73 đã fix nhưng chưa quét hết.

**Actor columns còn BIGINT/VARCHAR (cross-cluster):**

- KC attendance-grading: `attendance.marked_by`, `grades.graded_by`/`finalized_by`, `submissions.graded_by`, `subject_grades.reviewed_by`, `attendance_period.recorded_by` (cluster 03 §E)
- KC finance: `payments.received_by`/`payer_id`, `payment_records.recorded_by`, `payment_idempotency_keys.user_id` (cluster 04 §A6)
- KC rbac: `user_roles.assigned_by` VARCHAR(100), `vettings.decided_by_user_id` BIGINT (cluster 05 §A1)
- KC gamification: `reward_redemptions.approved_by` BIGINT (cluster 06 §B)
- KC compliance: `audit_log.actor_user_id`, `moderation_queue.assigned_reviewer_id`, `dmca_takedown_requests.reviewer_user_id`, `deletion_requests.user_id`, `incidents.reporter_user_id`/`assigned_officer_user_id`, `child_protection_audit_log.actor_id` (cluster 07 §A4) — **compliance risk: child-protection actor null + GDPR Art 17 audit gap**
- KC branding: `rebrand_approvals.initiator_user_id`/`approver_user_id` BIGINT (cluster 08 §A4)
- KH subscription `BaseEntity.created_by/updated_by` VARCHAR(100) (cluster 02 §A5) — cross-DB string vs UUID drift

## Proposed Fix

Migration sweep tương tự V73 nhưng cho actor user-id columns. Apply per cluster với DO-block dynamic. Đặc biệt ưu tiên compliance cluster (07) vì gap có legal risk.

## Acceptance Criteria

- [ ] Migration V## sweep actor columns → UUID per cluster (có thể batch multi-cluster)
- [ ] Verify code path không lỗi parse khi ghi UUID vào cột đã convert
- [ ] Document KH BaseEntity (VARCHAR(100)) — keep vs sweep decision (cross-DB normalization)
- [ ] Reference cluster docs 03/04/05/06/07/08 + KH 02

## Discovered in

10 cluster docs Wave 13. Sister GAP-795 (V73 original sweep).
