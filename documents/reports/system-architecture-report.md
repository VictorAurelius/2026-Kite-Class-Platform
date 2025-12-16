# BÁO CÁO KIẾN TRÚC HỆ THỐNG KITECLASS PLATFORM

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Tên dự án** | KiteClass Platform |
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 16/12/2025 |
| **Loại tài liệu** | Báo cáo kiến trúc hệ thống |

---

# PHẦN 1: TỔNG QUAN HỆ THỐNG

## 1.1. Giới thiệu

KiteClass Platform là nền tảng quản lý lớp học trực tuyến được thiết kế theo kiến trúc **Microservices**, cho phép các tổ chức giáo dục, doanh nghiệp đào tạo và giảng viên độc lập triển khai và vận hành hệ thống học trực tuyến một cách linh hoạt và có khả năng mở rộng cao.

## 1.2. Sơ đồ kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Web Browser │  │  Mobile App  │  │  Admin Panel │  │  Partner API │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
└─────────┼─────────────────┼─────────────────┼─────────────────┼─────────────┘
          │                 │                 │                 │
          └─────────────────┴────────┬────────┴─────────────────┘
                                     │ HTTPS/REST
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API GATEWAY LAYER                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                     Kong / NGINX API Gateway                         │    │
│  │  • SSL Termination  • Rate Limiting  • Authentication  • Routing    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │ RESTful API
          ┌───────────────────────────┼───────────────────────────┐
          │                           │                           │
          ▼                           ▼                           ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│   KITEHUB PLATFORM  │   │  KITECLASS CORE     │   │  EXPAND SERVICES    │
│   (Central Hub)     │   │  (Multi-Instance)   │   │  (Optional Modules) │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘
```

## 1.3. Mô hình giao tiếp giữa các Service

Tất cả các service trong hệ thống giao tiếp với nhau thông qua **RESTful API** theo các nguyên tắc:

| Đặc điểm | Mô tả |
|----------|-------|
| **Protocol** | HTTPS |
| **Format** | JSON |
| **Authentication** | JWT Token / API Key |
| **Versioning** | URL-based (v1, v2) |
| **Documentation** | OpenAPI/Swagger 3.0 |

---

# PHẦN 2: USE CASE TỔNG QUAN HỆ THỐNG

## 2.1. Sơ đồ Use Case tổng quan

```
                            ┌─────────────────────────────────────┐
                            │         KITECLASS PLATFORM          │
                            └─────────────────────────────────────┘
                                              │
     ┌────────────────────────────────────────┼────────────────────────────────────────┐
     │                                        │                                        │
     ▼                                        ▼                                        ▼
┌─────────────┐                        ┌─────────────┐                        ┌─────────────┐
│   Học viên  │                        │  Giảng viên │                        │  Quản trị   │
│  (Student)  │                        │ (Instructor)│                        │   viên      │
└──────┬──────┘                        └──────┬──────┘                        └──────┬──────┘
       │                                      │                                      │
       │  ┌──────────────────────────────────┐│┌──────────────────────────────────┐  │
       │  │         USE CASES                ││|         USE CASES                │  │
       │  ├──────────────────────────────────┤│├──────────────────────────────────┤  │
       ├──┤ • Đăng ký tài khoản              ││├──┤ • Tạo và quản lý khóa học      │  │
       ├──┤ • Đăng nhập hệ thống             ├┼┼──┤ • Tạo bài giảng/tài liệu       │  │
       ├──┤ • Tìm kiếm khóa học              │││  │ • Quản lý lịch học             │  │
       ├──┤ • Đăng ký khóa học               │││  │ • Điểm danh học viên           │  │
       ├──┤ • Xem bài giảng video            │││  │ • Tạo và chấm bài tập          │  │
       ├──┤ • Tham gia lớp học trực tuyến    │││  │ • Đánh giá học viên            │  │
       ├──┤ • Nộp bài tập                    │││  │ • Xem thống kê lớp học         │  │
       ├──┤ • Xem điểm và kết quả            │││  │ • Phát trực tiếp buổi học      │  │
       ├──┤ • Tham gia diễn đàn              │││  │ • Trả lời câu hỏi diễn đàn     │──┤
       └──┤ • Nhận thông báo                 │││  │ • Gửi thông báo cho học viên   │  │
          └──────────────────────────────────┘│└──────────────────────────────────┘  │
                                              │                                      │
                                              │  ┌──────────────────────────────────┐│
                                              │  │         USE CASES                ││
                                              │  ├──────────────────────────────────┤│
                                              │  │ • Quản lý tất cả người dùng      ├┤
                                              │  │ • Cấu hình hệ thống              ││
                                              │  │ • Quản lý các instance KiteClass ││
                                              │  │ • Giám sát hiệu năng hệ thống    ││
                                              │  │ • Quản lý bán hàng/thanh toán    ││
                                              │  │ • Xem báo cáo tổng hợp           ││
                                              │  │ • Quản lý tư vấn khách hàng      ││
                                              │  │ • Bảo trì và cập nhật hệ thống   ││
                                              │  └──────────────────────────────────┘│
                                              └────────────────────────────────────────
```

## 2.2. Danh sách Use Case tổng quan

### 2.2.1. Nhóm Use Case - Quản lý người dùng

| UC ID | Tên Use Case | Actor | Mô tả |
|-------|--------------|-------|-------|
| UC-001 | Đăng ký tài khoản | Học viên, Giảng viên | Cho phép người dùng tạo tài khoản mới trên hệ thống |
| UC-002 | Đăng nhập | Tất cả | Xác thực và cấp quyền truy cập hệ thống |
| UC-003 | Quản lý hồ sơ | Tất cả | Cập nhật thông tin cá nhân, ảnh đại diện |
| UC-004 | Đặt lại mật khẩu | Tất cả | Khôi phục mật khẩu qua email |
| UC-005 | Phân quyền người dùng | Admin | Gán vai trò và quyền hạn cho người dùng |

### 2.2.2. Nhóm Use Case - Quản lý khóa học

| UC ID | Tên Use Case | Actor | Mô tả |
|-------|--------------|-------|-------|
| UC-006 | Tạo khóa học | Giảng viên, Admin | Tạo mới khóa học với thông tin cơ bản |
| UC-007 | Cập nhật khóa học | Giảng viên, Admin | Chỉnh sửa thông tin khóa học |
| UC-008 | Xóa khóa học | Admin | Xóa khóa học khỏi hệ thống |
| UC-009 | Tìm kiếm khóa học | Học viên | Tìm kiếm theo từ khóa, danh mục, giảng viên |
| UC-010 | Đăng ký khóa học | Học viên | Ghi danh vào khóa học |

### 2.2.3. Nhóm Use Case - Học tập

| UC ID | Tên Use Case | Actor | Mô tả |
|-------|--------------|-------|-------|
| UC-011 | Xem bài giảng video | Học viên | Xem video bài giảng với theo dõi tiến độ |
| UC-012 | Tham gia lớp học trực tuyến | Học viên, Giảng viên | Tham gia buổi học qua video conference |
| UC-013 | Nộp bài tập | Học viên | Upload và nộp bài tập |
| UC-014 | Chấm bài tập | Giảng viên | Đánh giá và cho điểm bài tập |
| UC-015 | Xem kết quả học tập | Học viên | Xem điểm, tiến độ, chứng chỉ |

### 2.2.4. Nhóm Use Case - Vận hành

| UC ID | Tên Use Case | Actor | Mô tả |
|-------|--------------|-------|-------|
| UC-016 | Tư vấn khách hàng | Admin, Support | Chat hỗ trợ realtime với khách hàng |
| UC-017 | Quản lý đơn hàng | Admin | Xử lý đăng ký, thanh toán khóa học |
| UC-018 | Giám sát hệ thống | Admin | Monitor hiệu năng, logs, alerts |
| UC-019 | Tạo báo cáo | Admin, Giảng viên | Xuất báo cáo thống kê đa dạng |
| UC-020 | Quản lý instance | Super Admin | Tạo, cấu hình, scale các KiteClass instance |

---

# PHẦN 3: USE CASE CHI TIẾT TỪNG SERVICE

## 3.1. KITEHUB PLATFORM SERVICES

### 3.1.1. Sale Service

**Mô tả:** Service xử lý tất cả hoạt động marketing, bán hàng và thanh toán của nền tảng.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │    SALE SERVICE     │
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │  Khách   │                    │ Học viên │                    │  Admin   │
        │   hàng   │                    │          │                    │          │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-S01: Xem landing page      │                               │
             │ UC-S02: Xem danh sách khóa học│                               │
             │ UC-S03: Xem chi tiết khóa học │                               │
             │ UC-S04: Đăng ký dùng thử      │                               │
             │                               │                               │
             │                               │ UC-S05: Thanh toán khóa học   │
             │                               │ UC-S06: Xem lịch sử giao dịch │
             │                               │ UC-S07: Yêu cầu hoàn tiền     │
             │                               │                               │
             │                               │                               │ UC-S08: Quản lý sản phẩm
             │                               │                               │ UC-S09: Quản lý khuyến mãi
             │                               │                               │ UC-S10: Cấu hình thanh toán
             │                               │                               │ UC-S11: Xem báo cáo doanh thu
             │                               │                               │ UC-S12: Quản lý đối tác
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-S01 | Xem landing page | Khách hàng | Hiển thị trang chủ với thông tin giới thiệu, tính năng nổi bật | `GET /api/v1/landing` |
| UC-S02 | Xem danh sách khóa học | Khách hàng, Học viên | Lấy danh sách khóa học với filter, pagination | `GET /api/v1/courses` |
| UC-S03 | Xem chi tiết khóa học | Khách hàng, Học viên | Xem thông tin chi tiết, giảng viên, đánh giá | `GET /api/v1/courses/{id}` |
| UC-S04 | Đăng ký dùng thử | Khách hàng | Đăng ký tài khoản trial với thời hạn giới hạn | `POST /api/v1/trial/register` |
| UC-S05 | Thanh toán khóa học | Học viên | Xử lý thanh toán qua các cổng (VNPay, Momo, Card) | `POST /api/v1/payments` |
| UC-S06 | Xem lịch sử giao dịch | Học viên | Lấy danh sách các giao dịch đã thực hiện | `GET /api/v1/transactions` |
| UC-S07 | Yêu cầu hoàn tiền | Học viên | Gửi yêu cầu refund với lý do | `POST /api/v1/refunds` |
| UC-S08 | Quản lý sản phẩm | Admin | CRUD sản phẩm/gói dịch vụ | `CRUD /api/v1/admin/products` |
| UC-S09 | Quản lý khuyến mãi | Admin | Tạo, cập nhật mã giảm giá, chiến dịch | `CRUD /api/v1/admin/promotions` |
| UC-S10 | Cấu hình thanh toán | Admin | Thiết lập cổng thanh toán, phí | `PUT /api/v1/admin/payment-config` |
| UC-S11 | Xem báo cáo doanh thu | Admin | Dashboard doanh thu, thống kê bán hàng | `GET /api/v1/admin/reports/revenue` |
| UC-S12 | Quản lý đối tác | Admin | Quản lý affiliate, reseller partners | `CRUD /api/v1/admin/partners` |

---

### 3.1.2. Message Service

**Mô tả:** Service xử lý tất cả hoạt động giao tiếp, thông báo và hỗ trợ khách hàng realtime.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │   MESSAGE SERVICE   │
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │  Khách   │                    │ Học viên │                    │  Admin/  │
        │   hàng   │                    │ Giảng viên│                   │ Support  │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-M01: Chat tư vấn           │                               │
             │ UC-M02: Gửi yêu cầu hỗ trợ    │                               │
             │                               │                               │
             │                               │ UC-M03: Nhận thông báo        │
             │                               │ UC-M04: Quản lý thông báo     │
             │                               │ UC-M05: Chat trong lớp học    │
             │                               │ UC-M06: Gửi tin nhắn cá nhân  │
             │                               │                               │
             │                               │                               │ UC-M07: Trả lời chat support
             │                               │                               │ UC-M08: Gửi thông báo hàng loạt
             │                               │                               │ UC-M09: Quản lý chatbot
             │                               │                               │ UC-M10: Xem thống kê hỗ trợ
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-M01 | Chat tư vấn | Khách hàng | Trò chuyện realtime với tư vấn viên | `WS /api/v1/chat/support` |
| UC-M02 | Gửi yêu cầu hỗ trợ | Khách hàng | Tạo ticket hỗ trợ với mô tả vấn đề | `POST /api/v1/tickets` |
| UC-M03 | Nhận thông báo | Học viên, Giảng viên | Nhận notifications qua push/email/in-app | `GET /api/v1/notifications` |
| UC-M04 | Quản lý thông báo | Học viên, Giảng viên | Đánh dấu đã đọc, xóa, cấu hình preferences | `PUT /api/v1/notifications/{id}` |
| UC-M05 | Chat trong lớp học | Học viên, Giảng viên | Trao đổi trong phòng chat của lớp học | `WS /api/v1/chat/class/{classId}` |
| UC-M06 | Gửi tin nhắn cá nhân | Học viên, Giảng viên | Direct message giữa các users | `POST /api/v1/messages` |
| UC-M07 | Trả lời chat support | Admin, Support | Xử lý và phản hồi các yêu cầu hỗ trợ | `POST /api/v1/admin/tickets/{id}/reply` |
| UC-M08 | Gửi thông báo hàng loạt | Admin | Broadcast notification tới nhóm users | `POST /api/v1/admin/notifications/broadcast` |
| UC-M09 | Quản lý chatbot | Admin | Cấu hình responses, training chatbot | `CRUD /api/v1/admin/chatbot` |
| UC-M10 | Xem thống kê hỗ trợ | Admin | Dashboard về tickets, response time, satisfaction | `GET /api/v1/admin/support/stats` |

---

### 3.1.3. Maintaining Service

**Mô tả:** Service quản lý, giám sát và điều phối tất cả các instance KiteClass trong hệ thống.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │ MAINTAINING SERVICE │
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │  Super   │                    │  Admin   │                    │  System  │
        │  Admin   │                    │          │                    │ (Auto)   │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-MT01: Tạo instance mới     │                               │
             │ UC-MT02: Xóa instance         │                               │
             │ UC-MT03: Scale instance       │                               │
             │ UC-MT04: Backup/Restore       │                               │
             │ UC-MT05: Cấu hình multi-tenant│                               │
             │                               │                               │
             │                               │ UC-MT06: Xem health status    │
             │                               │ UC-MT07: Xem logs             │
             │                               │ UC-MT08: Cấu hình alerts      │
             │                               │ UC-MT09: Xem metrics          │
             │                               │                               │
             │                               │                               │ UC-MT10: Auto-scaling
             │                               │                               │ UC-MT11: Health check
             │                               │                               │ UC-MT12: Auto-recovery
             │                               │                               │ UC-MT13: Scheduled backup
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-MT01 | Tạo instance mới | Super Admin | Provision KiteClass instance mới cho khách hàng | `POST /api/v1/instances` |
| UC-MT02 | Xóa instance | Super Admin | Decommission và cleanup instance | `DELETE /api/v1/instances/{id}` |
| UC-MT03 | Scale instance | Super Admin | Điều chỉnh resources (CPU, RAM, replicas) | `PUT /api/v1/instances/{id}/scale` |
| UC-MT04 | Backup/Restore | Super Admin | Tạo backup và restore data | `POST /api/v1/instances/{id}/backup` |
| UC-MT05 | Cấu hình multi-tenant | Super Admin | Thiết lập isolation, quotas, limits | `PUT /api/v1/instances/{id}/config` |
| UC-MT06 | Xem health status | Admin | Dashboard tổng quan health các services | `GET /api/v1/health` |
| UC-MT07 | Xem logs | Admin | Truy vấn và filter logs tập trung | `GET /api/v1/logs` |
| UC-MT08 | Cấu hình alerts | Admin | Thiết lập ngưỡng cảnh báo, channels | `CRUD /api/v1/alerts` |
| UC-MT09 | Xem metrics | Admin | Visualize metrics (CPU, memory, latency) | `GET /api/v1/metrics` |
| UC-MT10 | Auto-scaling | System | Tự động scale dựa trên load | Internal |
| UC-MT11 | Health check | System | Kiểm tra định kỳ health các services | Internal |
| UC-MT12 | Auto-recovery | System | Tự động restart services bị lỗi | Internal |
| UC-MT13 | Scheduled backup | System | Backup tự động theo lịch | Internal |

---

## 3.2. KITECLASS CORE SERVICES

### 3.2.1. Main Class Service

**Mô tả:** Service cốt lõi quản lý thông tin lớp học, khóa học, lịch học và bài giảng.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │  MAIN CLASS SERVICE │
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │ Học viên │                    │ Giảng    │                    │  Admin   │
        │          │                    │   viên   │                    │          │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-MC01: Xem danh sách lớp    │                               │
             │ UC-MC02: Xem chi tiết lớp     │                               │
             │ UC-MC03: Xem lịch học         │                               │
             │ UC-MC04: Xem danh sách bài    │                               │
             │ UC-MC05: Tải tài liệu         │                               │
             │                               │                               │
             │                               │ UC-MC06: Tạo lớp học          │
             │                               │ UC-MC07: Cập nhật lớp học     │
             │                               │ UC-MC08: Quản lý lịch học     │
             │                               │ UC-MC09: Upload bài giảng     │
             │                               │ UC-MC10: Quản lý tài liệu     │
             │                               │                               │
             │                               │                               │ UC-MC11: Duyệt lớp học
             │                               │                               │ UC-MC12: Quản lý danh mục
             │                               │                               │ UC-MC13: Cấu hình template
             │                               │                               │ UC-MC14: Xem báo cáo lớp học
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-MC01 | Xem danh sách lớp | Học viên | Lấy danh sách lớp đã đăng ký | `GET /api/v1/classes` |
| UC-MC02 | Xem chi tiết lớp | Học viên | Xem thông tin chi tiết lớp học | `GET /api/v1/classes/{id}` |
| UC-MC03 | Xem lịch học | Học viên, Giảng viên | Lấy lịch học theo tuần/tháng | `GET /api/v1/classes/{id}/schedule` |
| UC-MC04 | Xem danh sách bài | Học viên | Lấy danh sách bài giảng của lớp | `GET /api/v1/classes/{id}/lessons` |
| UC-MC05 | Tải tài liệu | Học viên | Download tài liệu học tập | `GET /api/v1/materials/{id}/download` |
| UC-MC06 | Tạo lớp học | Giảng viên | Tạo lớp học mới với thông tin cơ bản | `POST /api/v1/classes` |
| UC-MC07 | Cập nhật lớp học | Giảng viên | Chỉnh sửa thông tin lớp học | `PUT /api/v1/classes/{id}` |
| UC-MC08 | Quản lý lịch học | Giảng viên | CRUD sessions/buổi học | `CRUD /api/v1/classes/{id}/sessions` |
| UC-MC09 | Upload bài giảng | Giảng viên | Tải lên nội dung bài giảng | `POST /api/v1/lessons` |
| UC-MC10 | Quản lý tài liệu | Giảng viên | CRUD tài liệu học tập | `CRUD /api/v1/materials` |
| UC-MC11 | Duyệt lớp học | Admin | Phê duyệt/từ chối lớp học mới | `PUT /api/v1/admin/classes/{id}/approve` |
| UC-MC12 | Quản lý danh mục | Admin | CRUD categories, subjects | `CRUD /api/v1/admin/categories` |
| UC-MC13 | Cấu hình template | Admin | Thiết lập templates lớp học | `CRUD /api/v1/admin/templates` |
| UC-MC14 | Xem báo cáo lớp học | Admin | Thống kê tổng quan các lớp học | `GET /api/v1/admin/reports/classes` |

---

### 3.2.2. User Service

**Mô tả:** Service quản lý toàn bộ người dùng, xác thực, phân quyền trong hệ thống.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │    USER SERVICE     │
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │  Guest   │                    │   User   │                    │  Admin   │
        │          │                    │(Authenticated)               │          │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-U01: Đăng ký tài khoản     │                               │
             │ UC-U02: Đăng nhập             │                               │
             │ UC-U03: Quên mật khẩu         │                               │
             │ UC-U04: Đăng nhập OAuth       │                               │
             │                               │                               │
             │                               │ UC-U05: Xem hồ sơ             │
             │                               │ UC-U06: Cập nhật hồ sơ        │
             │                               │ UC-U07: Đổi mật khẩu          │
             │                               │ UC-U08: Cài đặt bảo mật       │
             │                               │ UC-U09: Đăng xuất             │
             │                               │                               │
             │                               │                               │ UC-U10: Quản lý users
             │                               │                               │ UC-U11: Phân quyền
             │                               │                               │ UC-U12: Xem audit log
             │                               │                               │ UC-U13: Cấu hình OAuth
             │                               │                               │ UC-U14: Quản lý roles
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-U01 | Đăng ký tài khoản | Guest | Tạo tài khoản với email/phone verification | `POST /api/v1/auth/register` |
| UC-U02 | Đăng nhập | Guest | Xác thực và nhận JWT token | `POST /api/v1/auth/login` |
| UC-U03 | Quên mật khẩu | Guest | Gửi email reset password | `POST /api/v1/auth/forgot-password` |
| UC-U04 | Đăng nhập OAuth | Guest | Đăng nhập qua Google, Facebook | `GET /api/v1/auth/oauth/{provider}` |
| UC-U05 | Xem hồ sơ | User | Lấy thông tin profile | `GET /api/v1/users/me` |
| UC-U06 | Cập nhật hồ sơ | User | Update thông tin cá nhân | `PUT /api/v1/users/me` |
| UC-U07 | Đổi mật khẩu | User | Thay đổi password | `PUT /api/v1/users/me/password` |
| UC-U08 | Cài đặt bảo mật | User | Cấu hình 2FA, sessions | `PUT /api/v1/users/me/security` |
| UC-U09 | Đăng xuất | User | Invalidate token | `POST /api/v1/auth/logout` |
| UC-U10 | Quản lý users | Admin | CRUD tất cả users | `CRUD /api/v1/admin/users` |
| UC-U11 | Phân quyền | Admin | Gán roles/permissions cho user | `PUT /api/v1/admin/users/{id}/roles` |
| UC-U12 | Xem audit log | Admin | Xem lịch sử hoạt động users | `GET /api/v1/admin/audit-logs` |
| UC-U13 | Cấu hình OAuth | Admin | Thiết lập OAuth providers | `PUT /api/v1/admin/oauth/config` |
| UC-U14 | Quản lý roles | Admin | CRUD roles và permissions | `CRUD /api/v1/admin/roles` |

---

### 3.2.3. CMC Service (Class Management Core)

**Mô tả:** Service quản lý các hoạt động cốt lõi của lớp học: điểm danh, bài tập, đánh giá, thống kê.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │     CMC SERVICE     │
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │ Học viên │                    │ Giảng    │                    │  Admin   │
        │          │                    │   viên   │                    │          │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-CMC01: Điểm danh           │                               │
             │ UC-CMC02: Xem bài tập         │                               │
             │ UC-CMC03: Nộp bài tập         │                               │
             │ UC-CMC04: Xem điểm            │                               │
             │ UC-CMC05: Xem tiến độ         │                               │
             │ UC-CMC06: Xem lịch sử điểm danh│                              │
             │                               │                               │
             │                               │ UC-CMC07: Tạo phiên điểm danh │
             │                               │ UC-CMC08: Tạo bài tập         │
             │                               │ UC-CMC09: Chấm bài tập        │
             │                               │ UC-CMC10: Nhập điểm           │
             │                               │ UC-CMC11: Đánh giá học viên   │
             │                               │ UC-CMC12: Xem báo cáo lớp     │
             │                               │                               │
             │                               │                               │ UC-CMC13: Cấu hình đánh giá
             │                               │                               │ UC-CMC14: Quản lý rubric
             │                               │                               │ UC-CMC15: Export báo cáo
             │                               │                               │ UC-CMC16: Cấu hình điểm danh
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-CMC01 | Điểm danh | Học viên | Check-in buổi học (QR, GPS, manual) | `POST /api/v1/attendance/check-in` |
| UC-CMC02 | Xem bài tập | Học viên | Lấy danh sách bài tập được giao | `GET /api/v1/assignments` |
| UC-CMC03 | Nộp bài tập | Học viên | Submit bài làm | `POST /api/v1/assignments/{id}/submissions` |
| UC-CMC04 | Xem điểm | Học viên | Lấy bảng điểm cá nhân | `GET /api/v1/grades/me` |
| UC-CMC05 | Xem tiến độ | Học viên | Dashboard tiến độ học tập | `GET /api/v1/progress/me` |
| UC-CMC06 | Xem lịch sử điểm danh | Học viên | Lấy lịch sử attendance | `GET /api/v1/attendance/me` |
| UC-CMC07 | Tạo phiên điểm danh | Giảng viên | Mở session điểm danh | `POST /api/v1/attendance/sessions` |
| UC-CMC08 | Tạo bài tập | Giảng viên | Tạo assignment mới | `POST /api/v1/assignments` |
| UC-CMC09 | Chấm bài tập | Giảng viên | Grade submissions | `PUT /api/v1/submissions/{id}/grade` |
| UC-CMC10 | Nhập điểm | Giảng viên | Input grades cho học viên | `POST /api/v1/grades` |
| UC-CMC11 | Đánh giá học viên | Giảng viên | Viết nhận xét, feedback | `POST /api/v1/evaluations` |
| UC-CMC12 | Xem báo cáo lớp | Giảng viên | Thống kê lớp học | `GET /api/v1/reports/class/{id}` |
| UC-CMC13 | Cấu hình đánh giá | Admin | Thiết lập grading system | `PUT /api/v1/admin/grading-config` |
| UC-CMC14 | Quản lý rubric | Admin | CRUD rubrics đánh giá | `CRUD /api/v1/admin/rubrics` |
| UC-CMC15 | Export báo cáo | Admin | Xuất reports (Excel, PDF) | `GET /api/v1/admin/reports/export` |
| UC-CMC16 | Cấu hình điểm danh | Admin | Thiết lập rules điểm danh | `PUT /api/v1/admin/attendance-config` |

---

## 3.3. EXPAND SERVICES (Dịch vụ mở rộng)

### 3.3.1. Video Learning Service

**Mô tả:** Service quản lý video bài giảng, theo dõi tiến độ xem và analytics.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │VIDEO LEARNING SERVICE│
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │ Học viên │                    │ Giảng    │                    │  Admin   │
        │          │                    │   viên   │                    │          │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-VL01: Xem video            │                               │
             │ UC-VL02: Tiếp tục xem         │                               │
             │ UC-VL03: Xem tiến độ video    │                               │
             │ UC-VL04: Thay đổi chất lượng  │                               │
             │ UC-VL05: Tải video offline    │                               │
             │                               │                               │
             │                               │ UC-VL06: Upload video         │
             │                               │ UC-VL07: Quản lý video        │
             │                               │ UC-VL08: Xem analytics        │
             │                               │                               │
             │                               │                               │ UC-VL09: Cấu hình transcoding
             │                               │                               │ UC-VL10: Quản lý storage
             │                               │                               │ UC-VL11: Cấu hình CDN
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-VL01 | Xem video | Học viên | Stream video với adaptive bitrate | `GET /api/v1/videos/{id}/stream` |
| UC-VL02 | Tiếp tục xem | Học viên | Resume từ vị trí đã xem | `GET /api/v1/videos/{id}/resume` |
| UC-VL03 | Xem tiến độ video | Học viên | Tracking tiến độ xem | `GET /api/v1/videos/progress` |
| UC-VL04 | Thay đổi chất lượng | Học viên | Chọn quality (360p, 720p, 1080p) | `PUT /api/v1/videos/{id}/quality` |
| UC-VL05 | Tải video offline | Học viên | Download for offline viewing | `GET /api/v1/videos/{id}/download` |
| UC-VL06 | Upload video | Giảng viên | Upload và transcode video | `POST /api/v1/videos` |
| UC-VL07 | Quản lý video | Giảng viên | CRUD video metadata | `CRUD /api/v1/videos` |
| UC-VL08 | Xem analytics | Giảng viên | Video engagement metrics | `GET /api/v1/videos/{id}/analytics` |
| UC-VL09 | Cấu hình transcoding | Admin | Thiết lập encoding profiles | `PUT /api/v1/admin/transcoding-config` |
| UC-VL10 | Quản lý storage | Admin | Monitor và manage storage | `GET /api/v1/admin/storage` |
| UC-VL11 | Cấu hình CDN | Admin | Setup CDN distribution | `PUT /api/v1/admin/cdn-config` |

---

### 3.3.2. Streaming Service

**Mô tả:** Service phát trực tiếp buổi học với video conference và live streaming.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │  STREAMING SERVICE  │
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │ Học viên │                    │ Giảng    │                    │  Admin   │
        │          │                    │   viên   │                    │          │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-ST01: Tham gia stream      │                               │
             │ UC-ST02: Xem live             │                               │
             │ UC-ST03: Tương tác chat       │                               │
             │ UC-ST04: Giơ tay phát biểu    │                               │
             │ UC-ST05: Chia sẻ màn hình     │                               │
             │                               │                               │
             │                               │ UC-ST06: Bắt đầu stream       │
             │                               │ UC-ST07: Kết thúc stream      │
             │                               │ UC-ST08: Quản lý participants │
             │                               │ UC-ST09: Chia sẻ màn hình     │
             │                               │ UC-ST10: Recording            │
             │                               │                               │
             │                               │                               │ UC-ST11: Cấu hình streaming
             │                               │                               │ UC-ST12: Monitor bandwidth
             │                               │                               │ UC-ST13: Quản lý recordings
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-ST01 | Tham gia stream | Học viên | Join vào live session | `POST /api/v1/streams/{id}/join` |
| UC-ST02 | Xem live | Học viên | Watch live broadcast | `GET /api/v1/streams/{id}/watch` |
| UC-ST03 | Tương tác chat | Học viên, Giảng viên | Chat trong live stream | `WS /api/v1/streams/{id}/chat` |
| UC-ST04 | Giơ tay phát biểu | Học viên | Request to speak | `POST /api/v1/streams/{id}/raise-hand` |
| UC-ST05 | Chia sẻ màn hình (SV) | Học viên | Screen share khi được phép | `POST /api/v1/streams/{id}/share-screen` |
| UC-ST06 | Bắt đầu stream | Giảng viên | Start live session | `POST /api/v1/streams` |
| UC-ST07 | Kết thúc stream | Giảng viên | End live session | `POST /api/v1/streams/{id}/end` |
| UC-ST08 | Quản lý participants | Giảng viên | Mute, kick, allow speak | `PUT /api/v1/streams/{id}/participants` |
| UC-ST09 | Chia sẻ màn hình (GV) | Giảng viên | Screen share | `POST /api/v1/streams/{id}/share-screen` |
| UC-ST10 | Recording | Giảng viên | Ghi lại buổi học | `POST /api/v1/streams/{id}/record` |
| UC-ST11 | Cấu hình streaming | Admin | Setup streaming server | `PUT /api/v1/admin/streaming-config` |
| UC-ST12 | Monitor bandwidth | Admin | Theo dõi network usage | `GET /api/v1/admin/streaming/metrics` |
| UC-ST13 | Quản lý recordings | Admin | Manage recorded videos | `CRUD /api/v1/admin/recordings` |

---

### 3.3.3. Forum Service

**Mô tả:** Service diễn đàn trao đổi, hỏi đáp giữa học viên và giảng viên.

#### Sơ đồ Use Case

```
                                    ┌─────────────────────┐
                                    │    FORUM SERVICE    │
                                    └─────────────────────┘
                                              │
              ┌───────────────────────────────┼───────────────────────────────┐
              │                               │                               │
              ▼                               ▼                               ▼
        ┌──────────┐                    ┌──────────┐                    ┌──────────┐
        │ Học viên │                    │ Giảng    │                    │  Admin   │
        │          │                    │   viên   │                    │          │
        └────┬─────┘                    └────┬─────┘                    └────┬─────┘
             │                               │                               │
             │ UC-F01: Xem danh sách topics  │                               │
             │ UC-F02: Tạo topic mới         │                               │
             │ UC-F03: Trả lời topic         │                               │
             │ UC-F04: Tìm kiếm              │                               │
             │ UC-F05: Vote câu trả lời      │                               │
             │ UC-F06: Đánh dấu bookmark     │                               │
             │                               │                               │
             │                               │ UC-F07: Pin topic             │
             │                               │ UC-F08: Đánh dấu giải đáp     │
             │                               │ UC-F09: Quản lý topics        │
             │                               │                               │
             │                               │                               │ UC-F10: Moderation
             │                               │                               │ UC-F11: Quản lý categories
             │                               │                               │ UC-F12: Cấu hình forum
```

#### Chi tiết Use Case

| UC ID | Tên Use Case | Actor | Mô tả chi tiết | API Endpoint |
|-------|--------------|-------|----------------|--------------|
| UC-F01 | Xem danh sách topics | Học viên | Browse topics với filter | `GET /api/v1/forums/topics` |
| UC-F02 | Tạo topic mới | Học viên | Đặt câu hỏi mới | `POST /api/v1/forums/topics` |
| UC-F03 | Trả lời topic | Học viên, Giảng viên | Reply to topic | `POST /api/v1/forums/topics/{id}/replies` |
| UC-F04 | Tìm kiếm | Học viên | Search topics và replies | `GET /api/v1/forums/search` |
| UC-F05 | Vote câu trả lời | Học viên, Giảng viên | Upvote/downvote | `POST /api/v1/forums/replies/{id}/vote` |
| UC-F06 | Đánh dấu bookmark | Học viên | Save topic for later | `POST /api/v1/forums/topics/{id}/bookmark` |
| UC-F07 | Pin topic | Giảng viên | Ghim topic quan trọng | `PUT /api/v1/forums/topics/{id}/pin` |
| UC-F08 | Đánh dấu giải đáp | Giảng viên | Mark as answered | `PUT /api/v1/forums/topics/{id}/resolved` |
| UC-F09 | Quản lý topics | Giảng viên | Edit/delete own topics | `CRUD /api/v1/forums/topics` |
| UC-F10 | Moderation | Admin | Review, hide, delete content | `PUT /api/v1/admin/forums/moderate` |
| UC-F11 | Quản lý categories | Admin | CRUD forum categories | `CRUD /api/v1/admin/forums/categories` |
| UC-F12 | Cấu hình forum | Admin | Setup forum rules, settings | `PUT /api/v1/admin/forums/config` |

---

# PHẦN 4: CÔNG NGHỆ VÀ DEPLOYMENT

## 4.1. Tech Stack tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              TECHNOLOGY STACK                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  FRONTEND          │  BACKEND           │  DATABASE          │  DEVOPS      │
│  ─────────         │  ─────────         │  ─────────         │  ─────────   │
│  • Next.js 14      │  • NestJS          │  • PostgreSQL      │  • Docker    │
│  • React 18        │  • Node.js 20      │  • MongoDB         │  • Kubernetes│
│  • TypeScript      │  • TypeScript      │  • Redis           │  • Helm      │
│  • TailwindCSS     │  • RESTful API     │  • Elasticsearch   │  • Terraform │
│  • Redux Toolkit   │  • JWT Auth        │                    │  • ArgoCD    │
│  • Socket.io       │  • Swagger/OpenAPI │                    │  • Prometheus│
│  • React Query     │  • TypeORM/Prisma  │                    │  • Grafana   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4.2. Chi tiết công nghệ từng Service

### 4.2.1. KiteHub Platform Services

| Service | Framework | Database | Cache | Message Queue | Đặc biệt |
|---------|-----------|----------|-------|---------------|----------|
| **Sale Service** | NestJS + TypeScript | PostgreSQL | Redis | RabbitMQ | Payment Gateway Integration (VNPay, Momo, Stripe) |
| **Message Service** | NestJS + TypeScript | MongoDB | Redis | RabbitMQ | WebSocket (Socket.io), Push Notifications (Firebase) |
| **Maintaining Service** | NestJS + TypeScript | PostgreSQL | Redis | Apache Kafka | Prometheus Client, Kubernetes API |

### 4.2.2. KiteClass Core Services

| Service | Framework | Database | Cache | Message Queue | Đặc biệt |
|---------|-----------|----------|-------|---------------|----------|
| **Main Class Service** | NestJS + TypeScript | PostgreSQL | Redis | RabbitMQ | Full-text Search (Elasticsearch) |
| **User Service** | NestJS + TypeScript | PostgreSQL | Redis | RabbitMQ | OAuth2 (Passport.js), 2FA (Speakeasy) |
| **CMC Service** | NestJS + TypeScript | PostgreSQL + MongoDB | Redis | RabbitMQ | Report Generation (ExcelJS, PDFKit) |

### 4.2.3. Expand Services

| Service | Framework | Database | Cache | Message Queue | Đặc biệt |
|---------|-----------|----------|-------|---------------|----------|
| **Video Learning Service** | NestJS + TypeScript | PostgreSQL + MongoDB | Redis | RabbitMQ | FFmpeg, HLS Streaming, AWS S3 |
| **Streaming Service** | NestJS + TypeScript | MongoDB | Redis | Apache Kafka | WebRTC, MediaSoup, Jitsi Integration |
| **Forum Service** | NestJS + TypeScript | PostgreSQL | Redis + Elasticsearch | RabbitMQ | Full-text Search, Markdown Support |

## 4.3. Deployment Architecture

### 4.3.1. Infrastructure Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLOUD INFRASTRUCTURE                               │
│                              (AWS / GCP / Azure)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         KUBERNETES CLUSTER                              │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐     │ │
│  │  │   Namespace:     │  │   Namespace:     │  │   Namespace:     │     │ │
│  │  │   kitehub        │  │   kiteclass-1    │  │   kiteclass-n    │     │ │
│  │  │   ───────────    │  │   ───────────    │  │   ───────────    │     │ │
│  │  │  • sale-svc      │  │  • mainclass-svc │  │  • mainclass-svc │     │ │
│  │  │  • message-svc   │  │  • user-svc      │  │  • user-svc      │     │ │
│  │  │  • maintain-svc  │  │  • cmc-svc       │  │  • cmc-svc       │     │ │
│  │  │                  │  │  • expand-svcs   │  │  • expand-svcs   │     │ │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────┘     │ │
│  │                                                                        │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │ │
│  │  │                    SHARED SERVICES                               │  │ │
│  │  │  • Ingress Controller (NGINX)    • Cert Manager                 │  │ │
│  │  │  • Service Mesh (Istio/Linkerd)  • Secrets Manager              │  │ │
│  │  │  • Logging (ELK Stack)           • Monitoring (Prometheus)      │  │ │
│  │  └─────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                         MANAGED SERVICES                                 ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    ││
│  │  │ RDS/Cloud   │  │ ElastiCache │  │ S3/Cloud    │  │ CloudFront  │    ││
│  │  │ SQL         │  │ (Redis)     │  │ Storage     │  │ (CDN)       │    ││
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    ││
│  │  │ DocumentDB  │  │ MQ Service  │  │ CloudWatch  │  │ Route 53    │    ││
│  │  │ (MongoDB)   │  │ (RabbitMQ)  │  │ (Logs)      │  │ (DNS)       │    ││
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3.2. Deployment Configuration từng Service

| Service | Container Image | Resources (Min) | Resources (Max) | Replicas | Health Check |
|---------|-----------------|-----------------|-----------------|----------|--------------|
| **Sale Service** | `kiteclass/sale-svc:latest` | 256Mi / 0.25 CPU | 1Gi / 1 CPU | 2-5 | `/health` |
| **Message Service** | `kiteclass/message-svc:latest` | 512Mi / 0.5 CPU | 2Gi / 2 CPU | 3-10 | `/health` |
| **Maintaining Service** | `kiteclass/maintain-svc:latest` | 256Mi / 0.25 CPU | 1Gi / 1 CPU | 2-3 | `/health` |
| **Main Class Service** | `kiteclass/mainclass-svc:latest` | 256Mi / 0.25 CPU | 1Gi / 1 CPU | 2-5 | `/health` |
| **User Service** | `kiteclass/user-svc:latest` | 256Mi / 0.25 CPU | 1Gi / 1 CPU | 2-5 | `/health` |
| **CMC Service** | `kiteclass/cmc-svc:latest` | 256Mi / 0.25 CPU | 1Gi / 1 CPU | 2-5 | `/health` |
| **Video Learning Service** | `kiteclass/video-svc:latest` | 512Mi / 0.5 CPU | 4Gi / 4 CPU | 2-10 | `/health` |
| **Streaming Service** | `kiteclass/streaming-svc:latest` | 1Gi / 1 CPU | 8Gi / 8 CPU | 3-20 | `/health` |
| **Forum Service** | `kiteclass/forum-svc:latest` | 256Mi / 0.25 CPU | 1Gi / 1 CPU | 2-5 | `/health` |

### 4.3.3. Kubernetes Deployment Example (User Service)

```yaml
# user-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: kiteclass
  labels:
    app: user-service
    tier: backend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: kiteclass/user-svc:latest
        ports:
        - containerPort: 3000
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: postgres-url
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: cache-secrets
              key: redis-url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: auth-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: kiteclass
spec:
  selector:
    app: user-service
  ports:
  - port: 80
    targetPort: 3000
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
  namespace: kiteclass
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2
  maxReplicas: 5
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

## 4.4. CI/CD Pipeline

### 4.4.1. Pipeline Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CI/CD PIPELINE                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐  │
│   │  Code   │───▶│  Build  │───▶│  Test   │───▶│  Scan   │───▶│ Package │  │
│   │  Push   │    │         │    │         │    │         │    │         │  │
│   └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘  │
│       │              │              │              │              │         │
│       │         TypeScript     Unit Tests    SonarQube      Docker Build   │
│       │         Compilation    Integration   Security Scan  Push to ECR    │
│       │         Lint Check     E2E Tests     Dependency                    │
│       │                                      Check                          │
│       │                                                                     │
│       ▼                                                                     │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐                 │
│   │  Dev    │───▶│ Staging │───▶│   QA    │───▶│  Prod   │                 │
│   │ Deploy  │    │ Deploy  │    │ Testing │    │ Deploy  │                 │
│   └─────────┘    └─────────┘    └─────────┘    └─────────┘                 │
│       │              │              │              │                        │
│   Auto Deploy    Auto Deploy   Manual Test   Manual Approve                │
│   on PR Merge    on Dev Pass   QA Sign-off   Rolling Update                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.4.2. GitHub Actions Workflow Example

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Lint
        run: npm run lint

      - name: Type check
        run: npm run type-check

      - name: Unit tests
        run: npm run test:unit

      - name: Integration tests
        run: npm run test:integration

      - name: Build
        run: npm run build

  security-scan:
    needs: build-and-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run Snyk Security Scan
        uses: snyk/actions/node@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}

  docker-build:
    needs: [build-and-test, security-scan]
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    steps:
      - uses: actions/checkout@v4

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

  deploy-staging:
    needs: docker-build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: staging
    steps:
      - name: Deploy to Staging
        run: |
          kubectl set image deployment/user-service \
            user-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} \
            --namespace=staging

  deploy-production:
    needs: docker-build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production
    steps:
      - name: Deploy to Production
        run: |
          kubectl set image deployment/user-service \
            user-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} \
            --namespace=production
```

## 4.5. API Communication Pattern

### 4.5.1. RESTful API Standards

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          RESTFUL API STANDARDS                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  URL Pattern:  https://api.kiteclass.com/v1/{service}/{resource}/{id}       │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ HTTP Methods                                                             ││
│  │ ────────────                                                             ││
│  │ GET    /users          → List all users (with pagination)               ││
│  │ GET    /users/{id}     → Get single user                                ││
│  │ POST   /users          → Create new user                                ││
│  │ PUT    /users/{id}     → Update user (full)                             ││
│  │ PATCH  /users/{id}     → Update user (partial)                          ││
│  │ DELETE /users/{id}     → Delete user                                    ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ Response Format                                                          ││
│  │ ───────────────                                                          ││
│  │ {                                                                        ││
│  │   "success": true,                                                       ││
│  │   "data": { ... },                                                       ││
│  │   "meta": {                                                              ││
│  │     "page": 1,                                                           ││
│  │     "limit": 20,                                                         ││
│  │     "total": 100,                                                        ││
│  │     "totalPages": 5                                                      ││
│  │   }                                                                      ││
│  │ }                                                                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ Error Response                                                           ││
│  │ ──────────────                                                           ││
│  │ {                                                                        ││
│  │   "success": false,                                                      ││
│  │   "error": {                                                             ││
│  │     "code": "USER_NOT_FOUND",                                            ││
│  │     "message": "User with ID 123 not found",                             ││
│  │     "details": { ... }                                                   ││
│  │   }                                                                      ││
│  │ }                                                                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.5.2. Inter-Service Communication

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      INTER-SERVICE COMMUNICATION                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ Synchronous (RESTful HTTP)                                               ││
│  │ ──────────────────────────                                               ││
│  │                                                                          ││
│  │  ┌───────────┐     HTTP/REST      ┌───────────┐                         ││
│  │  │  CMC Svc  │ ──────────────────▶│ User Svc  │                         ││
│  │  └───────────┘  GET /users/{id}   └───────────┘                         ││
│  │                                                                          ││
│  │  Use case: Get user info for grading                                    ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ Asynchronous (Message Queue)                                             ││
│  │ ────────────────────────────                                             ││
│  │                                                                          ││
│  │  ┌───────────┐               ┌───────────┐               ┌───────────┐  ││
│  │  │ User Svc  │ ─ publish ──▶ │ RabbitMQ  │ ─ consume ──▶ │Message Svc│  ││
│  │  └───────────┘ user.created  └───────────┘               └───────────┘  ││
│  │                                                                          ││
│  │  Use case: Send welcome email when user registers                       ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ Event-Driven (Apache Kafka)                                              ││
│  │ ───────────────────────────                                              ││
│  │                                                                          ││
│  │  ┌───────────┐               ┌───────────┐               ┌───────────┐  ││
│  │  │Streaming  │ ─ publish ──▶ │   Kafka   │ ─ subscribe ─▶│Maintaining│  ││
│  │  │   Svc     │ stream.ended  └───────────┘               │   Svc     │  ││
│  │  └───────────┘                     │                     └───────────┘  ││
│  │                                    └──────── subscribe ─▶┌───────────┐  ││
│  │                                                          │ Video Svc │  ││
│  │                                                          └───────────┘  ││
│  │  Use case: Process recording after stream ends                          ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 5: TỔNG KẾT

## 5.1. Tổng hợp Services

| Nhóm | Service | Use Cases | Tech Stack | Deployment |
|------|---------|-----------|------------|------------|
| **KiteHub** | Sale Service | 12 | NestJS, PostgreSQL, Redis | K8s, 2-5 replicas |
| **KiteHub** | Message Service | 10 | NestJS, MongoDB, Socket.io | K8s, 3-10 replicas |
| **KiteHub** | Maintaining Service | 13 | NestJS, PostgreSQL, Kafka | K8s, 2-3 replicas |
| **KiteClass** | Main Class Service | 14 | NestJS, PostgreSQL, ES | K8s, 2-5 replicas |
| **KiteClass** | User Service | 14 | NestJS, PostgreSQL, OAuth2 | K8s, 2-5 replicas |
| **KiteClass** | CMC Service | 16 | NestJS, PostgreSQL, MongoDB | K8s, 2-5 replicas |
| **Expand** | Video Learning Service | 11 | NestJS, FFmpeg, S3 | K8s, 2-10 replicas |
| **Expand** | Streaming Service | 13 | NestJS, WebRTC, MediaSoup | K8s, 3-20 replicas |
| **Expand** | Forum Service | 12 | NestJS, PostgreSQL, ES | K8s, 2-5 replicas |

## 5.2. Tổng số Use Cases

- **KiteHub Platform**: 35 Use Cases
- **KiteClass Core**: 44 Use Cases
- **Expand Services**: 36 Use Cases
- **Tổng cộng**: **115 Use Cases**

## 5.3. Kết luận

Hệ thống KiteClass Platform được thiết kế theo kiến trúc Microservices với 9 services chính, cung cấp 115 use cases phục vụ đầy đủ nhu cầu của các tổ chức giáo dục từ việc quản lý lớp học, người dùng đến vận hành và kinh doanh. Với việc sử dụng các công nghệ hiện đại như NestJS, Kubernetes, và RESTful API, hệ thống đảm bảo tính linh hoạt, khả năng mở rộng và dễ bảo trì.

---

**Tài liệu được tạo bởi:** KiteClass Development Team
**Ngày cập nhật:** 16/12/2025
**Phiên bản:** 1.0
