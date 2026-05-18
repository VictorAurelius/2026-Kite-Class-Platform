# GAP-308: RBAC Granular Gating + Audit Log on Unauthorized 403

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — security + compliance + privacy critical
**Domain:** Backend (kiteclass-core role module + gateway) + Frontend (sidebar guard)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 6 ACs across admin + teacher personas

---

## Problem

P3 có 4 admin roles distinct (giám đốc / lễ tân / kế toán / ops admin) + teacher role. RBAC gating phải:

1. **Sidebar guard** — lễ tân login chỉ thấy 4 modules được phép (enrollment, attendance lookup, parent contact, class schedule); KHÔNG thấy financial / payroll / complaint admin
2. **URL gating** — direct access `/admin/payroll` từ lễ tân → 403 Forbidden
3. **Audit log unauthorized attempt** — mọi 403 được log với who/when/path/source IP cho security audit
4. **Welcome tour distinct per role** — mỗi role first login thấy onboarding tour cho role đó (không generic)
5. **Module-level permission preset** — wizard tạo admin với role dropdown auto-applies permission preset (không phải tạo từng permission một)

**Without granular gating + audit log:** lễ tân có thể leak payroll → kế toán bị phạt vi phạm Luật Lao động (lương private); cũng vi phạm Luật An ninh mạng 2018 nếu access pattern không log được.

## Root Cause

Module RBAC scaffold tồn tại nhưng granular gating + audit log chưa implement:
- `kiteclass-core/src/main/java/com/kiteclass/core/module/role/` chỉ có 7 files (Role/Permission/UserRole entity + repository + RoleService)
- Không có `RolePermissionPreset` cho 4 P3 roles
- Không có `UnauthorizedAccessAuditLog` entity
- Không có sidebar gate component cho frontend
- Không có welcome-tour-per-role flow

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Role entity | `kiteclass-core/.../module/role/entity/Role.java` | ✅ exists |
| Permission entity | `kiteclass-core/.../module/role/entity/Permission.java` | ✅ exists |
| UserRole entity | `kiteclass-core/.../module/role/entity/UserRole.java` | ✅ exists |
| RoleService | `kiteclass-core/.../module/role/service/RoleService.java` | ✅ exists (likely CRUD only) |
| RolePermissionPreset cho 4 P3 roles | — | ❌ missing |
| UnauthorizedAccessAuditLog entity | — | ❌ missing |
| URL gating @PreAuthorize per controller | — | ⚠️ unknown — needs verification |
| Sidebar gate FE component | — | ❌ missing |
| Welcome-tour-per-role | — | ❌ missing |
| 403 → audit log filter | — | ❌ missing |

## Proposed Fix

**Phase 1 — Permission preset + module gating (Wave 18-G):**
1. `RolePermissionPreset` entity + seed data cho 4 P3 roles + teacher
2. Apply `@PreAuthorize("hasPermission(...)")` consistently across all controllers
3. Frontend: sidebar component reads user permissions, hides forbidden modules

**Phase 2 — Audit log unauthorized (Wave 18-H):**
1. `UnauthorizedAccessAuditLog` entity (timestamp, userId, path, sourceIp, role, denyReason)
2. Spring Security access denied handler → log entry
3. Admin dashboard "Security audit log" view với filter (last 7d / unresolved attempts / per user)

**Phase 3 — Welcome tour per role (Wave 18-I):**
1. First-login flag per role
2. React tour component với steps tailored per role

## Acceptance Criteria

- [ ] 4 RolePermissionPreset seeded: GIAM_DOC / LE_TAN / KE_TOAN / OPS_ADMIN with distinct permission sets
- [ ] Lễ tân direct URL access `/admin/payroll` → 403 + audit log entry
- [ ] Lễ tân sidebar shows only 4 allowed modules; financial/payroll/complaint hidden
- [ ] Admin dashboard "Security log" displays last 100 unauthorized attempts with filter
- [ ] First login per role triggers welcome tour distinct per role (giám đốc tour ≠ lễ tân tour)
- [ ] Audit log entries immutable (WORM-style — see GAP-319)
- [ ] Test: simulate 100 unauthorized 403 attempts → audit log captures all 100 with correct metadata

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-ONBOARD-001 | Admin | `secondary/admin-in-P3.md` |
| AC-ONBOARD-002 | Admin | `secondary/admin-in-P3.md` (lễ tân 403 verification — primary) |
| AC-ONBOARD-003 | Admin (kế toán financial dashboard) | `secondary/admin-in-P3.md` |
| AC-OPS-002 | Admin | `secondary/admin-in-P3.md` |
| AC-EXIT-001 | Admin | `secondary/admin-in-P3.md` |
| AC-ONBOARD-001 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |

## Related

- Existing: GAP-058 (role hierarchy + RBAC — this gap is the audit-log delta + 4-role preset extension)
- Persona review: [`documents/00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md) §Finding 3
- Depends on: GAP-319 (WORM audit log) for immutability layer
- Compliance laws: Luật An ninh mạng 2018, Luật Bảo vệ Dữ liệu Cá nhân (PDPL 2023)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C. Existing RBAC scaffold scope verified; granular gating + audit-log-on-403 + 4-role preset + welcome-tour-per-role missing. 6 ACs blocked.
