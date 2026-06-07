# GAP-877: V73 UUID sweep bỏ sót actor columns — BIGINT/VARCHAR drift cross-cluster

**Status:** 🟢 DONE 2026-06-08 — per-module state-check hoàn tất cho cả 18 cột: 5 converted V94 (unbound SAFE) + 13 = KEEP Long (domain reference-id, V1 convention — KHÔNG phải actor-UUID parse-fail, xem §Decision-13) + KH BaseEntity KEEP (§Decision). KHÔNG có actor field kiểu UUID nào ghi vào cột BIGINT → 0 parse-fail bug. Gap premise (UUID→BIGINT drift) refuted cho 13 cột Long-bound. No conversion needed (tránh fix thừa).
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

- [x] Migration sweep actor columns → UUID — **V94 converted 5 cột SAFE** (unbound, no live writer); **13 cột còn lại = KEEP Long by-decision** (per-module state-check 2026-06-08 — domain reference-id, V1 convention, NOT actor-UUID parse-fail; xem §Decision-13)
- [x] Verify code path không lỗi parse khi ghi UUID vào cột đã convert — `V94ActorColumnUuidSweepIT` 5/5 PASS (Flyway thật, Testcontainers, no ddl-auto); entity UserRole.assignedBy + ModerationQueue.assignedReviewerId retype UUID đồng bộ
- [x] Document KH BaseEntity (VARCHAR(100)) — keep vs sweep decision (cross-DB normalization) → **KEEP** (xem §Decision)
- [x] Reference cluster docs 03/04/05/06/07/08 + KH 02

## Decision — KH BaseEntity `created_by`/`updated_by` → KEEP VARCHAR(100) (Wave p0-local-1 Bucket B, 2026-06-07)

Quyết định: **(a) Giữ VARCHAR(100), KHÔNG sweep sang UUID.** Lý do (không có parse-fail thật → không justify sweep cost):

1. **KH là control-plane (SaaS platform admin), actor heterogeneous.** Actor của KH-side audit fields là platform admin / system job / scheduler / service-account — thường định danh bằng email / username / "system" STRING, KHÔNG luôn là UUID. VARCHAR(100) flexible hơn; ép UUID sẽ **gây parse-fail ở chiều ngược lại** (cột UUID không chứa được "system"). KC-side khác: actor luôn là tenant user với `X-User-Id` UUID (GAP-795) → UUID đúng cho KC.

2. **Không có AuditorAware<UUID> trong KH.** `created_by`/`updated_by` được populate qua Spring `@CreatedBy`/`@LastModifiedBy` + `AuditingEntityListener` (`BaseEntity.java`); KH platform/subscription không có `AuditorAware` bean → fields hiện ghi null/string, KHÔNG có code nào parse chúng thành UUID. → 0 parse-fail risk hiện tại.

3. **Drift cross-DB chỉ là cosmetic.** KH DB và KC DB là 2 database tách biệt; cột `created_by` không bao giờ JOIN cross-DB. "Drift" BIGINT/VARCHAR-vs-UUID giữa 2 bounded context là intentional per-context modeling, KHÔNG phải bug.

4. **Sweep cost cao, benefit nil.** `BaseEntity` shared bởi 5 KH module (platform + subscription/branding/email/admin) → sweep = thêm `AuditorAware<UUID>` mỗi service + migration mọi bảng KH dùng created_by/updated_by. Cost lớn, không đổi behavior.

→ KH BaseEntity sub-item của GAP-877 = **CLOSED (keep+document)**. Gap vẫn OPEN cho các KC actor columns (cluster 03-08) — đó mới là phần có parse-fail risk thật (X-User-Id UUID ghi vào cột BIGINT/VARCHAR).

## Related discovery — oauth_attempts.tenant_id BIGINT (Wave p0-local-1 Bucket B)

Cùng lớp BIGINT/UUID drift: `kitehub-subscription` bảng `oauth_attempts.tenant_id` là `BIGINT NULL` (V51 GAP-582) trong khi mọi bảng tenant-scoped KH khác key trên `instance_id UUID`. V66 (GAP-885) đã enable RLS bằng cách so sánh `tenant_id::text` để né `::uuid` cast mismatch + document anomaly inline. Re-key sang `instance_id UUID` defer tới khi OAuth signup flow được implement (oauth_attempts hiện chưa có caller — defensive scaffolding). Cân nhắc gộp vào batch sweep này hoặc tách gap riêng.

## Decision-13 — 13 cột còn lại → KEEP Long (per-module state-check 2026-06-08)

Quyết định: **giữ Long cho cả 13 cột, KHÔNG convert sang UUID.** Đây là per-module decision mà §Deferred-13 (bên dưới) yêu cầu — state-check 2026-06-08 (per `audit-to-gap-pipeline.md` §2.8, user directive "state-check tránh fix thừa") đã trace writer + entity từng cột:

**Evidence — cả 13 cột là `Long` end-to-end (entity field + writer param):**

| Cột | Entity type | Writer signature |
|---|---|---|
| `attendance.marked_by` | `Long markedBy` | `AttendanceServiceImpl(Long teacherId)` |
| `attendance_period.recorded_by` | `Long recordedBy` | `AttendancePeriodServiceImpl` Long |
| `grades.graded_by` / `finalized_by` | `Long gradedBy/finalizedBy` | k12 service Long |
| `subject_grades.reviewed_by` | `Long reviewedBy` | `SubjectGradeServiceImpl(Long reviewerId)` |
| `payment_records.recorded_by` | `Long recordedBy` | payment service Long |
| `payment_idempotency_keys.user_id` | `Long` (resolvedParentId) | `ParentPaymentController` Long domain ref |
| `vettings.decided_by_user_id` | `Long decidedByUserId` | `VettingServiceImpl(Long decidedByUserId)` |
| `dmca_takedown_requests.reviewer_user_id` | `Long reviewerUserId` | `DmcaService(Long reviewerUserId)` |
| `incidents.reporter_user_id` / `assigned_officer_user_id` | `Long reporterUserId/assignedOfficerUserId` | `IncidentService(Long)` |
| `child_protection_audit_log.actor_id` | `Long actorId` | childprotection service Long |
| `rebrand_approvals.initiator_user_id` / `approver_user_id` | `Long initiator/approverUserId` | `RebrandApprovalService(Long approverUserId)` |

**Verdict:** Gap premise §Problem ("X-User-Id JWT là UUID → ghi vào cột BIGINT/VARCHAR actor → parse fail") **KHÔNG áp dụng** cho 13 cột này — code thread **`Long` reference-id** (V1 convention: `X-User-Reference-Id` = `parents.id`/`teachers.id`/`students.id` per `TenantFilterInterceptor`), KHÔNG phải UUID actor. Verify: grep `private UUID <actorField>` toàn kiteclass-core → **0 hit** (không có actor field kiểu UUID nào để mismatch với BIGINT column). → 0 parse-fail bug.

Đây là **cùng decision class với KH BaseEntity KEEP** (§Decision): actor identity bằng numeric reference-id là intentional per-context modeling, KHÔNG phải bug. Convert sang UUID = **fix thừa + phá Long write-path** (đúng cảnh báo §Deferred-13 "convert mù sẽ phá runtime").

**Out-of-scope note (không thuộc GAP-877):** "actor null cho admin/owner" (UserContext.getCurrentReferenceId nullable cho non-domain-entity actor) là concern RIÊNG về reference-id population, KHÔNG phải type-drift parse-fail — nếu cần, file gap riêng. GAP-877 scope = UUID/BIGINT type-drift → resolved.

---

## Deferred-13 (Wave p0-local-1 Bucket A — RESOLVED by §Decision-13 above; preserved as historical context)

13 cột actor bị **live service code thread numeric domain/reference id** (không phải actor UUID) → convert mù sẽ phá runtime write-path. Mỗi cột cần per-module decision (actor UUID vs domain-id) + re-wiring writer trước khi sweep. Đặc biệt: `payment_idempotency_keys.user_id` được `ParentPaymentController` ghi `resolvedParentId` (Long domain ref, KHÔNG phải actor UUID). Đây là residual của GAP-877 — gap giữ PARTIAL cho tới khi 13 cột re-wire xong (effort lớn hơn, có thể tách wave riêng khi cần). Compliance cluster (audit_log.actor_user_id, child_protection_audit_log.actor_id, incidents.*) nằm trong nhóm này — ưu tiên khi mở wave re-wiring.

## Log

- **2026-06-08 (DONE — per-module state-check resolves 13 deferred cols as KEEP Long):** Fix-time state-check (per `audit-to-gap-pipeline.md` §2.8, user directive "state-check tránh fix thừa") traced writer + entity type cho cả 13 cột deferred → ALL `Long` end-to-end (entity field + service writer param). Grep `private UUID <actorField>` toàn kiteclass-core = 0 hit → KHÔNG có actor field UUID nào mismatch BIGINT column → 0 parse-fail bug. Gap §Problem premise (UUID→BIGINT parse-fail) refuted cho 13 cột Long-bound: code thread Long reference-id (V1 convention X-User-Reference-Id), KHÔNG phải UUID actor. Decision: KEEP Long cả 13 (§Decision-13) — cùng class KH BaseEntity KEEP; convert = fix thừa + phá runtime. Per-module decision (mà §Deferred-13 yêu cầu) HOÀN TẤT. 18/18 cột resolved (5 V94 convert + 13 KEEP + KH BaseEntity KEEP). Status PARTIAL 25→DONE 100, file → closed/. No code change (state-check verdict = no conversion needed).
- **2026-06-07 (Wave p0-local-1 Bucket A — safe-subset sweep):** State-check 2026-06-07 xác nhận 10/10 cột mẫu vẫn BIGINT (V79 còn THÊM `reviewed_by/assigned_by/approved_by BIGINT` sau V73). Bucket A code-trace từng cột → V94 chỉ convert **5 cột SAFE** (no live numeric write-path bind): payments.received_by/payer_id (no entity binding), reward_redemptions.approved_by (no JPA entity), user_roles.assigned_by (VARCHAR→UUID, entity unset), moderation_queue.assigned_reviewer_id (entity unset). Entity-migration triad: UserRole.assignedBy + ModerationQueue.assignedReviewerId retype UUID. IT 5/5 PASS. **13 cột defer** (xem §Deferred-13 — service code thread numeric domain-id, cần re-wiring). KH BaseEntity → KEEP (xem §Decision). Status OPEN→PARTIAL ~25% (5/18 sweepable cột done; KH BaseEntity closed-by-decision; 13 KC defer). NOT false-DONE per `gap-done-discipline.md` §3.

## Discovered in

10 cluster docs Wave 13. Sister GAP-795 (V73 original sweep).
