# GAP-302: Homework Receipt — Mobile UX (Student Secondary)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core, new homework module) + Frontend (student mobile app)
**Found:** 2026-05-04 (Wave 17 Bucket B — student-in-P2 secondary persona review)
**Affects:** Student secondary persona in P1, P2, P3 (P5 needs full LMS instead — out of scope)

---

## Problem

P2 student expects: after each class session, see a simple "homework receipt" card on mobile:

> HW: làm trang 45-46 SBT, nộp Thứ 6
> Tap "Đã làm" để mark hoàn thành (optional)

Currently:

- `grep -ri homework kiteclass-core/src/main/java` returns 0 matches
- No `homework` module exists
- Teacher gradebook has score columns but no "post-class assignment text" field

Student-in-P2 review evidence: AC-OPS-003 FAIL.

## Root Cause

Wave plans focused on attendance + grades + invoices; homework as separate concern was not modeled. The persona AC explicitly says "không full LMS — receipt-only" so this is small but missing.

## Proposed Fix

1. New entity `Homework`: id, sessionId, classId, teacherId, text (max 500 chars), dueDate, attachmentUrl (optional)
2. Endpoint `POST /api/sessions/{id}/homework` (teacher creates) + `GET /api/students/{id}/homework?upcoming=true`
3. Self-report endpoint `POST /api/homework/{id}/mark-done` (student sets `markedDoneAt`)
4. UI student dashboard: "Bài tập" tab — list cards with deadline countdown
5. Notification: when teacher posts homework → student + parent notified (depends on GAP-063 for Zalo)
6. Out-of-scope: file upload, online quiz, auto-grading (P5 LMS territory)

## Acceptance Criteria

- [ ] `Homework` entity + migration
- [ ] Teacher POST endpoint with role check
- [ ] Student GET endpoint returns homework for student's enrolled classes only (tenant-scoped)
- [ ] Self-report mark-done endpoint
- [ ] UI: ≤2 taps from home to "Today's homework"
- [ ] Mobile-friendly (Tailwind/shadcn responsive)
- [ ] Notification integration (depends on GAP-063)
- [ ] Test: teacher posts → student receives → student marks done → teacher sees status

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-OPS-003 (student secondary)
- Soft-depends on: [GAP-063](GAP-063-sms-zalo-notification-integration.md) for notification dispatch
- Out-of-scope reference: full LMS (P5 K-12 territory)
