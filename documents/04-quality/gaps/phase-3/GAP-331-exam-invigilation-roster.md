# GAP-331: Exam Invigilation Roster with Teacher Schedule Conflict Detection

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D — NEW from P5 §"NEW-3")
**Related:** P5-k12-school.md AC-OPS-008

## Current State (verified 2026-05-04)

No invigilation module. Phó CM làm thủ công Excel.

## Problem

Đợt thi cuối kỳ ~96 slot coi thi (8 buổi × 12 phòng). Conflict với lịch dạy của chính GV → GV bỏ thi đột xuất.

## Proposed Fix

1. **InvigilationRoster entity:** linked to ExamSession (GAP-328)
2. **Conflict check:** GV teaching schedule vs invigilation slot
3. **Auto-suggest replacement** GV when conflict detected
4. **Push notification** to GV with schedule

## Acceptance Criteria

- [ ] InvigilationRoster + conflict detection
- [ ] Auto-suggest replacement
- [ ] Teacher notification
- [ ] Test: assign 96 slots → conflicts flagged + suggestions provided
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** GAP-328 (ExamSession)
- **Wave plan:** Bucket D Stage 5

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
