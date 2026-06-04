# GAP-946: KC-1 provisioning stub mode + silent DB exception swallow

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) — `kitehub-subscription` DatabaseProvisioningService + InstanceService
**Defer-to:** After Wave flow-kh3 finish

## Problem

`TenantProvisioningSaga.provisionInfrastructure` (kiteclass-core line 83-86) chỉ log "infrastructure provisioning stub"; `DatabaseProvisioningService.lifecycleEnabled=false` mặc định → DB không được tạo thực. `InstanceService:170-176` wrap `databaseProvisioningService.provisionDatabase(saved.getId())` trong `catch (Exception e) { log.error(...); /* Continue */ }`. Instance row lưu với `databaseUrl="pending"`, exception swallowed. User thấy "tenant ready" nhưng DB không tồn tại; KC-2+ subsequent flows fail với errors khó hiểu. Surfaced: persona Finding 1.1 + matrix A1×E1×EC2.

## Proposed Fix

Wire `database.lifecycle.enabled=true` cho real DB provisioning OR rethrow `DatabaseProvisioningException` (stop saga, mark instance FAILED) thay vì silent swallow. Validate `databaseUrl != "pending"` post-provision.

## Acceptance Criteria

- [ ] `grep "database.lifecycle.enabled" kitehub-subscription/src/main/resources/application*.yml` returns true cho prod profile
- [ ] `psql -c "SELECT id FROM instances WHERE database_url='pending'"` post-walk returns 0 rows
- [ ] Provisioning exception → instance status `FAILED` (verifiable via DB query)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,failure-mode-matrix}.md
- Flow Verification Campaign §4 row KC-1
