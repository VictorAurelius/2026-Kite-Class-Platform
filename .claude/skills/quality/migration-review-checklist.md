---
name: migration-review
description: "Dùng khi PR có Flyway migration (V*.sql), 'review migration', 'check migration', 'kiểm tra migration'. DBA checklist."
user-invocable: true
---

# Database Migration Review Checklist

Dùng khi PR thêm/sửa Flyway migrations (`V*__*.sql`, `U*__*.sql`) trong `src/main/resources/db/migration/`.

## Pre-merge Checklist

- [ ] **Backward compatible?** Old code can run against new schema (ADD COLUMN OK, DROP COLUMN needs deprecation period)
- [ ] **Rollback script?** Corresponding `U*__*.sql` undo migration provided, OR migration is non-destructive (ADD only)
- [ ] **Index impact?** New indexes on large tables use `CREATE INDEX CONCURRENTLY` (no table lock)
- [ ] **Data migration safe?** No long-running locks; batch UPDATE/DELETE for large tables (1000 rows/batch)
- [ ] **Default values?** NOT NULL columns have DEFAULT; no backfill query that locks entire table
- [ ] **Multi-tenant safe?** Changes affect all tenants equally; no tenant-specific DDL
- [ ] **Naming convention?** `V{N}__{description}.sql` where N follows sequence; description is snake_case
- [ ] **No DROP without backup?** DROP TABLE/COLUMN preceded by data export or confirmed obsolete
- [ ] **Lock duration?** ALTER TABLE on tables >100k rows must avoid ACCESS EXCLUSIVE lock
- [ ] **Tested?** Run on staging with production-like data volume; migration duration measured

## Flyway-specific Rules

- V-migrations are **one-way** — once applied, cannot be modified (checksum mismatch)
- Always check current version: `flyway info` before adding new migration
- Migration numbering must be sequential (no gaps, no duplicates)
- SQL must be idempotent where possible (`IF NOT EXISTS`, `IF EXISTS`)

## Gotchas

- PostgreSQL `ALTER TABLE ADD COLUMN ... DEFAULT` is fast since PG 11 (no rewrite)
- `CREATE INDEX CONCURRENTLY` cannot run inside a transaction — Flyway needs `-- flyway:executeInTransaction=false`
- Renaming columns breaks JPA mappings — coordinate with code change in same PR
- ENUM type changes (`ALTER TYPE ... ADD VALUE`) cannot be rolled back in PG
