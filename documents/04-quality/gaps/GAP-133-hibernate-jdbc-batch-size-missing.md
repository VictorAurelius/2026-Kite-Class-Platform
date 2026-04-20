# GAP-133: Hibernate `jdbc.batch_size` not configured — bulk inserts 1 INSERT per row

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend / Database / Performance
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** All Spring Boot services using JPA; hit hardest on `kiteclass-core` bulk-import (GAP-051 Wave 1)
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

Grep `spring.jpa.properties.hibernate.jdbc.batch_size` across all `application*.yml` → **zero matches**. Hibernate default is `batch_size = 0` (batching disabled). Every `persist()` issues its own JDBC INSERT round-trip.

Impact on Wave 1 bulk-import feature (GAP-051):
- `BulkImportChunkExecutor.processChunk()` loops rows and calls `studentService.createStudent(request, tenantId)` per row.
- Each `createStudent` issues ≥1 INSERT + 1-2 SELECT for uniqueness checks.
- With chunks of 100 rows: 300-400 round-trips per chunk. On localhost pgsql ~1ms each = 300-400ms per chunk overhead.
- With 10k rows (typical school roster): ~100 chunks × 400ms = 40s overhead just from non-batching.

## Context

GAP-051 shipped Wave 1 bulk import. Memory was added: `feedback_pr_log_commit.md` indicates the feature works functionally, but no perf tuning was done on the insert path.

## Evidence

- `grep 'batch_size' **/application*.yml` → 0 matches
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/service/BulkImportChunkExecutor.java:82` — per-row `createStudent` call
- Performance audit §1, §5

## Proposed Fix

1. Add to every `application.yml` where JPA is used:
   ```yaml
   spring:
     jpa:
       properties:
         hibernate:
           jdbc:
             batch_size: 50
             batch_versioned_data: true
           order_inserts: true
           order_updates: true
   ```
2. For bulk-import specifically, review `StudentServiceImpl.createStudent`:
   - Move duplicate-email check to a batch query (pre-check all emails in chunk against DB once).
   - Consider switching to `JdbcTemplate.batchUpdate` for the INSERT path if full JPA lifecycle not needed.
3. Add integration test measuring SQL round-trips for 100-row chunk (target: ≤5 batched statements).
4. Document perf tuning in `backend-standards.md`.

## Acceptance Criteria

- [ ] All `application.yml` services declare `jdbc.batch_size: 50` + `order_inserts: true`
- [ ] Bulk import 100-row chunk executes ≤10 SQL statements (via `QueryCountHolder` test)
- [ ] Bulk import 10k-row file p95 < 15s (currently estimated 40-60s)
- [ ] backend-standards.md updated with batching convention

## Related

- Audit: performance-audit-2026-04-19.md §5
- GAP-051 (Wave 1 bulk import — feature complete, perf not tuned)

## Log

- 2026-04-19 — Gap created from performance baseline audit
- 2026-04-20 — Fixed in feature/partb-perf-batch: added `spring.jpa.properties.hibernate.jdbc.batch_size: 50`, `batch_versioned_data: true`, `order_inserts: true`, `order_updates: true` to ALL 5 JPA-using services (`kiteclass-core`, `kitehub-subscription`, `kitehub-admin`, `kitehub-branding`, `kitehub-gateway`). `batch_versioned_data=true` is mandatory here because `BaseEntity` uses `@Version`. New `HibernateBatchConfigTest` reads production `application.yml` directly (bypassing the test override) and asserts all 4 properties — so a future regression fails at unit-test time. Query-count test for bulk import deferred (requires Docker + Testcontainers; existing kiteclass-core test base `IntegrationTestBase` is gated on `ENABLE_INTEGRATION_TESTS=true`).
