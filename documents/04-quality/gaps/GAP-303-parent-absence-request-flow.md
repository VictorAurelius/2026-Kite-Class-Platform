# GAP-303: Parent-Mediated Absence Request Flow

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core attendance + notification) + Frontend (parent portal)
**Found:** 2026-05-04 (Wave 17 Bucket B — student-in-P2 secondary persona review)
**Affects:** P2 Small Center, P3 Medium Center, P5 K-12 — every persona with active parent-mediated absence reporting

---

## Problem

Parent needs to report student absence ahead of time (e.g., "Ốm 2 ngày T2-T3"). Expected:

- Parent opens portal → "Báo nghỉ" → input dates + reason → submit
- Student app shows "Vắng có phép T2-T3" + makeup plan if owner schedules one
- Teacher's attendance pre-marked "Vắng có phép" (excused absence) instead of "Vắng" (unexcused)
- Owner receives notification

Currently:

- No `absence-request` or `parent-absence` module found in `kiteclass-core/src/main/java`
- Attendance is teacher-driven post-fact only; no advance excused absence

Student-in-P2 review evidence: AC-EDGE-002 FAIL.

## Root Cause

Attendance domain modeled as teacher input only. Anti-fraud (no student self-mark) was honored, but parent-side workflow not designed.

## Proposed Fix

1. New entity `AbsenceRequest`: id, studentId, classId(s), startDate, endDate, reason, requestedByParentId, status (PENDING/APPROVED/AUTO_APPROVED), createdAt
2. Endpoint `POST /api/absence-requests` (parent role) → defaults to AUTO_APPROVED for ≤2 days; PENDING for longer (owner manual approve)
3. Attendance pre-marking: when teacher opens session attendance → for students with active AbsenceRequest covering this session, default status = "EXCUSED_ABSENT"
4. UI parent portal: "Báo nghỉ" form + history view
5. Notification: owner receives "Phụ huynh A báo nghỉ con B 2 ngày" via GAP-063 channel
6. Audit log per request

## Acceptance Criteria

- [ ] `AbsenceRequest` entity + migration
- [ ] Parent POST endpoint with role check (parent of student only)
- [ ] Auto-approval for ≤2 day requests; longer requires owner approval
- [ ] Teacher attendance UI shows pre-marked "Vắng có phép" for students with active request
- [ ] UI parent portal: ≤4 fields; submit + confirmation
- [ ] Notification to owner (depends on GAP-063 for Zalo)
- [ ] Test: parent submits → teacher sees pre-marked → student app shows status

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-EDGE-002 (student secondary)
- Soft-depends on: [GAP-052](GAP-052-parent-portal.md) for parent UI surface
- Soft-depends on: [GAP-063](GAP-063-sms-zalo-notification-integration.md) for owner notification
- Cross-link: [GAP-186](GAP-186-child-protection-policy.md) — parent-mediated workflow consistent with policy
