# GAP-949: Tenant provisioning có 0 audit log row — vi phạm PDPL Art 11

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) + audit/compliance scope
**Defer-to:** After Wave flow-kh3 finish

## Problem

`AuthService.registerFromBetaInvite` chỉ log via `log.info(...)`. KHÔNG có row written to `admin_audit_logs` hoặc domain audit table cho `TENANT_PROVISIONED` event. PDPL Art 11 + Wave 85 immutable admin audit work KHÔNG cover tenant creation. Không trả lời được "tenant X provisioned khi nào, qua beta-invite nào, IP, fingerprint" post-incident. `TenantProvisioningSaga.java:53-86` cũng dùng `log.info` toàn bộ — không có `auditLog.recordProvisioningStep(...)` với REQUIRES_NEW propagation per `audit-service-isolation.md`. Surfaced: matrix A1×E4×EC2 + persona Finding 4.2.

## Proposed Fix

Wire audit log row insert (event_type=`TENANT_PROVISIONED`) trong AuthService + per saga step (`SAGA_STEP_*`) với `@Transactional(propagation = REQUIRES_NEW)` per `audit-service-isolation.md`. Capture: tenantId, ownerId, betaInviteId, IP, userAgent, timestamp, sagaStepNum.

## Acceptance Criteria

- [ ] `psql -c "SELECT * FROM admin_audit_logs WHERE event_type LIKE '%TENANT%' LIMIT 5"` post-walk returns rows
- [ ] Saga compensation failure also writes audit row
- [ ] REQUIRES_NEW verified (audit row persists even if outer tx rolls back)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{failure-mode-matrix,tenant-provisioning}.md
- Sister rule: `.claude/skills/...audit-service-isolation.md`
- Flow Verification Campaign §4 row KC-1
