# GAP-301: Student-app surface for tutoring-center scale (schedule view, attendance read-only, fee status, in-app inbox, sick-day report) + P2-shaped owner closure data export

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Feature-P1 — unblocks 6 student-in-P2 secondary ACs + 1 P2 owner exit AC)
**Domain:** Frontend / Backend
**Found:** 2026-05-04 (P2 Small Center persona review round 1)
**Persona blocked:** P2 Small Tutoring Center (specifically student-in-P2 secondary persona); also relevant to student-in-P3
**Wave:** TBD

## Problem

Existing `(dashboard)/students/` route is owner-CRUD (owner manages students), NOT a student-facing app. Student-in-P2 secondary AC doc enumerated 13 ACs; 6 of them FAIL purely because there is no student-facing surface to evaluate:

| Secondary AC | What's needed |
|---|---|
| AC-OPS-001 (student) | Student weekly schedule view (1-3 subjects), ≤2 taps from home |
| AC-OPS-002 (student) | Read-only attendance history (anti-fraud — student CANNOT self-mark) |
| AC-OPS-003 (student) | Homework receipt per session (text/link, NOT full LMS) |
| AC-OPS-004 (student) | Read-only grade view (gradebook entries) |
| AC-FIN-001 (student) | Read-only fee status ("Tháng 4: Đã đóng" / "Tháng 5: Chưa đóng") — NO Pay button for minor |
| AC-COMM-002 (student) | In-app inbox for class broadcast (sync with Zalo per GAP-063) |
| AC-EDGE-002 (student) | Parent-side sick-day report → student VIEW the status |

Plus one **owner-side** exit AC that needs the same data-export work:

| Owner AC | What's needed |
|---|---|
| AC-EXIT-003 (P2 owner) | Full tenant data export (xlsx + PDF) on closure within 7 days. Existing `DataExportService.java` is GDPR Art. 20 user-scope ZIP scaffold (own javadoc says scaffold) — not P2 owner closure shape. |

Components in `kiteclass-frontend/src/components/student/` exist (e.g., `dynamic-attendance-calendar.tsx`) but are not wired to a student app route.

## Root Cause

P2 student-app surface was not enumerated until secondary AC docs landed 2026-04-30 (GAP-153). Existing student-related code was built for K-12 student persona (P5), where a school-installed app with full LMS makes sense. P2 student-in-tutoring-center is a different shape — lighter, parent-mediated, no upload, no DM.

DataExportService scaffold targets GDPR user data portability, not tenant closure xlsx + PDF bundle.

## Proposed Fix

| Sub-task | Surface | Estimate |
|---|---|---|
| `(dashboard)/student/{me}/schedule` route — weekly grid for 1-3 subjects | Frontend | 0.5d |
| `(dashboard)/student/{me}/attendance` — read-only history (block POST/PUT) | Frontend + Backend authz | 0.5d |
| `(dashboard)/student/{me}/homework` — list latest assignment receipts; lightweight, no upload | Frontend | 0.5d |
| `(dashboard)/student/{me}/grades` — read-only | Frontend | 0.25d |
| `(dashboard)/student/{me}/fee-status` — read-only invoice list w/o Pay button | Frontend | 0.25d |
| `(dashboard)/student/{me}/inbox` — broadcast list synced with announcements | Frontend + Backend (announcement domain) | 1d |
| `POST /api/v1/students/{me}/leave-request` (parent-only RBAC) — student VIEWs result | Backend + Frontend | 1d |
| Tenant closure data-export: xlsx workbook (students/classes/attendance/grades/invoices/payments/commission) + PDF summary | Backend | 1.5d |

Announcement domain (broadcast persistence) does not currently exist (grep `announcement|broadcast` empty in kiteclass-core). New small module needed — could be co-shipped with GAP-063 or stand-alone.

## Acceptance Criteria

- [ ] Student log-in lands on a student-shaped home (NOT owner dashboard)
- [ ] Student cannot reach any owner/teacher route (RBAC test)
- [ ] Student attendance API rejects POST/PUT from student role
- [ ] Student fee-status view has NO "Pay now" button (anti-fraud — minor cannot trigger payment)
- [ ] Tenant closure export produces xlsx + PDF within 7 days of owner request
- [ ] AC-OPS-001/002/003/004 (student-in-P2), AC-FIN-001 (student), AC-COMM-002 (student), AC-EDGE-002 (student), AC-EXIT-003 (P2 owner) flip PASS in next P2 review

## Related

- Audit: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` §6 + §7
- Dependencies: GAP-063 (inbox-Zalo sync), GAP-186 (anti-fraud RBAC for minors)
- Reference AC: `documents/00-brd/persona-criteria/secondary/student-in-P2.md` (all 13 ACs)
- Existing scaffold: `kiteclass-core/src/main/java/com/kiteclass/core/module/retention/DataExportService.java` (GDPR-shape; tenant-closure shape is a sister file/method, not a replacement)
