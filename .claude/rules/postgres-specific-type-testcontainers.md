---
paths: ["**/*.java"]
---

# Postgres-Specific Type → Testcontainers Mandate

**Priority:** 🟠 MANDATORY — H2 hides Postgres-specific binding bugs that production exposes
**Version:** 1.0.0
**Created:** 2026-05-16
**Last-Reviewed:** 2026-05-16
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement per §6.5 Enforcement Parity Mandate; paired same-PR with hotfix `LoginAuditLog.ip` INET→VARCHAR migration + CI grep detector + worked self-test on 2026-05-16 admin-login 500 incident; no constraint loosening; existing entities grandfathered until next refresh)
**Applies to:** Every JPA entity with `@Column(columnDefinition = "...")` whose value references a Postgres-specific type (`inet`, `jsonb`, `tsvector`, `citext`, `hstore`, `cidr`, `macaddr`, `uuid[]`, `text[]`, `int4range`, `interval`, generated columns, custom domains). Also covers `@JdbcTypeCode(SqlTypes.JSON|INET|UUID|ARRAY)` Hibernate 6 type hints.

---

## 1. The Rule

> **Every JPA entity that maps a Postgres-specific column type MUST have an integration test running against real PostgreSQL via Testcontainers — NOT H2.** The test MUST exercise at least one CRUD round-trip (save → flush → retrieve) for the affected column. Unit tests using Mockito mocks of the repository are insufficient because they bypass the actual JDBC binding.

H2 in-memory accepts `setString` into columns declared `INET` / `JSONB` / `TSVECTOR` and silently treats them as VARCHAR. Postgres rejects with `SQLState 42804` (datatype mismatch) the moment the same code hits production. This rule mandates a test layer that catches the bug class BEFORE production.

---

## 2. Why this matters — 2026-05-16 admin login 500 incident

`LoginAuditLog.ip` declared `@Column(name = "ip", columnDefinition = "inet")` with Java type `String`. Hibernate binds via `PreparedStatement.setString` (varchar). Postgres rejects:

```
ERROR: column "ip" is of type inet but expression is of type character varying
Hint: You will need to rewrite or cast the expression.
```

Tests that ran in CI:
- `LoginAuditServiceTest` — 6 PASS, all using Mockito mock of `LoginAuditLogRepository`. Mock returns dummy entity → real SQL never executes → binding bug invisible.
- Integration tests use H2 in-memory. H2 has no native INET type; silently stored as VARCHAR. Passed.

First place the bug surfaced was production CloudWatch logs at 2026-05-16 — after every admin login. Cost: P0 incident, ~2h debug, hotfix PR. With this rule, the bug would have been caught at PR-time integration test on Testcontainers Postgres.

---

## 3. Postgres-specific types in scope

| Type | Why H2 misses it |
|------|------------------|
| `INET` | H2 has no native type; treats as VARCHAR. Postgres requires explicit `::inet` cast or `inet`-bound parameter. |
| `JSONB` / `JSON` | H2 stores as CLOB. Postgres validates JSON syntax + supports `@>` `?` operators only on real `jsonb`. |
| `TSVECTOR` | H2 has no full-text type. Postgres `to_tsvector(...)` mandatory. |
| `CITEXT` | H2 ignores case-insensitive collation difference. |
| `HSTORE` | H2 doesn't exist; would silently truncate to VARCHAR. |
| `UUID[]` / `TEXT[]` / array types | H2 has limited array support; Postgres needs `setArray` not `setObject`. |
| `INT4RANGE` / `TSTZRANGE` | H2 has no range types; Postgres has range operators. |
| `INTERVAL` | H2 has limited support; Postgres has rich interval arithmetic. |
| Generated columns (`GENERATED ALWAYS AS`) | H2 syntax different; Postgres-specific expression syntax. |
| Postgres-specific check constraints / domains | Behavior diverges across DBs. |

Reference: PostgreSQL docs §8.3 (network types), §8.14 (JSON), §8.11 (full text), §8.17 (range).

---

## 4. Required test shape

For each entity field matching §3 types, add a Testcontainers integration test:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)  // do NOT swap with H2
@Testcontainers
class LoginAuditLogPostgresIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired LoginAuditLogRepository repository;

    @Test void roundtrip_ip_column() {
        LoginAuditLog saved = repository.save(LoginAuditLog.builder()
            .userId(UUID.randomUUID())
            .loginAt(LocalDateTime.now())
            .ip("203.0.113.7")            // IPv4
            .fingerprintHash("a".repeat(64))
            .alertSent(false)
            .build());
        repository.flush();              // forces SQL execute, catches binding errors
        LoginAuditLog reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getIp()).isEqualTo("203.0.113.7");
    }

    @Test void roundtrip_ipv6() {
        // covers 45-char IPv6 worst case
        LoginAuditLog saved = repository.save(LoginAuditLog.builder()
            .userId(UUID.randomUUID())
            .loginAt(LocalDateTime.now())
            .ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334")
            .fingerprintHash("b".repeat(64))
            .alertSent(false)
            .build());
        repository.flush();
        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
```

Key requirements:
- `@AutoConfigureTestDatabase(replace = Replace.NONE)` — prevent Spring Boot from swapping in H2
- `@Container static PostgreSQLContainer<>("postgres:16")` — pin a major version aligned with prod
- `repository.flush()` after save — forces SQL execution; without flush, Hibernate may defer until txn end and you miss the error
- One test per distinct value space (IPv4, IPv6, edge case, null where applicable)

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Map Postgres-specific column as String + rely on `columnDefinition` | Either use proper Hibernate `@JdbcTypeCode`/custom `UserType` AND test on Testcontainers, OR migrate column to VARCHAR (acceptable trade-off for simple cases like IP-as-text) |
| Test only with `@Mock LoginAuditLogRepository` | Mock test the service logic; ADD a separate `@DataJpaTest + Testcontainers` for the entity binding |
| Use H2 + JPA compatibility mode "POSTGRESQL" | Mode helps with syntax (RETURNING clauses, etc.) but does NOT add native INET/JSONB types. Still fails to catch binding bugs. |
| Skip integration test "because production canary will catch it" | Canary catches AFTER deploy. Test catches BEFORE merge. Cost ratio ~100×. |
| Use single Testcontainer for all tests | Acceptable when fast (container reuse via `@Container static`). Per-test container is overkill. |

---

## 6. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 6.1 CI grep detector (active now — landed same PR)

Script `scripts/check-postgres-types-testcontainers.sh` (wired into `script-quality.yml`):

```bash
# Find entities with Postgres-specific columnDefinition
ENTITIES=$(grep -rln 'columnDefinition\s*=\s*"(inet|jsonb|tsvector|citext|hstore|cidr|macaddr|.*\[\])"' \
  kitehub/ kiteclass/ --include='*.java' -E)

# For each, verify a matching *IT.java or *PostgresIT.java exists
for ENTITY in $ENTITIES; do
  CLASS=$(basename "$ENTITY" .java)
  IT=$(find . -name "${CLASS}*IT.java" -o -name "${CLASS}*PostgresIT.java" | head -1)
  if [[ -z "$IT" ]]; then
    echo "WARN: $ENTITY uses Postgres-specific type but has no matching Testcontainers IT"
  fi
done
```

WARN mode initially (30-day grace from this PR merge); HARD STOP target 2026-06-15. Override via:

```
git commit -m "...
POSTGRES_TYPE_TC_SKIP: <entity> — <reason + follow-up gap link>"
```

### 6.2 Reviewer-checklist (manual)

When reviewing a PR that adds or modifies a JPA entity with `columnDefinition=`:
- [ ] Is the type Postgres-specific per §3?
- [ ] Does the PR include a Testcontainers integration test covering CRUD round-trip?
- [ ] Does the test call `repository.flush()` to force SQL execute?
- [ ] If migrating to VARCHAR (alternate fix): is the column length documented (e.g. 45 for IP, 4096 for short JSON, etc.)?

### 6.3 ArchUnit test (deferred to follow-up)

Future enhancement: ArchUnit rule that scans `@Column(columnDefinition=...)` and asserts a matching `*IT` class exists in test source root. Defer until grep detector proves stable.

---

## 7. Self-test (worked example — 2026-05-16 incident)

Apply rule retroactively at `LoginAuditLog.java` ship time (Wave 72b Bucket C 2026-05-13):

- Entity has `@Column(name = "ip", columnDefinition = "inet")` ✓ matches §3 (INET row)
- Test scan: `LoginAuditServiceTest` exists (Mockito, doesn't count). No `LoginAuditLog*IT.java`, no `LoginAudit*PostgresIT.java`.
- §1 mandate: MUST have Testcontainers integration test.
- §6.1 detector at PR-time: WARN → reviewer-checklist §6.2 row 2 unchecked → block merge OR ship Testcontainers test.

→ Rule fires correctly on the originating incident. ✅

Counterfactual: if rule existed at 2026-05-13, Testcontainers test would have hit `SQLState 42804` immediately on first `flush()`. Bug caught at PR review, not production.

---

## 8. Relationship to other rules

- **`audit-service-isolation.md`** (new sister-rule same PR) — covers the OTHER half of the 2026-05-16 incident (Bug 2, propagation)
- **`design-patterns.md`** §3.11 (new same PR) — anti-pattern table now warns about Postgres-binding mismatch
- **`backend-standards.md`** + **`testing-standards.md`** — existing standards reference Testcontainers; this rule mandates WHEN required
- **`release-deploy-standard.md`** §3 smoke-admin-login extension (same PR) — post-deploy net for the case test still misses
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-16 admin login 500 via 5-stage pipeline
- **`rule-change-process.md`** §5.1 atomic-unique bar — atomic (single concept: Postgres type → Testcontainers), unique (no existing rule mandates this), widely applicable (≥15 entities repo-wide with `columnDefinition=`), body ≤2 "and"

---

## 9. Log

- **2026-05-16 (v1.0.0):** Rule created. Triggered by 2026-05-16 production admin login 500 incident — `LoginAuditLog.ip` INET binding failure invisible to H2 + Mockito unit test, surfaced only in production. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged P0) → Classify ✓ (no existing rule mandates Testcontainers for Postgres-specific types; `testing-standards.md` discusses Testcontainers as option not mandate) → Rule+Enforce ✓ (this file + CI grep detector + reviewer-checklist + paired same-PR with `audit-service-isolation.md` + `design-patterns.md` §3.11 + smoke-admin-login extension per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example on the originating incident — rule fires) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint, no constraint loosening; existing entities grandfathered with 30-day grace, rule applies prospectively from this PR). ArchUnit deferred until grep detector stable.
