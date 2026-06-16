# GAP-1427: KC-5 grid điểm danh luôn 0 học viên (useState(initFn) dùng như useEffect)

**Status:** 🟢 DONE
**Priority:** P1
**Domain:** Frontend
**Found:** 2026-06-15 (KC-5/6/7/8/11 browser re-walk — Workflow 5 Opus agent qua nip.io)

## Problem

page.tsx auto-select session + seed attendanceRows dùng useState(()=>{}) → initializer chạy 1 lần lúc mount khi enrollments/sessions còn async chưa load → grid luôn rỗng. Fix: đổi 2 block sang useEffect với deps [sessions]/[enrollments.content].

## Related
- Discovered in: KC browser re-walk Workflow 2026-06-15 (goal "run hết flow cho dev G2"). Per discovery-to-gap-inline-filing.md.
