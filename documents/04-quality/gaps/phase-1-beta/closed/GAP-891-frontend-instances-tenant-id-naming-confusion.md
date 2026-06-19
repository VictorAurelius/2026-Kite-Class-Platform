# GAP-891: `frontend_instances` có cả `instance_id UUID` + `tenant_id VARCHAR` — naming confusion

**Status:** 🟢 DONE (wave-gap-audit-p1-1 2026-06-19 — substantive work shipped + CI-verified; residual cosmetic doc-ref/AC-checkbox only per verify pass)
**Priority:** 🟢 P3
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC branding/marketing)
**Affects:** `kiteclass-core` module branding; `frontend_instances` table

## Problem

V31 tạo `frontend_instances` với 2 cột định danh tenant khác kiểu:

- `instance_id UUID NOT NULL` — primary tenant identifier (RLS filter)
- `tenant_id VARCHAR(100) NOT NULL` — string slug/human-readable ID (FE deploy lookup, cross-service ref tới KiteHub `instances.slug`)

Confusion: 2 cột cùng "tenant" prefix nhưng 2 thứ khác. Comment migration không nói rõ.

## Proposed Fix

Migration rename `tenant_id` → `tenant_slug`. Update entity field name. Add JavaDoc + DB comment explain purpose.

## Acceptance Criteria

- [ ] Migration V## rename column
- [ ] Entity field rename
- [ ] DB COMMENT ON COLUMN
- [ ] Reference cluster doc 08-branding-marketing §A6

## Discovered in

`documents/02-architecture/database/kiteclass/08-branding-marketing.md` §A6
