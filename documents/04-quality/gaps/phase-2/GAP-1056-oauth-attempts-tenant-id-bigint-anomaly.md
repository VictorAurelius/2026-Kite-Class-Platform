# GAP-1056: oauth_attempts.tenant_id là BIGINT — nên re-key sang instance_id UUID

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB
**Found:** 2026-06-07 (Wave p0-local-1 Bucket B — oauth_attempts RLS V66)
**Affects:** `kitehub-subscription` bảng `oauth_attempts`

## Problem

`oauth_attempts.tenant_id` là `BIGINT NULL` (V51 / GAP-582) trong khi MỌI bảng tenant-scoped KH khác key trên `instance_id UUID`. Đây là lý do `V58__rls_sweep_kh.sql` SKIP nó (guard yêu cầu `data_type='uuid'`). V66 (GAP-885) phải dùng predicate `tenant_id::text = NULLIF(current_setting('app.current_tenant_id', true), '')` để né cast mismatch `::uuid` (GUC mang UUID-string vs cột BIGINT) — workaround hợp lệ nhưng cột vẫn lệch convention.

## Proposed Fix

Re-key `oauth_attempts.tenant_id` BIGINT → `instance_id UUID` (đồng bộ convention KH tenant-scoped tables) + sửa RLS policy V66 về predicate UUID chuẩn. Defer tới khi OAuth signup flow được implement (oauth_attempts hiện chưa có caller — defensive scaffolding, không data thật → migration an toàn).

## Acceptance Criteria

- [ ] Migration re-key `tenant_id` BIGINT → `instance_id UUID`
- [ ] V66 RLS policy predicate đổi về `instance_id = current_setting(...)::uuid` chuẩn
- [ ] IT cập nhật verify isolation trên cột mới

## Related

- Discovered in: Wave p0-local-1 Bucket B (V66 oauth_attempts RLS)
- Sister: [[GAP-877]] (cùng lớp BIGINT/UUID actor-id drift), [[GAP-885]] (RLS coverage)
