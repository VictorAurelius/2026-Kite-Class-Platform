# Skill: Error Handling & Logging

Quy chuẩn xử lý lỗi và logging cho KiteClass Platform.

## Mô tả

Tài liệu quy định:
- Error codes và messages chuẩn
- Exception handling patterns
- Logging levels và formats
- Monitoring và alerting
- Frontend error handling

## Trigger phrases

- "error handling"
- "xử lý lỗi"
- "logging"
- "error codes"
- "exception"

---

## Error Code System

### Error Code Format

```
{DOMAIN}_{CATEGORY}_{SPECIFIC}
```

| Component | Values | Ví dụ |
|-----------|--------|-------|
| DOMAIN | AUTH, USER, STUDENT, CLASS, BILLING, SYSTEM | AUTH |
| CATEGORY | VALIDATION, NOT_FOUND, CONFLICT, FORBIDDEN, INTERNAL | VALIDATION |
| SPECIFIC | Mô tả cụ thể | INVALID_CREDENTIALS |

### Error Codes Master List

#### Authentication (AUTH_)

| Code | HTTP | Message (VI) | Message (EN) |
|------|------|--------------|--------------|
| `AUTH_INVALID_CREDENTIALS` | 401 | Sai email hoặc mật khẩu | Invalid email or password |
| `AUTH_TOKEN_EXPIRED` | 401 | Phiên đăng nhập đã hết hạn | Session expired |
| `AUTH_TOKEN_INVALID` | 401 | Token không hợp lệ | Invalid token |
| `AUTH_REFRESH_EXPIRED` | 401 | Vui lòng đăng nhập lại | Please login again |
| `AUTH_ACCOUNT_LOCKED` | 403 | Tài khoản đã bị khóa | Account is locked |
| `AUTH_ACCOUNT_INACTIVE` | 403 | Tài khoản chưa được kích hoạt | Account is inactive |
| `AUTH_PERMISSION_DENIED` | 403 | Bạn không có quyền thực hiện | Permission denied |

#### User (USER_)

| Code | HTTP | Message |
|------|------|---------|
| `USER_NOT_FOUND` | 404 | Không tìm thấy người dùng |
| `USER_EMAIL_EXISTS` | 409 | Email đã được sử dụng |
| `USER_PHONE_EXISTS` | 409 | Số điện thoại đã được sử dụng |
| `USER_INVALID_PASSWORD` | 400 | Mật khẩu không đúng định dạng |

#### Student (STUDENT_)

| Code | HTTP | Message |
|------|------|---------|
| `STUDENT_NOT_FOUND` | 404 | Không tìm thấy học viên |
| `STUDENT_ALREADY_ENROLLED` | 409 | Học viên đã đăng ký lớp này |
| `STUDENT_EMAIL_EXISTS` | 409 | Email học viên đã tồn tại |
| `STUDENT_CANNOT_DELETE` | 422 | Không thể xóa học viên đang có lớp |

#### Class (CLASS_)

| Code | HTTP | Message |
|------|------|---------|
| `CLASS_NOT_FOUND` | 404 | Không tìm thấy lớp học |
| `CLASS_FULL` | 422 | Lớp đã đủ học viên |
| `CLASS_SCHEDULE_CONFLICT` | 409 | Trùng lịch với lớp khác |
| `CLASS_ALREADY_STARTED` | 422 | Lớp đã bắt đầu, không thể sửa |

#### Billing (BILLING_)

| Code | HTTP | Message |
|------|------|---------|
| `INVOICE_NOT_FOUND` | 404 | Không tìm thấy hóa đơn |
| `INVOICE_ALREADY_PAID` | 422 | Hóa đơn đã được thanh toán |
| `INVOICE_CANCELLED` | 422 | Hóa đơn đã bị hủy |
| `PAYMENT_FAILED` | 422 | Thanh toán thất bại |
| `PAYMENT_AMOUNT_MISMATCH` | 400 | Số tiền thanh toán không khớp |

#### Validation (VALIDATION_)

| Code | HTTP | Message |
|------|------|---------|
| `VALIDATION_ERROR` | 400 | Dữ liệu không hợp lệ |
| `VALIDATION_REQUIRED` | 400 | Trường bắt buộc |
| `VALIDATION_FORMAT` | 400 | Sai định dạng |
| `VALIDATION_RANGE` | 400 | Giá trị ngoài phạm vi cho phép |

#### System (SYSTEM_)

| Code | HTTP | Message |
|------|------|---------|
| `SYSTEM_INTERNAL_ERROR` | 500 | Lỗi hệ thống, vui lòng thử lại |
| `SYSTEM_SERVICE_UNAVAILABLE` | 503 | Dịch vụ tạm thời không khả dụng |
| `SYSTEM_RATE_LIMIT` | 429 | Quá nhiều yêu cầu, vui lòng chờ |
| `SYSTEM_MAINTENANCE` | 503 | Hệ thống đang bảo trì |

---

## Backend Exception Handling

### Exception Classes

```java
// Base exception
public abstract class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object[] args;

    protected BusinessException(String errorCode, HttpStatus status, Object... args) {
        super(errorCode);
        this.errorCode = errorCode;
        this.httpStatus = status;
        this.args = args;
    }

    // Getters...
}

// Specific exceptions
public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String entity, Object id) {
        super(entity.toUpperCase() + "_NOT_FOUND", HttpStatus.NOT_FOUND, id);
    }
}

public class ValidationException extends BusinessException {
    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors;
    }
}

public class ConflictException extends BusinessException {
    public ConflictException(String errorCode, Object... args) {
        super(errorCode, HttpStatus.CONFLICT, args);
    }
}

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String errorCode) {
        super(errorCode, HttpStatus.FORBIDDEN);
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, Locale locale) {

        String message = messageSource.getMessage(
            ex.getErrorCode(), ex.getArgs(), locale);

        ErrorResponse response = ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(message)
            .timestamp(Instant.now())
            .build();

        log.warn("Business exception: {} - {}", ex.getErrorCode(), message);

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            ValidationException ex) {

        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Dữ liệu không hợp lệ")
            .errors(ex.getFieldErrors())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (a, b) -> a
            ));

        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Dữ liệu không hợp lệ")
            .errors(errors)
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse response = ErrorResponse.builder()
            .code("SYSTEM_INTERNAL_ERROR")
            .message("Lỗi hệ thống, vui lòng thử lại sau")
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
```

### Error Response DTOs

```java
@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private Instant timestamp;
    private String path;
    private String traceId;
}

@Data
@Builder
public class ValidationErrorResponse extends ErrorResponse {
    private Map<String, String> errors;
}
```

---

## Logging

### Logging Levels

| Level | Khi nào dùng | Ví dụ |
|-------|--------------|-------|
| **ERROR** | Lỗi nghiêm trọng cần xử lý ngay | Exception không mong đợi, database down |
| **WARN** | Vấn đề tiềm ẩn nhưng không critical | Business exception, deprecated API |
| **INFO** | Thông tin quan trọng về hoạt động | Request/response, state changes |
| **DEBUG** | Chi tiết để debug | Variable values, flow tracing |
| **TRACE** | Chi tiết nhất | SQL queries, full payloads |

### Logging Format

```
timestamp | level | traceId | spanId | service | class | message
```

```
2025-02-10 10:30:45.123 | INFO | abc123 | def456 | core-service | StudentService | Created student id=1 name=Nguyen Van A
```

### Logging Configuration

```yaml
# application.yml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} | %5level | %X{traceId:-} | %X{spanId:-} | ${spring.application.name} | %logger{36} | %msg%n"
  level:
    root: INFO
    com.kiteclass: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
```

### Logging Best Practices

```java
@Slf4j
@Service
public class StudentService {

    public StudentDTO createStudent(CreateStudentRequest request) {
        log.info("Creating student: email={}", request.getEmail());

        try {
            // Business logic
            Student student = studentRepository.save(entity);

            log.info("Created student: id={}, name={}", student.getId(), student.getName());
            return StudentDTO.from(student);

        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate email: {}", request.getEmail());
            throw new ConflictException("USER_EMAIL_EXISTS", request.getEmail());
        }
    }

    public void deleteStudent(Long id) {
        log.info("Deleting student: id={}", id);

        Student student = studentRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Student not found: id={}", id);
                return new EntityNotFoundException("STUDENT", id);
            });

        if (!student.getEnrollments().isEmpty()) {
            log.warn("Cannot delete student with active enrollments: id={}", id);
            throw new BusinessException("STUDENT_CANNOT_DELETE", HttpStatus.UNPROCESSABLE_ENTITY, id);
        }

        studentRepository.delete(student);
        log.info("Deleted student: id={}", id);
    }
}
```

### Logging Don'ts

```java
// ❌ Don't log sensitive data
log.info("Login attempt: email={}, password={}", email, password);

// ✅ Do this instead
log.info("Login attempt: email={}", email);

// ❌ Don't log entire objects (may contain sensitive data)
log.info("Created user: {}", user);

// ✅ Log specific fields
log.info("Created user: id={}, email={}", user.getId(), user.getEmail());

// ❌ Don't use string concatenation
log.info("Processing user " + userId);

// ✅ Use parameterized logging
log.info("Processing user: id={}", userId);
```

---

## Frontend Error Handling

### API Error Handling

```typescript
// lib/api-client.ts
import axios, { AxiosError } from 'axios';
import { toast } from 'sonner';

export interface ApiError {
  code: string;
  message: string;
  errors?: Record<string, string>;
}

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
});

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    const apiError = error.response?.data;

    // Handle specific errors
    if (error.response?.status === 401) {
      // Redirect to login
      window.location.href = '/login';
      return Promise.reject(error);
    }

    if (error.response?.status === 403) {
      toast.error('Bạn không có quyền thực hiện hành động này');
      return Promise.reject(error);
    }

    // Show error message
    if (apiError?.message) {
      toast.error(apiError.message);
    } else {
      toast.error('Có lỗi xảy ra, vui lòng thử lại');
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

### React Query Error Handling

```typescript
// hooks/use-students.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';

export function useCreateStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateStudentRequest) =>
      apiClient.post('/students', data),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      toast.success('Thêm học viên thành công');
    },

    onError: (error: AxiosError<ApiError>) => {
      // Error already handled by interceptor
      // But can add specific handling here
      const code = error.response?.data?.code;

      if (code === 'STUDENT_EMAIL_EXISTS') {
        // Set form error
        form.setError('email', {
          message: 'Email đã được sử dụng'
        });
      }
    },
  });
}
```

### Form Validation Errors

```typescript
// Display validation errors from API
function StudentForm() {
  const form = useForm<StudentFormValues>();
  const { mutate, error } = useCreateStudent();

  // Handle API validation errors
  useEffect(() => {
    if (error?.response?.data?.errors) {
      const errors = error.response.data.errors;
      Object.entries(errors).forEach(([field, message]) => {
        form.setError(field as keyof StudentFormValues, {
          type: 'server',
          message,
        });
      });
    }
  }, [error, form]);

  return (
    <Form {...form}>
      {/* Form fields */}
    </Form>
  );
}
```

### Error Boundary

```typescript
// components/error-boundary.tsx
'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';

export default function ErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log error to monitoring service
    console.error('Error:', error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] space-y-4">
      <h2 className="text-xl font-semibold">Đã xảy ra lỗi</h2>
      <p className="text-muted-foreground">
        Vui lòng thử lại hoặc liên hệ hỗ trợ nếu lỗi tiếp tục xảy ra.
      </p>
      <Button onClick={reset}>Thử lại</Button>
    </div>
  );
}
```

---

## Monitoring & Alerting

### Metrics to Monitor

| Metric | Mô tả | Alert Threshold |
|--------|-------|-----------------|
| Error rate | % requests lỗi | > 1% |
| Response time P99 | Latency 99th percentile | > 2s |
| 5xx errors | Server errors | > 10/minute |
| Failed logins | Auth failures | > 50/minute |
| Queue depth | RabbitMQ messages | > 1000 |

### Structured Logging for ELK

```java
// Use MDC for context
MDC.put("userId", userId);
MDC.put("tenantId", tenantId);
MDC.put("action", "CREATE_STUDENT");

log.info("Student created successfully");

MDC.clear();
```

### Health Checks

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            return Health.up()
                .withDetail("database", "PostgreSQL")
                .build();
        } catch (SQLException ex) {
            return Health.down()
                .withException(ex)
                .build();
        }
    }
}
```

## Actions

### Thêm error code mới
1. Định nghĩa trong enum `ErrorCode`
2. Thêm message vào `messages.properties`
3. Update tài liệu này

### Xem logs
```bash
# Docker logs
docker logs -f kiteclass-core

# Kubernetes
kubectl logs -f deployment/core-service -n kiteclass
```
