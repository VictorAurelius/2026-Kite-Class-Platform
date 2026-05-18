# GAP-642: V54 JSONB columns Testcontainers IT missing

**Status:** 🟢 DONE 2026-05-18 (Wave 97 Bucket D salvage — `AdminAuditLogJsonbPostgresIT.java` created 318 lines Testcontainers PostgreSQL 16; mvn verify -P strict-warnings PASS 684 tests / 0 failures)
**Priority:** 🟠 P1
**Domain:** Backend (Testing)
**Detected:** 2026-05-18 (Wave 92 post-wave audit suite per GAP-619)
**Related Audits:** [documents/04-quality/audits/security/2026-05-18-wave-92-security-audit-v2.md](../audits/security/2026-05-18-wave-92-security-audit-v2.md)

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|---|---|---|
| V54 migration add `before_state` + `after_state` JSONB columns | `kitehub/kitehub-admin/src/main/resources/db/migration/V54__*.sql` | ✅ shipped Wave 92 Bucket A |
| `AdminAuditLog` entity với JSONB field mapping | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/AdminAuditLog.java` | ✅ shipped |
| H2 unit tests | `kitehub/kitehub-admin/src/test/java/com/kitehub/admin/audit/` | 🟡 partial — H2 không support PostgreSQL JSONB type |
| `AdminAuditLogJsonbPostgresIT` Testcontainers test | `kitehub/kitehub-admin/src/test/java/com/kitehub/admin/audit/` | ❌ missing |

**Grep commands run:**

```bash
find kitehub/kitehub-admin/src/test -name "*JsonbPostgresIT*" -o -name "*Testcontainers*"
grep -rn "@Testcontainers\|PostgreSQLContainer" kitehub/kitehub-admin/src/test/
grep -rn "JSONB\|jsonb" kitehub/kitehub-admin/src/main/resources/db/migration/V54*.sql
```

## Problem

Audit Security Wave 92 (2026-05-18) phát hiện NEW finding P1-4: V54 migration thêm 2 JSONB columns (`before_state` + `after_state`) trong `admin_audit_log` table, nhưng **thiếu Testcontainers integration test** verify PostgreSQL-specific JSONB binding.

Vi phạm `.claude/rules/postgres-specific-type-testcontainers.md` §3 mandate:
> Mọi PostgreSQL-specific type (JSONB, ARRAY, ENUM, INTERVAL, UUID) PHẢI có Testcontainers IT verify binding + serialization round-trip trên real Postgres image.

H2 in-memory database (dùng cho unit tests) **không support JSONB type** — H2 fall back sang VARCHAR/TEXT → silent type mismatch trong test environment. Production runs trên PostgreSQL 15 (per `release-deploy-standard.md` §3.1 staging env). Drift risk:
- Test pass H2 → production fail Postgres JSONB constraint
- JSON path queries (`->`, `->>`, `@>`) work production nhưng untested
- Null/empty JSONB edge cases untested

Grace window per rule: **2026-06-15** (≤30 ngày Wave 92 Bucket A landing date).

## Context

V54 enrichment ship 5 columns cho audit log full context capture (per `.claude/rules/admin-audit-immutability.md` + PDPL Art 11). 2/5 columns là JSONB:
- `before_state`: snapshot resource state trước action
- `after_state`: snapshot resource state sau action

Production audit replay scenarios cần query JSON paths như:
```sql
SELECT * FROM admin_audit_log
WHERE after_state @> '{"status": "APPROVED"}'
  AND created_at > NOW() - INTERVAL '30 days';
```

Test coverage hiện tại không verify production-grade JSONB behavior → P1 testing gap.

## Proposed Fix

### Step 1: Create AdminAuditLogJsonbPostgresIT

```java
// kitehub-admin/src/test/java/com/kitehub/admin/audit/AdminAuditLogJsonbPostgresIT.java
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminAuditLogJsonbPostgresIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    AdminAuditLogRepository repository;

    @Test
    void jsonbBinding_writeAndRead_roundTrip() {
        // Given
        Map<String, Object> beforeState = Map.of("status", "PENDING", "tenantId", "t-123");
        AdminAuditLog log = new AdminAuditLog();
        log.setBeforeState(beforeState);
        // ...

        // When
        AdminAuditLog saved = repository.save(log);
        AdminAuditLog loaded = repository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(loaded.getBeforeState()).isEqualTo(beforeState);
    }

    @Test
    void jsonPathQuery_filterByJsonbField_returnsCorrectRows() {
        // JSON path query via @Query với JSONB operator
    }

    @Test
    void nullJsonbColumn_handledCorrectly() { ... }

    @Test
    void emptyJsonbObject_serializedAsValidJson() { ... }
}
```

### Step 2: Cover edge cases

- Empty JSONB `{}` vs null column
- Large JSONB payload (>1KB) round-trip
- Special characters trong JSONB (unicode, escape sequences)
- JSON path query với non-existent key

### Step 3: Wire vào CI

Verify `mvn verify -Pintegration-tests` chạy Testcontainers IT trong Wave 94c CI run.

## Acceptance Criteria

- [ ] `AdminAuditLogJsonbPostgresIT` test class tồn tại với ≥4 test methods (binding round-trip, JSON path query, null handling, empty object)
- [ ] Tests run pass trên PostgreSQL 15 Testcontainer
- [ ] CI workflow chạy integration tests trong Wave 94c CI run
- [ ] Edge cases covered: null, empty `{}`, unicode chars, >1KB payload
- [ ] Grace window 2026-06-15 honored
- [ ] Pre-handoff self-test per `pre-handoff-self-test-completeness.md`

## Related

- **Audit origin:** [documents/04-quality/audits/security/2026-05-18-wave-92-security-audit-v2.md](../audits/security/2026-05-18-wave-92-security-audit-v2.md)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-94c-gap-619-wave-92-audit-suite.md`
- **Parent gap:** [GAP-619](GAP-619-wave-92-post-wave-audit-suite.md)
- **Sister gap:** [GAP-640](GAP-640-admin-audit-domain-3-layer-docs-missing.md) (same V54 enrichment, docs concern)
- **Code references:**
  - `kitehub/kitehub-admin/src/main/resources/db/migration/V54__*.sql`
  - `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/AdminAuditLog.java`
- **Rules:**
  - `.claude/rules/postgres-specific-type-testcontainers.md` §3 (paired Wave 92 mandate)
  - `.claude/rules/release-deploy-standard.md` §3.1 (staging Postgres parity)
  - `.claude/rules/audit-service-isolation.md` (audit log immutability cross-link)

## Log

- **2026-05-18** — Initial write-up. Filed từ Wave 92 post-wave audit suite (GAP-619) Security audit NEW finding P1-4. State-check confirmed `find kitehub-admin/src/test -name "*JsonbPostgresIT*"` returns 0 — Testcontainers IT cho V54 JSONB columns chưa exist. H2 unit tests không support JSONB → production drift risk. Grace window 2026-06-15 per rule. Phase 1 BETA security gate ≥80 affected.
