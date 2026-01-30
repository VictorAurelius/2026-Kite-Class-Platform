# BACKEND IMPLEMENTATION PLAN V2 - KiteClass Core Service

**Version:** 2.0
**Created:** 2026-01-30
**Updated:** 2026-01-30
**Status:** Ready for Implementation

**Tham chiếu:**
- `system-architecture-v3-final.md` (PHẦN 6B-6F)
- `architecture-clarification-qa.md` (PART 1-5)
- `database-migration-plan.md` (Database changes)

---

## MỤC LỤC

1. [Tổng quan](#tổng-quan)
2. [Phase 1: Core Infrastructure](#phase-1-core-infrastructure)
3. [Phase 2: Authentication & Authorization V3](#phase-2-authentication--authorization-v3)
4. [Phase 3: Feature Detection System](#phase-3-feature-detection-system)
5. [Phase 4: AI Branding Service Integration](#phase-4-ai-branding-service-integration)
6. [Phase 5: Preview Website Public APIs](#phase-5-preview-website-public-apis)
7. [Phase 6: Guest & Trial System](#phase-6-guest--trial-system)
8. [Phase 7: VietQR Payment System](#phase-7-vietqr-payment-system)
9. [Phase 8: Business Logic Modules](#phase-8-business-logic-modules)
10. [Testing Strategy](#testing-strategy)
11. [Deployment Plan](#deployment-plan)

---

# TỔNG QUAN

## Tech Stack

```yaml
Backend:
  Framework: Spring Boot 3.2+
  Language: Java 17
  Database: PostgreSQL 15
  Cache: Redis 7
  Message Queue: RabbitMQ
  Storage: AWS S3 (hoặc MinIO)
  Email: AWS SES

Architecture:
  Pattern: Microservices
  API: REST (JSON)
  Security: JWT + OAuth2
  Documentation: OpenAPI 3.0 (SpringDoc)
```

## Service Structure

```
kiteclass/
├── kiteclass-core/          # Instance-level business logic
│   ├── Classes, Students, Attendance
│   ├── Billing, Invoices, Payments
│   ├── Feature Detection
│   └── Public APIs (Preview Website)
│
├── kitehub/                 # Platform-level services
│   ├── Subscription Management
│   ├── AI Branding Service
│   ├── VietQR Payment (KiteHub level)
│   └── Email & Notification
│
└── kiteclass-gateway/       # API Gateway
    ├── Routing
    ├── Rate Limiting
    └── Authentication
```

## Implementation Phases

| Phase | Scope | Duration | Dependencies |
|-------|-------|----------|--------------|
| Phase 1 | Core Infrastructure | 1 tuần | None |
| Phase 2 | Authentication V3 | 1 tuần | Phase 1 |
| Phase 3 | Feature Detection | 3 ngày | Phase 1, 2 |
| Phase 4 | AI Branding Integration | 1 tuần | Phase 1, 2, 3 |
| Phase 5 | Preview Website APIs | 3 ngày | Phase 3, 4 |
| Phase 6 | Guest & Trial System | 1 tuần | Phase 2, 3, 5 |
| Phase 7 | VietQR Payment | 1 tuần | Phase 6 |
| Phase 8 | Business Modules | 4 tuần | Phase 1-7 |

**Total:** ~10 tuần (2.5 tháng)

---

# PHASE 1: CORE INFRASTRUCTURE

**Duration:** 1 tuần
**Dependencies:** None

## 1.1. Project Setup

### Maven Project Structure

```xml
<!-- pom.xml -->
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
    </dependencies>
</project>
```

### Package Structure

```
src/main/java/com/kiteclass/core/
├── KiteClassCoreApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── JpaConfig.java
│   ├── RedisConfig.java
│   ├── RabbitMQConfig.java
│   └── OpenApiConfig.java
├── domain/
│   ├── entity/          # JPA entities
│   ├── enums/           # Enums & constants
│   └── dto/             # DTOs
├── repository/          # Spring Data JPA repositories
├── service/
│   ├── impl/            # Service implementations
│   └── mapper/          # MapStruct mappers
├── controller/          # REST controllers
├── security/            # Security components
├── exception/           # Custom exceptions
├── util/                # Utility classes
└── event/               # Domain events
```

## 1.2. Base Entity & Auditing

```java
// src/main/java/com/kiteclass/core/domain/entity/BaseEntity.java
package com.kiteclass.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base entity với auditing fields
 * Tất cả entities kế thừa từ class này
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Soft delete
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Check if entity is deleted
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.deleted);
    }
}
```

## 1.3. Exception Handling

```java
// src/main/java/com/kiteclass/core/exception/GlobalExceptionHandler.java
package com.kiteclass.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(FeatureNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleFeatureNotAvailable(
        FeatureNotAvailableException ex
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Feature Not Available")
            .message(ex.getMessage())
            .metadata(Map.of(
                "feature", ex.getFeature(),
                "requiredTier", ex.getRequiredTier()
            ))
            .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// ErrorResponse.java
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, Object> metadata;
}

// Custom Exceptions
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

public class FeatureNotAvailableException extends RuntimeException {
    private final String feature;
    private final String requiredTier;

    public FeatureNotAvailableException(String feature, String requiredTier) {
        super(String.format(
            "Feature '%s' is not available. Required tier: %s",
            feature,
            requiredTier
        ));
        this.feature = feature;
        this.requiredTier = requiredTier;
    }

    // Getters
}
```

## 1.4. Configuration Files

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: kiteclass-core

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:kiteclass}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

server:
  port: 8080
  servlet:
    context-path: /api/v1

logging:
  level:
    com.kiteclass: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

## 1.5. Docker Compose for Development

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: kiteclass-postgres
    environment:
      POSTGRES_DB: kiteclass
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: kiteclass-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: kiteclass-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:
```

**Tasks Phase 1:**
- [x] Maven project setup
- [x] Package structure
- [x] BaseEntity với auditing
- [x] Global exception handling
- [x] Configuration files
- [x] Docker Compose for dev environment

**Deliverables:**
- Spring Boot application runnable
- Database connection working
- Redis connection working
- RabbitMQ connection working
- Swagger UI accessible at http://localhost:8080/swagger-ui.html

---

# PHASE 2: AUTHENTICATION & AUTHORIZATION V3

**Duration:** 1 tuần
**Dependencies:** Phase 1
**Reference:** `system-architecture-v3-final.md` PHẦN 6

## 2.1. User Entity & Roles

```java
// src/main/java/com/kiteclass/core/domain/entity/User.java
package com.kiteclass.core.domain.entity;

import com.kiteclass.core.domain.enums.UserRole;
import com.kiteclass.core.domain.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_instance", columnList = "instance_id")
})
@Getter
@Setter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private AccountType accountType; // TRIAL, FREE, PAID

    // Trial fields
    @Column(name = "trial_started_at")
    private LocalDateTime trialStartedAt;

    @Column(name = "trial_expires_at")
    private LocalDateTime trialExpiresAt;

    @Column(name = "is_trial_expired")
    private Boolean isTrialExpired = false;

    // Status
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Check if user is CENTER_OWNER
     */
    public boolean isOwner() {
        return this.role == UserRole.CENTER_OWNER;
    }

    /**
     * Check if user is on trial
     */
    public boolean isOnTrial() {
        return AccountType.TRIAL.equals(this.accountType) &&
               this.trialExpiresAt != null &&
               this.trialExpiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Get days left in trial
     */
    public long getTrialDaysLeft() {
        if (!isOnTrial()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(),
            this.trialExpiresAt
        );
    }
}

// UserRole.java
public enum UserRole {
    CENTER_OWNER,
    CENTER_ADMIN,
    TEACHER,
    STUDENT,
    PARENT,
    GUEST
}

// AccountType.java
public enum AccountType {
    TRIAL,   // 14-day trial
    FREE,    // Free tier
    PAID     // Paid subscription
}
```

## 2.2. JWT Service

```java
// src/main/java/com/kiteclass/core/security/JwtService.java
package com.kiteclass.core.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours
    private Long jwtExpiration;

    /**
     * Generate JWT token
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("instanceId", user.getInstanceId().toString());
        claims.put("accountType", user.getAccountType().name());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Extract user ID from token
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userIdStr);
    }

    /**
     * Extract instance ID from token
     */
    public UUID extractInstanceId(String token) {
        String instanceIdStr = extractClaim(token, claims -> claims.get("instanceId", String.class));
        return UUID.fromString(instanceIdStr);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private <T> T extractClaim(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claimsResolver.resolve(claims);
    }

    @FunctionalInterface
    private interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }
}
```

## 2.3. Authentication Filter

```java
// src/main/java/com/kiteclass/core/security/JwtAuthenticationFilter.java
package com.kiteclass.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Skip if no Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT
        jwt = authHeader.substring(7);

        try {
            // Validate token
            if (jwtService.validateToken(jwt)) {
                userEmail = jwtService.extractClaim(jwt, Claims::getSubject);

                // Load user details
                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                    // Set authentication
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log error but don't block request
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
```

## 2.4. Security Configuration

```java
// src/main/java/com/kiteclass/core/config/SecurityConfig.java
package com.kiteclass.core.config;

import com.kiteclass.core.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/public/**",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/actuator/health"
                ).permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
        throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Tasks Phase 2:**
- [x] User entity with roles
- [x] JWT service
- [x] Authentication filter
- [x] Security configuration
- [x] UserDetailsService implementation
- [x] Login/Register endpoints

**Deliverables:**
- JWT authentication working
- Role-based access control
- Trial user support
- Login API functional
- Protected endpoints secure

---

# PHASE 3: FEATURE DETECTION SYSTEM

**Duration:** 3 ngày
**Dependencies:** Phase 1, 2
**Reference:** `system-architecture-v3-final.md` PHẦN 6B, `architecture-clarification-qa.md` PART 1

## 3.1. Pricing Tier Enum

```java
// src/main/java/com/kiteclass/core/domain/enums/PricingTier.java
package com.kiteclass.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Pricing tiers cho KiteClass subscription
 *
 * Reference: architecture-clarification-qa.md Q1.1.2
 */
@Getter
@RequiredArgsConstructor
public enum PricingTier {
    BASIC(
        "BASIC",
        "Gói cơ bản",
        499_000L,
        50,    // maxStudents
        10,    // maxCourses
        false, // hasEngagement
        false, // hasMedia
        false  // hasPremium
    ),
    STANDARD(
        "STANDARD",
        "Gói tiêu chuẩn",
        999_000L,
        200,   // maxStudents
        50,    // maxCourses
        true,  // hasEngagement
        true,  // hasMedia
        false  // hasPremium
    ),
    PREMIUM(
        "PREMIUM",
        "Gói cao cấp",
        1_499_000L,
        Integer.MAX_VALUE, // unlimited students
        Integer.MAX_VALUE, // unlimited courses
        true,  // hasEngagement
        true,  // hasMedia
        true   // hasPremium
    );

    private final String code;
    private final String displayName;
    private final Long priceVND;
    private final int maxStudents;
    private final int maxCourses;
    private final boolean hasEngagement;
    private final boolean hasMedia;
    private final boolean hasPremium;

    /**
     * Get tier from code
     */
    public static PricingTier fromCode(String code) {
        for (PricingTier tier : values()) {
            if (tier.code.equalsIgnoreCase(code)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unknown tier: " + code);
    }

    /**
     * Check if tier has feature
     */
    public boolean hasFeature(String feature) {
        return switch (feature.toUpperCase()) {
            case "ENGAGEMENT" -> this.hasEngagement;
            case "MEDIA" -> this.hasMedia;
            case "PREMIUM" -> this.hasPremium;
            default -> false;
        };
    }
}
```

## 3.2. Instance Config Entity

```java
// src/main/java/com/kiteclass/core/domain/entity/InstanceConfig.java
package com.kiteclass.core.domain.entity;

import com.kiteclass.core.domain.enums.PricingTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Instance configuration với feature detection
 *
 * Reference: system-architecture-v3-final.md PHẦN 6B
 */
@Entity
@Table(name = "instance_configs")
@Getter
@Setter
public class InstanceConfig extends BaseEntity {

    @Column(name = "instance_id", nullable = false, unique = true)
    private UUID instanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PricingTier tier;

    /**
     * Feature flags (dynamic JSON)
     * Example: {"engagement": true, "media": false, "premium": false}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private Map<String, Boolean> features;

    /**
     * Limitations (dynamic JSON)
     * Example: {"maxStudents": 50, "maxCourses": 10}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "limitations", columnDefinition = "jsonb")
    private Map<String, Integer> limitations;

    /**
     * Check if instance has feature
     */
    public boolean hasFeature(String featureName) {
        if (features == null) return false;
        return features.getOrDefault(featureName, false);
    }

    /**
     * Get limitation value
     */
    public Integer getLimitation(String limitationName) {
        if (limitations == null) return 0;
        return limitations.getOrDefault(limitationName, 0);
    }

    /**
     * Initialize from pricing tier
     */
    public void initializeFromTier(PricingTier tier) {
        this.tier = tier;
        this.features = Map.of(
            "engagement", tier.isHasEngagement(),
            "media", tier.isHasMedia(),
            "premium", tier.isHasPremium()
        );
        this.limitations = Map.of(
            "maxStudents", tier.getMaxStudents(),
            "maxCourses", tier.getMaxCourses()
        );
    }
}
```

## 3.3. Feature Detection Service

```java
// src/main/java/com/kiteclass/core/service/FeatureDetectionService.java
package com.kiteclass.core.service;

import com.kiteclass.core.domain.entity.InstanceConfig;
import com.kiteclass.core.exception.FeatureNotAvailableException;
import com.kiteclass.core.repository.InstanceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Feature Detection Service
 *
 * Provides feature flag resolution và tier-based access control
 *
 * Reference: system-architecture-v3-final.md PHẦN 6B.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureDetectionService {

    private final InstanceConfigRepository instanceConfigRepo;

    /**
     * Get instance configuration (cached 5 minutes)
     */
    @Cacheable(value = "instanceConfigs", key = "#instanceId")
    public InstanceConfig getInstanceConfig(UUID instanceId) {
        return instanceConfigRepo.findByInstanceId(instanceId)
            .orElseThrow(() -> new NotFoundException(
                "Instance config not found: " + instanceId
            ));
    }

    /**
     * Check if instance has feature
     */
    public boolean hasFeature(UUID instanceId, String featureName) {
        try {
            InstanceConfig config = getInstanceConfig(instanceId);
            return config.hasFeature(featureName);
        } catch (Exception e) {
            log.warn("Error checking feature {}: {}", featureName, e.getMessage());
            return false;
        }
    }

    /**
     * Require feature (throw exception if not available)
     */
    public void requireFeature(UUID instanceId, String featureName) {
        if (!hasFeature(instanceId, featureName)) {
            InstanceConfig config = getInstanceConfig(instanceId);
            throw new FeatureNotAvailableException(
                featureName,
                getRequiredTier(featureName)
            );
        }
    }

    /**
     * Check if instance is within limit
     */
    public boolean isWithinLimit(
        UUID instanceId,
        String limitationName,
        int currentValue
    ) {
        InstanceConfig config = getInstanceConfig(instanceId);
        Integer limit = config.getLimitation(limitationName);
        return currentValue < limit;
    }

    /**
     * Require within limit (throw exception if exceeded)
     */
    public void requireWithinLimit(
        UUID instanceId,
        String limitationName,
        int currentValue
    ) {
        if (!isWithinLimit(instanceId, limitationName, currentValue)) {
            InstanceConfig config = getInstanceConfig(instanceId);
            Integer limit = config.getLimitation(limitationName);
            throw new LimitExceededException(
                String.format(
                    "Exceeded %s limit: %d/%d. Please upgrade your plan.",
                    limitationName,
                    currentValue,
                    limit
                )
            );
        }
    }

    /**
     * Get required tier for feature
     */
    private String getRequiredTier(String featureName) {
        return switch (featureName.toUpperCase()) {
            case "ENGAGEMENT" -> "STANDARD";
            case "MEDIA" -> "STANDARD";
            case "PREMIUM" -> "PREMIUM";
            default -> "UNKNOWN";
        };
    }
}
```

## 3.4. Feature Detection Controller

```java
// src/main/java/com/kiteclass/core/controller/FeatureDetectionController.java
package com.kiteclass.core.controller;

import com.kiteclass.core.domain.dto.InstanceConfigDTO;
import com.kiteclass.core.service.FeatureDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/instance")
@RequiredArgsConstructor
@Tag(name = "Feature Detection", description = "Feature detection & instance config APIs")
public class FeatureDetectionController {

    private final FeatureDetectionService featureService;

    /**
     * Get instance configuration
     *
     * Frontend gọi API này để detect features available cho instance
     *
     * @param instanceId Instance ID
     * @return InstanceConfigDTO
     */
    @Operation(summary = "Get instance configuration")
    @GetMapping("/{instanceId}/config")
    public ResponseEntity<InstanceConfigDTO> getInstanceConfig(
        @PathVariable UUID instanceId
    ) {
        InstanceConfig config = featureService.getInstanceConfig(instanceId);
        InstanceConfigDTO dto = InstanceConfigDTO.from(config);
        return ResponseEntity.ok(dto);
    }

    /**
     * Check if feature is available
     */
    @Operation(summary = "Check feature availability")
    @GetMapping("/{instanceId}/features/{featureName}")
    public ResponseEntity<FeatureCheckResponse> checkFeature(
        @PathVariable UUID instanceId,
        @PathVariable String featureName
    ) {
        boolean available = featureService.hasFeature(instanceId, featureName);

        FeatureCheckResponse response = FeatureCheckResponse.builder()
            .feature(featureName)
            .available(available)
            .instanceId(instanceId)
            .build();

        return ResponseEntity.ok(response);
    }
}

// DTOs
@Data
@Builder
public class InstanceConfigDTO {
    private UUID instanceId;
    private String tier;
    private Map<String, Boolean> features;
    private Map<String, Integer> limitations;

    public static InstanceConfigDTO from(InstanceConfig config) {
        return InstanceConfigDTO.builder()
            .instanceId(config.getInstanceId())
            .tier(config.getTier().getCode())
            .features(config.getFeatures())
            .limitations(config.getLimitations())
            .build();
    }
}

@Data
@Builder
public class FeatureCheckResponse {
    private String feature;
    private Boolean available;
    private UUID instanceId;
}
```

## 3.5. Feature-Gated API Example

```java
// Example: Attendance API (requires ENGAGEMENT feature)
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final FeatureDetectionService featureService;
    private final AttendanceService attendanceService;

    /**
     * Create attendance record
     *
     * Requires ENGAGEMENT feature (STANDARD tier or higher)
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('CENTER_ADMIN')")
    public ResponseEntity<AttendanceDTO> createAttendance(
        @RequestHeader("X-Instance-Id") UUID instanceId,
        @RequestBody @Valid AttendanceCreateRequest request
    ) {
        // Check feature availability
        featureService.requireFeature(instanceId, "ENGAGEMENT");

        // Create attendance
        Attendance attendance = attendanceService.create(instanceId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AttendanceDTO.from(attendance));
    }
}
```

**Tasks Phase 3:**
- [x] PricingTier enum
- [x] InstanceConfig entity
- [x] FeatureDetectionService
- [x] Feature Detection APIs
- [x] Feature-gated endpoints (examples)
- [x] Caching configuration
- [x] Unit tests

**Deliverables:**
- Feature detection system working
- API: GET /api/v1/instance/{id}/config
- API: GET /api/v1/instance/{id}/features/{feature}
- Feature-gated endpoints functional
- Redis caching configured

---

# PHASE 4: AI BRANDING SERVICE INTEGRATION

**Duration:** 1 tuần
**Dependencies:** Phase 1, 2, 3
**Reference:** `system-architecture-v3-final.md` PHẦN 6C, `architecture-clarification-qa.md` PART 2

## 4.1. Branding Assets Entity

```java
// src/main/java/com/kiteclass/core/domain/entity/BrandingJob.java
package com.kiteclass.core.domain.entity;

import com.kiteclass.core.domain.enums.BrandingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * AI Branding Generation Job
 *
 * Reference: system-architecture-v3-final.md PHẦN 6C.3
 */
@Entity
@Table(name = "branding_jobs", indexes = {
    @Index(name = "idx_branding_jobs_instance", columnList = "instance_id"),
    @Index(name = "idx_branding_jobs_status", columnList = "status")
})
@Getter
@Setter
public class BrandingJob extends BaseEntity {

    @Column(name = "job_id", nullable = false, unique = true, length = 50)
    private String jobId;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "organization_name", nullable = false, length = 200)
    private String organizationName;

    @Column(name = "language", length = 10)
    private String language = "vi";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BrandingStatus status;

    /**
     * Generated assets (JSON)
     *
     * Structure:
     * {
     *   "profileImages": {
     *     "cutout": "https://cdn.../cutout.png",
     *     "circle": "https://cdn.../circle.png",
     *     "square": "https://cdn.../square.png"
     *   },
     *   "heroImages": [
     *     "https://cdn.../hero-1.jpg",
     *     "https://cdn.../hero-2.jpg"
     *   ],
     *   "brandLogos": [
     *     "https://cdn.../logo-light.svg",
     *     "https://cdn.../logo-dark.svg"
     *   ],
     *   "banners": [
     *     "https://cdn.../banner-facebook.png",
     *     "https://cdn.../banner-youtube.png"
     *   ],
     *   "ogImage": "https://cdn.../og-image.jpg",
     *   "marketingCopy": {
     *     "headline": "...",
     *     "tagline": "...",
     *     "description": "..."
     *   }
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_assets", columnDefinition = "jsonb")
    private Map<String, Object> generatedAssets;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    /**
     * Check if job is complete
     */
    public boolean isComplete() {
        return BrandingStatus.COMPLETED.equals(this.status);
    }

    /**
     * Check if job failed
     */
    public boolean isFailed() {
        return BrandingStatus.FAILED.equals(this.status);
    }
}

// BrandingStatus.java
public enum BrandingStatus {
    PENDING,      // Job created, waiting to start
    PROCESSING,   // AI generation in progress
    COMPLETED,    // All assets generated
    FAILED        // Generation failed
}
```

## 4.2. AI Branding Service (HTTP Client to KiteHub)

```java
// src/main/java/com/kiteclass/core/service/AIBrandingService.java
package com.kiteclass.core.service;

import com.kiteclass.core.domain.entity.BrandingJob;
import com.kiteclass.core.domain.enums.BrandingStatus;
import com.kiteclass.core.repository.BrandingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * AI Branding Service
 *
 * Communicates with KiteHub AI Branding Service
 *
 * Reference: system-architecture-v3-final.md PHẦN 6C.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIBrandingService {

    private final BrandingJobRepository brandingJobRepo;
    private final RestTemplate restTemplate;

    @Value("${kitehub.branding.api.url}")
    private String kiteHubBrandingApiUrl;

    /**
     * Upload logo và start AI generation job
     *
     * @param instanceId Instance ID
     * @param logoFile Logo image file
     * @param organizationName Organization name
     * @param language Language (vi, en)
     * @return BrandingJob
     */
    public BrandingJob startBrandingGeneration(
        UUID instanceId,
        MultipartFile logoFile,
        String organizationName,
        String language
    ) {
        // Generate job ID
        String jobId = generateJobId();

        // Create branding job record
        BrandingJob job = new BrandingJob();
        job.setJobId(jobId);
        job.setInstanceId(instanceId);
        job.setOrganizationName(organizationName);
        job.setLanguage(language);
        job.setStatus(BrandingStatus.PENDING);
        job.setProgressPercentage(0);

        brandingJobRepo.save(job);

        // Call KiteHub AI Branding API (async)
        try {
            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Build request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("logo", logoFile.getResource());
            body.add("jobId", jobId);
            body.add("organizationName", organizationName);
            body.add("language", language);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Send async request
            String url = kiteHubBrandingApiUrl + "/api/v1/branding/generate";
            ResponseEntity<BrandingGenerationResponse> response = restTemplate.postForEntity(
                url,
                requestEntity,
                BrandingGenerationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                // Upload successful, job queued
                job.setStatus(BrandingStatus.PROCESSING);
                job.setProgressPercentage(5);
                brandingJobRepo.save(job);

                log.info("Branding job {} queued successfully", jobId);
            }

        } catch (Exception e) {
            log.error("Failed to start branding job {}: {}", jobId, e.getMessage());
            job.setStatus(BrandingStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            brandingJobRepo.save(job);
        }

        return job;
    }

    /**
     * Get job status
     *
     * Poll endpoint to check generation progress
     */
    public BrandingJob getJobStatus(String jobId) {
        return brandingJobRepo.findByJobId(jobId)
            .orElseThrow(() -> new NotFoundException("Branding job not found: " + jobId));
    }

    /**
     * Update job progress (called by KiteHub webhook)
     */
    public void updateJobProgress(String jobId, int progress) {
        BrandingJob job = getJobStatus(jobId);
        job.setProgressPercentage(progress);
        brandingJobRepo.save(job);
    }

    /**
     * Complete job với generated assets (called by KiteHub webhook)
     */
    public void completeJob(String jobId, Map<String, Object> generatedAssets) {
        BrandingJob job = getJobStatus(jobId);
        job.setStatus(BrandingStatus.COMPLETED);
        job.setProgressPercentage(100);
        job.setGeneratedAssets(generatedAssets);
        brandingJobRepo.save(job);

        log.info("Branding job {} completed successfully", jobId);
    }

    /**
     * Mark job as failed
     */
    public void failJob(String jobId, String errorMessage) {
        BrandingJob job = getJobStatus(jobId);
        job.setStatus(BrandingStatus.FAILED);
        job.setErrorMessage(errorMessage);
        brandingJobRepo.save(job);

        log.error("Branding job {} failed: {}", jobId, errorMessage);
    }

    private String generateJobId() {
        return "BRAND-" + System.currentTimeMillis() + "-" +
               UUID.randomUUID().toString().substring(0, 8);
    }
}

// Response DTOs
@Data
public class BrandingGenerationResponse {
    private String jobId;
    private String status;
    private String message;
}
```

## 4.3. Branding Controller

```java
// src/main/java/com/kiteclass/core/controller/BrandingController.java
package com.kiteclass.core.controller;

import com.kiteclass.core.domain.dto.BrandingJobDTO;
import com.kiteclass.core.service.AIBrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branding")
@RequiredArgsConstructor
@Tag(name = "AI Branding", description = "AI branding generation APIs")
public class BrandingController {

    private final AIBrandingService brandingService;

    /**
     * Upload logo và start AI generation
     *
     * POST /api/v1/branding/upload
     */
    @Operation(summary = "Upload logo and start AI branding generation")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CENTER_OWNER') or hasRole('CENTER_ADMIN')")
    public ResponseEntity<BrandingJobDTO> uploadLogo(
        @RequestHeader("X-Instance-Id") UUID instanceId,
        @RequestParam("logo") MultipartFile logoFile,
        @RequestParam("organizationName") String organizationName,
        @RequestParam(value = "language", defaultValue = "vi") String language
    ) {
        // Validate file
        if (logoFile.isEmpty()) {
            throw new ValidationException("Logo file is required");
        }

        // Validate file type
        String contentType = logoFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ValidationException("File must be an image");
        }

        // Validate file size (max 5MB)
        if (logoFile.getSize() > 5 * 1024 * 1024) {
            throw new ValidationException("File size must not exceed 5MB");
        }

        // Start generation
        BrandingJob job = brandingService.startBrandingGeneration(
            instanceId,
            logoFile,
            organizationName,
            language
        );

        BrandingJobDTO dto = BrandingJobDTO.from(job);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    /**
     * Get job status
     *
     * GET /api/v1/branding/status/{jobId}
     */
    @Operation(summary = "Get branding job status")
    @GetMapping("/status/{jobId}")
    public ResponseEntity<BrandingJobDTO> getJobStatus(
        @PathVariable String jobId
    ) {
        BrandingJob job = brandingService.getJobStatus(jobId);
        BrandingJobDTO dto = BrandingJobDTO.from(job);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get generated assets
     *
     * GET /api/v1/branding/assets/{jobId}
     */
    @Operation(summary = "Get generated branding assets")
    @GetMapping("/assets/{jobId}")
    public ResponseEntity<Map<String, Object>> getAssets(
        @PathVariable String jobId
    ) {
        BrandingJob job = brandingService.getJobStatus(jobId);

        if (!job.isComplete()) {
            throw new InvalidStateException("Branding job is not complete yet");
        }

        return ResponseEntity.ok(job.getGeneratedAssets());
    }

    /**
     * Webhook endpoint for KiteHub to update job progress
     *
     * POST /api/v1/branding/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
        @RequestBody BrandingWebhookPayload payload
    ) {
        switch (payload.getEvent()) {
            case "progress":
                brandingService.updateJobProgress(payload.getJobId(), payload.getProgress());
                break;
            case "completed":
                brandingService.completeJob(payload.getJobId(), payload.getAssets());
                break;
            case "failed":
                brandingService.failJob(payload.getJobId(), payload.getErrorMessage());
                break;
        }

        return ResponseEntity.ok().build();
    }
}

// DTOs
@Data
@Builder
public class BrandingJobDTO {
    private String jobId;
    private UUID instanceId;
    private String organizationName;
    private String language;
    private String status;
    private Integer progressPercentage;
    private Map<String, Object> generatedAssets;
    private String errorMessage;

    public static BrandingJobDTO from(BrandingJob job) {
        return BrandingJobDTO.builder()
            .jobId(job.getJobId())
            .instanceId(job.getInstanceId())
            .organizationName(job.getOrganizationName())
            .language(job.getLanguage())
            .status(job.getStatus().name())
            .progressPercentage(job.getProgressPercentage())
            .generatedAssets(job.getGeneratedAssets())
            .errorMessage(job.getErrorMessage())
            .build();
    }
}

@Data
public class BrandingWebhookPayload {
    private String event; // "progress", "completed", "failed"
    private String jobId;
    private Integer progress;
    private Map<String, Object> assets;
    private String errorMessage;
}
```

**Tasks Phase 4:**
- [x] BrandingJob entity
- [x] AIBrandingService
- [x] Branding APIs (upload, status, assets)
- [x] Webhook endpoint
- [x] File validation
- [x] Integration tests with KiteHub mock

**Deliverables:**
- API: POST /api/v1/branding/upload
- API: GET /api/v1/branding/status/{jobId}
- API: GET /api/v1/branding/assets/{jobId}
- API: POST /api/v1/branding/webhook
- KiteHub integration working

---

# PHASE 5: PREVIEW WEBSITE PUBLIC APIs

**Duration:** 3 ngày
**Dependencies:** Phase 3, 4
**Reference:** `system-architecture-v3-final.md` PHẦN 6D, `architecture-clarification-qa.md` PART 3

## 5.1. Public Course DTO

```java
// src/main/java/com/kiteclass/core/domain/dto/PublicCourseDTO.java
package com.kiteclass.core.domain.dto;

import com.kiteclass.core.domain.entity.Course;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Public Course DTO (filtered for guest users)
 *
 * Excludes internal fields: createdBy, updatedBy, version, etc.
 *
 * Reference: system-architecture-v3-final.md PHẦN 6D.4
 */
@Data
@Builder
public class PublicCourseDTO {
    private UUID id;
    private String title;
    private String description;
    private String category;
    private Long price; // VND
    private String teacherName;
    private String teacherBio;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private String schedule; // "Mon, Wed, Fri 18:00-20:00"
    private Integer totalSeats;
    private Integer availableSeats;

    public static PublicCourseDTO from(Course course) {
        return PublicCourseDTO.builder()
            .id(course.getId())
            .title(course.getTitle())
            .description(course.getDescription())
            .category(course.getCategory())
            .price(course.getPrice())
            .teacherName(course.getTeacher().getName())
            .teacherBio(course.getTeacher().getBio())
            .imageUrl(course.getImageUrl())
            .startDate(course.getStartDate())
            .endDate(course.getEndDate())
            .schedule(course.getSchedule())
            .totalSeats(course.getTotalSeats())
            .availableSeats(course.getAvailableSeats())
            .build();
    }
}
```

## 5.2. Public APIs Controller

```java
// src/main/java/com/kiteclass/core/controller/PublicController.java
package com.kiteclass.core.controller;

import com.kiteclass.core.domain.dto.PublicCourseDTO;
import com.kiteclass.core.domain.dto.PublicInstanceConfigDTO;
import com.kiteclass.core.domain.dto.PublicBrandingDTO;
import com.kiteclass.core.service.PublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Public APIs (no authentication required)
 *
 * Used by Preview Website (guest users)
 *
 * Reference: system-architecture-v3-final.md PHẦN 6D.5
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public APIs", description = "Public APIs for guest users (no auth)")
public class PublicController {

    private final PublicService publicService;

    /**
     * 1. Get instance configuration (public data only)
     *
     * GET /api/v1/public/instance/{instanceId}/config
     */
    @Operation(summary = "Get public instance configuration")
    @GetMapping("/instance/{instanceId}/config")
    public ResponseEntity<PublicInstanceConfigDTO> getInstanceConfig(
        @PathVariable UUID instanceId
    ) {
        PublicInstanceConfigDTO config = publicService.getPublicInstanceConfig(instanceId);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
            .body(config);
    }

    /**
     * 2. Get branding assets
     *
     * GET /api/v1/public/instance/{instanceId}/branding
     */
    @Operation(summary = "Get instance branding assets")
    @GetMapping("/instance/{instanceId}/branding")
    public ResponseEntity<PublicBrandingDTO> getBranding(
        @PathVariable UUID instanceId
    ) {
        PublicBrandingDTO branding = publicService.getPublicBranding(instanceId);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
            .body(branding);
    }

    /**
     * 3. Get public courses (catalog)
     *
     * GET /api/v1/public/instance/{instanceId}/courses
     */
    @Operation(summary = "Get public course catalog")
    @GetMapping("/instance/{instanceId}/courses")
    public ResponseEntity<List<PublicCourseDTO>> getPublicCourses(
        @PathVariable UUID instanceId,
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        List<PublicCourseDTO> courses = publicService.getPublicCourses(
            instanceId,
            category,
            page,
            size
        );

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
            .body(courses);
    }

    /**
     * 4. Get course details
     *
     * GET /api/v1/public/courses/{courseId}
     */
    @Operation(summary = "Get public course details")
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<PublicCourseDTO> getCourseDetails(
        @PathVariable UUID courseId
    ) {
        PublicCourseDTO course = publicService.getPublicCourseDetails(courseId);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
            .body(course);
    }

    /**
     * 5. Get instructors
     *
     * GET /api/v1/public/instance/{instanceId}/instructors
     */
    @Operation(summary = "Get public instructor list")
    @GetMapping("/instance/{instanceId}/instructors")
    public ResponseEntity<List<PublicInstructorDTO>> getInstructors(
        @PathVariable UUID instanceId
    ) {
        List<PublicInstructorDTO> instructors = publicService.getPublicInstructors(instanceId);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
            .body(instructors);
    }

    /**
     * 6. Submit contact form
     *
     * POST /api/v1/public/contact
     */
    @Operation(summary = "Submit contact form")
    @PostMapping("/contact")
    public ResponseEntity<ContactFormResponse> submitContactForm(
        @RequestBody @Valid ContactFormRequest request
    ) {
        publicService.handleContactForm(request);

        ContactFormResponse response = ContactFormResponse.builder()
            .success(true)
            .message("Cảm ơn bạn đã liên hệ! Chúng tôi sẽ phản hồi sớm nhất.")
            .build();

        return ResponseEntity.ok(response);
    }
}

// DTOs
@Data
@Builder
public class PublicInstanceConfigDTO {
    private UUID instanceId;
    private String organizationName;
    private String contactEmail;
    private String contactPhone;
    private String facebookUrl;
    private String messengerUrl;
    private String zaloUrl;
}

@Data
@Builder
public class PublicBrandingDTO {
    private Map<String, Object> brandingAssets;
}

@Data
@Builder
public class PublicInstructorDTO {
    private UUID id;
    private String name;
    private String bio;
    private String imageUrl;
}

@Data
public class ContactFormRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    @NotBlank
    private String message;

    private UUID courseId; // Optional
}

@Data
@Builder
public class ContactFormResponse {
    private Boolean success;
    private String message;
}
```

## 5.3. Rate Limiting for Public APIs

```java
// src/main/java/com/kiteclass/core/config/RateLimitConfig.java
package com.kiteclass.core.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration for public APIs
 *
 * Limit: 100 requests per minute per IP
 */
@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Bean
    public Map<String, Bucket> rateLimitBuckets() {
        return cache;
    }

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            100,
            Refill.intervally(100, Duration.ofMinutes(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
}

// Rate Limit Interceptor
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {

        String ip = getClientIP(request);
        Bucket bucket = rateLimitConfig.resolveBucket(ip);

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests");
            return false;
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
```

**Tasks Phase 5:**
- [x] PublicCourseDTO (filtered)
- [x] Public APIs controller (6 endpoints)
- [x] PublicService implementation
- [x] Rate limiting (100 req/min)
- [x] Cache headers (1 hour for config, 5 min for courses)
- [x] Integration tests

**Deliverables:**
- API: GET /api/v1/public/instance/{id}/config
- API: GET /api/v1/public/instance/{id}/branding
- API: GET /api/v1/public/instance/{id}/courses
- API: GET /api/v1/public/courses/{id}
- API: GET /api/v1/public/instance/{id}/instructors
- API: POST /api/v1/public/contact
- Rate limiting: 100 req/min per IP
- Cache headers configured

---

**[Document continues with Phase 6, 7, 8, Testing, and Deployment sections...]**

*Due to length constraints, I'll continue with the next phases in subsequent messages. This document is approximately 40% complete (~2,000+ lines so far).*

**Should I continue with:**
1. Phase 6: Guest & Trial System
2. Phase 7: VietQR Payment System
3. Phase 8: Business Logic Modules
4. Testing Strategy
5. Deployment Plan

Or would you like me to move to the next document (KiteHub Implementation Plan)?
