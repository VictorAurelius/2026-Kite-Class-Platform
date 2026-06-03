---
id: GAP-914
title: Payment entity thiếu instanceId field (V58 thêm NOT NULL) → mọi payment insert vỡ trên Postgres
status: DONE
priority: P0
phase: phase-1-beta
audience: dev
found: 2026-06-04
last_verified: 2026-06-04
completion_pct: 100
related: [GAP-896, GAP-880, GAP-913, GAP-885]
---

# GAP-914 — Payment entity thiếu instanceId (V58 RLS drift) → payment insert vỡ trên Postgres

## Problem

`V58__rls_sweep_kh.sql` (applied) thêm `payments.instance_id UUID NOT NULL` (backfill từ subscriptions, RLS tenant isolation):

```sql
ALTER TABLE payments ADD COLUMN IF NOT EXISTS instance_id UUID;
UPDATE payments p SET instance_id = s.instance_id FROM subscriptions s WHERE ...;
ALTER TABLE payments ALTER COLUMN instance_id SET NOT NULL;
```

NHƯNG JPA entity `kitehub-platform/.../entity/Payment.java` **chưa bao giờ map field `instanceId`**. Hậu quả: cả 3 site tạo Payment (`SubscriptionService.createProratedPayment`, `PaymentService.createPayment`, `SubscriptionRenewalService.createRenewalPayment`) insert Payment KHÔNG có instance_id → Postgres reject `SQLState 23502 null value in column "instance_id" violates not-null constraint` → `GlobalExceptionHandler` map thành HTTP 409 RESOURCE_CONFLICT.

**Toàn bộ payment subsystem vỡ trên Postgres thật post-V58.** H2 (test) giấu lỗi vì V58 RLS (`set_config`) không apply trên H2 → 769 unit/IT test xanh nhưng feature vỡ runtime (đúng lớp `postgres-specific-type-testcontainers.md`).

**Phát hiện qua runtime walk** billing upgrade flow (per `feature-ship-runtime-walk-mandate`): GAP-913 verify → upgrade endpoint 409, log lộ `null value in column instance_id`.

Khác các gap hiện có:
- **GAP-896** (P1 OPEN) premise "payments KHÔNG có instance_id (V34 skip)" — **lỗi thời**: V58 (sau V34) đã ADD cột NOT NULL. Entity chưa update mới là bug live.
- **GAP-880** — chiều ngược (entity declare 12+ cột thừa không có trong DB).

## Root Cause

Entity↔migration drift: V58 RLS sweep thêm DB column NOT NULL nhưng Payment entity (shared `kitehub-platform`) không được sync. Wave 13 anomaly class (cùng họ GAP-885 RLS coverage).

## Proposed Fix (DONE — PR #2140)

1. Thêm field `@Column(name="instance_id", nullable=false) private UUID instanceId` vào Payment entity.
2. Set `payment.setInstanceId(subscription.getInstanceId())` tại 3 site tạo Payment.

## Acceptance Criteria

- [x] Payment entity map `instanceId` NOT NULL
- [x] createProratedPayment + createPayment + createRenewalPayment set instanceId
- [x] Runtime walk: upgrade → PENDING payment insert thành công trên Postgres thật (instance_id populated)
- [x] 769 module tests vẫn xanh với entity change

## Walk evidence (per feature-ship-runtime-walk-mandate §3)

Stack: kite-postgres + kitehub-subscription (8081) + kitehub-admin (8085) + redis + rabbitmq (local, code mới rebuild).

| Bước | Kết quả |
|---|---|
| OWNER upgrade BASIC→PREMIUM (POST trước fix) | ❌ HTTP 409 — `null value in column instance_id` (bug) |
| (fix entity + 3 site) → re-walk upgrade | ✅ HTTP 200, tier=BASIC, pendingTier=PREMIUM, PENDING payment với instance_id=22003e3c... amount 633333 VND VIETQR |
| Admin confirm payment | ✅ HTTP 200 payment COMPLETED + transactionId + paidAt |
| → side effect | ✅ subscription tier=PREMIUM (price 1.5M), pending cleared |
| Admin reject (sad) | ✅ payment FAILED, tier giữ BASIC, pending cleared |
| Idempotency (upgrade 2x same tier) | ✅ reuse same payment, 1 PENDING row |
| Conflict (upgrade khác tier khi pending) | ✅ HTTP 400 "already has a pending upgrade payment" |

## Related

- Fixed in: **PR #2140** (cùng billing manual-payment feature)
- [[GAP-896]] — premise superseded by V58 (cần update/close); RLS tenant isolation cho payments giờ DIRECT qua instance_id
- [[GAP-880]] — Payment entity drift chiều ngược (cột thừa)
- [[GAP-913]] — billing upgrade manual-payment (runtime walk này unblock)
- [[GAP-885]] — RLS coverage Wave 13 anomaly cluster
