# GAP-898: `BrandingJob` entity ↔ migration drift cả 2 chiều — triad anti-pattern

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH branding)
**Affects:** `kitehub-branding` entity `BrandingJob` vs `branding_jobs` table

## Problem

**Cột DB tồn tại nhưng entity KHÔNG khai** (V4+V31): `brand_personality` (+ CHECK 6 giá trị), `color_scheme`, `logo_analysis`, `theme_extracted`, `created_by`/`updated_by` VARCHAR(100), `deleted` BOOLEAN.

→ Service ghi qua entity sẽ để 6 cột ở NULL/default → feature "brand personality picker" + soft-delete coi như mất ở app-layer.

**Cột entity tồn tại nhưng DB KHÔNG có:**
- `language` VARCHAR(10) NOT NULL — entity khai báo, V4+V31 KHÔNG tạo. Persist entity → `column does not exist` runtime (ddl-auto=validate) hoặc bỏ qua field (ddl-auto=none).

Triad drift `design-patterns.md` §3.12. CI script `check-entity-mapper-consistency.sh` (WARN mode) đã có nhưng chưa HARD STOP.

## Proposed Fix

Migration V## add `language VARCHAR(10) NOT NULL DEFAULT 'vi'` + map vào entity các cột còn thiếu HOẶC drop entity columns không dùng. Decide brand_personality/color_scheme là canonical (DB) hoặc entity-only.

## Acceptance Criteria

- [ ] Migration V## reconcile entity↔DB
- [ ] Entity map all DB columns hoặc drop unused
- [ ] CI script `check-entity-mapper-consistency.sh` HARD STOP enabled
- [ ] Reference cluster doc KH 03-branding §A2

## Discovered in

`documents/02-architecture/database/kitehub/03-branding.md` §A2
