# Backend Standards

**Version:** 2.0 (Consolidated)
**Gop tu:** code-style, api-design, database-design, enums-constants, error-logging,
maven-dependencies, ide-warnings-best-practices, spring-boot-upgrade-checklist

---

## Muc luc nhanh

| Can gi | Xem section |
|--------|-------------|
| Package structure / naming | [1. Code Style](#1-code-style) |
| REST API conventions | [2. API Design](#2-api-design) |
| Database naming, audit columns | [3. Database Design](#3-database-design) |
| Enums & Constants (Java + TS) | [4. Enums & Constants](#4-enums--constants) |
| Error codes, exception handling | [5. Error Handling & Logging](#5-error-handling--logging) |
| Maven dependencies, versions | [6. Maven Dependencies](#6-maven-dependencies) |
| IDE warnings, Spring upgrade | [7. IDE & Upgrade Tips](#7-ide--upgrade-tips) |

---

## 1. Code Style

### Package Structure

```
com.kiteclass.{service}/
├── config/           # Configuration classes
├── controller/       # REST controllers
├── service/          # Business logic
│   └── impl/         # Service implementations
├── repository/       # Data access
├── entity/           # JPA entities
├── dto/              # Data transfer objects
│   ├── request/      # Request DTOs
│   └── response/     # Response DTOs
├── mapper/           # Entity-DTO mappers
├── exception/        # Custom exceptions
├── util/             # Utility classes
└── constant/         # Constants & enums
```

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Package | lowercase | `com.kiteclass.core` |
| Class | PascalCase | `StudentService`, `InvoiceDTO` |
| Method | camelCase verb | `findById()`, `calculateTotal()` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Enum | PascalCase, UPPER values | `PaymentStatus.PAID` |

### Class Patterns

```java
// Controller: @RestController
public class StudentController { }

// Service interface + impl
public interface StudentService { }
public class StudentServiceImpl implements StudentService { }

// DTOs (Java Records preferred)
public record CreateStudentRequest(String name, String email) { }
public record StudentResponse(Long id, String name) { }

// Exceptions
public class StudentNotFoundException extends RuntimeException { }
```

### Java Best Practices

```java
// Records for DTOs (Java 14+)
public record CreateStudentRequest(
    @NotBlank String name,
    @Email String email
) {}

// @Builder on entities
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    // ...
}

// List.of() instead of Arrays.asList()
List<String> roles = List.of("OWNER", "ADMIN");

// Explicit Lombok imports (NEVER wildcard)
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
```

---

## 2. API Design

### URL Conventions

```
# Collection
GET    /api/v1/students          # Danh sach
POST   /api/v1/students          # Tao moi

# Single resource
GET    /api/v1/students/{id}
PUT    /api/v1/students/{id}
DELETE /api/v1/students/{id}     # Soft delete

# Nested resources
GET    /api/v1/classes/{id}/students
POST   /api/v1/classes/{id}/attendance

# Actions (non-CRUD)
POST   /api/v1/invoices/{id}/send
POST   /api/v1/students/{id}/enroll
```

### Standard Response Format

```java
// Success
{
  "success": true,
  "data": { ... },
  "message": "Created successfully"
}

// Error
{
  "success": false,
  "error": {
    "code": "STUDENT_NOT_FOUND",
    "message": "Khong tim thay hoc vien"
  }
}

// Paginated
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 10,
    "page": 0,
    "size": 10
  }
}
```

### Controller Pattern

```java
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(studentService.findAll(page, size)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        return ApiResponse.success(studentService.create(request));
    }
}
```

### Pagination Parameters

| Param | Default | Description |
|-------|---------|-------------|
| `page` | 0 | Zero-based page index |
| `size` | 20 | Page size (max 100) |
| `sort` | `createdAt,desc` | Sort field and direction |

---

## 3. Database Design

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Tables | snake_case, plural | `students`, `class_schedules` |
| Columns | snake_case | `first_name`, `created_at` |
| Primary Keys | `id` (BIGSERIAL) | `id` |
| Foreign Keys | `{table}_id` | `student_id` |
| Indexes | `idx_{table}_{columns}` | `idx_students_email` |

### Standard Audit Columns (BAT BUOC cho moi table)

```sql
id          BIGSERIAL PRIMARY KEY,
created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
created_by  BIGINT REFERENCES users(id),
updated_by  BIGINT REFERENCES users(id),
deleted     BOOLEAN DEFAULT FALSE NOT NULL,
deleted_at  TIMESTAMP WITH TIME ZONE,
version     INTEGER DEFAULT 0 NOT NULL
```

### Flyway Migration Rules

- Moi migration file: `V{number}__{description}.sql` (eg: `V12__add_index_students_email.sql`)
- KHONG bao gio DROP column/table ma khong co retention period
- Idempotent: migration phai safe to run multiple times
- Backward compatible: code moi phai work voi schema cu

### KiteClass Architecture

- **KiteHub DB**: Single shared DB (sales, subscriptions, instances, themes)
- **KiteClass Instance DB**: Database-per-tenant (complete isolation)
  - User, Class, Learning, Billing, Gamification, Parent, Settings modules

---

## 4. Enums & Constants

### Java Enum Pattern

```java
public enum UserRole {
    OWNER("Chu trung tam", "Full access"),
    ADMIN("Quan tri vien", "Manage users, classes, billing"),
    TEACHER("Giao vien", "Manage assigned classes"),
    STAFF("Nhan vien", "Limited access"),
    PARENT("Phu huynh", "View children's info");

    private final String displayNameVi;
    private final String description;

    UserRole(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }
    // getters...
}
```

### TypeScript Enum Pattern

```typescript
export const UserRole = {
  OWNER: 'OWNER',
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  STAFF: 'STAFF',
  PARENT: 'PARENT',
} as const;

export type UserRole = (typeof UserRole)[keyof typeof UserRole];

export const UserRoleLabels: Record<UserRole, string> = {
  OWNER: 'Chu trung tam',
  ADMIN: 'Quan tri vien',
  TEACHER: 'Giao vien',
  STAFF: 'Nhan vien',
  PARENT: 'Phu huynh',
};
```

### Common Enums

- `UserStatus`: ACTIVE, INACTIVE, PENDING, LOCKED, DELETED
- `ClassStatus`: UPCOMING, ACTIVE, COMPLETED, CANCELLED
- `InvoiceStatus`: DRAFT, SENT, PAID, OVERDUE, CANCELLED
- `PaymentStatus`: PENDING, COMPLETED, FAILED, REFUNDED
- `PricingTier`: FREE, BASIC, PREMIUM, ENTERPRISE

---

## 5. Error Handling & Logging

### Error Code Format

```
{DOMAIN}_{CATEGORY}_{SPECIFIC}
```

| Domain | Examples |
|--------|---------|
| AUTH | AUTH_INVALID_CREDENTIALS, AUTH_TOKEN_EXPIRED |
| USER | USER_NOT_FOUND, USER_EMAIL_EXISTS |
| STUDENT | STUDENT_NOT_FOUND, STUDENT_ALREADY_ENROLLED |
| CLASS | CLASS_NOT_FOUND, CLASS_FULL, CLASS_SCHEDULE_CONFLICT |
| BILLING | BILLING_INVOICE_NOT_FOUND, BILLING_PAYMENT_FAILED |
| SYSTEM | SYSTEM_INTERNAL_ERROR, SYSTEM_SERVICE_UNAVAILABLE |

### GlobalExceptionHandler Pattern

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ApiErrorResponse.of(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(ValidationException ex) {
        return ApiErrorResponse.of(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiErrorResponse.of("SYSTEM_INTERNAL_ERROR", "Internal server error");
    }
}
```

### Exception Ctor — Overload Resolution Gotcha (added 2026-05-06 per GAP-357)

`ValidationException` + `EntityNotFoundException` ship BOTH deprecated single-arg ctors AND non-deprecated `(String errorCode, Object... args)` varargs. Java overload resolution picks the **most specific** match — so `new ValidationException("CODE")` resolves to the **deprecated** `(String message)` ctor, not the varargs.

**3-pattern fix matrix** to force varargs resolution:

| Call shape | Fix | Example |
|---|---|---|
| No args | Add explicit empty array | `new ValidationException("CODE", new Object[0])` |
| Single `Long` arg | Cast to `Object` | `new EntityNotFoundException("CODE", (Object) id)` |
| ≥2 args | Already resolves to varargs | `new ValidationException("CODE", arg1, arg2)` (no fix needed) |

**Full migration scope** (per call site needing migration):
1. Design error code (e.g. `INCIDENT_TITLE_REQUIRED` not `"Title is required"`)
2. Add to `messages.properties` + `messages_vi.properties` (en + vi mirrored)
3. Replace call site with new ctor + cast/array if needed
4. **Update tests** — `getMessage()` returns the code (no MessageSource resolution at construction); `hasMessageContaining("Title")` becomes `hasMessageContaining("INCIDENT_TITLE_REQUIRED")`

**Effort:** N call sites = ~N error codes + ~2N properties entries (en+vi) + ~M test updates. **Per-module agent or wave-pack scope, NOT housekeeping.** Reference: `feedback_deprecated_ctor_overload_resolution.md` memory + GAP-357.

### Logging Standards

```java
// Logging levels:
log.trace("Detailed debug info (rarely used)");
log.debug("Debug info for dev (never in prod critical paths)");
log.info("Business events: {}", studentId);    // "Student created: {}"
log.warn("Unusual but handled: {}", reason);   // "Retry attempt: {}"
log.error("Error with context", exception);     // Always include exception

// NEVER log sensitive data:
// log.info("Password: {}", password);  // BAD
// log.info("Token: {}", token);        // BAD
```

---

## 6. Maven Dependencies

### Spring Boot & Spring Cloud Versions

| Framework | Version |
|-----------|---------|
| Spring Boot | **3.5.x** (LTS) |
| Spring Cloud | **2024.0.0** (compatible voi 3.5.x) |
| Java | **17** or **21** (LTS) |

**QUAN TRONG:** KHONG dung Spring Boot < 3.4.x (het support)

### Dependencies phai co explicit version

```xml
<!-- JWT - JJWT (khong quan ly boi Spring Boot) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>

<!-- Rate Limiting - Bucket4j -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

### Dependencies KHONG can explicit version (Spring Boot manages)

```xml
<!-- Spring Boot manages these versions automatically -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-actuator
postgresql (via spring-boot-starter-data-jpa)
flyway-core
lombok
```

### Kiem tra truoc khi add dependency moi

1. Check Spring Boot BOM: `spring.io/projects/spring-boot` → Dependencies
2. Neu co trong BOM: dung khong can `<version>`
3. Neu khong: specify exact version va ghi comment ly do

---

## 7. IDE & Upgrade Tips

### IDE Warnings - Unknown Spring Properties

```json
// src/main/resources/META-INF/additional-spring-configuration-metadata.json
{
  "properties": [
    {
      "name": "custom.property.name",
      "type": "java.lang.String",
      "description": "Description of the property"
    }
  ]
}
```

### Spring Boot Upgrade Checklist

Truoc khi upgrade:
1. Check Spring Cloud compatibility matrix: `spring.io/projects/spring-cloud`
2. Run `mvn versions:display-dependency-updates`
3. Ensure all integration tests pass (Testcontainers)
4. Update GitHub Actions versions (xem `devops/devops-standards.md`)

Key changes Spring Boot 3.4.0+:
- `@MockBean` deprecated → dung `@TestConfiguration`
- Bucket4j: `Bandwidth.classic()` deprecated → dung `Bandwidth.builder()`

### MapStruct - Unmapped Target Properties

```java
@Mapper(componentModel = "spring")
public interface StudentMapper {

    // Ignore audit fields set automatically
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)   // Set via separate endpoint
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Student student, UpdateStudentRequest request);
}
```
