# GAP-947: TenantSettings entity missing — không có per-tenant timezone/fiscalYear/schoolType/locale/Năm học

**Status:** 🟢 DONE (2026-06-07 — live walk PASS)
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

- [x] Flyway migration `V90__create_tenant_settings.sql` exists (table + unique 1:1 index + RLS tenant_isolation policy)
- [x] `TenantSettings` entity + repository + service shipped (`kiteclass-core/module/tenantsettings/`)
- [x] `GET/PUT /api/v1/tenants/{id}/settings` endpoint returns/updates settings (controller + DTOs + mapper)
- [x] Default Năm học auto-fill at provision (Sep-May VN K-12 semantic — `AcademicYearCalculator`)

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 — 2026-06-07)

Live RST walk against shared Docker stack (kiteclass-core :8088), tenant `kc1walk3` (instance `b40eb7b0-...`), persona = Owner (`X-Tenant-Id` + `X-User-Id` headers, gateway-equivalent):

- **GET** `/api/v1/tenants/{id}/settings` → HTTP 200, auto-created defaults (`academicYear=2025-2026`, `timezone=Asia/Ho_Chi_Minh`, `locale=vi`, `schoolType=CENTER`). DB row created in `kiteclass_shared.tenant_settings`.
- **PUT** `/api/v1/tenants/{id}/settings` (academicYear `2026-2027` + timezone `Asia/Bangkok` + schoolType `K12` + phone + themeConfig jsonb) → HTTP 200, all fields applied; provided-field-wins merge confirmed (`locale` untouched stayed `vi`).
- **GET** re-fetch → HTTP 200, all changes persisted.
- **jsonb round-trip:** `theme_config` stored as Postgres `jsonb` (`jsonb_typeof = object`), content `{"darkMode":true,"logoPosition":"left","primaryColor":"#2563eb"}` — verified via psql.

**Bug surfaced + fixed this PR (Bug #1):** GET/PUT both returned HTTP 500 `ConstraintDeclarationException HV000151` — `TenantSettingsServiceImpl.updateSettings` redefined the `@Valid` param constraint from the interface, breaking Hibernate Validator method-validation for the whole `@Validated` bean (even GET failed). Fix: declare `@Valid` once on the interface `TenantSettingsService.updateSettings`, remove from impl. Re-walk PASS after kiteclass-core rebuild. Mockito/MockMvc unit tests missed it (no CGLIB `@Validated` proxy method-validation path); a Spring-context test would catch it.

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (matrix + benchmark)
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{failure-mode-matrix,external-benchmark}.md
- Flow Verification Campaign §4 row KC-1
- 3-layer docs: `documents/01-business/kiteclass/tenant-settings/{rules,use-cases,api-contract}.md` (§ TenantSettings GAP-947 appended)

## Log

- **2026-06-07** KC-1 closure walk: live GET/PUT/GET round-trip PASS on shared stack (see Walk evidence). Surfaced + fixed Bug #1 (`HV000151` `@Valid` redefinition — moved constraint to interface). jsonb themeConfig round-trip verified. Flipped 🟢 DONE; git mv → `closed/`. Per `feature-ship-runtime-walk-mandate.md` §3.
- **2026-06-06** Wave provisioning-1 Bucket F: shipped `TenantSettings` entity + `SchoolType` enum + `AcademicYearCalculator` (Năm học VN Sep→May) + repository + service + mapper + controller (`GET/PUT /api/v1/tenants/{id}/settings`) + DTOs + Flyway `V90__create_tenant_settings.sql` (1:1 unique index + RLS) + 3-layer business docs appended. Tenant isolation: controller guard (path id == X-Tenant-Id) + RLS policy + service uses TenantContext scope. 15 unit tests PASS (`./mvnw test` AcademicYearCalculatorTest 7 + TenantSettingsServiceTest 4 + TenantSettingsControllerTest 4 incl cross-tenant IDOR negative). PARTIAL 90% — live runtime walk deferred to stack-up session per `feature-ship-runtime-walk-mandate.md` (FEATURE_SHIP_WALK_DEFER).
