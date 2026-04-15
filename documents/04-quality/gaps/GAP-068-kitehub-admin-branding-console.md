# GAP-068: KiteHub Admin AI-Branding Console

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Admin / AI Branding / Product
**Detected:** 2026-04-14 (post-Wave-2 stakeholder review)

## Problem

KiteHub AI Branding hiện chỉ có **customer-facing** UI (`/branding/*`). Admin side = **ZERO** branding management views.

Platform ops không thể:

- Xem tất cả tenant brandings ở một chỗ
- Visual preview grid để spot-check quality
- Regenerate branding on-behalf-of (support use case)
- CRUD template library (hiện templates seed ad-hoc)
- Phân tích template usage + AI cost per tenant
- Override quality gate cho edge cases
- Approve/reject brandings flagged bởi moderation

## Evidence

```
kitehub/kitehub-frontend/src/app/(admin)/admin/
├── instances/          ← exists
├── payments/           ← exists
├── revenue/            ← exists
└── page.tsx            ← dashboard
```

**No `branding/` folder under admin.**

```
kitehub/kitehub-frontend/src/app/(customer)/branding/
├── assets/             ← tenant-self-serve
├── templates/          ← tenant picks from seeded library
├── wizard/             ← tenant generates
└── page.tsx            ← tenant current branding
```

Admin có thể impersonate tenant (GAP-040) để xem branding từng tenant — nhưng không có bird's-eye view.

## Proposed Fix

### Route: `/admin/branding`

**Tabs:**

1. **Instances** — list tất cả tenant brandings
   - Visual preview tile grid (12 per page)
   - Columns: tenant name, tier, status (DEPLOYED/FAILED/REGENERATING), brandingVersion, lastDeployedAt
   - Filters: tier / status / quality score / flagged
   - Click tile → deep-dive instance branding detail
   - Bulk: re-run quality gate / force regenerate (with reason)

2. **Templates** — library CRUD
   - Grid of all templates with metadata (type, category, usage count)
   - Upload new template (SVG + meta)
   - Deprecate template (soft delete + migration path for in-use)
   - A/B template experiments (ties to GAP-022)
   - Quality review per template (5 criteria from AI branding rule §8)

3. **Analytics** — cross-tenant insights
   - Template usage heatmap
   - AI cost per tier (daily/weekly/monthly)
   - Regenerate distribution (free tier hitting limit?)
   - Quality gate fail rate over time
   - Top failed steps (drive improvement)

4. **Quality Moderation** — ties to GAP-023 + GAP-018
   - Queue of flagged brandings (content safety)
   - Side-by-side before/after for rebrand review
   - Approve / reject / request-changes
   - Audit trail per moderation decision

5. **Admin Actions** — emergency tooling
   - Manual override for quality gate (with required reason + audit)
   - Force template migration cho batch of tenants
   - Emergency stop: pause all AI generation (incident response)
   - Rebrand on-behalf (impersonation hook → triggers rebrand in tenant's name)

## Acceptance Criteria

- [ ] `/admin/branding` route with 5 tabs above
- [ ] Backend endpoints `/api/v1/admin/branding/*` (require ADMIN + specific permission)
- [ ] Instance list ≤ 1s load p95 (pagination + indexed queries)
- [ ] Visual preview ≤ 2s per tile (cached thumbnails)
- [ ] Bulk regenerate: throttled ≤ 10 concurrent per tier
- [ ] All admin actions recorded in audit trail + notified to tenant (configurable)
- [ ] 3-layer docs: `01-business/kitehub/admin-branding-console/`
- [ ] E2E Playwright: admin review flagged → approve → audit recorded
- [ ] Tier gating: template CRUD requires SUPER_ADMIN; regenerate-on-behalf requires SUPPORT+

## Dependencies

- GAP-008 (AI Agent workflow) → Wave 3 — gives regenerate API
- GAP-011 (template library curation) — underlying template data model
- GAP-012 (quality review) — feeds the Moderation tab
- GAP-023 (admin moderation tools) — substantial overlap, may merge
- GAP-040 (impersonation) — linked from admin actions
- GAP-022 (template analytics + A/B) — feeds Analytics + Templates tabs

**Consolidation opportunity:** GAP-068 + GAP-023 likely implemented together (shared UI, shared permission model). Track separately for clarity, merge in Sprint planning.

## Target Wave

**Wave 8 Admin & Support** (Sprint 6+).

Scope overlaps enough với GAP-023 / GAP-040 / GAP-022 to warrant a dedicated "Admin Console" workstream spanning ~3 weeks.

## Log

- 2026-04-14 — Detected during stakeholder review; platform ops flagged as blocker for maintaining branding quality at scale
