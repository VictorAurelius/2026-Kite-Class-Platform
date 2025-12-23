# BÁO CÁO USE CASE THEO SERVICE - KITECLASS PLATFORM V3
## Chi tiết chức năng từng service

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Tên dự án** | KiteClass Platform V3 |
| **Phiên bản** | 3.0 (Final) |
| **Ngày tạo** | 23/12/2025 |
| **Loại tài liệu** | Use Case Specification |
| **Tham chiếu** | system-architecture-v3-final.md |

---

# MỤC LỤC

1. [KITEHUB - Sale Module](#phần-1-kitehub---sale-module)
2. [KITEHUB - Message Module](#phần-2-kitehub---message-module)
3. [KITEHUB - Maintaining Module](#phần-3-kitehub---maintaining-module)
4. [KITEHUB - AI Agent Module](#phần-4-kitehub---ai-agent-module)
5. [KITECLASS - Core Service](#phần-5-kiteclass---core-service)
6. [KITECLASS - User Service](#phần-6-kiteclass---user-service)
7. [KITECLASS - Media Service](#phần-7-kiteclass---media-service)
8. [KITECLASS - Frontend](#phần-8-kiteclass---frontend)

---

# PHẦN 1: KITEHUB - SALE MODULE

## 1.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Quản lý bán hàng, landing page, đơn hàng, thanh toán |
| **Công nghệ** | NestJS (trong KiteHub Monolith) |
| **Database** | PostgreSQL - Schema: sales |
| **Actors** | Customer, Admin |

## 1.2. Use Cases

### UC-SALE-01: Xem trang giới thiệu sản phẩm

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-SALE-01: Xem trang giới thiệu sản phẩm                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Customer (Khách hàng tiềm năng)                                          │
│ Precondition: Không                                                             │
│ Trigger: Khách hàng truy cập website                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Khách hàng truy cập https://kiteclass.com                                   │
│ 2. Hệ thống hiển thị landing page với:                                         │
│    - Hero section giới thiệu platform                                          │
│    - Danh sách tính năng nổi bật                                               │
│    - Testimonials từ khách hàng                                                │
│    - Các gói sản phẩm (BASIC, STANDARD, PREMIUM)                               │
│    - Form đăng ký dùng thử / liên hệ                                           │
│ 3. Khách hàng có thể điều hướng đến các trang:                                 │
│    - /pricing: Chi tiết bảng giá                                               │
│    - /features: Chi tiết tính năng                                             │
│    - /demo: Xem demo                                                           │
│    - /contact: Liên hệ                                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Khách hàng có thông tin về sản phẩm                              │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-SALE-02: Đăng ký mua gói dịch vụ

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-SALE-02: Đăng ký mua gói dịch vụ                                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Customer                                                                 │
│ Precondition: Khách hàng đã xem thông tin sản phẩm                              │
│ Trigger: Khách hàng click "Đăng ký" trên gói dịch vụ                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Khách hàng chọn gói dịch vụ (BASIC/STANDARD/PREMIUM)                        │
│ 2. Hệ thống hiển thị form đăng ký:                                             │
│    - Tên tổ chức (*)                                                           │
│    - Email quản trị (*)                                                        │
│    - Subdomain mong muốn (*) - VD: acme.kiteclass.com                          │
│    - Số điện thoại                                                             │
│    - Ngành nghề                                                                │
│    - Upload ảnh/logo (cho AI Marketing)                                        │
│    - Slogan (optional)                                                         │
│ 3. Khách hàng điền thông tin và submit                                         │
│ 4. Hệ thống validate:                                                          │
│    - Email hợp lệ, chưa đăng ký                                                │
│    - Subdomain khả dụng                                                        │
│    - Ảnh đúng định dạng (nếu có)                                               │
│ 5. Hệ thống tạo đơn hàng với status = PENDING                                  │
│ 6. Redirect khách hàng đến trang thanh toán                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 4a. Subdomain đã tồn tại:                                                      │
│     - Hệ thống gợi ý subdomain khác                                            │
│     - Khách hàng chọn hoặc nhập mới                                            │
│ 4b. Email đã đăng ký:                                                          │
│     - Thông báo tài khoản đã tồn tại                                           │
│     - Gợi ý đăng nhập hoặc khôi phục mật khẩu                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Đơn hàng được tạo, chờ thanh toán                                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-SALE-03: Thanh toán đơn hàng

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-SALE-03: Thanh toán đơn hàng                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Customer                                                                 │
│ Precondition: Đã có đơn hàng với status = PENDING                               │
│ Trigger: Khách hàng ở trang thanh toán                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Hệ thống hiển thị thông tin đơn hàng:                                       │
│    - Gói dịch vụ đã chọn                                                       │
│    - Subdomain: acme.kiteclass.com                                             │
│    - Số tiền: 1,500,000 VND/tháng                                              │
│    - Chu kỳ thanh toán: Hàng tháng / Hàng năm                                  │
│ 2. Khách hàng chọn phương thức thanh toán:                                     │
│    - Chuyển khoản ngân hàng                                                    │
│    - Ví điện tử (MoMo, ZaloPay)                                                │
│    - Thẻ quốc tế (Visa/Mastercard)                                             │
│ 3. Khách hàng xác nhận thanh toán                                              │
│ 4. Hệ thống xử lý thanh toán                                                   │
│ 5. Thanh toán thành công:                                                      │
│    - Cập nhật order status = PAID                                              │
│    - Gửi email xác nhận                                                        │
│    - Trigger provisioning process                                               │
│ 6. Redirect đến trang "Đơn hàng thành công"                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 5a. Thanh toán thất bại:                                                       │
│     - Hiển thị thông báo lỗi                                                   │
│     - Cho phép thử lại hoặc đổi phương thức                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Đơn hàng đã thanh toán, bắt đầu provisioning                     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-SALE-04: Quản lý đơn hàng (Admin)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-SALE-04: Quản lý đơn hàng (Admin)                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Admin (KiteHub)                                                          │
│ Precondition: Admin đã đăng nhập                                                │
│ Trigger: Admin truy cập trang quản lý đơn hàng                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Admin truy cập /admin/orders                                                │
│ 2. Hệ thống hiển thị danh sách đơn hàng với filter:                            │
│    - Theo status (PENDING, PAID, PROVISIONING, ACTIVE, CANCELLED)              │
│    - Theo thời gian                                                            │
│    - Theo gói dịch vụ                                                          │
│ 3. Admin có thể:                                                               │
│    a. Xem chi tiết đơn hàng                                                    │
│    b. Xác nhận thanh toán thủ công (chuyển khoản)                              │
│    c. Hủy đơn hàng                                                             │
│    d. Xem tiến trình provisioning                                              │
│    e. Liên hệ khách hàng                                                       │
│ 4. Với mỗi action, hệ thống ghi log                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Admin đã quản lý đơn hàng                                        │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 2: KITEHUB - MESSAGE MODULE

## 2.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Hệ thống chat hỗ trợ khách hàng, thông báo |
| **Công nghệ** | NestJS + WebSocket (Socket.io) |
| **Database** | PostgreSQL - Schema: messaging |
| **Actors** | Customer, Agent (nhân viên hỗ trợ), System |

## 2.2. Use Cases

### UC-MSG-01: Khách hàng chat với bộ phận hỗ trợ

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MSG-01: Khách hàng chat với bộ phận hỗ trợ                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Customer                                                                 │
│ Precondition: Khách hàng đang ở website                                         │
│ Trigger: Khách hàng click vào widget chat                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Khách hàng click icon chat góc phải màn hình                                │
│ 2. Hệ thống hiển thị chat widget với:                                          │
│    - Lời chào tự động                                                          │
│    - Form nhập email (nếu chưa đăng nhập)                                      │
│ 3. Khách hàng nhập tin nhắn                                                    │
│ 4. Hệ thống:                                                                   │
│    a. Nếu có Agent online → Chuyển đến Agent                                   │
│    b. Nếu không có Agent → Chatbot AI trả lời FAQ                              │
│ 5. Agent/Chatbot trả lời                                                       │
│ 6. Cuộc hội thoại tiếp tục real-time                                           │
│ 7. Khi kết thúc, hệ thống:                                                     │
│    - Lưu lịch sử chat                                                          │
│    - Gửi transcript qua email (nếu có)                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Khách hàng đã được hỗ trợ                                        │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-MSG-02: Agent xử lý chat

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MSG-02: Agent xử lý chat                                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Agent (Nhân viên hỗ trợ)                                                 │
│ Precondition: Agent đã đăng nhập, status = Available                            │
│ Trigger: Có cuộc chat mới được assign                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Agent nhận thông báo có chat mới                                            │
│ 2. Agent mở cuộc chat                                                          │
│ 3. Hệ thống hiển thị:                                                          │
│    - Thông tin khách hàng                                                      │
│    - Lịch sử chat trước đó (nếu có)                                            │
│    - Context từ trang khách đang xem                                           │
│ 4. Agent trả lời khách hàng                                                    │
│ 5. Agent có thể:                                                               │
│    a. Gửi file đính kèm                                                        │
│    b. Gửi canned responses (mẫu câu trả lời)                                   │
│    c. Transfer sang Agent khác                                                 │
│    d. Đánh tag cuộc chat                                                       │
│ 6. Khi hoàn thành, Agent close cuộc chat                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Cuộc chat được xử lý, lưu lịch sử                                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-MSG-03: Gửi thông báo hệ thống

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MSG-03: Gửi thông báo hệ thống                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: System (Automated)                                                       │
│ Precondition: Có event cần thông báo                                            │
│ Trigger: Order status change, Instance ready, etc.                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. System event được trigger                                                   │
│ 2. Message Module nhận event từ queue                                          │
│ 3. Xác định người nhận và kênh gửi:                                            │
│    - Email                                                                     │
│    - Push notification                                                         │
│    - In-app notification                                                       │
│ 4. Render template notification với data                                       │
│ 5. Gửi qua các kênh đã xác định                                               │
│ 6. Log kết quả gửi                                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Ví dụ notifications:                                                           │
│ - "Đơn hàng của bạn đã được xác nhận"                                          │
│ - "Instance acme.kiteclass.com đã sẵn sàng sử dụng"                            │
│ - "Hóa đơn tháng 12 đã được tạo"                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Thông báo đã được gửi                                            │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 3: KITEHUB - MAINTAINING MODULE

## 3.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Provisioning, quản lý và giám sát các KiteClass instances |
| **Công nghệ** | NestJS + Kubernetes Client |
| **Database** | PostgreSQL - Schema: instances |
| **Actors** | System, Admin |

## 3.2. Use Cases

### UC-MAINT-01: Tự động provisioning instance mới

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MAINT-01: Tự động provisioning instance mới                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: System (Triggered by Sale Module)                                        │
│ Precondition: Order status = PAID, AI assets đã generate xong                   │
│ Trigger: Event ORDER_PAID từ queue                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Maintaining Module nhận event ORDER_PAID                                    │
│ 2. Tạo instance record với status = PROVISIONING                               │
│ 3. Tạo Kubernetes namespace: kc-{subdomain}                                    │
│ 4. Deploy các resources:                                                       │
│    a. PostgreSQL database (hoặc schema trong shared DB)                        │
│    b. Redis cache                                                              │
│    c. Core Service deployment                                                  │
│    d. User Service deployment                                                  │
│    e. Media Service deployment                                                 │
│    f. Frontend deployment                                                      │
│    g. API Gateway (Ingress)                                                    │
│ 5. Chờ tất cả pods healthy (max 10 phút)                                       │
│ 6. Chạy database migrations                                                    │
│ 7. Seed initial data:                                                          │
│    - Admin account                                                             │
│    - Default roles & permissions                                               │
│    - AI-generated assets từ AI Agent Module                                    │
│ 8. Verify services healthy (health checks)                                     │
│ 9. Cập nhật instance status = ACTIVE                                           │
│ 10. Publish event INSTANCE_READY                                               │
│ 11. Message Module gửi email thông báo cho customer                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 5a. Provisioning timeout:                                                      │
│     - Set status = FAILED                                                      │
│     - Rollback resources                                                       │
│     - Alert Admin                                                              │
│     - Thông báo lỗi cho customer                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Instance sẵn sàng sử dụng tại {subdomain}.kiteclass.com          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-MAINT-02: Monitor instance health

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MAINT-02: Monitor instance health                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: System (Scheduled Job)                                                   │
│ Precondition: Instances đang hoạt động                                          │
│ Trigger: Cron job chạy mỗi 1 phút                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Job lấy danh sách instances với status = ACTIVE                             │
│ 2. Với mỗi instance:                                                           │
│    a. Check Kubernetes pod status                                              │
│    b. Call health endpoint của từng service                                    │
│    c. Thu thập metrics:                                                        │
│       - CPU usage                                                              │
│       - Memory usage                                                           │
│       - Disk usage                                                             │
│       - Request latency                                                        │
│       - Error rate                                                             │
│    d. Lưu metrics vào database                                                 │
│ 3. Nếu phát hiện issue:                                                        │
│    a. Pod crashed → Trigger auto-restart                                       │
│    b. High resource usage → Alert Admin                                        │
│    c. Service unhealthy → Alert và xem xét restart                             │
│ 4. Cập nhật dashboard monitoring                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Metrics được thu thập, issues được phát hiện                     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-MAINT-03: Suspend/Resume instance

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MAINT-03: Suspend/Resume instance                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Admin (hoặc System khi quá hạn thanh toán)                               │
│ Precondition: Instance đang ACTIVE hoặc SUSPENDED                               │
│ Trigger: Admin action hoặc payment overdue                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow (Suspend):                                                            │
│ 1. Admin chọn instance cần suspend                                             │
│ 2. Hệ thống xác nhận action                                                    │
│ 3. Thực hiện:                                                                  │
│    a. Scale deployments to 0 replicas                                          │
│    b. Giữ nguyên database và data                                              │
│    c. Update instance status = SUSPENDED                                       │
│ 4. Thông báo cho customer                                                      │
│                                                                                 │
│ Main Flow (Resume):                                                             │
│ 1. Admin chọn instance cần resume                                              │
│ 2. Hệ thống xác nhận payment status                                            │
│ 3. Thực hiện:                                                                  │
│    a. Scale deployments back to normal                                         │
│    b. Wait for pods healthy                                                    │
│    c. Update instance status = ACTIVE                                          │
│ 4. Thông báo cho customer                                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Instance đã được suspend/resume                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 4: KITEHUB - AI AGENT MODULE

## 4.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Tự động tạo marketing assets từ ảnh khách hàng |
| **Công nghệ** | NestJS + External AI APIs (OpenAI, Stability, Remove.bg) |
| **Database** | PostgreSQL - Schema: ai_assets |
| **Actors** | System (triggered by order) |

## 4.2. Use Cases

### UC-AI-01: Generate marketing assets từ ảnh

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-AI-01: Generate marketing assets từ ảnh                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: System (Triggered after order paid)                                      │
│ Precondition: Order có upload ảnh                                               │
│ Trigger: Event ORDER_PAID                                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Nhận event ORDER_PAID với order_id                                          │
│ 2. Lấy thông tin đơn hàng:                                                     │
│    - Original image URL                                                        │
│    - Organization name                                                         │
│    - Slogan (nếu có)                                                           │
│    - Brand colors (nếu có)                                                     │
│ 3. Tạo generation job với status = QUEUED                                      │
│ 4. Process image:                                                              │
│    a. Download original image                                                  │
│    b. Call Remove.bg API → Background removed image                            │
│    c. Extract dominant colors (nếu chưa có brand colors)                       │
│    d. Generate variations: circle, square crops                                │
│ 5. Generate text copy (GPT-4):                                                 │
│    - Hero headline                                                             │
│    - Sub-headline                                                              │
│    - CTA text                                                                  │
│    - Section headlines                                                         │
│ 6. Generate images (Stable Diffusion SDXL):                                    │
│    a. Hero banner (1920x600)                                                   │
│    b. About banner (1200x400)                                                  │
│    c. Courses banner (1200x400)                                                │
│    d. Contact banner (1200x400)                                                │
│ 7. Generate logo variants:                                                     │
│    - Primary logo (with org name)                                              │
│    - Secondary logo (alt colors)                                               │
│    - Icon only                                                                 │
│ 8. Generate OG image (1200x630)                                                │
│ 9. Upload all assets to CDN (S3/R2)                                            │
│ 10. Save asset URLs to database                                                │
│ 11. Update job status = COMPLETED                                              │
│ 12. Publish event AI_ASSETS_READY                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 4b. Remove.bg API fails:                                                       │
│     - Retry up to 3 times                                                      │
│     - If still fails, use fallback: auto-crop without background removal       │
│                                                                                 │
│ 6a. SDXL generation fails:                                                     │
│     - Retry with different seed                                                │
│     - If still fails, use template-based fallback                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Output Assets:                                                                  │
│ - 3 profile variations (circle, square, original cutout)                       │
│ - 1 hero banner                                                                │
│ - 3 section banners                                                            │
│ - 3 logo variants                                                              │
│ - 1 OG image                                                                   │
│ - Marketing copy JSON                                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Assets sẵn sàng cho provisioning                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 5: KITECLASS - CORE SERVICE

## 5.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Service chính quản lý nghiệp vụ giáo dục |
| **Công nghệ** | Java Spring Boot |
| **Database** | PostgreSQL (instance-specific) |
| **Modules** | Class, Learning, Billing, Gamification, Parent, Forum |
| **Actors** | CENTER_OWNER, CENTER_ADMIN, TEACHER, STUDENT, PARENT |

## 5.2. CLASS MODULE - Use Cases

### UC-CLASS-01: Tạo khóa học mới

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-CLASS-01: Tạo khóa học mới                                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: CENTER_OWNER, CENTER_ADMIN                                               │
│ Precondition: User đã đăng nhập với quyền course:create                         │
│ Trigger: User chọn "Tạo khóa học mới"                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. User truy cập /admin/courses/new                                            │
│ 2. Hệ thống hiển thị form:                                                     │
│    - Tên khóa học (*)                                                          │
│    - Mô tả                                                                     │
│    - Danh mục                                                                  │
│    - Ảnh thumbnail                                                             │
│    - Học phí đề xuất                                                           │
│    - Số buổi dự kiến                                                           │
│ 3. User điền thông tin và submit                                               │
│ 4. Hệ thống validate và lưu khóa học                                           │
│ 5. Redirect đến trang chi tiết khóa học                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Khóa học được tạo, sẵn sàng mở lớp                               │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-CLASS-02: Tạo lớp học từ khóa học

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-CLASS-02: Tạo lớp học từ khóa học                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: CENTER_OWNER, CENTER_ADMIN                                               │
│ Precondition: Có khóa học, có giáo viên                                         │
│ Trigger: User chọn "Mở lớp mới"                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. User chọn khóa học muốn mở lớp                                              │
│ 2. Hệ thống hiển thị form:                                                     │
│    - Tên lớp (VD: Toán 10A - Khóa 1/2025)                                      │
│    - Giáo viên phụ trách (*)                                                   │
│    - Ngày bắt đầu (*)                                                          │
│    - Lịch học (chọn thứ + giờ)                                                 │
│    - Phòng học                                                                 │
│    - Sĩ số tối đa                                                              │
│    - Học phí lớp (kế thừa từ khóa học hoặc custom)                             │
│    - Cách tính phí: FIXED (cố định) / PER_SESSION (theo buổi)                  │
│ 3. User điền thông tin và submit                                               │
│ 4. Hệ thống:                                                                   │
│    a. Validate không trùng lịch giáo viên                                      │
│    b. Validate không trùng lịch phòng                                          │
│    c. Tạo lớp học                                                              │
│    d. Auto-generate lịch học theo recurring                                    │
│    e. Tạo config học phí cho lớp                                               │
│ 5. Thông báo thành công, redirect đến trang lớp                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 4a. Trùng lịch giáo viên:                                                      │
│     - Hiển thị cảnh báo                                                        │
│     - Gợi ý lịch trống của giáo viên                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Lớp học được tạo, có lịch học, sẵn sàng nhận học viên            │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-CLASS-03: Thêm học viên vào lớp

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-CLASS-03: Thêm học viên vào lớp                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: CENTER_OWNER, CENTER_ADMIN                                               │
│ Precondition: Lớp học đã tạo, học viên đã có trong hệ thống                     │
│ Trigger: User chọn "Thêm học viên"                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. User truy cập trang lớp học > Tab "Học viên"                                │
│ 2. User click "Thêm học viên"                                                  │
│ 3. Hệ thống hiển thị:                                                          │
│    - Search box tìm học viên                                                   │
│    - Danh sách học viên chưa thuộc lớp nào                                     │
│ 4. User tìm và chọn học viên                                                   │
│ 5. User xác nhận thêm                                                          │
│ 6. Hệ thống:                                                                   │
│    a. Kiểm tra sĩ số chưa đạt max                                              │
│    b. Thêm học viên vào lớp                                                    │
│    c. Tự động tạo enrollment record                                            │
│    d. Khởi tạo điểm tích lũy cho học viên (Gamification)                       │
│    e. Tạo liên kết để phụ huynh đăng ký (nếu chưa có)                          │
│ 7. Cập nhật danh sách lớp                                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Học viên thuộc lớp, có thể điểm danh                             │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.3. LEARNING MODULE - Use Cases

### UC-LEARN-01: Điểm danh buổi học

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-LEARN-01: Điểm danh buổi học                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: TEACHER, CENTER_ADMIN                                                    │
│ Precondition: Có buổi học trong ngày, giáo viên được phân công                  │
│ Trigger: Giáo viên mở trang điểm danh                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Giáo viên truy cập /teacher/attendance                                      │
│ 2. Hệ thống hiển thị danh sách lớp hôm nay                                     │
│ 3. Giáo viên chọn lớp cần điểm danh                                            │
│ 4. Hệ thống hiển thị:                                                          │
│    - Thông tin buổi học (ngày, giờ, phòng)                                     │
│    - Danh sách học viên với checkbox                                           │
│    - Trạng thái: Có mặt / Vắng có phép / Vắng không phép                       │
│ 5. Giáo viên đánh dấu từng học viên                                            │
│ 6. Giáo viên có thể thêm ghi chú cho từng học viên                             │
│ 7. Giáo viên submit điểm danh                                                  │
│ 8. Hệ thống:                                                                   │
│    a. Lưu attendance records                                                   │
│    b. Trigger Gamification: +10 điểm cho học viên có mặt                       │
│    c. Trigger Notification: Gửi thông báo cho phụ huynh (vắng)                 │
│    d. Cập nhật thống kê điểm danh                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Buổi học đã điểm danh, phụ huynh được thông báo                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-LEARN-02: Nhập điểm kiểm tra

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-LEARN-02: Nhập điểm kiểm tra                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: TEACHER                                                                  │
│ Precondition: Có bài kiểm tra đã tạo                                            │
│ Trigger: Giáo viên muốn nhập điểm                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Giáo viên truy cập /teacher/grades                                          │
│ 2. Chọn lớp và bài kiểm tra                                                    │
│ 3. Hệ thống hiển thị:                                                          │
│    - Thông tin bài kiểm tra (tên, ngày, hệ số)                                 │
│    - Bảng nhập điểm với danh sách học viên                                     │
│ 4. Giáo viên nhập điểm cho từng học viên (0-10)                                │
│ 5. Giáo viên có thể thêm nhận xét                                              │
│ 6. Giáo viên submit                                                            │
│ 7. Hệ thống:                                                                   │
│    a. Validate điểm hợp lệ                                                     │
│    b. Lưu điểm và tính điểm trung bình                                         │
│    c. Trigger Gamification:                                                    │
│       - Điểm >= 8: +30 điểm                                                    │
│       - Điểm = 10: +50 điểm (bonus)                                            │
│    d. Trigger Notification: Gửi cho phụ huynh                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Điểm đã nhập, phụ huynh được thông báo                           │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-LEARN-03: Giao và chấm bài tập

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-LEARN-03: Giao và chấm bài tập                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: TEACHER (giao), STUDENT (nộp), TEACHER (chấm)                            │
│ Precondition: Lớp học đang hoạt động                                            │
│ Trigger: Giáo viên muốn giao bài tập                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│                                                                                 │
│ [Giao bài tập - TEACHER]                                                       │
│ 1. Giáo viên tạo bài tập mới:                                                  │
│    - Tiêu đề                                                                   │
│    - Mô tả / hướng dẫn                                                         │
│    - File đính kèm (nếu có)                                                    │
│    - Deadline nộp bài                                                          │
│ 2. Hệ thống tạo bài tập và thông báo học viên                                  │
│                                                                                 │
│ [Nộp bài - STUDENT]                                                            │
│ 3. Học viên xem bài tập trong /student/assignments                             │
│ 4. Học viên làm bài và upload file                                             │
│ 5. Học viên submit trước deadline                                              │
│ 6. Hệ thống ghi nhận submission                                                │
│                                                                                 │
│ [Chấm bài - TEACHER]                                                           │
│ 7. Giáo viên xem danh sách submissions                                         │
│ 8. Với mỗi bài:                                                                │
│    - Review nội dung                                                           │
│    - Chấm điểm (0-10)                                                          │
│    - Viết nhận xét                                                             │
│ 9. Hệ thống:                                                                   │
│    - Lưu điểm và feedback                                                      │
│    - Trigger Gamification: +20 điểm (nộp đúng hạn)                             │
│    - Thông báo học viên và phụ huynh                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Bài tập đã chấm, kết quả được thông báo                          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.4. BILLING MODULE - Use Cases

### UC-BILL-01: Tạo hóa đơn học phí tự động

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-BILL-01: Tạo hóa đơn học phí tự động                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: System (Scheduled Job)                                                   │
│ Precondition: Cuối tháng                                                        │
│ Trigger: Cron job ngày cuối mỗi tháng                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Job chạy lúc 00:00 ngày cuối tháng                                          │
│ 2. Lấy danh sách lớp học đang hoạt động                                        │
│ 3. Với mỗi lớp:                                                                │
│    a. Lấy config học phí (FIXED / PER_SESSION)                                 │
│    b. Lấy danh sách học viên                                                   │
│    c. Với mỗi học viên:                                                        │
│       - Tính số buổi trong tháng                                               │
│       - Tính số buổi đã đi học (từ attendance)                                 │
│       - Tính học phí:                                                          │
│         + FIXED: Học phí cố định                                               │
│         + PER_SESSION: Số buổi đi × Đơn giá                                    │
│       - Áp dụng giảm giá (nếu có)                                              │
│       - Tạo invoice record                                                     │
│       - Generate QR Code VietQR                                                │
│ 4. Gửi thông báo cho phụ huynh (batch)                                         │
│ 5. Log kết quả                                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Hóa đơn tháng đã tạo, phụ huynh được thông báo                   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-BILL-02: Xem và thanh toán hóa đơn (Phụ huynh)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-BILL-02: Xem và thanh toán hóa đơn (Phụ huynh)                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: PARENT                                                                   │
│ Precondition: Phụ huynh đã đăng nhập, có hóa đơn chưa thanh toán                │
│ Trigger: Phụ huynh truy cập trang học phí                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Phụ huynh truy cập /parent/billing                                          │
│ 2. Hệ thống hiển thị:                                                          │
│    - Danh sách hóa đơn theo con                                                │
│    - Filter: Tất cả / Chưa thanh toán / Đã thanh toán                          │
│ 3. Phụ huynh click vào hóa đơn cần xem                                         │
│ 4. Hệ thống hiển thị chi tiết:                                                 │
│    - Thông tin học viên, lớp học                                               │
│    - Chi tiết tính phí (số buổi, đơn giá, giảm giá)                            │
│    - Tổng tiền cần đóng                                                        │
│    - QR Code thanh toán                                                        │
│    - Thông tin chuyển khoản                                                    │
│ 5. Phụ huynh quét QR bằng app ngân hàng                                        │
│ 6. Phụ huynh chuyển khoản                                                      │
│ 7. Sau khi đối soát, hóa đơn chuyển status = PAID                              │
│ 8. Hệ thống gửi xác nhận                                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 5a. Thanh toán tiền mặt:                                                       │
│     - Phụ huynh đóng tại trung tâm                                             │
│     - Nhân viên cập nhật thanh toán thủ công                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Hóa đơn đã thanh toán                                            │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-BILL-03: Thu tiền và đối soát thủ công

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-BILL-03: Thu tiền và đối soát thủ công                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: CENTER_ADMIN                                                             │
│ Precondition: Có hóa đơn chưa thanh toán                                        │
│ Trigger: Phụ huynh đóng tiền mặt hoặc chuyển khoản                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Admin truy cập /admin/billing                                               │
│ 2. Tìm hóa đơn theo học viên hoặc mã hóa đơn                                   │
│ 3. Click "Thu tiền"                                                            │
│ 4. Nhập thông tin:                                                             │
│    - Số tiền thu                                                               │
│    - Phương thức (tiền mặt / chuyển khoản)                                     │
│    - Mã giao dịch (nếu chuyển khoản)                                           │
│    - Ghi chú                                                                   │
│ 5. Admin xác nhận                                                              │
│ 6. Hệ thống:                                                                   │
│    a. Tạo payment record                                                       │
│    b. Cập nhật invoice (paid_amount, status)                                   │
│    c. Cập nhật công nợ học viên                                                │
│    d. Gửi xác nhận cho phụ huynh                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 4a. Thanh toán một phần:                                                       │
│     - Nhập số tiền < tổng hóa đơn                                              │
│     - Status chuyển thành PARTIAL                                              │
│     - Còn nợ = Tổng - Đã đóng                                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Thanh toán được ghi nhận                                         │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.5. GAMIFICATION MODULE - Use Cases

### UC-GAME-01: Tích điểm tự động

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-GAME-01: Tích điểm tự động                                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: System (Event-driven)                                                    │
│ Precondition: Có event học tập phát sinh                                        │
│ Trigger: Events từ Learning Module                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Learning Module publish event (VD: ATTENDANCE_MARKED)                       │
│ 2. Gamification Module nhận event                                              │
│ 3. Xác định loại action và tra cứu point_rules                                 │
│ 4. Tính điểm:                                                                  │
│    - Base points từ rule                                                       │
│    - Check bonus conditions                                                    │
│ 5. Cập nhật student_points                                                     │
│ 6. Insert point_history                                                        │
│ 7. Check badge achievements:                                                   │
│    - Nếu đạt điều kiện badge mới → Award badge                                 │
│ 8. Notify student về điểm vừa nhận                                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Ví dụ Events:                                                                   │
│ - ATTENDANCE_MARKED: +10 pts (có mặt), +15 pts (đúng giờ)                      │
│ - ASSIGNMENT_SUBMITTED: +20 pts (đúng hạn)                                     │
│ - TEST_GRADED: +30 pts (>=8), +50 pts (=10)                                    │
│ - VIDEO_COMPLETED: +15 pts                                                     │
│ - STREAK_7_DAYS: +100 pts bonus                                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Điểm học viên được cập nhật                                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-GAME-02: Đổi điểm lấy quà

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-GAME-02: Đổi điểm lấy quà                                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: STUDENT                                                                  │
│ Precondition: Học viên có điểm khả dụng                                         │
│ Trigger: Học viên truy cập Reward Store                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Học viên truy cập /student/rewards                                          │
│ 2. Hệ thống hiển thị:                                                          │
│    - Điểm hiện có: 1,500 pts                                                   │
│    - Danh sách quà có thể đổi                                                  │
│ 3. Học viên chọn quà muốn đổi                                                  │
│ 4. Hệ thống kiểm tra:                                                          │
│    - Đủ điểm?                                                                  │
│    - Quà còn số lượng?                                                         │
│ 5. Học viên xác nhận đổi                                                       │
│ 6. Hệ thống:                                                                   │
│    a. Trừ điểm từ available_points                                             │
│    b. Cộng vào used_points                                                     │
│    c. Tạo reward_exchange record (status = PENDING)                            │
│    d. Giảm quantity của reward                                                 │
│    e. Notify Admin để duyệt                                                    │
│ 7. Hiển thị mã đổi quà cho học viên                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Tiếp theo (Admin duyệt):                                                       │
│ 8. Admin xem danh sách exchange requests                                       │
│ 9. Admin duyệt và đánh dấu đã phát quà                                         │
│ 10. Status chuyển thành DELIVERED                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Điểm đã trừ, quà đã đổi                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-GAME-03: Xem bảng xếp hạng

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-GAME-03: Xem bảng xếp hạng                                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: STUDENT, TEACHER, PARENT                                                 │
│ Precondition: Không                                                             │
│ Trigger: User truy cập leaderboard                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. User truy cập /leaderboard                                                  │
│ 2. Chọn phạm vi:                                                               │
│    - Theo lớp (chỉ lớp của mình)                                               │
│    - Toàn trường                                                               │
│ 3. Chọn thời gian:                                                             │
│    - Tuần này                                                                  │
│    - Tháng này                                                                 │
│    - Tất cả thời gian                                                          │
│ 4. Hệ thống hiển thị:                                                          │
│    - Top 10 học viên (rank, tên, điểm, thành tích nổi bật)                     │
│    - Vị trí của bản thân (nếu là học viên)                                     │
│ 5. Click vào học viên để xem profile công khai                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: User xem được bảng xếp hạng                                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.6. PARENT MODULE - Use Cases

### UC-PARENT-01: Phụ huynh tự đăng ký qua Zalo OTP

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-PARENT-01: Phụ huynh tự đăng ký qua Zalo OTP                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: PARENT (chưa có tài khoản)                                               │
│ Precondition: Có link đăng ký từ trung tâm                                      │
│ Trigger: Phụ huynh mở link đăng ký                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Admin/Teacher tạo link đăng ký cho học viên                                 │
│    Link: https://acme.kiteclass.com/parent/register?student=ABC123             │
│ 2. Gửi link cho phụ huynh (Zalo, SMS, email)                                   │
│ 3. Phụ huynh mở link                                                           │
│ 4. Hệ thống hiển thị thông tin học viên và form:                               │
│    - Họ tên phụ huynh (*)                                                      │
│    - Số điện thoại Zalo (*)                                                    │
│    - Quan hệ (Bố/Mẹ/Người giám hộ)                                             │
│ 5. Phụ huynh nhập thông tin và submit                                          │
│ 6. Hệ thống:                                                                   │
│    a. Validate số điện thoại                                                   │
│    b. Generate OTP 6 số                                                        │
│    c. Call Zalo OA API gửi OTP                                                 │
│ 7. Phụ huynh nhận OTP qua Zalo                                                 │
│ 8. Phụ huynh nhập OTP trên web                                                 │
│ 9. Hệ thống verify OTP:                                                        │
│    a. Tạo tài khoản PARENT                                                     │
│    b. Liên kết với student_id                                                  │
│    c. Gửi welcome message                                                      │
│ 10. Redirect đến Parent Dashboard                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 6a. Số điện thoại đã đăng ký:                                                  │
│     - Thông báo đã có tài khoản                                                │
│     - Gợi ý đăng nhập                                                          │
│ 8a. OTP sai/hết hạn:                                                           │
│     - Cho phép gửi lại OTP (max 3 lần)                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Tài khoản phụ huynh được tạo và liên kết                         │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-PARENT-02: Xem thông tin học tập của con

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-PARENT-02: Xem thông tin học tập của con                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: PARENT                                                                   │
│ Precondition: Phụ huynh đã đăng nhập, có liên kết với học viên                  │
│ Trigger: Phụ huynh truy cập Dashboard                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Phụ huynh đăng nhập vào Parent Portal                                       │
│ 2. Dashboard hiển thị danh sách con em                                         │
│ 3. Phụ huynh chọn con muốn xem                                                 │
│ 4. Hệ thống hiển thị:                                                          │
│    a. Thông tin cơ bản:                                                        │
│       - Tên, lớp, giáo viên phụ trách                                          │
│    b. Điểm danh:                                                               │
│       - Tỷ lệ điểm danh tháng này: 95%                                         │
│       - Lịch sử điểm danh                                                      │
│    c. Điểm số:                                                                 │
│       - Điểm trung bình: 8.5                                                   │
│       - Danh sách bài kiểm tra và điểm                                         │
│    d. Bài tập:                                                                 │
│       - Bài tập chưa nộp                                                       │
│       - Bài tập đã chấm                                                        │
│    e. Điểm tích lũy:                                                           │
│       - Tổng điểm: 1,500 pts                                                   │
│       - Huy hiệu đã đạt                                                        │
│    f. Lịch học tuần này                                                        │
│ 5. Phụ huynh có thể:                                                           │
│    - Xem chi tiết từng mục                                                     │
│    - Tải báo cáo học tập                                                       │
│    - Liên hệ giáo viên                                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Phụ huynh nắm được tình hình học tập của con                     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-PARENT-03: Nhận thông báo từ trung tâm

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-PARENT-03: Nhận thông báo từ trung tâm                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: PARENT                                                                   │
│ Precondition: Phụ huynh đã đăng ký tài khoản                                    │
│ Trigger: Có sự kiện cần thông báo                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Các loại thông báo:                                                             │
│                                                                                 │
│ 1. ATTENDANCE:                                                                 │
│    - "Con bạn đã có mặt tại lớp hôm nay"                                       │
│    - "Con bạn vắng buổi học ngày hôm nay"                                      │
│                                                                                 │
│ 2. GRADE:                                                                      │
│    - "Con bạn đạt 9 điểm bài kiểm tra Toán"                                    │
│    - "Bài tập Văn đã được chấm điểm"                                           │
│                                                                                 │
│ 3. INVOICE:                                                                    │
│    - "Hóa đơn học phí tháng 12 đã được tạo"                                    │
│    - "Nhắc nhở: Hóa đơn sắp đến hạn thanh toán"                                │
│                                                                                 │
│ 4. GENERAL:                                                                    │
│    - "Thông báo nghỉ lễ Tết Nguyên Đán"                                        │
│    - "Lịch thi học kỳ đã cập nhật"                                             │
│                                                                                 │
│ Kênh gửi:                                                                       │
│ - Push notification (App)                                                      │
│ - Zalo OA Message                                                              │
│ - Email                                                                        │
│ - In-app notification                                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Phụ huynh nhận được thông báo kịp thời                           │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.7. FORUM MODULE - Use Cases

### UC-FORUM-01: Đặt câu hỏi trên diễn đàn

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-FORUM-01: Đặt câu hỏi trên diễn đàn                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: STUDENT, TEACHER                                                         │
│ Precondition: User đã đăng nhập                                                 │
│ Trigger: User muốn hỏi về bài học                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. User truy cập /forum                                                        │
│ 2. Chọn danh mục (theo lớp hoặc chủ đề)                                        │
│ 3. Click "Đặt câu hỏi mới"                                                     │
│ 4. Nhập:                                                                       │
│    - Tiêu đề câu hỏi                                                           │
│    - Nội dung chi tiết                                                         │
│    - Đính kèm ảnh/file (nếu cần)                                               │
│ 5. Submit câu hỏi                                                              │
│ 6. Hệ thống:                                                                   │
│    a. Tạo topic mới                                                            │
│    b. Notify giáo viên phụ trách lớp                                           │
│    c. Trigger Gamification: +5 pts                                             │
│ 7. Câu hỏi hiển thị trên forum                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Câu hỏi được đăng, giáo viên được thông báo                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 6: KITECLASS - USER SERVICE

## 6.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Authentication, Authorization, User Management |
| **Công nghệ** | Java Spring Boot + Spring Security |
| **Database** | PostgreSQL - Schema: user_module |
| **Actors** | All users |

## 6.2. Use Cases

### UC-USER-01: Đăng nhập hệ thống

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-USER-01: Đăng nhập hệ thống                                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: All users (CENTER_OWNER, ADMIN, TEACHER, STUDENT, PARENT)                │
│ Precondition: Có tài khoản trong hệ thống                                       │
│ Trigger: User truy cập trang login                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. User truy cập /login                                                        │
│ 2. Nhập email và password                                                      │
│ 3. Submit form                                                                 │
│ 4. User Service:                                                               │
│    a. Validate credentials                                                     │
│    b. Check account status (active, suspended)                                 │
│    c. Generate JWT access token (1h expiry)                                    │
│    d. Generate refresh token (7 days expiry)                                   │
│    e. Log login event                                                          │
│ 5. Return tokens                                                               │
│ 6. Frontend lưu token và redirect đến dashboard phù hợp với role               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 4a. Sai password:                                                              │
│     - Tăng failed_attempts                                                     │
│     - Nếu >= 5 lần → Lock account 30 phút                                      │
│     - Return error "Invalid credentials"                                       │
│                                                                                 │
│ 4b. Account suspended:                                                         │
│     - Return error "Account suspended"                                         │
│     - Gợi ý liên hệ admin                                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: User đã đăng nhập, có JWT token                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-USER-02: OAuth2 Social Login (Google)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-USER-02: OAuth2 Social Login (Google)                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: All users                                                                │
│ Precondition: Có tài khoản Google                                               │
│ Trigger: User click "Đăng nhập với Google"                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. User click "Đăng nhập với Google"                                           │
│ 2. Redirect đến Google OAuth consent screen                                    │
│ 3. User chọn tài khoản Google và authorize                                     │
│ 4. Google redirect về /auth/google/callback với auth code                      │
│ 5. User Service:                                                               │
│    a. Exchange code for Google tokens                                          │
│    b. Get user info từ Google                                                  │
│    c. Tìm hoặc tạo user theo Google ID                                         │
│    d. Generate JWT tokens                                                      │
│ 6. Redirect đến dashboard với token                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Alternative Flow:                                                               │
│ 5c. User chưa tồn tại và email chưa được invite:                               │
│     - Thông báo cần được mời bởi admin                                         │
│     - Không tự động tạo tài khoản                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: User đăng nhập thành công                                        │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-USER-03: Quản lý người dùng (Admin)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-USER-03: Quản lý người dùng (Admin)                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: CENTER_OWNER, CENTER_ADMIN                                               │
│ Precondition: Admin đã đăng nhập                                                │
│ Trigger: Admin truy cập trang quản lý user                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Admin truy cập /admin/users                                                 │
│ 2. Hệ thống hiển thị danh sách users với filter:                               │
│    - Theo role                                                                 │
│    - Theo status                                                               │
│    - Search by name/email                                                      │
│ 3. Admin có thể:                                                               │
│    a. Thêm user mới (mời qua email)                                            │
│    b. Sửa thông tin user                                                       │
│    c. Thay đổi role                                                            │
│    d. Suspend/Activate user                                                    │
│    e. Reset password                                                           │
│    f. Xem activity log                                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Users được quản lý                                               │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-USER-04: Phân quyền (Authorization)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-USER-04: Phân quyền (Authorization)                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: System (on every request)                                                │
│ Precondition: User đã authenticate                                              │
│ Trigger: User gọi API                                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Request đến API Gateway                                                     │
│ 2. Gateway extract JWT từ header                                               │
│ 3. Gateway validate JWT signature và expiry                                    │
│ 4. Gateway extract user info và roles                                          │
│ 5. Forward request với headers:                                                │
│    - X-User-Id                                                                 │
│    - X-User-Email                                                              │
│    - X-User-Roles                                                              │
│    - X-Tenant-Id                                                               │
│ 6. Target service:                                                             │
│    a. Check @RequirePermission annotation                                      │
│    b. Check @RequireRole annotation                                            │
│    c. Check resource-level access (nếu cần)                                    │
│ 7. Nếu authorized → Process request                                            │
│ 8. Nếu unauthorized → Return 403 Forbidden                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Request được authorize hoặc reject                               │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 7: KITECLASS - MEDIA SERVICE

## 7.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Video upload, transcode, streaming |
| **Công nghệ** | Node.js + FFmpeg + WebSocket |
| **Storage** | S3/CloudFlare R2 |
| **Actors** | TEACHER, STUDENT |

## 7.2. Use Cases

### UC-MEDIA-01: Upload video bài giảng

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MEDIA-01: Upload video bài giảng                                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: TEACHER                                                                  │
│ Precondition: Giáo viên được phân công lớp                                      │
│ Trigger: Giáo viên muốn upload video                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Giáo viên truy cập /teacher/videos                                          │
│ 2. Click "Upload video mới"                                                    │
│ 3. Nhập thông tin:                                                             │
│    - Tiêu đề video                                                             │
│    - Mô tả                                                                     │
│    - Lớp học liên quan                                                         │
│    - File video (MP4, MOV, max 2GB)                                            │
│ 4. Giáo viên upload file                                                       │
│ 5. Hệ thống:                                                                   │
│    a. Validate file format và size                                             │
│    b. Upload to temporary storage                                              │
│    c. Queue transcoding job                                                    │
│ 6. Hiển thị progress: "Đang xử lý video..."                                    │
│ 7. Transcoding service:                                                        │
│    a. Generate multiple qualities (360p, 480p, 720p, 1080p)                    │
│    b. Generate HLS segments                                                    │
│    c. Generate thumbnail                                                       │
│    d. Upload processed files to CDN                                            │
│ 8. Cập nhật video status = READY                                               │
│ 9. Notify giáo viên                                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Video sẵn sàng phát                                              │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-MEDIA-02: Xem video bài giảng

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MEDIA-02: Xem video bài giảng                                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: STUDENT                                                                  │
│ Precondition: Học viên thuộc lớp có video                                       │
│ Trigger: Học viên mở video                                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Học viên truy cập /student/videos                                           │
│ 2. Xem danh sách video của lớp                                                 │
│ 3. Click vào video muốn xem                                                    │
│ 4. Hệ thống:                                                                   │
│    a. Check quyền truy cập                                                     │
│    b. Generate signed URL (time-limited)                                       │
│    c. Load video player                                                        │
│ 5. Video player:                                                               │
│    a. Auto-detect network và chọn quality phù hợp                              │
│    b. Load HLS stream                                                          │
│    c. Track watch progress                                                     │
│ 6. Khi xem xong (>= 90%):                                                      │
│    a. Mark video as completed                                                  │
│    b. Trigger Gamification: +15 pts                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Học viên đã xem video, progress được track                       │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-MEDIA-03: Phát trực tiếp buổi học

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-MEDIA-03: Phát trực tiếp buổi học                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: TEACHER (host), STUDENT (viewer)                                         │
│ Precondition: Có lịch học, giáo viên được phân công                             │
│ Trigger: Giáo viên bắt đầu live                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow (Teacher):                                                            │
│ 1. Giáo viên truy cập lớp học                                                  │
│ 2. Click "Bắt đầu Live"                                                        │
│ 3. Hệ thống:                                                                   │
│    a. Tạo streaming session                                                    │
│    b. Generate stream key                                                      │
│    c. Setup WebRTC/RTMP ingest                                                 │
│ 4. Giáo viên share screen và/hoặc camera                                       │
│ 5. Hệ thống notify học viên "Buổi học đã bắt đầu"                              │
│                                                                                 │
│ Main Flow (Student):                                                            │
│ 6. Học viên nhận notification                                                  │
│ 7. Click vào notification                                                      │
│ 8. Hệ thống load live player                                                   │
│ 9. Học viên xem live và có thể:                                                │
│    - Chat real-time                                                            │
│    - Raise hand (giơ tay)                                                      │
│    - React (emoji)                                                             │
│                                                                                 │
│ Kết thúc:                                                                       │
│ 10. Giáo viên click "Kết thúc Live"                                            │
│ 11. Hệ thống:                                                                  │
│     a. Stop stream                                                             │
│     b. Process recording (nếu enabled)                                         │
│     c. Save as video resource                                                  │
│     d. Mark attendance cho những ai join                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Buổi học live hoàn thành, có recording                           │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 8: KITECLASS - FRONTEND

## 8.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Giao diện người dùng multi-role |
| **Công nghệ** | Next.js (React) + SSR |
| **UI Framework** | Tailwind CSS / Shadcn |
| **Actors** | All roles |

## 8.2. Use Cases theo Role

### UC-FE-01: Dashboard theo role

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-FE-01: Dashboard theo role                                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: All authenticated users                                                  │
│ Precondition: User đã đăng nhập                                                 │
│ Trigger: User access /dashboard                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. User truy cập /dashboard                                                    │
│ 2. Frontend check JWT và extract role                                          │
│ 3. Render dashboard phù hợp:                                                   │
│                                                                                 │
│ CENTER_OWNER / CENTER_ADMIN:                                                   │
│ ┌───────────────────────────────────────────────────────────────────────────┐  │
│ │ • Overview: Tổng học viên, lớp, doanh thu                                │  │
│ │ • Quick actions: Tạo lớp, thêm học viên, tạo hóa đơn                     │  │
│ │ • Charts: Doanh thu tháng, tỷ lệ điểm danh                               │  │
│ │ • Alerts: Công nợ, sắp hết hạn gói                                        │  │
│ │ • Recent activities                                                       │  │
│ └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                 │
│ TEACHER:                                                                        │
│ ┌───────────────────────────────────────────────────────────────────────────┐  │
│ │ • Today's classes: Lịch dạy hôm nay                                      │  │
│ │ • Quick actions: Điểm danh, nhập điểm, upload video                      │  │
│ │ • My classes: Danh sách lớp phụ trách                                    │  │
│ │ • Pending tasks: Bài tập chưa chấm, câu hỏi chưa trả lời                 │  │
│ │ • Calendar view                                                          │  │
│ └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                 │
│ STUDENT:                                                                        │
│ ┌───────────────────────────────────────────────────────────────────────────┐  │
│ │ • My points: Điểm tích lũy, badges                                       │  │
│ │ • Today's schedule: Lịch học hôm nay                                     │  │
│ │ • Assignments: Bài tập cần nộp                                           │  │
│ │ • Recent grades: Điểm gần đây                                            │  │
│ │ • Quick access: Videos, Forum, Rewards                                   │  │
│ └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                 │
│ PARENT:                                                                         │
│ ┌───────────────────────────────────────────────────────────────────────────┐  │
│ │ • Children overview: Danh sách con                                       │  │
│ │ • Recent activities: Hoạt động gần đây của con                           │  │
│ │ • Unpaid invoices: Hóa đơn chưa thanh toán                               │  │
│ │ • Notifications: Thông báo mới                                           │  │
│ │ • Quick contact: Liên hệ giáo viên                                       │  │
│ └───────────────────────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: User thấy dashboard phù hợp với role                             │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-FE-02: Responsive Multi-device Support

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-FE-02: Responsive Multi-device Support                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: All users                                                                │
│ Precondition: Không                                                             │
│ Trigger: User truy cập từ bất kỳ device                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Device Support:                                                                 │
│                                                                                 │
│ Desktop (>= 1024px):                                                           │
│ • Full sidebar navigation                                                      │
│ • Multi-column layouts                                                         │
│ • Full data tables                                                             │
│ • Keyboard shortcuts                                                           │
│                                                                                 │
│ Tablet (768px - 1023px):                                                       │
│ • Collapsible sidebar                                                          │
│ • Adjusted layouts                                                             │
│ • Touch-friendly buttons                                                       │
│                                                                                 │
│ Mobile (< 768px):                                                              │
│ • Bottom navigation                                                            │
│ • Single column layout                                                         │
│ • Swipe gestures                                                               │
│ • Optimized for touch                                                          │
│ • PWA support (install to home screen)                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: UI tối ưu cho từng loại device                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 9: KITEHUB - FRONTEND

## 9.1. Tổng quan

| Thuộc tính | Giá trị |
|------------|---------|
| **Mô tả** | Giao diện website marketing, admin dashboard, customer portal |
| **Công nghệ** | Next.js (React) + SSR/SSG |
| **UI Framework** | Tailwind CSS |
| **Actors** | Visitor, Customer, Admin, Agent |

## 9.2. Use Cases

### UC-HUB-FE-01: Landing Page & Marketing Website

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-HUB-FE-01: Landing Page & Marketing Website                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Visitor (Khách truy cập)                                                 │
│ Precondition: Không                                                             │
│ Trigger: Visitor truy cập https://kiteclass.com                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Visitor truy cập trang chủ                                                  │
│ 2. Hệ thống render Landing Page với các sections:                              │
│                                                                                 │
│    ┌─────────────────────────────────────────────────────────────────────┐     │
│    │                         KITECLASS.COM                                │     │
│    ├─────────────────────────────────────────────────────────────────────┤     │
│    │  [Logo]                    [Tính năng] [Bảng giá] [Demo] [Đăng nhập]│     │
│    ├─────────────────────────────────────────────────────────────────────┤     │
│    │                                                                      │     │
│    │           QUẢN LÝ TRUNG TÂM ĐÀO TẠO CHUYÊN NGHIỆP                  │     │
│    │                                                                      │     │
│    │      Nền tảng all-in-one cho trung tâm giáo dục                    │     │
│    │      với AI-powered branding và quản lý toàn diện                  │     │
│    │                                                                      │     │
│    │            [Dùng thử miễn phí]  [Xem Demo]                          │     │
│    │                                                                      │     │
│    ├─────────────────────────────────────────────────────────────────────┤     │
│    │  TÍNH NĂNG NỔI BẬT                                                  │     │
│    │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       │     │
│    │  │Quản lý  │ │Điểm danh│ │Học phí  │ │Phụ huynh│ │  Game   │       │     │
│    │  │Lớp học  │ │Điểm số  │ │Hóa đơn  │ │ Portal  │ │hóa điểm │       │     │
│    │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘       │     │
│    │                                                                      │     │
│    ├─────────────────────────────────────────────────────────────────────┤     │
│    │  BẢNG GIÁ                                                           │     │
│    │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │     │
│    │  │    BASIC     │ │   STANDARD   │ │   PREMIUM    │                │     │
│    │  │  500k/tháng  │ │  1.5tr/tháng │ │  3tr/tháng   │                │     │
│    │  │  50 học viên │ │ 200 học viên │ │  Unlimited   │                │     │
│    │  │  [Đăng ký]   │ │  [Đăng ký]   │ │  [Đăng ký]   │                │     │
│    │  └──────────────┘ └──────────────┘ └──────────────┘                │     │
│    │                                                                      │     │
│    ├─────────────────────────────────────────────────────────────────────┤     │
│    │  KHÁCH HÀNG NÓI GÌ VỀ CHÚNG TÔI                                    │     │
│    │  [Testimonials slider]                                              │     │
│    │                                                                      │     │
│    ├─────────────────────────────────────────────────────────────────────┤     │
│    │  FAQ | LIÊN HỆ | FOOTER                                            │     │
│    └─────────────────────────────────────────────────────────────────────┘     │
│                                                                                 │
│ 3. Visitor có thể điều hướng đến:                                              │
│    - /features: Chi tiết tính năng                                             │
│    - /pricing: Chi tiết bảng giá                                               │
│    - /demo: Xem demo tương tác                                                 │
│    - /blog: Blog & tin tức                                                     │
│    - /contact: Form liên hệ                                                    │
│    - /login: Đăng nhập (Customer/Admin)                                        │
│    - /register: Đăng ký dùng thử                                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│ SEO Features:                                                                   │
│ • Server-Side Rendering cho tất cả pages                                       │
│ • Meta tags, Open Graph, Twitter Cards                                         │
│ • Sitemap.xml tự động generate                                                 │
│ • Schema.org structured data                                                   │
│ • Core Web Vitals optimized                                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Visitor có thông tin sản phẩm, có thể đăng ký                    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-HUB-FE-02: Admin Dashboard (KiteHub Admin)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-HUB-FE-02: Admin Dashboard (KiteHub Admin)                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Admin, Agent (nhân viên KiteHub)                                         │
│ Precondition: Admin/Agent đã đăng nhập                                          │
│ Trigger: Admin truy cập /admin                                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Admin đăng nhập tại /admin/login                                            │
│ 2. Hệ thống hiển thị Admin Dashboard:                                          │
│                                                                                 │
│    ┌─────────────────────────────────────────────────────────────────────┐     │
│    │  KITEHUB ADMIN                                          [Admin ▼]   │     │
│    ├──────────────┬──────────────────────────────────────────────────────┤     │
│    │              │                                                       │     │
│    │  Dashboard   │  OVERVIEW                                            │     │
│    │  ──────────  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐        │     │
│    │  📊 Overview │  │ 156    │ │ 12     │ │ 8      │ │ 45tr   │        │     │
│    │  📦 Orders   │  │Instances│ │New Today│ │Pending │ │Revenue │        │     │
│    │  🏢 Instances│  └────────┘ └────────┘ └────────┘ └────────┘        │     │
│    │  👥 Customers│                                                       │     │
│    │  💬 Support  │  RECENT ORDERS                                       │     │
│    │  🤖 AI Jobs  │  ┌─────────────────────────────────────────────────┐ │     │
│    │  📈 Analytics│  │ #12345 │ ACME Academy │ STANDARD │ PAID │ 1.5tr │ │     │
│    │  ⚙️ Settings │  │ #12344 │ ABC Center   │ BASIC    │ PEND │ 500k  │ │     │
│    │              │  │ #12343 │ XYZ School   │ PREMIUM  │ ACTV │ 3tr   │ │     │
│    │  ──────────  │  └─────────────────────────────────────────────────┘ │     │
│    │  Agents (5)  │                                                       │     │
│    │  • Lan ●     │  INSTANCE HEALTH                                     │     │
│    │  • Minh ●    │  ┌─────────────────────────────────────────────────┐ │     │
│    │  • Hùng ○    │  │ 🟢 acme.kiteclass.com    - Healthy - 45 users  │ │     │
│    │              │  │ 🟢 abc.kiteclass.com     - Healthy - 120 users │ │     │
│    │              │  │ 🟡 xyz.kiteclass.com     - Warning - High CPU  │ │     │
│    │              │  └─────────────────────────────────────────────────┘ │     │
│    │              │                                                       │     │
│    └──────────────┴──────────────────────────────────────────────────────┘     │
│                                                                                 │
│ 3. Admin có thể truy cập các modules:                                          │
│    a. Orders: Quản lý đơn hàng, xác nhận thanh toán                            │
│    b. Instances: Xem trạng thái, suspend/resume, xem logs                      │
│    c. Customers: Quản lý thông tin khách hàng                                  │
│    d. Support: Xem/assign chat tickets                                         │
│    e. AI Jobs: Xem tiến trình AI generation                                    │
│    f. Analytics: Báo cáo doanh thu, growth metrics                             │
│    g. Settings: Cấu hình hệ thống, pricing, templates                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Admin quản lý được toàn bộ hệ thống KiteHub                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-HUB-FE-03: Customer Portal

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-HUB-FE-03: Customer Portal                                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Customer (Chủ trung tâm đã mua gói)                                      │
│ Precondition: Customer đã có tài khoản và subscription                          │
│ Trigger: Customer truy cập /portal                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Customer đăng nhập tại /portal/login                                        │
│ 2. Hệ thống hiển thị Customer Portal:                                          │
│                                                                                 │
│    ┌─────────────────────────────────────────────────────────────────────┐     │
│    │  MY KITECLASS                                      [Nguyễn Văn A ▼] │     │
│    ├──────────────┬──────────────────────────────────────────────────────┤     │
│    │              │                                                       │     │
│    │  Dashboard   │  THÔNG TIN SUBSCRIPTION                              │     │
│    │  ──────────  │                                                       │     │
│    │  🏠 Overview │  ┌─────────────────────────────────────────────────┐ │     │
│    │  📄 Billing  │  │  Gói: STANDARD                                  │ │     │
│    │  🌐 Instance │  │  Instance: acme.kiteclass.com                   │ │     │
│    │  🎨 Branding │  │  Trạng thái: 🟢 Active                          │ │     │
│    │  💬 Support  │  │  Hết hạn: 15/01/2026                            │ │     │
│    │  📊 Usage    │  │  Học viên: 145/200                              │ │     │
│    │              │  │                                                  │ │     │
│    │              │  │  [Nâng cấp gói]  [Gia hạn]                       │ │     │
│    │              │  └─────────────────────────────────────────────────┘ │     │
│    │              │                                                       │     │
│    │              │  THỐNG KÊ SỬ DỤNG THÁNG NÀY                         │     │
│    │              │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐        │     │
│    │              │  │ 145    │ │ 12     │ │ 850    │ │ 98%    │        │     │
│    │              │  │Học viên│ │Giáo viên│ │Buổi học│ │Uptime  │        │     │
│    │              │  └────────┘ └────────┘ └────────┘ └────────┘        │     │
│    │              │                                                       │     │
│    │              │  HÓA ĐƠN GẦN NHẤT                                   │     │
│    │              │  ┌─────────────────────────────────────────────────┐ │     │
│    │              │  │ 12/2025 │ 1,500,000đ │ Đã thanh toán │ [Xem]   │ │     │
│    │              │  │ 11/2025 │ 1,500,000đ │ Đã thanh toán │ [Xem]   │ │     │
│    │              │  └─────────────────────────────────────────────────┘ │     │
│    │              │                                                       │     │
│    └──────────────┴──────────────────────────────────────────────────────┘     │
│                                                                                 │
│ 3. Customer có thể:                                                            │
│    a. Overview: Xem thông tin tổng quan subscription                           │
│    b. Billing: Xem hóa đơn, thanh toán, tải invoice PDF                        │
│    c. Instance: Truy cập nhanh vào instance, xem status                        │
│    d. Branding: Xem/tải lại AI-generated assets                                │
│    e. Support: Gửi ticket hỗ trợ, xem lịch sử chat                             │
│    f. Usage: Xem thống kê sử dụng, quotas                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Customer quản lý được subscription của mình                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-HUB-FE-04: Agent Chat Interface

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-HUB-FE-04: Agent Chat Interface                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Agent (Nhân viên hỗ trợ)                                                 │
│ Precondition: Agent đã đăng nhập, status = Available                            │
│ Trigger: Agent truy cập /admin/support hoặc có chat mới                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Agent đăng nhập vào admin panel                                             │
│ 2. Truy cập Support module                                                     │
│ 3. Hệ thống hiển thị Chat Interface:                                           │
│                                                                                 │
│    ┌─────────────────────────────────────────────────────────────────────┐     │
│    │  SUPPORT CENTER                              [Online ●] [Agent: Lan]│     │
│    ├───────────────┬─────────────────────────────────────────────────────┤     │
│    │               │                                                      │     │
│    │  CONVERSATIONS│  CHAT WITH: khach@gmail.com                         │     │
│    │  ───────────  │  Page: /pricing  •  IP: 42.112.xxx                  │     │
│    │               │  ─────────────────────────────────────────────────  │     │
│    │  🔴 New (3)   │                                                      │     │
│    │  ├─ khach@... │  👤 Khách: Cho tôi hỏi gói Standard có bao          │     │
│    │  ├─ abc@...   │     nhiêu học viên vậy?                             │     │
│    │  └─ xyz@...   │                                           14:30     │     │
│    │               │                                                      │     │
│    │  🟡 Active(2) │  👨‍💼 Lan: Chào anh/chị! Gói Standard hỗ trợ         │     │
│    │  ├─ demo@...  │     tối đa 200 học viên ạ.                          │     │
│    │  └─ test@...  │                                           14:31     │     │
│    │               │                                                      │     │
│    │  🟢 Resolved  │  👤 Khách: Vậy có thể nâng cấp sau không?           │     │
│    │  (15 today)   │                                           14:32     │     │
│    │               │                                                      │     │
│    │               │  ─────────────────────────────────────────────────  │     │
│    │               │                                                      │     │
│    │               │  [Canned Responses ▼] [Attach 📎] [Transfer 👥]     │     │
│    │               │  ┌─────────────────────────────────────────────┐    │     │
│    │               │  │ Nhập tin nhắn...                       [Gửi]│    │     │
│    │               │  └─────────────────────────────────────────────┘    │     │
│    │               │                                                      │     │
│    │  ───────────  │  CUSTOMER INFO                                      │     │
│    │  Quick Stats  │  • Email: khach@gmail.com                           │     │
│    │  📊 12 chats  │  • Visits: 5                                        │     │
│    │  ⏱ Avg: 3min  │  • Last page: /pricing                              │     │
│    │               │  • Previous chats: 0                                │     │
│    └───────────────┴─────────────────────────────────────────────────────┘     │
│                                                                                 │
│ 4. Agent có thể:                                                               │
│    a. Trả lời chat real-time                                                   │
│    b. Sử dụng Canned Responses (mẫu câu trả lời)                               │
│    c. Đính kèm file/hình ảnh                                                   │
│    d. Transfer chat sang agent khác                                            │
│    e. Đánh tag/note cho conversation                                           │
│    f. Close/Resolve conversation                                               │
│    g. Xem thông tin khách hàng và lịch sử                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Agent đã hỗ trợ khách hàng                                       │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### UC-HUB-FE-05: Instance Management UI

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ UC-HUB-FE-05: Instance Management UI                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Actor: Admin                                                                    │
│ Precondition: Admin đã đăng nhập                                                │
│ Trigger: Admin truy cập /admin/instances                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Main Flow:                                                                      │
│ 1. Admin truy cập Instances module                                             │
│ 2. Hệ thống hiển thị danh sách instances:                                      │
│                                                                                 │
│    ┌─────────────────────────────────────────────────────────────────────┐     │
│    │  INSTANCES                              [Search...] [Filter ▼] [+]  │     │
│    ├─────────────────────────────────────────────────────────────────────┤     │
│    │                                                                      │     │
│    │  ┌───────────────────────────────────────────────────────────────┐  │     │
│    │  │ 🟢 acme.kiteclass.com                                         │  │     │
│    │  │    ACME Academy │ STANDARD │ 145 users │ Uptime: 99.9%        │  │     │
│    │  │    Created: 01/12/2025 │ Expires: 15/01/2026                  │  │     │
│    │  │    CPU: 23% │ Memory: 45% │ Storage: 12GB/50GB                │  │     │
│    │  │    [View] [Logs] [Suspend] [Settings]                         │  │     │
│    │  └───────────────────────────────────────────────────────────────┘  │     │
│    │                                                                      │     │
│    │  ┌───────────────────────────────────────────────────────────────┐  │     │
│    │  │ 🟡 xyz.kiteclass.com                              ⚠ Warning   │  │     │
│    │  │    XYZ School │ PREMIUM │ 890 users │ Uptime: 98.5%           │  │     │
│    │  │    CPU: 85% │ Memory: 78% │ Storage: 45GB/100GB               │  │     │
│    │  │    Alert: High CPU usage for 2 hours                          │  │     │
│    │  │    [View] [Logs] [Scale Up] [Settings]                        │  │     │
│    │  └───────────────────────────────────────────────────────────────┘  │     │
│    │                                                                      │     │
│    │  ┌───────────────────────────────────────────────────────────────┐  │     │
│    │  │ 🔴 old.kiteclass.com                              Suspended   │  │     │
│    │  │    Old Center │ BASIC │ 0 users │ Payment overdue: 30 days    │  │     │
│    │  │    [Resume] [Contact Customer] [Delete]                       │  │     │
│    │  └───────────────────────────────────────────────────────────────┘  │     │
│    │                                                                      │     │
│    └─────────────────────────────────────────────────────────────────────┘     │
│                                                                                 │
│ 3. Click vào instance để xem chi tiết:                                         │
│    a. Overview: Thông tin tổng quan                                            │
│    b. Metrics: Charts CPU, Memory, Requests over time                          │
│    c. Logs: Real-time logs từ các services                                     │
│    d. Events: Audit log của instance                                           │
│    e. Settings: Cấu hình, quotas                                               │
│    f. Actions: Restart, Scale, Suspend, Delete                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│ Postcondition: Admin quản lý được tất cả instances                              │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# TỔNG HỢP USE CASES

## Theo Service

| Service | Số lượng Use Cases | Key Use Cases |
|---------|-------------------|---------------|
| **KITEHUB - Sale** | 4 | Đăng ký, thanh toán, quản lý đơn hàng |
| **KITEHUB - Message** | 3 | Chat hỗ trợ, thông báo hệ thống |
| **KITEHUB - Maintaining** | 3 | Provisioning, monitoring, suspend/resume |
| **KITEHUB - AI Agent** | 1 | Generate marketing assets |
| **KITEHUB - Frontend** | 5 | Landing page, Admin dashboard, Customer portal, Agent chat, Instance management |
| **KITECLASS - Core (Class)** | 3 | Tạo khóa học, tạo lớp, thêm học viên |
| **KITECLASS - Core (Learning)** | 3 | Điểm danh, nhập điểm, bài tập |
| **KITECLASS - Core (Billing)** | 3 | Tạo hóa đơn, thanh toán, thu tiền |
| **KITECLASS - Core (Gamification)** | 3 | Tích điểm, đổi quà, bảng xếp hạng |
| **KITECLASS - Core (Parent)** | 3 | Đăng ký Zalo OTP, xem thông tin, nhận thông báo |
| **KITECLASS - Core (Forum)** | 1 | Đặt câu hỏi |
| **KITECLASS - User** | 4 | Login, OAuth, quản lý user, authorization |
| **KITECLASS - Media** | 3 | Upload video, xem video, live streaming |
| **KITECLASS - Frontend** | 2 | Dashboard multi-role, responsive |
| **TỔNG** | **41** | |

## Theo Actor

| Actor | Số Use Cases liên quan |
|-------|------------------------|
| CENTER_OWNER | 12 |
| CENTER_ADMIN | 14 |
| TEACHER | 10 |
| STUDENT | 9 |
| PARENT | 6 |
| Visitor (KiteHub) | 1 |
| Customer (KiteHub) | 5 |
| Admin (KiteHub) | 7 |
| Agent (KiteHub) | 2 |
| System (Automated) | 8 |

---

*Báo cáo được tạo bởi: Claude Assistant*
*Ngày: 23/12/2025*
*Phiên bản: 3.0 (Final)*
