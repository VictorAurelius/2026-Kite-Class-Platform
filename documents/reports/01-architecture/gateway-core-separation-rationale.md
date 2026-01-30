# Báo Cáo: Lý Do Tách Biệt Gateway Service và Core Service

**Ngày:** 2026-01-27
**Phiên bản:** 1.0
**Tác giả:** Architecture Team

---

## 1. Tóm Tắt Điều Hành (Executive Summary)

Báo cáo này giải thích lý do kiến trúc của KiteClass Platform tách biệt **Gateway Service** (lớp xác thực) và **Core Service** (lớp nghiệp vụ), đồng thời giải đáp thắc mắc về tính hợp lý của việc tách biệt này khi phát hiện thiếu thiết kế liên kết dữ liệu giữa các service.

**Kết luận chính:**
- ✅ Kiến trúc tách Gateway/Core là **ĐÚNG và cần thiết**
- ✅ Vấn đề thiếu thiết kế liên kết User-Entity là vấn đề về **PATTERN** chứ không phải lỗi KIẾN TRÚC
- ✅ Giải pháp UserType + ReferenceId đã được bổ sung vào tài liệu
- ✅ Việc merge Gateway + User Service là được (cùng liên quan đến authentication)
- ❌ Việc merge Core vào Gateway+User là **SAI** (vi phạm separation of concerns)

---

## 2. Bối Cảnh và Lịch Sử Phát Triển

### 2.1. Quá Trình Tối Ưu Kiến Trúc

KiteClass Platform đã trải qua 2 giai đoạn tối ưu hóa:

```
Phase 1: 7 Services (Quá phức tạp)
├── Gateway Service
├── User Service
├── Main Class Service
├── CMC Service
├── Forum Service
├── Notification Service
└── Media Service

Phase 2: 4 Services (Tối ưu lần 1)
├── Gateway Service
├── User Service
├── Core Service (Main + CMC + Forum)
└── Media Service

Phase 3: 3-5 Services (Tối ưu lần 2 - V3.1)
├── Gateway Service (merged with User) ← RECOMMENDED
├── Core Service
├── Notification Service (standalone)
├── Email Service (standalone)
└── Media Service (optional, theo pricing tier)
```

### 2.2. Lý Do Consolidation

**Vấn đề của 7 services:**
- Overhead cao (~900MB RAM)
- Quá nhiều service cho quy mô dự án
- Các service nhỏ có phụ thuộc chặt chẽ (Main Class + CMC + Forum)
- Chi phí triển khai và vận hành lớn

**Giải pháp V3.1:**
- Merge các service **cùng domain** (Main + CMC + Forum → Core)
- Merge các service **cùng chức năng** (Gateway + User → cùng về authentication)
- Giữ riêng các service **khác responsibility** (Core, Notification, Email)

---

## 3. Tại Sao PHẢI Tách Gateway và Core Service?

### 3.1. Nguyên Tắc Separation of Concerns

```
┌─────────────────────────────────────────────────────────────┐
│                   AUTHENTICATION LAYER                       │
│  (Gateway + User Service)                                    │
│  - WHO are you?                                              │
│  - Verify credentials                                        │
│  - Issue JWT tokens                                          │
│  - Route requests                                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP + JWT
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   BUSINESS LOGIC LAYER                       │
│  (Core Service)                                              │
│  - WHAT can you do?                                          │
│  - Domain logic: Class, Assignment, Grade                    │
│  - Business rules                                            │
│  - Data processing                                           │
└─────────────────────────────────────────────────────────────┘
```

**Các lớp này có trách nhiệm khác nhau hoàn toàn:**
- **Authentication Layer:** Xác thực danh tính, quản lý session, routing
- **Business Logic Layer:** Xử lý nghiệp vụ, áp dụng business rules

### 3.2. Security Isolation (Cô Lập Bảo Mật)

**Gateway Service:**
- Chứa thông tin nhạy cảm: password hash, JWT secret, OAuth credentials
- Có attack surface lớn (public-facing)
- Cần security hardening đặc biệt
- Thường xuyên bị target bởi brute-force attacks, credential stuffing

**Core Service:**
- Chứa business data
- Không nên expose password hash hay JWT logic
- Chỉ nhận request đã được authenticate
- Giảm thiểu security risk

**Nếu merge vào một service:**
```
❌ Gateway+Core merged:
   └── Một lỗ hổng bảo mật → ảnh hưởng cả authentication VÀ business data
   └── Password breach → toàn bộ hệ thống bị xâm nhập
   └── Business logic bug có thể expose authentication data

✅ Gateway/Core separated:
   └── Lỗ hổng ở Gateway → chỉ ảnh hưởng authentication layer
   └── Lỗ hổng ở Core → không expose password hay JWT secret
   └── Có thể apply security policy khác nhau cho từng layer
```

### 3.3. Single Responsibility Principle

Mỗi service chỉ nên có **MỘT lý do để thay đổi:**

**Gateway Service thay đổi khi:**
- Thêm/sửa authentication method (OAuth, SAML, etc.)
- Thay đổi JWT strategy
- Cập nhật routing rules
- Thêm rate limiting, security middleware

**Core Service thay đổi khi:**
- Thêm/sửa business features (assignment, grade calculation)
- Thay đổi business rules
- Cập nhật data models
- Tối ưu query performance

➡️ Nếu merge, một thay đổi về authentication có thể ảnh hưởng business logic và ngược lại.

### 3.4. Independent Scaling

```
Scaling Requirements:

Gateway Service:
├── High CPU (JWT signing/verification)
├── High network I/O (all requests pass through)
├── Stateless (easy to scale horizontally)
└── Peak load: morning (8-9am) khi users đồng loạt login

Core Service:
├── High memory (complex business logic)
├── Database-intensive (CRUD operations)
├── May need caching layer
└── Peak load: cả ngày (phân tán theo activity)

Merged Service:
❌ Không thể scale riêng theo nhu cầu
❌ Lãng phí tài nguyên (scale cả hai khi chỉ cần scale một)
```

### 3.5. Clear API Boundaries

```
Gateway API (Public):
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
GET  /api/v1/auth/me

Core API (Internal - behind Gateway):
GET  /api/v1/students/{id}
POST /api/v1/assignments
GET  /api/v1/classes/{id}/students

Merged Service:
❌ Public và internal API trộn lẫn
❌ Khó phân biệt authentication endpoint vs business endpoint
❌ Khó implement security policy khác nhau
```

### 3.6. Team Ownership và Development Velocity

```
Separated:
├── Security Team → owns Gateway/Auth
├── Backend Team → owns Core/Business Logic
├── Independent deployment cycles
└── Parallel development

Merged:
├── Conflict giữa teams
├── Phải coordinate mọi thay đổi
├── Slow down deployment
└── Increased risk (mọi deploy ảnh hưởng cả auth lẫn business)
```

---

## 4. So Sánh 3 Phương Án Kiến Trúc

### Option 1: Gateway + User MERGED, Core SEPARATED ✅ RECOMMENDED

```
┌─────────────────────────────────┐
│   Gateway+User Service          │
│   (Authentication Layer)        │
│   - Login/Logout                │
│   - JWT management              │
│   - User CRUD                   │
│   - Routing                     │
└─────────────────────────────────┘
              ↓ HTTP + JWT
┌─────────────────────────────────┐
│   Core Service                  │
│   (Business Logic Layer)        │
│   - Student/Teacher/Parent      │
│   - Class/Assignment/Grade      │
│   - Forum/CMC                   │
└─────────────────────────────────┘
```

**Ưu điểm:**
- ✅ Gateway và User cùng domain (authentication) → hợp lý khi merge
- ✅ Tiết kiệm ~128MB RAM
- ✅ Giảm 1 service trong deployment
- ✅ Giữ được separation giữa Auth và Business Logic
- ✅ Clear boundaries: Authentication vs Domain Logic

**Nhược điểm:**
- ⚠️ Gateway Service hơi "nặng" hơn (chứa cả User CRUD)
- ⚠️ Cần thiết kế API rõ ràng để phân biệt routing vs user management

**Kết luận:** ✅ Đây là phương án được RECOMMEND trong V3.1

---

### Option 2: Gateway SEPARATED, User SEPARATED, Core SEPARATED

```
┌──────────────────┐
│  Gateway Service │ (Routing only)
└──────────────────┘
         ↓
┌──────────────────┐
│  User Service    │ (Authentication)
└──────────────────┘
         ↓
┌──────────────────┐
│  Core Service    │ (Business Logic)
└──────────────────┘
```

**Ưu điểm:**
- ✅ Tách biệt hoàn toàn: Routing / Auth / Business
- ✅ Dễ hiểu, rõ ràng từng responsibility

**Nhược điểm:**
- ❌ Thêm overhead (~128MB RAM)
- ❌ Thêm 1 service trong deployment
- ❌ Gateway và User quá gắn chặt → việc tách biệt không mang lại lợi ích lớn
- ❌ Extra network hop (Gateway → User → Core)

**Kết luận:** ⚠️ Có thể dùng nhưng không tối ưu cho project nhỏ/vừa

---

### Option 3: Gateway + User + Core MERGED ❌ KHÔNG NÊN

```
┌─────────────────────────────────┐
│   Monolithic Service            │
│   - Authentication              │
│   - User CRUD                   │
│   - Business Logic              │
│   - All domain models           │
└─────────────────────────────────┘
```

**Ưu điểm:**
- ✅ Tiết kiệm tối đa tài nguyên
- ✅ Deployment đơn giản (1 service)
- ✅ Không cần service-to-service communication

**Nhược điểm:**
- ❌ **VI PHẠM Separation of Concerns** (authentication logic + business logic)
- ❌ **Mất Security Isolation** (password hash cùng DB với business data)
- ❌ **Không thể scale riêng** (auth load vs business load khác nhau)
- ❌ **Tight coupling** (thay đổi auth ảnh hưởng business và ngược lại)
- ❌ **Team conflict** (security team vs backend team cùng codebase)
- ❌ **Monolithic smell** (trở lại monolith architecture)

**Kết luận:** ❌ **TUYỆT ĐỐI KHÔNG NÊN** merge Core vào Gateway

---

## 5. Vấn Đề Cross-Service Data Linking

### 5.1. Phát Hiện Gap Trong Tài Liệu

**Vấn đề được phát hiện:**
- Gateway có entity `User` (authentication)
- Core có entity `Student`, `Teacher`, `Parent` (business)
- **KHÔNG có thiết kế liên kết** giữa User và các entity này
- Không rõ Student/Teacher/Parent sẽ login như thế nào

**Câu hỏi đặt ra:**
> "Nếu việc liên kết dữ liệu giữa Gateway và Core phức tạp đến vậy, liệu có nên merge lại thành một service?"

### 5.2. Đây Là Vấn Đề Về PATTERN, Không Phải ARCHITECTURE

**Phân tích:**
1. **Architecture Decision (quyết định kiến trúc):** Tách Gateway/Core
   - Quyết định này là **ĐÚNG** vì các lý do đã nêu ở Section 3

2. **Implementation Pattern (pattern triển khai):** Cách liên kết User-Entity
   - Pattern này **BỊ THIẾU** trong tài liệu ban đầu
   - Đây là vấn đề về **HOW** (cách thực hiện), không phải **WHAT** (cái gì nên làm)

**So sánh:**
```
Ví dụ tương tự:
- Architecture: "Xe hơi phải có động cơ riêng và hệ thống lái riêng" ← ĐÚNG
- Pattern: "Làm thế nào để động cơ truyền lực đến bánh xe?" ← Thiếu hướng dẫn

Không thể kết luận: "Vì không biết cách truyền lực, nên gộp động cơ và bánh xe thành một khối"
Giải pháp đúng: Thiết kế hệ thống truyền động (transmission system)
```

### 5.3. Giải Pháp: UserType + ReferenceId Pattern

**Pattern đã được bổ sung vào tài liệu:**

```java
// Gateway Service
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;

    private String email;
    private String passwordHash;

    // Cross-service linking
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;  // STUDENT/TEACHER/PARENT/ADMIN/STAFF

    @Column(name = "reference_id")
    private Long referenceId;    // ID in Core Service
}

public enum UserType {
    ADMIN,      // referenceId = null
    STAFF,      // referenceId = null
    TEACHER,    // referenceId → teachers.id in Core
    PARENT,     // referenceId → parents.id in Core
    STUDENT     // referenceId → students.id in Core
}
```

**Login Flow:**
```
1. Student login với email/password
   POST /api/v1/auth/login
   { "email": "student@example.com", "password": "..." }

2. Gateway verify credentials
   - Tìm User trong Gateway DB
   - Verify password
   - Kiểm tra userType = STUDENT, referenceId = 123

3. Gateway call Core để lấy Student profile
   GET http://core-service/internal/students/123
   Headers: X-Internal-Request: true

4. Gateway trả về JWT + profile
   {
     "accessToken": "...",
     "user": { "id": 1, "userType": "STUDENT" },
     "profile": { "studentId": 123, "name": "Nguyen Van A" }
   }
```

**Tài liệu đã được bổ sung:**
- `.claude/skills/cross-service-data-strategy.md` (585 dòng, hướng dẫn chi tiết)
- `.claude/skills/architecture-overview.md` (cập nhật Cross-Service Relationships)
- `documents/plans/database-design.md` (cập nhật Microservices Database Strategy)
- `.claude/skills/api-design.md` (cập nhật Service-to-Service Communication)

---

## 6. So Sánh Với Industry Best Practices

### 6.1. AWS Architecture

```
AWS Cognito (Authentication)  →  Lambda/ECS (Business Logic)
     ↓                                  ↓
  RDS User Pool              →     RDS Application DB

- Cognito KHÔNG BAO GIỜ merge với Application Service
- Authentication layer luôn tách biệt
- Application nhận JWT và validate, không quản lý authentication
```

### 6.2. Google Cloud Platform

```
Firebase Auth / Identity Platform  →  Cloud Functions/Cloud Run
          ↓                                      ↓
   Firestore (users)              →    Cloud SQL (app data)

- Identity Platform là service riêng
- Application services không handle authentication
```

### 6.3. Auth0 / Okta (Industry Standard)

```
Auth0 Service (SaaS)  →  Your Backend API
       ↓                        ↓
  Auth0 Database      →   Your Database

- Authentication hoàn toàn tách biệt (thậm chí dùng SaaS)
- Backend chỉ verify JWT
- Không ai merge Auth0 vào Application
```

### 6.4. Microservices Pattern Libraries

**Tất cả các pattern sau đều tách Authentication Service:**
- Chris Richardson's Microservices Patterns
- Martin Fowler's Microservices Guide
- NGINX Microservices Reference Architecture
- Spring Cloud Documentation

**Pattern phổ biến:**
```
API Gateway (routing + rate limiting)
     ↓
Auth Service (authentication + authorization)
     ↓
Business Services (domain logic)
```

➡️ **KẾT LUẬN:** Industry không bao giờ merge Authentication vào Business Logic service

---

## 7. Câu Hỏi Thường Gặp (FAQ)

### Q1: Tại sao không dùng shared database giữa Gateway và Core?

**A:** Vi phạm nguyên tắc Database-per-Service trong microservices:
- Mỗi service phải sở hữu data của mình
- Shared database tạo tight coupling
- Thay đổi schema ảnh hưởng nhiều services
- Không thể scale database riêng cho từng service

### Q2: UserType + ReferenceId có phải là overhead không cần thiết?

**A:** Không, đây là pattern chuẩn trong microservices:
- AWS sử dụng pattern tương tự (Cognito Sub ID + Application User ID)
- Pattern này cho phép:
  - One User → Multiple Profiles (ví dụ: vừa là Teacher vừa là Parent)
  - Soft delete User không ảnh hưởng Business data
  - Migrate authentication provider (OAuth, SAML) mà không ảnh hưởng Core

### Q3: Có thể dùng Foreign Key giữa Gateway và Core không?

**A:** KHÔNG, vì:
- Gateway DB và Core DB là 2 database riêng biệt
- PostgreSQL không support cross-database FK constraints
- Vi phạm microservices independence principle

### Q4: Nếu merge Gateway+Core, có thể tiết kiệm bao nhiêu RAM?

**A:** ~200-300MB, nhưng đánh đổi:
- Mất security isolation
- Mất khả năng scale riêng
- Tight coupling
- Vi phạm separation of concerns

➡️ Không đáng để đánh đổi architecture tốt để tiết kiệm vài trăm MB RAM

### Q5: Service-to-service communication có chậm không?

**A:** Minimal overhead:
- HTTP call trong internal network: <5ms
- JWT verification: <1ms
- Feign Client có connection pooling và retry logic
- Có thể cache profile data để giảm calls

### Q6: Làm thế nào để đảm bảo data consistency giữa User và Student?

**A:** Sử dụng pattern:
- **Saga Pattern:** Create User → Create Student → Update referenceId
- **Compensating Transaction:** Nếu create Student fail → rollback create User
- **Eventual Consistency:** OK for non-critical operations
- **Distributed Transaction:** Nếu thực sự cần strong consistency (overhead cao)

---

## 8. Kết Luận và Khuyến Nghị

### 8.1. Kiến Trúc Hiện Tại Là ĐÚNG

✅ **Việc tách Gateway và Core Service là quyết định ĐÚNG vì:**
1. Separation of Concerns (Authentication vs Business Logic)
2. Security Isolation (bảo vệ sensitive authentication data)
3. Single Responsibility (mỗi service một trách nhiệm rõ ràng)
4. Independent Scaling (scale riêng theo nhu cầu)
5. Clear API Boundaries (public API vs internal API)
6. Team Ownership (security team vs backend team)
7. Industry Best Practice (AWS, GCP, Auth0 đều tách biệt)

### 8.2. Vấn Đề Đã Được Giải Quyết

✅ **Gap về Cross-Service Data Linking đã được bổ sung:**
1. UserType + ReferenceId Pattern đã được thiết kế
2. Tài liệu đã được cập nhật đầy đủ:
   - `cross-service-data-strategy.md` (585 dòng implementation guide)
   - `architecture-overview.md` (Cross-Service Relationships)
   - `database-design.md` (Microservices Database Strategy)
   - `api-design.md` (Service-to-Service Communication)
3. Code examples, SQL migrations, và flows đã được cung cấp
4. Security considerations đã được đề cập

### 8.3. Khuyến Nghị Triển Khai

**Phase 1: Gateway + User Merge (V3.1 - RECOMMENDED)**
```
┌─────────────────────────────────┐
│   Gateway+User Service          │ ← Merge để tiết kiệm resources
│   - Authentication & Routing    │
└─────────────────────────────────┘
              ↓
┌─────────────────────────────────┐
│   Core Service                  │ ← Giữ riêng business logic
│   - All domain models           │
└─────────────────────────────────┘
```

**Phase 2: Scale nếu cần (tương lai)**
- Nếu load lớn: Tách Gateway và User lại thành 2 services
- Nếu Core quá lớn: Tách thành Core + Forum + Media
- Luôn giữ nguyên tắc: Authentication layer ≠ Business logic layer

### 8.4. Tuyên Bố Rõ Ràng

| Phương Án | Trạng Thái | Lý Do |
|-----------|-----------|--------|
| **Gateway + User (merged)** + Core (separated) | ✅ **RECOMMENDED** | Cân bằng giữa resource optimization và good architecture |
| Gateway (separated) + User (separated) + Core (separated) | ⚠️ OK nhưng không tối ưu | Overhead không cần thiết cho project vừa |
| **Gateway + User + Core (merged)** | ❌ **TUYỆT ĐỐI KHÔNG** | Vi phạm separation of concerns, mất security isolation |

---

## 9. Tài Liệu Tham Khảo

### Internal Documents
1. `.claude/skills/cross-service-data-strategy.md` - Cross-service data linking implementation
2. `.claude/skills/architecture-overview.md` - System architecture overview
3. `documents/plans/database-design.md` - Database schema and microservices strategy
4. `.claude/skills/api-design.md` - API contracts and service communication
5. `documents/reports/system-architecture-v3-final.md` - Architecture evolution history
6. `documents/reports/service-optimization-report.md` - Service consolidation analysis
7. `documents/reports/architecture-optimization-analysis.md` - Gateway vs User merge analysis

### External References
1. Chris Richardson - Microservices Patterns (Database per Service pattern)
2. Martin Fowler - Microservices Guide (Service boundaries)
3. AWS Well-Architected Framework (Security best practices)
4. NGINX Microservices Reference Architecture
5. Spring Cloud Documentation (Service-to-service communication)

---

## Phụ Lục A: Ví Dụ Code Chi Tiết

### Login Flow Implementation

**1. Gateway Service - AuthController**
```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1. Verify credentials in Gateway DB
        User user = authService.authenticate(request.getEmail(), request.getPassword());

        // 2. Generate JWT
        String accessToken = jwtService.generateToken(user);

        // 3. Fetch profile from Core Service based on userType
        Object profile = null;
        if (user.getUserType() == UserType.STUDENT) {
            profile = coreServiceClient.getStudent(user.getReferenceId());
        } else if (user.getUserType() == UserType.TEACHER) {
            profile = coreServiceClient.getTeacher(user.getReferenceId());
        }

        // 4. Return response
        return ResponseEntity.ok(LoginResponse.builder()
            .accessToken(accessToken)
            .user(UserDTO.from(user))
            .profile(profile)
            .build());
    }
}
```

**2. Core Service - Internal API**
```java
@RestController
@RequestMapping("/internal/students")
public class InternalStudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudent(
            @PathVariable Long id,
            @RequestHeader("X-Internal-Request") String internalHeader) {

        // Verify this is internal request
        if (!"true".equals(internalHeader)) {
            throw new ForbiddenException("Not allowed");
        }

        Student student = studentService.findById(id);
        return ResponseEntity.ok(StudentResponse.from(student));
    }
}
```

**3. Feign Client - Gateway calls Core**
```java
@FeignClient(name = "core-service", url = "${core.service.url}")
public interface CoreServiceClient {

    @GetMapping("/internal/students/{id}")
    StudentResponse getStudent(
        @PathVariable("id") Long id,
        @RequestHeader("X-Internal-Request") String internalHeader
    );

    @GetMapping("/internal/teachers/{id}")
    TeacherResponse getTeacher(
        @PathVariable("id") Long id,
        @RequestHeader("X-Internal-Request") String internalHeader
    );
}
```

### Create Account Flow

```java
@Service
@Transactional
public class UserRegistrationService {

    public UserRegistrationResponse registerStudent(StudentRegistrationRequest request) {
        // 1. Create User in Gateway DB (without referenceId yet)
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .userType(UserType.STUDENT)
            .status(UserStatus.PENDING)
            .build();
        User savedUser = userRepository.save(user);

        try {
            // 2. Create Student in Core Service via API
            CreateStudentRequest coreRequest = CreateStudentRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
            StudentResponse student = coreServiceClient.createStudent(coreRequest);

            // 3. Update User with referenceId
            savedUser.setReferenceId(student.getId());
            savedUser.setStatus(UserStatus.ACTIVE);
            userRepository.save(savedUser);

            return UserRegistrationResponse.success(savedUser, student);

        } catch (Exception e) {
            // Compensating transaction: rollback User creation
            userRepository.delete(savedUser);
            throw new RegistrationFailedException("Failed to create student profile", e);
        }
    }
}
```

---

**Kết thúc báo cáo**
