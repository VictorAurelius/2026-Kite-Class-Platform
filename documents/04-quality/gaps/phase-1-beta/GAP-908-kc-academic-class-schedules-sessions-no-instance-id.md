# GAP-908: KC academic structure `class_schedules` + `class_sessions` thiếu `instance_id` direct

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC academic structure)
**Affects:** `kiteclass-core` `class_schedules`, `class_sessions`, `course_prerequisites` tables

## Problem

11/12 bảng cluster academic structure có cột `instance_id UUID` và bật RLS FORCED. Hai bảng con `class_schedules` và `class_sessions` cùng bảng nối `course_prerequisites` KHÔNG có `instance_id` (scope gián tiếp qua FK tới `classes`/`courses`).

Pattern tương tự GAP-887 (`student_badges`) — RLS gián tiếp qua join, raw query bypass FK → cross-tenant leak risk.

## Proposed Fix

Migration V## denormalize `instance_id` (backfill từ classes/courses FK) + add RLS policy. Hoặc document accept risk + audit code path không raw query.

## Acceptance Criteria

- [ ] Decision documented (denormalize vs accept)
- [ ] Nếu denormalize: migration V## + RLS
- [ ] Reference cluster doc 01-academic-structure narrative

## Discovered in

`documents/02-architecture/database/kiteclass/01-academic-structure.md` narrative section
