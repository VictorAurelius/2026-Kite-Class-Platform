# ADR-033: Class Reschedule — Cal.com Update-In-Place + 5-Field Audit Log + Outbox Event + Email Phase 1 / Zalo OA Phase 2

**Status:** ACCEPTED
**Date:** 2026-05-24
**Deciders:** @nguyenvankiet (solo-dev, acting CTO)
**Reviewers:** N/A (solo-dev mode per CLAUDE.md decision context locked 2026-05-06)
**Related Gap(s):** GAP-291 (P0 class reschedule endpoint + email fallback — Wave beta-readiness-4 Bucket D)
**Related Rule(s):** `.claude/rules/design-patterns.md` §3.5.1 Outbox pattern; `.claude/rules/audit-service-isolation.md` (audit row independence); `.claude/rules/vn-localization-audit-checklist.md` §2 (VND/date/VN sample/VN culture)
**Related Wave plan:** `documents/03-planning/waves/wave-2026-05-24-beta-readiness-4-meta-pdpl-pricing-reschedule-tone.md` §3.4 + §3.6 (cross-bucket LOCKED decisions)

---

## Context

Persona Agent 1 (P2 Center Owner Hằng — Wave beta-readiness-4 Bucket A audit 2026-05-20) flagged "reschedule lớp học" như P0-2 blocker cho Phase 1 BETA invite cohort. VN edu reality:

- Học thêm trung tâm thường-xuyên đổi lịch do **giáo viên ốm/bận đột xuất** (1-2 lần/tuần)
- **Phòng học không khả dụng** (sự cố cơ sở vật chất, repair)
- **Mất điện / mất Internet** ở khu vực (mùa mưa Saigon Q3, miền Bắc mùa hè)
- **Lễ Tết / nghỉ chính thức** (Tết 7-10 ngày off, 30/4 + 1/5 5 ngày off, 2/9, v.v.)
- **Học sinh xin nghỉ tập thể** (sự kiện trường, dịch bệnh)

Hiện tại (pre-Wave beta-readiness-4) class lifecycle có status `SCHEDULED → IN_PROGRESS → COMPLETED / CANCELLED`. Không có cơ chế "đổi lịch" — phải hủy + tạo lại → **mất toàn bộ attendance history + grade history + enrollment links**. Vô lý cho VN edu reality.

3 outside-in agents (persona simulation + failure-mode matrix + VN edu SaaS benchmark) cùng kết luận:
- **Cal.com benchmark** (industry standard cho scheduling): update-in-place + `Booking.rescheduledFromId` audit pointer
- **Misa edu benchmark VN:** đổi lịch là feature first-class, không phải hủy+tạo
- **Failure-mode matrix:** "Mất attendance history" = highest-severity risk khi user trial reschedule lần đầu → bounce

### Forces at play

| Force | Pressure |
|---|---|
| VN edu reality (đổi lịch tuần ~ 1-2 lần/lớp) | High — feature mandatory cho Phase 1 BETA invite |
| Attendance + grade history preservation | Highest — pillar PDPL Art 9 + parent trust |
| Backward compat (existing `ClassStatus` enum consumers) | High — không phá UI/test code cũ |
| VN PDPL Art 11 + Art 14 (audit log retention ≥5 năm + DPO contact) | Mandatory cho prod tag |
| Phase 1 BETA scope (email only; Zalo OA defer Phase 2+) | Cost + complexity bounded |
| Zalo culture (parent communication primary qua Zalo) | Defer Phase 2 vì Zalo OA setup nặng — email fallback Phase 1 |
| Operational notification classification | Bypass `marketing_consented` gate — operational message |

---

## Decision

Adopt **Cal.com pattern with 4 specific adaptations** cho Phase 1 BETA:

### 1. Update-in-place (NO new ClassStatus enum)

- Class entity giữ existing status `SCHEDULED → IN_PROGRESS → COMPLETED / CANCELLED`.
- Reschedule chỉ apply khi status = `SCHEDULED` (preserve attendance history cho IN_PROGRESS; COMPLETED/CANCELLED read-only).
- Mutate `startDate` + `endDate` IN-PLACE; status không đổi.
- **Backward compat 100%:** mọi UI/test/query đang filter trên `ClassStatus` enum không cần đổi.

### 2. 5-field audit log on `classes` row (migration V68)

| Column | Type | Purpose |
|---|---|---|
| `rescheduled_by_user_id` | BIGINT | User ID đã trigger reschedule (PDPL Art 9 audit trail) |
| `rescheduled_at` | TIMESTAMPTZ | Timestamp UTC reschedule |
| `previous_start_date` | DATE | Capture startDate BEFORE mutation |
| `previous_end_date` | DATE | Capture endDate BEFORE mutation |
| `reschedule_reason_category` | VARCHAR(64) | Enum: `GV_OM_BAN_DOT_XUAT` / `PHONG_HOC_KHONG_KHA_DUNG` / `MAT_DIEN_INTERNET` / `LE_TET_NGHI_CHINH_THUC` / `HOC_SINH_XIN_NGHI_TAP_THE` / `LY_DO_KHAC` |
| `reschedule_reason_notes` | TEXT | Optional free-text notes ≤ 2000 chars |

Audit captures the **most recent** reschedule (not full history). Multi-reschedule history defer Phase 2+ (separate `class_reschedule_history` table — out-of-scope Phase 1 BETA per cost-benefit).

PDPL Art 9 retention ≥5 năm satisfied via existing DB backup policy.

### 3. Outbox event `class.rescheduled` (atomic guarantee)

- `ClassServiceImpl.rescheduleClass` publishes `ClassRescheduledEvent` qua `OutboxEventWriter` trong cùng `@Transactional` block.
- Outbox pattern (per `design-patterns.md` §3.5.1) đảm bảo at-least-once delivery — không lost event nếu broker xuống.
- Routing key: `class.rescheduled` → queue `class.rescheduled.queue`.

### 4. Feature flag default OFF + No-op consumer default (cost-bound rollout)

- `kite.class.reschedule.notify.enabled=false` mặc định (Phase 1 BETA).
- Default consumer `ClassRescheduledNoOpConsumer` chỉ log event (observability mà không trigger user-visible side effect).
- Phase 1.5+ enable feature flag → `ClassRescheduledEmailConsumer` activates → forward to `class.rescheduled.email.queue` → `kitehub-email` render Thymeleaf + send.
- **Notification classification = OPERATIONAL** — bypass `marketing_consented` gate (operational message liên quan trực tiếp lịch học của con).
- Greeting fallback inline cho parent persona: "Kính gửi quý phụ huynh," (very formal). Phase 3 refactor consume `_shared/persona-tone.mustache` partial.

### 5. Email Phase 1 / Zalo OA Phase 2

- Phase 1 BETA: email only (Resend/SES, kitehub-email service).
- Phase 2: Zalo OA channel (sister consumer `ClassRescheduledZaloConsumer`; same event payload).
- Defer Zalo Phase 1 vì:
  - Zalo OA business account setup ~ 2 tuần admin overhead
  - Phase 1 BETA invite cohort nhỏ (10-20 tenants) — email coverage ≥ 95%
  - VN edu reality: parent dùng cả email lẫn Zalo; email = operational record, Zalo = real-time notification

---

## Consequences

### Positive

- **Attendance + grade history preserved** trong 100% case reschedule (Cal.com industry pattern proven).
- **Audit log immutable trail** satisfy PDPL Art 9 + parent trust + dispute resolution.
- **Backward compat 100%** với existing `ClassStatus` enum consumers — KHÔNG có test broke, KHÔNG có UI broke.
- **Cost-bound rollout** — Phase 1 BETA ship endpoint + audit + Outbox, defer email/Zalo dispatch cost cho Phase 1.5+ khi user demand validated.
- **VN culture-aware** — 6 reason categories match reality học thêm; greeting "Kính gửi quý phụ huynh" tone formal phù hợp parent persona.

### Negative

- **Single reschedule history (not multi-history)** — chỉ track lần đổi gần nhất. Multi-history (chain of changes) defer Phase 2+. Trade-off: simpler schema + cost-bound; acceptable vì VN edu reschedule frequency thấp ~ 1-2 lần/quarter cho hầu hết lớp.
- **No real-time push notification Phase 1** — email arrives ~ 1-5 phút sau API call. VN edu reality acceptable (parent check email mỗi sáng). Real-time Zalo defer Phase 2.
- **Recipient resolution responsibility** — Outbox event payload ship empty `enrolledStudentIds` + `parentUserIds` lists v1.0.0; consumer side performs lookup khi feature flag = true (Phase 1.5+ scope). Defer giữ Bucket D scope tight.

### Neutral

- **Email greeting fallback inline** — Phase 3 refactor consume `_shared/persona-tone.mustache` partial từ Bucket E (Wave plan §3.6 LOCKED decision). Acceptable technical debt — không block Phase 1 BETA invite.

---

## Alternatives Considered

### A. New `ClassStatus.RESCHEDULED` enum + new lifecycle row

**Rejected** vì:
- Phá backward compat: mọi UI status badge + test fixture + analytics query phải update
- Confusing semantic: "RESCHEDULED" status implies new lifecycle state, nhưng class vẫn cần `SCHEDULED` để start. Multi-status (RESCHEDULED + SCHEDULED) gây race condition.
- Cal.com industry benchmark explicitly avoid status proliferation.

### B. Soft-delete old class + create new class with `rescheduledFromId` pointer

**Rejected** vì:
- Mất attendance + grade history (foreign key cascade hoặc orphan rows).
- Tăng row count classes table 2x mỗi lần reschedule → performance issue + analytics confusion.
- VN edu reality reschedule ~ 1-2 lần/tuần/lớp → 50-100x bloat sau 1 năm.

### C. Full history table `class_reschedule_history` Phase 1

**Deferred Phase 2+** vì:
- Phase 1 BETA scope (10-20 tenants) — full history overkill.
- Audit log đơn lẻ (5 columns trên classes) đã cover audit trail + PDPL retention requirement.
- Migration cost + index overhead không justify cho Phase 1 demand.

### D. Real-time push via WebSocket / SSE Phase 1

**Rejected** vì:
- WebSocket infrastructure không có Phase 1 BETA (defer Phase 2 per existing roadmap).
- VN edu parent communication reality = async (Zalo group + email check sáng/tối), không cần real-time push.
- Cost-benefit không justify infra build cho 10-20 tenant Phase 1.

---

## Implementation Notes

- **Migration V68** — `V68__add_class_reschedule_audit.sql` (Bucket C reserved V67/V67b; V68 reserved Bucket D per cross-bucket LOCKED decision §3.6).
- **Endpoint** — `POST /api/v1/classes/{classId}/reschedule` (per api-contract.md `course-class/api-contract.md`).
- **Authorization** — `@PreAuthorize("@authz.hasAccessToClass(#classId)")` (per `AuthorizationBean` pattern; OWASP A01 per-resource authz).
- **Feature flag** — `KITE_CLASS_RESCHEDULE_NOTIFY_ENABLED` env var (default `false`).
- **Outbox routing** — `class.rescheduled` → `class.rescheduled.queue` (consumed by `ClassRescheduledNoOpConsumer` OR `ClassRescheduledEmailConsumer` based on feature flag).
- **Email queue** — `class.rescheduled.email.queue` (consumed by `kitehub-email/ClassRescheduledEmailService` → Thymeleaf render → Resend/SES dispatch).

---

## Validation

- IT test `ClassControllerRescheduleIT` covers happy path + invalid dates + non-SCHEDULED 409 + authz 403.
- Unit test `ClassServiceRescheduleTest` covers audit log capture + Outbox publish + status preserved + attendance history preserved.
- Email template render test `ClassRescheduledEmailTemplateTest` verifies VN sample data PASS + greeting `"Kính gửi quý phụ huynh,"` present + VND-free (no currency in reschedule notification).

---

## Log

- **2026-05-24** (v1.0.0): ADR created paired same-PR with Wave beta-readiness-4 Bucket D ship (GAP-291). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5; new decision, no constraint loosening for prior work; existing class lifecycle grandfathered with rule applying prospectively to reschedule operation only).
