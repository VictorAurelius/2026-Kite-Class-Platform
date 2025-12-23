# BÁO CÁO ĐÁNH GIÁ VÀ TỐI ƯU KIẾN TRÚC SERVICES
## KiteClass Instance

| Thuộc tính | Giá trị |
|------------|---------|
| **Ngày** | 18/12/2025 |
| **Phiên bản** | 1.0 |
| **Loại tài liệu** | Đánh giá kiến trúc |

---

## PHẦN 1: CẤU TRÚC SERVICES HIỆN TẠI

### 1.1. Danh sách Services

KiteClass Instance hiện tại gồm **7 services**:

| Service | Công nghệ | Chức năng chính |
|---------|-----------|-----------------|
| **Main Class Service** | Java Spring Boot | Quản lý lớp học, khóa học, lịch học |
| **User Service** | Java Spring Boot | Quản lý người dùng, xác thực, phân quyền |
| **CMC Service** (Class Management Core) | Java Spring Boot | Điểm danh, bài tập, đánh giá, thống kê |
| **Video Service** | Java Spring Boot | Quản lý video bài giảng, theo dõi tiến độ xem |
| **Streaming Service** | Node.js | Phát trực tiếp buổi học, WebSocket |
| **Forum Service** | Java Spring Boot | Diễn đàn trao đổi, hỏi đáp |
| **Frontend** | Next.js | Giao diện người dùng, SSR |

---

## PHẦN 2: ĐÁNH GIÁ KIẾN TRÚC HIỆN TẠI

### 2.1. Điểm mạnh

✅ **Single Responsibility**: Mỗi service có trách nhiệm rõ ràng
- Main Class: Chỉ quản lý lớp/khóa học
- User: Chỉ quản lý người dùng
- CMC: Chỉ quản lý hoạt động học tập
- Video: Chỉ quản lý video
- Streaming: Chỉ xử lý live stream
- Forum: Chỉ quản lý diễn đàn

✅ **Công nghệ phù hợp**:
- Java Spring Boot: Ổn định cho business logic phức tạp
- Node.js cho Streaming: Phù hợp xử lý WebSocket real-time
- Next.js cho Frontend: SSR tốt cho SEO

✅ **Scale độc lập**: Mỗi service có thể scale riêng theo nhu cầu

✅ **Polyglot**: Cho phép dùng công nghệ khác nhau cho từng service

### 2.2. Điểm yếu và vấn đề

#### ❌ QUÁ NHIỀU SERVICES CHO QUY MÔ NHỎ

**Vấn đề:**
- 6 backend services + 1 frontend = 7 containers per instance
- Mỗi instance cần 7 pods riêng biệt
- Chi phí infrastructure cao
- Complexity trong deployment và monitoring

**Phân tích:**
- Nếu có 100 instances = **700 containers**
- Overhead của service mesh, service discovery lớn
- Debugging cross-service khó khăn

#### ❌ OVERLAP GIỮA MAIN CLASS VÀ CMC SERVICE

**Vấn đề:**
- Main Class: Quản lý lớp học, khóa học, lịch học
- CMC: Điểm danh, bài tập, đánh giá
- Cả hai đều liên quan đến "lớp học" (class)

**Overlap cụ thể:**
- Lịch học (schedule) thuộc Main Class hay CMC?
- Bài giảng (lesson) thuộc Main Class hay CMC?
- Khi tạo bài tập, cần gọi Main Class lấy thông tin lớp
- Khi điểm danh, cần gọi Main Class lấy danh sách học viên

**Hệ quả:**
- Tight coupling giữa 2 services
- Nhiều API calls qua lại
- Khó maintain ranh giới rõ ràng

#### ❌ USER SERVICE QUÁ PHỤ THUỘC

**Vấn đề:**
- Tất cả services đều cần gọi User Service để xác thực
- User Service trở thành single point of failure
- Latency tăng do mọi request đều qua User Service

#### ❌ VIDEO VÀ STREAMING CÓ THỂ GỘP

**Vấn đề:**
- Video Service: Quản lý video đã ghi
- Streaming Service: Phát trực tiếp
- Cả hai đều liên quan đến "video content"
- Live stream thường được record → cần sync với Video Service

#### ❌ FORUM SERVICE ĐỘC LẬP QUÁ MỨC

**Vấn đề:**
- Forum thường không có traffic cao
- Forum tightly coupled với khóa học (course forum)
- Tách riêng Forum không mang lại lợi ích scale rõ ràng

---

## PHẦN 3: ĐỀ XUẤT TỐI ƯU

### 3.1. Phương án 1: Giữ nguyên 7 services ❌

| Ưu điểm | Nhược điểm |
|---------|------------|
| Không cần refactor | Chi phí cao |
| Linh hoạt tối đa | Phức tạp trong vận hành |
| | Over-engineering |

**Đánh giá: 4/10**

### 3.2. Phương án 2: Gộp thành 4 services ✅ (KHUYẾN NGHỊ)

```
┌─────────────────────────────────────────────────────────────────┐
│                    KITECLASS INSTANCE V2                        │
│                      (4 Services)                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  CORE SERVICE (Java Spring Boot)                          │  │
│  │  Gộp: Main Class + CMC + Forum                            │  │
│  │  • Class Module: Quản lý lớp, khóa học, lịch học          │  │
│  │  • Learning Module: Bài tập, điểm danh, đánh giá          │  │
│  │  • Forum Module: Diễn đàn, hỏi đáp                        │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  USER SERVICE (Java Spring Boot) - Giữ nguyên             │  │
│  │  • Quản lý người dùng, xác thực, phân quyền               │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  MEDIA SERVICE (Node.js)                                  │  │
│  │  Gộp: Video + Streaming                                   │  │
│  │  • Video Module: Upload, transcode, serve video           │  │
│  │  • Streaming Module: Live stream, WebSocket               │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  FRONTEND (Next.js) - Giữ nguyên                          │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

| Ưu điểm | Nhược điểm |
|---------|------------|
| Giảm 43% số services (7 → 4) | Cần refactor code |
| Giảm complexity trong deployment | Core Service lớn hơn |
| Communication local (nhanh hơn) | Khó scale từng module riêng |
| Vẫn giữ được tính modular | |

**Đánh giá: 8/10**

### 3.3. Phương án 3: Gộp thành 3 services

- Core Service: Main Class + CMC + Forum + User
- Media Service: Video + Streaming
- Frontend: Next.js

**⚠️ Không khuyến nghị** vì User là security-critical, nên tách riêng.

**Đánh giá: 6/10**

---

## PHẦN 4: SO SÁNH CHI TIẾT

### 4.1. So sánh số lượng components

| Metric | Hiện tại | Đề xuất | Giảm |
|--------|----------|---------|------|
| Backend services | 6 | 3 | **-50%** |
| Containers/instance | 7 | 4 | **-43%** |
| 100 instances | 700 | 400 | **-300** |
| Database connections | 6 pools | 3 pools | **-50%** |
| Code repositories | 6 | 3 | **-50%** |

### 4.2. So sánh inter-service communication

**Hiện tại (7 services):**
```
Main Class ←→ CMC (điểm danh cần lấy danh sách học viên)
Main Class ←→ Video (gắn video vào bài giảng)
Main Class ←→ Forum (tạo forum cho khóa học)
CMC ←→ User (lấy thông tin học viên cho điểm)
Video ←→ Streaming (record live thành video)
Forum ←→ User (hiển thị avatar, tên người post)
Tất cả ←→ User (authentication)

→ Tổng: 10+ network calls cho 1 use case phức tạp
```

**Đề xuất (4 services):**
```
Core Service: Internal method calls (class ↔ learning ↔ forum)
Core ←→ User (authentication)
Core ←→ Media (embed video)
Media Service: Internal calls (video ↔ streaming)

→ Tổng: 3-5 network calls cho cùng use case
→ Giảm: 60% network overhead
```

### 4.3. So sánh chi phí (100 instances)

| Resource | Hiện tại | Đề xuất | Tiết kiệm |
|----------|----------|---------|-----------|
| Pods (EKS) | 700 | 400 | 300 pods |
| CPU (request) | 350 cores | 200 cores | 150 cores |
| Memory (request) | 700 GB | 400 GB | 300 GB |
| Load balancers | 600 | 300 | 300 |
| **Chi phí EKS/tháng** | ~$2,500 | ~$1,500 | **~$1,000** |
| **Chi phí ALB/tháng** | ~$1,200 | ~$600 | **~$600** |
| **Tổng tiết kiệm** | | | **~$1,600/tháng** |

---

## PHẦN 5: CHI TIẾT CORE SERVICE SAU KHI GỘP

### 5.1. Module Structure

```
core-service/
├── src/main/java/com/kiteclass/core/
│   │
│   ├── class/                      # Class Module (từ Main Class)
│   │   ├── controller/
│   │   │   ├── CourseController.java
│   │   │   ├── ClassController.java
│   │   │   └── ScheduleController.java
│   │   ├── service/
│   │   ├── entity/
│   │   └── repository/
│   │
│   ├── learning/                   # Learning Module (từ CMC)
│   │   ├── controller/
│   │   │   ├── AttendanceController.java
│   │   │   ├── AssignmentController.java
│   │   │   └── GradeController.java
│   │   ├── service/
│   │   ├── entity/
│   │   └── repository/
│   │
│   ├── forum/                      # Forum Module
│   │   ├── controller/
│   │   ├── service/
│   │   ├── entity/
│   │   └── repository/
│   │
│   └── shared/                     # Shared components
│       ├── config/
│       ├── security/
│       ├── exception/
│       └── util/
```

### 5.2. Internal Communication

**Trước (inter-service HTTP call):**
```java
@Service
public class AttendanceService {
    @Autowired
    private RestTemplate restTemplate;

    public List<Student> getClassStudents(Long classId) {
        // Network call - latency ~50-100ms
        return restTemplate.getForObject(
            "http://main-class-service/api/classes/" + classId + "/students",
            StudentList.class
        );
    }
}
```

**Sau (internal method call):**
```java
@Service
public class AttendanceService {
    @Autowired
    private ClassService classService;  // Same JVM

    public List<Student> getClassStudents(Long classId) {
        // Local call - latency ~1ms
        return classService.getStudentsByClassId(classId);
    }
}
```

### 5.3. Database Schema (Shared)

```sql
-- Core Service dùng 1 database với multiple schemas

CREATE SCHEMA class_module;
CREATE SCHEMA learning_module;
CREATE SCHEMA forum_module;

-- Tables in class_module
CREATE TABLE class_module.courses (...);
CREATE TABLE class_module.classes (...);
CREATE TABLE class_module.schedules (...);

-- Tables in learning_module
CREATE TABLE learning_module.attendances (...);
CREATE TABLE learning_module.assignments (...);
CREATE TABLE learning_module.grades (...);

-- Tables in forum_module
CREATE TABLE forum_module.topics (...);
CREATE TABLE forum_module.posts (...);

-- Cross-module queries vẫn OK (same database)
SELECT c.name, COUNT(a.id) as attendance_count
FROM class_module.classes c
JOIN learning_module.attendances a ON a.class_id = c.id
GROUP BY c.id;
```

---

## PHẦN 6: KẾ HOẠCH MIGRATION

| Phase | Thời gian | Công việc |
|-------|-----------|-----------|
| **Phase 1** | 1 tuần | Chuẩn bị: Tạo Core Service project, setup module structure |
| **Phase 2** | 1 tuần | Migrate Main Class → Core |
| **Phase 3** | 1 tuần | Migrate CMC → Core |
| **Phase 4** | 3 ngày | Migrate Forum → Core |
| **Phase 5** | 1 tuần | Gộp Video + Streaming → Media Service |
| **Phase 6** | 1 tuần | Testing & Deployment |
| **Tổng** | **5-6 tuần** | |

---

## PHẦN 7: KẾT LUẬN VÀ KHUYẾN NGHỊ

### 7.1. Tóm tắt đánh giá

Kiến trúc hiện tại (7 services):
- ✅ Đúng về lý thuyết microservices
- ❌ **OVER-ENGINEERING** cho quy mô platform giáo dục
- ❌ Chi phí và complexity cao không cần thiết
- ❌ Tight coupling tự nhiên giữa Main Class, CMC, Forum

### 7.2. Khuyến nghị

> **KHUYẾN NGHỊ: Áp dụng Phương án 2 (4 Services)**

| Service hiện tại | → | Service mới |
|------------------|---|-------------|
| Main Class + CMC + Forum | → | **Core Service** (Modular Monolith) |
| User Service | → | **User Service** (giữ nguyên) |
| Video + Streaming | → | **Media Service** |
| Frontend | → | **Frontend** (giữ nguyên) |

### 7.3. Lợi ích dự kiến

- ✅ Giảm 43% số containers (700 → 400 cho 100 instances)
- ✅ Giảm 60% network overhead giữa services
- ✅ Tiết kiệm ~$1,600/tháng chi phí infrastructure
- ✅ Đơn giản hóa deployment và monitoring
- ✅ Dễ debug hơn (ít cross-service calls)
- ✅ Vẫn giữ được tính modular trong code

### 7.4. Rủi ro

- ⚠️ Effort refactor: ~5-6 tuần
- ⚠️ Core Service trở nên lớn hơn
- ⚠️ Nếu cần scale module riêng sẽ khó hơn

### 7.5. Quyết định cuối cùng

Với quy mô dự kiến (100-500 instances), kiến trúc **4 services là PHÙ HỢP**.
Nếu scale lên 1000+ instances, có thể cân nhắc tách lại các modules.

---

## PHỤ LỤC: BẢNG SO SÁNH NHANH

| Tiêu chí | 7 Services | 4 Services |
|----------|------------|------------|
| Containers | 7/instance | 4/instance |
| Network calls | 10+/request | 3-5/request |
| Chi phí | Cao | Trung bình |
| Complexity | Cao | Trung bình |
| Debug | Khó | Dễ hơn |
| Scale module | Dễ | Khó hơn |
| Refactor | 0 | 5-6 tuần |
| Phù hợp quy mô | 1000+ instances | 100-500 instances |

---

*Báo cáo được tạo bởi: Claude Assistant*
*Ngày: 18/12/2025*
