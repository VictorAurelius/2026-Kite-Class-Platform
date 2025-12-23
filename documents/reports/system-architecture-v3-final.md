# BÁO CÁO KIẾN TRÚC HỆ THỐNG V3 - KITECLASS PLATFORM
## Phiên bản chính thức - Final Version

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Tên dự án** | KiteClass Platform V3 |
| **Phiên bản** | 3.0 (Final) |
| **Ngày tạo** | 23/12/2025 |
| **Loại tài liệu** | Báo cáo kiến trúc hệ thống chính thức |
| **Thay đổi chính** | Bổ sung Phụ huynh, Hóa đơn, Gamification từ BeeClass |

---

## TỔNG QUAN THAY ĐỔI TỪ V2

| Tính năng | V2 | V3 | Lý do |
|-----------|----|----|-------|
| **AI Quiz Generator** | ✅ Có | ❌ Loại bỏ | Phức tạp, chưa ổn định, khó triển khai |
| **AI Marketing Agent** | ✅ Có | ✅ Giữ nguyên | Đã ổn định, mang lại giá trị cao |
| **Actor Phụ huynh** | ❌ Không | ✅ Bổ sung | Học từ BeeClass - nhu cầu thực tế |
| **Hệ thống Hóa đơn** | ❌ Không | ✅ Bổ sung | Học từ BeeClass - quản lý tài chính |
| **Gamification** | ❌ Không | ✅ Bổ sung | Học từ BeeClass - tăng engagement |
| **Kiến trúc KiteClass** | 7 services | 4 services | Tối ưu từ service-optimization-report |

---

# PHẦN 1: KIẾN TRÚC TỔNG QUAN V3

## 1.1. Sơ đồ kiến trúc toàn hệ thống

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          KITECLASS PLATFORM V3 (FINAL)                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                       KITEHUB (MODULAR MONOLITH)                           │ │
│  │                            NestJS Application                               │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │ │
│  │  │    Sale      │  │   Message    │  │  Maintaining │  │  AI Agent    │   │ │
│  │  │   Module     │  │    Module    │  │    Module    │  │   Module     │   │ │
│  │  │              │  │              │  │              │  │  (Marketing) │   │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │ │
│  │                                                                            │ │
│  │                    ┌─────────────────────────────────┐                    │ │
│  │                    │         Shared Layer            │                    │ │
│  │                    │  • Auth  • Database  • Queue    │                    │ │
│  │                    └─────────────────────────────────┘                    │ │
│  │                                                                            │ │
│  │         ┌─────────────┐    ┌─────────────┐    ┌─────────────┐             │ │
│  │         │ PostgreSQL  │    │    Redis    │    │  RabbitMQ   │             │ │
│  │         └─────────────┘    └─────────────┘    └─────────────┘             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                       │                                         │
│                              RESTful API / Events                               │
│                                       │                                         │
│          ┌────────────────────────────┼────────────────────────────┐            │
│          │                            │                            │            │
│          ▼                            ▼                            ▼            │
│  ┌────────────────────┐    ┌────────────────────┐    ┌────────────────────┐    │
│  │   KITECLASS #1     │    │   KITECLASS #2     │    │   KITECLASS #N     │    │
│  │  (4 Services V3)   │    │  (4 Services V3)   │    │  (4 Services V3)   │    │
│  │                    │    │                    │    │                    │    │
│  │ ┌────────────────┐ │    │ ┌────────────────┐ │    │ ┌────────────────┐ │    │
│  │ │ API Gateway    │ │    │ │ API Gateway    │ │    │ │ API Gateway    │ │    │
│  │ ├────────────────┤ │    │ ├────────────────┤ │    │ ├────────────────┤ │    │
│  │ │ Core Service   │ │    │ │ Core Service   │ │    │ │ Core Service   │ │    │
│  │ │ User Service   │ │    │ │ User Service   │ │    │ │ User Service   │ │    │
│  │ │ Media Service  │ │    │ │ Media Service  │ │    │ │ Media Service  │ │    │
│  │ │ Frontend       │ │    │ │ Frontend       │ │    │ │ Frontend       │ │    │
│  │ └────────────────┘ │    │ └────────────────┘ │    │ └────────────────┘ │    │
│  └────────────────────┘    └────────────────────┘    └────────────────────┘    │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                      EXTERNAL SERVICES                                      │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │ │
│  │  │   OpenAI     │  │  Stability   │  │  Remove.bg   │  │   Zalo API   │   │ │
│  │  │   GPT-4      │  │    SDXL      │  │              │  │   (OTP/Msg)  │   │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 1.2. Actors trong hệ thống V3

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              ACTORS HỆ THỐNG V3                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                        KITEHUB ACTORS                                     │   │
│  │  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐              │   │
│  │  │   Customer   │     │    Admin     │     │    Agent     │              │   │
│  │  │  (Đăng ký    │     │  (Quản trị   │     │  (Hỗ trợ     │              │   │
│  │  │   mua gói)   │     │   KiteHub)   │     │   khách)     │              │   │
│  │  └──────────────┘     └──────────────┘     └──────────────┘              │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                      KITECLASS INSTANCE ACTORS                            │   │
│  │                                                                           │   │
│  │  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐              │   │
│  │  │CENTER_OWNER  │     │CENTER_ADMIN  │     │   TEACHER    │              │   │
│  │  │ (Chủ trung   │     │ (Quản trị    │     │  (Giáo viên) │              │   │
│  │  │    tâm)      │     │   viên)      │     │              │              │   │
│  │  └──────────────┘     └──────────────┘     └──────────────┘              │   │
│  │                                                                           │   │
│  │  ┌──────────────┐     ┌──────────────┐                                   │   │
│  │  │   STUDENT    │     │   PARENT     │  ⭐ NEW                           │   │
│  │  │  (Học viên)  │     │ (Phụ huynh)  │                                   │   │
│  │  └──────────────┘     └──────────────┘                                   │   │
│  │                                                                           │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Chi tiết Actor PARENT (Mới)

| Thuộc tính | Mô tả |
|------------|-------|
| **Mô tả** | Phụ huynh của học viên, theo dõi con em tại trung tâm |
| **Đăng ký** | Tự đăng ký qua link/QR code, xác thực OTP Zalo |
| **Quan hệ** | Liên kết với 1 hoặc nhiều STUDENT (con em) |
| **Quyền hạn** | Xem điểm danh, điểm số, hóa đơn học phí của con |
| **Kênh liên lạc** | Nhận thông báo qua Zalo, app, email |

---

# PHẦN 2: KIẾN TRÚC KITECLASS INSTANCE V3

## 2.1. Cấu trúc 4 Services (Tối ưu)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         KITECLASS INSTANCE V3                                    │
│                           (4 Services Architecture)                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                         API GATEWAY                                         │ │
│  │                    (Spring Cloud Gateway)                                   │ │
│  │  • JWT Validation  • Rate Limiting  • CORS  • Load Balancing              │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                       │                                         │
│         ┌─────────────────────────────┼─────────────────────────────┐           │
│         │                             │                             │           │
│         ▼                             ▼                             ▼           │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐  │
│  │    CORE SERVICE      │  │    USER SERVICE      │  │    MEDIA SERVICE     │  │
│  │  (Java Spring Boot)  │  │  (Java Spring Boot)  │  │     (Node.js)        │  │
│  │                      │  │                      │  │                      │  │
│  │  ┌────────────────┐  │  │  • Authentication    │  │  • Video upload      │  │
│  │  │ Class Module   │  │  │  • Authorization     │  │  • Transcoding       │  │
│  │  │ • Khóa học     │  │  │  • User management   │  │  • Live streaming    │  │
│  │  │ • Lớp học      │  │  │  • Role/Permission   │  │  • WebSocket         │  │
│  │  │ • Lịch học     │  │  │  • Multi-tenant      │  │  • Recording         │  │
│  │  └────────────────┘  │  │                      │  │                      │  │
│  │  ┌────────────────┐  │  └──────────────────────┘  └──────────────────────┘  │
│  │  │Learning Module │  │                                                      │
│  │  │ • Điểm danh    │  │                                                      │
│  │  │ • Bài tập      │  │                                                      │
│  │  │ • Điểm số      │  │                                                      │
│  │  └────────────────┘  │                                                      │
│  │  ┌────────────────┐  │                                                      │
│  │  │Billing Module  │  │  ⭐ NEW                                              │
│  │  │ • Học phí      │  │                                                      │
│  │  │ • Hóa đơn      │  │                                                      │
│  │  │ • Công nợ      │  │                                                      │
│  │  │ • QR Payment   │  │                                                      │
│  │  └────────────────┘  │                                                      │
│  │  ┌────────────────┐  │                                                      │
│  │  │Gamification    │  │  ⭐ NEW                                              │
│  │  │ Module         │  │                                                      │
│  │  │ • Điểm tích lũy│  │                                                      │
│  │  │ • Đổi quà      │  │                                                      │
│  │  │ • Huy hiệu     │  │                                                      │
│  │  │ • Bảng xếp hạng│  │                                                      │
│  │  └────────────────┘  │                                                      │
│  │  ┌────────────────┐  │                                                      │
│  │  │ Forum Module   │  │                                                      │
│  │  │ • Diễn đàn     │  │                                                      │
│  │  │ • Hỏi đáp      │  │                                                      │
│  │  └────────────────┘  │                                                      │
│  │  ┌────────────────┐  │                                                      │
│  │  │Parent Module   │  │  ⭐ NEW                                              │
│  │  │ • Parent Portal│  │                                                      │
│  │  │ • Notifications│  │                                                      │
│  │  │ • Report View  │  │                                                      │
│  │  └────────────────┘  │                                                      │
│  └──────────────────────┘                                                      │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                         FRONTEND (Next.js)                                  │ │
│  │  • Server-Side Rendering  • Multi-role UI  • Responsive Design            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                          DATABASES                                          │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                      │ │
│  │  │  PostgreSQL  │  │    Redis     │  │   RabbitMQ   │                      │ │
│  │  │  (Primary)   │  │   (Cache)    │  │   (Queue)    │                      │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                      │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.2. Module Structure của Core Service V3

```
core-service/
├── src/main/java/com/kiteclass/core/
│   │
│   ├── class/                          # Class Module
│   │   ├── controller/
│   │   │   ├── CourseController.java
│   │   │   ├── ClassController.java
│   │   │   └── ScheduleController.java
│   │   ├── service/
│   │   ├── entity/
│   │   └── repository/
│   │
│   ├── learning/                       # Learning Module
│   │   ├── controller/
│   │   │   ├── AttendanceController.java
│   │   │   ├── AssignmentController.java
│   │   │   └── GradeController.java
│   │   ├── service/
│   │   ├── entity/
│   │   └── repository/
│   │
│   ├── billing/                        # ⭐ Billing Module (NEW)
│   │   ├── controller/
│   │   │   ├── TuitionController.java
│   │   │   ├── InvoiceController.java
│   │   │   ├── PaymentController.java
│   │   │   └── DebtController.java
│   │   ├── service/
│   │   │   ├── TuitionCalculatorService.java
│   │   │   ├── InvoiceGeneratorService.java
│   │   │   ├── QRCodePaymentService.java
│   │   │   └── DebtTrackingService.java
│   │   ├── entity/
│   │   │   ├── Tuition.java
│   │   │   ├── Invoice.java
│   │   │   ├── Payment.java
│   │   │   └── Debt.java
│   │   └── repository/
│   │
│   ├── gamification/                   # ⭐ Gamification Module (NEW)
│   │   ├── controller/
│   │   │   ├── PointController.java
│   │   │   ├── RewardController.java
│   │   │   ├── BadgeController.java
│   │   │   └── LeaderboardController.java
│   │   ├── service/
│   │   │   ├── PointCalculatorService.java
│   │   │   ├── RewardExchangeService.java
│   │   │   ├── BadgeAwardService.java
│   │   │   └── LeaderboardService.java
│   │   ├── entity/
│   │   │   ├── Point.java
│   │   │   ├── Reward.java
│   │   │   ├── Badge.java
│   │   │   ├── StudentBadge.java
│   │   │   └── RewardExchange.java
│   │   └── repository/
│   │
│   ├── parent/                         # ⭐ Parent Module (NEW)
│   │   ├── controller/
│   │   │   ├── ParentPortalController.java
│   │   │   ├── ParentNotificationController.java
│   │   │   └── ParentReportController.java
│   │   ├── service/
│   │   │   ├── ParentRegistrationService.java
│   │   │   ├── ParentNotificationService.java
│   │   │   ├── ChildLinkingService.java
│   │   │   └── ParentReportService.java
│   │   ├── entity/
│   │   │   ├── ParentStudent.java       # Liên kết phụ huynh - học viên
│   │   │   └── ParentNotification.java
│   │   └── repository/
│   │
│   ├── forum/                          # Forum Module
│   │   ├── controller/
│   │   ├── service/
│   │   ├── entity/
│   │   └── repository/
│   │
│   └── shared/                         # Shared components
│       ├── config/
│       ├── security/
│       ├── exception/
│       └── util/
```

---

# PHẦN 3: HỆ THỐNG HÓA ĐƠN HỌC PHÍ (BILLING)

## 3.1. Tổng quan Billing Module

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           BILLING MODULE OVERVIEW                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Mục đích:                                                                      │
│  Quản lý toàn bộ chu trình tài chính: Tính học phí → Tạo hóa đơn → Thu tiền    │
│  → Theo dõi công nợ → Báo cáo tài chính                                        │
│                                                                                  │
│  Tính năng chính (Học từ BeeClass):                                            │
│  ✅ Tính học phí theo buổi hoặc cố định theo tháng                             │
│  ✅ Tự động tạo phiếu thu hàng tháng                                           │
│  ✅ QR Code chuyển khoản ngân hàng                                             │
│  ✅ Theo dõi công nợ theo học viên                                              │
│  ✅ Thông báo nhắc thanh toán cho phụ huynh                                     │
│  ✅ Báo cáo doanh thu theo thời gian                                            │
│                                                                                  │
│  Luồng hoạt động:                                                               │
│                                                                                  │
│  [1] Cấu hình học phí     [2] Tính học phí      [3] Tạo hóa đơn               │
│  ────────────────────     ───────────────       ──────────────                 │
│  • Học phí lớp            • Cuối tháng tự động  • PDF phiếu thu               │
│  • Loại tính (buổi/tháng) • Hoặc thủ công       • QR Code thanh toán          │
│  • Giảm giá/ưu đãi        • Trừ điểm danh vắng  • Gửi cho phụ huynh           │
│                                                                                  │
│  [4] Thu tiền             [5] Công nợ           [6] Báo cáo                    │
│  ───────────              ──────────            ──────────                     │
│  • Tiền mặt               • Theo dõi chưa đóng  • Doanh thu                    │
│  • Chuyển khoản           • Nhắc thanh toán     • Công nợ                      │
│  • Đối soát QR            • Lịch sử thanh toán  • Học viên nợ                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 3.2. Database Schema - Billing

```sql
-- Schema: billing
CREATE SCHEMA billing;

-- Cấu hình học phí theo lớp
CREATE TABLE billing.tuition_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    class_id UUID NOT NULL REFERENCES class_module.classes(id),
    calculation_type VARCHAR(20) NOT NULL, -- 'FIXED' hoặc 'PER_SESSION'
    fixed_amount DECIMAL(12,0),            -- Học phí cố định/tháng
    per_session_amount DECIMAL(12,0),      -- Học phí theo buổi
    discount_percent DECIMAL(5,2) DEFAULT 0,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP DEFAULT NOW(),
    created_by UUID NOT NULL
);

-- Hóa đơn học phí
CREATE TABLE billing.invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number VARCHAR(50) UNIQUE NOT NULL, -- VD: INV-2025-001234
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    class_id UUID NOT NULL REFERENCES class_module.classes(id),
    billing_month DATE NOT NULL,               -- Tháng tính phí

    -- Chi tiết tính phí
    total_sessions INT,                        -- Tổng buổi trong tháng
    attended_sessions INT,                     -- Số buổi đi học
    absent_sessions INT,                       -- Số buổi vắng

    -- Số tiền
    base_amount DECIMAL(12,0) NOT NULL,        -- Học phí gốc
    discount_amount DECIMAL(12,0) DEFAULT 0,   -- Giảm giá
    final_amount DECIMAL(12,0) NOT NULL,       -- Số tiền cần đóng
    paid_amount DECIMAL(12,0) DEFAULT 0,       -- Đã đóng
    debt_amount DECIMAL(12,0) DEFAULT 0,       -- Còn nợ

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PARTIAL, PAID, OVERDUE
    due_date DATE NOT NULL,

    -- QR Payment
    qr_code_data TEXT,                         -- Nội dung QR VietQR
    bank_account VARCHAR(50),
    bank_name VARCHAR(100),

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Thanh toán
CREATE TABLE billing.payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID NOT NULL REFERENCES billing.invoices(id),
    amount DECIMAL(12,0) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,      -- CASH, BANK_TRANSFER, MOMO, ZALOPAY
    payment_date TIMESTAMP NOT NULL,
    transaction_ref VARCHAR(100),              -- Mã giao dịch ngân hàng
    note TEXT,
    received_by UUID REFERENCES user_module.users(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Công nợ
CREATE TABLE billing.debts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    total_debt DECIMAL(12,0) NOT NULL DEFAULT 0,
    last_payment_date TIMESTAMP,
    overdue_months INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'NORMAL',      -- NORMAL, WARNING, CRITICAL
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_invoices_student ON billing.invoices(student_id);
CREATE INDEX idx_invoices_status ON billing.invoices(status);
CREATE INDEX idx_invoices_month ON billing.invoices(billing_month);
CREATE INDEX idx_payments_invoice ON billing.payments(invoice_id);
CREATE INDEX idx_debts_student ON billing.debts(student_id);
```

## 3.3. QR Code Payment Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          QR CODE PAYMENT FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  [1] TẠO HÓA ĐƠN                                                               │
│  ────────────────                                                               │
│                                                                                  │
│  Cuối tháng, hệ thống tự động:                                                 │
│  • Tính học phí dựa trên điểm danh                                             │
│  • Tạo hóa đơn cho từng học viên                                               │
│  • Generate QR Code VietQR                                                      │
│                                                                                  │
│       │                                                                         │
│       ▼                                                                         │
│                                                                                  │
│  [2] GENERATE QR CODE (VietQR Standard)                                        │
│  ──────────────────────────────────────                                        │
│                                                                                  │
│  QR Data Format:                                                                │
│  {                                                                              │
│    "bankId": "970415",                    // Mã ngân hàng VietinBank           │
│    "accountNo": "1234567890",             // Số tài khoản trung tâm            │
│    "accountName": "TRUNG TAM TIENG ANH ABC",                                   │
│    "amount": 1500000,                     // Số tiền                           │
│    "description": "HP T12-2025 NGUYEN VAN A"  // Nội dung chuyển khoản        │
│  }                                                                              │
│                                                                                  │
│  QR Code Image:                                                                 │
│  ┌─────────────────────────────────────┐                                       │
│  │  ┌─────────────────────────────┐    │                                       │
│  │  │      ┌───┐    ┌───┐         │    │                                       │
│  │  │      │░░░│    │░░░│         │    │                                       │
│  │  │      └───┘    └───┘         │    │                                       │
│  │  │    ░░░░░░░░░░░░░░░░░        │    │                                       │
│  │  │    ░░░░ VIETQR ░░░░        │    │                                       │
│  │  │    ░░░░░░░░░░░░░░░░░        │    │                                       │
│  │  │      ┌───┐                  │    │                                       │
│  │  │      │░░░│                  │    │                                       │
│  │  │      └───┘                  │    │                                       │
│  │  └─────────────────────────────┘    │                                       │
│  │                                      │                                       │
│  │  TRUNG TAM TIENG ANH ABC            │                                       │
│  │  STK: 1234567890 - VietinBank       │                                       │
│  │  Số tiền: 1,500,000 VND             │                                       │
│  │  ND: HP T12-2025 NGUYEN VAN A       │                                       │
│  └─────────────────────────────────────┘                                       │
│                                                                                  │
│       │                                                                         │
│       ▼                                                                         │
│                                                                                  │
│  [3] GỬI CHO PHỤ HUYNH                                                        │
│  ────────────────────                                                          │
│                                                                                  │
│  • Push notification lên app                                                   │
│  • Gửi qua Zalo OA                                                             │
│  • Email (nếu có)                                                              │
│                                                                                  │
│       │                                                                         │
│       ▼                                                                         │
│                                                                                  │
│  [4] PHỤ HUYNH THANH TOÁN                                                     │
│  ─────────────────────────                                                     │
│                                                                                  │
│  Phụ huynh mở app ngân hàng:                                                   │
│  • Quét QR Code                                                                 │
│  • Tự động điền thông tin chuyển khoản                                         │
│  • Xác nhận và thanh toán                                                      │
│                                                                                  │
│       │                                                                         │
│       ▼                                                                         │
│                                                                                  │
│  [5] ĐỐI SOÁT TỰ ĐỘNG                                                         │
│  ────────────────────                                                          │
│                                                                                  │
│  Hệ thống kiểm tra:                                                            │
│  • Match nội dung chuyển khoản với mã hóa đơn                                  │
│  • Cập nhật trạng thái PAID                                                    │
│  • Gửi xác nhận cho phụ huynh                                                  │
│  • Cập nhật công nợ học viên                                                   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 4: HỆ THỐNG GAMIFICATION

## 4.1. Tổng quan Gamification Module

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         GAMIFICATION MODULE OVERVIEW                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Mục đích:                                                                      │
│  Tăng engagement của học viên thông qua cơ chế game: Điểm thưởng, huy hiệu,    │
│  bảng xếp hạng, đổi quà.                                                        │
│                                                                                  │
│  Tính năng chính (Học từ BeeClass):                                            │
│  ✅ Tích điểm khi tham gia hoạt động                                           │
│  ✅ Đổi điểm lấy quà/voucher                                                   │
│  ✅ Huy hiệu thành tích                                                        │
│  ✅ Bảng xếp hạng theo lớp/toàn trường                                         │
│                                                                                  │
│  Cơ chế tích điểm:                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐    │
│  │  Hành động                          │  Điểm thưởng                     │    │
│  ├─────────────────────────────────────┼──────────────────────────────────┤    │
│  │  Điểm danh đúng giờ                 │  +10 điểm                        │    │
│  │  Hoàn thành bài tập đúng hạn        │  +20 điểm                        │    │
│  │  Điểm kiểm tra >= 8                 │  +30 điểm                        │    │
│  │  Điểm kiểm tra = 10                 │  +50 điểm (bonus)                │    │
│  │  Streak điểm danh 7 ngày            │  +100 điểm (bonus)               │    │
│  │  Tham gia diễn đàn (hỏi/trả lời)    │  +5 điểm                         │    │
│  │  Xem hết video bài giảng            │  +15 điểm                        │    │
│  └─────────────────────────────────────┴──────────────────────────────────┘    │
│                                                                                  │
│  Danh mục quà đổi:                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐    │
│  │  Quà                                │  Điểm cần                        │    │
│  ├─────────────────────────────────────┼──────────────────────────────────┤    │
│  │  Sticker pack                       │  100 điểm                        │    │
│  │  Voucher giảm 50k học phí           │  500 điểm                        │    │
│  │  Sách/Tài liệu học tập              │  800 điểm                        │    │
│  │  Voucher giảm 200k học phí          │  1500 điểm                       │    │
│  │  Khóa học miễn phí 1 tháng          │  3000 điểm                       │    │
│  └─────────────────────────────────────┴──────────────────────────────────┘    │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 4.2. Database Schema - Gamification

```sql
-- Schema: gamification
CREATE SCHEMA gamification;

-- Cấu hình điểm
CREATE TABLE gamification.point_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    action_type VARCHAR(50) NOT NULL UNIQUE, -- ATTENDANCE, ASSIGNMENT, TEST_SCORE, etc
    action_name VARCHAR(100) NOT NULL,
    base_points INT NOT NULL,
    bonus_condition JSONB,                   -- Điều kiện thưởng thêm
    bonus_points INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Điểm tích lũy của học viên
CREATE TABLE gamification.student_points (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    total_points INT NOT NULL DEFAULT 0,
    available_points INT NOT NULL DEFAULT 0,  -- Điểm chưa đổi
    used_points INT NOT NULL DEFAULT 0,       -- Điểm đã đổi
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(student_id)
);

-- Lịch sử tích điểm
CREATE TABLE gamification.point_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    action_type VARCHAR(50) NOT NULL,
    points INT NOT NULL,                      -- Có thể âm (khi đổi quà)
    reference_id UUID,                        -- ID của attendance/assignment/etc
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Huy hiệu
CREATE TABLE gamification.badges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url TEXT,
    requirement_type VARCHAR(50) NOT NULL,    -- TOTAL_POINTS, STREAK, PERFECT_SCORE, etc
    requirement_value INT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Huy hiệu của học viên
CREATE TABLE gamification.student_badges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    badge_id UUID NOT NULL REFERENCES gamification.badges(id),
    awarded_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(student_id, badge_id)
);

-- Danh mục quà
CREATE TABLE gamification.rewards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    image_url TEXT,
    points_required INT NOT NULL,
    reward_type VARCHAR(50) NOT NULL,         -- VOUCHER, PHYSICAL, DIGITAL
    reward_value JSONB,                       -- Chi tiết quà (giá trị voucher, etc)
    quantity_available INT,                   -- NULL = unlimited
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Lịch sử đổi quà
CREATE TABLE gamification.reward_exchanges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    reward_id UUID NOT NULL REFERENCES gamification.rewards(id),
    points_used INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, DELIVERED, CANCELLED
    exchange_code VARCHAR(50) UNIQUE,          -- Mã đổi quà
    approved_by UUID REFERENCES user_module.users(id),
    approved_at TIMESTAMP,
    delivered_at TIMESTAMP,
    note TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Bảng xếp hạng (cached, updated daily)
CREATE TABLE gamification.leaderboard (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    class_id UUID REFERENCES class_module.classes(id), -- NULL = toàn trường
    period VARCHAR(20) NOT NULL,              -- WEEKLY, MONTHLY, ALL_TIME
    rank INT NOT NULL,
    total_points INT NOT NULL,
    calculated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(student_id, class_id, period)
);

-- Indexes
CREATE INDEX idx_point_history_student ON gamification.point_history(student_id);
CREATE INDEX idx_point_history_date ON gamification.point_history(created_at);
CREATE INDEX idx_student_badges_student ON gamification.student_badges(student_id);
CREATE INDEX idx_leaderboard_class ON gamification.leaderboard(class_id, period, rank);
```

## 4.3. Gamification Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           GAMIFICATION FLOW                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  [A] TÍCH ĐIỂM TỰ ĐỘNG                                                         │
│  ─────────────────────                                                          │
│                                                                                  │
│  ┌─────────────┐      ┌─────────────────┐      ┌─────────────────┐             │
│  │   Giáo viên │      │   Core Service  │      │  Gamification   │             │
│  │  điểm danh  │─────▶│   Event Bus     │─────▶│    Service      │             │
│  └─────────────┘      └─────────────────┘      └────────┬────────┘             │
│                                                         │                       │
│                              ┌───────────────────────────┘                      │
│                              │                                                   │
│                              ▼                                                   │
│                       ┌──────────────────────────────────────────┐              │
│                       │  1. Check point rule for ATTENDANCE      │              │
│                       │  2. Calculate base points (+10)          │              │
│                       │  3. Check bonus conditions               │              │
│                       │     - Đúng giờ? → +5 bonus               │              │
│                       │     - Streak 7 ngày? → +100 bonus        │              │
│                       │  4. Update student_points                │              │
│                       │  5. Insert point_history                 │              │
│                       │  6. Check badge achievements             │              │
│                       │  7. Notify student                       │              │
│                       └──────────────────────────────────────────┘              │
│                                                                                  │
│  [B] ĐỔI QUÀ                                                                   │
│  ───────────                                                                    │
│                                                                                  │
│  ┌─────────────┐      ┌─────────────────┐      ┌─────────────────┐             │
│  │  Học viên   │      │   Reward Store  │      │   Admin/Staff   │             │
│  │  chọn quà   │─────▶│   (Frontend)    │─────▶│   Duyệt & phát  │             │
│  └─────────────┘      └─────────────────┘      └─────────────────┘             │
│                                                                                  │
│       │                        │                        │                       │
│       ▼                        ▼                        ▼                       │
│  ┌──────────┐          ┌──────────────┐          ┌──────────────┐              │
│  │ Xem điểm │          │ Tạo request  │          │ Xác nhận     │              │
│  │ hiện có  │          │ đổi quà      │          │ giao quà     │              │
│  │ 1500 pts │          │ (trừ điểm)   │          │ cập nhật     │              │
│  └──────────┘          └──────────────┘          │ status       │              │
│                                                   └──────────────┘              │
│                                                                                  │
│  [C] BẢNG XẾP HẠNG                                                             │
│  ─────────────────                                                              │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                     BẢNG XẾP HẠNG - THÁNG 12/2025                        │   │
│  ├──────┬──────────────────────┬───────────────┬───────────────────────────┤   │
│  │ Hạng │ Học viên             │ Điểm tháng    │ Thành tích                │   │
│  ├──────┼──────────────────────┼───────────────┼───────────────────────────┤   │
│  │  🥇  │ Nguyễn Văn A         │ 850 pts       │ 🎯 100% điểm danh        │   │
│  │  🥈  │ Trần Thị B           │ 720 pts       │ ⭐ 3 bài 10 điểm          │   │
│  │  🥉  │ Lê Văn C             │ 680 pts       │ 📚 Hoàn thành 100% BT    │   │
│  │  4   │ Phạm Thị D           │ 550 pts       │                           │   │
│  │  5   │ Hoàng Văn E          │ 480 pts       │                           │   │
│  └──────┴──────────────────────┴───────────────┴───────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 5: CỔNG PHỤ HUYNH (PARENT PORTAL)

## 5.1. Tổng quan Parent Module

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           PARENT PORTAL OVERVIEW                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Mục đích:                                                                      │
│  Cung cấp kênh thông tin cho phụ huynh theo dõi con em tại trung tâm           │
│                                                                                  │
│  Tính năng chính (Học từ BeeClass):                                            │
│  ✅ Tự đăng ký qua Zalo OTP (không cần admin tạo tài khoản)                    │
│  ✅ Liên kết với con em (1 phụ huynh - nhiều học viên)                          │
│  ✅ Xem điểm danh, điểm số, nhận xét của con                                   │
│  ✅ Xem và thanh toán hóa đơn học phí                                          │
│  ✅ Nhận thông báo từ giáo viên/trung tâm                                      │
│  ✅ Xem lịch học và lịch thi                                                   │
│                                                                                  │
│  Kênh tiếp cận:                                                                 │
│  • Web Portal: parent.{instance}.kiteclass.com                                  │
│  • Mobile App: KiteClass Parent (iOS/Android)                                   │
│  • Zalo Mini App (tương lai)                                                    │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.2. Parent Registration Flow (Zalo OTP)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      PARENT REGISTRATION FLOW (Zalo OTP)                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  [1] TRUNG TÂM TẠO LINK ĐĂNG KÝ                                                │
│  ──────────────────────────────                                                 │
│                                                                                  │
│  Admin/Teacher tạo link cho học viên → Gửi link cho phụ huynh                  │
│  Link format: https://acme.kiteclass.com/parent/register?student=ABC123        │
│                                                                                  │
│       │                                                                         │
│       ▼                                                                         │
│                                                                                  │
│  [2] PHỤ HUYNH MỞ LINK                                                         │
│  ────────────────────                                                           │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────┐                    │
│  │           ĐĂNG KÝ TÀI KHOẢN PHỤ HUYNH                  │                    │
│  │                                                         │                    │
│  │  Trung tâm: ACME Academy                               │                    │
│  │  Học viên: Nguyễn Văn An (Lớp Toán 10A)               │                    │
│  │                                                         │                    │
│  │  ─────────────────────────────────────────────────────  │                    │
│  │                                                         │                    │
│  │  Họ tên phụ huynh: _______________________             │                    │
│  │                                                         │                    │
│  │  Số điện thoại Zalo: _______________________           │                    │
│  │                                                         │                    │
│  │  Quan hệ với học viên: [ Bố ▼ ]                        │                    │
│  │                                                         │                    │
│  │  [ Gửi mã OTP qua Zalo ]                               │                    │
│  │                                                         │                    │
│  └────────────────────────────────────────────────────────┘                    │
│                                                                                  │
│       │                                                                         │
│       ▼                                                                         │
│                                                                                  │
│  [3] GỬI OTP QUA ZALO                                                          │
│  ────────────────────                                                           │
│                                                                                  │
│  Hệ thống call Zalo OA API → Gửi tin nhắn OTP                                  │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────┐                    │
│  │  📱 ZALO - ACME Academy                                │                    │
│  │  ────────────────────────────────────────────────────  │                    │
│  │                                                         │                    │
│  │  Chào Anh/Chị Nguyễn Văn Bình,                         │                    │
│  │                                                         │                    │
│  │  Mã OTP xác thực tài khoản phụ huynh của bạn là:       │                    │
│  │                                                         │                    │
│  │              🔐 123456                                  │                    │
│  │                                                         │                    │
│  │  Mã có hiệu lực trong 5 phút.                          │                    │
│  │  Vui lòng không chia sẻ mã này với người khác.         │                    │
│  │                                                         │                    │
│  │  ACME Academy                                          │                    │
│  └────────────────────────────────────────────────────────┘                    │
│                                                                                  │
│       │                                                                         │
│       ▼                                                                         │
│                                                                                  │
│  [4] XÁC THỰC & TẠO TÀI KHOẢN                                                  │
│  ────────────────────────────                                                   │
│                                                                                  │
│  Phụ huynh nhập OTP → Hệ thống verify → Tạo tài khoản PARENT                   │
│  → Link với student_id → Redirect to Parent Dashboard                          │
│                                                                                  │
│       │                                                                         │
│       ▼                                                                         │
│                                                                                  │
│  [5] PARENT DASHBOARD                                                          │
│  ────────────────────                                                           │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────┐    │
│  │                    TRANG PHỤ HUYNH - ACME Academy                      │    │
│  ├────────────────────────────────────────────────────────────────────────┤    │
│  │                                                                         │    │
│  │  👋 Xin chào, Anh Nguyễn Văn Bình                                     │    │
│  │                                                                         │    │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │    │
│  │  │  👦 CON EM CỦA BẠN                                              │  │    │
│  │  │                                                                  │  │    │
│  │  │  ┌────────────┐                                                 │  │    │
│  │  │  │ 📷 Avatar  │  Nguyễn Văn An                                  │  │    │
│  │  │  │            │  Lớp: Toán 10A - Thầy Minh                     │  │    │
│  │  │  │            │  Điểm danh tháng: 95% ✅                        │  │    │
│  │  │  │            │  [Xem chi tiết →]                               │  │    │
│  │  │  └────────────┘                                                 │  │    │
│  │  │                                                                  │  │    │
│  │  │  [+ Thêm con em khác]                                           │  │    │
│  │  └─────────────────────────────────────────────────────────────────┘  │    │
│  │                                                                         │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                │    │
│  │  │ 📅 LỊCH HỌC  │  │ 📝 ĐIỂM SỐ  │  │ 💰 HỌC PHÍ  │                │    │
│  │  │              │  │              │  │              │                │    │
│  │  │ 3 buổi/tuần  │  │ TB: 8.5      │  │ Còn nợ: 0đ  │                │    │
│  │  │ [Xem →]      │  │ [Xem →]      │  │ [Xem →]     │                │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                │    │
│  │                                                                         │    │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │    │
│  │  │  🔔 THÔNG BÁO MỚI                                              │  │    │
│  │  │  • Nhắc điểm danh: Ngày mai có buổi học lúc 18:00             │  │    │
│  │  │  • Thầy Minh: An đã hoàn thành tốt bài kiểm tra tuần này      │  │    │
│  │  │  • Hóa đơn tháng 12 đã được tạo                               │  │    │
│  │  └─────────────────────────────────────────────────────────────────┘  │    │
│  │                                                                         │    │
│  └────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.3. Database Schema - Parent

```sql
-- Bảng liên kết phụ huynh - học viên
CREATE TABLE user_module.parent_students (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_id UUID NOT NULL REFERENCES user_module.users(id),
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    relationship VARCHAR(20) NOT NULL,        -- FATHER, MOTHER, GUARDIAN, OTHER
    is_primary BOOLEAN DEFAULT false,         -- Phụ huynh chính
    linked_at TIMESTAMP DEFAULT NOW(),
    linked_by UUID REFERENCES user_module.users(id), -- Admin/teacher link hoặc tự đăng ký
    status VARCHAR(20) DEFAULT 'ACTIVE',
    UNIQUE(parent_id, student_id)
);

-- Đăng ký phụ huynh (pending verification)
CREATE TABLE user_module.parent_registrations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES user_module.students(id),
    phone_number VARCHAR(15) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(20) NOT NULL,
    otp_code VARCHAR(6),
    otp_expires_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING',     -- PENDING, VERIFIED, EXPIRED
    created_at TIMESTAMP DEFAULT NOW()
);

-- Thông báo cho phụ huynh
CREATE TABLE user_module.parent_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_id UUID NOT NULL REFERENCES user_module.users(id),
    student_id UUID REFERENCES user_module.students(id), -- NULL = thông báo chung
    notification_type VARCHAR(50) NOT NULL,   -- ATTENDANCE, GRADE, INVOICE, GENERAL
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    sent_via JSONB,                           -- ["APP", "ZALO", "EMAIL"]
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_parent_students_parent ON user_module.parent_students(parent_id);
CREATE INDEX idx_parent_students_student ON user_module.parent_students(student_id);
CREATE INDEX idx_parent_notifications_parent ON user_module.parent_notifications(parent_id);
```

---

# PHẦN 6: AUTHENTICATION & AUTHORIZATION V3

## 6.1. Ma trận phân quyền cập nhật (thêm PARENT)

| Permission | CENTER_OWNER | CENTER_ADMIN | TEACHER | STUDENT | PARENT |
|------------|:------------:|:------------:|:-------:|:-------:|:------:|
| **Course** |
| course:create | ✅ | ✅ | ❌ | ❌ | ❌ |
| course:read | ✅ | ✅ | ✅ | ✅ | ✅* |
| **Class** |
| class:create | ✅ | ✅ | ❌ | ❌ | ❌ |
| class:read | ✅ | ✅ | ✅* | ✅* | ✅* |
| **Attendance** |
| attendance:view | ✅ | ✅ | ✅* | ✅* | ✅* |
| attendance:mark | ✅ | ✅ | ✅* | ❌ | ❌ |
| **Grade** |
| grade:view | ✅ | ✅ | ✅* | ✅* | ✅* |
| grade:create | ✅ | ✅ | ✅* | ❌ | ❌ |
| **Billing** |
| billing:manage | ✅ | ✅ | ❌ | ❌ | ❌ |
| billing:view_own | ✅ | ✅ | ❌ | ✅* | ✅* |
| billing:pay | ❌ | ❌ | ❌ | ❌ | ✅* |
| **Gamification** |
| points:view | ✅ | ✅ | ✅* | ✅* | ✅* |
| rewards:exchange | ❌ | ❌ | ❌ | ✅* | ❌ |
| rewards:manage | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Parent Portal** |
| parent:register | ❌ | ❌ | ❌ | ❌ | ✅ |
| parent:view_child | ❌ | ❌ | ❌ | ❌ | ✅* |
| parent:receive_notification | ❌ | ❌ | ❌ | ❌ | ✅ |

*\* Chỉ trong phạm vi lớp/học viên được liên kết*

## 6.2. JWT Token Structure V3 (thêm parent_of)

```json
{
  "sub": "user-uuid-12345",
  "email": "parent@example.com",
  "name": "Nguyễn Văn Bình",
  "roles": ["PARENT"],
  "permissions": [
    "attendance:view",
    "grade:view",
    "billing:view_own",
    "billing:pay",
    "parent:view_child"
  ],
  "tenant_id": "center-uuid-789",
  "instance_id": "instance-001",

  // ⭐ NEW: Danh sách con em (cho PARENT role)
  "parent_of": [
    {
      "student_id": "student-uuid-001",
      "student_name": "Nguyễn Văn An",
      "class_ids": ["class-uuid-001", "class-uuid-002"]
    },
    {
      "student_id": "student-uuid-002",
      "student_name": "Nguyễn Thị Bình",
      "class_ids": ["class-uuid-003"]
    }
  ],

  "iat": 1703145600,
  "exp": 1703149200,
  "iss": "kiteclass-auth"
}
```

---

# PHẦN 7: TỔNG KẾT KIẾN TRÚC V3

## 7.1. So sánh các phiên bản

| Tính năng | V1 | V2 | V3 (Final) |
|-----------|:--:|:--:|:----------:|
| **KiteHub** |
| Architecture | 3 Microservices | Modular Monolith | Modular Monolith |
| AI Marketing | ❌ | ✅ | ✅ |
| **KiteClass Instance** |
| Services | 7 | 7 | **4** (Optimized) |
| AI Quiz Generator | ❌ | ❌ (Proposed) | ❌ (Rejected) |
| **Features** |
| Actor Parent | ❌ | ❌ | ✅ |
| Billing/Invoice | ❌ | ❌ | ✅ |
| Gamification | ❌ | ❌ | ✅ |
| QR Payment | ❌ | ❌ | ✅ |
| Zalo OTP | ❌ | ❌ | ✅ |

## 7.2. Danh sách tính năng V3

### KiteHub (Modular Monolith)
- ✅ Sale Module: Landing page, pricing, orders
- ✅ Message Module: Chat support, notifications
- ✅ Maintaining Module: Instance provisioning, monitoring
- ✅ AI Agent Module: Marketing assets generation

### KiteClass Instance (4 Services)

**Core Service:**
- ✅ Class Module: Courses, classes, schedules
- ✅ Learning Module: Attendance, assignments, grades
- ✅ Forum Module: Q&A forum
- ✅ **Billing Module**: Tuition, invoices, payments, debts, QR
- ✅ **Gamification Module**: Points, badges, rewards, leaderboard
- ✅ **Parent Module**: Portal, notifications, child linking

**User Service:**
- ✅ Authentication (JWT + OAuth)
- ✅ Authorization (RBAC)
- ✅ Multi-tenant support
- ✅ Parent registration (Zalo OTP)

**Media Service:**
- ✅ Video upload & transcode
- ✅ Live streaming (WebSocket)
- ✅ Recording

**Frontend:**
- ✅ Multi-role UI (Admin, Teacher, Student, Parent)
- ✅ Responsive design
- ✅ SSR for SEO

## 7.3. Lý do loại bỏ AI Quiz Generator

| Tiêu chí | Đánh giá | Ghi chú |
|----------|----------|---------|
| **Độ phức tạp** | Cao | Cần nhiều prompt engineering, fine-tuning |
| **Chi phí** | ~$128/tháng | Chi phí LLM API đáng kể |
| **Độ ổn định** | Thấp | Output không nhất quán, cần human review |
| **ROI** | Chưa rõ | Khách hàng chưa thực sự cần tính năng này |
| **Rủi ro** | Cao | Câu hỏi sai có thể ảnh hưởng chất lượng giáo dục |

**Kết luận:** Tính năng AI Quiz Generator sẽ được đánh giá lại trong các phiên bản sau khi:
- Công nghệ LLM ổn định hơn
- Chi phí giảm
- Có feedback thực tế từ khách hàng

## 7.4. Roadmap triển khai V3

| Phase | Tính năng | Ưu tiên |
|-------|-----------|---------|
| **Phase 1** | Tối ưu 7→4 services | P0 |
| **Phase 2** | Billing Module (QR Payment) | P0 |
| **Phase 3** | Parent Module (Zalo OTP) | P1 |
| **Phase 4** | Gamification Module | P1 |
| **Phase 5** | Testing & Deployment | P0 |

---

*Báo cáo được tạo bởi: Claude Assistant*
*Ngày: 23/12/2025*
*Phiên bản: 3.0 (Final)*
