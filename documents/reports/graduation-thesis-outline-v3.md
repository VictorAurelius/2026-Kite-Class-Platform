# ĐỀ CƯƠNG Ý TƯỞNG ĐỒ ÁN TỐT NGHIỆP

## TÊN ĐỀ TÀI: XÂY DỰNG HỆ THỐNG QUẢN LÝ TRUNG TÂM GIÁO DỤC THEO KIẾN TRÚC MICROSERVICES - KITECLASS PLATFORM V3.1

---

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Phiên bản** | 3.1 (Final) |
| **Ngày cập nhật** | 23/12/2025 |
| **Thay đổi từ V2** | Thêm Phụ huynh, Hóa đơn, Gamification, Tối ưu kiến trúc |
| **Tham chiếu** | system-architecture-v3-final.md, service-use-cases-v3.md |

---

# MỤC LỤC

- [PHẦN 1: Ý TƯỞNG ĐỀ TÀI](#phần-1-ý-tưởng-đề-tài)
- [PHẦN 2: KIẾN TRÚC HỆ THỐNG V3.1](#phần-2-kiến-trúc-hệ-thống-v31)
- [PHẦN 3: CÁC ACTORS VÀ USE CASES](#phần-3-các-actors-và-use-cases)
- [PHẦN 4: QUY TRÌNH MỞ NODE VỚI AI AGENT](#phần-4-quy-trình-mở-node-với-ai-agent)
- [PHẦN 5: THIẾT KẾ DATABASE](#phần-5-thiết-kế-database)
- [PHẦN 6: CÔNG NGHỆ SỬ DỤNG](#phần-6-công-nghệ-sử-dụng)
- [PHẦN 7: KẾ HOẠCH TRIỂN KHAI](#phần-7-kế-hoạch-triển-khai)
- [PHẦN 8: ĐIỂM MẠNH CỦA ĐỀ TÀI](#phần-8-điểm-mạnh-của-đề-tài)
- [PHẦN 9: THỬ THÁCH CỦA ĐỀ TÀI](#phần-9-thử-thách-của-đề-tài)
- [KẾT LUẬN](#kết-luận)
- [PHỤ LỤC](#phụ-lục)

---

# PHẦN 1: Ý TƯỞNG ĐỀ TÀI

## 1.1. Bối cảnh và lý do chọn đề tài

Trong bối cảnh chuyển đổi số giáo dục đang diễn ra mạnh mẽ tại Việt Nam, các trung tâm giáo dục, trung tâm ngoại ngữ, và các tổ chức đào tạo nhỏ đang gặp nhiều khó khăn trong việc quản lý:

- **Quản lý học viên phức tạp**: Theo dõi điểm danh, điểm số, tiến độ học tập
- **Thu học phí thủ công**: Ghi chép sổ sách, khó theo dõi công nợ
- **Thiếu kênh liên lạc với phụ huynh**: Phụ huynh không nắm được tình hình học tập của con
- **Marketing và xây dựng thương hiệu khó khăn**: Chi phí cao, không có nguồn lực

KiteClass Platform được phát triển nhằm giải quyết các vấn đề trên bằng cách cung cấp một nền tảng SaaS cho phép mỗi trung tâm nhanh chóng có hệ thống quản lý riêng với:
- Giao diện và thương hiệu cá nhân hóa (tự động bởi AI)
- Hệ thống quản lý học viên, lớp học đầy đủ
- Cổng phụ huynh để theo dõi con em
- Thanh toán học phí qua QR Code (VietQR)
- Gamification để tăng hứng thú học tập

## 1.2. Mục tiêu đề tài

1. **Xây dựng nền tảng SaaS** quản lý trung tâm giáo dục với khả năng multi-tenancy
2. **Áp dụng kiến trúc Microservices** cho KiteClass instances với tối ưu chi phí (3-5 services)
3. **Tích hợp AI Agent** để tự động hóa tạo thương hiệu và content marketing
4. **Xây dựng hệ thống thanh toán** học phí với VietQR
5. **Phát triển Cổng phụ huynh** để tăng cường liên lạc gia đình - trung tâm
6. **Triển khai Gamification** để tăng hứng thú học tập

## 1.3. Phạm vi nghiên cứu

| Khía cạnh | Phạm vi |
|-----------|---------|
| **Đối tượng sử dụng** | Trung tâm ngoại ngữ, trung tâm kỹ năng, trung tâm luyện thi |
| **Actors** | Center Owner, Center Admin, Teacher, Student, Parent, Customer, Admin KiteHub |
| **Kiến trúc** | Microservices (KiteClass) + Modular Monolith (KiteHub) |
| **Platform** | Web application (PWA ready cho mobile) |
| **Cloud** | Kubernetes trên VPS/Cloud provider |

## 1.4. Nguồn học hỏi thực tế

Đề tài được phát triển dựa trên phân tích hệ thống **BeeClass** - một nền tảng quản lý trung tâm đang hoạt động thực tế tại Việt Nam, với các tính năng học hỏi:

- Hệ thống quản lý phụ huynh và liên kết học viên
- Quy trình thanh toán học phí qua QR Code
- Hệ thống điểm thưởng và gamification
- Notification qua Zalo OA

---

# PHẦN 2: KIẾN TRÚC HỆ THỐNG V3.1

## 2.1. Tổng quan kiến trúc

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     KITECLASS PLATFORM V3.1 (OPTIMIZED)                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                       KITEHUB (MODULAR MONOLITH)                           │ │
│  │                        Java Spring Boot Application                         │ │
│  │                                                                            │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │ │
│  │  │    Sale      │  │   Message    │  │  Maintaining │  │  AI Agent    │   │ │
│  │  │   Module     │  │    Module    │  │    Module    │  │   Module     │   │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │ │
│  │                                                                            │ │
│  │  ┌────────────────────────────────────────────────────────────────────┐   │ │
│  │  │                     KITEHUB FRONTEND (Next.js)                     │   │ │
│  │  │  • Landing Page  • Admin Dashboard  • Customer Portal  • Chat UI  │   │ │
│  │  └────────────────────────────────────────────────────────────────────┘   │ │
│  │                                                                            │ │
│  │         ┌─────────────┐    ┌─────────────┐    ┌─────────────┐             │ │
│  │         │ PostgreSQL  │    │    Redis    │    │  RabbitMQ   │             │ │
│  │         └─────────────┘    └─────────────┘    └─────────────┘             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                       │                                         │
│                              Provisioning / Events                              │
│                                       │                                         │
│  ┌────────────────────┐    ┌────────────────────┐    ┌────────────────────┐    │
│  │   KITECLASS #1     │    │   KITECLASS #2     │    │   KITECLASS #N     │    │
│  │ (3-5 Services)     │    │ (3-5 Services)     │    │ (3-5 Services)     │    │
│  │                    │    │                    │    │                    │    │
│  │ • User+Gateway ⚡  │    │ • User+Gateway ⚡  │    │ • User+Gateway ⚡  │    │
│  │ • Core Service ⚡  │    │ • Core Service ⚡  │    │ • Core Service ⚡  │    │
│  │ • Engagement 📦    │    │ • Engagement 📦    │    │ • Engagement 📦    │    │
│  │ • Media 📦         │    │ • Media 📦         │    │ • Media 📦         │    │
│  │ • Frontend ⚡      │    │ • Frontend ⚡      │    │ • Frontend ⚡      │    │
│  └────────────────────┘    └────────────────────┘    └────────────────────┘    │
│                                                                                  │
│  ⚡ = Bắt buộc    📦 = Tùy chọn (theo gói)                                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.2. So sánh kiến trúc V2 vs V3.1

| Thuộc tính | V2 (Cũ) | V3.1 (Mới) | Lý do thay đổi |
|------------|---------|------------|----------------|
| **Số services** | 7 services cố định | 3-5 services linh hoạt | Giảm 50% RAM, dễ maintain |
| **Gateway** | Service riêng | Merge vào User Service | Giảm 1 container, giảm latency |
| **Gamification** | Không có | ✅ Có (optional) | Học từ BeeClass |
| **Parent Portal** | Không có | ✅ Có | Học từ BeeClass |
| **Billing** | Không có | ✅ Có với VietQR | Nhu cầu thực tế |
| **RAM/Instance** | ~4GB | ~2.5GB (min) | Tiết kiệm 40% |

## 2.3. Chi tiết KiteClass Instance Services

### 2.3.1. User + Gateway Service (Bắt buộc) - 512MB RAM

| Module | Chức năng |
|--------|-----------|
| **API Gateway** | Rate limiting, Request routing, Auth validation |
| **Authentication** | JWT, OAuth2, Zalo OTP login |
| **User Management** | CRUD users, roles, permissions |
| **Session Management** | Token refresh, logout |

### 2.3.2. Core Service (Bắt buộc) - 1GB RAM

| Module | Chức năng |
|--------|-----------|
| **Course Management** | Khóa học, chương trình học |
| **Class Management** | Lớp học, lịch học, phòng học |
| **Student Management** | Học viên, đăng ký lớp |
| **Attendance** | Điểm danh theo buổi |
| **Grading** | Chấm điểm, đánh giá |
| **Billing** | Hóa đơn, thanh toán VietQR |

### 2.3.3. Engagement Service (Tùy chọn) - 512MB RAM

| Module | Chức năng |
|--------|-----------|
| **Gamification** | Điểm thưởng, huy hiệu, bảng xếp hạng |
| **Parent Portal** | Liên kết phụ huynh, theo dõi con |
| **Forum** | Diễn đàn trao đổi |
| **Notification** | Gửi thông báo Zalo, Email |

### 2.3.4. Media Service (Tùy chọn) - 512MB RAM

| Module | Chức năng |
|--------|-----------|
| **Video Management** | Upload, streaming video bài giảng |
| **Live Class** | Học trực tuyến WebRTC |
| **File Storage** | Quản lý tài liệu học tập |

### 2.3.5. Frontend (Bắt buộc) - 256MB RAM

| Feature | Mô tả |
|---------|-------|
| **Admin Portal** | Dashboard cho CENTER_OWNER, CENTER_ADMIN |
| **Teacher Portal** | Quản lý lớp, điểm danh, chấm điểm |
| **Student Portal** | Xem lịch, điểm, bài tập |
| **Parent App (PWA)** | Theo dõi con, thanh toán |

## 2.4. Tại sao chọn kiến trúc này?

### KiteHub: Modular Monolith

```
✅ Ưu điểm:
├── Giao tiếp module đơn giản (method calls)
├── Transaction xuyên module dễ dàng
├── Chi phí vận hành thấp (1 deployment)
├── Phù hợp với traffic thấp (admin operations)
└── Dễ debug và maintain

❌ Tại sao không Microservices:
├── Các module liên kết chặt (Sale → AI → Maintaining)
├── Traffic thấp, không cần scale riêng
└── Overkill cho use case này
```

### KiteClass: Microservices (3-5 services)

```
✅ Ưu điểm:
├── Scale độc lập theo nhu cầu
├── Isolation tốt giữa tenants
├── Deploy/update từng service riêng
├── Fault tolerance (1 service down không ảnh hưởng)
└── Linh hoạt theo gói dịch vụ

⚠️ Trade-offs được chấp nhận:
├── Phức tạp hơn monolith
├── Network latency giữa services
└── Cần monitoring đầy đủ
```

### Tại sao KHÔNG dùng Service Registry (Eureka/Consul)?

```
Sau phân tích chi tiết (xem service-registry-analysis.md):

❌ KHÔNG KHUYẾN NGHỊ vì:
├── Chỉ có 3-5 services → quá ít để cần registry
├── Tăng 40% RAM overhead (~800MB)
├── Tăng độ phức tạp vận hành
├── ROI = -95% (chi phí > lợi ích)
└── Kubernetes Service Discovery đủ dùng

✅ Thay thế bằng:
├── Kubernetes Service (internal DNS)
├── Docker Compose (dev environment)
└── Hard-coded URLs với config (simple & works)
```

---

# PHẦN 3: CÁC ACTORS VÀ USE CASES

## 3.1. Tổng quan Actors

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              ACTORS HỆ THỐNG V3.1                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  KITEHUB ACTORS                          KITECLASS INSTANCE ACTORS              │
│  ─────────────────                       ──────────────────────────              │
│                                                                                  │
│  ┌──────────────┐                        ┌──────────────┐                       │
│  │   Customer   │                        │CENTER_OWNER  │ ← Chủ trung tâm       │
│  │  (Mua gói)   │                        └──────────────┘                       │
│  └──────────────┘                               │                               │
│                                                 ▼                               │
│  ┌──────────────┐                        ┌──────────────┐                       │
│  │    Admin     │                        │CENTER_ADMIN  │ ← Quản trị viên       │
│  │  (KiteHub)   │                        └──────────────┘                       │
│  └──────────────┘                               │                               │
│                                                 ▼                               │
│  ┌──────────────┐                        ┌──────────────┐                       │
│  │    Agent     │                        │   TEACHER    │ ← Giáo viên           │
│  │  (Hỗ trợ)    │                        └──────────────┘                       │
│  └──────────────┘                               │                               │
│                                                 ▼                               │
│                                          ┌──────────────┐                       │
│                                          │   STUDENT    │ ← Học viên            │
│                                          └──────────────┘                       │
│                                                 │                               │
│                                                 ▼                               │
│                                          ┌──────────────┐                       │
│                                          │   PARENT     │ ← Phụ huynh ⭐ NEW    │
│                                          └──────────────┘                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 3.2. Use Cases theo Actor

### CENTER_OWNER (Chủ trung tâm)

| UC ID | Use Case | Mô tả |
|-------|----------|-------|
| UC-OWN-01 | Dashboard tổng quan | Xem doanh thu, số học viên, báo cáo |
| UC-OWN-02 | Quản lý nhân sự | Thêm/sửa/xóa Admin, Teacher |
| UC-OWN-03 | Cài đặt trung tâm | Logo, thông tin, cấu hình |
| UC-OWN-04 | Báo cáo tài chính | Công nợ, thu chi, lợi nhuận |
| UC-OWN-05 | AI Marketing | Tạo content quảng cáo tự động |

### CENTER_ADMIN (Quản trị viên)

| UC ID | Use Case | Mô tả |
|-------|----------|-------|
| UC-ADM-01 | Quản lý khóa học | CRUD khóa học, chương trình |
| UC-ADM-02 | Quản lý lớp học | Tạo lớp, xếp lịch, phân phòng |
| UC-ADM-03 | Quản lý học viên | Đăng ký, chuyển lớp, bảo lưu |
| UC-ADM-04 | Quản lý học phí | Tạo hóa đơn, thu tiền, công nợ |
| UC-ADM-05 | Gửi thông báo | Thông báo Zalo, Email, App |

### TEACHER (Giáo viên)

| UC ID | Use Case | Mô tả |
|-------|----------|-------|
| UC-TEA-01 | Xem lịch dạy | Calendar view các buổi dạy |
| UC-TEA-02 | Điểm danh | Check-in học viên từng buổi |
| UC-TEA-03 | Chấm điểm | Nhập điểm, nhận xét |
| UC-TEA-04 | Giao bài tập | Upload tài liệu, deadline |
| UC-TEA-05 | Thảo luận | Forum với học viên |

### STUDENT (Học viên)

| UC ID | Use Case | Mô tả |
|-------|----------|-------|
| UC-STU-01 | Xem lịch học | Thời khóa biểu, phòng học |
| UC-STU-02 | Xem điểm | Điểm từng môn, GPA |
| UC-STU-03 | Làm bài tập | Nộp bài online |
| UC-STU-04 | Diễn đàn | Hỏi đáp, trao đổi |
| UC-STU-05 | Điểm thưởng | Xem điểm, đổi quà |

### PARENT (Phụ huynh) ⭐ NEW

| UC ID | Use Case | Mô tả |
|-------|----------|-------|
| UC-PAR-01 | Đăng ký/Liên kết | QR Code + OTP Zalo xác thực |
| UC-PAR-02 | Xem điểm danh con | Theo từng buổi học |
| UC-PAR-03 | Xem điểm số con | Báo cáo học tập |
| UC-PAR-04 | Thanh toán học phí | QR Code VietQR |
| UC-PAR-05 | Nhận thông báo | Zalo, App push |
| UC-PAR-06 | Nhắn tin với GV | Chat trực tiếp |

---

# PHẦN 4: QUY TRÌNH MỞ NODE VỚI AI AGENT

## 4.1. Tổng quan quy trình

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    QUY TRÌNH PROVISION KITECLASS NODE                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Customer                    KiteHub                         KiteClass          │
│  ────────                    ───────                         ─────────          │
│     │                           │                               │               │
│     │  1. Đăng ký + Upload ảnh  │                               │               │
│     │ ─────────────────────────>│                               │               │
│     │                           │                               │               │
│     │                           │  2. AI Agent xử lý (30s)      │               │
│     │                           │  ┌───────────────────────┐    │               │
│     │                           │  │ • Remove background   │    │               │
│     │                           │  │ • Extract colors      │    │               │
│     │                           │  │ • Generate content    │    │               │
│     │                           │  │ • Create logos/banners│    │               │
│     │                           │  └───────────────────────┘    │               │
│     │                           │                               │               │
│     │  3. Preview & Confirm     │                               │               │
│     │ <─────────────────────────│                               │               │
│     │ ─────────────────────────>│                               │               │
│     │                           │                               │               │
│     │                           │  4. Provision (3-5 phút)      │               │
│     │                           │  ┌───────────────────────┐    │               │
│     │                           │  │ • Create Database     │────┼──> PostgreSQL │
│     │                           │  │ • Deploy Services     │────┼──> K8s Pods   │
│     │                           │  │ • Setup Domain        │────┼──> DNS        │
│     │                           │  │ • Apply Branding      │    │               │
│     │                           │  └───────────────────────┘    │               │
│     │                           │                               │               │
│     │  5. Bàn giao              │                               │               │
│     │ <─────────────────────────│                               │               │
│     │  • URL: abc.kiteclass.com │                               │               │
│     │  • Admin credentials      │                               │               │
│     │  • Tài liệu hướng dẫn     │                               │               │
│     │                           │                               │               │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 4.2. AI Agent Processing

| Bước | Công nghệ | Thời gian | Chi phí |
|------|-----------|-----------|---------|
| Remove background | Remove.bg API | 2s | $0.05 |
| Extract colors | Local (Python) | 1s | $0 |
| Generate marketing copy | OpenAI GPT-4 | 5s | $0.03 |
| Generate logos (3 versions) | Stability SDXL | 15s | $0.06 |
| Generate banners (5 sizes) | Stability SDXL | 25s | $0.05 |
| **TOTAL** | | **~30s** | **~$0.19** |

## 4.3. Infrastructure Provisioning

```yaml
# Resources được tạo cho mỗi instance:

Database:
  - PostgreSQL database riêng (isolated)
  - Initial schema migration
  - Default seed data (roles, permissions)

Kubernetes:
  - Namespace: kiteclass-{tenant}
  - Deployments: user-gateway, core, frontend
  - Services: ClusterIP internal
  - Ingress: abc.kiteclass.com
  - ConfigMaps: branding, config
  - Secrets: db credentials, api keys

Storage:
  - S3 bucket: kiteclass-{tenant}-assets
  - CDN distribution
```

---

# PHẦN 5: THIẾT KẾ DATABASE

## 5.1. Chiến lược Database

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                       DATABASE STRATEGY                                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  STRATEGY: Database-per-tenant (Complete isolation)                              │
│                                                                                  │
│  ┌────────────────────────┐                                                      │
│  │     KITEHUB DB         │  ← 1 database duy nhất cho platform                  │
│  │  ├── sales.*           │                                                      │
│  │  ├── messages.*        │                                                      │
│  │  ├── maintaining.*     │                                                      │
│  │  └── ai_agents.*       │                                                      │
│  └────────────────────────┘                                                      │
│                                                                                  │
│  ┌────────────────────────┐  ┌────────────────────────┐                         │
│  │  KITECLASS DB #1       │  │  KITECLASS DB #2       │  ...                    │
│  │  (Tenant: ABC)         │  │  (Tenant: XYZ)         │                         │
│  │  ├── users             │  │  ├── users             │                         │
│  │  ├── classes           │  │  ├── classes           │                         │
│  │  ├── invoices          │  │  ├── invoices          │                         │
│  │  └── ...               │  │  └── ...               │                         │
│  └────────────────────────┘  └────────────────────────┘                         │
│                                                                                  │
│  BENEFITS:                                                                       │
│  ✓ Complete data isolation                                                       │
│  ✓ Easy backup/restore per tenant                                                │
│  ✓ No data leakage risk                                                          │
│  ✓ Independent scaling                                                           │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 5.2. KiteHub Database Schema

| Schema | Tables | Mô tả |
|--------|--------|-------|
| **sales** | customers, pricing_plans, subscriptions, orders, payments | Quản lý bán hàng |
| **messages** | chat_sessions, chat_messages, notifications | Chat và thông báo |
| **maintaining** | instances, instance_configs, health_checks | Quản lý instances |
| **ai_agents** | ai_sessions, marketing_assets, generated_content | AI processing |

## 5.3. KiteClass Instance Database Schema

| Module | Tables | Mô tả |
|--------|--------|-------|
| **User** | users, roles, permissions, user_roles | Quản lý người dùng |
| **Class** | courses, classes, class_schedules, enrollments, rooms | Quản lý lớp học |
| **Learning** | attendance, grades, assignments, submissions | Học tập |
| **Billing** | invoices, invoice_items, payments, tuition_configs | Học phí |
| **Gamification** | point_rules, student_points, badges, rewards | Điểm thưởng |
| **Parent** | parents, parent_children, parent_notifications | Phụ huynh |
| **Forum** | forum_topics, forum_posts, forum_comments | Diễn đàn |

## 5.4. ERD tóm tắt

```
[Xem sơ đồ chi tiết: diagrams/05-erd-v3.png]

Core Entities:
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│  users ──┬──< user_roles >──── roles                                            │
│          │                                                                       │
│          ├──< enrollments >──── classes ──── courses                            │
│          │         │                │                                            │
│          │         │                └──── class_schedules                        │
│          │         │                                                             │
│          │         ├──< attendance                                               │
│          │         └──< grades                                                   │
│          │                                                                       │
│          ├──< invoices >──< invoice_items                                        │
│          │         └──< payments                                                 │
│          │                                                                       │
│          ├──< student_points                                                     │
│          └──< student_badges >──── badges                                        │
│                                                                                  │
│  parents ──< parent_children >── users (students)                               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 6: CÔNG NGHỆ SỬ DỤNG

## 6.1. Tech Stack Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           TECHNOLOGY STACK                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  BACKEND                          FRONTEND                                       │
│  ───────                          ────────                                       │
│  • Java 21                        • Next.js 14 (App Router)                     │
│  • Spring Boot 3.2                • TypeScript 5                                 │
│  • Spring Security                • TailwindCSS                                  │
│  • Spring Data JPA                • Shadcn/UI                                    │
│  • Spring WebSocket               • React Query                                  │
│                                   • Zustand                                      │
│                                                                                  │
│  DATABASE                         INFRASTRUCTURE                                 │
│  ────────                         ──────────────                                 │
│  • PostgreSQL 15                  • Docker                                       │
│  • Redis 7                        • Kubernetes                                   │
│                                   • GitHub Actions                               │
│                                   • Nginx                                        │
│                                                                                  │
│  MESSAGE QUEUE                    EXTERNAL SERVICES                              │
│  ─────────────                    ─────────────────                              │
│  • RabbitMQ 3.12                  • OpenAI GPT-4                                 │
│                                   • Stability AI (SDXL)                          │
│                                   • Remove.bg                                    │
│                                   • Zalo API                                     │
│                                   • VietQR                                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 6.2. Coding Standards

| Ngôn ngữ | Standard | Tools |
|----------|----------|-------|
| **Java** | Google Java Style Guide | Checkstyle, SpotBugs |
| **TypeScript** | Airbnb JavaScript Style | ESLint, Prettier |
| **SQL** | Snake_case conventions | Flyway migrations |
| **Git** | Conventional Commits | Husky, commitlint |

## 6.3. Design Patterns áp dụng

**Backend:**
- Repository Pattern
- Service Layer Pattern
- DTO Pattern (Request/Response)
- Factory Pattern (Payment gateways)
- Strategy Pattern (Pricing)
- Observer/Event Pattern (Domain events)

**Frontend:**
- Component Composition
- Custom Hooks Pattern
- Container/Presenter Pattern
- Compound Components

---

# PHẦN 7: KẾ HOẠCH TRIỂN KHAI

## 7.1. Timeline tổng quan (9 tháng)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         PROJECT TIMELINE - 9 THÁNG                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Phase 1: Discovery (4 tuần) ════════════════════                               │
│  ├── Khảo sát actors (Survey + Interview)                                        │
│  ├── Phân tích yêu cầu                                                           │
│  └── Finalize thiết kế                                                           │
│                                                                                  │
│  Phase 2: Foundation (8 tuần) ════════════════════════════════                  │
│  ├── Setup infrastructure                                                        │
│  ├── User + Gateway Service                                                      │
│  ├── Core Service (basic)                                                        │
│  └── Frontend skeleton                                                           │
│                                                                                  │
│  Phase 3: Core Features (12 tuần) ════════════════════════════════════════════  │
│  ├── Complete Core Service                                                       │
│  ├── KiteHub modules                                                             │
│  ├── Billing + VietQR                                                            │
│  └── Integration testing                                                         │
│                                                                                  │
│  Phase 4: Advanced (8 tuần) ════════════════════════════════                    │
│  ├── Engagement Service                                                          │
│  ├── Parent Portal                                                               │
│  ├── AI Agent integration                                                        │
│  └── Performance optimization                                                    │
│                                                                                  │
│  Phase 5: Launch (4 tuần) ════════════════════                                  │
│  ├── UAT testing                                                                 │
│  ├── Bug fixes                                                                   │
│  ├── Documentation                                                               │
│  └── Deployment                                                                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 7.2. MVP Scope (Đề xuất)

| Included in MVP | NOT in MVP |
|-----------------|------------|
| ✅ User + Gateway Service | ❌ Media Service |
| ✅ Core Service (full) | ❌ Forum |
| ✅ Billing với VietQR | ❌ Live streaming |
| ✅ Parent Portal (basic) | ❌ Gamification |
| ✅ KiteHub (full) | ❌ AI Quiz Generator |
| ✅ Frontend (all portals) | |

## 7.3. Quy trình khảo sát

```
Trước khi coding, thực hiện khảo sát theo survey-interview-plan.md:

Phase 1 (Tuần 1-2): Online Survey
├── Google Forms cho 7 actors
├── Mục tiêu: 50+ responses
└── Phân tích định lượng

Phase 2 (Tuần 3-4): In-depth Interview
├── 2-3 interviews/actor
├── Discovery → Validation
└── Phân tích định tính

Phase 3 (Tuần 4): Synthesis
├── Feature prioritization matrix
├── Điều chỉnh Use Cases
└── Finalize MVP scope
```

---

# PHẦN 8: ĐIỂM MẠNH CỦA ĐỀ TÀI

## 8.1. Giải quyết vấn đề thực tế

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         ĐIỂM MẠNH CỦA ĐỀ TÀI                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  1. GIẢI QUYẾT VẤN ĐỀ THỰC TẾ                                                    │
│     ├── Học hỏi từ BeeClass (hệ thống đang chạy thực tế)                         │
│     ├── Đáp ứng nhu cầu thị trường Việt Nam                                      │
│     ├── Parent Portal - tính năng đặc thù VN                                     │
│     └── VietQR - thanh toán phổ biến tại VN                                      │
│                                                                                  │
│  2. KIẾN TRÚC TỐI ƯU                                                             │
│     ├── Microservices đúng chỗ (KiteClass)                                       │
│     ├── Modular Monolith đúng chỗ (KiteHub)                                      │
│     ├── Không dùng Service Registry (phân tích ROI -95%)                         │
│     └── Tiết kiệm 40% RAM so với V2                                              │
│                                                                                  │
│  3. TÍCH HỢP AI SÁNG TẠO                                                         │
│     ├── AI Marketing Agent tự động tạo content                                   │
│     ├── Kết hợp GPT-4 + Stability AI + Remove.bg                                 │
│     ├── Chi phí chỉ $0.19/instance                                               │
│     └── Tiết kiệm 3-5 ngày công designer                                         │
│                                                                                  │
│  4. ĐẦY ĐỦ TÀI LIỆU                                                              │
│     ├── Architecture docs                                                        │
│     ├── Use Case specs                                                           │
│     ├── Database design                                                          │
│     ├── Survey plan                                                              │
│     ├── Development checklist                                                    │
│     └── Project schedule                                                         │
│                                                                                  │
│  5. CÔNG NGHỆ HIỆN ĐẠI                                                           │
│     ├── Java 21 + Spring Boot 3.2                                                │
│     ├── Next.js 14 (App Router)                                                  │
│     ├── Kubernetes deployment                                                    │
│     └── CI/CD với GitHub Actions                                                 │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 8.2. Khả năng mở rộng

| Tính năng | Khả năng mở rộng |
|-----------|------------------|
| **Multi-tenant** | Thêm instance không giới hạn |
| **Services** | Thêm service mới dễ dàng |
| **AI Agents** | Mở rộng sang Quiz, Tutoring |
| **Ngành nghề** | Áp dụng cho gym, spa, etc. |

---

# PHẦN 9: THỬ THÁCH CỦA ĐỀ TÀI

## 9.1. Thử thách kỹ thuật

| Thử thách | Mức độ | Giải pháp |
|-----------|--------|-----------|
| Kiến thức rộng (Backend + Frontend + DevOps + AI) | 🔴 Cao | Tập trung MVP, học dần |
| Khối lượng code lớn (~20,000+ LOC) | 🔴 Cao | Tái sử dụng code, generate boilerplate |
| Multi-tenancy complexity | 🟡 Trung bình | Database-per-tenant (đơn giản nhất) |
| Kubernetes deployment | 🟡 Trung bình | Bắt đầu với Docker Compose, lên K8s sau |
| AI integration | 🟢 Thấp | APIs đã có sẵn, chỉ cần integrate |

## 9.2. Thử thách phi kỹ thuật

| Thử thách | Mức độ | Giải pháp |
|-----------|--------|-----------|
| Thời gian 9 tháng | 🟡 Trung bình | Ưu tiên MVP, bỏ qua advanced features |
| Chi phí infrastructure | 🟡 Trung bình | Dev local, chỉ deploy khi demo |
| Khảo sát người dùng | 🟢 Thấp | Có plan chi tiết, dùng online tools |
| Testing đầy đủ | 🟡 Trung bình | Focus unit + integration tests |

## 9.3. Risk Mitigation

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         RISK MITIGATION STRATEGIES                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  RISK: Không kịp deadline                                                        │
│  MITIGATION:                                                                     │
│  ├── Define MVP scope rõ ràng                                                    │
│  ├── Có fallback plan cho mỗi phase                                              │
│  └── Weekly progress check                                                       │
│                                                                                  │
│  RISK: Kiến thức chưa đủ                                                         │
│  MITIGATION:                                                                     │
│  ├── Learning roadmap trong required-knowledge.md                                │
│  ├── Code samples và examples                                                    │
│  └── Tập trung công nghệ core trước                                              │
│                                                                                  │
│  RISK: Khảo sát không đủ người                                                   │
│  MITIGATION:                                                                     │
│  ├── Multiple channels (Facebook groups, forums)                                 │
│  ├── Incentives cho participants                                                 │
│  └── Fallback: Dùng BeeClass data làm reference                                  │
│                                                                                  │
│  RISK: Infrastructure costs vượt budget                                          │
│  MITIGATION:                                                                     │
│  ├── Dev trên local Docker                                                       │
│  ├── Free tier services khi có thể                                               │
│  └── Chỉ deploy cloud khi demo/present                                           │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# KẾT LUẬN

Đề tài **"Xây dựng hệ thống quản lý trung tâm giáo dục theo kiến trúc Microservices - KiteClass Platform V3.1"** là một đồ án tốt nghiệp có tính thực tiễn cao với các điểm nổi bật:

1. **Giải quyết vấn đề thực tế**: Học hỏi từ BeeClass, đáp ứng nhu cầu quản lý trung tâm giáo dục tại Việt Nam

2. **Kiến trúc tối ưu**: Kết hợp Microservices và Modular Monolith đúng chỗ, phân tích ROI để loại bỏ Service Registry không cần thiết

3. **Tích hợp AI sáng tạo**: AI Agent tự động tạo thương hiệu với chi phí chỉ $0.19/instance

4. **Tính năng đặc thù Việt Nam**: Parent Portal, VietQR payment, Zalo OTP

5. **Đầy đủ chuẩn bị**: Architecture docs, Use Cases, Database design, Survey plan, Development checklist, Project schedule, Diagrams

Với 9 tháng triển khai, đề tài tập trung vào MVP với các core features, có khả năng mở rộng sau khi hoàn thành. Các thử thách được nhận diện rõ ràng với mitigation strategies cụ thể.

Đề tài thể hiện khả năng:
- Phân tích và thiết kế hệ thống phức tạp
- Lựa chọn công nghệ và kiến trúc phù hợp
- Tích hợp AI vào ứng dụng thực tế
- Lập kế hoạch và quản lý dự án

---

# PHỤ LỤC

## A. Danh sách tài liệu

| # | Tài liệu | Đường dẫn |
|---|----------|-----------|
| 1 | Kiến trúc hệ thống V3.1 | documents/reports/system-architecture-v3-final.md |
| 2 | Use Cases theo Service | documents/reports/service-use-cases-v3.md |
| 3 | Database Design | documents/plans/database-design.md |
| 4 | Survey & Interview Plan | documents/plans/survey-interview-plan.md |
| 5 | Required Knowledge | documents/plans/required-knowledge.md |
| 6 | Project Schedule | documents/plans/project-schedule.md |
| 7 | Feature Development Checklist | documents/plans/feature-development-checklist.md |
| 8 | Service Registry Analysis | documents/reports/service-registry-analysis.md |

## B. Danh sách Diagrams

| # | Diagram | Đường dẫn |
|---|---------|-----------|
| 1 | Architecture Simple | diagrams/01-architecture-simple.puml |
| 2 | BFD Actors | diagrams/02-bfd-actors.puml |
| 3 | ERD | diagrams/03-erd.puml |
| 4 | Architecture Full | diagrams/04-architecture-full.puml |
| 5 | System Overview | diagrams/05-system-overview-v3.puml |
| 6 | Business Flow | diagrams/06-business-flow-v3.puml |

## C. Tham khảo

- BeeClass Platform (https://beeclass.net) - Học hỏi tính năng
- Google Java Style Guide - Coding standards
- Airbnb JavaScript Style Guide - Frontend standards
- OWASP Top 10 - Security checklist

---

*Tài liệu được tạo bởi: KiteClass Team*
*Phiên bản: 3.1 (Final)*
*Ngày cập nhật: 23/12/2025*
