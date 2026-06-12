# PLAN: KiteClass Core Service Implementation

## Thông tin

| Thuộc tính | Giá trị |
|------------|---------|
| **Service** | kiteclass-core-service |
| **Version** | V4.1 (Bundled Model) ⭐ NEW |
| **Tech Stack** | Spring Boot 3.5.10, Java 17, PostgreSQL 15 |
| **RAM** | ~900MB (650MB base + 250MB LMS/Marketing) |
| **Mục đích** | Core business logic: Classes, Students, Attendance, Billing, LMS, Marketing |
| **Tham chiếu** | architecture-overview, database-design, api-design |
| **Last Updated** | 2026-02-26 |

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

## File Storage Integration (Cross-Cutting)

**Purpose**: Integrate Storage Service với các modules hiện có (Student, Teacher, Course) để hỗ trợ avatar upload, document attachments.

### Current State

- **Student** entity có `avatar_url: VARCHAR(500)` - stores URL string
- **Teacher** entity có `avatar_url: VARCHAR(500)` - stores URL string
- **Course** entity CÓ THỂ cần `syllabus_file_id: UUID` - FK to uploaded_files

### After PR 2.10.1 (Storage Service)

**Option 1: File ID Reference (Recommended)**
- Change `avatar_url` type to `UUID` (FK to uploaded_files table)
- Better audit trail (who uploaded, when, file size, access control)
- Can enforce access control (PRIVATE, COURSE, PUBLIC)

**Option 2: Direct S3 URL (Simpler)**
- Keep `avatar_url: VARCHAR(500)` as S3 URL: `https://cdn.kitehub.me/tenant-123/avatars/abc.png`
- No FK reference to uploaded_files (looser coupling)
- Simpler to implement, no cross-table joins

**Recommendation**: **Option 1** (File ID Reference) cho:
- Better audit trail
- Access control enforcement
- Storage quota tracking (know which user consumes how much)

### Avatar Upload Flow (Frontend → Storage Service)

1. **Frontend initiates upload**:
   ```
   POST /api/v1/files/upload/initiate
   Body: { fileName, fileSize, fileType: "AVATAR", mimeType: "image/png" }
   Response: { uploadUrl, fileId, expiresIn: 600 }
   ```

2. **Frontend uploads to S3 directly**:
   ```
   PUT {uploadUrl}
   Body: <file binary>
   Headers: { Content-Type: image/png }
   ```

3. **Frontend completes upload**:
   ```
   POST /api/v1/files/{fileId}/complete
   Response: { fileId, status: "READY", downloadUrl }
   ```

4. **Frontend updates Student/Teacher**:
   ```
   PUT /api/v1/students/{id}
   Body: { avatarFileId: "{fileId}" }  // NEW field
   ```

5. **Backend updates entity**:
   ```java
   student.setAvatarFileId(avatarFileId);
   studentRepository.save(student);
   ```

6. **Frontend displays avatar**:
   ```
   GET /api/v1/files/{avatarFileId}/download
   Response: { downloadUrl: "https://s3.../presigned-url", expiresIn: 86400 }
   ```

### Migration Strategy

1. Add new column `avatar_file_id UUID` to students/teachers tables (nullable)
2. Keep old `avatar_url VARCHAR(500)` for backward compatibility (deprecated)
3. Migrate existing URLs to uploaded_files records (background job)
4. Update APIs to accept both `avatarUrl` (deprecated) and `avatarFileId` (new)
5. Phase out `avatar_url` sau 2-3 versions

### Document Attachments (Course Syllabus, Assignment Submissions)

- Similar flow như avatar upload
- **Course**: `syllabus_file_id UUID` (FK to uploaded_files)
- **Assignment Submission**: `submission_file_id UUID` (FK to uploaded_files)
- Access control: `access_level = 'COURSE'` (only teacher + enrolled students)

### Service Layer Changes

```java
// StudentService.java
@Service
public class StudentServiceImpl implements StudentService {
    private final FileServiceClient fileServiceClient; // Feign client to Storage Service

    public StudentResponse updateAvatar(Long studentId, UUID avatarFileId) {
        // 1. Verify file exists and is READY
        FileMetadataResponse file = fileServiceClient.getFileMetadata(avatarFileId);
        if (!file.getStatus().equals("READY")) {
            throw new InvalidFileStateException("File not ready");
        }

        // 2. Update student
        Student student = studentRepository.findById(studentId);
        student.setAvatarFileId(avatarFileId);
        studentRepository.save(student);

        return mapToResponse(student);
    }
}
```

### DTOs Update

```java
// StudentResponse.java
public class StudentResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;

    @Deprecated
    private String avatarUrl; // Keep for backward compatibility

    private UUID avatarFileId; // NEW - FK to uploaded_files
    private String avatarDownloadUrl; // NEW - Presigned URL (fetched from Storage Service)
}
```

**Related Documentation**: See [Storage Service Design](./storage-service-design.md) for complete API flows, quota enforcement, and testing strategies.

**Related PRs**:
- PR 2.10.1: Storage Service (foundation)
- PR 2.3.2: Student Module Update (avatar_file_id migration)
- PR 2.3.3: Teacher Module Update (avatar_file_id migration)

---

# PHASE 4: V4.1 GUEST-FACING MODULES (Bundled Model) ⭐ NEW

## 4.1. Module: LMS (Learning Management System)

### Overview
Enable guest learning features and student progress tracking. This module provides structured learning paths with course modules, lessons, and progress tracking.

### Package Structure

```
module/lms/
├── controller/
│   ├── CourseModuleController.java
│   ├── LessonController.java
│   └── LearningProgressController.java
├── service/
│   ├── CourseModuleService.java
│   ├── LessonService.java
│   └── LearningProgressService.java
├── repository/
│   ├── CourseModuleRepository.java
│   ├── LessonRepository.java
│   ├── LearningResourceRepository.java
│   └── LessonProgressRepository.java
├── entity/
│   ├── CourseModule.java
│   ├── Lesson.java
│   ├── LearningResource.java
│   └── LessonProgress.java
├── dto/
│   ├── CourseModuleResponse.java
│   ├── LessonResponse.java
│   ├── CreateModuleRequest.java
│   ├── CreateLessonRequest.java
│   └── LearningProgressResponse.java
└── mapper/
    ├── CourseModuleMapper.java
    └── LessonMapper.java
```

### Entities

#### CourseModule.java
```java
package com.kiteclass.core.module.lms.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.course.entity.Course;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "course_modules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"course_id", "order_number", "instance_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseModule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_number", nullable = false)
    @Builder.Default
    private Integer orderNumber = 0;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();
}
```

#### Lesson.java
```java
package com.kiteclass.core.module.lms.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lessons", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"module_id", "order_number", "instance_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "is_trial", nullable = false)
    @Builder.Default
    private boolean isTrial = false;  // ⭐ KEY: Guest access control

    @Column(name = "order_number", nullable = false)
    @Builder.Default
    private Integer orderNumber = 0;

    @Column(name = "estimated_duration") // minutes
    private Integer estimatedDuration;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LearningResource> resources = new ArrayList<>();
}
```

#### LearningResource.java
```java
package com.kiteclass.core.module.lms.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "learning_resources")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningResource extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // PDF, VIDEO, SLIDES, QUIZ, OTHER

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "file_size") // bytes
    private Long fileSize;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;
}
```

#### LessonProgress.java
```java
package com.kiteclass.core.module.lms.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "lesson_id", "instance_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgress extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId; // From Gateway User.id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "progress_percent")
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;
}
```

### Business Rules

- **BR-LMS-001**: Guest users can only access lessons where `isTrial = true`
- **BR-LMS-002**: Student must have active enrollment in course to access paid lessons (`isTrial = false`)
- **BR-LMS-003**: Progress auto-saves when user marks lesson as completed
- **BR-LMS-004**: Module `order_number` must be unique per course
- **BR-LMS-005**: Lesson `order_number` must be unique per module

### REST Endpoints

#### Public/Guest Endpoints
```java
// CourseModuleController
GET  /api/v1/courses/{courseId}/modules
     - List all modules for a course (with lessons metadata)
     - Authorization: Public (guest access)
     - Returns: List<CourseModuleResponse> with lesson counts

GET  /api/v1/modules/{moduleId}/lessons
     - List lessons in a module
     - Authorization: Public (guest sees only trial lessons)
     - Returns: List<LessonResponse> (filtered by isTrial if guest)
```

#### Student Endpoints
```java
// LessonController
GET  /api/v1/lessons/{lessonId}
     - View lesson detail
     - Authorization: Student with enrollment OR isTrial = true
     - Returns: LessonResponse with content and resources

POST /api/v1/lessons/{lessonId}/complete
     - Mark lesson as completed
     - Authorization: Student only
     - Body: { progressPercent: 100 }
     - Returns: LearningProgressResponse

// LearningProgressController
GET  /api/v1/courses/{courseId}/progress
     - Get student learning progress for a course
     - Authorization: Student only
     - Returns: CourseProgressResponse (modules, lessons, completion %)
```

#### Teacher/Admin Endpoints
```java
// CourseModuleController
POST /api/v1/courses/{courseId}/modules
     - Create new module
     - Authorization: Teacher/Admin
     - Body: CreateModuleRequest
     - Returns: CourseModuleResponse

PUT  /api/v1/modules/{moduleId}
     - Update module
     - Authorization: Teacher/Admin
     - Body: UpdateModuleRequest
     - Returns: CourseModuleResponse

DELETE /api/v1/modules/{moduleId}
     - Delete module (soft delete)
     - Authorization: Teacher/Admin
     - Returns: 204 No Content

// LessonController
POST /api/v1/modules/{moduleId}/lessons
     - Add lesson to module
     - Authorization: Teacher/Admin
     - Body: CreateLessonRequest
     - Returns: LessonResponse

PUT  /api/v1/lessons/{lessonId}
     - Update lesson (including toggling isTrial)
     - Authorization: Teacher/Admin
     - Body: UpdateLessonRequest
     - Returns: LessonResponse

DELETE /api/v1/lessons/{lessonId}
     - Delete lesson (soft delete)
     - Authorization: Teacher/Admin
     - Returns: 204 No Content
```

### DTOs

```java
// CourseModuleResponse
public record CourseModuleResponse(
    Long id,
    Long courseId,
    String title,
    String description,
    Integer orderNumber,
    Integer lessonCount,
    Integer trialLessonCount
) {}

// LessonResponse
public record LessonResponse(
    Long id,
    Long moduleId,
    String title,
    String content,
    String videoUrl,
    boolean isTrial,
    Integer orderNumber,
    Integer estimatedDuration,
    List<LearningResourceResponse> resources
) {}

// CreateModuleRequest
public record CreateModuleRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 5000) String description,
    @Min(0) Integer orderNumber
) {}

// CreateLessonRequest
public record CreateLessonRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 10000) String content,
    @URL String videoUrl,
    @NotNull boolean isTrial,
    @Min(0) Integer orderNumber,
    @Min(1) Integer estimatedDuration
) {}

// LearningProgressResponse
public record LearningProgressResponse(
    Long lessonId,
    String lessonTitle,
    boolean completed,
    Instant completedAt,
    Integer progressPercent
) {}
```

### Testing Requirements

#### Unit Tests (CourseModuleServiceTest, LessonServiceTest)
- Create module with valid data
- Update module order number
- Delete module cascades to lessons
- Create lesson with isTrial flag
- Update lesson content and toggle isTrial
- Validate unique constraints (module order, lesson order)
- **Minimum: 8 tests per service**

#### Integration Tests
- Guest can access trial lessons only
- Student with enrollment can access all lessons
- Student without enrollment blocked from paid lessons
- Progress tracking persists correctly
- Module/lesson ordering maintained

---

## 4.2. Module: Marketing

### Overview
Landing page management, lead capture, and contact forms for guest-to-student conversion funnel.

### Package Structure

```
module/marketing/
├── controller/
│   ├── LandingPageController.java
│   ├── LeadController.java
│   └── ContactController.java
├── service/
│   ├── LandingPageService.java
│   ├── LeadService.java
│   └── ContactService.java
├── repository/
│   ├── LandingPageRepository.java
│   ├── LeadRepository.java
│   └── ContactMessageRepository.java
├── entity/
│   ├── LandingPage.java
│   ├── Lead.java
│   └── ContactMessage.java
└── dto/
    ├── LandingPageResponse.java
    ├── LeadResponse.java
    ├── ContactFormRequest.java
    └── UpdateLandingPageRequest.java
```

### Entities

#### LandingPage.java
```java
package com.kiteclass.core.module.marketing.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "landing_pages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPage extends BaseEntity {

    @Column(name = "instance_id", nullable = false, unique = true)
    private UUID instanceId; // One landing page per tenant

    @Column(name = "hero_title", length = 200)
    private String heroTitle;

    @Column(name = "hero_subtitle", length = 500)
    private String heroSubtitle;

    @Column(name = "teacher_bio", columnDefinition = "TEXT")
    private String teacherBio;

    @Column(name = "hero_image_url", length = 500)
    private String heroImageUrl;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "tagline", length = 200)
    private String tagline;

    @Column(name = "primary_color", length = 7) // Hex color #RRGGBB
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;
}
```

#### Lead.java
```java
package com.kiteclass.core.module.marketing.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.course.entity.Course;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_leads_instance_id", columnList = "instance_id"),
    @Index(name = "idx_leads_email", columnList = "email"),
    @Index(name = "idx_leads_status", columnList = "status"),
    @Index(name = "idx_leads_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private LeadSource source; // LANDING_PAGE, CONTACT_FORM, TRIAL, REFERRAL

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW; // NEW, CONTACTED, CONVERTED, LOST

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_interest_id")
    private Course courseInterest;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "last_contacted_at")
    private Instant lastContactedAt;
}
```

#### ContactMessage.java
```java
package com.kiteclass.core.module.marketing.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_messages", indexes = {
    @Index(name = "idx_contact_messages_instance_id", columnList = "instance_id"),
    @Index(name = "idx_contact_messages_is_read", columnList = "is_read"),
    @Index(name = "idx_contact_messages_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessage extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;
}
```

#### Enums

```java
// LeadSource.java
public enum LeadSource {
    LANDING_PAGE("Landing Page"),
    CONTACT_FORM("Contact Form"),
    TRIAL("Trial Enrollment"),
    REFERRAL("Referral");

    private final String displayName;
}

// LeadStatus.java
public enum LeadStatus {
    NEW("Mới"),
    CONTACTED("Đã liên hệ"),
    CONVERTED("Đã chuyển đổi"),
    LOST("Mất khách");

    private final String displayName;
}
```

### Business Rules

- **BR-MKT-001**: Each tenant has exactly one landing page (1:1 relationship with instance_id)
- **BR-MKT-002**: Lead automatically created when guest submits contact form or trial request
- **BR-MKT-003**: Lead status transitions: NEW → CONTACTED → CONVERTED/LOST
- **BR-MKT-004**: When lead converts to student, status = CONVERTED (link to Student entity)
- **BR-MKT-005**: Contact messages sorted by created_at DESC (newest first)

### REST Endpoints

#### Public Endpoints
```java
// LandingPageController
GET  /api/v1/tenants/{instanceId}/landing
     - Get landing page content for a tenant
     - Authorization: Public (guest access)
     - Returns: LandingPageResponse

// LeadController
POST /api/v1/leads
     - Register as lead (trial request)
     - Authorization: Public (guest access)
     - Body: CreateLeadRequest
     - Returns: LeadResponse

// ContactController
POST /api/v1/contact
     - Send contact message
     - Authorization: Public (guest access)
     - Body: ContactFormRequest
     - Returns: 201 Created
```

#### Admin/Teacher Endpoints
```java
// LandingPageController
PUT  /api/v1/tenants/{instanceId}/landing
     - Update landing page content
     - Authorization: Admin/Teacher
     - Body: UpdateLandingPageRequest
     - Returns: LandingPageResponse

// LeadController
GET  /api/v1/leads
     - List leads with filters
     - Authorization: Admin/Teacher
     - Query: ?status=NEW&source=LANDING_PAGE&page=0&size=20
     - Returns: PageResponse<LeadResponse>

PUT  /api/v1/leads/{leadId}
     - Update lead status/notes
     - Authorization: Admin/Teacher
     - Body: UpdateLeadRequest
     - Returns: LeadResponse

// ContactController
GET  /api/v1/contact/messages
     - List contact messages
     - Authorization: Admin/Teacher
     - Query: ?isRead=false&page=0&size=20
     - Returns: PageResponse<ContactMessageResponse>

PUT  /api/v1/contact/messages/{messageId}/read
     - Mark message as read
     - Authorization: Admin/Teacher
     - Returns: 204 No Content
```

### DTOs

```java
// LandingPageResponse
public record LandingPageResponse(
    Long id,
    UUID instanceId,
    String heroTitle,
    String heroSubtitle,
    String teacherBio,
    String heroImageUrl,
    String logoUrl,
    String tagline,
    String primaryColor,
    String secondaryColor
) {}

// LeadResponse
public record LeadResponse(
    Long id,
    String name,
    String email,
    String phone,
    LeadSource source,
    LeadStatus status,
    Long courseInterestId,
    String message,
    Instant createdAt,
    Instant lastContactedAt
) {}

// CreateLeadRequest
public record CreateLeadRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 100) String name,
    @Pattern(regexp = "^0\\d{9}$") String phone,
    @NotNull LeadSource source,
    Long courseInterestId,
    @Size(max = 5000) String message
) {}

// ContactFormRequest
public record ContactFormRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email String email,
    @Pattern(regexp = "^0\\d{9}$") String phone,
    @NotBlank @Size(max = 5000) String message
) {}

// UpdateLandingPageRequest
public record UpdateLandingPageRequest(
    @Size(max = 200) String heroTitle,
    @Size(max = 500) String heroSubtitle,
    @Size(max = 10000) String teacherBio,
    @URL String heroImageUrl,
    @URL String logoUrl,
    @Size(max = 200) String tagline,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String primaryColor,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String secondaryColor
) {}
```

### Testing Requirements

#### Unit Tests (LandingPageServiceTest, LeadServiceTest, ContactServiceTest)
- Create/update landing page per tenant
- Create lead from contact form
- Update lead status (NEW → CONTACTED → CONVERTED)
- Create contact message from guest
- Mark contact message as read
- Filter leads by status/source
- **Minimum: 8 tests per service**

#### Integration Tests
- Guest can view landing page without authentication
- Guest can submit contact form (creates lead + contact message)
- Admin can update landing page branding
- Admin can list unread contact messages
- Lead status workflow validation

---

# PHASE 5: DATABASE MIGRATIONS

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
├── V8__seed_initial_data.sql
├── V9__create_lms_tables.sql          # ⭐ V4.1 NEW
├── V10__create_marketing_tables.sql   # ⭐ V4.1 NEW
└── V11__seed_demo_lms_content.sql     # ⭐ V4.1 NEW (Optional)
```

### V9__create_lms_tables.sql (NEW V4.1)

```sql
-- V9: Create LMS Module Tables
-- Purpose: Course structure (modules → lessons → resources) and progress tracking

CREATE TABLE course_modules (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    order_number INTEGER NOT NULL DEFAULT 0,

    -- Multi-tenant & Audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uk_course_modules_course_order
        UNIQUE (course_id, order_number, instance_id, deleted)
);

CREATE INDEX idx_course_modules_course_id ON course_modules(course_id);
CREATE INDEX idx_course_modules_instance_id ON course_modules(instance_id);

COMMENT ON TABLE course_modules IS 'Learning modules within a course';
COMMENT ON COLUMN course_modules.order_number IS 'Display order (unique per course)';

-- Lessons Table
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    video_url VARCHAR(500),
    is_trial BOOLEAN NOT NULL DEFAULT FALSE,
    order_number INTEGER NOT NULL DEFAULT 0,
    estimated_duration INTEGER, -- minutes

    -- Multi-tenant & Audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uk_lessons_module_order
        UNIQUE (module_id, order_number, instance_id, deleted)
);

CREATE INDEX idx_lessons_module_id ON lessons(module_id);
CREATE INDEX idx_lessons_is_trial ON lessons(is_trial) WHERE deleted = FALSE;
CREATE INDEX idx_lessons_instance_id ON lessons(instance_id);

COMMENT ON COLUMN lessons.is_trial IS 'Guest access flag: true = public, false = requires enrollment';

-- Learning Resources Table
CREATE TABLE learning_resources (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    url VARCHAR(500),
    file_size BIGINT, -- bytes

    -- Multi-tenant & Audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_learning_resources_type
        CHECK (type IN ('PDF', 'VIDEO', 'SLIDES', 'QUIZ', 'OTHER'))
);

CREATE INDEX idx_learning_resources_lesson_id ON learning_resources(lesson_id);

-- Lesson Progress Table
CREATE TABLE lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP WITH TIME ZONE,
    progress_percent INTEGER DEFAULT 0,

    -- Multi-tenant & Audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_lesson_progress_user_lesson
        UNIQUE (user_id, lesson_id, instance_id),
    CONSTRAINT chk_progress_percent_range
        CHECK (progress_percent >= 0 AND progress_percent <= 100)
);

CREATE INDEX idx_lesson_progress_user_id ON lesson_progress(user_id);
CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress(lesson_id);
CREATE INDEX idx_lesson_progress_completed ON lesson_progress(completed);

COMMENT ON TABLE lesson_progress IS 'Student learning progress per lesson';
```

### V10__create_marketing_tables.sql (NEW V4.1)

```sql
-- V10: Create Marketing Module Tables
-- Purpose: Landing page content, lead capture, and contact management

CREATE TABLE landing_pages (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL UNIQUE,
    hero_title VARCHAR(200),
    hero_subtitle VARCHAR(500),
    teacher_bio TEXT,
    hero_image_url VARCHAR(500),
    logo_url VARCHAR(500),
    tagline VARCHAR(200),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX idx_landing_pages_instance_id ON landing_pages(instance_id);

COMMENT ON TABLE landing_pages IS 'Tenant-specific landing page content (1:1 with tenant)';
COMMENT ON COLUMN landing_pages.primary_color IS 'Hex color for branding (#RRGGBB)';

-- Leads Table
CREATE TABLE leads (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(20),
    source VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    course_interest_id BIGINT REFERENCES courses(id),
    message TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_contacted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_leads_source
        CHECK (source IN ('LANDING_PAGE', 'CONTACT_FORM', 'TRIAL', 'REFERRAL')),
    CONSTRAINT chk_leads_status
        CHECK (status IN ('NEW', 'CONTACTED', 'CONVERTED', 'LOST'))
);

CREATE INDEX idx_leads_instance_id ON leads(instance_id);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_created_at ON leads(created_at);

COMMENT ON TABLE leads IS 'Guest leads for conversion tracking';

-- Contact Messages Table
CREATE TABLE contact_messages (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_contact_messages_instance_id ON contact_messages(instance_id);
CREATE INDEX idx_contact_messages_is_read ON contact_messages(is_read);
CREATE INDEX idx_contact_messages_created_at ON contact_messages(created_at);

COMMENT ON TABLE contact_messages IS 'Guest contact form submissions';
```

### V11__seed_demo_lms_content.sql (NEW V4.1 - Optional)

```sql
-- V11: Seed Demo LMS Content
-- Purpose: Insert demo course structure for testing
-- WARNING: FOR DEMO/TESTING ONLY - DELETE IN PRODUCTION

-- Note: Assumes demo course exists from V8__seed_initial_data.sql
-- Insert demo modules
INSERT INTO course_modules (course_id, title, description, order_number, instance_id, created_at, updated_at, deleted)
SELECT
    c.id,
    'Module 1: Introduction to Java',
    'Learn the basics of Java programming language',
    1,
    c.instance_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM courses c WHERE c.code = 'DEMO-JAVA-2026';

INSERT INTO course_modules (course_id, title, description, order_number, instance_id, created_at, updated_at, deleted)
SELECT
    c.id,
    'Module 2: Object-Oriented Programming',
    'Master OOP concepts in Java',
    2,
    c.instance_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM courses c WHERE c.code = 'DEMO-JAVA-2026';

-- Insert demo lessons (1 trial, 2 paid per module)
INSERT INTO lessons (module_id, title, content, video_url, is_trial, order_number, estimated_duration, instance_id, created_at, updated_at, deleted)
SELECT
    m.id,
    'Lesson 1.1: What is Java? (FREE)',
    'Introduction to Java and its ecosystem. This is a free trial lesson.',
    'https://example.com/video/java-intro.mp4',
    TRUE, -- Trial lesson
    1,
    30,
    m.instance_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM course_modules m WHERE m.title = 'Module 1: Introduction to Java';

INSERT INTO lessons (module_id, title, content, video_url, is_trial, order_number, estimated_duration, instance_id, created_at, updated_at, deleted)
SELECT
    m.id,
    'Lesson 1.2: Installing Java JDK',
    'Step-by-step guide to install Java Development Kit.',
    'https://example.com/video/java-install.mp4',
    FALSE, -- Paid lesson
    2,
    45,
    m.instance_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
FROM course_modules m WHERE m.title = 'Module 1: Introduction to Java';

-- Add more lessons as needed...
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
│   ├── student/
│   │   ├── controller/StudentControllerTest.java
│   │   ├── service/StudentServiceTest.java
│   │   └── repository/StudentRepositoryTest.java
│   ├── lms/                                    # ⭐ V4.1 NEW
│   │   ├── controller/
│   │   │   ├── CourseModuleControllerTest.java
│   │   │   └── LessonControllerTest.java
│   │   ├── service/
│   │   │   ├── CourseModuleServiceTest.java
│   │   │   └── LessonServiceTest.java
│   │   └── repository/
│   │       ├── CourseModuleRepositoryTest.java
│   │       └── LessonRepositoryTest.java
│   └── marketing/                             # ⭐ V4.1 NEW
│       ├── controller/
│       │   ├── LandingPageControllerTest.java
│       │   └── LeadControllerTest.java
│       ├── service/
│       │   ├── LandingPageServiceTest.java
│       │   └── LeadServiceTest.java
│       └── repository/
│           ├── LandingPageRepositoryTest.java
│           └── LeadRepositoryTest.java
├── integration/
│   ├── StudentIntegrationTest.java
│   ├── LMSIntegrationTest.java               # ⭐ V4.1 NEW
│   └── MarketingIntegrationTest.java         # ⭐ V4.1 NEW
└── testutil/
    ├── IntegrationTestBase.java
    ├── TestDataBuilder.java
    └── LMSTestDataBuilder.java               # ⭐ V4.1 NEW
```

## V4.1 Test Coverage Requirements

### LMS Module Tests
- **CourseModuleServiceTest**: Create/update/delete modules, order validation
- **LessonServiceTest**: Create/update lessons, toggle isTrial flag, access control
- **LearningProgressServiceTest**: Mark complete, track progress percentage
- **LMSIntegrationTest**: Guest access to trial lessons, student enrollment check

### Marketing Module Tests
- **LandingPageServiceTest**: Create/update tenant landing page (1:1 constraint)
- **LeadServiceTest**: Create lead, update status (NEW → CONTACTED → CONVERTED)
- **ContactServiceTest**: Submit contact form, mark as read
- **MarketingIntegrationTest**: Guest form submission, lead conversion workflow

### Minimum Coverage
- **Unit tests**: Minimum 8 tests per service
- **Line coverage**: >80%
- **Branch coverage**: >75%

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

## Phase 3: Business Modules (Core)
- [ ] Student module (CRUD + search)
- [ ] Course module
- [ ] Class module with schedules
- [ ] Enrollment module
- [ ] Attendance module
- [ ] Invoice module
- [ ] Payment module
- [ ] Parent module

## Phase 4: V4.1 Guest-Facing Modules ⭐ NEW
- [ ] LMS: CourseModule entity + CRUD
- [ ] LMS: Lesson entity + CRUD + isTrial access control
- [ ] LMS: LearningResource entity
- [ ] LMS: LessonProgress tracking
- [ ] Marketing: LandingPage entity + CRUD (1:1 per tenant)
- [ ] Marketing: Lead entity + status workflow
- [ ] Marketing: ContactMessage entity + read tracking
- [ ] Marketing: Lead conversion to Student integration

## Phase 4.5: V4.1 Trial Learning System (Phase 2) ⭐ NEW
- [ ] Lead entity extension: Add `user_id` column (FK to Gateway users)
- [ ] TrialQuota entity: Daily lesson access limits (3 lessons/day)
- [ ] LeadSource enum: LANDING_PAGE, CONTACT_FORM, TRIAL_SIGNUP, REFERRAL
- [ ] LeadStatus enum: NEW, CONTACTED, CONVERTED, LOST
- [ ] LeadService: registerForTrial() - Create lead + call Gateway magic link API
- [ ] LeadService: getLeadByUserId() - Fetch lead profile for trial users
- [ ] LeadService: convertToStudent() - Payment verification + role update + enrollment
- [ ] TrialQuotaService: checkAndIncrementQuota() - Enforce 3 lessons/day limit
- [ ] TrialQuotaService: getQuotaStatus() - Return remaining quota for UI display
- [ ] LessonService enhancement: getLessonWithAccessControl() - Check TRIAL_USER role + quota
- [ ] GatewayClient: updateUserRole() - Call Gateway API to update TRIAL_USER → STUDENT
- [ ] PaymentService (mock): verifyPayment() - Mock payment verification for Phase 1
- [ ] LeadController: POST /register-trial, GET /me, GET /quota, POST /{id}/convert
- [ ] Lesson access control: Check `is_trial_accessible` flag before returning content
- [ ] Multi-tenant isolation: All queries filtered by `instance_id`
- [ ] Error codes: LEAD_EMAIL_EXISTS, TRIAL_QUOTA_EXCEEDED, TRIAL_USER_PAID_LESSON_ACCESS_DENIED
- [ ] Validation: Email uniqueness per tenant, quota limits (0-3), conversion payment verification

**Implementation Order**:
1. Entities first: Lead, TrialQuota (with Hibernate filters)
2. Repositories: LeadRepository, TrialQuotaRepository
3. Services: LeadService, TrialQuotaService (with @Transactional)
4. Enhancement: LessonService.getLessonWithAccessControl()
5. Controllers: LeadController endpoints
6. Integration: GatewayClient for cross-service calls
7. Testing: Unit tests + Integration tests for quota enforcement

**Key Patterns**:
- Use TenantContext.getInstanceId() for multi-tenant queries
- Quota reset logic: New record per user per day (no cleanup needed)
- Progress preservation: lesson_progress uses user_id (not student_id) → automatic preservation
- Magic link flow: Core calls Gateway API, Gateway sends email, user verifies, Core updates Lead.user_id

**References**:
- PR 2.13: Trial Registration & Quota Management
- PR 2.14: Lead to Student Conversion
- Migration V12: CREATE TABLE trial_quotas, ALTER TABLE leads ADD user_id

## Phase 5: Database
- [ ] Flyway migrations V1-V8 (Core modules)
- [ ] V9: LMS tables (course_modules, lessons, learning_resources, lesson_progress) ⭐ NEW
- [ ] V10: Marketing tables (landing_pages, leads, contact_messages) ⭐ NEW
- [ ] V11: Seed demo LMS content (optional) ⭐ NEW
- [ ] V12: Trial Learning tables (trial_quotas, leads.user_id extension, courses.is_trial, lessons.is_trial_accessible) ⭐ NEW Phase 2
- [ ] Indexes optimization
- [ ] Seed data for testing

## Phase 6: Testing
- [ ] Unit tests for all services (>80% coverage)
- [ ] LMS module tests: CourseModuleService, LessonService, LearningProgressService ⭐ NEW
- [ ] Marketing module tests: LandingPageService, LeadService, ContactService ⭐ NEW
- [ ] Trial Learning tests: LeadService, TrialQuotaService, LessonService.getLessonWithAccessControl() ⭐ NEW Phase 2
- [ ] Integration tests for repositories
- [ ] LMS integration tests (guest access control) ⭐ NEW
- [ ] Marketing integration tests (lead workflow) ⭐ NEW
- [ ] Trial Learning integration tests: Quota enforcement, multi-tenant isolation, conversion flow ⭐ NEW Phase 2
- [ ] Controller tests with MockMvc
- [ ] Test data builders

## Phase 7: Deployment
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
