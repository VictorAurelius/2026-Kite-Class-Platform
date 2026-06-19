# GAP-987: RLS layer invisible trong test profile (Flyway disabled + ddl-auto create-drop)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (test coverage)
**Found:** 2026-06-05 (Wave security-1 Bucket A spike — discovery)
**Affects:** `application-test.yml` (Flyway off, `ddl-auto: create-drop`), mọi Testcontainers IT

## Problem

Test profile tắt Flyway + dùng `ddl-auto: create-drop` → RLS migrations (V58+) KHÔNG áp dụng trong test → IT suite chỉ validate lớp `@Filter`, KHÔNG validate lớp RLS. Nghĩa là defense-in-depth RLS không có regression guard ở test (xem [[GAP-985]]). GAP-983 fix được test ở lớp @Filter (đủ cho leak hiện tại) nhưng RLS layer untested.

## Proposed Fix

Thêm 1 Testcontainers IT chạy real Flyway (profile riêng `test-flyway` hoặc dedicated IT với `ddl-auto: none` + Flyway on) verify RLS policy chặn cross-tenant ở DB layer độc lập @Filter.

## Acceptance Criteria
- [ ] 1 IT chạy Flyway thật trên Postgres Testcontainer + assert RLS chặn cross-tenant by-id khi @Filter disabled

## Related
- Discovered in: Wave security-1 spike 2026-06-05
- Pairs với: [[GAP-985]] RLS layer fix
