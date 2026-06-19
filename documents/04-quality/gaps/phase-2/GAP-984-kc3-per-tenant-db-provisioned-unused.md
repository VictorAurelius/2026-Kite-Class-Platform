# GAP-984: Per-tenant DB provisioned nhưng kiteclass-core dùng shared DB — isolation model mismatch

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DevOps (architecture)
**Found:** 2026-06-05 (Wave flow-kc3 KC-3 walk state-check)
**Affects:** Tenant provisioning vs runtime datasource; `instances.database_url` semantics

## Problem

Phát hiện trong KC-3 walk: **mô hình isolation khai báo ≠ mô hình isolation thực tế.**

- `instances.database_url` của sky-education khai báo per-tenant DB: `jdbc:postgresql://kite-postgres:5432/kiteclass_0edaee10` (+ user `kiteclass_0edaee10_user`).
- Các DB per-tenant TỒN TẠI thật (provisioned đầy đủ schema): `kiteclass_0abe093c`, `kiteclass_0edaee10`, `kiteclass_126eaa8c`, `kiteclass_ad0fa96e`.
- NHƯNG kiteclass-core thực tế connect **shared DB**: `SPRING_DATASOURCE_URL=jdbc:postgresql://kite-postgres:5432/kiteclass_shared`.
- Mọi data (course/class/session/teacher của TẤT CẢ tenant) pool trong `kiteclass_shared`, isolate bằng cột `instance_id`. Các per-tenant DB nhận **0 row** — provisioned nhưng dead.

Hệ quả:
1. **Declared model (DB-per-tenant) misleading** — đọc `instances.database_url` tưởng strong isolation (DB riêng/user riêng), thực tế là shared-DB + column isolation (yếu hơn, phụ thuộc app filter — xem [[GAP-983]] leak).
2. **Dead provisioning cost** — tạo + maintain N per-tenant DB không dùng (storage, connection, migration drift risk: per-tenant DB schema có thể lệch shared DB theo thời gian).
3. **Thesis/security doc risk** — nếu tài liệu kiến trúc/PDPL claim "mỗi tenant 1 DB riêng" thì sai thực tế (per `thesis-as-future-state-mandate.md` — có thể là goal-state Phase 1.5+ chưa đạt; cần align).

## Proposed Fix

Quyết định + document rõ 1 trong 2 hướng (ADR):
- **(A) Shared-DB canonical:** ngừng provision per-tenant DB; sửa `instances.database_url` semantics (hoặc dùng làm logical key only); document isolation = shared-DB + instance_id + RLS. Hardening đi kèm GAP-983 (bật RLS) + GAP-749 (filter sweep).
- **(B) Per-tenant DB canonical (Phase 1.5+ goal):** wire kiteclass-core multi-datasource routing theo `database_url`; per `thesis-as-future-state-mandate.md` treat như goal-state, file Phase 1.5 wave.

## Acceptance Criteria

- [ ] ADR quyết định shared-DB vs per-tenant-DB là canonical cho Phase 1 BETA
- [ ] `instances.database_url` semantics documented (used vs logical-only)
- [ ] Nếu shared-DB: per-tenant DB provisioning removed HOẶC documented as future (B)
- [ ] Thesis/architecture doc isolation claim aligned với thực tế

## Related

- Discovered in: Wave flow-kc3 KC-3 walk (2026-06-05)
- Isolation leak sibling: [[GAP-983]] (cross-tenant by-id read leak — phụ thuộc shared-DB app filter)
- [[GAP-746]] / [[GAP-749]] tenant filter sweep
- Per `discovery-to-gap-inline-filing.md` §3 + `thesis-as-future-state-mandate.md`
