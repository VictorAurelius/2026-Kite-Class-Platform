---
title: Wave flow-kc5 — Attendance mark + period rollup
status: active
created: 2026-06-05
updated: 2026-06-05
waves: [flow-kc5]
tag_primary: flow
tags_secondary: [kc5, attendance, mark, period, rollup, kiteclass, campaign]
counter: 5
campaign: flow-verification-campaign
gaps: []
---

# Wave flow-kc5 — Attendance mark + period rollup

**Goal:** Walk end-to-end flow KC-5 (Teacher/Admin điểm danh học sinh trong buổi học — mark single + mark bulk + xem stats; K12 period attendance + daily rollup secondary) trên stack production-equivalent, đạt **G1 PASS**. Đứng sau KC-4 (enrollment) — điểm danh giả định học sinh đã enroll vào lớp.

**Trigger:** KC-5 unblocked sau KC-4 G1 PASS 2026-06-05 (enroll student→class). Campaign chain: KC-4 → {KC-5 attendance, KC-6 grade, KC-7 invoice}. KC-5 tiêu thụ enrollment + class-session làm input.

## 1. Brainstorm

**State-check (2026-06-05):** KC-5 = kiteclass-core flow. Endpoints CONFIRMED tồn tại (grep, no partial-impl risk):
- ✅ `AttendanceController` @ `/api/v1/attendance` — POST (mark single), POST `/classes/{classId}/sessions/{sessionId}/attendance` (bulk per-session), GET `/{id}`, GET `/enrollment/{enrollmentId}`, GET `/classes/{classId}/sessions/{sessionId}/attendance`, GET `/stats/student/{studentId}`, GET `/stats/class/{classId}`, PATCH `/{id}`, DELETE `/{id}`.
- ✅ `AttendanceClassBatchController` @ `/api/v1/attendance/class` — POST `/{classId}/batch`.
- ✅ `AttendancePeriodController` @ `/api/v1/attendance/periods` — POST, PATCH `/{id}`, GET `/daily-rollup`, GET `/students/{studentId}`, GET `/classes/{classId}`, GET `/subject-sections/{subjectSectionId}` (K12 period attendance, secondary scope).
- ✅ `ParentAttendanceFacetController` @ `/api/v1/parent` — GET `/children/{childId}/attendance` (parent portal facet — KC-8 overlap, defer).

**Pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` (BẮT BUỘC — Teacher/Admin mark-attendance persona):** Opus agent spawned 2026-06-05 → artifact `documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc5-attendance.md`. Likely failure modes: duplicate student+session (BR-ATT-001) / EXCUSED_ABSENCE thiếu notes (BR-ATT-005) / permission matrix non-class-teacher mark (BR-ATT-006/007) / attendance_rate recalc off-by-one hoặc div-by-zero (BR-ATT-008) / invalid status enum → 500 vs 400 / mark cho enrollment/session không tồn tại → 404 vs 500 / bulk partial-failure transaction boundary / cross-tenant attendance by-id (GAP-983 surface — verify fix giữ vững).

**Isolation context (post GAP-983):** KC-5 lookup attendance by id + by session + by enrollment + by student. PHẢI verify cross-tenant: attendance record của tenant A KHÔNG đọc/sửa được bởi tenant B. Đây là re-walk verify per `pre-handoff-self-test-completeness.md` §3 (GAP-983 fix Wave security-1 — Hibernate tenantFilter + RLS GUC chỉ active khi TenantContext.isSet() + transaction).

**Blocker:** none known. Cần class-session fixture (KC-3 data: class 14 sky) + enrolled student (KC-4 data) để mark attendance against.

## 2. Task Breakdown

| Bucket | Scope | Owner | Walk class |
|---|---|---|---|
| 0 (Pre-walk) | Opus persona sim agent → ≥5 failure modes per `pre-walk-persona-simulation-mandate.md` §3 | Coordinator | n/a (DONE — agent spawned) |
| A (Walk) | Coordinator G1 walk: mark single attendance (happy PRESENT/LATE/ABSENT/EXCUSED + duplicate + permission + cross-tenant) | Coordinator | user-facing ✅ pre-walk required |
| B (Walk) | Mark bulk per-session walk: grid submit (happy + partial-fail + empty list) + stats recalc verify (student rate + class rate) per `pre-handoff-self-test-completeness.md` §2.1 | Coordinator | user-facing ✅ |
| C (Fix) | Batch-fix high-confidence pre-walk findings + walk-surfaced bugs (catalog-then-batch per `feature-ship-runtime-walk-mandate.md` §3.4) | agent/coordinator | — |
| D (G2 handoff) | G2 recipe MD per `g2-handoff-md-mandate.md` khi G1 PASS | Coordinator | — |

**Period attendance (K12) scope note:** `AttendancePeriodController` (period-level tiết học, GVCN/bộ môn) là K12-only secondary. G1 walk focus class-session attendance (Phase 1 BETA primary). Period rollup spot-check nếu thời gian cho phép; else defer G2/Phase 3 K12.

## 3. Scope

Full §3 expansion happens at walk-time (after pre-walk agent returns). Skeleton:
- **BE (kiteclass-core):** `module/attendance/**` (AttendanceController + AttendanceClassBatchController + AttendancePeriodController + service impls + Attendance entity + AttendanceRepository + mapper + DTOs).
- **Verify target:** mark student (KC-4 enrolled) trong class 14 session → attendance record created; duplicate same student+session → 409; EXCUSED_ABSENCE thiếu notes → 400; bulk grid submit → N records; stats `/stats/student/{id}` + `/stats/class/{id}` recalc attendance_rate đúng.
- **Isolation (post GAP-983):** cross-tenant attendance by-id read/PATCH/DELETE → 404 (tenant A không chạm record tenant B).
- **Dependency:** KC-3 data (class 14 sky tenant + sessions). KC-4 data (enrolled students). Class-session existence.

## 4. State-Check Evidence

Verified 2026-06-05 (grep, no `| head` per `audit-to-gap-pipeline.md` §2.5):

| Symbol | Verify command | Verdict |
|---|---|---|
| `AttendanceController` @ /api/v1/attendance | `grep -rn "RequestMapping\|PostMapping\|GetMapping" attendance/controller/AttendanceController.java` | ✅ POST mark + bulk-per-session + GET stats/{student,class} + PATCH/DELETE /{id} + GET /enrollment/{id} |
| `AttendanceClassBatchController` @ /api/v1/attendance/class | `grep -rn "RequestMapping\|PostMapping" attendance/controller/AttendanceClassBatchController.java` | ✅ POST /{classId}/batch |
| `AttendancePeriodController` @ /api/v1/attendance/periods | `grep -rn "RequestMapping" attendance/controller/AttendancePeriodController.java` | ✅ POST + PATCH /{id} + GET daily-rollup (K12 secondary) |
| Attendance entity extends BaseEntity | `grep -rln "extends BaseEntity" attendance/` | ✅ tenant-scoped (@Filter inherited per GAP-983 fix) |

Detailed request DTO + BR-ATT-* service logic + permission matrix = read at walk-time (Bucket A/B) to avoid duplicating pre-walk agent's investigation.

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — coordinator walk | Claude | Mark single happy (4 statuses) + duplicate 409 + EXCUSED-no-notes 400 + permission guard + cross-tenant reject; bulk grid submit + partial-fail + stats recalc; production-equivalent stack | ⬜ |
| G2 — human walk | User | Per G2 recipe MD (Bucket D) — mark attendance via UI grid + xem stats | ⬜ |
| G3 — production parity | User | Post AWS restore — multi-tenant attendance isolation + period rollup K12 | ⬜ |

## 6. Agent Spawn Pattern

_(n/a — flow-walk wave: Bucket 0 = 1 Opus pre-walk persona-sim agent (background); G1 = coordinator manual walk on local stack. No parallel bucket-agent fan-out. Fix-agents spawned ad-hoc per finding per `agent-model-opus-default.md`.)_

## 7. Closure Protocol

1. Catalog walk findings → file gaps inline per `discovery-to-gap-inline-filing.md` §3.
2. Batch-fix high-confidence (pre-walk + walk) per `feature-ship-runtime-walk-mandate.md` §3.4.
3. Re-walk affected scope per `pre-handoff-self-test-completeness.md` §3 (if fixes shipped).
4. G2 recipe MD per `g2-handoff-md-mandate.md` (Bucket D).
5. Flip campaign §4 KC-5 row → 🔄 walk-pass-pending-human.
6. wave-history.jsonl append; frontmatter draft → active.
7. CSV + ROADMAP sync per `post-merge-sync-completeness.md`.

## 9. Pre-walk findings (2026-06-05) — `audits/persona-review/2026-06-05-pre-walk-kc5-attendance.md`

12 failure modes (5 HIGH / 5 MEDIUM / 2 LOW). **Contract surprises (quan trọng cho walk):**
- ⚠️ **Mark dùng `enrollmentId` + `sessionId`, KHÔNG phải `studentId` + `classId`.** Body POST `/api/v1/attendance` = `{enrollmentId, sessionId, status, notes?}`. Walker phải resolve enrollment_id trước (sky: enrollment 32 = student 4 → class 14; sessions class 14 = id 1-27).
- ⚠️ **Enum status = `EXCUSED` (KHÔNG `EXCUSED_ABSENCE`).** Gửi `EXCUSED_ABSENCE` → 400. Dùng `EXCUSED`.
- ⚠️ **Single-mark KHÔNG dùng `X-Teacher-Id`** (chỉ bulk/period/batch). Authz single-mark giờ qua `@PreAuthorize hasAccessToEnrollment` → cần `X-User-Id` = `classes.teacher_id` (class 14 = `00aa4ce9-0f7c-48a9-bf8d-6e974ba30023`) hoặc admin.

**HIGH bugs batch-fix TRƯỚC walk (đã ship code):**
- **GAP-991** P1 — single-mark thiếu authz (OWASP A01, cross-flow sweep GAP-729) → `@PreAuthorize hasAccessToEnrollment`.
- **GAP-992** P1 — session-status guard (BR-ATTEND-002) + session-existence → 404 SESSION_NOT_FOUND / 400 SESSION_NOT_MARKABLE.
- **GAP-993** P2 — EXCUSED requires notes (BR-ATT-005) → 400 EXCUSED_REQUIRES_NOTE.
- **GAP-994** P2 — rate formula BR-ATT-008 (PRESENT+LATE)/total.
- **GAP-995** P3 — docs drift EXCUSED_ABSENCE → EXCUSED (rules.md + use-cases.md; closes GAP-232 follow-up).

MEDIUM (spot-check walk): bulk findAllById misleading error soft-delete (#6) / bulk duplicate 400 vs 409 docs (#7) / markedBy null single-mark (#8) / ADMIN chặn PATCH override (#10). LOW: studentName placeholder (#11) / invalid-input graceful 400/404 (#12).

Unit `AttendanceServiceTest` PASS (11+2 mới). Integration tests (3 file) update qua agent (mock authz + session fixtures + rate recompute).

## 8. Log

- **2026-06-05 (plan ship):** Filed sau KC-4 G1 PASS (enrollment unblock). State-check confirmed attendance endpoints exist (3 controllers — class-session mark + class-batch + period K12). Pre-walk persona sim agent (Opus) spawned. NEW isolation surface post GAP-983: attendance by-id + by-session + by-enrollment + by-student lookups → cross-tenant re-walk verify. Walk (Bucket A/B) after pre-walk findings land + batch-fix HIGH bugs.
- **2026-06-05 (batch-fix + walk batch 1):** 5 HIGH pre-walk bugs fixed (GAP-991..995). Unit `AttendanceServiceTest` PASS; 3 IT files green (agent). G1 walk batch 1: guards verified ✅ GAP-992 (session 99999 → 404) + GAP-993 (EXCUSED no notes → 400) + GAP-995 (EXCUSED_ABSENCE → 400). **Walk discovered W-1 (GAP-996 P0):** happy-path 500 — attendance schema↔entity drift (student_id NOT NULL + lowercase status CHECK + stale unique) chặn mọi write trên Flyway schema; IT mù vì `ddl-auto=create-drop` thay Flyway (0 rows ever). V87 migration created (student_id nullable + uppercase status CHECK + enrollment-session unique). Rebuild #2 → re-walk per `feature-ship-runtime-walk-mandate.md` §3.4.

## 10. G1 Outcome (2026-06-05)

**G1 ✅ PASS** (production-equivalent walk, post V87 + rebuild #2):

| # | Scenario | Result |
|---|---|---|
| W1 | Happy PRESENT/LATE/ABSENT/MAKEUP/EXCUSED+notes (5 status) | 201 ✅ |
| W2 | Duplicate (enrollment+session) | 400 ✅ |
| W3 | EXCUSED no notes (GAP-993) | 400 ✅ |
| W4 | Session COMPLETED (GAP-992, single+bulk) | 400 SESSION_NOT_MARKABLE ✅ |
| W4b | Session non-existent 99999 (GAP-992) | 404 SESSION_NOT_FOUND ✅ |
| W5 | `EXCUSED_ABSENCE` old enum (GAP-995) | 400 ✅ |
| W6/W6b | No / wrong X-User-Id (GAP-991 authz OWASP A01) | 403 ✅ |
| W7 | Cross-tenant khanh mark sky | 403 ✅ |
| W8 | Stats rate (GAP-994 — 1 PRESENT + 1 LATE / 5) | 40.0% ✅ (formula cũ = 20%) |
| W9/W9b | Bulk happy / bulk into COMPLETED | 201 / 400 ✅ |
| W10/W10b | GET by-id sky / khanh (GAP-983 re-walk) | 200 / 404 ✅ isolation holds |
| W10c | GET by-id no tenant header | 200 ⚠️ → GAP-997 P3 (known GAP-983 limit, gateway-trust, not prod-reachable) |

**GAP-991/992/993/994/995/996 → DONE** (walk-verified). GAP-997 P3 OPEN (defense-in-depth, defer Phase 1.5+). Period attendance (K12) secondary — defer G2/Phase 3.

**Walk fixtures (dev DB):** enrollment 32 → ACTIVE; teacher_classes (3,14,MAIN_TEACHER); session 5 → COMPLETED; attendance rows created. G2 human test re-walks via UI.

G2 handoff: [`2026-06-05-g2-recipe-kc5-attendance.md`](../../05-guides/operations/2026-06-05-g2-recipe-kc5-attendance.md). Campaign §4: KC-5 → 🔄 walk-pass-pending-human.
