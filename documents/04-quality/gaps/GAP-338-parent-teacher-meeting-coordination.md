# GAP-338: Parent-Teacher Meeting Coordination + Calendar.ics + RSVP + Biên bản

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend + Frontend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-COMM-005

## Current State (verified 2026-05-04)

No meeting coordination module. GVCN tự gửi giấy mời + collect tay.

## Problem

Họp PH 4 lần/năm. GVCN tạo lịch + mời 42 PH RSVP + ghi biên bản + share post-meeting. Without: chaotic, không tracking.

## Proposed Fix

1. **Meeting entity:** title, datetime, location, agenda, organizer_id, audience (class/grade/school)
2. **calendar.ics generation** + email/SMS attach
3. **RSVP tracking:** Tham gia / Không / Cử người
4. **Biên bản digital:** post-meeting upload + share to absent PH

## Acceptance Criteria

- [ ] Meeting entity + RSVP
- [ ] calendar.ics in invitation
- [ ] Biên bản upload + share
- [ ] Test: 42 PH invited → RSVP + meeting + biên bản accessible to all
- [ ] business-logic-review.md 5-attribute (Source: TT 32/2020 quy định họp PH)

## Related

- **Depends on:** GAP-321 (parent portal)
- **Wave plan:** Bucket D Stage 4

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
