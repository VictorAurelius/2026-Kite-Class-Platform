# GAP-970: Audit log row missing cho tenant-switch action — lateral-movement blindspot

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant switch observability) — security forensics
**Defer-to:** After Wave flow-kh3 finish

## Problem

Owner switching tenants — security-sensitive action — KHÔNG có audit row. Không detect được lateral-movement-after-compromise scenario. Plus matrix A6×E4×EC2 — TenantSettings audit log on partial failure: nếu audit service dùng default `@Transactional` propagation, audit failure rolls back the settings UPDATE → need REQUIRES_NEW. Surfaced: matrix A8×E4×EC6 + A6×E4×EC2.

## Proposed Fix

Wire audit log row insert (event_type=`TENANT_SWITCH`, `TENANT_SETTINGS_UPDATED`) với `@Transactional(propagation = REQUIRES_NEW)` per `audit-service-isolation.md`. Capture: userId, tenantId_from, tenantId_to, IP, userAgent.

## Acceptance Criteria

- [ ] Walk tenant switch → `psql -c "SELECT * FROM admin_audit_logs WHERE event_type='TENANT_SWITCH' ORDER BY created_at DESC LIMIT 1"` returns row
- [ ] Settings update audit row written với REQUIRES_NEW (persists even nếu outer tx rolls back)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-failure-mode-matrix.md A8×E4×EC6 + A6×E4×EC2
- Sister: GAP-532 (tenant-switch flow), GAP-949 (provisioning audit log)
- Flow Verification Campaign §4 row KC-1
