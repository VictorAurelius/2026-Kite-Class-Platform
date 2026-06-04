# GAP-947: TenantSettings entity missing — không có per-tenant timezone/fiscalYear/schoolType/locale/Năm học

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant settings) — bao gồm VN academic year (Năm học) requirement
**Defer-to:** After Wave flow-kh3 finish

## Problem

`grep -rn "TenantSettings\|tenant_settings" kitehub/ kiteclass/ --include="*.java"` returns 0 hits. `system_config.locale` là GLOBAL (`ProductionSeedRunner:130`), không phải per-tenant. Settings hiện scatter trong `Instance.contactEmail`, `Instance.organizationName` — KHÔNG có `timezone`, `fiscalYear`, `schoolType`, `address`, `logo`, `phone`, `academicYearStart`. Campaign §3 KC-1 list "edit tenant settings" + "default settings apply" nhưng BE code không tồn tại — walk sẽ 404 trên mọi settings endpoint. Per benchmark B1 (MISA QLTH R85+ pattern) + C2 recommendation: Năm học = required field tại provision (VN K-12 Sep YYYY → May YYYY+1). Surfaced: matrix A6×E2×EC7 + benchmark B1+C2.

## Proposed Fix

Tạo entity `TenantSettings` + table `tenant_settings` (1:1 với `instances`) với fields: `timezone` (Asia/Ho_Chi_Minh default), `locale` (vi default), `fiscalYear`, `academicYearStart`, `schoolType`, `address`, `phone`, `logoUrl`, `themeConfig`. Năm học default = current năm học (Sep YYYY → May YYYY+1) auto-compute. Controller + DTO + 3-layer business docs (`tenant-settings/{rules,use-cases,api-contract}.md` — VERIFY existing).

## Acceptance Criteria

- [ ] Flyway migration `V<N>__create_tenant_settings.sql` exists
- [ ] `TenantSettings` entity + repository + service shipped
- [ ] `GET/PUT /api/v1/tenants/{id}/settings` endpoint returns/updates settings
- [ ] Default Năm học auto-fill at provision (Sep-May VN K-12 semantic)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (matrix + benchmark)
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{failure-mode-matrix,external-benchmark}.md
- Flow Verification Campaign §4 row KC-1
