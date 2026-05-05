# GAP-271: Track 2 Port — kitehub-admin → production Next.js (NEW K-12 Principal scope)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — Tier 1 P5 K-12 School Principal persona)
**Domain:** Frontend
**Found:** 2026-04-29
**Affects:** `kitehub-frontend/src/app/(admin)/` — platform admin + K-12 principal routes

## Problem

HTML prototype `kitehub-admin/` (avg 107.2/128, 12 dense-desktop screens, R3 PR #703) covers P5 K-12 School Principal persona — large displays, 50+ teachers, 500-3000 students scale, MoET-compliant report cards. Production `(admin)/` group exists but covers platform admin (KiteHub ops) — does NOT cover K-12 principal scope.

## Current State (verified 2026-04-29)

`kitehub-frontend/src/app/(admin)/` exists for platform admin. K-12 principal route group MAY need separate `(school-admin)` route or extension of existing admin group with persona switcher.

## Proposed Fix

Port 12 dense-desktop screens for P5 K-12 School Principal.

**Scope:**
- School overview (KPIs: enrollment, attendance, fee collection, conduct flags)
- Bulk student import (G1 component — enrollment week 500/day scale)
- Teacher management (50+ teachers list + assign-to-class + role hierarchy)
- Academic calendar (semester/term + holidays + exam weeks)
- Report card generation (G3 + G10 + MoET compliance stamp)
- Parent communication monitor (escalation queue + SLA timer)
- Annual fees panel
- Conduct/behavior tracking (5-step escalation ladder)
- Multi-class roster (class × subject × teacher matrix, 25×9)
- School profile + settings
- Empty states + Login

**Tech direction:**
- Decision needed: separate `(school-admin)` route group OR extension of `(admin)` with persona switcher
- ⌘K command palette (per dossier P5 power-user spec)
- Hierarchy breadcrumb: school → semester → class
- Dense tables with sortable + sticky-header + bulk-select + pagination
- MoET compliance indicator on report card screen (legal requirement)

## Acceptance Criteria

- [ ] All 12 screens ≥105/128
- [ ] Decision documented: route group naming + persona auth boundary
- [ ] Bulk import G1 component handles 500-row CSV (post-GAP-273)
- [ ] G3 gradebook + G4 schedule + G8 attendance calendar imported
- [ ] MoET compliance stamp visible on report card output
- [ ] Hierarchy breadcrumb on all nested screens
- [ ] ⌘K palette accessible
- [ ] Real K-12 mock data (Trường THCS Nguyễn Du, 1247 HS, etc.)
- [ ] Vietnamese-only
- [ ] WCAG AA preserved

## Related

- HTML prototype: `ui_kits/kitehub-admin/` (Wave Round 3 PR #703)
- Component dependencies: GAP-273 (G1, G3, G4, G8, G10)
- Cluster 4 KH admin sister gaps: GAP-066/067/068 (existing platform admin scope, oversized — see ROADMAP)
- **External review (Wave 20 Bucket B, 2026-05-05):** `documents/04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md` — avg **101.1/128** APPROVE WITH POLISH
- **🚫 BLOCKER:** [GAP-364](GAP-364-kitehub-admin-polish-school-profile-rebuild.md) (P2 — school-profile rebuild + 5 medium-priority polish items) MUST close before Track 2 port avg-floor compliance
- Parent quality-gate gap: [GAP-348](GAP-348-round-3-ui-kits-persona-driven-review.md) (🟡 PARTIAL)

## Effort estimate

~2 weeks (dense data + new persona route + hierarchy nav). Wave-pack candidate.

## Log

- **2026-04-29:** Filed after user accepted Round 3 quality.
