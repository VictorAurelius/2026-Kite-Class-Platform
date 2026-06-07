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
- [x] Document KH BaseEntity (VARCHAR(100)) — keep vs sweep decision (cross-DB normalization) → **KEEP** (xem §Decision)
- [ ] Reference cluster docs 03/04/05/06/07/08 + KH 02

## Decision — KH BaseEntity `created_by`/`updated_by` → KEEP VARCHAR(100) (Wave p0-local-1 Bucket B, 2026-06-07)

Quyết định: **(a) Giữ VARCHAR(100), KHÔNG sweep sang UUID.** Lý do (không có parse-fail thật → không justify sweep cost):

1. **KH là control-plane (SaaS platform admin), actor heterogeneous.** Actor của KH-side audit fields là platform admin / system job / scheduler / service-account — thường định danh bằng email / username / "system" STRING, KHÔNG luôn là UUID. VARCHAR(100) flexible hơn; ép UUID sẽ **gây parse-fail ở chiều ngược lại** (cột UUID không chứa được "system"). KC-side khác: actor luôn là tenant user với `X-User-Id` UUID (GAP-795) → UUID đúng cho KC.

2. **Không có AuditorAware<UUID> trong KH.** `created_by`/`updated_by` được populate qua Spring `@CreatedBy`/`@LastModifiedBy` + `AuditingEntityListener` (`BaseEntity.java`); KH platform/subscription không có `AuditorAware` bean → fields hiện ghi null/string, KHÔNG có code nào parse chúng thành UUID. → 0 parse-fail risk hiện tại.

3. **Drift cross-DB chỉ là cosmetic.** KH DB và KC DB là 2 database tách biệt; cột `created_by` không bao giờ JOIN cross-DB. "Drift" BIGINT/VARCHAR-vs-UUID giữa 2 bounded context là intentional per-context modeling, KHÔNG phải bug.

4. **Sweep cost cao, benefit nil.** `BaseEntity` shared bởi 5 KH module (platform + subscription/branding/email/admin) → sweep = thêm `AuditorAware<UUID>` mỗi service + migration mọi bảng KH dùng created_by/updated_by. Cost lớn, không đổi behavior.

→ KH BaseEntity sub-item của GAP-877 = **CLOSED (keep+document)**. Gap vẫn OPEN cho các KC actor columns (cluster 03-08) — đó mới là phần có parse-fail risk thật (X-User-Id UUID ghi vào cột BIGINT/VARCHAR).

## Related discovery — oauth_attempts.tenant_id BIGINT (Wave p0-local-1 Bucket B)

Cùng lớp BIGINT/UUID drift: `kitehub-subscription` bảng `oauth_attempts.tenant_id` là `BIGINT NULL` (V51 GAP-582) trong khi mọi bảng tenant-scoped KH khác key trên `instance_id UUID`. V66 (GAP-885) đã enable RLS bằng cách so sánh `tenant_id::text` để né `::uuid` cast mismatch + document anomaly inline. Re-key sang `instance_id UUID` defer tới khi OAuth signup flow được implement (oauth_attempts hiện chưa có caller — defensive scaffolding). Cân nhắc gộp vào batch sweep này hoặc tách gap riêng.

## Discovered in

10 cluster docs Wave 13. Sister GAP-795 (V73 original sweep).
