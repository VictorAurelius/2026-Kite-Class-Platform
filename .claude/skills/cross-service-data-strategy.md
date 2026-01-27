# Cross-Service Data Relationship Strategy

**Tài liệu chiến lược:** Quản lý mối quan hệ dữ liệu giữa services trong kiến trúc microservices

**Version:** 1.0
**Last Updated:** 2026-01-27
**Author:** KiteClass Team

---

## 📋 Tổng quan

### Vấn đề cần giải quyết

Trong kiến trúc microservices của KiteClass Platform:
- **Gateway Service**: Quản lý Authentication & Authorization (users, roles, JWT)
- **Core Service**: Quản lý Business Logic (students, teachers, parents, classes)

**Câu hỏi chính:**
- Làm sao Student/Teacher/Parent login vào hệ thống?
- User credentials ở đâu? Business data ở đâu?
- Làm sao link giữa authentication identity và business entity?

### Yêu cầu kiến trúc

| Yêu cầu | Giải thích |
|---------|------------|
| ✅ **Service Independence** | Gateway và Core hoàn toàn độc lập về database |
| ✅ **Clear Separation** | Authentication logic ≠ Business logic |
| ✅ **Single Source of Truth** | User credentials chỉ trong Gateway, business data chỉ trong Core |
| ✅ **No Direct FK** | Không có foreign key trực tiếp giữa 2 databases |
| ✅ **Cross-Service Communication** | REST API calls giữa services |

---

## 🎯 Giải pháp đã chọn: UserType + ReferenceId Pattern

### Kiến trúc tổng quan

```
┌──────────────────────────────────────────────────────────────────┐
│                GATEWAY SERVICE (Authentication)                   │
│                Database: kiteclass_{tenant}_gateway              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  users table                              │  │
│  ├───────────────────────────────────────────────────────────┤  │
│  │ id                  BIGSERIAL PK                          │  │
│  │ email               VARCHAR UNIQUE                        │  │
│  │ password_hash       VARCHAR                               │  │
│  │ name                VARCHAR                               │  │
│  │ user_type           VARCHAR(20)  ◄─── STUDENT/TEACHER/   │  │
│  │                                       PARENT/ADMIN/STAFF  │  │
│  │ reference_id        BIGINT       ◄─── ID trong Core DB   │  │
│  │ status              VARCHAR                               │  │
│  └───────────────────────────────────────────────────────────┘  │
│                            │                                    │
│                            │ reference_id links to              │
│                            └────────────────┐                   │
└─────────────────────────────────────────────│───────────────────┘
                                              │
┌─────────────────────────────────────────────│───────────────────┐
│                CORE SERVICE (Business Logic) │                   │
│                Database: kiteclass_{tenant}_core                │
├──────────────────────────────────────────────────────────────────┤
│                                              │                   │
│  ┌────────────────────┐  ┌──────────────────│───────────────┐   │
│  │  students table    │  │  teachers table  │               │   │
│  ├────────────────────┤  ├──────────────────┴───────────────┤   │
│  │ id         PK      │◄─┤ id         PK                    │   │
│  │ name               │  │ name                             │   │
│  │ email              │  │ department                       │   │
│  │ date_of_birth      │  │ specialization                   │   │
│  │ status             │  │ bio                              │   │
│  └────────────────────┘  └──────────────────────────────────┘   │
│                                                                  │
│  ┌────────────────────┐                                          │
│  │  parents table     │                                          │
│  ├────────────────────┤                                          │
│  │ id         PK      │◄─────────────────────────────────────────┤
│  │ name               │                                          │
│  │ email              │                                          │
│  │ relationship       │                                          │
│  └────────────────────┘                                          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### UserType Enum

```java
public enum UserType {
    ADMIN,      // Admin - không có referenceId (internal user)
    STAFF,      // Nhân viên - không có referenceId (internal user)
    TEACHER,    // referenceId → teachers.id trong Core DB
    PARENT,     // referenceId → parents.id trong Core DB
    STUDENT     // referenceId → students.id trong Core DB
}
```

### Mapping Table

| user_type | reference_id | Ý nghĩa | Login? | Profile trong Core? |
|-----------|--------------|---------|--------|---------------------|
| `ADMIN` | `NULL` | Admin/Owner quản trị hệ thống | ✅ | ❌ |
| `STAFF` | `NULL` | Nhân viên văn phòng | ✅ | ❌ |
| `TEACHER` | `teachers.id` | Giáo viên | ✅ | ✅ |
| `PARENT` | `parents.id` | Phụ huynh | ✅ | ✅ |
| `STUDENT` | `students.id` | Học viên | ✅ | ✅ |

---

## 🔄 Implementation Flows

### 1. Login Flow với Profile Retrieval

```
┌─────────────────────────────────────────────────────────────────┐
│                         LOGIN FLOW                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Frontend              Gateway Service        Core Service      │
│     │                       │                       │           │
│     │──POST /login─────────►│                       │           │
│     │  {email, password}    │                       │           │
│     │                       │                       │           │
│     │                       │──1. Authenticate      │           │
│     │                       │──2. Query users table │           │
│     │                       │   (user_type=TEACHER, │           │
│     │                       │    reference_id=456)  │           │
│     │                       │                       │           │
│     │                       │──3. Generate JWT      │           │
│     │                       │   (include user_type) │           │
│     │                       │                       │           │
│     │                       │──4. GET /teachers/456─►│          │
│     │                       │◄──Teacher profile─────│           │
│     │                       │                       │           │
│     │◄──Login response──────│                       │           │
│     │  {                    │                       │           │
│     │    accessToken,       │                       │           │
│     │    refreshToken,      │                       │           │
│     │    user: {            │                       │           │
│     │      id, email,       │                       │           │
│     │      userType         │                       │           │
│     │    },                 │                       │           │
│     │    profile: {         │                       │           │
│     │      teacherId: 456,  │                       │           │
│     │      department: "...",│                      │           │
│     │      specialization   │                       │           │
│     │    }                  │                       │           │
│     │  }                    │                       │           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Tạo Student Account Flow

```java
// === GATEWAY SERVICE ===
@Service
public class StudentAccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CoreServiceClient coreServiceClient;  // Feign Client

    @Transactional
    public StudentAccountResponse createStudentAccount(
        CreateStudentAccountRequest request
    ) {
        // 1. Validate email không trùng
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        // 2. Tạo Student trong Core DB (via API)
        StudentCreateRequest coreRequest = StudentCreateRequest.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .dateOfBirth(request.getDateOfBirth())
            .build();

        StudentResponse student = coreServiceClient.createStudent(coreRequest);

        // 3. Tạo User trong Gateway DB
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .userType(UserType.STUDENT)
            .referenceId(student.getId())  // Link to Core DB
            .status(UserStatus.ACTIVE)
            .build();

        User savedUser = userRepository.save(user);

        // 4. Assign role STUDENT
        roleService.assignRole(savedUser.getId(), "STUDENT");

        return StudentAccountResponse.builder()
            .userId(savedUser.getId())
            .studentId(student.getId())
            .email(savedUser.getEmail())
            .build();
    }
}
```

### 3. Xóa Student Account Flow

```java
// === GATEWAY SERVICE ===
@Transactional
public void deleteStudentAccount(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException());

    if (user.getUserType() != UserType.STUDENT) {
        throw new InvalidUserTypeException();
    }

    // 1. Soft delete User trong Gateway
    user.setDeleted(true);
    user.setDeletedAt(Instant.now());
    userRepository.save(user);

    // 2. Soft delete Student trong Core (via API)
    if (user.getReferenceId() != null) {
        coreServiceClient.deleteStudent(user.getReferenceId());
    }
}
```

---

## 💻 Code Implementation

### Gateway Service - User Entity

```java
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    // *** Cross-service linking fields ***
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType = UserType.ADMIN;

    @Column(name = "reference_id")
    private Long referenceId;
    // *** End cross-service fields ***

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    private String phone;
    private String avatarUrl;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
```

### Gateway Service - Feign Client

```java
@FeignClient(
    name = "core-service",
    url = "${services.core.url}",
    configuration = CoreServiceClientConfig.class
)
public interface CoreServiceClient {

    @GetMapping("/api/v1/students/{id}")
    StudentResponse getStudent(@PathVariable("id") Long id);

    @PostMapping("/api/v1/students")
    StudentResponse createStudent(@RequestBody StudentCreateRequest request);

    @DeleteMapping("/api/v1/students/{id}")
    void deleteStudent(@PathVariable("id") Long id);

    @GetMapping("/api/v1/teachers/{id}")
    TeacherResponse getTeacher(@PathVariable("id") Long id);

    @GetMapping("/api/v1/parents/{id}")
    ParentResponse getParent(@PathVariable("id") Long id);
}
```

### Core Service - Student Entity

```java
@Entity
@Table(name = "students")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String note;

    // NO userId field - linked via Gateway.users.reference_id
}
```

### Core Service - Controller with X-Headers

```java
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping
    public PageResponse<StudentDTO> getStudents(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Type") String userType,
        @RequestHeader("X-Reference-Id") Long referenceId,
        Pageable pageable
    ) {
        // Authorization based on userType
        if ("TEACHER".equals(userType)) {
            // Teacher chỉ xem students trong classes của mình
            return studentService.getStudentsByTeacher(referenceId, pageable);
        } else if ("PARENT".equals(userType)) {
            // Parent chỉ xem con của mình
            return studentService.getStudentsByParent(referenceId, pageable);
        } else if ("ADMIN".equals(userType) || "OWNER".equals(userType)) {
            // Admin/Owner xem tất cả
            return studentService.getAllStudents(pageable);
        }

        throw new ForbiddenException("You don't have permission to view students");
    }

    @GetMapping("/{id}")
    public StudentDTO getStudent(
        @PathVariable Long id,
        @RequestHeader("X-User-Type") String userType,
        @RequestHeader("X-Reference-Id") Long referenceId
    ) {
        StudentDTO student = studentService.getStudent(id);

        // Authorization check
        if ("PARENT".equals(userType)) {
            // Parent chỉ xem con của mình
            if (!studentService.isParentOfStudent(referenceId, id)) {
                throw new ForbiddenException();
            }
        }

        return student;
    }
}
```

---

## 🗄️ Database Migration

### Gateway DB Migration

```sql
-- V6__add_user_type_and_reference_id.sql

ALTER TABLE users
    ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    ADD COLUMN reference_id BIGINT NULL;

CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_reference_id ON users(reference_id);

-- Add constraint
ALTER TABLE users
    ADD CONSTRAINT chk_users_user_type CHECK (
        user_type IN ('ADMIN', 'STAFF', 'TEACHER', 'PARENT', 'STUDENT')
    );

-- Comments
COMMENT ON COLUMN users.user_type IS
    'User type: ADMIN, STAFF, TEACHER, PARENT, STUDENT';
COMMENT ON COLUMN users.reference_id IS
    'ID của entity tương ứng trong Core DB (students.id / teachers.id / parents.id)';
```

### Core DB Tables

```sql
-- V1__create_students_table.sql

CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),
    address TEXT,
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_students_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED')
    )
);

CREATE INDEX idx_students_email ON students(email) WHERE deleted = FALSE;
CREATE INDEX idx_students_phone ON students(phone);
CREATE INDEX idx_students_status ON students(status) WHERE deleted = FALSE;

-- NO userId field - linked via Gateway.users.reference_id
COMMENT ON TABLE students IS
    'Student business entities - linked to Gateway users via reference_id';
```

---

## ✅ Ưu điểm

| Ưu điểm | Giải thích |
|---------|------------|
| ✅ **Service Independence** | Gateway và Core hoàn toàn độc lập về database và deployment |
| ✅ **Clear Separation of Concerns** | Authentication logic ≠ Business logic |
| ✅ **Single Source of Truth** | Credentials chỉ trong Gateway, business data chỉ trong Core |
| ✅ **Scalability** | Có thể scale Gateway và Core service độc lập |
| ✅ **Security** | JWT generation/validation chỉ trong Gateway |
| ✅ **Flexibility** | Admin/Staff không cần entity trong Core |
| ✅ **Consistent Pattern** | Dễ áp dụng cho Teacher, Parent tương tự Student |

---

## ⚠️ Nhược điểm và Giải pháp

| Nhược điểm | Giải pháp |
|------------|-----------|
| ⚠️ **No FK Constraints** | Validate tại application layer, API contracts, integration tests |
| ⚠️ **Two Database Queries** | Cache profile data trong Gateway (Redis), TTL 1 hour |
| ⚠️ **Data Consistency** | Transaction outbox pattern, eventual consistency |
| ⚠️ **Complex Queries** | Denormalize nếu cần, API Gateway aggregation pattern |
| ⚠️ **Orphan Records Risk** | Background job để cleanup orphan records |

---

## 🔒 Security Considerations

### Service-to-Service Authentication

**Option 1: Shared Secret Token**
```yaml
# Gateway & Core application.yml
services:
  auth:
    internal-token: ${INTERNAL_SERVICE_TOKEN}  # Same secret
```

```java
@Component
public class ServiceAuthFilter implements WebFilter {
    @Value("${services.auth.internal-token}")
    private String internalToken;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String serviceToken = exchange.getRequest()
            .getHeaders()
            .getFirst("X-Service-Token");

        if (!internalToken.equals(serviceToken)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
```

**Option 2: mTLS (Production)**
- Certificate-based authentication
- More secure, harder to setup

---

## 📚 Related Documentation

- [Architecture Overview](architecture-overview.md) - Microservices architecture
- [Auth Module](../kiteclass-core/docs/modules/auth-module.md) - Authentication details
- [Database Design](../documents/plans/database-design.md) - Database schema
- [API Design](api-design.md) - Service-to-Service APIs

---

**Generated:** 2026-01-27
**Author:** KiteClass Team
**Status:** ✅ IMPLEMENTED
