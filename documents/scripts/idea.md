# ĐỀ CƯƠNG Ý TƯỞNG ĐỒ ÁN TỐT NGHIỆP

## Tên đề tài
**Xây dựng hệ thống quản lý lớp học trực tuyến theo kiến trúc Microservices - KiteClass Platform**

---

## 1. Giới thiệu

### 1.1. Bối cảnh và lý do chọn đề tài
Trong bối cảnh chuyển đổi số giáo dục đang diễn ra mạnh mẽ, nhu cầu về các nền tảng học trực tuyến linh hoạt, có khả năng mở rộng và tùy chỉnh cao ngày càng tăng. Các giải pháp hiện tại thường gặp hạn chế về khả năng tùy biến theo đặc thù từng tổ chức giáo dục.

### 1.2. Mục tiêu đề tài
- Xây dựng nền tảng quản lý lớp học trực tuyến theo kiến trúc microservices
- Phát triển hệ thống có khả năng mở rộng, tùy chỉnh linh hoạt theo nhu cầu khách hàng
- Tích hợp đầy đủ các tính năng từ quản lý lớp học đến vận hành và kinh doanh

### 1.3. Phạm vi nghiên cứu
- **Đối tượng**: Các tổ chức giáo dục, doanh nghiệp đào tạo, giảng viên độc lập
- **Phạm vi chức năng**: Quản lý lớp học, người dùng, nội dung học tập và vận hành nền tảng

---

## 2. Kiến trúc hệ thống (System Architecture)

Hệ thống **KiteClass Platform** được thiết kế theo mô hình **Microservices Architecture**, phân chia thành 2 nhóm dịch vụ chính:

### 2.1. KiteClass Microservices (Core Services)
Nhóm dịch vụ cốt lõi phục vụ hoạt động của từng lớp học:

| Service | Chức năng |
|---------|-----------|
| **Main Class Service** | Quản lý thông tin lớp học, khóa học, lịch học, bài giảng |
| **User Service** | Quản lý người dùng (giảng viên, học viên), phân quyền, xác thực |
| **CMC Service** (Class Management Core) | Dịch vụ quản trị lõi: điểm danh, bài tập, đánh giá, thống kê |

### 2.2. Expand Services (Dịch vụ mở rộng tùy chọn)
Các module bổ sung theo nhu cầu:
- **Video Learning Service**: Quản lý video bài giảng, theo dõi tiến độ xem
- **Online Learning Service**: Học trực tuyến đồng bộ/không đồng bộ
- **Streaming Service**: Phát trực tiếp buổi học
- **Forum Service**: Diễn đàn trao đổi, hỏi đáp

### 2.3. KiteHub Platform
Nền tảng trung tâm kết nối, điều khiển và vận hành tổng thể:

| Service | Chức năng |
|---------|-----------|
| **Sale Service** | Trang chủ, landing page, quảng bá dịch vụ, đăng ký khóa học |
| **Message Service** | Hỗ trợ tư vấn khách hàng realtime, chatbot, notification |
| **Maintaining Service** | Quản lý, giám sát, bảo trì và điều phối các instance KiteClass |

### 2.4. Mô hình tổng quan
```
┌─────────────────────────────────────────────────────────────┐
│                    KiteHub Platform                         │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐    │
│  │ Sale Service│  │Message Svc │  │ Maintaining Svc  │    │
│  └─────────────┘  └─────────────┘  └──────────────────┘    │
└─────────────────────────┬───────────────────────────────────┘
                          │ Điều phối & Quản lý
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  KiteClass #1   │ │  KiteClass #2   │ │  KiteClass #n   │
│ ┌─────────────┐ │ │ ┌─────────────┐ │ │ ┌─────────────┐ │
│ │ Main Class  │ │ │ │ Main Class  │ │ │ │ Main Class  │ │
│ │ User Svc    │ │ │ │ User Svc    │ │ │ │ User Svc    │ │
│ │ CMC Svc     │ │ │ │ CMC Svc     │ │ │ │ CMC Svc     │ │
│ ├─────────────┤ │ │ ├─────────────┤ │ │ ├─────────────┤ │
│ │ + Expand    │ │ │ │ + Expand    │ │ │ │ + Expand    │ │
│ │   Services  │ │ │ │   Services  │ │ │ │   Services  │ │
│ └─────────────┘ │ │ └─────────────┘ │ │ └─────────────┘ │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## 3. Lộ trình phát triển (Development Roadmap)

| Phase | Mục tiêu | Công việc chính | Kết quả kỳ vọng |
|-------|----------|-----------------|-----------------|
| **Phase 1** | Xây dựng Core Services | Thiết kế kiến trúc tổng thể; Phát triển Main Class, User, CMC Services; Xây dựng RESTful API chuẩn; Thiết lập CI/CD pipeline | Hệ thống lớp học cơ bản hoạt động ổn định, có khả năng tạo và quản lý lớp học |
| **Phase 2** | Xây dựng KiteHub Platform | Phát triển Sale, Message, Maintaining Services; Tích hợp với Core Services qua API Gateway; Xây dựng dashboard quản trị | Nền tảng quản lý và vận hành đa lớp học hoàn chỉnh |
| **Phase 3** | Mở rộng Expand Services | Phát triển các module: Video Learning, Streaming, Forum; Tích hợp dạng plugin; Tối ưu hiệu năng hệ thống | Hệ sinh thái đa dạng, đáp ứng nhu cầu khách hàng |

---

## 4. Điểm nổi bật và lợi thế cạnh tranh (USP)

### 4.1. Khởi tạo nhanh lớp học trực tuyến
- Tạo và cấu hình lớp học chỉ với vài thao tác đơn giản
- Hỗ trợ đa dạng template và giao diện tùy chỉnh
- Triển khai tức thì (instant deployment)

### 4.2. Kiến trúc Microservices linh hoạt
- Dễ dàng thêm, thay thế hoặc nâng cấp từng service độc lập
- Không ảnh hưởng đến hoạt động của các service khác
- Hỗ trợ horizontal scaling theo nhu cầu

### 4.3. Nền tảng quản lý toàn diện (KiteHub)
- Tích hợp đầy đủ: Tư vấn → Bán hàng → Triển khai → Vận hành
- Hệ sinh thái gắn kết từ marketing đến chăm sóc khách hàng
- Dashboard quản trị trực quan, realtime monitoring

### 4.4. Tích hợp AI và phân tích dữ liệu (Định hướng phát triển)
- Phân tích hành vi và tiến độ học tập của học viên
- Gợi ý nội dung học tập cá nhân hóa
- Dự đoán kết quả và cảnh báo sớm học viên có nguy cơ bỏ học

### 4.5. Tùy chỉnh cao theo nhu cầu doanh nghiệp
- Lựa chọn linh hoạt các module dịch vụ cần thiết
- Tùy biến giao diện, workflow theo đặc thù tổ chức
- Hỗ trợ white-label cho đối tác

---

## 5. Công nghệ dự kiến sử dụng

| Thành phần | Công nghệ |
|------------|-----------|
| **Backend** | Node.js / NestJS, RESTful API, GraphQL |
| **Frontend** | React.js / Next.js |
| **Database** | PostgreSQL, MongoDB, Redis |
| **Message Queue** | RabbitMQ / Apache Kafka |
| **Container** | Docker, Kubernetes |
| **CI/CD** | GitHub Actions, Jenkins |
| **Cloud** | AWS / GCP / Azure |

---

## 6. Kết luận

Đề tài hướng đến xây dựng một nền tảng quản lý lớp học trực tuyến hiện đại, áp dụng kiến trúc microservices để đảm bảo tính linh hoạt, khả năng mở rộng và dễ bảo trì. Hệ thống KiteClass Platform không chỉ đáp ứng nhu cầu quản lý lớp học cơ bản mà còn cung cấp giải pháp toàn diện từ vận hành đến kinh doanh cho các tổ chức giáo dục.