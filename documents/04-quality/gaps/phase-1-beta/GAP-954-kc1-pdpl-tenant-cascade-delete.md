# GAP-954: PDPL Art 23 tenant DELETE cascade incomplete — orphan data

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Compliance
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant lifecycle DELETE) + PDPL compliance
**Defer-to:** After Wave flow-kh3 finish

## Problem

Per `tenant-provisioning/rules.md` BR-PROV-005 + `instance-lifecycle/rules.md`: lifecycle FSM chỉ cover NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED → REGENERATING → FAILED loop. KHÔNG cover SUSPEND/DELETE state-machine. Khi Owner xoá tenant trial cũ → click "Đóng trung tâm A" → BE call DELETE → orphan data (DB row, MinIO bucket, DNS record, S3 logo). 6 tháng sau bị PDPL audit hỏi "retain dữ liệu sau xoá account?". Surfaced: persona Finding 2.3.

## Proposed Fix

Extend FSM với SUSPENDED + DELETED states + transitions (per benchmark §A row 8: soft-delete 30d → permanent). Implement cascade: tenant DB drop / MinIO bucket purge / DNS record delete / S3 logo delete + audit log `TENANT_DELETED` event. Wave deferred to off-boarding scope per GAP-201 partial.

## Acceptance Criteria

- [ ] FSM định nghĩa transition DEPLOYED → SUSPENDED → DELETED (or ARCHIVED) + grace 30d
- [ ] Cascade delete script verified xoá: DB, MinIO bucket, DNS record, S3 logo
- [ ] PDPL Art 23 retention policy documented + tested
- [ ] Audit row `TENANT_DELETED` written

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 2.3
- Sister: GAP-201 (Tenant Off-boarding Runbook PARTIAL 50%)
- Flow Verification Campaign §4 row KC-1
