# GAP-296: Substitute Teacher Attribution + Commission

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core) + Frontend (admin/owner UI)
**Found:** 2026-05-04 (Wave 17 Bucket B — P2 persona review)
**Affects:** P1 Solo Teacher (occasional sub), P2 Small Center (regular sub), P3 Medium Center (frequent sub), P5 K-12 (substitute teacher pool)
**Reserved range:** GAP-296..305 (Wave 17 Bucket B — P2)

---

## Problem

P2 Small Center reality: 5-10% of sessions need a substitute (giáo viên chính ốm, owner thay tạm, GV thuê đi học). Current system has no "substitute teacher" concept:

- Attendance ghi cứng vào `scheduled_teacher_id`. Nếu owner thay GV A teaching → attendance vẫn attribute cho GV A.
- Commission attribution wrong (when GAP-057 lands): paid GV A despite owner doing the work.
- No audit trail of who actually taught the session.

P2 review evidence: AC-OPS-006 FAIL — `grep -ri substitute` returns 0 hits in `kiteclass-core/src/main/java/com/kiteclass/core/module/`.

## Root Cause

Schema design omitted substitute case. `ClassScheduleSlot.java` has only `teacher_id` (the assigned one). No `actual_teacher_id` or `substitution_log` table.

## Proposed Fix

**Option A (light):** Add `actual_teacher_id` (nullable) to `class_session` row. If null → assigned teacher taught. If set → substitute. Commission engine reads `COALESCE(actual_teacher_id, teacher_id)`.

**Option B (audit-heavy):** New `session_substitution` table tracking original teacher, substitute, timestamp, reason, owner approval. Required for K-12 (P5) audit but overkill for P2.

**Recommendation:** Ship Option A first (P2/P1 sufficient). Option B as P5-only follow-up gap if K-12 audit demands.

ADR needed: state who is allowed to assign substitute (owner only? scheduled teacher?).

## Acceptance Criteria

- [ ] Schema: nullable `actual_teacher_id` on `class_session` (or equivalent attendance row)
- [ ] Service: `SessionService.assignSubstitute(sessionId, substituteTeacherId, reason)` with role check (Owner role required for P2)
- [ ] UI: Owner can mark substitute on session detail screen (mobile-friendly, ≤2 taps)
- [ ] Attendance + grade entries recorded under substitute teacher (when GAP-057 lands, commission credits substitute)
- [ ] Audit log entry on each substitution with original + substitute + reason
- [ ] Test: substitute persists; original teacher still visible in history view; commission test (when GAP-057 ready) credits substitute

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-OPS-006, AC-EXIT-002
- Depends on: [GAP-057](GAP-057-payroll-teacher-commission.md) for commission attribution
- Cross-persona: should re-verify in P1, P3, P5 reviews (Buckets A/C/D parallel)
- Wave plan: [wave-2026-05-04-persona-review-round-1.md](../../03-planning/waves/wave-2026-05-04-persona-review-round-1.md)
