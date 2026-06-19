# GAP-1428: KC-5 GET attendance stats không yêu cầu auth → leak data tenant (cùng class GAP-1031 gateway pass-through)

**Status:** 🟢 DONE (wave-phase1-close2 2026-06-19 — see Log)
**Priority:** P1
**Domain:** Backend
**Found:** 2026-06-15 (KC-5/6/7/8/11 browser re-walk — Workflow 5 Opus agent qua nip.io)

## Problem

UNAUTH GET /api/v1/attendance/stats/student/{id} → 200 + data thật (totalSessions/present...). getStudentStats KHÔNG @PreAuthorize; gateway pass-through-on-missing-token. Fix: thêm @PreAuthorize hasAnyRole(STAFF) or @authz.hasAccessToStudent. Batch chung sweep IDOR/gateway GAP-1015/1019/1023/1025/1031.

## Related
- Discovered in: KC browser re-walk Workflow 2026-06-15 (goal "run hết flow cho dev G2"). Per discovery-to-gap-inline-filing.md.


## Log — 2026-06-19 (wave-phase1-close2, state-check DONE)

@PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToStudent") present (AttendanceController L236); runtime unauth GET → 400 (no 200+data leak), auth → 200. Swept with IDOR/gateway batch GAP-1015/1019/1023/1025/1031.

Status → DONE per gap-done-discipline §2 (AC verified at code/runtime level). G2 browser walk = coordinator follow-up where applicable.