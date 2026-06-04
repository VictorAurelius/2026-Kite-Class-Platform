# GAP-962: TenantSettings concurrent edit không có optimistic locking — last-write-wins

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant settings concurrency)
**Defer-to:** After Wave flow-kh3 finish

## Problem

TenantSettings entity missing (GAP-947); khi add, JPA default `save` overwrites silent. KHÔNG có `@Version` optimistic lock → silent loss khi 2 staff edit settings same time. Realistic: owner + 1 admin edit school address simultaneously → 1 update lost. Surfaced: matrix A6×E2×EC8.

## Proposed Fix

Khi implement GAP-947 (TenantSettings entity), include `@Version` Long `version` field cho JPA optimistic lock. Map `OptimisticLockException` → 409 với friendly "Setting đã được cập nhật bởi người khác, vui lòng reload" message + show current value.

## Acceptance Criteria

- [ ] `TenantSettings.java` has `@Version` field
- [ ] Concurrent edit test: 2 PUT settings với stale version → 1 success (200) + 1 conflict (409)
- [ ] FE handles 409 với reload + diff display

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-failure-mode-matrix.md A6×E2×EC8
- Sister: GAP-947 (TenantSettings entity — parent gap), GAP-944 (cache invalidation pattern)
- Flow Verification Campaign §4 row KC-1
