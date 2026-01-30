# BÁO CÁO PHÂN TÍCH TỐI ƯU KIẾN TRÚC KITECLASS INSTANCE

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Tên dự án** | KiteClass Platform V3 |
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 23/12/2025 |
| **Loại tài liệu** | Báo cáo phân tích kiến trúc |
| **Tham chiếu** | system-architecture-v3-final.md, service-optimization-report.md |

---

# MỤC LỤC

1. [Phân tích Gateway vs User Service](#phần-1-phân-tích-gateway-vs-user-service)
2. [Phân tích Core Service và Extend Services](#phần-2-phân-tích-core-service-và-extend-services)
3. [Đề xuất kiến trúc tối ưu](#phần-3-đề-xuất-kiến-trúc-tối-ưu)
4. [Kết luận và khuyến nghị](#phần-4-kết-luận-và-khuyến-nghị)

---

# PHẦN 1: PHÂN TÍCH GATEWAY VS USER SERVICE

## 1.1. Kiến trúc hiện tại (4 Services)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         KITECLASS INSTANCE HIỆN TẠI                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                         API GATEWAY                                         │ │
│  │                    (Spring Cloud Gateway)                                   │ │
│  │                                                                             │ │
│  │  Chức năng:                                                                 │ │
│  │  • JWT Validation (verify signature, check expiry)                         │ │
│  │  • Rate Limiting (Redis counter)                                           │ │
│  │  • CORS handling                                                            │ │
│  │  • Request Routing                                                          │ │
│  │  • Load Balancing (nếu có multiple instances)                              │ │
│  │                                                                             │ │
│  │  Resources: ~256MB RAM, ~0.25 vCPU                                         │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                       │                                         │
│         ┌─────────────────────────────┼─────────────────────────────┐           │
│         │                             │                             │           │
│         ▼                             ▼                             ▼           │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐  │
│  │    USER SERVICE      │  │    CORE SERVICE      │  │    MEDIA SERVICE     │  │
│  │  (Java Spring Boot)  │  │  (Java Spring Boot)  │  │     (Node.js)        │  │
│  │                      │  │                      │  │                      │  │
│  │  • Authentication    │  │  • Class Module      │  │  • Video upload      │  │
│  │  • Token generation  │  │  • Learning Module   │  │  • Transcoding       │  │
│  │  • User CRUD         │  │  • Billing Module    │  │  • Live streaming    │  │
│  │  • Role management   │  │  • Gamification      │  │  • WebSocket         │  │
│  │  • OAuth integration │  │  • Parent Module     │  │                      │  │
│  │                      │  │  • Forum Module      │  │                      │  │
│  │  ~512MB RAM          │  │  ~1GB RAM            │  │  ~512MB RAM          │  │
│  │  ~0.5 vCPU           │  │  ~1 vCPU             │  │  ~0.5 vCPU           │  │
│  └──────────────────────┘  └──────────────────────┘  └──────────────────────┘  │
│                                                                                  │
│  TỔNG RESOURCES: ~2.25GB RAM, ~2.25 vCPU (không kể Frontend, DB, Redis)        │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 1.2. So sánh Gateway vs User Service

### Bảng so sánh chức năng

| Chức năng | API Gateway | User Service | Trùng lặp? |
|-----------|:-----------:|:------------:|:----------:|
| **Authentication** |
| JWT Signature Verification | ✅ | ✅ | ⚠️ Có |
| Token Expiry Check | ✅ | ✅ | ⚠️ Có |
| Token Generation | ❌ | ✅ | - |
| Refresh Token Handling | ❌ | ✅ | - |
| **Authorization** |
| Extract User Claims | ✅ | ✅ | ⚠️ Có |
| Permission Check | ❌ | ✅ | - |
| Role-based Access | ❌ | ✅ | - |
| **User Management** |
| User CRUD | ❌ | ✅ | - |
| Password Management | ❌ | ✅ | - |
| OAuth Integration | ❌ | ✅ | - |
| **Routing & Traffic** |
| Request Routing | ✅ | ❌ | - |
| Rate Limiting | ✅ | ❌ | - |
| CORS Handling | ✅ | ❌ | - |
| Load Balancing | ✅ | ❌ | - |

### Phân tích trùng lặp

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         LUỒNG XỬ LÝ JWT HIỆN TẠI                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Request với JWT Token                                                          │
│         │                                                                        │
│         ▼                                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  GATEWAY: JWT Validation (LẦN 1)                                         │   │
│  │  • Verify signature với public key                                        │   │
│  │  • Check expiry                                                           │   │
│  │  • Extract claims                                                         │   │
│  │  • Inject X-User-Id, X-User-Roles headers                                │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│         │                                                                        │
│         ▼                                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  CORE/USER SERVICE: Security Filter (LẦN 2?)                             │   │
│  │                                                                           │   │
│  │  Có 2 cách tiếp cận:                                                     │   │
│  │                                                                           │   │
│  │  [A] Trust Gateway Headers (Hiện tại đề xuất)                            │   │
│  │      • Đọc X-User-Id, X-User-Roles từ headers                            │   │
│  │      • Không verify JWT lại                                               │   │
│  │      → ✅ Không trùng lặp, nhưng phụ thuộc vào Gateway                   │   │
│  │                                                                           │   │
│  │  [B] Verify JWT lại (Bảo mật hơn)                                        │   │
│  │      • Verify signature lần nữa                                           │   │
│  │      → ⚠️ Trùng lặp xử lý                                                │   │
│  │                                                                           │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 1.3. Đánh giá tính cần thiết của Gateway

### Lợi ích của Gateway

| Lợi ích | Mô tả | Đánh giá |
|---------|-------|:--------:|
| **Centralized Auth** | Một điểm xác thực duy nhất | ⭐⭐⭐ Quan trọng |
| **Rate Limiting** | Bảo vệ khỏi DDoS/abuse | ⭐⭐⭐ Quan trọng |
| **CORS** | Xử lý tập trung | ⭐⭐ Trung bình |
| **Routing** | Định tuyến đến services | ⭐⭐ Trung bình |
| **Load Balancing** | Phân tải | ⭐ Không cần (1 instance/service) |

### Chi phí của Gateway

| Chi phí | Mô tả | Mức độ |
|---------|-------|:------:|
| **RAM** | ~256MB thêm cho mỗi instance | ⭐⭐ Trung bình |
| **CPU** | ~0.25 vCPU overhead | ⭐ Thấp |
| **Latency** | Thêm 1-5ms mỗi request | ⭐ Thấp |
| **Complexity** | Thêm 1 service cần maintain | ⭐⭐ Trung bình |
| **Startup Time** | Thêm thời gian khởi động | ⭐ Thấp |

## 1.4. Phương án tối ưu Gateway

### Phương án A: Giữ nguyên Gateway (Khuyến nghị)

```
Ưu điểm:
✅ Tách biệt concerns rõ ràng
✅ Rate limiting tập trung
✅ Dễ scale từng phần
✅ Security defense-in-depth

Nhược điểm:
❌ Thêm resource overhead
❌ Thêm latency nhỏ
```

### Phương án B: Merge Gateway vào User Service

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PHƯƠNG ÁN B: USER SERVICE + GATEWAY                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                    USER SERVICE (Enhanced)                                  │ │
│  │                    Java Spring Boot + Spring Cloud Gateway                  │ │
│  │                                                                             │ │
│  │  ┌──────────────────────────────────────────────────────────────────────┐  │ │
│  │  │  Gateway Layer (embedded)                                            │  │ │
│  │  │  • Rate Limiting Filter                                              │  │ │
│  │  │  • CORS Filter                                                       │  │ │
│  │  │  • Routing to Core/Media                                             │  │ │
│  │  └──────────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                             │ │
│  │  ┌──────────────────────────────────────────────────────────────────────┐  │ │
│  │  │  Auth Layer                                                          │  │ │
│  │  │  • JWT Generation/Validation                                         │  │ │
│  │  │  • OAuth Integration                                                 │  │ │
│  │  │  • Session Management                                                │  │ │
│  │  └──────────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                             │ │
│  │  ┌──────────────────────────────────────────────────────────────────────┐  │ │
│  │  │  User Management Layer                                               │  │ │
│  │  │  • User CRUD                                                         │  │ │
│  │  │  • Role/Permission                                                   │  │ │
│  │  └──────────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                             │ │
│  │  Resources: ~640MB RAM (256+512 tiết kiệm ~128MB), ~0.5 vCPU              │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  TIẾT KIỆM: ~128MB RAM, ~0.25 vCPU, giảm 1 container                           │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Đánh giá Phương án B:**

| Tiêu chí | Đánh giá | Ghi chú |
|----------|:--------:|---------|
| Tiết kiệm resource | ⭐⭐ | ~128MB RAM, ~0.25 vCPU |
| Giảm complexity | ⭐⭐ | Ít container hơn |
| Separation of concerns | ⭐ | Giảm, nhưng chấp nhận được |
| Maintainability | ⭐⭐ | User Service lớn hơn nhưng logic gần nhau |

### Phương án C: Không dùng Gateway (Frontend gọi thẳng)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PHƯƠNG ÁN C: KHÔNG GATEWAY                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                            FRONTEND (Next.js)                                   │
│                                   │                                              │
│         ┌─────────────────────────┼─────────────────────────┐                   │
│         │                         │                         │                   │
│         ▼                         ▼                         ▼                   │
│  ┌──────────────┐         ┌──────────────┐         ┌──────────────┐            │
│  │ USER SERVICE │         │ CORE SERVICE │         │MEDIA SERVICE │            │
│  │              │         │              │         │              │            │
│  │ /api/auth/* │         │ /api/core/* │         │ /api/media/* │            │
│  │              │         │              │         │              │            │
│  │ JWT Verify   │         │ JWT Verify   │         │ JWT Verify   │            │
│  │ Rate Limit   │         │ Rate Limit   │         │ Rate Limit   │            │
│  └──────────────┘         └──────────────┘         └──────────────┘            │
│                                                                                  │
│  ⚠️ Vấn đề:                                                                     │
│  • Mỗi service phải tự implement JWT verify → Trùng lặp code                   │
│  • Mỗi service phải tự implement Rate Limiting → Phức tạp                      │
│  • CORS phải configure ở mỗi service                                           │
│  • Frontend phải biết địa chỉ của từng service                                 │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Đánh giá Phương án C:**

| Tiêu chí | Đánh giá | Ghi chú |
|----------|:--------:|---------|
| Tiết kiệm resource | ⭐⭐⭐ | Không có Gateway |
| Giảm complexity | ⭐ | TĂNG complexity ở mỗi service |
| Code duplication | ❌ | JWT verify, rate limit ở 3 nơi |
| Maintainability | ❌ | Khó maintain, dễ inconsistent |

## 1.5. Kết luận Gateway

| Phương án | Resource | Complexity | Recommend |
|-----------|:--------:|:----------:|:---------:|
| **A: Giữ Gateway riêng** | ~256MB | Trung bình | ⭐⭐⭐ Nếu cần scale |
| **B: Merge vào User Service** | Tiết kiệm ~128MB | Thấp hơn | ⭐⭐⭐ **Khuyến nghị cho SMB** |
| **C: Không Gateway** | Tiết kiệm ~256MB | CAO (duplicate code) | ❌ Không khuyến nghị |

**Khuyến nghị: Phương án B** cho các trung tâm nhỏ-vừa (< 500 học viên) để:
- Tiết kiệm resource
- Giảm số container cần quản lý
- Vẫn có đầy đủ chức năng Gateway

---

# PHẦN 2: PHÂN TÍCH CORE SERVICE VÀ EXTEND SERVICES

## 2.1. Kiến trúc hiện tại

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    KIẾN TRÚC HIỆN TẠI: 1 CORE + 1 EXTEND                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                         CORE SERVICE (Bắt buộc)                             │ │
│  │                                                                             │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │ │
│  │  │    Class    │  │   Learning  │  │   Billing   │  │Gamification │        │ │
│  │  │   Module    │  │   Module    │  │   Module    │  │   Module    │        │ │
│  │  │             │  │             │  │             │  │             │        │ │
│  │  │ • Khóa học  │  │ • Điểm danh │  │ • Học phí   │  │ • Điểm      │        │ │
│  │  │ • Lớp học   │  │ • Bài tập   │  │ • Hóa đơn   │  │ • Huy hiệu  │        │ │
│  │  │ • Lịch học  │  │ • Điểm số   │  │ • Công nợ   │  │ • Đổi quà   │        │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │ │
│  │                                                                             │ │
│  │  ┌─────────────┐  ┌─────────────┐                                          │ │
│  │  │   Parent    │  │    Forum    │                                          │ │
│  │  │   Module    │  │   Module    │                                          │ │
│  │  │             │  │             │                                          │ │
│  │  │ • Portal    │  │ • Hỏi đáp   │                                          │ │
│  │  │ • Thông báo │  │ • Thảo luận │                                          │ │
│  │  └─────────────┘  └─────────────┘                                          │ │
│  │                                                                             │ │
│  │  Resources: ~1GB RAM, ~1 vCPU                                              │ │
│  │  LUÔN PHẢI CHẠY cho mọi gói dịch vụ                                        │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                       MEDIA SERVICE (Tùy chọn)                              │ │
│  │                                                                             │ │
│  │  • Video upload & transcode                                                 │ │
│  │  • Live streaming (WebSocket)                                               │ │
│  │  • Recording                                                                │ │
│  │                                                                             │ │
│  │  Resources: ~512MB RAM, ~0.5 vCPU                                          │ │
│  │  CHỈ CHẠY nếu gói có Live/Video feature                                    │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.2. Phân tích tải trọng Core Service

### Vấn đề hiện tại

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         VẤN ĐỀ TẢI TRỌNG CORE SERVICE                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  CORE SERVICE chứa 6 modules:                                                   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                                                                          │    │
│  │    Class ──────┐                                                        │    │
│  │                │                                                        │    │
│  │  Learning ─────┼───────┐                                                │    │
│  │                │       │                                                │    │
│  │  Billing ──────┼───────┼───────┐                                        │    │
│  │                │       │       │                                        │    │
│  │  Gamification ─┼───────┼───────┼───────┐                                │    │
│  │                │       │       │       │                                │    │
│  │  Parent ───────┼───────┼───────┼───────┼───────┐                        │    │
│  │                │       │       │       │       │                        │    │
│  │  Forum ────────┼───────┼───────┼───────┼───────┼───────┐                │    │
│  │                │       │       │       │       │       │                │    │
│  │                ▼       ▼       ▼       ▼       ▼       ▼                │    │
│  │         ┌──────────────────────────────────────────────────┐            │    │
│  │         │            SHARED RESOURCES                       │            │    │
│  │         │  • Database Connection Pool (shared)              │            │    │
│  │         │  • Redis Connection (shared)                      │            │    │
│  │         │  • Thread Pool (shared)                           │            │    │
│  │         │  • Memory Heap (shared)                           │            │    │
│  │         └──────────────────────────────────────────────────┘            │    │
│  │                                                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  ⚠️ Vấn đề:                                                                     │
│  1. TẤT CẢ modules chia sẻ cùng resources → Một module bận có thể ảnh hưởng   │
│     các module khác                                                            │
│  2. Không thể scale riêng từng module                                          │
│  3. Khách hàng gói BASIC vẫn phải load toàn bộ modules dù không dùng hết      │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Phân tích mức độ sử dụng theo gói

| Module | BASIC | STANDARD | PREMIUM | Tần suất sử dụng |
|--------|:-----:|:--------:|:-------:|:----------------:|
| Class | ✅ | ✅ | ✅ | Cao |
| Learning | ✅ | ✅ | ✅ | Cao |
| Billing | ✅ | ✅ | ✅ | Trung bình |
| Gamification | ❌ | ✅ | ✅ | Thấp-Trung bình |
| Parent | ❌ | ✅ | ✅ | Trung bình |
| Forum | ❌ | ❌ | ✅ | Thấp |
| **Media (extend)** | ❌ | ✅ (limited) | ✅ (full) | Cao khi dùng |

### Đánh giá tải trọng thực tế

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         ƯỚC TÍNH TẢI TRỌNG THEO GÓI                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  GÓI BASIC (≤50 học viên, không có Gamification/Parent/Forum/Media)            │
│  ─────────────────────────────────────────────────────────────────             │
│                                                                                  │
│  Modules sử dụng: Class, Learning, Billing                                      │
│  Modules không dùng: Gamification, Parent, Forum                                │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │  Core Service Load:                                                        │ │
│  │                                                                            │ │
│  │  [████████████░░░░░░░░] 60% (~600MB used of 1GB allocated)                │ │
│  │                                                                            │ │
│  │  → 40% resources LÃNG PHÍ cho modules không dùng                          │ │
│  │                                                                            │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  GÓI STANDARD (≤200 học viên, có Gamification/Parent, có Media limited)        │
│  ─────────────────────────────────────────────────────────────────             │
│                                                                                  │
│  Modules sử dụng: Class, Learning, Billing, Gamification, Parent               │
│  Modules không dùng: Forum                                                      │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │  Core Service Load:                                                        │ │
│  │                                                                            │ │
│  │  [████████████████░░░░] 80% (~800MB used of 1GB allocated)                │ │
│  │                                                                            │ │
│  │  + Media Service: ~512MB                                                   │ │
│  │                                                                            │ │
│  │  → 20% resources cho Forum không dùng                                      │ │
│  │                                                                            │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  GÓI PREMIUM (Unlimited, full features)                                        │
│  ─────────────────────────────────────                                         │
│                                                                                  │
│  Modules sử dụng: TẤT CẢ                                                       │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │  Core Service Load:                                                        │ │
│  │                                                                            │ │
│  │  [████████████████████] 100% (~1GB allocated)                             │ │
│  │                                                                            │ │
│  │  + Media Service: ~512MB (full features)                                   │ │
│  │                                                                            │ │
│  │  → Sử dụng hiệu quả                                                        │ │
│  │                                                                            │ │
│  └───────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.3. Phân tích tính cơ động (Extend Services)

### Vấn đề hiện tại: Chỉ có 1 Extend Service

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    VẤN ĐỀ TÍNH CƠ ĐỘNG HIỆN TẠI                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Khi khách hàng đăng ký gói:                                                    │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                        MÀN HÌNH CHỌN GÓI                                 │    │
│  │                                                                          │    │
│  │   BASIC           STANDARD         PREMIUM                               │    │
│  │   500k/th         1.5tr/th         3tr/th                               │    │
│  │                                                                          │    │
│  │   ✅ Quản lý lớp   ✅ Quản lý lớp    ✅ Quản lý lớp                       │    │
│  │   ✅ Điểm danh     ✅ Điểm danh      ✅ Điểm danh                         │    │
│  │   ✅ Học phí       ✅ Học phí        ✅ Học phí                           │    │
│  │   ❌ Gamification  ✅ Gamification   ✅ Gamification                      │    │
│  │   ❌ Parent Portal ✅ Parent Portal  ✅ Parent Portal                     │    │
│  │   ❌ Forum         ❌ Forum          ✅ Forum                              │    │
│  │   ❌ Video/Live    ✅ Video (limited)✅ Video (unlimited)                 │    │
│  │                                                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  ⚠️ VẤN ĐỀ:                                                                     │
│                                                                                  │
│  1. KHÔNG THỂ chọn riêng lẻ tính năng                                          │
│     → Khách muốn BASIC + Gamification phải mua STANDARD                        │
│     → Khách muốn BASIC + Video phải mua STANDARD                               │
│                                                                                  │
│  2. Core Service LUÔN PHẢI LOAD toàn bộ modules                                │
│     → Gói BASIC vẫn có code Gamification, Parent, Forum trong memory           │
│                                                                                  │
│  3. Chỉ có 1 extend service (Media)                                            │
│     → Không có Gamification Service riêng                                      │
│     → Không có Parent Service riêng                                            │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.4. Các phương án tối ưu

### Phương án A: Giữ nguyên (Hiện tại)

```
Core Service (all modules) + Media Service (optional)

✅ Ưu điểm:
- Đơn giản, ít container
- Dễ deploy, maintain
- Communication giữa modules nhanh (in-process)

❌ Nhược điểm:
- Lãng phí resource cho gói thấp
- Không linh hoạt pricing
- Một module lỗi ảnh hưởng toàn bộ
```

### Phương án B: Module Flags (Feature Toggle)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PHƯƠNG ÁN B: CORE SERVICE VỚI FEATURE FLAGS                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                         CORE SERVICE                                        │ │
│  │                                                                             │ │
│  │  application.yml:                                                          │ │
│  │  ┌──────────────────────────────────────────────────────────────────────┐  │ │
│  │  │  features:                                                            │  │ │
│  │  │    gamification:                                                      │  │ │
│  │  │      enabled: ${FEATURE_GAMIFICATION:false}                          │  │ │
│  │  │    parent:                                                            │  │ │
│  │  │      enabled: ${FEATURE_PARENT:false}                                │  │ │
│  │  │    forum:                                                             │  │ │
│  │  │      enabled: ${FEATURE_FORUM:false}                                 │  │ │
│  │  └──────────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                             │ │
│  │  @ConditionalOnProperty(name = "features.gamification.enabled")           │ │
│  │  @Configuration                                                            │ │
│  │  public class GamificationConfig { ... }                                   │ │
│  │                                                                             │ │
│  │  → Module code vẫn trong JAR nhưng KHÔNG LOAD nếu disabled                │ │
│  │  → Tiết kiệm một phần memory (beans không được khởi tạo)                  │ │
│  │  → Nhưng class files vẫn trong classpath                                   │ │
│  │                                                                             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  Provisioning theo gói:                                                         │
│                                                                                  │
│  BASIC:    FEATURE_GAMIFICATION=false, FEATURE_PARENT=false, FEATURE_FORUM=false│
│  STANDARD: FEATURE_GAMIFICATION=true,  FEATURE_PARENT=true,  FEATURE_FORUM=false│
│  PREMIUM:  FEATURE_GAMIFICATION=true,  FEATURE_PARENT=true,  FEATURE_FORUM=true │
│                                                                                  │
│  ✅ Ưu điểm:                                                                    │
│  - Giảm memory usage (~10-20% cho disabled modules)                            │
│  - Cùng 1 Docker image cho tất cả gói                                          │
│  - Linh hoạt enable/disable runtime                                            │
│                                                                                  │
│  ❌ Nhược điểm:                                                                 │
│  - JAR size không giảm                                                          │
│  - Vẫn cùng 1 process, không thể scale riêng                                   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Phương án C: Tách thêm Extend Services

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PHƯƠNG ÁN C: MULTIPLE EXTEND SERVICES                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                    CORE SERVICE (Slim - Bắt buộc)                           │ │
│  │                                                                             │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                         │ │
│  │  │    Class    │  │   Learning  │  │   Billing   │                         │ │
│  │  │   Module    │  │   Module    │  │   Module    │                         │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                         │ │
│  │                                                                             │ │
│  │  Resources: ~512MB RAM (giảm từ 1GB)                                       │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│         │                                                                        │
│         │ RabbitMQ / REST                                                       │
│         │                                                                        │
│  ┌──────┴──────┬──────────────┬──────────────┬──────────────┐                   │
│  │             │              │              │              │                   │
│  ▼             ▼              ▼              ▼              ▼                   │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐                   │
│  │ MEDIA   │ │GAMIFICAT│ │ PARENT  │ │  FORUM  │ │NOTIFICAT│                   │
│  │ SERVICE │ │  ION    │ │ SERVICE │ │ SERVICE │ │  ION    │                   │
│  │         │ │ SERVICE │ │         │ │         │ │ SERVICE │                   │
│  │ ~512MB  │ │ ~256MB  │ │ ~256MB  │ │ ~256MB  │ │ ~128MB  │                   │
│  │ Node.js │ │  Java   │ │  Java   │ │  Java   │ │  Java   │                   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘                   │
│                                                                                  │
│  Khách hàng có thể chọn:                                                        │
│  • Core only: 512MB                                                             │
│  • Core + Gamification: 768MB                                                   │
│  • Core + Parent: 768MB                                                         │
│  • Core + Media: 1024MB                                                         │
│  • Core + Gamification + Parent + Media: 1536MB                                 │
│  • Full (all services): 1920MB                                                  │
│                                                                                  │
│  ⚠️ VẤN ĐỀ LỚN:                                                                 │
│  - QUÁ NHIỀU containers (6 services)                                           │
│  - Inter-service communication overhead                                         │
│  - Phức tạp deployment                                                          │
│  - Chi phí Kubernetes cao hơn                                                   │
│  - Distributed transactions phức tạp                                           │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Phương án D: Hybrid - Core + 2 Extend Services (Khuyến nghị)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PHƯƠNG ÁN D: CORE + 2 EXTEND SERVICES                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Nguyên tắc phân chia:                                                          │
│  • Core: Chức năng CỐT LÕI, mọi trung tâm đều cần                              │
│  • Engagement Service: Tăng tương tác (tùy chọn)                               │
│  • Media Service: Video/Streaming (tùy chọn, resource-intensive)               │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                    CORE SERVICE (Bắt buộc)                                  │ │
│  │                                                                             │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                         │ │
│  │  │    Class    │  │   Learning  │  │   Billing   │                         │ │
│  │  │   Module    │  │   Module    │  │   Module    │                         │ │
│  │  │             │  │             │  │             │                         │ │
│  │  │ • Khóa học  │  │ • Điểm danh │  │ • Học phí   │                         │ │
│  │  │ • Lớp học   │  │ • Bài tập   │  │ • Hóa đơn   │                         │ │
│  │  │ • Lịch học  │  │ • Điểm số   │  │ • Công nợ   │                         │ │
│  │  │ • Học viên  │  │             │  │ • QR Pay    │                         │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                         │ │
│  │                                                                             │ │
│  │  Resources: ~768MB RAM, ~0.75 vCPU                                         │ │
│  │  Công nghệ: Java Spring Boot                                               │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────────────┐   │
│  │   ENGAGEMENT SERVICE (Tùy chọn) │  │     MEDIA SERVICE (Tùy chọn)        │   │
│  │                                 │  │                                      │   │
│  │  ┌───────────┐  ┌───────────┐   │  │  • Video upload                     │   │
│  │  │Gamification│ │  Parent   │   │  │  • Transcoding                      │   │
│  │  │           │  │  Portal   │   │  │  • Live streaming                   │   │
│  │  │ • Điểm    │  │           │   │  │  • WebSocket                        │   │
│  │  │ • Huy hiệu│  │ • Portal  │   │  │  • Recording                        │   │
│  │  │ • Đổi quà │  │ • Notify  │   │  │                                      │   │
│  │  │ • Ranking │  │ • Reports │   │  │                                      │   │
│  │  └───────────┘  └───────────┘   │  │                                      │   │
│  │                                 │  │                                      │   │
│  │  ┌───────────┐                  │  │                                      │   │
│  │  │   Forum   │                  │  │                                      │   │
│  │  │  Module   │                  │  │                                      │   │
│  │  └───────────┘                  │  │                                      │   │
│  │                                 │  │                                      │   │
│  │  Resources: ~384MB RAM          │  │  Resources: ~512MB RAM               │   │
│  │  Công nghệ: Java Spring Boot    │  │  Công nghệ: Node.js                  │   │
│  └─────────────────────────────────┘  └─────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Bảng cấu hình theo gói - Phương án D

| Gói | Core Service | Engagement Service | Media Service | Tổng RAM |
|-----|:------------:|:------------------:|:-------------:|:--------:|
| **BASIC** | ✅ ~768MB | ❌ | ❌ | ~768MB |
| **STANDARD** | ✅ ~768MB | ✅ ~384MB | ✅ ~512MB (limited) | ~1.6GB |
| **PREMIUM** | ✅ ~768MB | ✅ ~384MB | ✅ ~512MB (full) | ~1.6GB |
| **CUSTOM** | ✅ ~768MB | Tùy chọn | Tùy chọn | Linh hoạt |

### Pricing linh hoạt với Phương án D

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         BẢNG GIÁ LINH HOẠT                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                    CHỌN GÓI CƠ BẢN                                      │   │
│   │                                                                          │   │
│   │    BASIC              STANDARD           PREMIUM                        │   │
│   │    500k/th            1tr/th             2tr/th                         │   │
│   │    50 HV              200 HV             Unlimited                      │   │
│   │                                                                          │   │
│   │    ✅ Core features   ✅ Core features   ✅ Core features               │   │
│   │    ✅ Class/Learning  ✅ Class/Learning  ✅ Class/Learning              │   │
│   │    ✅ Billing         ✅ Billing         ✅ Billing                      │   │
│   │                                                                          │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                    THÊM TÍNH NĂNG (Add-ons)                             │   │
│   │                                                                          │   │
│   │    □ ENGAGEMENT PACK              □ MEDIA PACK                          │   │
│   │      +300k/th                       +500k/th                            │   │
│   │                                                                          │   │
│   │      ✅ Gamification                ✅ Video upload (50GB)              │   │
│   │      ✅ Parent Portal               ✅ Video transcode                  │   │
│   │      ✅ Forum                       ✅ Live streaming                   │   │
│   │      ✅ Zalo notifications          ✅ Recording                        │   │
│   │                                                                          │   │
│   └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│   Ví dụ:                                                                        │
│   • BASIC + Engagement = 500k + 300k = 800k/th                                 │
│   • STANDARD + Media = 1tr + 500k = 1.5tr/th                                   │
│   • PREMIUM + Engagement + Media = 2tr + 300k + 500k = 2.8tr/th                │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.5. So sánh các phương án

| Tiêu chí | A: Hiện tại | B: Feature Flags | C: Multi-extend | D: Hybrid (2 extend) |
|----------|:-----------:|:----------------:|:---------------:|:--------------------:|
| **Số services** | 3-4 | 3-4 | 6-7 | 4-5 |
| **RAM BASIC** | ~2.25GB | ~2GB | ~1.2GB | ~1GB |
| **RAM PREMIUM** | ~2.25GB | ~2.25GB | ~2GB | ~1.9GB |
| **Linh hoạt pricing** | ⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Độ phức tạp deploy** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐ | ⭐⭐ |
| **Maintainability** | ⭐⭐⭐ | ⭐⭐ | ⭐ | ⭐⭐ |
| **Scale từng phần** | ⭐ | ⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Inter-service overhead** | Thấp | Thấp | Cao | Trung bình |

---

# PHẦN 3: ĐỀ XUẤT KIẾN TRÚC TỐI ƯU

## 3.1. Kiến trúc đề xuất: Kết hợp Phương án B + D

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    KIẾN TRÚC TỐI ƯU ĐỀ XUẤT                                      │
│                    (Gateway merged + 2 Extend Services)                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                              FRONTEND (Next.js)                                 │
│                                     │                                            │
│                                     ▼                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                    USER & GATEWAY SERVICE (Merged)                          │ │
│  │                         Java Spring Boot                                    │ │
│  │                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────┐   │ │
│  │  │  GATEWAY LAYER                                                       │   │ │
│  │  │  • Rate Limiting (Redis)                                             │   │ │
│  │  │  • CORS                                                              │   │ │
│  │  │  • Request Routing to Core/Engagement/Media                          │   │ │
│  │  └─────────────────────────────────────────────────────────────────────┘   │ │
│  │                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────┐   │ │
│  │  │  AUTH LAYER                                                          │   │ │
│  │  │  • JWT Generation & Validation                                       │   │ │
│  │  │  • OAuth 2.0 (Google, Facebook)                                      │   │ │
│  │  │  • Session Management                                                │   │ │
│  │  └─────────────────────────────────────────────────────────────────────┘   │ │
│  │                                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────────────┐   │ │
│  │  │  USER MANAGEMENT                                                     │   │ │
│  │  │  • User CRUD                                                         │   │ │
│  │  │  • Role & Permission                                                 │   │ │
│  │  │  • Multi-tenant isolation                                            │   │ │
│  │  └─────────────────────────────────────────────────────────────────────┘   │ │
│  │                                                                             │ │
│  │  Resources: ~640MB RAM, ~0.5 vCPU                                          │ │
│  │  LUÔN CHẠY                                                                 │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                       │                                         │
│         ┌─────────────────────────────┼─────────────────────────────┐           │
│         │                             │                             │           │
│         ▼                             ▼                             ▼           │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐  │
│  │    CORE SERVICE      │  │  ENGAGEMENT SERVICE  │  │    MEDIA SERVICE     │  │
│  │  (Bắt buộc)          │  │     (Tùy chọn)       │  │     (Tùy chọn)       │  │
│  │                      │  │                      │  │                      │  │
│  │  ┌────────────────┐  │  │  ┌────────────────┐  │  │  • Video upload      │  │
│  │  │ Class Module   │  │  │  │ Gamification   │  │  │  • Transcoding       │  │
│  │  ├────────────────┤  │  │  │ • Points       │  │  │  • Live streaming    │  │
│  │  │ Learning Module│  │  │  │ • Badges       │  │  │  • WebSocket         │  │
│  │  ├────────────────┤  │  │  │ • Rewards      │  │  │  • Recording         │  │
│  │  │ Billing Module │  │  │  │ • Leaderboard  │  │  │                      │  │
│  │  │ (+ Feature     │  │  │  ├────────────────┤  │  │                      │  │
│  │  │  Flags cho     │  │  │  │ Parent Portal  │  │  │                      │  │
│  │  │  minor modules)│  │  │  │ • Dashboard    │  │  │                      │  │
│  │  └────────────────┘  │  │  │ • Notifications│  │  │                      │  │
│  │                      │  │  ├────────────────┤  │  │                      │  │
│  │  ~768MB RAM          │  │  │ Forum Module   │  │  │  ~512MB RAM          │  │
│  │  ~0.75 vCPU          │  │  └────────────────┘  │  │  ~0.5 vCPU           │  │
│  │  LUÔN CHẠY           │  │  ~384MB RAM          │  │  TÙY CHỌN            │  │
│  │                      │  │  TÙY CHỌN            │  │                      │  │
│  └──────────────────────┘  └──────────────────────┘  └──────────────────────┘  │
│                                                                                  │
│                         ┌──────────────────────────┐                            │
│                         │      SHARED INFRA        │                            │
│                         │  PostgreSQL, Redis,      │                            │
│                         │  RabbitMQ                │                            │
│                         └──────────────────────────┘                            │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 3.2. Resource theo gói dịch vụ

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         RESOURCE ALLOCATION BY PLAN                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  GÓI BASIC (500k/th)                                                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                                                                          │    │
│  │  User+Gateway: 640MB  +  Core: 768MB  +  Frontend: 256MB                │    │
│  │                                                                          │    │
│  │  = TỔNG: ~1.7GB RAM                                                     │    │
│  │                                                                          │    │
│  │  So với hiện tại (2.25GB): TIẾT KIỆM ~500MB (22%)                       │    │
│  │                                                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  GÓI BASIC + ENGAGEMENT (800k/th)                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                                                                          │    │
│  │  User+Gateway: 640MB  +  Core: 768MB  +  Engagement: 384MB              │    │
│  │  + Frontend: 256MB                                                       │    │
│  │                                                                          │    │
│  │  = TỔNG: ~2GB RAM                                                       │    │
│  │                                                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  GÓI STANDARD (1tr/th) - Core + Engagement                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                                                                          │    │
│  │  User+Gateway: 640MB  +  Core: 768MB  +  Engagement: 384MB              │    │
│  │  + Frontend: 256MB                                                       │    │
│  │                                                                          │    │
│  │  = TỔNG: ~2GB RAM                                                       │    │
│  │                                                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  GÓI STANDARD + MEDIA (1.5tr/th)                                                │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                                                                          │    │
│  │  User+Gateway: 640MB  +  Core: 768MB  +  Engagement: 384MB              │    │
│  │  + Media: 512MB  +  Frontend: 256MB                                      │    │
│  │                                                                          │    │
│  │  = TỔNG: ~2.5GB RAM                                                     │    │
│  │                                                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  GÓI PREMIUM (2.8tr/th) - Full features                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                                                                          │    │
│  │  User+Gateway: 640MB  +  Core: 768MB  +  Engagement: 384MB              │    │
│  │  + Media: 512MB  +  Frontend: 256MB                                      │    │
│  │                                                                          │    │
│  │  = TỔNG: ~2.5GB RAM                                                     │    │
│  │                                                                          │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 3.3. Provisioning Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         PROVISIONING FLOW BY PLAN                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Khách hàng đăng ký:                                                            │
│  ─────────────────                                                              │
│                                                                                  │
│  1. Chọn gói cơ bản (BASIC/STANDARD/PREMIUM)                                   │
│  2. Chọn Add-ons (Engagement Pack, Media Pack)                                  │
│  3. Thanh toán                                                                  │
│                                                                                  │
│  Hệ thống provisioning:                                                         │
│  ─────────────────────                                                          │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                           │   │
│  │  if (order.plan === 'BASIC' && !order.addons.includes('ENGAGEMENT')) {  │   │
│  │    // Deploy: User+Gateway + Core + Frontend                             │   │
│  │    services = ['user-gateway', 'core', 'frontend'];                      │   │
│  │  }                                                                        │   │
│  │                                                                           │   │
│  │  if (order.addons.includes('ENGAGEMENT')) {                              │   │
│  │    // Thêm Engagement Service                                            │   │
│  │    services.push('engagement');                                           │   │
│  │  }                                                                        │   │
│  │                                                                           │   │
│  │  if (order.addons.includes('MEDIA')) {                                   │   │
│  │    // Thêm Media Service                                                 │   │
│  │    services.push('media');                                                │   │
│  │  }                                                                        │   │
│  │                                                                           │   │
│  │  kubernetes.deploy(services, {                                            │   │
│  │    namespace: `kiteclass-${order.instanceId}`,                           │   │
│  │    resources: calculateResources(services)                                │   │
│  │  });                                                                      │   │
│  │                                                                           │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 4: KẾT LUẬN VÀ KHUYẾN NGHỊ

## 4.1. Tổng kết phân tích

### Gateway vs User Service

| Câu hỏi | Trả lời |
|---------|---------|
| **Có thể tối ưu?** | ✅ Có, merge Gateway vào User Service tiết kiệm ~128MB RAM |
| **Tính cần thiết của Gateway riêng?** | ⭐⭐ Trung bình - có thể merge cho SMB instances |
| **Gateway làm tăng tải trọng?** | ⭐ Thấp - chỉ thêm ~256MB RAM, ~0.25 vCPU |
| **Gateway tăng phức tạp?** | ⭐⭐ Có, thêm 1 container cần manage |

### Core Service và Extend Services

| Câu hỏi | Trả lời |
|---------|---------|
| **1 extend service có hợp lý?** | ⚠️ Không hoàn toàn - thiếu linh hoạt pricing |
| **Core Service quá nặng?** | ⭐⭐ Có với gói BASIC (40% resource không dùng) |
| **Mất tính cơ động?** | ✅ Đúng - không thể mix-and-match features |
| **Đề xuất?** | Tách thành 2 extend services: Engagement + Media |

## 4.2. Khuyến nghị cuối cùng

### Kiến trúc tối ưu đề xuất

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              KIẾN TRÚC TỐI ƯU                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  SỐ SERVICES: 3-5 (tùy gói)                                                     │
│                                                                                  │
│  1. USER + GATEWAY SERVICE (Bắt buộc)     - Java Spring Boot - ~640MB          │
│  2. CORE SERVICE (Bắt buộc)               - Java Spring Boot - ~768MB          │
│  3. ENGAGEMENT SERVICE (Tùy chọn)         - Java Spring Boot - ~384MB          │
│  4. MEDIA SERVICE (Tùy chọn)              - Node.js          - ~512MB          │
│  5. FRONTEND (Bắt buộc)                   - Next.js          - ~256MB          │
│                                                                                  │
│  + Shared: PostgreSQL, Redis, RabbitMQ                                         │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### So sánh với kiến trúc hiện tại

| Tiêu chí | Hiện tại (4 services) | Đề xuất (3-5 services) | Cải thiện |
|----------|:---------------------:|:----------------------:|:---------:|
| RAM gói BASIC | ~2.25GB | ~1.7GB | -22% |
| RAM gói PREMIUM | ~2.25GB | ~2.5GB | +11% (nhưng đủ features) |
| Số containers BASIC | 4-5 | 3-4 | -20% |
| Linh hoạt pricing | ⭐ | ⭐⭐⭐ | +200% |
| Scale từng phần | ⭐ | ⭐⭐ | +100% |
| Complexity | ⭐⭐⭐ | ⭐⭐ | Giảm |

### Action Items

| STT | Hành động | Độ ưu tiên | Effort |
|:---:|-----------|:----------:|:------:|
| 1 | Merge Gateway vào User Service | P1 | Trung bình |
| 2 | Tách Gamification, Parent, Forum thành Engagement Service | P1 | Cao |
| 3 | Update bảng giá với Add-on pricing | P1 | Thấp |
| 4 | Update provisioning logic cho dynamic services | P1 | Trung bình |
| 5 | Update system-architecture-v3-final.md | P2 | Thấp |
| 6 | Update service-use-cases-v3.md | P2 | Thấp |

---

*Báo cáo được tạo bởi: Claude Assistant*
*Ngày: 23/12/2025*
*Phiên bản: 1.0*
