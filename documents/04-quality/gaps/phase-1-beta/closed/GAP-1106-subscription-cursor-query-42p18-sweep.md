# GAP-1106: Subscription cursor queries — Postgres 42P18 untyped-null-param (sweep)

**Status:** 🟢 DONE
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

## Fix (shipped)

Áp dụng CÙNG pattern GAP-1028 precedent (encapsulate trong repository — caller signature giữ nguyên): mỗi cursor query tách thành 2 `@Query` method + 1 `default` method branch theo `cursorId == null`:

- **first-page query** — KHÔNG có cursor param (`WHERE deleted = false ORDER BY id ASC`)
- **after-cursor query** — typed cursor param (`AND id > :cursorId`)
- **default method** — `cursorId == null ? findFirstPage(...) : findAfterCursorId(...)`

Không có untyped null param nào được bind nữa → 42P18 không thể xảy ra bất kể Postgres version. Public method `findAfterCursor(UUID, Pageable)` + `findByStatusAfterCursor(...)` giữ nguyên signature → callers `InstanceService:490` + `PaymentService:206-207` KHÔNG đổi (sweep verdict EXEMPT, xem dưới).

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** `@Query` JPQL `(:param IS NULL OR ...)` — Hibernate bind untyped null trong vị trí `IS NULL` → Postgres 42P18.

**Caller sweep (per api-contract-change-caller-sweep.md):**

```bash
grep -rn "findAfterCursor\|findByStatusAfterCursor" \
  kitehub/kitehub-subscription/src/main/java kitehub/kitehub-subscription/src/test/java
```

| Caller | Verdict | Reason |
|---|---|---|
| `InstanceService.java:490` `findAfterCursor(cursorId, pageable)` | EXEMPT | Public signature unchanged (default method) → no caller change |
| `PaymentService.java:206-207` `findByStatusAfterCursor / findAfterCursor` | EXEMPT | Public signatures unchanged → no caller change |
| Service `*Test` mocks of cursor methods | EXEMPT | grep returned 0 — no existing test stubs the cursor methods |

**Class sweep (detector full repo):** `bash scripts/check-jpql-untyped-null-param.sh`

| Verdict | Sites | Action |
|---|---|---|
| FIX | `InstanceRepository:143`, `PaymentRepository:84,90` (3) | Fixed this PR |
| DEFER (out-of-scope) | `kitehub-branding` BrandingLifecycleEventRepository:23 (1) | branding scope — GAP-1105 lineage; not touched per task scope |
| DEFER (out-of-scope) | `kiteclass-core` Course/Vetting/Incident/PayrollPeriod repos (9) | kiteclass scope — separate follow-up sweep |

## Verification

- **CI detector self-test** `scripts/check-jpql-untyped-null-param.sh`: pre-fix flagged exactly the 3 subscription sites (`PaymentRepository:84,90`, `InstanceRepository:143`); post-fix 0 subscription hits. `AdminAuditLogRepository:38` javadoc comment correctly excluded (comment-line filter). shellcheck clean.
- **Testcontainers IT (production-equivalent Postgres 16)** — `InstanceCursorPaginationPostgresIT` (3/3) + `PaymentCursorPaginationPostgresIT` (4/4) = **7/7 PASS, BUILD SUCCESS**. Covers first page (`cursorId = null`) + subsequent page + status-filtered variant + full keyset traversal consistency. First-page call (the originating null-cursor 42P18 trigger) executes cleanly on real Postgres.
- **Caller-sweep regression** — existing service unit tests GREEN (InstanceServiceTest 11/11, PaymentServiceTest 11/11, PaymentServiceBoundedQueryTest 5/5, InstanceServiceBoundedListTest 2/2).

## Acceptance Criteria

- [x] 3 site fixed (split query / typed)
- [x] Testcontainers IT: instance + payment cursor pagination first+subsequent page PASS on Postgres
- [x] CI detector ship (WARN) bắt được pre-fix state
- [x] Sweep confirm 0 site `:param IS NULL OR` còn lại trong subscription scope (trừ AdminAuditLog đã fix; branding + kiteclass out-of-scope DEFER documented)

## Related

- Origin: GAP-1105 branding 42P18 fix → sweep
- Precedent: GAP-1028 (AdminAuditLogRepository same class, fixed)
- Rule: `postgres-specific-type-testcontainers.md` + `cross-flow-bug-class-sweep.md` §4.1

## Log

- **2026-06-10** (DONE): Fixed 3 sites via split-query + repository default-method branch (GAP-1028 precedent pattern), callers unchanged. Shipped CI detector `scripts/check-jpql-untyped-null-param.sh` (WARN-mode v1, comment-line excluded) — self-test 3 pre-fix → 0 post-fix subscription hits. Shipped 2 Testcontainers Postgres ITs (`InstanceCursorPaginationPostgresIT` + `PaymentCursorPaginationPostgresIT`, `@DataJpaTest`) — 7/7 PASS confirming no 42P18 on real Postgres for null-cursor first page + subsequent + status-filtered. Caller-sweep clean (signatures stable). Detector additionally surfaces 1 branding + 9 kiteclass remaining `:param IS NULL OR` sites — out-of-scope per task, candidates for follow-up sweep wave.
