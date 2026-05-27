---
audience: dev
---

# GAP-778 — Plan §3 vs KC actual route naming drift (`/finance` + `/reports`)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Meta
**Found:** 2026-05-27 (Wave 106 RST Mảng B4 + B10 + B11 probe)
**Affects:** Wave 106 plan §3 B10 + B11 expected nav vs actual routes
**Phase:** phase-1-beta

## Problem

Wave 106 plan §3 implicit expects (per B10 "Thu chi" + B11 "Báo cáo"):
- `/finance` — Thu chi page
- `/reports` — Báo cáo standalone

Probe result:
```
KC /finance: 404
KC /reports: 404
KC /billing: 200     ← actual Thu chi route
KC /attendance/reports: 200    ← only nested reports
```

Plan-vs-code drift. Một trong 2 phải sync:
1. Plan §3 update: B10 → "Thu chi (/billing)" + B11 → "Báo cáo (defer Phase 1.5+ — chỉ có attendance reports nested)"
2. Code add: `/finance` route alias HOẶC `/reports` top-level page

## Root Cause

Plan §3 viết theo persona-level concept ("Thu chi" = generic financial concept), code dùng SaaS convention `/billing`. Không ai update bidirectional sync.

## Proposed Fix

Option A — Update plan §3 only (zero code change):
- B10 expected route: `/billing` (actual)
- B11 mark "Báo cáo doanh thu defer — chỉ có /attendance/reports trong Phase 1 BETA"

Option B — Add route aliases (`/finance` redirect → `/billing`):
- `next.config.js` redirects để legacy plan refs không 404
- Vẫn keep `/billing` canonical

Option C — Hybrid: A (plan sync) + Option C cho `/reports` standalone via GAP-775

## Acceptance Criteria

- [ ] Decision logged
- [ ] Plan §3 §"Bằng chứng kiểm tra trạng thái" updated với verified route names
- [ ] RST re-walk: nav links match plan expected routes

## Related

- Wave 106 plan §3 §"Bằng chứng kiểm tra trạng thái" — đã verify `/dashboard` `/students` `/teachers` `/courses` `/classes` `/attendance` nhưng skip `/finance` + `/reports` verify
- Sister: GAP-775 (no top-level ReportController) — affects Option C decision
- Per `audit-to-gap-pipeline.md` §2.6 wave-plan state-check — plan §3 nên có precise route names trong State-Check Evidence table
