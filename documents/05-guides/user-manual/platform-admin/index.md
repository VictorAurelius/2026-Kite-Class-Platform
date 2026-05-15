---
persona: platform-admin
topic: index
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 4
---

# Hướng dẫn Platform Admin — Tổng quan dashboard

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **4 phút**

## TL;DR

Platform Admin (em Mai) là người vận hành toàn hệ thống KiteHub — duyệt beta tenant mới, theo dõi sức khoẻ hệ thống, hỗ trợ tenant qua impersonation.

- 🎯 **Vai trò:** Internal admin (KiteHub team), KHÔNG phải tenant user
- 🔑 **Đăng nhập:** `admin@kitehub.me` qua `/admin/login` (PLATFORM_ADMIN role)
- 📊 **6 module chính:** Tenant approval · Impersonation · Monitoring · Tenant management · Email events · Audit log
- 🛡️ **Trách nhiệm:** SLO uptime 99.5%, beta cohort growth, incident response

---

## 1. Em Mai làm gì hàng ngày?

| Khung giờ | Việc làm | Thời lượng |
|---|---|---|
| 8h30 sáng | Check dashboard sức khoẻ hệ thống (`/admin/monitoring`) | 5 phút |
| 9h00-10h00 | Duyệt beta access request mới (`/admin/beta-requests`) | 30-60 phút |
| 10h-12h | Trả lời support ticket từ tenant, có thể dùng impersonation để debug | 1-2 giờ |
| Chiều | Review audit log, file gap khi phát hiện anomaly | 1-2 giờ |
| Trước về | Snapshot DB backup verify chạy đúng (Velero/RDS automated) | 5 phút |

---

## 2. 6 module trong Admin Console

<!-- Screenshot placeholder: capture index-step-1.png — 1440×900 vi-VN — show /admin sidebar nav với 6 module: Beta Requests, Impersonation, Monitoring, Tenants, Email Events, Audit Log -->

### 2.1 Beta Approval

Duyệt yêu cầu truy cập Beta từ Anonymous Prospect (chị Hằng / em Vy submit form). Xem chi tiết [Duyệt Beta tenant](beta-approval.md).

### 2.2 Impersonation

Đăng nhập tạm thời vào tenant (TTL 30 giây) để debug — không tiết lộ password tenant. Xem [Impersonation tool](impersonation.md).

### 2.3 Monitoring

Theo dõi sức khoẻ hệ thống realtime: error rate, P95 latency, queue depth. Xem [Theo dõi hệ thống](monitoring.md).

### 2.4 Tenant Management

Bulk operations: suspend tenant không trả phí, export tenant data theo PDPL request. Xem [Quản lý tenant](tenant-management.md).

### 2.5 Email Events

Xem trạng thái email transactional (Resend) — verify đã giao tới phụ huynh/giáo viên. Khi tenant phàn nàn "không nhận được email" → check ở đây.

### 2.6 Audit Log

Xem mọi action nhạy cảm: impersonation entry/exit, bulk delete, role change, billing override. Required cho PDPL compliance.

---

## 3. Quyền hạn (RBAC)

Platform Admin có scope toàn hệ thống — đứng trên mọi tenant:

| Action | PLATFORM_ADMIN | OWNER | STAFF |
|---|:---:|:---:|:---:|
| Duyệt beta tenant mới | ✅ | ❌ | ❌ |
| Impersonate tenant user | ✅ | ❌ | ❌ |
| Xem audit log toàn hệ thống | ✅ | (chỉ tenant mình) | ❌ |
| Suspend tenant | ✅ | ❌ | ❌ |
| Export tenant data (PDPL) | ✅ | (chỉ tenant mình) | ❌ |
| Cấu hình email transactional | ✅ | ❌ | ❌ |

Quyền `PLATFORM_ADMIN` chỉ được cấp cho internal KiteHub team — KHÔNG bao giờ cấp cho tenant.

---

## 4. Quy trình khi có incident

1. Phát hiện qua monitoring (error rate spike, P95 latency >2s, queue lag) hoặc tenant báo
2. Mở `/admin/audit-log` lọc theo timestamp + tenant_id
3. Nếu cần debug sâu → dùng impersonation (TTL 30s, audit log auto-record)
4. File gap trong `documents/04-quality/gaps/` với severity P0/P1
5. Hotfix → deploy qua workflow_dispatch (per `release-deploy-standard.md` §9)
6. Post-mortem trong 48h, link gap + ROADMAP entry

---

## 5. Tiếp theo

- 🆕 [Duyệt Beta tenant](beta-approval.md)
- 🔐 [Impersonation tool 30s TTL](impersonation.md)
- 📊 [Theo dõi hệ thống](monitoring.md)
- 🏢 [Quản lý tenant bulk operations](tenant-management.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email nội bộ: [admin-support@kitehub.me](mailto:admin-support@kitehub.me)
- 📚 Operations runbook: [`documents/05-guides/operations/`](../../operations/)
- 🐛 Báo lỗi: file gap trong [`documents/04-quality/gaps/`](../../../04-quality/gaps/)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
