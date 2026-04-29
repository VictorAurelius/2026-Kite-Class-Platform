# GAP-278: Track 2 Coverage — KH platform admin kit (KH ops, NOT K-12 Principal)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — KiteHub internal staff persona, NOT customer-facing)
**Domain:** Frontend / Design System
**Found:** 2026-04-29 via audit §2.5
**Affects:** `kitehub-frontend/src/app/(admin)/**` + `kitehub-frontend/src/components/admin/**`

## CRITICAL CLARIFICATION

This GAP is for **KH platform-ops viewpoint** — KiteHub internal staff managing tenants/payments/revenue/instances across the SaaS.

This is **DIFFERENT** from `kitehub-admin/` HTML kit (Wave Round 3 PR #703) which targets **P5 K-12 School Principal** — an institutional tenant persona with completely different surface (academic-calendar, bulk-import, conduct, fees, report-cards, multi-class-roster).

Both kits coexist. GAP-271 ports kitehub-admin K-12 kit to NEW route. GAP-278 (this) ports/redesigns existing platform-admin route.

## Current State (verified 2026-04-29)

| Path | Type | Status |
|------|:----:|:------:|
| `(admin)/admin/page.tsx` | KH ops dashboard | exists, ~52/128 R1 |
| `(admin)/admin/instances/page.tsx` | Tenant instances list | exists, R1 |
| `(admin)/admin/instances/[id]/page.tsx` | Tenant instance detail | exists, R1 |
| `(admin)/admin/payments/page.tsx` | Cross-tenant payments | exists, R1 |
| `(admin)/admin/revenue/page.tsx` | Platform revenue analytics | exists, R1 |
| `(admin)/layout.tsx` | Platform admin shell | exists |
| `components/admin/AdminInstancesTable.tsx` | Admin table primitive | exists |
| `components/admin/AdminPaymentsTable.tsx` | Admin table primitive | exists |

## Proposed Fix

Create `ui_kits/kitehub-platform-admin/` HTML kit:

**Screens (~7-10):**
- KH ops dashboard (cross-tenant KPIs: total tenants / MRR / churn / instances by tier)
- Tenant instances list (multi-column with tier filter + suspend/resume bulk action)
- Tenant instance detail (subscription history + branding state + audit log + force-rebrand action)
- Cross-tenant payments (filter by status + bulk reconcile action)
- Platform revenue analytics (charts: MRR / ARR / cohort retention / churn)
- KH ops layout (different shell from `kitehub-pro-v2` customer shell)
- Empty states (no tenants / no payments / first day post-launch)

**Tech direction:**
- Density-heavy desktop (KH staff are power users, not casual)
- ⌘K palette across all admin pages
- Differentiated visual treatment from customer-facing surface (e.g., subtle "KH OPS" indicator)
- Reuse G9 (instance lifecycle) + G10 (payment timeline) components from GAP-273

## Acceptance Criteria

- [ ] HTML kit ≥105/128 across screens
- [ ] Visually distinct from `kitehub-admin/` K-12 Principal kit
- [ ] Visually distinct from `kitehub-pro-v2/` customer kit
- [ ] G9 + G10 components imported (post-GAP-273)
- [ ] Cross-tenant queries scope clear (KH staff sees ALL tenants vs customer sees their own)
- [ ] Vietnamese-only KH staff data (mock realistic ops scenarios)
- [ ] Production ported ≥105/128
- [ ] WCAG AA preserved

## Related

- Audit evidence: §2.5
- Sister gap (different scope, same `(admin)` group): GAP-271 (K-12 Principal kit port)
- Decision needed: separate `(school-admin)` route group OR persona switcher in shared `(admin)`

## Effort estimate

~1-2 weeks. Wave-pack candidate.

## Log

- **2026-04-29:** Filed from audit synthesis. CRITICAL clarification: distinct from kitehub-admin K-12 kit.
