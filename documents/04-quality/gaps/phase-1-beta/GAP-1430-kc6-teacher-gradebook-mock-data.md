# GAP-1430: KC-6 teacher gradebook FE wired vào MOCK data, không gọi /api/v1/grades → không có UI nhập điểm thật

**Status:** 🔵 OPEN
**Priority:** P1
**Domain:** Frontend
**Found:** 2026-06-15 (KC-5/6/7/8/11 browser re-walk — Workflow 5 Opus agent qua nip.io)

## Problem

(teacher)/teacher/grades/[classId]/page.tsx import TEACHER_PROFILE/buildSampleStudents từ teacher-mock-data. BE grade flow sạch (calculate 88/B+/3.30, finalize, transcript, stats verified) nhưng không có FE surface thật → dev G2 không nhập điểm qua UI được. Cần wire FE vào grades API (FE-completion, defer).

## Related
- Discovered in: KC browser re-walk Workflow 2026-06-15 (goal "run hết flow cho dev G2"). Per discovery-to-gap-inline-filing.md.
