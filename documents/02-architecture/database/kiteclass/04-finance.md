---
title: "KiteClass DB Schema — Cluster Tài chính / Lương"
audience: mixed
created: 2026-06-02
last-reviewed: 2026-06-02
---

# Cluster Tài chính / Lương (KiteClass)

> **TL;DR** — Cluster này gồm **7 bảng**: `invoices`, `invoice_items`, `payments`, `payment_records`,
> `payment_idempotency_keys`, `payroll_configs`, `payroll_periods`.
>
> - **Hóa đơn**: `invoices` (1 hóa đơn / học sinh) → `invoice_items` (dòng chi tiết, CASCADE).
> - **Thanh toán có 2 hệ song song** (xem [§ anomalies](#-ghi-chú-schema-anomalies)):
>   - `payments` — bản ghi V1, định hướng **cổng thanh toán online** (VNPay/MoMo redirect).
>   - `payment_records` — bản ghi V69 (GAP-292b), định hướng **thu thủ công tại trung tâm** (tiền mặt / chuyển khoản / VietQR / MoMo). Đây là hệ **canonical** cho luồng Phase 1 BETA.
> - **Lương giáo viên**: `payroll_configs` (cấu hình lương / GV) + `payroll_periods` (kỳ lương). Phase 1 chỉ HOURLY.
> - **Idempotency**: `payment_idempotency_keys` (V61, riêng cho parent payment) — chú ý còn 1 bảng `idempotency_keys` (V66) shared cross-domain KHÔNG thuộc cluster này.
> - **Đơn vị tiền**: phần lớn `DECIMAL/NUMERIC(_,2)` VND (đồng có scale 2). `payment_idempotency_keys` comment nói VND minor-unit BIGINT nhưng KHÔNG có cột amount.
> - **RLS** (V58 → V59 hardened): bật trên `invoices`, `payments`, `payroll_configs`, `payroll_periods`. KHÔNG bật trên `invoice_items` (không có `instance_id`), `payment_records` + `payment_idempotency_keys` (tạo sau V58/V59 — RLS chưa apply).

---

## ERD

```mermaid
erDiagram
    students ||--o{ invoices : "student_id"
    classes  ||--o{ invoices : "class_id (nullable)"
    invoices ||--o{ invoice_items : "invoice_id (CASCADE)"
    invoices ||--o{ payments : "invoice_id"
    invoices ||--o{ payment_records : "invoice_id (FK)"
    invoices ||--o{ payment_idempotency_keys : "invoice_id (logical)"
    payments ||--o{ payment_idempotency_keys : "payment_id (logical)"
    teachers ||--o{ payroll_configs : "teacher_id"
    teachers ||--o{ payroll_periods : "teacher_id"

    invoices {
        bigint id PK
        uuid instance_id
        varchar invoice_number
        bigint student_id FK
        bigint class_id FK
        decimal subtotal
        decimal total
        decimal balance_due "GENERATED"
        varchar status
    }
    invoice_items {
        bigint id PK
        bigint invoice_id FK
        varchar description
        decimal unit_price
        decimal amount
    }
    payments {
        bigint id PK
        uuid instance_id
        varchar payment_number
        bigint invoice_id FK
        decimal amount
        varchar payment_method
        varchar status
        bigint received_by
    }
    payment_records {
        bigint id PK
        uuid instance_id
        bigint invoice_id FK
        varchar method
        numeric amount
        bigint recorded_by
    }
    payment_idempotency_keys {
        bigint id PK
        uuid instance_id
        varchar idempotency_key
        bigint user_id
        bigint invoice_id
        bigint payment_id
    }
    payroll_configs {
        bigint id PK
        uuid instance_id
        bigint teacher_id
        varchar type
        decimal hourly_rate
    }
    payroll_periods {
        bigint id PK
        uuid instance_id
        bigint teacher_id
        decimal gross_amount
        decimal net_amount
        varchar status
    }
```

> Ghi chú quan hệ: các FK gắn `FK` ở ERD là **FK thật trong DB**. Các quan hệ `payment_records → invoices` là FK thật (`fk_payment_records_invoice`). `payments → invoices`, `invoices → students/classes` là FK thật trong V1. `payment_idempotency_keys.invoice_id` / `.payment_id` là **liên kết logic** (KHÔNG có FK constraint — xem anomalies).

---

## `invoices`

**Mục đích.** Hóa đơn học phí + phí của học sinh. Mỗi hóa đơn thuộc 1 tenant (`instance_id`), tham chiếu 1 học sinh, theo dõi vòng đời thanh toán (DRAFT → PARTIAL → PAID). Tạo ở `V1`, bổ sung audit cột ở `V26`, bật RLS ở `V58/V59`.

| Cột | Kiểu | Null | Default | Khóa/Index | Ý nghĩa |
|---|---|---|---|---|---|
| `id` | BIGSERIAL | NO | seq | PK | Khóa chính |
| `instance_id` | UUID | NO | — | `idx_invoices_instance`; UNIQUE thành phần | Tenant ID (multi-tenant isolation) |
| `invoice_number` | VARCHAR(50) | NO | — | UNIQUE `(instance_id, invoice_number)` | Số hóa đơn, vd `INV-2025-0001` |
| `student_id` | BIGINT | NO | — | FK → `students(id)`; `idx_invoices_student` | Học sinh được lập hóa đơn (cross-cluster → cluster Học sinh) |
| `class_id` | BIGINT | YES | — | FK → `classes(id)`; `idx_invoices_class` | Lớp liên quan (nullable) |
| `period_start` | DATE | NO | — | `idx_invoices_period` | Đầu kỳ tính phí |
| `period_end` | DATE | NO | — | `idx_invoices_period` | Cuối kỳ tính phí |
| `subtotal` | DECIMAL(12,2) | NO | — | — | Tạm tính (VND) trước giảm trừ |
| `discount` | DECIMAL(12,2) | YES | `0` | — | Giảm trừ (VND); cột legacy — ưu tiên dùng `InvoiceAdjustment` |
| `total` | DECIMAL(12,2) | NO | — | — | Tổng tiền (VND) sau giảm trừ |
| `amount_paid` | DECIMAL(12,2) | YES | `0` | — | Đã thanh toán (VND) |
| `balance_due` | DECIMAL(12,2) | — | `GENERATED ALWAYS AS (total - amount_paid) STORED` | — | Còn nợ (VND) — cột tính, lưu sẵn |
| `issue_date` | DATE | NO | `CURRENT_DATE` | — | Ngày phát hành |
| `due_date` | DATE | NO | — | `idx_invoices_due_date` (partial WHERE status IN pending/partially_paid) | Hạn thanh toán |
| `status` | VARCHAR(50) | YES | `'pending'` | `idx_invoices_status`; CHECK | Trạng thái — CHECK DB: `draft, pending, partially_paid, paid, overdue, cancelled` (xem anomalies về drift với enum entity) |
| `notes` | TEXT | YES | — | — | Ghi chú |
| `created_at` | TIMESTAMPTZ | NO | `CURRENT_TIMESTAMP` | — | Audit — tạo |
| `updated_at` | TIMESTAMPTZ | NO | `CURRENT_TIMESTAMP` | — | Audit — cập nhật |
| `created_by` | BIGINT → **UUID** | YES | — | — | Actor tạo. V1 = BIGINT; **V73 convert → UUID** (X-User-Id JWT `sub`) |
| `updated_by` | BIGINT → **UUID** | YES | — | — | Actor cập nhật. Thêm bởi `V26`; **V73 convert → UUID** |
| `version` | BIGINT | YES | — *(không SET DEFAULT)* | — | Optimistic lock. Thêm bởi `V26`. ⚠️ Cột này **KHÔNG** được V62/V63 set DEFAULT 0 (xem anomalies) |

**Constraints**: `uk_invoices_instance_number UNIQUE(instance_id, invoice_number)`; `chk_invoices_amounts CHECK(subtotal>=0 AND discount>=0 AND total>=0 AND amount_paid>=0)`; `chk_invoices_status CHECK(status IN ...)`.

**Quan hệ FK**
- Out: `student_id → students(id)`, `class_id → classes(id)` (cross-cluster: Học sinh / Lớp).
- In: `invoice_items.invoice_id → invoices(id)` (CASCADE), `payments.invoice_id → invoices(id)`, `payment_records.invoice_id → invoices(id)`. `payment_idempotency_keys.invoice_id` là tham chiếu logic (no FK).

**RLS + ghi chú**
- Tenant-scoped ✅. RLS bật ở `V58` (`ENABLE` + `FORCE ROW LEVEL SECURITY` + policy `tenant_isolation`), hardened ở `V59` (admin-bypass `app.is_platform_admin` + NULL force-fail — bỏ escape hatch `NULLIF` default-allow).
- Soft-delete: entity `Invoice extends BaseEntity` ⇒ kỳ vọng có cột `deleted` + `enrollment_id` + index `idx_invoices_deleted`/`idx_invoices_enrollment`, nhưng **migration KHÔNG tạo các cột này** (xem anomalies — drift nặng).

---

## `invoice_items`

**Mục đích.** Dòng chi tiết của hóa đơn (học phí, tài liệu, phí khác). Phụ thuộc vòng đời hóa đơn cha (xóa hóa đơn → CASCADE xóa item). Tạo ở `V1`.

| Cột | Kiểu | Null | Default | Khóa/Index | Ý nghĩa |
|---|---|---|---|---|---|
| `id` | BIGSERIAL | NO | seq | PK | Khóa chính |
| `invoice_id` | BIGINT | NO | — | FK → `invoices(id)` **ON DELETE CASCADE**; `idx_invoice_items_invoice` | Hóa đơn cha |
| `description` | VARCHAR(255) | NO | — | — | Mô tả dòng |
| `quantity` | INTEGER | YES | `1` | — | Số lượng |
| `unit_price` | DECIMAL(12,2) | NO | — | — | Đơn giá (VND) |
| `amount` | DECIMAL(12,2) | NO | — | — | Thành tiền dòng (VND) |
| `item_type` | VARCHAR(50) | YES | — | — | Loại: `tuition, material, other` (comment DB). Entity enum `InvoiceItemType` = TUITION/MATERIALS/REGISTRATION_FEE/EXAM_FEE/OTHER — drift (xem anomalies) |
| `reference_id` | BIGINT | YES | — | — | Tham chiếu (class_id, session_id…) — không FK |
| `created_at` | TIMESTAMPTZ | NO | `CURRENT_TIMESTAMP` | — | Audit — tạo |
| `created_by` | BIGINT | YES | — | — | Thêm bởi `V26`; **V73 convert → UUID** (created_by/updated_by sweep) |
| `updated_by` | BIGINT | YES | — | — | Thêm bởi `V26`; **V73 convert → UUID** |
| `version` | BIGINT | YES | `0` | — | Thêm bởi `V26`; **V62 SET DEFAULT 0** + backfill NULL→0 |

**Constraints**: chỉ FK CASCADE tới `invoices`. Không có UNIQUE riêng.

**Quan hệ FK**
- Out: `invoice_id → invoices(id)` (cardinality N-1; nhiều item / 1 hóa đơn).
- In: không.

**RLS + ghi chú**
- **KHÔNG** tenant-scoped trực tiếp: bảng **không có cột `instance_id`** ⇒ V58/V59 **bỏ qua** (`Skipping table (no instance_id column)`). Cô lập tenant qua hóa đơn cha (item luôn truy cập qua `invoice_id`).
- Entity `InvoiceItem` KHÔNG extends `BaseEntity` (không `deleted`/`instance_id`/`version` qua superclass; `version` chỉ tồn tại do V26 thêm ở DB).

---

## `payments`

**Mục đích.** Bản ghi thanh toán cho hóa đơn — **hệ V1**, định hướng cổng thanh toán online (cash/bank_transfer/momo/zalopay/qr). Tạo ở `V1`, bổ sung audit ở `V26`, RLS ở `V58/V59`. ⚠️ Entity JPA `Payment` đã **drift rất xa** so với bảng này (xem anomalies).

| Cột | Kiểu | Null | Default | Khóa/Index | Ý nghĩa |
|---|---|---|---|---|---|
| `id` | BIGSERIAL | NO | seq | PK | Khóa chính |
| `instance_id` | UUID | NO | — | `idx_payments_instance`; UNIQUE thành phần | Tenant ID |
| `payment_number` | VARCHAR(50) | NO | — | UNIQUE `(instance_id, payment_number)` | Số phiếu thu, vd `PAY-2025-0001` |
| `invoice_id` | BIGINT | NO | — | FK → `invoices(id)`; `idx_payments_invoice` | Hóa đơn được thanh toán |
| `amount` | DECIMAL(12,2) | NO | — | CHECK `> 0` | Số tiền (VND) |
| `payment_method` | VARCHAR(50) | NO | — | — | Phương thức: `cash, bank_transfer, momo, zalopay, qr` (comment) |
| `transaction_id` | VARCHAR(100) | YES | — | — | Mã giao dịch cổng (nullable trong DB) |
| `qr_code_url` | TEXT | YES | — | — | URL mã QR |
| `payer_id` | BIGINT | YES | — | `idx_payments_payer` | User ID người trả (parent) — từ Gateway, **NO FK** |
| `payer_name` | VARCHAR(255) | YES | — | — | Tên người trả |
| `status` | VARCHAR(50) | YES | `'pending'` | `idx_payments_status`; CHECK | `pending, completed, failed, refunded` |
| `notes` | TEXT | YES | — | — | Ghi chú |
| `receipt_url` | TEXT | YES | — | — | URL biên lai |
| `paid_at` | TIMESTAMPTZ | YES | — | `idx_payments_date` | Thời điểm thanh toán |
| `created_at` | TIMESTAMPTZ | NO | `CURRENT_TIMESTAMP` | — | Audit — tạo |
| `updated_at` | TIMESTAMPTZ | NO | `CURRENT_TIMESTAMP` | — | Audit — cập nhật |
| `received_by` | BIGINT | YES | — | — | Actor nhận tiền — từ Gateway, **NO FK**. ⚠️ V73 **KHÔNG** convert cột này → vẫn BIGINT (xem anomalies) |
| `created_by` | BIGINT → **UUID** | YES | — | — | Thêm bởi `V26`; **V73 convert → UUID** |
| `updated_by` | BIGINT → **UUID** | YES | — | — | Thêm bởi `V26`; **V73 convert → UUID** |
| `version` | BIGINT | YES | — *(không SET DEFAULT)* | — | Thêm bởi `V26`. ⚠️ KHÔNG được V62/V63 set DEFAULT 0 (xem anomalies) |

**Constraints**: `uk_payments_instance_number UNIQUE(instance_id, payment_number)`; `chk_payments_amount CHECK(amount > 0)`; `chk_payments_status CHECK(status IN (...))`.

**Quan hệ FK**
- Out: `invoice_id → invoices(id)` (N-1). `payer_id`/`received_by` là user ID từ Gateway (không FK trong DB — cross-service).
- In: `payment_idempotency_keys.payment_id` là tham chiếu logic (no FK).

**RLS + ghi chú**
- Tenant-scoped ✅. RLS V58 + V59 (admin-bypass + NULL force-fail), giống `invoices`.

---

## `payment_records`

**Mục đích.** Bản ghi **thu thủ công** tại trung tâm (tiền mặt / chuyển khoản / VietQR / MoMo) do giáo viên/admin nhập tay đánh dấu hóa đơn đã trả. **Phân biệt rõ** với `payments` (cổng online). Tạo ở `V69` (GAP-292b, Wave beta-readiness-4 Bucket C). Đây là **hệ canonical** cho luồng thu phí Phase 1 BETA (xem anomalies). Map từ entity `PaymentRecord extends BaseEntity`.

| Cột | Kiểu | Null | Default | Khóa/Index | Ý nghĩa |
|---|---|---|---|---|---|
| `id` | BIGSERIAL | NO | seq | PK | Khóa chính |
| `instance_id` | UUID | NO | — | `idx_payment_records_instance_id`; `idx_payment_records_tenant_period (instance_id, paid_at) WHERE deleted=false` | Tenant ID (BaseEntity) |
| `created_at` | TIMESTAMPTZ | NO | `NOW()` | — | Audit — tạo (BaseEntity) |
| `updated_at` | TIMESTAMPTZ | YES | — | — | Audit — cập nhật (BaseEntity) |
| `created_by` | BIGINT → **UUID** | YES | — | — | Actor tạo (BaseEntity). **V73 convert → UUID** |
| `updated_by` | BIGINT → **UUID** | YES | — | — | Actor cập nhật (BaseEntity). **V73 convert → UUID** |
| `deleted` | BOOLEAN | NO | `FALSE` | dùng trong partial index | Soft-delete (BaseEntity) |
| `version` | BIGINT | YES | — *(không default trong V69)* | — | Optimistic lock (BaseEntity). ⚠️ V69 KHÔNG set DEFAULT 0 (V62/V63 chỉ chạy các bảng cũ) |
| `invoice_id` | BIGINT | NO | — | FK → `invoices(id)` (`fk_payment_records_invoice`); `idx_payment_records_invoice_id` | Hóa đơn liên quan (cùng tenant — BR-PAYMENT-METHOD-003) |
| `method` | VARCHAR(30) | NO | — | `idx_payment_records_method`; CHECK | Enum `PaymentRecordMethod`: `CASH, BANK_TRANSFER, VIETQR, MOMO` |
| `amount` | NUMERIC(19,2) | NO | — | CHECK `> 0` | Số tiền nhận (VND). NUMERIC(19,2) tới ~9.99×10^16 đ |
| `paid_at` | TIMESTAMPTZ | NO | — | `idx_payment_records_paid_at` | Thời điểm nhận tiền thực tế |
| `note` | VARCHAR(500) | YES | — | — | Ghi chú tự do (vd "Phụ huynh em Hồng thanh toán 2 tháng") |
| `recorded_by` | BIGINT | NO | — | — | User (GV/admin) ghi nhận — audit trail. ⚠️ Là **actor user-id nhưng kiểu BIGINT**, V73 KHÔNG convert (xem anomalies) |

**Constraints**: `chk_payment_records_method CHECK(method IN ('CASH','BANK_TRANSFER','VIETQR','MOMO'))`; `chk_payment_records_amount_positive CHECK(amount > 0)`; `fk_payment_records_invoice FK(invoice_id) → invoices(id)`.

**Quan hệ FK**
- Out: `invoice_id → invoices(id)` (N-1). `recorded_by` là user ID (không FK).
- In: không.

**RLS + ghi chú**
- Tenant-scoped qua `instance_id` (BaseEntity) nhưng cô lập **chỉ ở tầng code** (Hibernate `tenantFilter` + `@PreAuthorize`). ⚠️ Bảng tạo ở V69, **sau** V58/V59 ⇒ **RLS DB-level CHƯA được apply** cho bảng này (xem anomalies). Idempotency: dùng bảng shared `idempotency_keys` (V66) scope=PAYMENT (BR-PAYMENT-METHOD-004) — KHÔNG dùng `payment_idempotency_keys`.

---

## `payment_idempotency_keys`

**Mục đích.** Lưu trạng thái idempotency cho **luồng parent payment** (Wave 105 Bucket D, GAP-705): ánh xạ `Idempotency-Key` header → `payment_id` của lần ghi đầu, để click "trả tiền" lặp lại trả về CÙNG payment thay vì tạo mới. Tạo ở `V61`. (Bảng này KHÔNG có entity JPA tương ứng tên `payment_idempotency_keys` — dùng trực tiếp qua service.)

| Cột | Kiểu | Null | Default | Khóa/Index | Ý nghĩa |
|---|---|---|---|---|---|
| `id` | BIGSERIAL | NO | seq | PK | Khóa chính |
| `instance_id` | UUID | NO | — | UNIQUE thành phần | Tenant ID |
| `idempotency_key` | VARCHAR(64) | NO | — | UNIQUE `(instance_id, idempotency_key)` | Giá trị `Idempotency-Key` client gửi (UUID/ksuid/ulid) |
| `user_id` | BIGINT | NO | — | `idx_payment_idempotency_user` | Caller identity (scope key/user). Comment: sẽ bind real principal khi Bucket E land. **Kiểu BIGINT** |
| `invoice_id` | BIGINT | NO | — | `idx_payment_idempotency_invoice` | Hóa đơn của thanh toán (tham chiếu logic, **NO FK**) |
| `payment_id` | BIGINT | NO | — | — | Payment tạo ở request đầu tiên (tham chiếu logic, **NO FK**) |
| `qr_payload` | TEXT | YES | — | — | Payload VietQR trả ở request đầu (replay trả lại cùng QR) |
| `created_at` | TIMESTAMP | NO | `NOW()` | — | Thời điểm tạo key. ⚠️ `TIMESTAMP` (không TZ) |
| `expires_at` | TIMESTAMP | NO | `NOW() + INTERVAL '24 hours'` | `idx_payment_idempotency_expires` | Hết hạn 24h (khớp VietQR partner-bank); sweeper nền xóa row quá hạn |

**Constraints**: `uk_payment_idempotency_scope UNIQUE(instance_id, idempotency_key)` — cùng key từ 2 parent/tenant khác nhau là 2 payment phân biệt (đa tenant).

**Quan hệ FK**
- Out: `invoice_id` / `payment_id` / `user_id` đều là tham chiếu **logic** — KHÔNG có FK constraint (comment V61: FK chờ Bucket E wire real principal id từ JWT).
- In: không.

**RLS + ghi chú**
- Có cột `instance_id` (NOT NULL) nhưng tạo ở `V61` (**sau** V58/V59) ⇒ **RLS DB-level CHƯA apply**. Cô lập tenant chỉ qua UNIQUE scope + code (xem anomalies).
- **Pattern idempotency cũ**: chỉ `created_at`/`expires_at` (TTL window) — KHÔNG có audit set đầy đủ (updated_at/created_by/deleted/version) như BaseEntity. Bảng kế nhiệm rộng hơn là `idempotency_keys` (V66, cross-domain, không thuộc cluster này).

---

## `payroll_configs`

**Mục đích.** Cấu hình lương theo từng giáo viên / tenant (GAP-057 Phase 1, Wave 18a Bucket C). Phase 1 chỉ tính engine cho `type=HOURLY`; các cột SALARY/COMMISSION/HYBRID được persist sẵn nhưng **inert** tới Phase 2 (GAP-057b). Tạo ở `V48`, RLS ở `V58/V59`. Map từ entity `PayrollConfig extends BaseEntity`.

| Cột | Kiểu | Null | Default | Khóa/Index | Ý nghĩa |
|---|---|---|---|---|---|
| `id` | BIGSERIAL | NO | seq | PK | Khóa chính |
| `instance_id` | UUID | NO | — | `idx_payroll_configs_instance_id`; UNIQUE thành phần | Tenant ID (BaseEntity) |
| `teacher_id` | BIGINT | NO | — | `idx_payroll_configs_teacher_id`; UNIQUE thành phần | GV. (Là PK teachers thật, không phải actor — đúng kiểu BIGINT) |
| `type` | VARCHAR(20) | NO | — | `idx_payroll_configs_type`; CHECK | Enum `PayrollType`: `SALARY, HOURLY, COMMISSION, HYBRID`. Phase 1 chỉ HOURLY |
| `hourly_rate` | DECIMAL(15,2) | YES | — | CHECK `NULL OR > 0` | VND/giờ. Bắt buộc khi type=HOURLY (BR-PAYROLL-002) |
| `base_salary` | DECIMAL(15,2) | YES | — | — | Lương cơ bản (VND) — inert Phase 1 |
| `commission_percent` | DECIMAL(5,2) | YES | — | CHECK `NULL OR 0..100` | % hoa hồng — inert Phase 1 |
| `gvcn_allowance` | DECIMAL(15,2) | YES | — | — | Phụ cấp GVCN (VND) — inert Phase 1 |
| `bonuses` | TEXT | YES | — | — | JSON map thưởng (Phase 2 sẽ pair `@JdbcTypeCode(SqlTypes.JSON)`). Entity map field `bonusesJson` |
| `created_at` | TIMESTAMP | NO | `CURRENT_TIMESTAMP` | — | Audit — tạo (BaseEntity). ⚠️ `TIMESTAMP` không TZ |
| `updated_at` | TIMESTAMP | YES | — | — | Audit — cập nhật. ⚠️ `TIMESTAMP` không TZ |
| `created_by` | BIGINT → **UUID** | YES | — | — | Actor (BaseEntity). **V73 convert → UUID** |
| `updated_by` | BIGINT → **UUID** | YES | — | — | Actor (BaseEntity). **V73 convert → UUID** |
| `deleted` | BOOLEAN | NO | `FALSE` | dùng trong unique index | Soft-delete |
| `version` | BIGINT | NO | `0` | — | Optimistic lock — **V48 đã set DEFAULT 0 ngay từ đầu** (khác các bảng V1) |

**Constraints**: `chk_payroll_config_type`; `chk_payroll_config_hourly_rate_positive`; `chk_payroll_config_commission_range`. **UNIQUE index** `uk_payroll_configs_teacher_tenant ON (teacher_id, instance_id) WHERE deleted = FALSE` — 1 config / GV / tenant (BR-PAYROLL-001, loại soft-deleted).

**Quan hệ FK**
- Out: `teacher_id` tham chiếu logic tới teachers (comment "FK to teachers.id" nhưng V48 **KHÔNG** khai báo FK constraint).
- In: không.

**RLS + ghi chú**
- Tenant-scoped ✅. RLS V58 + V59. `version` đã có DEFAULT 0 (không cần V62/V63).

---

## `payroll_periods`

**Mục đích.** 1 dòng / GV / kỳ lương (thường theo tháng). Phase 1 tạo trạng thái `DRAFT` với deductions=0; APPROVED/PAID + TNCN/BHXH/BHYT để Phase 2 (GAP-057b). Tạo ở `V48`, RLS ở `V58/V59`. Map từ entity `PayrollPeriod extends BaseEntity`.

| Cột | Kiểu | Null | Default | Khóa/Index | Ý nghĩa |
|---|---|---|---|---|---|
| `id` | BIGSERIAL | NO | seq | PK | Khóa chính |
| `instance_id` | UUID | NO | — | `idx_payroll_periods_instance_id` | Tenant ID (BaseEntity) |
| `teacher_id` | BIGINT | NO | — | `idx_payroll_periods_teacher_id` | GV (PK teachers, đúng kiểu BIGINT) |
| `start_date` | DATE | NO | — | `idx_payroll_periods_dates (start_date, end_date)` | Đầu kỳ (inclusive) |
| `end_date` | DATE | NO | — | `idx_payroll_periods_dates`; CHECK `end_date >= start_date` | Cuối kỳ (inclusive) |
| `hours_worked` | DECIMAL(7,2) | YES | — | — | Giờ dạy trong kỳ (HOURLY) — Phase 1 derive từ ClassSession |
| `gross_amount` | DECIMAL(15,2) | NO | — | CHECK `>= 0` | Lương gộp (VND). HOURLY = hours×rate HALF_EVEN scale 2 |
| `deductions` | DECIMAL(15,2) | NO | `0` | CHECK `>= 0` | Khấu trừ (VND). Phase 1 luôn 0 (BR-PAYROLL-006) |
| `net_amount` | DECIMAL(15,2) | NO | — | CHECK `>= 0` | Lương thực nhận = gross − deductions |
| `status` | VARCHAR(20) | NO | `'DRAFT'` | `idx_payroll_periods_status`; CHECK | Enum `PayrollStatus`: `DRAFT, APPROVED, PAID` |
| `created_at` | TIMESTAMP | NO | `CURRENT_TIMESTAMP` | — | Audit — tạo. ⚠️ `TIMESTAMP` không TZ |
| `updated_at` | TIMESTAMP | YES | — | — | Audit — cập nhật. ⚠️ `TIMESTAMP` không TZ |
| `created_by` | BIGINT → **UUID** | YES | — | — | Actor (BaseEntity). **V73 convert → UUID** |
| `updated_by` | BIGINT → **UUID** | YES | — | — | Actor (BaseEntity). **V73 convert → UUID** |
| `deleted` | BOOLEAN | NO | `FALSE` | — | Soft-delete |
| `version` | BIGINT | NO | `0` | — | Optimistic lock — **V48 set DEFAULT 0 ngay từ đầu** |

**Constraints**: `chk_payroll_period_dates CHECK(end_date >= start_date)`; `chk_payroll_period_status CHECK(status IN ('DRAFT','APPROVED','PAID'))`; `chk_payroll_period_amounts_nonneg CHECK(gross_amount>=0 AND deductions>=0 AND net_amount>=0)`.

**Quan hệ FK**
- Out: `teacher_id` tham chiếu logic tới teachers (không FK constraint trong V48).
- In: không.

**RLS + ghi chú**
- Tenant-scoped ✅. RLS V58 + V59. `version` đã DEFAULT 0.

---

## Ghi chú schema (anomalies)

### A1 — Hai hệ thanh toán song song: `payments` vs `payment_records`

Đây là điểm dễ nhầm nhất của cluster.

| Khía cạnh | `payments` (V1) | `payment_records` (V69, GAP-292b) |
|---|---|---|
| Định hướng | Cổng thanh toán **online** (VNPay/MoMo/ZaloPay redirect) | Thu **thủ công** tại trung tâm (tiền mặt / chuyển khoản / VietQR / MoMo) |
| Người tạo | Hệ thống / callback cổng | Giáo viên / admin nhập tay |
| Enum method | `cash, bank_transfer, momo, zalopay, qr` (lowercase, comment) | `CASH, BANK_TRANSFER, VIETQR, MOMO` (UPPERCASE, CHECK + enum `PaymentRecordMethod`) |
| Kiểu amount | `DECIMAL(12,2)` | `NUMERIC(19,2)` |
| Idempotency | `payment_idempotency_keys` (V61) | shared `idempotency_keys` (V66) scope=PAYMENT |
| Actor column | `received_by` BIGINT, `payer_id` BIGINT | `recorded_by` BIGINT |
| RLS DB-level | ✅ (V58/V59) | ❌ chưa (tạo sau V58/V59) |
| BaseEntity | KHÔNG (entity `Payment` tự khai id + tự thêm `created_by`) | CÓ (extends BaseEntity) |

**Cái nào canonical?** Cho luồng Phase 1 BETA (trung tâm thu phí thủ công), `payment_records` là **canonical** — nó là bản ghi mới nhất, gắn idempotency shared (V66), gắn FK thật tới `invoices`, và là hệ mà luồng GAP-292b/GAP-705 thực sự dùng. `payments` là di sản V1 + entity `Payment` (định hướng cổng online) mà phần lớn cột **chưa từng được migration tạo** (xem A2). Hai hệ KHÔNG có FK liên kết nhau; `payment_idempotency_keys.payment_id` (V61) trỏ logic tới `payments`, còn `payment_records` dùng `idempotency_keys`.

### A2 — Entity `Payment` ↔ bảng `payments` drift NẶNG

Entity JPA `Payment` (module `payment`) khai báo các cột **KHÔNG tồn tại** trong bảng `payments` (V1/V26) và **KHÔNG có migration nào tạo**: `installment_id`, `gateway_transaction_id`, `payment_url`, `gateway_response`, `receipt_number`, `initiated_at`, `expires_at`, `completed_at`, `failed_at`, `refunded_at`, `failure_reason`, và `payment_status` (entity) vs `status` (DB). Entity cũng đặt `transaction_id NOT NULL UNIQUE` trong khi DB là nullable không-unique. Entity dùng enum `PaymentMethod` (CASH/BANK_TRANSFER/MOMO/VNPAY/ZALOPAY/CREDIT_CARD) + `PaymentStatus` (PENDING/PROCESSING/COMPLETED/FAILED/REFUNDED) — cả hai khác giá trị/CHECK lowercase của bảng V1. ⇒ Entity và bảng đã rẽ nhánh; nếu chạy trên Postgres với migration thuần (không `ddl-auto=update`), query qua entity `Payment` sẽ lỗi cột-không-tồn-tại. Cần 1 migration reconcile hoặc xác nhận `payments` (V1) là di sản còn `payment_records` là hệ thực dùng.

### A3 — Entity `Invoice` ↔ bảng `invoices` drift (cột thiếu)

Entity `Invoice extends BaseEntity` ⇒ kỳ vọng cột `deleted BOOLEAN NOT NULL` + `version`, và thêm khai báo `enrollment_id` (+ unique `uk_invoices_enrollment`, index `idx_invoices_enrollment`, `idx_invoices_deleted`). **Migration KHÔNG tạo** `invoices.deleted` lẫn `invoices.enrollment_id` (V1 không có; V26 chỉ thêm `updated_by`+`version`). Hibernate `tenantFilter` (`instance_id = :tenantId`) + soft-delete filter dựa `deleted` ⇒ query thật sẽ lỗi nếu DB không có cột `deleted`. Đây là drift cần backfill migration.

### A4 — Enum ↔ CHECK constraint drift (`invoices.status`, `invoice_items.item_type`)

- `invoices.status`: CHECK DB = `draft, pending, partially_paid, paid, overdue, cancelled` (6 giá trị **lowercase**). Entity enum `InvoiceStatus` = `DRAFT, SENT, PAID, PARTIAL, OVERDUE, CANCELLED, REFUNDED` (7 giá trị **UPPERCASE**, `@Enumerated(STRING)`). Mismatch kép: hoa/thường + tập giá trị (`SENT`/`REFUNDED`/`PARTIAL` không có trong CHECK; `pending`/`partially_paid` không có trong enum). Persist `SENT`/`REFUNDED` sẽ vi phạm CHECK.
- `invoice_items.item_type`: comment DB `tuition, material, other` vs enum `InvoiceItemType` = `TUITION, MATERIALS, REGISTRATION_FEE, EXAM_FEE, OTHER` (UPPERCASE). Không có CHECK constraint nên không reject nhưng giá trị lưu là UPPERCASE.

### A5 — Kiểu tiền không nhất quán (BIGINT vs NUMERIC vs DECIMAL)

- `invoices`, `invoice_items`, `payments`: `DECIMAL(12,2)`.
- `payment_records`: `NUMERIC(19,2)`.
- `payroll_*`: `DECIMAL(15,2)` (hourly_rate/base_salary/gvcn/gross/deductions/net), `DECIMAL(7,2)` (hours_worked), `DECIMAL(5,2)` (commission_percent).
- `payment_idempotency_keys`: comment V61 nói "amount stored BIGINT VND minor-unit" nhưng bảng **KHÔNG có cột amount** nào — chỉ là ghi chú định hướng, không hiện thực. ⇒ Cả cluster không có cột tiền BIGINT thực; nhưng tài liệu/quy ước minor-unit BIGINT mâu thuẫn với thực tế DECIMAL/NUMERIC scale 2.

### A6 — Actor column kiểu BIGINT bị V73 sweep BỎ SÓT

V73 (GAP-795) chỉ convert `created_by`/`updated_by` (+ `classes.teacher_id`, `classes.rescheduled_by_user_id`, `parent_invitations.invited_by_user_id`) sang UUID. Các **cột actor user-id còn lại trong cluster vẫn BIGINT** (không được sweep):

- `payments.received_by` BIGINT, `payments.payer_id` BIGINT.
- `payment_records.recorded_by` BIGINT (NOT NULL).
- `payment_idempotency_keys.user_id` BIGINT.

> Ghi chú: task gốc nhắc `approved_by`/`paid_by` — **các cột này KHÔNG tồn tại** trong cluster. Các actor BIGINT thực sự bị bỏ sót là `received_by` / `payer_id` / `recorded_by` / `user_id`. Vì X-User-Id JWT là UUID (per V73 RCA), các cột BIGINT này sẽ KHÔNG nhận được user-id thật (parse fail) — drift cùng lớp với bug V73 đã fix nhưng chưa quét hết.

### A7 — `version` thiếu DEFAULT 0 trên `invoices`, `payments`, `payment_records`

V26 thêm `version BIGINT` (không default) cho `invoices` + `payments`. V62 chỉ set DEFAULT 0 cho `invoice_items` (+10 bảng khác); V63 cho 8 bảng khác. **`invoices.version` và `payments.version` KHÔNG nằm trong V62/V63** ⇒ vẫn không DEFAULT. `payment_records.version` (V69) cũng không default. Raw INSERT (seed/test fixture) vào 3 bảng này có thể NPE tại flush (cùng lớp lỗi mà V62 mô tả) — nhưng `invoices`/`payments` không extends BaseEntity-with-@Version theo cách giống nhau (Invoice/Payment entity tự quản version), nên rủi ro thực tế thấp hơn `invoice_items`. Vẫn là bất nhất so với 19 bảng đã được V62/V63 chuẩn hóa.

### A8 — TIMESTAMP vs TIMESTAMPTZ không nhất quán

- TIMESTAMPTZ: `invoices`, `payments` (created_at/updated_at/paid_at), `payment_records` (created_at/updated_at/paid_at).
- TIMESTAMP (không TZ): `payroll_configs`, `payroll_periods` (created_at/updated_at — V48), `payment_idempotency_keys` (created_at/expires_at — V61).

⇒ Trộn timezone-aware và timezone-naive trong cùng cluster. Bảng cũ V1 + bảng mới V69 dùng TZ; bảng V48 + V61 dùng naive TIMESTAMP — rủi ro lệch giờ khi so sánh kỳ lương / hết hạn idempotency qua múi giờ.

### A9 — RLS coverage gap (bảng tạo sau V58/V59)

V58 (enable RLS) + V59 (hardening) dùng danh sách bảng tĩnh chạy 1 lần. Bảng tạo **sau** không được enable RLS DB-level:

- `payment_records` (V69): có `instance_id` nhưng **CHƯA** có policy `tenant_isolation` → chỉ dựa code-level (`tenantFilter` + `@PreAuthorize`).
- `payment_idempotency_keys` (V61): có `instance_id` nhưng **CHƯA** RLS → chỉ dựa UNIQUE scope + code.

⇒ 2 bảng tài chính mới thiếu lớp phòng thủ DB-level mà `invoices`/`payments`/`payroll_*` đã có. Cần migration RLS bổ sung (hoặc DO-block re-run với danh sách mở rộng).

### A10 — Bảng idempotency phụ cận (ngoài 7 bảng cluster)

`idempotency_keys` (V66, Wave beta-readiness-2 GAP-730) là bảng **shared cross-domain** (PK `(tenant_id, idempotency_key, scope)`; scope SIGNUP/ENROLLMENT/BETA_REQUEST/PAYMENT; `user_id UUID`; `request_hash`; `response_status`/`response_body`). Nó KHÔNG thuộc 7 bảng cluster nhưng phục vụ luồng PAYMENT của `payment_records`. Lưu ý 2 bảng idempotency cùng tồn tại: `payment_idempotency_keys` (V61, hẹp, parent payment) + `idempotency_keys` (V66, rộng, generic) — `payment_records` dùng cái thứ hai.

---

## Liên kết

- [README cluster database KiteClass](../README.md)
- [Bản đồ kiến trúc database tổng thể](../../database-architecture-map.md)
