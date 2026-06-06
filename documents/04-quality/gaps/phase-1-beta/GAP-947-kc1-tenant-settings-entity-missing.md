# GAP-947: TenantSettings entity missing — không có per-tenant timezone/fiscalYear/schoolType/locale/Năm học

**Status:** 🟡 PARTIAL (90%)
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

## Remaining (10% — why PARTIAL not DONE)

- [ ] Live runtime walk on production-equivalent Docker stack per `feature-ship-runtime-walk-mandate.md` §1 (user-facing feature). Deferred — agent worktree không có Docker stack; wave plan §5 lên lịch live walk khi stack up + pre-walk persona simulation. Code path covered by 15 unit tests (service 4 + controller 4 authz incl cross-tenant IDOR negative + util 7). Coordinator flip DONE sau live walk.

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (matrix + benchmark)
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{failure-mode-matrix,external-benchmark}.md
- Flow Verification Campaign §4 row KC-1
- 3-layer docs: `documents/01-business/kiteclass/tenant-settings/{rules,use-cases,api-contract}.md` (§ TenantSettings GAP-947 appended)

## Log

- **2026-06-06** Wave provisioning-1 Bucket F: shipped `TenantSettings` entity + `SchoolType` enum + `AcademicYearCalculator` (Năm học VN Sep→May) + repository + service + mapper + controller (`GET/PUT /api/v1/tenants/{id}/settings`) + DTOs + Flyway `V90__create_tenant_settings.sql` (1:1 unique index + RLS) + 3-layer business docs appended. Tenant isolation: controller guard (path id == X-Tenant-Id) + RLS policy + service uses TenantContext scope. 15 unit tests PASS (`./mvnw test` AcademicYearCalculatorTest 7 + TenantSettingsServiceTest 4 + TenantSettingsControllerTest 4 incl cross-tenant IDOR negative). PARTIAL 90% — live runtime walk deferred to stack-up session per `feature-ship-runtime-walk-mandate.md` (FEATURE_SHIP_WALK_DEFER).
