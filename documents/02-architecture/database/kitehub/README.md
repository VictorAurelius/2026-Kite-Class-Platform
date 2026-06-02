---
title: KiteHub Database — Schema Reference (control-plane, 33 bảng)
audience: mixed
status: active
created: 2026-06-02
last-reviewed: 2026-06-02
---

# KiteHub Database — Schema Reference

Database `kitehub` (control-plane) — sở hữu migration bởi `kitehub-subscription`, chia sẻ bởi `kitehub-platform`/`branding`/`admin`. 33 bảng, 4 cluster. Xem [README gốc](../README.md) cho quy ước chung + 2-database overview.

## Cluster index

| File | Cluster |
|---|---|
| [`01-auth-user-instance.md`](01-auth-user-instance.md) | Auth / User / Instance |
| [`02-subscription-billing.md`](02-subscription-billing.md) | Subscription / Billing |
| [`03-branding.md`](03-branding.md) | Branding / AI job / Outbox |
| [`04-email-compliance-admin.md`](04-email-compliance-admin.md) | Email / Compliance / Admin / Staff |

## ERD tổng KiteHub

> Lắp ráp sau khi 4 cluster file hoàn tất (FK graph toàn DB từ các cluster). Mỗi cluster file có ERD riêng của cluster đó. Tham chiếu nhanh FK graph high-level: [`database-architecture-map.md` §2](../../database-architecture-map.md).
