# GAP-1483: Per-tenant physical-DB provisioning vs shared-DB model — câu hỏi thiết kế

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-18 (Bước 2 deploy — handoff Gap 3 design question)
**Affects:** kitehub-subscription `DatabaseProvisioningService` + beta-signup flow

## Problem

Handoff 2026-06-18 Gap 3 fix làm beta-signup provisioning **thành công** (admin conn `DATABASE_ADMIN_*` trỏ RDS master thay vì localhost:5433). Nhưng nổi lên câu hỏi thiết kế: `application-production.yml` đặt `database.lifecycle.enabled=true` → mỗi beta-signup `createPhysicalDatabase` tạo 1 DB vật lý per-tenant (`kiteclass_<hash>`). Trong khi model thực tế = **shared-DB + RLS** (ADR-023, kiteclass-core dùng `kiteclass_shared` chung cho mọi tenant).

→ Per-tenant physical DB là **vestigial** (per handoff note) — tạo ra nhưng kiteclass-core không dùng (nó dùng kiteclass_shared + RLS). Provisioning vẫn phải chạy (GAP-946 fail-loud), nhưng tạo DB vật lý thừa = lãng phí + nhầm lẫn model.

## Proposed Fix (cần quyết định thiết kế)

Chọn 1:
- **(a)** Giữ provisioning physical DB (fail-loud check) nhưng document rõ nó vestigial; HOẶC
- **(b)** Tắt `database.lifecycle.enabled` (hoặc đổi `createPhysicalDatabase` thành no-op/RLS-row-insert) cho shared-DB model — beta-signup chỉ tạo tenant row + RLS scope, không tạo DB vật lý.

Cần đối chiếu ADR-023 (shared-DB RLS) + GAP-946 (fail-loud) trước khi quyết.

## Acceptance Criteria

- [ ] Quyết định thiết kế ghi vào ADR (giữ vs bỏ per-tenant physical DB)
- [ ] Code khớp quyết định (provisioning logic + lifecycle flag)
- [ ] beta-signup vẫn 200 + tenant truy cập được qua RLS

## Related

- Discovered in: PR #2490 deploy session 2026-06-18
- ADR-023 (gateway key resolver / shared-DB RLS), GAP-946 (fail-loud provisioning check)
