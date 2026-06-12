# PLAN: KiteClass Gateway Service Implementation

## Thông tin

| Thuộc tính | Giá trị |
|------------|---------|
| **Service** | kiteclass-gateway |
| **Version** | V4.1 (Bundled Model) ⭐ |
| **Tech Stack** | Spring Boot 3.5+, Spring Cloud Gateway, Java 17 |
| **Mục đích** | API Gateway + User Service + Authentication + Guest Access |
| **Port** | 8080 |
| **Last Updated** | 2026-02-26 |
| **Tham chiếu** | architecture-overview, api-design, database-design, **maven-dependencies** |
| **New in V4.1** | Guest user type handling, Trial lesson access control routing |

> **QUAN TRỌNG:** Luôn check `.claude/skills/maven-dependencies.md` để lấy versions chuẩn trước khi tạo pom.xml

---

# TỔNG QUAN

## Responsibilities

```
kiteclass-gateway/
│
├── 🚪 API Gateway
│   ├── Route requests to Core Service
│   ├── Rate limiting
│   ├── Request/Response logging
│   └── CORS handling
│
├── 👤 User Service
│   ├── User CRUD
│   ├── Role & Permission management
│   └── Profile management
│
├── 🔐 Authentication
│   ├── Login/Logout
│   ├── JWT token generation & validation
│   ├── Refresh token
│   └── Password reset
│
└── 🛡️ Authorization
    ├── Role-based access control (RBAC)
    ├── Permission checking
    └── Token validation filter
```

---

# PHASE 1: PROJECT INITIALIZATION

## 1.1. Project Structure

```
kiteclass/
└── kiteclass-gateway/
    ├── src/
    │   ├── main/
    │   │   ├── java/com/kiteclass/gateway/
    │   │   │   ├── KiteclassGatewayApplication.java
    │   │   │   ├── config/
    │   │   │   ├── security/
    │   │   │   ├── filter/
    │   │   │   ├── module/
    │   │   │   │   └── user/
    │   │   │   └── common/
    │   │   └── resources/
    │   │       ├── application.yml
    │   │       └── db/migration/
    │   └── test/
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
    <artifactId>kiteclass-gateway</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>KiteClass Gateway Service</name>
    <description>API Gateway + User Service for KiteClass Instance</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Cloud Gateway -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-r2dbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- R2DBC PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>r2dbc-postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway (requires JDBC for migrations) -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
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

        <!-- Rate Limiting -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>8.7.0</version>
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

        <!-- OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
            <version>2.3.0</version>
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
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
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
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 1.3. Application Configuration

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: kiteclass-gateway

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

  # R2DBC (Reactive)
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:kiteclass_dev}
    username: ${DB_USERNAME:kiteclass}
    password: ${DB_PASSWORD:kiteclass123}
    pool:
      initial-size: 5
      max-size: 20

  # JDBC (for Flyway migrations)
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:kiteclass_dev}
    username: ${DB_USERNAME:kiteclass}
    password: ${DB_PASSWORD:kiteclass123}

  flyway:
    enabled: true
    locations: classpath:db/migration

  # Redis
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

  # Cloud Gateway
  cloud:
    gateway:
      routes:
        # Core Service Routes
        - id: core-students
          uri: ${CORE_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/v1/students/**
          filters:
            - AuthenticationFilter

        - id: core-classes
          uri: ${CORE_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/v1/classes/**
          filters:
            - AuthenticationFilter

        - id: core-attendance
          uri: ${CORE_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/v1/attendance/**
          filters:
            - AuthenticationFilter

        - id: core-invoices
          uri: ${CORE_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/v1/invoices/**,/api/v1/payments/**
          filters:
            - AuthenticationFilter

      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_UNIQUE

      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"
              - "${FRONTEND_URL:http://localhost:3000}"
            allowedMethods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600

server:
  port: 8080

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-super-secret-key-min-512-bits-long-for-hs512-algorithm-security}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:3600000}      # 1 hour
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 days

# Rate Limiting
rate-limit:
  enabled: true
  requests-per-second: 100
  burst-capacity: 200

# Logging
logging:
  level:
    root: INFO
    com.kiteclass: DEBUG
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,gateway
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
    gateway:
      enabled: true
```

---

# PHASE 2: SECURITY & AUTHENTICATION

## 2.1. Package Structure

```
com.kiteclass.gateway/
├── config/
│   ├── SecurityConfig.java
│   ├── R2dbcConfig.java
│   └── RedisConfig.java
├── security/
│   ├── jwt/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtProperties.java
│   │   └── TokenType.java
│   ├── UserPrincipal.java
│   └── SecurityContextRepository.java
├── filter/
│   ├── AuthenticationFilter.java
│   ├── RateLimitFilter.java
│   └── RequestLoggingFilter.java
└── module/
    ├── auth/
    └── user/
```

## 2.2. JWT Token Provider

```java
package com.kiteclass.gateway.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate access token.
     */
    public String generateAccessToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("type", TokenType.ACCESS.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate refresh token.
     */
    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", TokenType.REFRESH.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate token and return claims.
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract user ID from token.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extract roles from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = validateToken(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * Check if token is access token.
     */
    public boolean isAccessToken(String token) {
        Claims claims = validateToken(token);
        String type = claims.get("type", String.class);
        return TokenType.ACCESS.name().equals(type);
    }
}
```

## 2.3. Security Configuration

```java
package com.kiteclass.gateway.config;

import com.kiteclass.gateway.security.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityContextRepository securityContextRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                .securityContextRepository(securityContextRepository)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return Mono.empty();
                        })
                        .accessDeniedHandler((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return Mono.empty();
                        })
                )

                .authorizeExchange(auth -> auth
                        // Public endpoints
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers("/api/v1/auth/login").permitAll()
                        .pathMatchers("/api/v1/auth/refresh").permitAll()
                        .pathMatchers("/api/v1/auth/forgot-password").permitAll()
                        .pathMatchers("/api/v1/auth/reset-password").permitAll()
                        .pathMatchers("/swagger-ui/**", "/api-docs/**").permitAll()

                        // Protected endpoints
                        .pathMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "OWNER")
                        .pathMatchers("/api/v1/settings/**").hasAnyRole("ADMIN", "OWNER")

                        // All other requests require authentication
                        .anyExchange().authenticated()
                )

                .build();
    }
}
```

## 2.4. Authentication Filter (Gateway Filter)

```java
package com.kiteclass.gateway.filter;

import com.kiteclass.gateway.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Extract token from header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // Validate token
                if (!jwtTokenProvider.isAccessToken(token)) {
                    return onError(exchange, "Invalid token type", HttpStatus.UNAUTHORIZED);
                }

                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);

                // Add user info to headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", String.join(",", roles))
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (JwtException e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        log.warn("Authentication error: {} - {}", status, message);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties if needed
    }
}
```

## 2.5. Rate Limiting Filter

```java
package com.kiteclass.gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

        if (bucket.tryConsume(1)) {
            return chain.filter(exchange);
        }

        log.warn("Rate limit exceeded for IP: {}", clientIp);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After", "1");
        return exchange.getResponse().setComplete();
    }

    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }
}
```

---

# PHASE 3: USER MODULE

## 3.1. Package Structure

```
module/user/
├── controller/
│   └── UserController.java
├── service/
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java
├── repository/
│   └── UserRepository.java
├── entity/
│   ├── User.java
│   ├── Role.java
│   └── Permission.java
├── dto/
│   ├── UserResponse.java
│   ├── CreateUserRequest.java
│   ├── UpdateUserRequest.java
│   └── ChangePasswordRequest.java
└── mapper/
    └── UserMapper.java
```

## 3.2. Entities

### User.java

```java
package com.kiteclass.gateway.module.user.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;

@Table("users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private Long id;

    @Column("email")
    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("name")
    private String name;

    @Column("phone")
    private String phone;

    @Column("avatar_url")
    private String avatarUrl;

    @Column("status")
    private String status;  // ACTIVE, INACTIVE, LOCKED, PENDING

    @Column("email_verified")
    private Boolean emailVerified;

    @Column("last_login_at")
    private Instant lastLoginAt;

    @Column("failed_login_attempts")
    private Integer failedLoginAttempts;

    @Column("locked_until")
    private Instant lockedUntil;

    // Audit fields
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    @Column("deleted")
    private Boolean deleted;

    @Column("deleted_at")
    private Instant deletedAt;
}
```

### Role.java

```java
package com.kiteclass.gateway.module.user.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    private Long id;

    @Column("code")
    private String code;  // OWNER, ADMIN, TEACHER, STAFF, PARENT

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("is_system")
    private Boolean isSystem;  // System roles cannot be deleted
}
```

### UserRole.java (Join Table)

```java
@Table("user_roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("role_id")
    private Long roleId;

    @Column("assigned_at")
    private Instant assignedAt;

    @Column("assigned_by")
    private Long assignedBy;
}
```

## 3.3. Repository (R2DBC)

```java
package com.kiteclass.gateway.module.user.repository;

import com.kiteclass.gateway.module.user.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    Mono<User> findByEmailAndDeletedFalse(String email);

    Mono<Boolean> existsByEmailAndDeletedFalse(String email);

    @Query("""
        SELECT u.* FROM users u
        WHERE u.deleted = false
        AND (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR u.status = :status)
        ORDER BY u.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    Flux<User> findBySearchCriteria(String search, String status, int limit, int offset);

    @Query("SELECT COUNT(*) FROM users WHERE deleted = false")
    Mono<Long> countActive();
}

// RoleRepository.java
@Repository
public interface RoleRepository extends R2dbcRepository<Role, Long> {

    Mono<Role> findByCode(String code);

    Flux<Role> findAllByIsSystemTrue();
}

// UserRoleRepository.java
@Repository
public interface UserRoleRepository extends R2dbcRepository<UserRole, Long> {

    Flux<UserRole> findByUserId(Long userId);

    @Query("""
        SELECT r.* FROM roles r
        INNER JOIN user_roles ur ON r.id = ur.role_id
        WHERE ur.user_id = :userId
    """)
    Flux<Role> findRolesByUserId(Long userId);

    Mono<Void> deleteByUserId(Long userId);
}
```

## 3.4. Service

```java
package com.kiteclass.gateway.module.user.service;

import com.kiteclass.gateway.module.user.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserResponse> createUser(CreateUserRequest request);

    Mono<UserResponse> getUserById(Long id);

    Mono<UserResponse> getUserByEmail(String email);

    Flux<UserResponse> getUsers(String search, String status, int page, int size);

    Mono<Long> countUsers();

    Mono<UserResponse> updateUser(Long id, UpdateUserRequest request);

    Mono<Void> changePassword(Long id, ChangePasswordRequest request);

    Mono<Void> deleteUser(Long id);

    Mono<Void> assignRoles(Long userId, List<String> roleCodes);
}
```

```java
package com.kiteclass.gateway.module.user.service.impl;

import com.kiteclass.gateway.common.exception.*;
import com.kiteclass.gateway.module.user.dto.*;
import com.kiteclass.gateway.module.user.entity.*;
import com.kiteclass.gateway.module.user.mapper.UserMapper;
import com.kiteclass.gateway.module.user.repository.*;
import com.kiteclass.gateway.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Mono<UserResponse> createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.email());

        return userRepository.existsByEmailAndDeletedFalse(request.email())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new DuplicateResourceException("USER_EMAIL_EXISTS", request.email()));
                    }

                    User user = userMapper.toEntity(request);
                    user.setPasswordHash(passwordEncoder.encode(request.password()));
                    user.setStatus("ACTIVE");
                    user.setDeleted(false);
                    user.setFailedLoginAttempts(0);
                    user.setEmailVerified(false);

                    return userRepository.save(user);
                })
                .flatMap(savedUser ->
                        assignRolesToUser(savedUser.getId(), request.roles())
                                .then(Mono.just(savedUser))
                )
                .flatMap(this::enrichUserWithRoles)
                .doOnSuccess(user -> log.info("Created user with id: {}", user.id()));
    }

    @Override
    public Mono<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .filter(user -> !user.getDeleted())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User", id)))
                .flatMap(this::enrichUserWithRoles);
    }

    @Override
    public Flux<UserResponse> getUsers(String search, String status, int page, int size) {
        int offset = page * size;
        return userRepository.findBySearchCriteria(search, status, size, offset)
                .flatMap(this::enrichUserWithRoles);
    }

    @Override
    @Transactional
    public Mono<UserResponse> updateUser(Long id, UpdateUserRequest request) {
        return userRepository.findById(id)
                .filter(user -> !user.getDeleted())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User", id)))
                .flatMap(user -> {
                    userMapper.updateEntity(user, request);
                    return userRepository.save(user);
                })
                .flatMap(this::enrichUserWithRoles);
    }

    @Override
    @Transactional
    public Mono<Void> changePassword(Long id, ChangePasswordRequest request) {
        return userRepository.findById(id)
                .filter(user -> !user.getDeleted())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User", id)))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                        return Mono.error(new BusinessException("AUTH_INVALID_PASSWORD",
                                org.springframework.http.HttpStatus.BAD_REQUEST));
                    }
                    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                    return userRepository.save(user);
                })
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> deleteUser(Long id) {
        return userRepository.findById(id)
                .filter(user -> !user.getDeleted())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User", id)))
                .flatMap(user -> {
                    user.setDeleted(true);
                    user.setDeletedAt(Instant.now());
                    return userRepository.save(user);
                })
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> assignRoles(Long userId, List<String> roleCodes) {
        return userRoleRepository.deleteByUserId(userId)
                .then(assignRolesToUser(userId, roleCodes));
    }

    private Mono<Void> assignRolesToUser(Long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(roleCodes)
                .flatMap(roleCode -> roleRepository.findByCode(roleCode))
                .flatMap(role -> {
                    UserRole userRole = UserRole.builder()
                            .userId(userId)
                            .roleId(role.getId())
                            .assignedAt(Instant.now())
                            .build();
                    return userRoleRepository.save(userRole);
                })
                .then();
    }

    private Mono<UserResponse> enrichUserWithRoles(User user) {
        return userRoleRepository.findRolesByUserId(user.getId())
                .map(Role::getCode)
                .collectList()
                .map(roles -> userMapper.toResponse(user, roles));
    }
}
```

## 3.5. Controller

```java
package com.kiteclass.gateway.module.user.controller;

import com.kiteclass.gateway.common.dto.ApiResponse;
import com.kiteclass.gateway.common.dto.PageResponse;
import com.kiteclass.gateway.module.user.dto.*;
import com.kiteclass.gateway.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @Operation(summary = "Get all users with pagination")
    public Mono<ResponseEntity<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return userService.getUsers(search, status, page, size)
                .collectList()
                .zipWith(userService.countUsers())
                .map(tuple -> {
                    PageResponse<UserResponse> response = PageResponse.<UserResponse>builder()
                            .content(tuple.getT1())
                            .page(page)
                            .size(size)
                            .totalElements(tuple.getT2())
                            .totalPages((int) Math.ceil((double) tuple.getT2() / size))
                            .build();
                    return ResponseEntity.ok(response);
                });
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER') or #id == authentication.principal.id")
    @Operation(summary = "Get user by ID")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> getUser(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @Operation(summary = "Create a new user")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        return userService.createUser(request)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(user, "Tạo người dùng thành công")));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @Operation(summary = "Update user")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        return userService.updateUser(id, request)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user, "Cập nhật thành công")));
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("#id == authentication.principal.id")
    @Operation(summary = "Change user password")
    public Mono<ResponseEntity<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {

        return userService.changePassword(id, request)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Delete user")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Assign roles to user")
    public Mono<ResponseEntity<Void>> assignRoles(
            @PathVariable Long id,
            @RequestBody List<String> roleCodes) {

        return userService.assignRoles(id, roleCodes)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
```

---

# PHASE 4: AUTH MODULE

## 4.1. Package Structure

```
module/auth/
├── controller/
│   └── AuthController.java
├── service/
│   ├── AuthService.java
│   └── impl/
│       └── AuthServiceImpl.java
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RefreshTokenRequest.java
│   ├── ForgotPasswordRequest.java
│   └── ResetPasswordRequest.java
└── entity/
    └── RefreshToken.java
```

## 4.2. Auth Service

```java
package com.kiteclass.gateway.module.auth.service;

import com.kiteclass.gateway.module.auth.dto.*;
import reactor.core.publisher.Mono;

public interface AuthService {

    Mono<LoginResponse> login(LoginRequest request);

    Mono<LoginResponse> refreshToken(RefreshTokenRequest request);

    Mono<Void> logout(String refreshToken);

    Mono<Void> forgotPassword(ForgotPasswordRequest request);

    Mono<Void> resetPassword(ResetPasswordRequest request);
}
```

```java
package com.kiteclass.gateway.module.auth.service.impl;

import com.kiteclass.gateway.common.exception.BusinessException;
import com.kiteclass.gateway.module.auth.dto.*;
import com.kiteclass.gateway.module.auth.entity.RefreshToken;
import com.kiteclass.gateway.module.auth.repository.RefreshTokenRepository;
import com.kiteclass.gateway.module.auth.service.AuthService;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import com.kiteclass.gateway.module.user.repository.UserRoleRepository;
import com.kiteclass.gateway.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    @Override
    @Transactional
    public Mono<LoginResponse> login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        return userRepository.findByEmailAndDeletedFalse(request.email())
                .switchIfEmpty(Mono.error(new BusinessException("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> validateUserAndPassword(user, request.password()))
                .flatMap(this::generateTokens)
                .doOnError(e -> log.warn("Login failed for {}: {}", request.email(), e.getMessage()));
    }

    private Mono<User> validateUserAndPassword(User user, String password) {
        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            return Mono.error(new BusinessException("AUTH_ACCOUNT_LOCKED", HttpStatus.FORBIDDEN));
        }

        // Check if account is active
        if (!"ACTIVE".equals(user.getStatus())) {
            return Mono.error(new BusinessException("AUTH_ACCOUNT_INACTIVE", HttpStatus.FORBIDDEN));
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return handleFailedLogin(user)
                    .then(Mono.error(new BusinessException("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED)));
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        return userRepository.save(user);
    }

    private Mono<User> handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60));
            log.warn("Account locked for user: {} after {} failed attempts", user.getEmail(), attempts);
        }

        return userRepository.save(user);
    }

    private Mono<LoginResponse> generateTokens(User user) {
        return userRoleRepository.findRolesByUserId(user.getId())
                .map(role -> role.getCode())
                .collectList()
                .flatMap(roles -> {
                    String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
                    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

                    // Save refresh token
                    RefreshToken tokenEntity = RefreshToken.builder()
                            .token(refreshToken)
                            .userId(user.getId())
                            .expiresAt(Instant.now().plusMillis(604800000)) // 7 days
                            .build();

                    return refreshTokenRepository.save(tokenEntity)
                            .map(saved -> LoginResponse.builder()
                                    .accessToken(accessToken)
                                    .refreshToken(refreshToken)
                                    .tokenType("Bearer")
                                    .expiresIn(3600)
                                    .user(UserInfo.builder()
                                            .id(user.getId())
                                            .email(user.getEmail())
                                            .name(user.getName())
                                            .roles(roles)
                                            .build())
                                    .build());
                });
    }

    @Override
    @Transactional
    public Mono<LoginResponse> refreshToken(RefreshTokenRequest request) {
        return refreshTokenRepository.findByToken(request.refreshToken())
                .switchIfEmpty(Mono.error(new BusinessException("AUTH_REFRESH_EXPIRED", HttpStatus.UNAUTHORIZED)))
                .flatMap(token -> {
                    if (token.getExpiresAt().isBefore(Instant.now())) {
                        return refreshTokenRepository.delete(token)
                                .then(Mono.error(new BusinessException("AUTH_REFRESH_EXPIRED", HttpStatus.UNAUTHORIZED)));
                    }

                    return userRepository.findById(token.getUserId())
                            .filter(user -> !user.getDeleted() && "ACTIVE".equals(user.getStatus()))
                            .switchIfEmpty(Mono.error(new BusinessException("AUTH_ACCOUNT_INACTIVE", HttpStatus.FORBIDDEN)))
                            .flatMap(user -> {
                                // Delete old refresh token
                                return refreshTokenRepository.delete(token)
                                        .then(generateTokens(user));
                            });
                });
    }

    @Override
    @Transactional
    public Mono<Void> logout(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .flatMap(refreshTokenRepository::delete)
                .then();
    }

    @Override
    public Mono<Void> forgotPassword(ForgotPasswordRequest request) {
        // TODO: Implement email sending with reset token
        return userRepository.findByEmailAndDeletedFalse(request.email())
                .flatMap(user -> {
                    // Generate reset token and save
                    // Send email with reset link
                    log.info("Password reset requested for: {}", request.email());
                    return Mono.empty();
                })
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        // TODO: Validate reset token and update password
        return Mono.empty();
    }
}
```

## 4.3. Auth Controller

```java
package com.kiteclass.gateway.module.auth.controller;

import com.kiteclass.gateway.common.dto.ApiResponse;
import com.kiteclass.gateway.module.auth.dto.*;
import com.kiteclass.gateway.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        return authService.refreshToken(request)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate refresh token")
    public Mono<ResponseEntity<Void>> logout(
            @RequestBody RefreshTokenRequest request) {

        return authService.logout(request.refreshToken())
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public Mono<ResponseEntity<ApiResponse<String>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        return authService.forgotPassword(request)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success(null, "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu"))));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public Mono<ResponseEntity<ApiResponse<String>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        return authService.resetPassword(request)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success(null, "Đặt lại mật khẩu thành công"))));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public Mono<ResponseEntity<ApiResponse<UserInfo>>> getCurrentUser(
            @RequestHeader("X-User-Id") Long userId) {

        // User info is extracted from JWT by the filter
        return authService.getCurrentUser(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)));
    }
}
```

---

## Cross-Service File Access

**Scenario**: Gateway service cần serve avatar URLs hoặc validate file access permissions trước khi cho phép download.

### Challenge

- Storage Service owns file metadata (`uploaded_files` table)
- Gateway service handles authentication/authorization
- Frontend requests files through Gateway (hoặc direct từ Storage Service?)

### Architecture Options

**Option 1: Direct Storage Service Access (Recommended)**
- Frontend calls Storage Service directly: `GET /api/v1/files/{id}/download`
- Storage Service validates token (JWT validation filter)
- Returns presigned S3 URL (24h expiry)
- **Pros**: No extra hop through Gateway, lower latency
- **Cons**: Storage Service must implement full JWT validation

**Option 2: Gateway Proxy**
- Frontend calls Gateway: `GET /api/gateway/files/{id}/download`
- Gateway validates token, proxies to Storage Service
- Storage Service returns presigned URL
- **Pros**: Centralized auth validation
- **Cons**: Extra network hop, higher latency

**Recommendation**: **Option 1** (Direct Access) với:
- Storage Service implements `JwtAuthenticationFilter` (reuse from Gateway)
- Shared JWT secret (environment variable)
- Storage Service validates token, checks user permissions (uploaded_by, access_level)

### Avatar URL in User Profile Response

**Problem**: User profile response needs avatar URL, but avatar stored in Storage Service.

**Solution**: Store `avatar_file_id` in Gateway `users` table (denormalized)

1. When user uploads avatar:
   - Frontend calls Storage Service → Get `fileId`
   - Frontend calls Gateway: `PUT /api/v1/users/me { avatarFileId: "uuid" }`
   - Gateway updates `users.avatar_file_id = uuid`

2. When fetching user profile:
   - Gateway returns `avatarFileId` in response
   - Frontend calls Storage Service: `GET /api/v1/files/{avatarFileId}/download` → Get presigned URL
   - Frontend displays image từ presigned URL

**Alternative** (simpler):
- Gateway stores full S3 URL: `users.avatar_url = "https://cdn.../avatar.png"`
- Sync via RabbitMQ event: Storage Service publishes `AvatarUploadedEvent` → Gateway subscriber updates `users.avatar_url`

### CORS Configuration

Storage Service must allow cross-origin requests từ Frontend:

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000"); // Dev
        config.addAllowedOrigin("https://kitehub.me"); // Prod
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

### Related PRs

- PR 1.12: Gateway JWT validation sharing with Storage Service
- PR 2.10.1: Storage Service file management
- PR 3.10: Frontend profile picture upload

**Documentation**: See [Storage Service Design](./storage-service-design.md) for access control matrix (PRIVATE, COURSE, PUBLIC).

---

# PHASE 5: DATABASE MIGRATIONS

## Flyway Migration Files

```
src/main/resources/db/migration/
├── V1__create_user_tables.sql
├── V2__create_role_permission_tables.sql
├── V3__create_refresh_token_table.sql
└── V4__seed_roles_permissions.sql
```

### V1__create_user_tables.sql

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING'))
);

CREATE UNIQUE INDEX idx_users_email ON users(email) WHERE deleted = false;
CREATE INDEX idx_users_status ON users(status) WHERE deleted = false;
```

### V2__create_role_permission_tables.sql

```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    module VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    assigned_by BIGINT REFERENCES users(id),
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
```

### V3__create_refresh_token_table.sql

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### V4__seed_roles_permissions.sql

```sql
-- Insert system roles
INSERT INTO roles (code, name, description, is_system) VALUES
('OWNER', 'Chủ trung tâm', 'Full access to all features', true),
('ADMIN', 'Quản trị viên', 'Manage users, classes, billing', true),
('TEACHER', 'Giáo viên', 'Manage assigned classes, attendance', true),
('STAFF', 'Nhân viên', 'Limited access based on permissions', true),
('PARENT', 'Phụ huynh', 'View children information', true);

-- Insert permissions
INSERT INTO permissions (code, name, module) VALUES
-- User permissions
('USER_VIEW', 'Xem danh sách người dùng', 'USER'),
('USER_CREATE', 'Tạo người dùng', 'USER'),
('USER_UPDATE', 'Cập nhật người dùng', 'USER'),
('USER_DELETE', 'Xóa người dùng', 'USER'),

-- Student permissions
('STUDENT_VIEW', 'Xem danh sách học viên', 'STUDENT'),
('STUDENT_CREATE', 'Tạo học viên', 'STUDENT'),
('STUDENT_UPDATE', 'Cập nhật học viên', 'STUDENT'),
('STUDENT_DELETE', 'Xóa học viên', 'STUDENT'),

-- Class permissions
('CLASS_VIEW', 'Xem danh sách lớp học', 'CLASS'),
('CLASS_CREATE', 'Tạo lớp học', 'CLASS'),
('CLASS_UPDATE', 'Cập nhật lớp học', 'CLASS'),
('CLASS_DELETE', 'Xóa lớp học', 'CLASS'),

-- Attendance permissions
('ATTENDANCE_VIEW', 'Xem điểm danh', 'ATTENDANCE'),
('ATTENDANCE_MARK', 'Điểm danh', 'ATTENDANCE'),

-- Billing permissions
('INVOICE_VIEW', 'Xem hóa đơn', 'BILLING'),
('INVOICE_CREATE', 'Tạo hóa đơn', 'BILLING'),
('INVOICE_UPDATE', 'Cập nhật hóa đơn', 'BILLING'),
('PAYMENT_RECORD', 'Ghi nhận thanh toán', 'BILLING'),

-- Settings permissions
('SETTINGS_VIEW', 'Xem cài đặt', 'SETTINGS'),
('SETTINGS_UPDATE', 'Cập nhật cài đặt', 'SETTINGS');

-- Assign permissions to roles
-- OWNER has all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.code = 'OWNER';

-- ADMIN has most permissions except some settings
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code NOT IN ('USER_DELETE', 'SETTINGS_UPDATE');

-- TEACHER has limited permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'TEACHER' AND p.code IN (
    'STUDENT_VIEW', 'CLASS_VIEW', 'ATTENDANCE_VIEW', 'ATTENDANCE_MARK'
);

-- Create default owner user (password: Admin@123)
INSERT INTO users (email, password_hash, name, status, email_verified)
VALUES ('owner@kiteclass.local', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IhQRnO.xbK0l.lqZRvNT2TcMmwCJIG', 'System Owner', 'ACTIVE', true);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'owner@kiteclass.local' AND r.code = 'OWNER';
```

---

# PHASE 6: DOCKERFILE & DEPLOYMENT

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

RUN addgroup -S kiteclass && adduser -S kiteclass -G kiteclass
USER kiteclass

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

# IMPLEMENTATION CHECKLIST

## Phase 1: Project Setup
- [ ] Create Maven project
- [ ] Configure application.yml
- [ ] Verify Spring Cloud Gateway starts

## Phase 2: Security
- [ ] JWT Token Provider
- [ ] Security Configuration
- [ ] Authentication Filter
- [ ] Rate Limiting Filter

## Phase 3: User Module
- [ ] User Entity + Repository
- [ ] Role & Permission Entities
- [ ] User Service (CRUD)
- [ ] User Controller

## Phase 4: Auth Module
- [ ] Login/Logout
- [ ] JWT generation
- [ ] Refresh token
- [ ] Password reset (optional)

## Phase 4.5: Trial User Authentication (V4.1 Phase 2) ⭐ NEW
- [ ] UserRole enum: Add TRIAL_USER value (via Migration V12)
- [ ] MagicLinkService: generateMagicLink() - Create secure token (UUID) with 30min expiry
- [ ] MagicLinkService: verifyMagicLink() - Verify token + create/find User (role=TRIAL_USER) + generate JWT
- [ ] MagicLinkService: createTrialUser() - Helper to create User with TRIAL_USER role
- [ ] MagicLinkController: POST /auth/magic-link/send - Generate token + send email
- [ ] MagicLinkController: GET /auth/magic-link/verify?token={token} - Verify + return JWT
- [ ] MagicLinkData record: Store email, instanceId, createdAt in Redis
- [ ] Redis storage: Key pattern `magic_link:{token}`, TTL 30 minutes
- [ ] Email integration: Use existing EmailService to send magic link emails
- [ ] JWT claims: Ensure TRIAL_USER role included in token payload
- [ ] SecurityConfig: Add TRIAL_USER to authorized roles for trial endpoints
- [ ] Rate limiting: Apply stricter limits for TRIAL_USER (30 req/min vs 100 req/min)
- [ ] RateLimitConfig: userRoleKeyResolver() - Detect TRIAL_USER role from JWT
- [ ] One-time use: Delete token from Redis after successful verification
- [ ] Error handling: InvalidTokenException for expired/invalid magic links
- [ ] Multi-tenant support: Magic links scoped to instanceId

**Implementation Order**:
1. Migration V12: ALTER TYPE user_role ADD VALUE 'TRIAL_USER'
2. MagicLinkService: Business logic for token generation/verification
3. MagicLinkController: REST endpoints for magic link flow
4. SecurityConfig: Update authorization rules for TRIAL_USER
5. RateLimitConfig: Add role-based rate limiting
6. Testing: Unit tests + Integration tests for magic link flow

**Key Patterns**:
- Use Redis for token storage (existing RedisTemplate)
- Reuse EmailService for magic link emails
- JWT generation: Reuse existing JwtTokenProvider
- Magic link URL format: `{baseUrl}/auth/verify?token={uuid}`
- Token expiry: 30 minutes (Duration.ofMinutes(30))
- Security: HTTPS required for magic link URLs

**References**:
- PR 1.13: Trial User Authentication Support
- Migration V12: user_role enum extension (Gateway)
- Core PR 2.13: Trial registration calls this API

## Phase 5: Database
- [ ] Flyway migrations V1-V11 (existing)
- [ ] V12: Add TRIAL_USER to user_role enum ⭐ NEW Phase 2
- [ ] Seed data (roles, permissions, default user)

## Phase 6: Testing & Deployment
- [ ] Unit tests
- [ ] Integration tests
- [ ] Magic link authentication tests: Token generation, verification, expiry, one-time use ⭐ NEW Phase 2
- [ ] TRIAL_USER JWT tests: Role in claims, token validation ⭐ NEW Phase 2
- [ ] Rate limiting tests: TRIAL_USER stricter limits ⭐ NEW Phase 2
- [ ] Dockerfile
- [ ] Documentation

---

# NOTES FOR CLAUDE

1. **Gateway uses WebFlux (reactive)** - khác với Core Service (MVC)
2. **R2DBC** cho reactive database access
3. **Authentication Filter** add headers cho downstream services
4. **Rate limiting** per IP address
5. **Refresh tokens** stored in database
6. **Default owner account**: owner@kiteclass.local / Admin@123
