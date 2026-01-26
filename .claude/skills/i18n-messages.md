# Skill: Internationalization (i18n) & Messages

Quy chuẩn quản lý messages và đa ngữ cho KiteClass Platform.

## Mô tả

Tài liệu quy định:
- KHÔNG hardcode messages trong code
- Sử dụng message codes + MessageSource
- Cấu trúc file messages properties
- Pattern cho error messages

## Trigger phrases

- "error message"
- "i18n"
- "internationalization"
- "đa ngữ"
- "message"
- "thông báo lỗi"

---

## Nguyên tắc bắt buộc

1. **KHÔNG hardcode messages trong Java code**
2. **Sử dụng message codes** (VD: `error.user.not_found`)
3. **Messages được định nghĩa trong** `messages.properties`
4. **Hỗ trợ parameters** cho dynamic values

---

## Cấu trúc Message Codes

### Format

```
{category}.{entity}.{action}
```

### Categories

| Category | Mô tả | Ví dụ |
|----------|-------|-------|
| `error` | Lỗi | `error.user.not_found` |
| `validation` | Validation | `validation.email.invalid` |
| `success` | Thành công | `success.user.created` |
| `info` | Thông tin | `info.payment.pending` |

### Ví dụ Message Codes

```properties
# Error messages
error.entity.not_found={0} với ID {1} không tồn tại
error.entity.duplicate={0} đã tồn tại
error.internal=Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.
error.unauthorized=Bạn không có quyền truy cập
error.forbidden=Truy cập bị từ chối

# Auth errors
error.auth.invalid_credentials=Email hoặc mật khẩu không đúng
error.auth.account_locked=Tài khoản đã bị khóa
error.auth.account_inactive=Tài khoản chưa được kích hoạt
error.auth.token_expired=Phiên đăng nhập đã hết hạn
error.auth.token_invalid=Token không hợp lệ

# Validation messages
validation.required={0} là bắt buộc
validation.min_length={0} phải có ít nhất {1} ký tự
validation.max_length={0} không được vượt quá {1} ký tự
validation.email.invalid=Email không hợp lệ
validation.phone.invalid=Số điện thoại không hợp lệ (phải có 10 số, bắt đầu bằng 0)
validation.date.invalid=Ngày không hợp lệ
validation.data.invalid=Dữ liệu không hợp lệ

# Success messages
success.user.created=Tạo người dùng thành công
success.user.updated=Cập nhật thành công
success.user.deleted=Xóa thành công
success.password.changed=Đổi mật khẩu thành công
success.password.reset_sent=Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu
```

---

## File Structure

```
src/main/resources/
├── messages.properties          # Default (Vietnamese)
├── messages_en.properties       # English
└── messages_vi.properties       # Vietnamese (explicit)
```

---

## Spring Configuration

### MessageSource Bean

```java
package com.kiteclass.gateway.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
            new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600); // Reload every hour in dev
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
```

---

## MessageService

```java
package com.kiteclass.gateway.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service for retrieving i18n messages.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Get message by code with current locale.
     */
    public String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    /**
     * Get message by code with parameters.
     */
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * Get message with specific locale.
     */
    public String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }
}
```

---

## Message Codes Class

```java
package com.kiteclass.gateway.common.constant;

/**
 * Centralized message codes for i18n.
 *
 * <p>IMPORTANT: All messages must be defined in messages.properties
 */
public final class MessageCodes {

    private MessageCodes() {}

    // Error codes
    public static final String ENTITY_NOT_FOUND = "error.entity.not_found";
    public static final String ENTITY_DUPLICATE = "error.entity.duplicate";
    public static final String INTERNAL_ERROR = "error.internal";
    public static final String UNAUTHORIZED = "error.unauthorized";
    public static final String FORBIDDEN = "error.forbidden";

    // Auth codes
    public static final String AUTH_INVALID_CREDENTIALS = "error.auth.invalid_credentials";
    public static final String AUTH_ACCOUNT_LOCKED = "error.auth.account_locked";
    public static final String AUTH_ACCOUNT_INACTIVE = "error.auth.account_inactive";
    public static final String AUTH_TOKEN_EXPIRED = "error.auth.token_expired";
    public static final String AUTH_TOKEN_INVALID = "error.auth.token_invalid";

    // Validation codes
    public static final String VALIDATION_REQUIRED = "validation.required";
    public static final String VALIDATION_MIN_LENGTH = "validation.min_length";
    public static final String VALIDATION_MAX_LENGTH = "validation.max_length";
    public static final String VALIDATION_EMAIL_INVALID = "validation.email.invalid";
    public static final String VALIDATION_PHONE_INVALID = "validation.phone.invalid";
    public static final String VALIDATION_DATA_INVALID = "validation.data.invalid";

    // Success codes
    public static final String SUCCESS_CREATED = "success.user.created";
    public static final String SUCCESS_UPDATED = "success.user.updated";
    public static final String SUCCESS_DELETED = "success.user.deleted";
}
```

---

## Updated Exception Classes

### BusinessException

```java
@Getter
public class BusinessException extends RuntimeException {

    private final String code;          // Message code (e.g., "error.auth.invalid_credentials")
    private final HttpStatus status;
    private final Object[] args;        // Arguments for message formatting

    public BusinessException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
        this.args = null;
    }

    public BusinessException(String code, HttpStatus status, Object... args) {
        super(code);
        this.code = code;
        this.status = status;
        this.args = args;
    }
}
```

### EntityNotFoundException

```java
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String entityName, Long id) {
        super(MessageCodes.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND, entityName, id);
    }

    public EntityNotFoundException(String entityName, String field, String value) {
        super(MessageCodes.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND, entityName, value);
    }
}
```

---

## Updated GlobalExceptionHandler

```java
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(
            BusinessException ex,
            ServerWebExchange exchange) {

        // Resolve message from code
        String message = ex.getArgs() != null
            ? messageService.getMessage(ex.getCode(), ex.getArgs())
            : messageService.getMessage(ex.getCode());

        String path = exchange.getRequest().getPath().value();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), message, path);

        return Mono.just(ResponseEntity.status(ex.getStatus()).body(response));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            WebExchangeBindException ex,
            ServerWebExchange exchange) {

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                    .add(error.getDefaultMessage()); // Already resolved by validator
        }

        String path = exchange.getRequest().getPath().value();
        String message = messageService.getMessage(MessageCodes.VALIDATION_DATA_INVALID);
        ErrorResponse response = ErrorResponse.withFieldErrors(
                MessageCodes.VALIDATION_DATA_INVALID,
                message,
                path,
                fieldErrors
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnexpectedException(
            Exception ex,
            ServerWebExchange exchange) {
        log.error("Unexpected exception", ex);

        String path = exchange.getRequest().getPath().value();
        String message = messageService.getMessage(MessageCodes.INTERNAL_ERROR);
        ErrorResponse response = ErrorResponse.of(MessageCodes.INTERNAL_ERROR, message, path);

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}
```

---

## Usage Examples

### Throwing Exceptions

```java
// ❌ KHÔNG hardcode message
throw new BusinessException("AUTH_ERROR", "Email không đúng", HttpStatus.UNAUTHORIZED);

// ✅ Sử dụng message code
throw new BusinessException(MessageCodes.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);

// ✅ Với parameters
throw new EntityNotFoundException("User", userId);
// -> "User với ID 123 không tồn tại"
```

### In Service Layer

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final MessageService messageService;

    public UserResponse createUser(CreateUserRequest request) {
        // ... create user ...

        // Return success message
        String message = messageService.getMessage(MessageCodes.SUCCESS_CREATED);
        return ApiResponse.success(user, message);
    }
}
```

### Bean Validation

```java
public record CreateUserRequest(
    @NotBlank(message = "{validation.required}")
    @Size(min = 2, max = 100, message = "{validation.min_length}")
    String name,

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email.invalid}")
    String email,

    @Pattern(regexp = "^0\\d{9}$", message = "{validation.phone.invalid}")
    String phone
) {}
```

---

## Checklist

Khi code exception/message:

1. ✅ Tạo message code trong `MessageCodes.java`
2. ✅ Thêm message vào `messages.properties`
3. ✅ Sử dụng `MessageService` hoặc exception với code
4. ❌ KHÔNG hardcode string message trong Java
5. ❌ KHÔNG dùng String.format() cho error messages

---

## Actions

### Thêm message mới
1. Thêm constant vào `MessageCodes.java`
2. Thêm message vào `messages.properties`
3. (Optional) Thêm vào `messages_en.properties`

### Khi review code
- Tìm hardcoded Vietnamese strings
- Đảm bảo tất cả exception dùng message codes
- Verify messages.properties có đầy đủ entries
