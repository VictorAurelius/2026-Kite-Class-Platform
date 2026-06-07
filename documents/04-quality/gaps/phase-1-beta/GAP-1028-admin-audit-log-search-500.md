# GAP-1028: Admin audit-log list 500 — could not determine data type of parameter (nullable filter)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (KH-9 admin console G1 walk)
**Affects:** `AdminAuditLogRepository.search` (kitehub-subscription, consumed by kitehub-admin `AdminAuditLogController`)

## Problem

KH-9 G1 walk: `GET /api/v1/admin/audit-logs` (default load, no filters) → **HTTP 500**. Admin audit-log viewer hoàn toàn không xem được.

Error (admin service log): `org.postgresql.util.PSQLException: ERROR: could not determine data type of parameter $5`.

Root cause candidate: `AdminAuditLogRepository.search` JPQL dùng pattern optional-filter:
```jpql
WHERE (:action IS NULL OR a.action = :action)
  AND (:adminUserId IS NULL OR a.adminUserId = :adminUserId)
  AND (:from IS NULL OR a.createdAt >= :from)
  AND (:to IS NULL OR a.createdAt <= :to)
```
Khi tất cả filter null (default panel load), Postgres không infer được type của null bind cho LocalDateTime param (`:from`/`:to`) → 500.

## ⚠️ Discrepancy cần investigate (per release-fix-retry-budget §3.5)

`AdminAuditLogSearchPostgresIT:77` test CHÍNH XÁC case all-null `search(null,null,null,null,PageRequest.of(0,20))` trên Testcontainers Postgres và **PASS** — nhưng live 500. Khác biệt cần xác định TRƯỚC khi fix:
- **Postgres version**: IT dùng `postgres:16`; live `kite-postgres` có thể version khác → null-param type-inference khác.
- **Pageable sort**: live controller `@PageableDefault` + `clampPageable` có thể inject Sort khác IT's `PageRequest.of(0,20)` (no sort) → Spring Data restructure query.
- **Count query**: `Page<>` sinh count query riêng — có thể count query là chỗ fail.

KHÔNG patch vội bằng Specification rewrite trước khi confirm trigger thật (IT contradicts simple nullable-param theory).

## Proposed Fix (sau investigation)

Likely: convert `search` sang `JpaSpecificationExecutor` + dynamic Specification (chỉ add predicate cho non-null filter → không có null-param typing). Lưu ý multi-module: repo ở subscription, consumed by admin → rebuild cả 2 (`mvnw -pl kitehub-subscription -am` + admin). Match Postgres version live vs Testcontainers trong IT.

## Acceptance Criteria

- [ ] Root cause confirmed (version / sort / count) — documented
- [ ] `GET /api/v1/admin/audit-logs` default load → 200 (empty or rows)
- [ ] Filtered (action/admin/date) → 200 đúng
- [ ] IT cover all-null + filtered trên Postgres version khớp live

## Related

- Discovered in: KH-9 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh9-admin-console.md` (FM-1 area)
- Related: GAP-1029 (audit completeness + table drift)

## Log

- **2026-06-07** (Wave g2-blockers-1 Bucket A, inline): Fix robust-by-construction thay vì chẩn đoán chính xác trigger. `AdminAuditLogRepository.search` chuyển từ single JPQL `(:param IS NULL OR ...)` sang `JpaSpecificationExecutor` + dynamic `Specification` — chỉ add predicate cho filter non-null → **KHÔNG BAO GIỜ bind null param** → lỗi "could not determine data type of parameter $5" không thể xảy ra bất kể Postgres version / sort / count-query restructuring (giải quyết discrepancy IT-pass-vs-live-500 mà không cần reproduce live: nguyên nhân đều là null-param typing, Specification loại bỏ hoàn toàn). Sort forced created_at DESC qua PageRequest. Signature `search(...)` giữ nguyên → consumer `AdminAuditLogController` không đổi. **Status 🟡 PARTIAL ~80%** — code fix + compile PASS; **residual:** (a) IT all-null + filtered (Postgres version khớp live); (b) G3 gateway :9000 re-walk GET /api/v1/admin/audit-logs → 200 pending coordinator.
