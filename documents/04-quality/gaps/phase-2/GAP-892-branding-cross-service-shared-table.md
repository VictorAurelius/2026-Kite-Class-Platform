# GAP-892: `branding` table cross-service shared — race risk + documentation gap

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / Architecture
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC branding/marketing)
**Affects:** `branding` table — owned bởi `kitehub-branding` (KH) + cross-service đọc/ghi từ `kiteclass-core`

## Problem

`branding` originally provision bởi `kitehub-branding`. V40 (GAP-065) thêm `CREATE TABLE IF NOT EXISTS` ở kiteclass-core (idempotent với existing env). Hệ quả: cả 2 service đọc/ghi cùng bảng `branding` — pattern hiếm.

Hiện kiteclass-core chỉ read (branding render); KH branding service own write. Race nếu cả 2 ghi. Invariant ai own write KHÔNG documented rõ.

## Proposed Fix

Document ownership invariant trong `documents/02-architecture/multi-tenant-architecture.md`. Apply lock contract qua code (ReadOnly annotation hoặc dedicated read role). Future refactor: tách thành 2 bảng riêng nếu access pattern diverge.

## Acceptance Criteria

- [ ] Ownership invariant documented
- [ ] Code review enforce read-only access ở kiteclass-core
- [ ] Reference cluster doc 08-branding-marketing §A7

## Discovered in

`documents/02-architecture/database/kiteclass/08-branding-marketing.md` §A7
