# GAP-082: Database Migration Review Checklist

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend / Data Safety
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** All Flyway migrations (kiteclass-core, kitehub services)

## Problem

`output-review-mandate.md` flags migrations as VIOLATION — relied on code review only. No dedicated checklist for: backward compatibility, rollback script, index impact, lock duration, data safety.

1 sai migration = data loss không rollback được (Flyway V-migrations are one-way).

## Proposed Fix

1. Tạo checklist `quality/migration-review/SKILL.md`:
   - [ ] Backward compatible? (old code still works with new schema)
   - [ ] Rollback script provided? (U-migration hoặc manual SQL)
   - [ ] Index impact assessed? (large table + new index = lock)
   - [ ] No `DROP COLUMN/TABLE` without data backup confirmation
   - [ ] Tested on staging with production-like data volume
   - [ ] Lock duration < 30s (no ALTER TABLE on large table without CONCURRENTLY)
   - [ ] Naming convention: `V{date}__{description}.sql`
2. Thêm vào PR template: "[ ] Migration: reviewed per migration-review checklist"

## Acceptance Criteria

- [ ] Skill file tồn tại
- [ ] All existing migrations pass checklist (hoặc exceptions documented)
- [ ] New migration PRs require checklist completion
