# GAP-1426: KC-5 FE bulk-mark gọi sai endpoint /attendance/bulk → 405 (UI điểm danh hỏng hoàn toàn)

**Status:** 🟢 DONE
**Priority:** P0
**Domain:** Frontend
**Found:** 2026-06-15 (KC-5/6/7/8/11 browser re-walk — Workflow 5 Opus agent qua nip.io)

## Problem

Nút 'Lưu điểm danh' gọi POST /api/v1/attendance/bulk (chỉ có trong README) → 405. BE thật: POST /api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance. Fix: markBulkAttendance(classId, data) + đúng path; sweep hook useMarkBulkAttendance + page caller pass classId. Browser-verified sau fix.

## Related
- Discovered in: KC browser re-walk Workflow 2026-06-15 (goal "run hết flow cho dev G2"). Per discovery-to-gap-inline-filing.md.
