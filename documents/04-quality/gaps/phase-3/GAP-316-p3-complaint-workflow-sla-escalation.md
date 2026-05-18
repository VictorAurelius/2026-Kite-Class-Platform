# GAP-316: Complaint Workflow with SLA Tracking + Auto-Escalation

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core complaint module new) + Frontend (parent portal + admin dashboard)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 3 ACs across tenant + admin (giám đốc + lễ tân) + payment dispute

---

## Problem

P3 với 250 students × 2 parents = 500 contacts. Complaint volume scale up. Workflow:
1. Parent submit complaint qua portal hoặc lễ tân log call
2. Auto-categorize (academic / financial / safety / behavioral)
3. Auto-route safety → giám đốc immediate; others → lễ tân first
4. SLA timer 48h start; auto-escalate nếu vượt
5. Resolution log captured (action, parent response, closure date)
6. Payment dispute extension (refund flow + audit log)

## Root Cause

Không có module complaint — `find -iname "*complaint*"` → 0 results. Email rời rạc không trace được, no SLA tracking.

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Complaint entity | — | ❌ missing |
| ComplaintCategory enum | — | ❌ missing |
| SLA timer service | — | ❌ missing |
| Auto-escalation rules | — | ❌ missing |
| Parent portal complaint form | — | ❌ missing |
| Admin complaint queue dashboard | — | ❌ missing |
| Payment dispute extension | — | ❌ missing |

## Proposed Fix

1. `Complaint` entity (parent_id, category, description, evidence_urls, sla_due_at, status, resolution)
2. `ComplaintRoutingService.route()` based on category + escalation rules
3. SLA timer scheduler with auto-escalate notification (depends GAP-063)
4. Frontend parent portal: submit form
5. Frontend admin: queue view with filter (category / SLA status / unresolved)
6. Payment dispute sub-workflow with refund/credit + audit log

## Acceptance Criteria

- [ ] Complaint entity with 4 categories + status transitions
- [ ] Auto-route safety → giám đốc within 1 phút
- [ ] SLA 48h timer with notification at 24h + 36h + 48h
- [ ] Auto-escalate to giám đốc if lễ tân doesn't acknowledge in 24h
- [ ] Resolution log captures action + parent response + timestamp
- [ ] Payment dispute extension: refund/credit applied with audit log
- [ ] Parent portal form mobile-friendly; supports evidence file upload
- [ ] Admin queue dashboard with filters + bulk actions

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-COMM-004 | Tenant Director | `P3-medium-center.md` |
| AC-EDGE-003 | Tenant Director | `P3-medium-center.md` (payment dispute) |
| AC-OPS-006 | Admin (giám đốc) | `secondary/admin-in-P3.md` |

## Related

- Existing: GAP-052 (parent portal — extends with complaint form)
- Depends on: GAP-063 (Zalo notification for SLA alerts)
- Persona review: §2 (Tenant AC-COMM-004, AC-EDGE-003), §4 (Admin AC-OPS-006)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
