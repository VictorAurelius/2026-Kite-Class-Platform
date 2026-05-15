---
persona: platform-admin
topic: tenant-management
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 5
---

# Quản lý tenant — Bulk operations + PDPL data export

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **5 phút**

## TL;DR

Khi tenant không trả phí, hết hạn beta, hoặc yêu cầu PDPL data export — em Mai dùng `/admin/tenants` để suspend/export/delete data theo quy trình.

- 🏢 **List view:** Tất cả tenant với status (TRIAL, ACTIVE, SUSPENDED, EXPIRED, DELETED)
- 🚫 **Suspend:** Khoá đăng nhập tenant không trả phí, giữ data 90 ngày
- 📥 **Export PDPL:** Tenant request data → xuất ZIP toàn bộ data tenant
- 🗑️ **Delete:** Hard-delete sau 90 ngày suspended hoặc tenant request xoá

---

## 1. Mở danh sách tenant

<!-- Screenshot placeholder: capture tenant-management-step-1.png — 1440×900 vi-VN — show /admin/tenants table với 5 mock rows (Sky Education ACTIVE 120hs / Quang Minh TRIAL 45hs / Phương Đông SUSPENDED 80hs ...) + filter dropdown status + nút "Bulk action" -->

URL: `/admin/tenants`

| Cột | Ý nghĩa |
|---|---|
| Center Name | Tên trung tâm |
| Status | TRIAL / ACTIVE / SUSPENDED / EXPIRED / DELETED |
| Tier | FREE / PRO / PREMIUM / ENTERPRISE / BETA_FREE_6M |
| Students | Số học sinh hiện tại |
| MRR | Doanh thu hàng tháng (VND) |
| Created | Ngày tạo |
| Last Active | Ngày active cuối |

Filter: by status / tier / region / date range.

---

## 2. Suspend tenant (không trả phí)

### 2.1 Khi nào suspend?

- Tenant `ACTIVE` quá hạn invoice 30 ngày → suspend
- Tenant Beta quá 6 tháng không upgrade tier → suspend (FREE tier vẫn ok dưới 30 hs)
- Tenant vi phạm Terms (spam, scam) → suspend ngay + report

### 2.2 Quy trình suspend

<!-- Screenshot placeholder: capture tenant-management-step-2.png — 1440×900 vi-VN — show suspend modal "Xác nhận suspend Sky Education? Tenant sẽ bị khoá đăng nhập, data giữ 90 ngày" + textarea reason + nút Đồng ý mũi tên đỏ -->

Click tenant row → **Action** dropdown → **Suspend**.

Modal yêu cầu:
- **Lý do** (≥20 ký tự, ghi audit log)
- **Notification** (checkbox): gửi email tenant owner "Tài khoản đã bị tạm khoá vì..."
- Confirm

Hệ thống:
1. Update `tenants.status=SUSPENDED`, `suspended_at=now()`, `suspended_by=admin@kitehub.me`, `suspended_reason`
2. Revoke all active JWT của tenant (force logout)
3. Set `tenants.deletion_eligible_at=now()+90 days`
4. Write audit log `tenant.suspend`
5. Gửi email tenant owner (nếu checkbox bật)

### 2.3 Tenant sau khi suspend

- Cannot login `/dashboard` → trang báo "Tài khoản đã bị tạm khoá. Liên hệ support@kitehub.me"
- Data giữ nguyên 90 ngày
- Có thể reactivate trong 90 ngày: **Action** dropdown → **Reactivate**

---

## 3. Export data PDPL request

Theo PDPL 2023 + Decree 13/2023/NĐ-CP, tenant có quyền yêu cầu xuất toàn bộ data cá nhân.

### 3.1 Quy trình export

<!-- Screenshot placeholder: capture tenant-management-step-3.png — 1440×900 vi-VN — show /admin/tenants/{id}/export view với checklist (Students data / Teachers / Classes / Payments / Attendance / Audit log) + nút "Tạo ZIP export" mũi tên đỏ -->

Mở detail tenant → tab **Data Export** → checklist module muốn export:

- [ ] Students (full PII)
- [ ] Teachers
- [ ] Classes + Schedules
- [ ] Payments + Invoices
- [ ] Attendance records
- [ ] Audit log

Click **Tạo ZIP export** → hệ thống chạy background job tạo file ZIP encrypted với password gửi tenant qua email khác channel.

Format ZIP:
```
tenant-{id}-export-2026-05-15.zip
├── students.csv (BOM UTF-8)
├── teachers.csv
├── classes.csv
├── payments.csv
├── attendance.csv
├── audit-log.csv
└── README.md (cấu trúc file, instruction)
```

### 3.2 Email password kèm export

Sau khi ZIP ready (T+5-15 phút tuỳ data size):
1. Hệ thống gửi email 1 chứa link download (TTL 24h)
2. Hệ thống gửi SMS hoặc Zalo password ZIP (nếu có verified phone)
3. Tenant tải về + giải nén → đọc data

---

## 4. Hard delete (PDPL right to erasure)

⚠️ **Không thể undo** — verify kỹ trước khi click.

### 4.1 Khi nào delete?

- Suspended ≥90 ngày + không reactivate → hệ thống tự cảnh báo
- Tenant request "xoá toàn bộ dữ liệu của tôi" theo PDPL
- Quyết định pháp lý từ cơ quan có thẩm quyền

### 4.2 Quy trình delete

<!-- Screenshot placeholder: capture tenant-management-step-4.png — 1440×900 vi-VN — show hard-delete confirmation modal với cảnh báo "⚠️ KHÔNG THỂ UNDO — tất cả data tenant sẽ bị xoá vĩnh viễn" + checkbox "Tôi đã backup audit log" + input "Gõ DELETE để xác nhận" + nút đỏ -->

Detail tenant → **Danger zone** → **Hard delete**.

Modal yêu cầu:
- [ ] Tick: Đã backup audit log tenant
- [ ] Tick: Có consent từ tenant owner hoặc legal mandate
- Gõ verbatim `DELETE-{tenant-name}` để confirm
- Click **Xoá vĩnh viễn**

Hệ thống:
1. Backup audit log row vào `archive_tenants_deleted` (giữ 7 năm theo legal retention)
2. CASCADE DELETE tất cả row liên quan tenant (students, teachers, classes, payments, attendance, ...)
3. Delete MinIO/S3 objects của tenant (logo, branding assets, exported files)
4. Update `tenants.status=DELETED`, `deleted_at=now()`, `deleted_by=admin@kitehub.me`
5. Write audit log `tenant.delete` với row count xoá per bảng

---

## 5. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| Suspend nhưng tenant vẫn login được | JWT chưa expire (cache) | Wait 5 phút hoặc revoke JWT manually qua /admin/sessions |
| Export ZIP fail "Out of memory" | Tenant >1000 hs, data quá lớn | Chunk export per module, gửi nhiều ZIP nhỏ |
| Hard delete fail FK violation | Quên CASCADE order | Check migration `V*__cascade-tenant.sql` đã apply |
| Tenant claim "data của tôi bị mất" sau suspend | Hiểu nhầm | Show audit log, data vẫn còn 90 ngày sau suspend |

---

## 6. Liên kết

- [Tổng quan Platform Admin](index.md)
- [Beta Approval](beta-approval.md)
- [Impersonation](impersonation.md)
- PDPL compliance docs: [`documents/01-business/compliance/`](../../../01-business/)

---

## 🆘 Cần hỗ trợ?

- 📧 Email nội bộ: [admin-support@kitehub.me](mailto:admin-support@kitehub.me)
- ⚖️ Legal escalation: contact legal counsel trước khi hard-delete khi unsure
- 📊 Trạng thái beta: [/beta-status](/beta-status)
