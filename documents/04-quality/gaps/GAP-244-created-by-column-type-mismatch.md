# GAP-244: `created_by` / `updated_by` column type mismatch (V29+ migrations VARCHAR vs entity Long)

**Status:** 🟢 DONE 2026-04-28 — Path A shipped: `V46__align_audit_columns_to_bigint.sql` ALTERs `created_by` / `updated_by` from VARCHAR to BIGINT across 19 V28..V44 tables (junction `role_permissions` excluded). Idempotent DO block keyed on `information_schema.columns.data_type`. New test `Wave02MigrationsTest.v46_audit_columns_aligned_to_bigint` asserts every audit column resolves to `bigint` post-migration. Full kiteclass-core suite 1123/1123 ✅. Dev profile `ddl-auto: create-drop` workaround can now be reverted as opportunistic follow-up (not strictly required — V46 makes Flyway+validate path viable).
**Priority:** 🟠 P1 (blocks `ddl-auto: validate` + Flyway path on a fresh dev DB; latent in production where pre-V29 tables happen to use BIGINT)
**Domain:** Backend (kiteclass-core schema)
**Detected:** 2026-04-27 during GAP-235 Sub-PR G live-screenshot attempt
**Affects:** `kiteclass-core` boot on dev profile, any future `ddl-auto: validate` run, schema consistency across services

## Current State (verified 2026-04-27)

`com.kiteclass.core.common.entity.BaseEntity`:

```java
@CreatedBy
@Column(name = "created_by", updatable = false)
private Long createdBy;

@LastModifiedBy
@Column(name = "updated_by")
private Long updatedBy;
```

But Flyway migrations declare two different types for the same audit columns:

| Migration era | Type used | Examples |
|---------------|-----------|----------|
| V1–V25 | `BIGINT` | `V1__create_core_schema.sql`, `V26__add_missing_audit_columns.sql` |
| V29+ | `VARCHAR(100)` | `V29__create_k12_tables.sql`, `V30__create_role_hierarchy_tables.sql`, `V33__create_outbox_events_table.sql`, `V34__create_rebrand_approvals_table.sql`, `V35__create_audit_log_table.sql`, `V39__create_quality_reports_table.sql` (and counting) |

`grep -rE "created_by\s+VARCHAR" kiteclass/kiteclass-core/src/main/resources/db/migration/` returns 10+ hits across at least six migration files.

## Why this hasn't blown up earlier

- **Production**: `ddl-auto: validate`. As long as a tenant's DB was migrated incrementally (V1 → V25 BIGINT, then V29+ added new tables with VARCHAR), Hibernate validates each entity against its own table — and the entity ↔ column comparison runs against the **same** column. So if somewhere V29+ entity had `private String createdBy`, validation passed. **This means there's almost certainly an entity layer somewhere with `String createdBy`** — or all V29+ entities skip BaseEntity.
- **Tests**: `ddl-auto: create-drop` overrides Flyway entirely; Hibernate creates the schema from entity types so no mismatch surfaces.
- **Wave 4 hardening** (per memory `feedback_jpa_jsonb_jdbctypecode.md`): six `jsonb` columns went latent — same class of issue. Likely a code-review pattern where audit columns aren't checked against BaseEntity.

A fresh dev DB starting from V1 with `validate` blows up on the first V29+ table whose entity DOES extend BaseEntity (`AcademicYear` was the first one we hit).

## Proposed Fix

Two viable paths — pick one, don't mix:

### Path A — Standardize on `BIGINT` (canonical, recommended)

1. Add migration `V46__align_created_by_updated_by_to_bigint.sql` doing one of:
   - `ALTER TABLE ... ALTER COLUMN created_by TYPE BIGINT USING NULLIF(created_by, '')::BIGINT;` for every V29+ table.
   - Drop and re-add the column if data is dev-only (faster, but breaks audit history).
2. Audit any entity that has `private String createdBy` — these would now break `validate`. Likely candidates: V29+ entities that defined their own audit fields instead of extending `BaseEntity`.
3. Verify: full `mvn test` + boot `kiteclass-core --spring.profiles.active=local` against a freshly migrated DB.

### Path B — Standardize on `VARCHAR`

Less work on entity side IF most V29+ entities are already `String`-typed. Requires:
1. Change `BaseEntity.createdBy` / `updatedBy` to `String`.
2. Migration to convert V1–V25 `BIGINT` columns to `VARCHAR(100)`: `ALTER TABLE ... USING created_by::VARCHAR;`
3. Audit `AuditorAware` bean — likely returns Long today, change to return String (probably the user ID `toString()`).
4. Touch every entity that extends BaseEntity if there's any direct numeric arithmetic on `createdBy` (unlikely, but check).

## Acceptance Criteria

- [ ] Boot `kiteclass-core` with `--spring.profiles.active=local` against a freshly Flyway-migrated DB without schema-validation errors
- [ ] Full `mvn test` green (no regression in 1100+ test suite)
- [ ] At least one new test that boots the FULL Spring context with `ddl-auto: validate` against a Flyway-migrated DB to catch this kind of drift early
- [ ] Migration is **idempotent** + safe under concurrent writes (use `ALTER TABLE IF EXISTS` + `IF NOT EXISTS` on column ops)

## Out-of-scope

- Production data migration plan — separate ops PR with downtime window if Path A is chosen and existing tenants have non-numeric values
- Refactoring `AuditorAware` provider — only relevant for Path B
- Tenant DB rolling upgrade strategy — runbook in 05-guides

## Workaround (today, for dev only)

`application-dev.yml` overrides `ddl-auto: create-drop` + disables Flyway, letting Hibernate generate matching schema from entities:

```yaml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Productionsafe? **No.** Dev convenience only.

## Related

- Memory: `feedback_jpa_jsonb_jdbctypecode.md` (sibling Wave 4 schema-drift case — `jsonb` columns missing `@JdbcTypeCode`)
- GAP-242: admin module Flyway test incompatibility (already-fixed cousin issue)
- GAP-235 Sub-PR G live-screenshot attempt that surfaced this gap (PR #591)

## Log

- **2026-04-28** — Path A shipped. Entity-layer audit confirmed all 23 V29+ entities extend BaseEntity (Long); domain code never reads `createdBy` / `updatedBy` numerically; `AuditorAware<Long>` returns `Optional<Long>`. So column ALTER is the only required change. `V46__align_audit_columns_to_bigint.sql` uses an idempotent DO block scanning `information_schema.columns` for VARCHAR/text audit columns on the 19 affected tables and ALTERs each with `USING NULLIF(col,'')::BIGINT`. `Wave02MigrationsTest` extended with column-type assertion (proves alignment post-migration). 1123/1123 kiteclass-core tests green.
- **2026-04-27** — Filed during GAP-235 Sub-PR G live-screenshot attempt. `dev-start.sh` boot of Core failed with `Schema-validation: wrong column type encountered in column [created_by] in table [academic_years]; found [varchar], but expecting [bigint]`. Workaround applied to dev profile via `ddl-auto: create-drop`; root-cause repair tracked here. Path A (BIGINT canonical) recommended pending entity-layer audit.
