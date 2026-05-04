# GAP-299: Substitute teacher attribution model (no-loss-of-history when teacher swaps in for one session)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Feature-P2 — edge case, lower frequency than commission/onboarding)
**Domain:** Backend / Data Model
**Found:** 2026-05-04 (P2 Small Center persona review round 1)
**Persona blocked:** P2 Small Tutoring Center, P3 Medium Center
**Wave:** TBD

## Problem

**AC-OPS-006 (P2 owner):** Owner-as-teacher takes over Teacher A's "Anh 12B" session for 1 day → marks attendance + uploads handout. Today there is no substitute concept:
- grep `substitute` in `kiteclass-core/src/main/java` returns 0 hits
- `ClassSession` entity has no `substitute_teacher_id` column
- Attendance + grade entries FK directly to the scheduled teacher_id

Owner workaround today: manually edit `class.teacher_id` for the affected session — but that mis-attributes commission for that session AND risks corrupting historical attribution if the owner forgets to revert.

## Root Cause

`ClassSession` was modelled as "a single occurrence of a class" with the implicit assumption the teacher is fixed by the parent class. Substitution wasn't enumerated until persona review.

## Proposed Fix

| Sub-task | Surface | Estimate |
|---|---|---|
| Add nullable `substitute_teacher_id` (FK to teacher) + `substitute_reason` to `ClassSession` | Backend + migration | 0.5d |
| Effective-teacher resolver: `session.effectiveTeacher() = substituteTeacherId ?? class.teacherId` | Backend | 0.25d |
| Attendance + grade write paths use `effectiveTeacher()` for attribution | Backend | 0.25d |
| Commission engine (GAP-057) consumes `effectiveTeacher()` per session | Backend | (covered by GAP-057 spec) |
| Schedule UI shows substitute indicator + override in session detail | Frontend | 0.5d |

Substitute commission policy (config key, default = "credit substitute, not original") TBD with finance — call out in `documents/01-business/kiteclass/clazz/rules.md`.

## Acceptance Criteria

- [ ] `class_sessions` table has `substitute_teacher_id` column + Flyway migration
- [ ] Adding a substitute to a future session does not modify the parent class
- [ ] Attendance taken during a substituted session attributes to the substitute teacher
- [ ] Commission (when GAP-057 ships) credits substitute per config policy
- [ ] AC-OPS-006 (P2 owner) flips PASS in next P2 review

## Related

- Audit: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` §2
- Dependency: GAP-057 (commission consumes effective teacher)
- Reference AC: `documents/00-brd/persona-criteria/P2-small-center.md` AC-OPS-006
