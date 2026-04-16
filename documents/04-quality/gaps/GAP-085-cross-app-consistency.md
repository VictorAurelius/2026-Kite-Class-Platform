# GAP-085: Cross-App Consistency Check

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Architecture / Quality
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** KiteHub + KiteClass shared infrastructure

## Problem

KiteHub và KiteClass share: PostgreSQL, Redis, RabbitMQ, MinIO (prefix `kite-`). Khi 1 app thay đổi schema hoặc message format, app kia có thể break mà không ai biết.

Không có skill verify: cả 2 apps implement cùng business rules, cùng message contracts, cùng DB schema expectations.

## Proposed Fix

1. Tạo checklist trong `quality/cross-app-check.md`:
   - [ ] Shared DB tables: schema changes communicated to both apps
   - [ ] RabbitMQ message format: producer + consumer aligned
   - [ ] Redis key patterns: no namespace collision
   - [ ] Config keys: shared infrastructure config consistent
   - [ ] Business rules: same rule ID (BR-xxx) implemented same way in both apps
2. Trigger: khi PR touches shared infrastructure code (`kite-*` prefixed services)
3. Part of wave-completion-check Level 5 (integration)

## Acceptance Criteria

- [ ] Checklist exists
- [ ] PRs touching shared code require cross-app check
- [ ] No undocumented shared schema dependencies
