# Attendance — Business Rules

**Domain:** KiteClass Core
**Version:** 1.1
**Updated:** 2026-06-15

---

## 1. Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-ATT-001 | One record per student per session | UNIQUE(student_id, class_session_id) |
| BR-ATT-002 | **(Planned — Phase 1.5)** Status by check-in time | PRESENT (within grace), LATE (within threshold), ABSENT (beyond or no check-in) — *depends on QR check-in (UC-ATT-03); not yet implemented* |
| BR-ATT-003 | **(Planned — Phase 1.5)** Grace period | 5 minutes after session start (configurable) — *config key not yet wired (see §4)* |
| BR-ATT-004 | **(Planned — Phase 1.5)** Late threshold | 15 minutes after session start (configurable) — *config key not yet wired (see §4)* |
| BR-ATT-005 | EXCUSED requires note | Must have excuse note to set this status |
| BR-ATT-006 | Teacher can override | MAIN_TEACHER or ASSISTANT can manually set any status |
| BR-ATT-007 | Admin full access | ADMIN can mark attendance for any class |
| BR-ATT-008 | Rate calculation | `attendance_rate = (PRESENT + LATE) / total_sessions * 100%` |
| BR-ATT-009 | Multi-tenant isolation | All queries filtered by `instance_id` |

**Attendance statuses (5 values — must match `AttendanceStatus` enum):** PRESENT, LATE, ABSENT, EXCUSED, MAKEUP

| Status | Display VI | Short | Points deduction (gamification) | Meaning |
|--------|-----------|-------|---------------------------------|---------|
| PRESENT | Có mặt | P | `0` | Student attended on time |
| LATE | Đi trễ | T | `−5` | Student attended after grace period |
| ABSENT | Vắng | V | `−10` | Student did not attend |
| EXCUSED | Có phép | CP | `0` | Approved absence (requires note per BR-ATT-005) |
| MAKEUP | Học bù | HB | `0` | Student attended a make-up session for a missed class |

> Points-deduction values are the source of truth in `AttendanceStatus.getPointsDeduction()` — MAKEUP carries **no deduction** (same as PRESENT/EXCUSED).

### Permission Matrix

| Action | MAIN_TEACHER | ASSISTANT | ADMIN | Student |
|--------|-------------|-----------|-------|---------|
| Mark attendance | Yes (own classes) | Yes (own classes) | Yes (all) | No |
| View own record | No | No | No | Yes |
| View class report | Yes (own classes) | Yes (own classes) | Yes (all) | No |
| Override status | Yes | No | Yes | No |
| Export report | Yes (own classes) | No | Yes (all) | No |

> **Status assignment (current implementation):** All statuses — including `MAKEUP` ("Học bù") — are set **manually** by MAIN_TEACHER / ASSISTANT / ADMIN via UC-ATT-01/02/04/09. There is **no automatic status determination** today; the time-based auto-status path (BR-ATT-002/003/004 + UC-ATT-03 QR check-in) is **(Planned — Phase 1.5)**.

### BR-ATT-CLASS-BATCH-001: Class-overview batch save cap (GAP-268a)

- **Value:** 200 cells per request (`ClassBatchAttendanceRequest.entries.@Size(max=200)`)
- **Rationale:** 10 tiết × 20 students = 200 = realistic worst-case cho one class one day; cap leaves headroom for combined bộ môn classes mà không cho phép unbounded payloads gây DoS.
- **Source:** `informed gut` derived from Wave 18b2 `AttendancePeriodBatchCreateRequest` precedent (cap 60) + class-overview UI scope; quarterly re-review per `business-logic-review.md` §2.1.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-10). Stakeholder review queued via GAP-156.
- **Compliance check:** N/A — operational cap, không touch regulated data.
- **Review cadence:** Quarterly. **Next review:** 2026-08-10. Event triggers: complaint về cap quá nhỏ cho class lớn (>20 students), hoặc audit phát hiện cell count consistently > 200.

### BR-ATT-CLASS-BATCH-002: Idempotency via DB unique index (GAP-268a)

- **Value:** Resubmit cùng `(classId, date, body)` → cùng final state, KHÔNG duplicate rows
- **Rationale:** Network retries + double-click + offline-replay paths đều cần idempotency. DB-level unique index `(student_id, subject_section_id, date, period_no, instance_id)` (V50 migration) là source of truth — application layer chỉ "upsert" qua repository pattern.
- **Source:** Wave 18b2 GAP-323b precedent + standard REST idempotency principle.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-10).
- **Compliance check:** N/A — implementation invariant.
- **Review cadence:** Annual (stable rule). **Next review:** 2027-05-10. Event triggers: V50 migration changed.

---

## 2. Flow

### Check-in Flow (Manual)
1. Teacher opens class session
2. Teacher marks each student: PRESENT, LATE, ABSENT, EXCUSED
3. System saves attendance records
4. Attendance rate auto-recalculated

### Check-in Flow (QR Code) — **(Planned — Phase 1.5, not yet implemented)**

> The QR check-in + time-based auto-status flow below describes the planned design only. There is **no QR/check-in/auto-status code** in `module/attendance` today (see UC-ATT-03 + BR-ATT-002/003/004 + §4 config, all marked Planned). The shipped path is manual marking (Manual flow above + UC-ATT-01/02/04/09).

1. Teacher generates QR code for session (valid for session duration)
2. Student scans QR -> system records check-in time
3. Status auto-determined by check-in time vs session start:
   - `check_in <= start + 5min` -> PRESENT
   - `check_in <= start + 15min` -> LATE
   - `check_in > start + 15min` -> ABSENT (too late)
4. Teacher can override any auto-determined status

### Integration with Grade Module
1. Attendance rate feeds into grade calculation
2. Attendance component weight: configurable per course (default 10%)
3. Score = attendance_rate (e.g., 94.4% attendance = 94.4/100 score)

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| (Planned) Low attendance warning | attendance-warning | Student + parent email |
| (Planned) Absence notification | absence-notice | Student email |

> Email templates not yet implemented. Planned for future PRs.

---

## 4. Config

> **(Planned — Phase 1.5):** The config keys below describe the planned QR check-in + time-based auto-status + attendance-warning features (BR-ATT-002/003/004, UC-ATT-03, §3 Emails). They are **NOT present in any `application*.yml`** and have no effect in the current build (verified `grep` → 0 hits). The shipped manual-marking path (UC-ATT-01/02/04/09) requires none of these. When the feature is built, add the keys to `application.yml` in the same PR per the 3-layer rule.

| Key | Default | Description | Status |
|-----|---------|-------------|--------|
| `attendance.grace-period-minutes` | `5` | Minutes after start for PRESENT | Planned — Phase 1.5 |
| `attendance.late-threshold-minutes` | `15` | Minutes after start for LATE | Planned — Phase 1.5 |
| `attendance.qr-code.expiry` | session duration | QR code validity | Planned — Phase 1.5 |
| `attendance.low-warning-threshold` | `70%` | Warn if rate drops below | Planned — Phase 1.5 |
| `attendance.grade-weight` | `10%` | Default weight in grade calculation | Planned — Phase 1.5 |

### Database Indexes
- `uk_attendance_student_session` — Unique (student_id, class_session_id)
- `idx_attendance_session_id` — Attendance per session
- `idx_attendance_student_id` — Attendance per student
- `idx_attendance_status` — Filter by status

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — Luật Giáo dục 2019 (attendance tracking obligation); PDPL 2023 (student PII in attendance records).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Attendance regulation amendment, ≥5 tenant complaints about policy.

## Log

- **2026-06-15** (GAP-1321 + GAP-1320, Business Logic audit 2026-06-14): (1) Added `MAKEUP` ("Học bù") to §1 status list + a status/points-deduction table to match `AttendanceStatus` enum (5 values) + Layer-2/3 (`api-contract.md`, `use-cases.md`). Added permission-matrix note clarifying all statuses (incl. MAKEUP) are set manually — no auto-determination today. (2) Doc-honesty: marked BR-ATT-002/003/004 (time-based status), §2 QR Code check-in flow, and §4 config keys as **(Planned — Phase 1.5)** — these describe an unimplemented feature (`grep` → 0 code refs, 0 yml keys, no QR/check-in logic in `module/attendance`). Feature build deferred to a future feature wave; this PR is doc-honesty only.
- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
