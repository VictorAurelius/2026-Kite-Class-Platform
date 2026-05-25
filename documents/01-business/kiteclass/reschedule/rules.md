# Reschedule — Business Rules

**Domain:** KiteClass Core (`module.clazz` — class reschedule subdomain)
**Version:** 1.0
**Updated:** 2026-05-25
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/`
**ADR:** [`ADR-033-class-reschedule-pattern.md`](../../../02-architecture/adr/ADR-033-class-reschedule-pattern.md)

---

## 1. Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-RESCHEDULE-001 | Reschedule preserves attendance + grade history | KHÔNG tạo `ClassStatus.RESCHEDULED` mới; chỉ update `startDate`/`endDate` + log `RescheduleHistory` row. Attendance + grade rows giữ nguyên reference. |
| BR-RESCHEDULE-002 | `reasonCategory` mandatory | FE dropdown enforce 6 categories enum `RescheduleReasonCategory` (GV_OM_BAN_DOT_XUAT / PHONG_HOC_KHONG_KHA_DUNG / MAT_DIEN_INTERNET / LE_TET_NGHI_CHINH_THUC / HOC_SINH_XIN_NGHI_TAP_THE / LY_DO_KHAC). |
| BR-RESCHEDULE-003 | `reasonNotes` optional, max 2000 chars | `@Size(max = 2000)` trên `RescheduleClassRequest.reasonNotes`. Bắt buộc khi `reasonCategory = LY_DO_KHAC` (FE validation per ADR-033 §UX). |
| BR-RESCHEDULE-004 | `newStartDate` ≥ today | Không cho phép reschedule về quá khứ. `@NotNull` + service-layer check `newStartDate.isBefore(LocalDate.now())` → 400. |
| BR-RESCHEDULE-005 | `newEndDate` > `newStartDate` | Service-layer validation; nếu equal hoặc reversed → 400. |
| BR-RESCHEDULE-006 | Permission: TEACHER/ADMIN của class hiện tại | Reschedule là teacher-led action; PARENT/STUDENT không có quyền. Service-layer check qua TenantContext + class membership. |
| BR-RESCHEDULE-007 | Outbox event `ClassRescheduledEvent` emitted | Trong cùng `@Transactional`, persist class update + outbox row. Consumer `ClassRescheduledEmailConsumer` notify parents (Phase 1.5+). Phase 1 BETA: `ClassRescheduledNoOpConsumer` placeholder. |
| BR-RESCHEDULE-008 | Multi-tenant isolation | Mọi query filter theo `instance_id` qua TenantContext interceptor. |
| BR-RESCHEDULE-009 | Tránh đụng holiday VN | Nếu `newStartDate`/`newEndDate` rơi vào `Holiday` row (Wave 2 GAP-053), FE WARN (không BLOCK); teacher có thể override với reason ghi rõ. |
| BR-RESCHEDULE-010 | History row append-only | `RescheduleHistory` table append rows on every reschedule; KHÔNG update/delete (PDPL Art 11 audit trail). |

### BR-RESCHEDULE-001: Reschedule preserves history (LOCKED ADR-033)

- **Value:** Update `Class.startDate` + `endDate` in-place; persist `RescheduleHistory` row với `oldStartDate` / `oldEndDate` / `newStartDate` / `newEndDate` / `reasonCategory` / `reasonNotes` / `rescheduledBy` / `rescheduledAt`. KHÔNG tạo new ClassStatus.
- **Rationale:** Tạo `RESCHEDULED` ClassStatus mới sẽ break analytics aggregate (attendance rate per class) + làm grade calculation phải walk history. Update in-place đơn giản hơn + audit trail tách riêng. Per ADR-033 §Alternatives, option B (new status) bị reject.
- **Source:** ADR-033 LOCKED cross-bucket decision §3.6 (Wave beta-readiness-4 Bucket D).
- **Reviewer:** @nguyenvankiet (acting Tech Lead + Product Owner, solo-dev, 2026-05-25). Stakeholder review queued via GAP-156.
- **Compliance check:** **Considered** — Luật Giáo dục 2019 (class schedule transparency cho phụ huynh); PDPL 2023 Art 11 (audit log append-only).
- **Review cadence:** Annual (stable design). **Next review:** 2027-05-25. Event triggers: BE persona feedback "muốn xem RESCHEDULED status separate", hoặc analytics team request status field cho lifecycle reporting.

### BR-RESCHEDULE-002: 6-category reason dropdown (LOCKED Wave br-4 GAP-291)

- **Value:** Enum `RescheduleReasonCategory` 6 giá trị Vietnamese display: "Giáo viên ốm/bận đột xuất" / "Phòng học không khả dụng" / "Mất điện / mất Internet" / "Lễ Tết / nghỉ chính thức" / "Học sinh xin nghỉ tập thể" / "Lý do khác".
- **Rationale:** VN edu market research Wave br-4 surveyed 12 trung tâm dạy thêm → 6 categories cover >95% reschedule events. Free-text only → analytics impossible + retroactive support burden cao. Mandatory dropdown + optional notes balance UX vs structured data.
- **Source:** Wave beta-readiness-4 Bucket D outside-in audit + competitor analysis (MISA, FastTrac).
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-25).
- **Compliance check:** N/A — operational metadata.
- **Review cadence:** Quarterly cho first 2 quarters (Phase 1 BETA), sau đó Annual. **Next review:** 2026-08-25. Event triggers: ≥3 tenant complaints "thiếu category", analytics dashboard surface category usage <50% cho any category.

---

## 2. Flow

### Reschedule Flow (Teacher-led)

1. Teacher mở UI `(teacher)/teacher/classes/[classId]` → click "Đổi lịch"
2. UI hiển thị modal với:
   - DatePicker `newStartDate` (default = class current `startDate`)
   - DatePicker `newEndDate` (default = class current `endDate`)
   - Dropdown `reasonCategory` (mandatory, 6 options)
   - Textarea `reasonNotes` (optional, max 2000 chars; mandatory nếu category = LY_DO_KHAC)
3. Teacher submit → FE call `POST /api/v1/classes/{classId}/reschedule`
4. BE `ClassServiceImpl.rescheduleClass`:
   - Validate permission (BR-RESCHEDULE-006)
   - Validate dates (BR-RESCHEDULE-004/005)
   - Check holiday overlap → WARN response if any (BR-RESCHEDULE-009)
   - Persist new `Class.startDate`/`endDate` + append `RescheduleHistory` row + outbox event — same `@Transactional`
5. Response 200 + `ClassResponse` (updated)
6. Outbox dispatcher async emit `ClassRescheduledEvent` → consumer notify parents (Phase 1.5+) hoặc no-op (Phase 1 BETA)

### Integration với Attendance + Grade

- Attendance rows liên kết qua `class_session_id` (KHÔNG via class date) → reschedule không affect attendance history
- Grade rows liên kết qua `class_id` → unchanged
- Future sessions tự động shift theo `startDate` + class schedule rule (per `ADR-002-academic-year-structure.md`)

### Integration với Holiday VN

- `HolidayRepository` cho lookup VN public holidays (Tết, 30/4, 1/5, 2/9, etc.)
- Service-layer call `holidayRepository.findOverlapping(newStartDate, newEndDate)` → if non-empty, response include `holidayWarning` field; FE hiện toast "Lịch mới đè vào ngày lễ <name>, bạn có muốn tiếp tục?"

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| Class rescheduled (Phase 1.5+) | `class-rescheduled-parent` | Parent email + Zalo OA backup |
| Class rescheduled (Phase 1 BETA) | (no-op consumer placeholder) | N/A |

Phase 1 BETA: `ClassRescheduledNoOpConsumer` log event nhưng KHÔNG send email (avoid spam during beta). Phase 1.5 enable real email template.

---

## 4. Config

| Key | Default | Description |
|-----|---------|-------------|
| `kiteclass.reschedule.notes-max-length` | `2000` | Max characters cho `reasonNotes` |
| `kiteclass.reschedule.holiday-check.enabled` | `true` | Toggle holiday overlap warning |
| `kiteclass.reschedule.outbox-event.enabled` | `true` | Toggle outbox emission (defensive flag) |
| `kiteclass.reschedule.consumer.mode` | `no-op` | `no-op` (Phase 1) / `email` (Phase 1.5+) |

### Database Indexes

- `idx_reschedule_history_class_id` — Reschedule history per class
- `idx_reschedule_history_rescheduled_at` — Recent reschedules across center

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules trong file này derive từ: ADR-033 (LOCKED design decision), Wave beta-readiness-4 Bucket D outside-in audit, VN edu market research 12 TT, competitor benchmark (MISA / FastTrac).
- **Rationale:** Rule values reflect product judgment + VN edu market norms (Mon-Sat working days, holiday awareness Tết/30-4/1-5/2-9, Zalo OA backup channel). Detailed per-rule rationale backfilled during GAP-156 Phase 2.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead, solo-dev, 2026-05-25). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3.
- **Compliance check:** **Considered** — Luật Giáo dục 2019 (class schedule transparency), PDPL 2023 Art 11 (audit trail append-only).
- **Review cadence:** Quarterly (default). **Next review:** 2026-08-25. Event triggers: VN edu regulation amendment, ≥5 tenant complaints về reschedule UX, analytics surface unused reason category.

## Log

- **2026-05-25** Initial 3-layer business docs filed per GAP-738 (Wave beta-readiness-8 Bucket B). Closes Wave br-4 Bucket D code-doc sync gap (PR #1781 ship code but skip 3-layer docs). Rules extracted từ `RescheduleClassRequest.java` + `RescheduleReasonCategory.java` + ADR-033.
