# GAP-324: Exam Workflow — Mid-term/Final Distinction, Approval Chain, Publishing Window, Re-take

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (blocks K-12 daily ops + MOET học bạ generation)
**Domain:** Backend (KiteClass Core) + Frontend
**Detected:** 2026-05-04 (P5 K-12 persona review Round 1)
**Related Docs:**
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-OPS-005 + AC-EDGE-004
- `documents/00-brd/persona-criteria/secondary/student-in-P5.md` AC-OPS-005
- `documents/00-brd/persona-criteria/secondary/teacher-employee-in-P5.md` AC-OPS-008

## Current State (verified 2026-05-04)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Assignment entity | `module/assignment/Assignment.java` (4.5K) | ✅ shipped |
| Assignment publish/close endpoints | `AssignmentController.java` POST `/{id}/publish` + `/{id}/close` | ✅ shipped |
| Exam-vs-regular distinction | nowhere | ❌ missing |
| Approval chain (Tổ trưởng → Hiệu trưởng) | nowhere | ❌ missing |
| Publishing window control (auto-release at time T) | nowhere | ❌ missing |
| Exam re-take workflow (sick HS) | nowhere | ❌ missing |
| Phòng thi room assignment | nowhere | ❌ missing |
| Giấy mời phụ huynh PDF gen | `module/document/pdf/` (generic) | 🟡 partial |

**Grep commands run:**
```bash
grep -rli "exam\|invigil\|kỳ thi\|midterm\|finalexam\|HK1\|HK2" kiteclass/ documents/01-business/
# Returns: only generic mentions in business docs, no exam-specific code
```

## Problem

K-12 schools run mid-term + final exams per kỳ. AC-OPS-005 P5 + AC-OPS-008 teacher require: Phó CM creates schedule → admin generates phòng thi (3 HS/phòng SBD MOET-style) + giấy mời phụ huynh PDF → GV chấm + nhập điểm trong window 7d → Tổ trưởng duyệt → Hiệu trưởng ký thông báo → publish; HS sees results in publish window only (not before approval).

Re-take (AC-EDGE-004): HS ốm + giấy bệnh viện → admin tạo re-take session ≤7d sau khỏi → điểm tính bình thường, không penalty.

Current `Assignment` entity treats everything as homework — no exam-specific lifecycle.

## Proposed Fix

1. New entity `ExamSession` (id, instance_id, type ENUM[MIDTERM/FINAL/RETAKE], subject_section_id, semester_id, start_at, end_at, sbd_pattern, room_assignment JSONB, status ENUM[DRAFT/SCHEDULED/IN_GRADING/AWAITING_APPROVAL/APPROVED/PUBLISHED])
2. New entity `ExamScore` (exam_session_id, student_id, score, graded_by, graded_at, is_retake)
3. State machine via existing State Pattern (per `design-patterns.md` §3.3 — no switch)
4. Approval chain: GV submits → Tổ trưởng approve → Hiệu trưởng approve → publish triggers grade publication to HS + parent
5. Publishing window: status = APPROVED but `publish_at` future → cron releases at exact time
6. Re-take workflow: admin creates new ExamSession with type=RETAKE, evidence_url (giấy bệnh viện), Phó CM approval; score replaces original
7. PDF gen: phòng thi list + giấy mời phụ huynh templates per MOET style

## Acceptance Criteria

- [ ] ExamSession + ExamScore entities + V## migration
- [ ] State machine enforces transitions (test: cannot publish without APPROVED)
- [ ] HS endpoint returns scores ONLY when status=PUBLISHED (test verifies)
- [ ] Re-take endpoint creates new ExamSession + Phó CM approval workflow
- [ ] PDF generators ship for phòng thi + giấy mời (templates in `module/document/pdf/templates/`)
- [ ] Audit log per state transition

## Related

- GAP-055 (báo cáo MOET — exam scores feed học bạ)
- GAP-058 (role hierarchy — approval chain)
- GAP-059 (conduct — affects lên lớp)
- GAP-061 (promotion logic — uses exam scores)

## Log

- 2026-05-04 — Filed by Wave 17 Bucket D. State-check: Assignment exists but no exam lifecycle.
