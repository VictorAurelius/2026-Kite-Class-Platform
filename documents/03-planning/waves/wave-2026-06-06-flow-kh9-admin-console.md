---
title: Wave flow-kh9 — KH-9 Admin console G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kh9]
wave: wave-2026-06-06-flow-kh9
tag_primary: flow-kh9
tags_secondary: [admin, audit, beta-request, instance-mgmt]
date: 2026-06-06
flow: KH-9 (Admin console: instance/audit/beta-request mgmt)
gaps: [GAP-1028, GAP-1029, GAP-1030]
---

# Wave flow-kh9 — KH-9 Admin console G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KH-9 — PLATFORM_ADMIN console: dashboard + instance mgmt (list/suspend/activate) + audit logs + beta-request mgmt. Flow secondary thứ 5.

## 1. Brainstorm

KH-9 = kitehub-admin (`/api/platform/admin` + `/api/v1/admin/**`) + beta-request (subscription). Pre-walk Opus persona simulation (12 FM): gateway routing + PLATFORM_ADMIN role chain SOLID (admin endpoints genuinely @PreAuthorize-guarded — KHÁC KH-8 InstanceController hole). Must-run: FM-4 seed-role + FM-1 audit-table-drift + FM-3 suspend-path.

## 2. Task Breakdown

1. Static pre-walk: admin seed role, audit-log table drift, suspend path.
2. Walk: dashboard + instances list/suspend/activate + audit logs + beta-requests + inverse-authz (non-admin → 403).
3. Catalog findings; assess inline vs gap.
4. File gaps → flip campaign.

## 3. Scope

Walk-only G1 cho admin console. Không inline fix wave này — audit-log 500 là multi-module (subscription repo → admin) + có IT-passes-live-fails discrepancy cần investigation (per `release-fix-retry-budget` §3.5, không patch vội) → gap. Walk solo; 1 Opus pre-walk agent. Admin login cần 2FA enrollment (by-design) → temp-relaxed `totp_required` cho walk, restored sau.

## 4. State-Check Evidence

| Symbol | Verdict | Evidence |
|---|---|---|
| `AdminController` + `AdminInstancesController` + `AdminAuditLogController` @PreAuthorize | ✅ SOLID | class-level `hasRole('PLATFORM_ADMIN')`, @EnableMethodSecurity active |
| PLATFORM_ADMIN role chain (JWT→gateway→filter→@PreAuthorize) | ✅ SOLID | TokenService claim → X-User-Roles → ROLE_PLATFORM_ADMIN → match |
| `admin_audit_log` (V36) vs `admin_audit_logs` (V50) | 🆕 **DRIFT** (FM-1) | both tables exist |
| Beta-request admin endpoints | ✅ guarded | not KH-8-style hole |

## 5. Verification Gates

### Pre-walk
12 failure mode, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh9-admin-console.md`. Role chain solid; admin 2FA enrollment gate.

### G1 walk — evidence

Credential `admin.test@test.vn / Test@1234` (PLATFORM_ADMIN; 2FA temp-relaxed for walk), gateway :9000.

**Happy paths (PASS):**

| Step | Kết quả |
|---|---|
| `GET /api/platform/admin/dashboard` | 200 |
| `GET /api/platform/admin/instances` | 200 (6 instances) |
| `PATCH /instances/{id}/suspend` | 200, DB status SUSPENDED ✓ |
| `PATCH /instances/{id}/activate` | 200 (restored ACTIVE) ✓ |
| `GET /api/v1/admin/beta-requests` | 200 |

**Inverse authz (PASS — gate works):** owner.test (non-admin) → `GET /admin/dashboard` 403 + `PATCH /suspend` 403. PLATFORM_ADMIN gate effective.

**Bugs surfaced (4) — all filed (no inline fix):**

| FM | Severity | Verdict |
|---|---|---|
| Audit-log list 500 (`could not determine data type of parameter $5`; IT-passes-live-fails) | P1 | GAP-1028 (investigation + multi-module) |
| FM-2 suspend/activate not @Auditable → no audit row | P1 | GAP-1029 |
| FM-1 admin_audit_log (singular V36) vs admin_audit_logs (plural V50) drift → immutability on wrong table | P1 | GAP-1029 |
| FM-5 double-suspend → 200 not 409 | P2 | GAP-1030 |

**No inline fix** — audit-log 500 multi-module + discrepancy (IT all-null PASS on Testcontainers PG16 vs live 500) needs investigation before fix per `release-fix-retry-budget` §3.5; FM-1/FM-2/FM-5 follow.

## 6. Agent Spawn Pattern

N/A — walk solo. 1 Opus background agent cho pre-walk persona simulation.

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1028: Admin audit-log list 500 (P1, Backend — nullable-param/IT-discrepancy, multi-module)
- GAP-1029: Admin audit completeness + table drift (P1, Backend — suspend not audited + dual-table)
- GAP-1030: Admin suspend/activate state guards (P2, Backend — double-suspend 409)

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

KH-9 admin console authz SOLID (good — contrast KH-8 GAP-1025 InstanceController no-authz). Audit-completeness gap (FM-2) = sweep candidate: mọi admin mutation cần @Auditable (beta/impersonation có, suspend/activate thiếu) — covered in GAP-1029 fix. State-guard pattern (GAP-1030 double-suspend 409) giống GAP-1026 (purge 409) — cùng "missing state precondition" class.

### Sync targets

Campaign §4 KH-9 → `🔄 walk-pass-pending-human`; wave-history flow-kh9; gap-status.csv 3 rows; audits-index pre-walk row. Admin 2FA restored; suspended instance restored ACTIVE.

### Outcome

KH-9 **G1 ✅ PASS** — admin console core (dashboard + instance mgmt + beta-requests) reachable + work; PLATFORM_ADMIN gate verified effective (inverse-authz 403). Audit sub-leg có bugs (viewer 500 + mutations not audited) → gaps. **Lưu ý review/G2:** GAP-1028 audit-500 cần investigate IT-vs-live discrepancy trước fix; GAP-1029 audit-completeness là PDPL/A09 compliance concern.

## 8. Log

- **2026-06-06:** Wave flow-kh9 — KH-9 G1 walk complete. No inline fix (audit-500 multi-module + investigation needed). 3 gaps (GAP-1028 P1 audit-500 + GAP-1029 P1 audit-completeness/drift + GAP-1030 P2 state-guards). Admin role chain SOLID (inverse-authz verified). Campaign row → walk-pass-pending-human.
