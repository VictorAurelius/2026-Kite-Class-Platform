# GAP-887: `student_badges` thiếu `instance_id` → không có RLS policy

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC gamification)
**Affects:** `kiteclass-core` module gamification; `student_badges` table

## Problem

5/6 bảng gamification có `instance_id` + RLS FORCED. `student_badges` KHÔNG có `instance_id` (V1 không khai, V26 không thêm). V58/V59 sanity-check skip → bảng không có policy `tenant_isolation`.

Cô lập tenant gián tiếp qua FK (student_id/badge_id). Raw `SELECT * FROM student_badges` không qua join trả mọi dòng cross-tenant. Hiện không có entity JPA cho bảng này → access hạn chế, nhưng future feature wire entity sẽ cần RLS.

## Proposed Fix

Migration thêm `instance_id UUID NOT NULL` + backfill từ FK + add RLS policy. Batch với gamification entity wave nếu plan.

## Acceptance Criteria

- [ ] Migration V## add `instance_id` + backfill + index + RLS
- [ ] Reference cluster doc 06-gamification §A

## Discovered in

`documents/02-architecture/database/kiteclass/06-gamification.md` §A
