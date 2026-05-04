---
title: Period Attendance — Business Rules (K-12 schools)
status: draft
created: 2026-05-04
updated: 2026-05-04
domain: kiteclass.period-attendance
gaps: [GAP-323]
---

# Period Attendance — Business Rules (K-12 schools, TT 22/2021)

> Phase 1A scope only — schema + read-only API. Write API + idempotency, daily
> roll-up, and concurrent load contract land in GAP-323b. Grade formula
> service + state machine land in GAP-323c.

## Frontmatter (5-attribute review per `business-logic-review.md`)

- **Source:** TT 22/2021/TT-BGDĐT (Thông tư 22/2021 quy định về đánh giá học
  sinh THCS, THPT) Điều 7 (đánh giá thường xuyên + giữa kỳ + cuối kỳ); TT
  32/2018/TT-BGDĐT (Chương trình GDPT 2018 — 13 môn THCS, 17 môn THPT, 5–10
  tiết/ngày). Operational baseline cross-checked against P5 K-12 persona
  review Finding 3 (`documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md`).
- **Rationale:** K-12 trường công lập have a fundamentally different daily
  ops model (per-tiết, multi-môn, multi-GV) from private trung tâm (per-day,
  single-GV). Capturing this dimension at the data layer is a prerequisite
  for AC-OPS-001 (GVCN ≤2 min điểm danh for 42 HS), AC-OPS-002 (period-based
  attendance roster), AC-OPS-003 (12–15 môn gradebook + ĐTBmHK formula),
  conduct grade (GAP-059), and học bạ MOET (GAP-055).
- **Reviewer:** @nguyenvankiet (acting Education domain expert + Product
  Owner, solo-dev, 2026-05-04). Phase 1A scope is schema + read-only API
  only; full review against MOET reviewers + 3 GVCN trường công lập queued
  for GAP-323b alongside the write API and concurrent load test.
- **Compliance check:** **Compliant** — TT 22/2021 Đ.7 (đánh giá thường
  xuyên + giữa kỳ + cuối kỳ formula) shipped as javadoc on existing
  `SubjectGrade` (GAP-054 Phase 1) and will be promoted to executable
  `GradeFormulaService` in GAP-323c. TT 32/2018 (GDPT 2018) chuong trinh
  shapes the SubjectSection seed (GAP-327). PDPL N/A for Phase 1A — the
  read-only surface returns existing fields already covered by general
  retention rules.
- **Review cadence:** **Annual + event-driven** on TT 22/2021 amendment, on
  MoET chương trình GDPT update, or whenever AC-OPS-001 (≤2 min target) is
  measured against real GVCN sample. **Next review:** 2027-05-04 OR within
  30 days of any TT 22 / TT 32 amending decree publication.

## 1. Scope

K-12 schools (`tenant.vertical_type = 'K12_SCHOOL'`) record attendance once
per (student, subject_section, date, period_no). Centers
(`vertical_type = 'CENTER'`, default) keep using the legacy per-day
`Attendance` table — they do not interact with this domain.

## 2. Vocabulary

| Term | Vietnamese | Definition |
|------|------------|------------|
| Period (tiết) | tiết | One ~45-minute slot in the daily schedule. K-12 schools run 5–10 tiết/day. |
| SubjectSection (lớp bộ môn) | lớp bộ môn | One môn (subject) of one HomeroomClass with one bộ môn teacher. |
| HomeroomClass (lớp chủ nhiệm) | lớp chủ nhiệm | The K-12 cohort (e.g., 10A1) shared across all SubjectSections. |
| GVCN | Giáo viên chủ nhiệm | Homeroom teacher who records the daily aggregate. |
| GV bộ môn | Giáo viên bộ môn | Subject teacher who records per-tiết status. |
| Tổ trưởng | Tổ trưởng chuyên môn | Subject-area lead who reviews + approves grades (GAP-323c scope). |

## 3. Business Rules

### BR-VERTICAL-001 — Tenant operating-model discriminator

| Attribute | Value |
|-----------|-------|
| **Value** | `instances.vertical_type` ∈ {`CENTER`, `K12_SCHOOL`}; default `CENTER`. |
| **Source** | KiteClass dual-vertical strategy (Wave 18b1 plan §1). |
| **Rationale** | Existing trung tâm tenants must continue operating unchanged; K-12 schools require a fundamentally different model. A discriminator is cheaper than a separate deployment and keeps shared infra (auth, billing, parent portal) intact. |
| **Reviewer** | @nguyenvankiet (acting Product Owner). |
| **Compliance check** | N/A. |
| **Review cadence** | Annual; event-driven if a third vertical (e.g., trường ngoài công lập song ngữ) is added. |

### BR-PERIOD-ATT-001 — SubjectSection FK required

A row in `attendance_period.subject_section_id` MUST reference a non-deleted
row in `subject_sections.id` (the entity introduced by GAP-054 Phase 1).
Phase 1A enforces this at the service layer via the existing JPA filter; a
DB-level FK is deferred to GAP-323b alongside write-path hardening.

### BR-PERIOD-ATT-002 — Period number range

`period_no` ∈ ℤ⁺ (integer ≥ 1). Realistic K-12 range is 1..10 (TT 32/2018
GDPT 2018 maximum 10 tiết/day for THPT). Phase 1A keeps the schema CHECK
liberal (`period_no > 0`) and lets the service layer enforce the K-12
contract per `tenant.vertical_type`. A tighter CHECK (`period_no BETWEEN 1
AND 10`) lands in GAP-323b once the write API + GVCN UI confirm no edge
cases (extra periods, half-day events).

### BR-PERIOD-ATT-003 — Uniqueness

For non-deleted rows: `(student_id, subject_section_id, date, period_no,
instance_id)` is unique. A student cannot be marked twice for the same period
of the same subject on the same day. Soft-deleted rows are excluded so a
mistakenly-recorded period can be re-recorded after deletion.

### BR-PERIOD-ATT-004 — Status enum reuse

`status` reuses `AttendanceStatus` (`PRESENT`, `ABSENT`, `LATE`, `EXCUSED`,
`MAKEUP`). Same enum the legacy per-day `Attendance` uses; UI conventions
(short codes P/V/T/CP/HB, color classes) and gamification points logic
remain consistent.

### BR-PERIOD-ATT-005 — Recorded-by is required for audit

`recorded_by` (user ID of GV bộ môn or GVCN) MUST be non-null. Audit trail
is mandatory under TT 22/2021 (giáo viên chịu trách nhiệm về kết quả đánh
giá). Phase 1A relies on the recorded-by client header (same pattern as
`AttendanceController#markAttendance`); Phase 1B will add server-side
authorization (RBAC: only the SubjectSection's bộ môn teacher OR the
HomeroomClass GVCN can write).

### BR-PERIOD-ATT-006 — recorded_at separates from date

`date` = lesson day (the day the tiết occurred). `recorded_at` = server
timestamp at recording. They differ when GVCN back-dates entry within an
audit window. Phase 1A allows arbitrary backdating; the audit window
contract (e.g., ≤24 h after lesson) is BR-PERIOD-ATT-007 in GAP-323b.

### BR-PERIOD-ATT-007 — (deferred to GAP-323b) Audit window for write

Reserved. Will define how many hours after `date` a write/edit is permitted
without Tổ trưởng override.

## 4. Out-of-scope (tracked separately)

| Item | Tracked in |
|------|-----------|
| Write API (POST/PATCH/DELETE) + idempotency | GAP-323b |
| GVCN mobile UI ≤2 min for 42 HS | GAP-323b |
| Daily aggregation view (vắng cả ngày = vắng ≥7 tiết) | GAP-323b |
| Concurrent điểm danh load test (30 GVCN, 5 phút) | GAP-323b |
| `GradeFormulaService` TT 22/2021 ĐTBmHK + ĐTBmCN | GAP-323c |
| Grade state machine DRAFT → REVIEWED → PUBLISHED | GAP-323c |
| Multi-subject gradebook UI (12–15 môn) | GAP-323c |
| Per-table CHECK enforcing K-12 vertical_type | GAP-323b |

## 5. Log

- **2026-05-04** Phase 1A rules.md created alongside V50 + V24 migrations
  and read-only API (Wave 18b1 Bucket F, GAP-323).
