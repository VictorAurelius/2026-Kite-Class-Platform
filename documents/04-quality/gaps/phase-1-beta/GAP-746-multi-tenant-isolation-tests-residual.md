# GAP-746 — Multi-tenant isolation tests residual fail post-truncate (GAP-735 last 2/6)

**Status:** OPEN
**Priority:** P2
**Domain:** Backend (test infrastructure)
**Phase:** phase-1-beta
**Completion:** 0%
**Found:** 2026-05-25
**Updated:** 2026-05-25

## Problem

Wave meta-2 PR #1819 (TestFixtureCleanup dynamic truncate via pg_tables introspection) unblocked **4/6 GAP-735 baseline failures** (all 4 CourseSecurityTest SQL injection tests). 2 tests vẫn fail với DIFFERENT failure mode (không phải uk_invoices_instance_number constraint violation nữa):

| Test | Failure | Line |
|---|---|---|
| `EnrollmentIT.enrollStudent_shouldIsolate_multiTenantData` | `Status expected:<404> but was:<500>` | EnrollmentIntegrationTest.java:479 |
| `InvoiceFlowIT.testMultiTenantIsolation_InvoiceFilters` | `No matching value at JSON path "$.data.content[*].studentId"` | — |

Both tests có `multiTenantIsolation` trong tên → test logic intentionally tạo data trong 2 tenants (A + B) + assert isolation.

## Root cause hypothesis (untested)

`TestFixtureCleanup.beforeTestMethod` (order=3500) truncate ALL tables BEFORE `@BeforeEach setUp()`. Nếu test:
1. setUp() tạo data tenant A (OK)
2. test method tạo data tenant B + verify cross-tenant query returns only tenant-A data (still OK)

→ But assertion fails on 500/JSON-empty. Có thể:
- Foreign key cascade từ truncate xóa references mà test depend
- Test setUp dùng @BeforeAll (run once) instead of @BeforeEach → truncate giữa methods xóa class-level fixtures
- TenantContext propagation timing issue post-truncate

## Investigation steps (Wave meta-3)

1. Read full test method bodies (line 479 EnrollmentIT + InvoiceFlowIT test)
2. Verify @BeforeEach vs @BeforeAll usage
3. Add debug log trong test setUp + assertion to confirm data state
4. Try @Sql(scripts="...", executionPhase=AFTER_TEST_METHOD) thay vì listener for these 2 classes only (alternative cleanup timing)

## Proposed Fix (Wave meta-3 candidate)

### Option A — Inspect + targeted fix per test
Read each test, identify specific dependency on prior state, fix test logic to be self-contained.

### Option B — Disable listener on these 2 classes
Remove `@TestExecutionListeners` from EnrollmentIT + InvoiceFlowIT; let `@Transactional@Rollback` handle (these 2 may have been written with rollback assumption that listener truncate breaks).

## Acceptance Criteria

- [ ] `EnrollmentIT.enrollStudent_shouldIsolate_multiTenantData` PASS
- [ ] `InvoiceFlowIT.testMultiTenantIsolation_InvoiceFilters` PASS
- [ ] No regression in other 1482 tests
- [ ] `./mvnw verify -P strict-warnings` clean
- [ ] GAP-735 flips DONE 100%, GAP-745 flips DONE 100%, `ADMIN_MERGE_OVERRIDE: GAP-735` trailer no longer needed prospectively

## Out-of-scope

- Broader test-data builder refactor (not needed; 4/6 already fixed by truncate)

## Related

- GAP-735 PARTIAL 90% — broader flake originally
- GAP-745 PARTIAL 85% — InvoiceTestDataBuilder + listener architecture
- Wave meta-2 PR #1819 — dynamic truncate listener shipped
