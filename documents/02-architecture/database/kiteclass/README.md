---
title: KiteClass Database — Schema Reference (multi-tenant domain, ~65 bảng)
audience: mixed
status: active
created: 2026-06-02
last-reviewed: 2026-06-02
---

# KiteClass Database — Schema Reference

Database `kiteclass` (multi-tenant domain) — sở hữu migration bởi `kiteclass-core`. ~67 bảng tenant-scoped (`instance_id` + RLS FORCED per V58 — **trừ cụm LMS tạo ở V79 chưa enable RLS DB-level**, xem [`09-lms.md`](09-lms.md) anomaly A + GAP-1112), 9 cluster. Xem [README gốc](../README.md) cho quy ước chung + multi-tenant overview.

## Cluster index

| File | Cluster |
|---|---|
| [`01-academic-structure.md`](01-academic-structure.md) | Cấu trúc học vụ |
| [`02-people-enrollment.md`](02-people-enrollment.md) | Con người / Ghi danh |
| [`03-attendance-grading.md`](03-attendance-grading.md) | Điểm danh / Điểm số |
| [`04-finance.md`](04-finance.md) | Tài chính / Lương |
| [`05-rbac.md`](05-rbac.md) | Phân quyền (RBAC) |
| [`06-gamification.md`](06-gamification.md) | Gamification |
| [`07-compliance-audit.md`](07-compliance-audit.md) | Compliance / Audit / Moderation |
| [`08-branding-marketing.md`](08-branding-marketing.md) | Branding / Marketing / Infra |
| [`09-lms.md`](09-lms.md) | LMS / Nội dung học tập |

## ERD tổng KiteClass

> Lắp ráp sau khi 9 cluster file hoàn tất. `students` là entity trung tâm (top FK target). Mỗi cluster file có ERD riêng. Tham chiếu FK graph high-level: [`database-architecture-map.md` §2](../../database-architecture-map.md).
