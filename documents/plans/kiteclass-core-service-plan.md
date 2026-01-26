# PLAN: KiteClass Core Service Implementation

## Thông tin

| Thuộc tính | Giá trị |
|------------|---------|
| **Service** | kiteclass-core-service |
| **Tech Stack** | Spring Boot 3.2+, Java 17, PostgreSQL 15 |
| **Mục đích** | Core business logic: Classes, Students, Attendance, Billing |
| **Tham chiếu** | architecture-overview, database-design, api-design |

---

# PHASE 1: PROJECT INITIALIZATION

## 1.1. Tạo Project Structure

```
kiteclass/
└── kiteclass-core/
    ├── src/
    │   ├── main/
    │   │   ├── java/com/kiteclass/core/
    │   │   └── resources/
    │   └── test/
    │       └── java/com/kiteclass/core/
    ├── pom.xml
    ├── Dockerfile
    └── README.md
```

## 1.2. Maven Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
    </parent>

    <groupId>com.kiteclass</groupId>
    <artifactId>kiteclass-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>KiteClass Core Service</name>
    <description>Core business service for KiteClass Instance</description>

    <properties>
        <java.version>17</java.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <springdoc.version>2.3.0</springdoc.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- OpenAPI Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Monitoring -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## 1.3. Application Entry Point

```java
// src/main/java/com/kiteclass/core/KiteclassCoreApplication.java
package com.kiteclass.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KiteclassCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(KiteclassCoreApplication.class, args);
    }
}
```

## 1.4. Application Configuration

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: kiteclass-core

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/kiteclass_dev}
    username: ${SPRING_DATASOURCE_USERNAME:kiteclass}
    password: ${SPRING_DATASOURCE_PASSWORD:kiteclass123}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    open-in-view: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: ${SPRING_REDIS_PORT:6379}

  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
    port: ${SPRING_RABBITMQ_PORT:5672}
    username: ${SPRING_RABBITMQ_USERNAME:kiteclass}
    password: ${SPRING_RABBITMQ_PASSWORD:kiteclass123}

server:
  port: 8081

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-super-secret-key-min-512-bits-long-for-hs512-algorithm}
  expiration: ${JWT_EXPIRATION:3600000}

# Logging
logging:
  level:
    root: INFO
    com.kiteclass: DEBUG
    org.hibernate.SQL: DEBUG

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true

# OpenAPI
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

# PHASE 2: COMMON/SHARED COMPONENTS

## 2.1. Package Structure

```
com.kiteclass.core/
├── config/              # Configuration classes
├── common/              # Shared components
│   ├── dto/             # Common DTOs
│   ├── entity/          # Base entities
│   ├── exception/       # Exceptions & handlers
│   ├── util/            # Utilities
│   └── constant/        # Constants & enums
├── security/            # Security config & JWT
└── module/              # Business modules
```

## 2.2. Common Entities

### BaseEntity.java

```java
package com.kiteclass.core.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by")
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
    }
}
```

## 2.3. Common DTOs

### ApiResponse.java

```java
package com.kiteclass.core.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private String message;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
```

### PageResponse.java

```java
package com.kiteclass.core.common.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
```

### ErrorResponse.java

```java
package com.kiteclass.core.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String code;
    private String message;
    private Map<String, String> details;
    private Instant timestamp;
    private String path;
    private String traceId;
}
```

## 2.4. Exception Handling

### BusinessException.java

```java
package com.kiteclass.core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object[] args;

    public BusinessException(String errorCode, HttpStatus status, Object... args) {
        super(errorCode);
        this.errorCode = errorCode;
        this.httpStatus = status;
        this.args = args;
    }
}
```

### Specific Exceptions

```java
// EntityNotFoundException.java
package com.kiteclass.core.common.exception;

import org.springframework.http.HttpStatus;

public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String entity, Object id) {
        super(entity.toUpperCase() + "_NOT_FOUND", HttpStatus.NOT_FOUND, id);
    }
}

// DuplicateResourceException.java
public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String errorCode, Object... args) {
        super(errorCode, HttpStatus.CONFLICT, args);
    }
}

// ValidationException.java
public class ValidationException extends BusinessException {
    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
```

### GlobalExceptionHandler.java

```java
package com.kiteclass.core.common.exception;

import com.kiteclass.core.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request, Locale locale) {

        String message = messageSource.getMessage(
                ex.getErrorCode(), ex.getArgs(), ex.getErrorCode(), locale);

        log.warn("Business exception: {} - {}", ex.getErrorCode(), message);

        ErrorResponse response = ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Dữ liệu không hợp lệ")
                .details(ex.getFieldErrors())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid",
                        (a, b) -> a
                ));

        ErrorResponse response = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Dữ liệu không hợp lệ")
                .details(errors)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at {}: ", request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .code("SYSTEM_INTERNAL_ERROR")
                .message("Lỗi hệ thống, vui lòng thử lại sau")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

## 2.5. Constants & Enums

### Enums (Tham khảo skill enums-constants.md)

```java
// StudentStatus.java
package com.kiteclass.core.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudentStatus {
    ACTIVE("Đang học"),
    INACTIVE("Tạm nghỉ"),
    GRADUATED("Đã tốt nghiệp"),
    DROPPED("Đã nghỉ học"),
    PENDING("Chờ xác nhận");

    private final String displayName;
}

// ClassStatus.java
public enum ClassStatus {
    DRAFT("Nháp"),
    SCHEDULED("Đã lên lịch"),
    IN_PROGRESS("Đang diễn ra"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String displayName;
}

// AttendanceStatus.java
public enum AttendanceStatus {
    PRESENT("Có mặt", 10),
    ABSENT("Vắng", -10),
    LATE("Đi trễ", -5),
    EXCUSED("Có phép", 0);

    private final String displayName;
    private final int points;
}

// InvoiceStatus.java
public enum InvoiceStatus {
    DRAFT, SENT, PAID, PARTIAL, OVERDUE, CANCELLED
}

// PaymentMethod.java
public enum PaymentMethod {
    CASH, BANK_TRANSFER, MOMO, VNPAY, ZALOPAY
}

// DayOfWeek.java (sử dụng java.time.DayOfWeek)
```

## 2.6. Configuration Classes

### JpaConfig.java

```java
package com.kiteclass.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.empty();
            }
            // Extract user ID from principal
            // return Optional.of(((UserPrincipal) auth.getPrincipal()).getId());
            return Optional.empty(); // TODO: Implement after security
        };
    }
}
```

### CacheConfig.java

```java
package com.kiteclass.core.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("students", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("classes", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("settings", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
```

### RabbitConfig.java

```java
package com.kiteclass.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_EVENTS = "kiteclass.events";
    public static final String QUEUE_NOTIFICATIONS = "kiteclass.notifications";
    public static final String QUEUE_GAMIFICATION = "kiteclass.gamification";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE_EVENTS);
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS).build();
    }

    @Bean
    public Queue gamificationQueue() {
        return QueueBuilder.durable(QUEUE_GAMIFICATION).build();
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(eventsExchange)
                .with("attendance.#");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
```

---

# PHASE 3: BUSINESS MODULES

## Module Implementation Order (Priority)

| # | Module | Priority | Dependencies |
|---|--------|----------|--------------|
| 1 | **Student** | P0 | None |
| 2 | **Course** | P0 | None |
| 3 | **Class** | P0 | Course |
| 4 | **Enrollment** | P0 | Student, Class |
| 5 | **Attendance** | P0 | Class, Student |
| 6 | **Invoice** | P1 | Student, Enrollment |
| 7 | **Payment** | P1 | Invoice |
| 8 | **Parent** | P1 | Student |
| 9 | **Notification** | P2 | All above |

---

## 3.1. Module: Student

### Package Structure

```
module/student/
├── controller/
│   └── StudentController.java
├── service/
│   ├── StudentService.java
│   └── impl/
│       └── StudentServiceImpl.java
├── repository/
│   └── StudentRepository.java
├── entity/
│   └── Student.java
├── dto/
│   ├── StudentResponse.java
│   ├── CreateStudentRequest.java
│   ├── UpdateStudentRequest.java
│   └── StudentSearchCriteria.java
└── mapper/
    └── StudentMapper.java
```

### Entity

```java
// Student.java
package com.kiteclass.core.module.student.entity;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "students", indexes = {
    @Index(name = "idx_students_email", columnList = "email"),
    @Index(name = "idx_students_phone", columnList = "phone"),
    @Index(name = "idx_students_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Relationships will be added when implementing other modules
    // @OneToMany(mappedBy = "student")
    // private List<Enrollment> enrollments;
}
```

### DTOs

```java
// StudentResponse.java
package com.kiteclass.core.module.student.dto;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.StudentStatus;

import java.time.LocalDate;

public record StudentResponse(
    Long id,
    String name,
    String email,
    String phone,
    LocalDate dateOfBirth,
    Gender gender,
    String address,
    String avatarUrl,
    StudentStatus status,
    String note
) {}

// CreateStudentRequest.java
public record CreateStudentRequest(
    @NotBlank(message = "Tên là bắt buộc")
    @Size(min = 2, max = 100, message = "Tên phải từ 2-100 ký tự")
    String name,

    @Email(message = "Email không hợp lệ")
    @Size(max = 255)
    String email,

    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại không hợp lệ")
    String phone,

    LocalDate dateOfBirth,

    Gender gender,

    @Size(max = 1000)
    String address,

    String note
) {}

// UpdateStudentRequest.java
public record UpdateStudentRequest(
    @NotBlank(message = "Tên là bắt buộc")
    @Size(min = 2, max = 100)
    String name,

    @Email
    String email,

    @Pattern(regexp = "^0\\d{9}$")
    String phone,

    LocalDate dateOfBirth,

    Gender gender,

    String address,

    StudentStatus status,

    String note
) {}
```

### Repository

```java
package com.kiteclass.core.module.student.repository;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.module.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByIdAndDeletedFalse(Long id);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByPhoneAndDeletedFalse(String phone);

    @Query("""
        SELECT s FROM Student s
        WHERE s.deleted = false
        AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR s.status = :status)
    """)
    Page<Student> findBySearchCriteria(
            @Param("search") String search,
            @Param("status") StudentStatus status,
            Pageable pageable
    );

    List<Student> findByStatusAndDeletedFalse(StudentStatus status);

    long countByStatusAndDeletedFalse(StudentStatus status);
}
```

### Mapper

```java
package com.kiteclass.core.module.student.mapper;

import com.kiteclass.core.module.student.dto.*;
import com.kiteclass.core.module.student.entity.Student;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    StudentResponse toResponse(Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "deleted", constant = "false")
    Student toEntity(CreateStudentRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Student student, UpdateStudentRequest request);
}
```

### Service

```java
// StudentService.java
package com.kiteclass.core.module.student.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.student.dto.*;
import org.springframework.data.domain.Pageable;

public interface StudentService {
    StudentResponse createStudent(CreateStudentRequest request);
    StudentResponse getStudentById(Long id);
    PageResponse<StudentResponse> getStudents(String search, String status, Pageable pageable);
    StudentResponse updateStudent(Long id, UpdateStudentRequest request);
    void deleteStudent(Long id);
}

// StudentServiceImpl.java
package com.kiteclass.core.module.student.service.impl;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.student.dto.*;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.mapper.StudentMapper;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Override
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        log.info("Creating student with email: {}", request.email());

        validateUniqueConstraints(request.email(), request.phone(), null);

        Student student = studentMapper.toEntity(request);
        student = studentRepository.save(student);

        log.info("Created student with id: {}", student.getId());
        return studentMapper.toResponse(student);
    }

    @Override
    @Cacheable(value = "students", key = "#id")
    public StudentResponse getStudentById(Long id) {
        log.debug("Getting student by id: {}", id);
        return studentRepository.findByIdAndDeletedFalse(id)
                .map(studentMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Student", id));
    }

    @Override
    public PageResponse<StudentResponse> getStudents(String search, String status, Pageable pageable) {
        StudentStatus statusEnum = status != null ? StudentStatus.valueOf(status) : null;

        Page<Student> page = studentRepository.findBySearchCriteria(search, statusEnum, pageable);

        return PageResponse.from(page.map(studentMapper::toResponse));
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        log.info("Updating student id: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Student", id));

        validateUniqueConstraints(request.email(), request.phone(), id);

        studentMapper.updateEntity(student, request);
        student = studentRepository.save(student);

        log.info("Updated student id: {}", id);
        return studentMapper.toResponse(student);
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public void deleteStudent(Long id) {
        log.info("Deleting student id: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Student", id));

        // TODO: Check for active enrollments before delete
        // if (!student.getEnrollments().isEmpty()) {
        //     throw new BusinessException("STUDENT_CANNOT_DELETE", HttpStatus.UNPROCESSABLE_ENTITY);
        // }

        student.softDelete();
        studentRepository.save(student);

        log.info("Soft deleted student id: {}", id);
    }

    private void validateUniqueConstraints(String email, String phone, Long excludeId) {
        if (email != null) {
            boolean exists = excludeId == null
                    ? studentRepository.existsByEmailAndDeletedFalse(email)
                    : studentRepository.findByIdAndDeletedFalse(excludeId)
                            .map(s -> !s.getEmail().equals(email) &&
                                    studentRepository.existsByEmailAndDeletedFalse(email))
                            .orElse(false);

            if (exists) {
                throw new DuplicateResourceException("STUDENT_EMAIL_EXISTS", email);
            }
        }

        if (phone != null) {
            // Similar check for phone
        }
    }
}
```

### Controller

```java
package com.kiteclass.core.module.student.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.student.dto.*;
import com.kiteclass.core.module.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student management APIs")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Get all students with pagination and filtering")
    public ResponseEntity<PageResponse<StudentResponse>> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(studentService.getStudents(search, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student by ID")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudentById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new student")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {

        StudentResponse response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo học viên thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update student")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(studentService.updateStudent(id, request), "Cập nhật thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete student (soft delete)")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 3.2. Module: Course

(Tương tự Student module - Quản lý khóa học/chương trình)

### Key Entities

```java
@Entity
@Table(name = "courses")
public class Course extends BaseEntity {
    private String name;
    private String code;
    private String description;
    private Integer totalSessions;
    private BigDecimal defaultTuitionFee;
    private CourseStatus status;
}
```

---

## 3.3. Module: Class

### Key Entities

```java
@Entity
@Table(name = "classes")
public class ClassEntity extends BaseEntity {
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    private Integer maxStudents;
    private BigDecimal tuitionFee;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ClassStatus status;

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL)
    private List<ClassSchedule> schedules;
}

@Entity
@Table(name = "class_schedules")
public class ClassSchedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;

    private String room;
}

@Entity
@Table(name = "class_sessions")
public class ClassSession extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer sessionNumber;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    private String topic;
    private String note;
}
```

---

## 3.4. Module: Enrollment

```java
@Entity
@Table(name = "enrollments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "class_id"})
})
public class Enrollment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    private LocalDate enrollmentDate;
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal tuitionAmount;
    private BigDecimal discountPercent;
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    private String note;
}
```

---

## 3.5. Module: Attendance

```java
@Entity
@Table(name = "attendance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"session_id", "student_id"})
})
public class Attendance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    private LocalTime checkinTime;
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private User markedBy;
}
```

### Attendance Service (Key Methods)

```java
public interface AttendanceService {
    // Mark attendance for a session
    void markAttendance(Long sessionId, List<MarkAttendanceRequest> records);

    // Get attendance by class and date range
    List<AttendanceResponse> getAttendanceByClass(Long classId, LocalDate from, LocalDate to);

    // Get attendance statistics for a student
    StudentAttendanceStats getStudentAttendanceStats(Long studentId, Long classId);
}
```

---

## 3.6. Module: Invoice & Payment

```java
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {
    @Column(unique = true)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    private LocalDate issueDate;
    private LocalDate dueDate;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<InvoiceItem> items;

    @OneToMany(mappedBy = "invoice")
    private List<Payment> payments;
}

@Entity
@Table(name = "invoice_items")
public class InvoiceItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;
}

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private String transactionRef;
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String note;
}
```

---

# PHASE 4: DATABASE MIGRATIONS

## Flyway Migration Files

```
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__create_student_tables.sql
├── V3__create_course_class_tables.sql
├── V4__create_enrollment_tables.sql
├── V5__create_attendance_tables.sql
├── V6__create_billing_tables.sql
├── V7__create_settings_tables.sql
└── V8__seed_initial_data.sql
```

### V1__init_schema.sql

```sql
-- Create schemas
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS settings;

-- Set search path
SET search_path TO core, billing, settings, public;
```

### V2__create_student_tables.sql

```sql
CREATE TABLE core.students (
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

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0 NOT NULL,

    -- Constraints
    CONSTRAINT chk_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED', 'PENDING')),
    CONSTRAINT chk_students_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
);

CREATE UNIQUE INDEX idx_students_email ON core.students(email) WHERE deleted = false AND email IS NOT NULL;
CREATE UNIQUE INDEX idx_students_phone ON core.students(phone) WHERE deleted = false AND phone IS NOT NULL;
CREATE INDEX idx_students_status ON core.students(status) WHERE deleted = false;
CREATE INDEX idx_students_name ON core.students(name) WHERE deleted = false;
```

(Tiếp tục với các migration files khác theo database-design.md)

---

# PHASE 5: TESTING

## Test Structure

```
src/test/java/com/kiteclass/core/
├── module/
│   └── student/
│       ├── controller/
│       │   └── StudentControllerTest.java
│       ├── service/
│       │   └── StudentServiceTest.java
│       └── repository/
│           └── StudentRepositoryTest.java
├── integration/
│   └── StudentIntegrationTest.java
└── testutil/
    ├── IntegrationTestBase.java
    └── TestDataBuilder.java
```

## Test Examples

(Tham khảo skill testing-guide.md)

---

# PHASE 6: DOCKER & DEPLOYMENT

## Dockerfile

```dockerfile
# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S kiteclass && adduser -S kiteclass -G kiteclass
USER kiteclass

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## docker-compose.yml (local dev)

```yaml
version: '3.8'

services:
  core-service:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/kiteclass_dev
      - SPRING_REDIS_HOST=redis
      - SPRING_RABBITMQ_HOST=rabbitmq
    depends_on:
      - postgres
      - redis
      - rabbitmq
```

---

# IMPLEMENTATION CHECKLIST

## Phase 1: Project Setup
- [ ] Create Maven project with dependencies
- [ ] Configure application.yml for all profiles
- [ ] Create main application class
- [ ] Verify project builds and starts

## Phase 2: Common Components
- [ ] BaseEntity with audit fields
- [ ] Common DTOs (ApiResponse, PageResponse, ErrorResponse)
- [ ] Exception classes and GlobalExceptionHandler
- [ ] All Enums (StudentStatus, ClassStatus, etc.)
- [ ] Configuration classes (JPA, Cache, RabbitMQ)
- [ ] Message properties (i18n)

## Phase 3: Business Modules
- [ ] Student module (CRUD + search)
- [ ] Course module
- [ ] Class module with schedules
- [ ] Enrollment module
- [ ] Attendance module
- [ ] Invoice module
- [ ] Payment module
- [ ] Parent module

## Phase 4: Database
- [ ] All Flyway migrations
- [ ] Indexes optimization
- [ ] Seed data for testing

## Phase 5: Testing
- [ ] Unit tests for all services (>80% coverage)
- [ ] Integration tests for repositories
- [ ] Controller tests with MockMvc
- [ ] Test data builders

## Phase 6: Deployment
- [ ] Dockerfile
- [ ] docker-compose.yml
- [ ] Health check endpoints
- [ ] Prometheus metrics

---

# NOTES FOR CLAUDE

1. **Implement modules one by one** - Complete Student module fully before moving to Course
2. **Follow the coding conventions** in `code-style.md`
3. **Use enums from** `enums-constants.md`
4. **Write tests** for each module following `testing-guide.md`
5. **Error handling** follows `error-logging.md`
6. **API design** follows `api-design.md`
7. **Git commits** follow `git-workflow.md` conventions

## Commands to Run

```bash
# Build
./mvnw clean package

# Run tests
./mvnw test

# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Generate coverage report
./mvnw jacoco:report
```
