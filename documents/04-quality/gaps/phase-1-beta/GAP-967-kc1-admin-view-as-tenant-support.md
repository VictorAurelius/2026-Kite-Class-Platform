# GAP-967: Admin "view-as-tenant" support UI missing — 30-min SRE response delay

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Admin platform support) — Phase 1 BETA reactive support
**Defer-to:** After Wave flow-kh3 finish

## Problem

Multi-tenant DB isolation (per `DatabaseProvisioningService`) creates per-instance DB với random password encrypted AES-256-GCM. Admin platform KHÔNG có "view-as-tenant" UI. Owner Tuấn report "không thấy student tôi nhập tuần trước" → admin platform cần inspect tenant DB → phải call SRE → unsealed via KMS → 30 min response time → Owner đã giận. Surfaced: persona Finding 4.4.

## Proposed Fix

Wire admin endpoint `/admin/tenants/{id}/impersonate` issuing scoped read-only JWT (1-hour TTL, audit-logged). FE button trong admin tenant detail. Audit row `ADMIN_IMPERSONATED_TENANT` mỗi click (security-critical).

## Acceptance Criteria

- [ ] `POST /api/v1/admin/tenants/{id}/impersonate` returns scoped JWT cho PLATFORM_ADMIN role
- [ ] JWT TTL ≤ 1 hour, read-only scope
- [ ] Audit log row written với admin user + target tenant + timestamp + reason field

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 4.4
- Flow Verification Campaign §4 row KC-1
