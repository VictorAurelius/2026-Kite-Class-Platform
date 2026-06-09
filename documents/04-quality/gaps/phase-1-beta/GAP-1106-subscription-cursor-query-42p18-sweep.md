# GAP-1106: Subscription cursor queries — Postgres 42P18 untyped-null-param (sweep)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-09 (cross-flow sweep of GAP-1105 branding 42P18 fix)
**Affects:** kitehub-subscription `InstanceRepository:143`, `PaymentRepository:84,90`

## Problem

Sweep (per `cross-flow-bug-class-sweep.md`) bug-class `(:param IS NULL OR ...)` sau khi fix branding lifecycle-events 42P18 tìm 3 sister site CÙNG CLASS trong kitehub-subscription cursor pagination:

- `InstanceRepository.java:143` — `AND (:cursorId IS NULL OR i.id > :cursorId)`
- `PaymentRepository.java:84` — `AND (:cursorId IS NULL OR p.id > :cursorId)`
- `PaymentRepository.java:90` — `AND (:cursorId IS NULL OR p.id > :cursorId)`

Hibernate expand named param thành 2 positional `?` → `(? IS NULL OR ... > ?)`; param đầu (`? IS NULL`) Postgres KHÔNG suy được kiểu → `42P18 could not determine data type` lúc PREPARE (như branding bug đã chứng minh empirically). H2 (test) che. Precedent: `AdminAuditLogRepository:38` đã fix class này (GAP-1028) → class tái diễn.

**Statically-detectable** → per `cross-flow-bug-class-sweep.md` §4.1 nên ship CI detector grep `:param IS NULL OR` trong `@Query`.

## Proposed Fix

1. Verify empirically: Testcontainers Postgres IT exercise cursor pagination (instance-list + payment-list first page cursorId=null + subsequent) → confirm 42P18.
2. Fix per GAP-1028 precedent: split 2 query method (with/without cursor) HOẶC bỏ null branch nếu cursor luôn có default. Caller branch theo cursorId==null.
3. Ship CI detector `scripts/check-jpql-untyped-null-param.sh` (grep `:[a-zA-Z]+ IS NULL OR` trong @Query) WARN-mode.

## Acceptance Criteria

- [ ] 3 site fixed (split query / typed)
- [ ] Testcontainers IT: instance + payment cursor pagination first+subsequent page PASS on Postgres
- [ ] CI detector ship (WARN) bắt được pre-fix state
- [ ] Sweep confirm 0 site `:param IS NULL OR` còn lại (trừ AdminAuditLog đã fix)

## Related

- Origin: GAP-1105 branding 42P18 fix → sweep
- Precedent: GAP-1028 (AdminAuditLogRepository same class, fixed)
- Rule: `postgres-specific-type-testcontainers.md` + `cross-flow-bug-class-sweep.md` §4.1
