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

## PHẦN 7: API GATEWAY VÀ KIẾN TRÚC AUTHENTICATION/AUTHORIZATION

### 7.1. Tổng quan API Gateway

API Gateway đóng vai trò **điểm vào duy nhất** (single entry point) cho tất cả requests từ client, đảm nhiệm các chức năng quan trọng trong kiến trúc microservices.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              KITECLASS GATEWAY                                │
│                           (Spring Cloud Gateway)                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│  │   Request   │───▶│   Route     │───▶│   Filter    │───▶│   Load      │   │
│  │   Logging   │    │   Matching  │    │   Chain     │    │   Balancer  │   │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘   │
│                                              │                               │
│                                              ▼                               │
│                           ┌─────────────────────────────────┐               │
│                           │      SECURITY FILTERS           │               │
│                           │  • JWT Validation               │               │
│                           │  • Rate Limiting                │               │
│                           │  • CORS Handling                │               │
│                           │  • Request Sanitization         │               │
│                           └─────────────────────────────────┘               │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    ▼                  ▼                  ▼
            ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
            │   CORE      │    │   USER      │    │   MEDIA     │
            │   SERVICE   │    │   SERVICE   │    │   SERVICE   │
            └─────────────┘    └─────────────┘    └─────────────┘
```

### 7.2. Công nghệ Gateway

| Công nghệ | Mô tả | Lý do chọn |
|-----------|-------|------------|
| **Spring Cloud Gateway** | API Gateway reactive | Tích hợp tốt với Spring Boot ecosystem |
| **Netflix Zuul** (alternative) | Legacy gateway | Không khuyến nghị - đã deprecated |
| **Kong** (alternative) | Enterprise gateway | Phù hợp nếu cần plugin ecosystem |

**Cấu hình Gateway cơ bản:**

```yaml
# application.yml - Gateway Configuration
spring:
  cloud:
    gateway:
      routes:
        # Core Service Routes
        - id: core-service
          uri: lb://core-service
          predicates:
            - Path=/api/courses/**, /api/classes/**, /api/attendance/**, /api/forum/**
          filters:
            - JwtAuthenticationFilter
            - RateLimiter=10, 1s

        # User Service Routes
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/auth/**, /api/users/**
          filters:
            - name: CircuitBreaker
              args:
                name: userServiceCB
                fallbackUri: forward:/fallback/user

        # Media Service Routes
        - id: media-service
          uri: lb://media-service
          predicates:
            - Path=/api/videos/**, /api/streaming/**
          filters:
            - JwtAuthenticationFilter
            - RewritePath=/api/(?<segment>.*), /${segment}

      # Global filters áp dụng cho tất cả routes
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin
        - AddResponseHeader=X-Response-Time, %{responseTime}ms
```

### 7.3. Kiến trúc Authentication (Xác thực)

KiteClass sử dụng **JWT (JSON Web Token)** kết hợp với **OAuth 2.0** để xác thực người dùng.

#### 7.3.1. Luồng xác thực (Authentication Flow)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        AUTHENTICATION FLOW                                  │
└────────────────────────────────────────────────────────────────────────────┘

┌─────────┐         ┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│ Client  │         │   Gateway   │         │ User Service │         │  Database   │
└────┬────┘         └──────┬──────┘         └──────┬───────┘         └──────┬──────┘
     │                     │                       │                        │
     │  1. POST /auth/login                        │                        │
     │  { email, password }                        │                        │
     │────────────────────▶│                       │                        │
     │                     │                       │                        │
     │                     │  2. Forward request   │                        │
     │                     │──────────────────────▶│                        │
     │                     │                       │                        │
     │                     │                       │  3. Verify credentials │
     │                     │                       │───────────────────────▶│
     │                     │                       │                        │
     │                     │                       │  4. User data + roles  │
     │                     │                       │◀───────────────────────│
     │                     │                       │                        │
     │                     │  5. Generate JWT      │                        │
     │                     │     (access_token +   │                        │
     │                     │      refresh_token)   │                        │
     │                     │◀──────────────────────│                        │
     │                     │                       │                        │
     │  6. Return tokens   │                       │                        │
     │  { access_token,    │                       │                        │
     │    refresh_token,   │                       │                        │
     │    expires_in }     │                       │                        │
     │◀────────────────────│                       │                        │
     │                     │                       │                        │
```

#### 7.3.2. Cấu trúc JWT Token

```json
// JWT Header
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "kiteclass-key-2024"
}

// JWT Payload
{
  "sub": "user-uuid-12345",              // Subject (User ID)
  "email": "teacher@example.com",
  "name": "Nguyễn Văn A",
  "roles": ["TEACHER", "CENTER_ADMIN"],  // User roles
  "permissions": [                        // Specific permissions
    "class:create",
    "class:read",
    "class:update",
    "attendance:manage",
    "grade:manage"
  ],
  "tenant_id": "center-uuid-789",        // Multi-tenant support
  "instance_id": "instance-001",         // KiteClass instance
  "iat": 1703145600,                     // Issued at
  "exp": 1703149200,                     // Expires (1 hour)
  "iss": "kiteclass-auth",               // Issuer
  "aud": "kiteclass-api"                 // Audience
}

// JWT Signature
RSASHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  privateKey
)
```

#### 7.3.3. Token Management

```java
@Service
public class TokenService {

    private static final long ACCESS_TOKEN_VALIDITY = 3600;       // 1 hour
    private static final long REFRESH_TOKEN_VALIDITY = 604800;    // 7 days

    @Value("${jwt.private-key}")
    private RSAPrivateKey privateKey;

    @Value("${jwt.public-key}")
    private RSAPublicKey publicKey;

    /**
     * Tạo Access Token chứa thông tin user và permissions
     */
    public String generateAccessToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getFullName())
            .claim("roles", user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList()))
            .claim("permissions", extractPermissions(user))
            .claim("tenant_id", user.getTenantId())
            .claim("instance_id", user.getInstanceId())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY * 1000))
            .setIssuer("kiteclass-auth")
            .setAudience("kiteclass-api")
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact();
    }

    /**
     * Tạo Refresh Token để làm mới Access Token
     */
    public String generateRefreshToken(User user) {
        String tokenId = UUID.randomUUID().toString();

        // Lưu refresh token vào Redis với TTL
        refreshTokenRepository.save(new RefreshToken(
            tokenId,
            user.getId(),
            REFRESH_TOKEN_VALIDITY
        ));

        return Jwts.builder()
            .setId(tokenId)
            .setSubject(user.getId().toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY * 1000))
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact();
    }

    /**
     * Xác thực và parse JWT Token
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Token đã hết hạn");
        } catch (JwtException e) {
            throw new InvalidTokenException("Token không hợp lệ");
        }
    }
}
```

#### 7.3.4. Gateway Authentication Filter

```java
@Component
public class JwtAuthenticationFilter implements GatewayFilter, Ordered {

    @Autowired
    private TokenService tokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Bỏ qua các endpoints không cần auth
        if (isPublicEndpoint(request.getPath().value())) {
            return chain.filter(exchange);
        }

        // Lấy token từ header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid Authorization header",
                          HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            Claims claims = tokenService.validateToken(token);

            // Thêm user info vào headers để các service downstream sử dụng
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Email", claims.get("email", String.class))
                .header("X-User-Roles", String.join(",", claims.get("roles", List.class)))
                .header("X-Tenant-Id", claims.get("tenant_id", String.class))
                .header("X-Instance-Id", claims.get("instance_id", String.class))
                .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (TokenExpiredException e) {
            return onError(exchange, "Token expired", HttpStatus.UNAUTHORIZED);
        } catch (InvalidTokenException e) {
            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/health") ||
               path.startsWith("/actuator");
    }

    @Override
    public int getOrder() {
        return -100; // Run early in the filter chain
    }
}
```

### 7.4. Kiến trúc Authorization (Phân quyền)

KiteClass sử dụng mô hình **RBAC (Role-Based Access Control)** kết hợp với **Permission-based Access Control**.

#### 7.4.1. Mô hình phân quyền

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        RBAC + PERMISSION MODEL                              │
└────────────────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────────────────────────────────────────────────┐
│    USER     │     │                      ROLES                              │
│             │     ├─────────────────────────────────────────────────────────┤
│  user_id    │────▶│  SUPER_ADMIN    │ Quản trị toàn hệ thống               │
│  email      │     │  CENTER_OWNER   │ Chủ trung tâm (tenant owner)         │
│  tenant_id  │     │  CENTER_ADMIN   │ Quản trị viên trung tâm              │
│             │     │  TEACHER        │ Giáo viên                             │
│             │     │  STUDENT        │ Học viên                              │
│             │     │  PARENT         │ Phụ huynh                             │
└─────────────┘     └─────────────────┬───────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PERMISSIONS                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  Resource     │  Actions                    │  Format                       │
├───────────────┼─────────────────────────────┼───────────────────────────────┤
│  course       │  create, read, update,      │  course:create                │
│               │  delete, publish            │  course:read                  │
│───────────────┼─────────────────────────────┼───────────────────────────────│
│  class        │  create, read, update,      │  class:create                 │
│               │  delete, manage_students    │  class:manage_students        │
│───────────────┼─────────────────────────────┼───────────────────────────────│
│  attendance   │  view, mark, edit           │  attendance:mark              │
│───────────────┼─────────────────────────────┼───────────────────────────────│
│  grade        │  view, create, edit         │  grade:create                 │
│───────────────┼─────────────────────────────┼───────────────────────────────│
│  assignment   │  create, read, grade,       │  assignment:create            │
│               │  submit                     │  assignment:grade             │
│───────────────┼─────────────────────────────┼───────────────────────────────│
│  video        │  upload, view, delete       │  video:upload                 │
│───────────────┼─────────────────────────────┼───────────────────────────────│
│  streaming    │  start, join, end           │  streaming:start              │
│───────────────┼─────────────────────────────┼───────────────────────────────│
│  billing      │  view, create, manage       │  billing:manage               │
│───────────────┼─────────────────────────────┼───────────────────────────────│
│  report       │  view, export               │  report:export                │
└───────────────┴─────────────────────────────┴───────────────────────────────┘
```

#### 7.4.2. Ma trận phân quyền Role - Permission

| Permission | SUPER_ADMIN | CENTER_OWNER | CENTER_ADMIN | TEACHER | STUDENT | PARENT |
|------------|:-----------:|:------------:|:------------:|:-------:|:-------:|:------:|
| **Course** |
| course:create | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| course:read | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| course:update | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| course:delete | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Class** |
| class:create | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| class:read | ✅ | ✅ | ✅ | ✅* | ✅* | ✅* |
| class:manage_students | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Attendance** |
| attendance:view | ✅ | ✅ | ✅ | ✅* | ✅* | ✅* |
| attendance:mark | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ |
| **Grade** |
| grade:view | ✅ | ✅ | ✅ | ✅* | ✅* | ✅* |
| grade:create | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ |
| **Billing** |
| billing:view | ✅ | ✅ | ✅ | ❌ | ✅* | ✅* |
| billing:manage | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Streaming** |
| streaming:start | ✅ | ✅ | ✅ | ✅* | ❌ | ❌ |
| streaming:join | ✅ | ✅ | ✅ | ✅* | ✅* | ✅* |

*\* Chỉ trong phạm vi lớp học/khóa học được phân công*

#### 7.4.3. Authorization Service Implementation

```java
@Service
public class AuthorizationService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * Kiểm tra user có permission cụ thể không
     */
    public boolean hasPermission(String userId, String permission) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(p -> p.getName().equals(permission));
    }

    /**
     * Kiểm tra user có quyền truy cập resource cụ thể không
     * (Kết hợp permission + scope)
     */
    public boolean canAccessResource(String userId, String resourceType,
                                     String resourceId, String action) {
        String permission = resourceType + ":" + action;

        // 1. Kiểm tra có permission không
        if (!hasPermission(userId, permission)) {
            return false;
        }

        // 2. Kiểm tra scope (chỉ áp dụng cho một số roles)
        User user = userRepository.findById(userId).orElseThrow();

        // SUPER_ADMIN, CENTER_OWNER, CENTER_ADMIN: full access trong tenant
        if (hasAnyRole(user, "SUPER_ADMIN", "CENTER_OWNER", "CENTER_ADMIN")) {
            return isSameTenant(user, resourceId, resourceType);
        }

        // TEACHER: chỉ access lớp được phân công
        if (hasRole(user, "TEACHER")) {
            return isTeacherOfResource(user, resourceId, resourceType);
        }

        // STUDENT: chỉ access lớp đã đăng ký
        if (hasRole(user, "STUDENT")) {
            return isStudentOfResource(user, resourceId, resourceType);
        }

        // PARENT: chỉ access thông tin con
        if (hasRole(user, "PARENT")) {
            return isParentOfStudentInResource(user, resourceId, resourceType);
        }

        return false;
    }

    /**
     * Kiểm tra teacher có được phân công vào resource không
     */
    private boolean isTeacherOfResource(User teacher, String resourceId,
                                        String resourceType) {
        switch (resourceType) {
            case "class":
                return classRepository.existsByIdAndTeacherId(
                    resourceId, teacher.getId());
            case "course":
                return courseRepository.existsByIdAndTeacherId(
                    resourceId, teacher.getId());
            case "attendance":
            case "grade":
            case "assignment":
                // Lấy class_id từ resource, kiểm tra teacher của class đó
                String classId = getClassIdFromResource(resourceType, resourceId);
                return classRepository.existsByIdAndTeacherId(
                    classId, teacher.getId());
            default:
                return false;
        }
    }
}
```

#### 7.4.4. Annotation-based Authorization

```java
// Custom annotation cho method-level security
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();  // e.g., "class:create"
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();  // e.g., {"TEACHER", "CENTER_ADMIN"}
}

// Aspect để xử lý authorization
@Aspect
@Component
public class AuthorizationAspect {

    @Autowired
    private AuthorizationService authService;

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint,
                                RequirePermission requirePermission) {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String permission = requirePermission.value();

        if (!authService.hasPermission(userId, permission)) {
            throw new AccessDeniedException(
                "Không có quyền: " + permission);
        }
    }

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        Authentication auth = SecurityContextHolder.getContext()
            .getAuthentication();

        boolean hasRequiredRole = Arrays.stream(requireRole.value())
            .anyMatch(role -> auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)));

        if (!hasRequiredRole) {
            throw new AccessDeniedException(
                "Yêu cầu role: " + Arrays.toString(requireRole.value()));
        }
    }
}

// Sử dụng trong Controller
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    @PostMapping
    @RequirePermission("class:create")
    public ResponseEntity<ClassDTO> createClass(@RequestBody CreateClassRequest request) {
        // Chỉ user có quyền class:create mới vào được đây
        return ResponseEntity.ok(classService.create(request));
    }

    @GetMapping("/{id}")
    @RequirePermission("class:read")
    public ResponseEntity<ClassDTO> getClass(@PathVariable String id) {
        // Thêm kiểm tra resource-level access
        String userId = getCurrentUserId();
        if (!authService.canAccessResource(userId, "class", id, "read")) {
            throw new AccessDeniedException("Không có quyền xem lớp học này");
        }
        return ResponseEntity.ok(classService.getById(id));
    }

    @PostMapping("/{id}/attendance")
    @RequireRole({"TEACHER", "CENTER_ADMIN"})
    @RequirePermission("attendance:mark")
    public ResponseEntity<Void> markAttendance(
            @PathVariable String id,
            @RequestBody MarkAttendanceRequest request) {
        // Chỉ TEACHER hoặc CENTER_ADMIN có quyền attendance:mark
        return ResponseEntity.ok().build();
    }
}
```

### 7.5. Multi-Tenant Authorization

Do KiteClass là nền tảng multi-tenant, cần đảm bảo **tenant isolation** (cô lập dữ liệu giữa các tenant).

```
┌────────────────────────────────────────────────────────────────────────────┐
│                      MULTI-TENANT AUTHORIZATION                             │
└────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────┐
                    │           REQUEST               │
                    │   Authorization: Bearer <JWT>   │
                    │   JWT contains: tenant_id       │
                    └───────────────┬─────────────────┘
                                    │
                                    ▼
                    ┌─────────────────────────────────┐
                    │       GATEWAY FILTER            │
                    │   Extract tenant_id from JWT    │
                    │   Set X-Tenant-Id header        │
                    └───────────────┬─────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                          CORE SERVICE                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                    TenantContext Filter                              │  │
│  │  • Đọc X-Tenant-Id từ header                                        │  │
│  │  • Set vào ThreadLocal TenantContext                                │  │
│  │  • Tự động apply tenant filter cho tất cả queries                   │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                    Hibernate Tenant Filter                           │  │
│  │  @FilterDef(name = "tenantFilter",                                  │  │
│  │            parameters = @ParamDef(name = "tenantId", type = "string")) │
│  │  @Filter(name = "tenantFilter",                                     │  │
│  │          condition = "tenant_id = :tenantId")                       │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Kết quả: SELECT * FROM classes WHERE tenant_id = 'center-123' AND ...    │
│           (tenant filter tự động được thêm vào mọi query)                  │
└───────────────────────────────────────────────────────────────────────────┘
```

```java
// Tenant Context - ThreadLocal storage
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}

// Filter để set tenant từ request
@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String tenantId = request.getHeader("X-Tenant-Id");

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

// Entity với tenant isolation
@Entity
@Table(name = "classes")
@FilterDef(name = "tenantFilter",
           parameters = @ParamDef(name = "tenantId", type = "string"))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ClassEntity {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    private String name;
    // ...

    @PrePersist
    public void prePersist() {
        // Tự động set tenant_id khi tạo mới
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenantId();
        }
    }
}

// Repository tự động apply tenant filter
@Repository
public class ClassRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<ClassEntity> findAll() {
        Session session = entityManager.unwrap(Session.class);

        // Enable tenant filter
        session.enableFilter("tenantFilter")
               .setParameter("tenantId", TenantContext.getTenantId());

        return session.createQuery("FROM ClassEntity", ClassEntity.class)
                      .getResultList();
        // Query thực tế: SELECT * FROM classes WHERE tenant_id = ?
    }
}
```

### 7.6. Security Best Practices

| Aspect | Implementation | Mô tả |
|--------|----------------|-------|
| **Token Storage** | HttpOnly Cookie | Không lưu token trong localStorage |
| **Token Rotation** | Refresh token rotation | Mỗi lần refresh, tạo refresh token mới |
| **Rate Limiting** | 100 req/min per user | Ngăn brute force attacks |
| **CORS** | Whitelist domains | Chỉ cho phép origins được đăng ký |
| **Password** | BCrypt với salt | Không lưu plain text password |
| **Session** | Stateless JWT | Không lưu session trên server |
| **Audit Log** | Log all auth events | Theo dõi login/logout/permission changes |
| **IP Blocking** | Fail2ban integration | Block IP sau 5 failed attempts |

### 7.7. OAuth 2.0 Integration (Social Login)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                      OAUTH 2.0 FLOW (Google Login)                          │
└────────────────────────────────────────────────────────────────────────────┘

┌─────────┐     ┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ Browser │     │  Frontend   │     │ User Service │     │   Google    │
└────┬────┘     └──────┬──────┘     └──────┬───────┘     └──────┬──────┘
     │                 │                   │                    │
     │ 1. Click "Login with Google"        │                    │
     │────────────────▶│                   │                    │
     │                 │                   │                    │
     │ 2. Redirect to Google OAuth         │                    │
     │◀────────────────│                   │                    │
     │                 │                   │                    │
     │ 3. Google login page                │                    │
     │─────────────────────────────────────────────────────────▶│
     │                 │                   │                    │
     │ 4. User authorizes                  │                    │
     │◀────────────────────────────────────────────────────────│
     │                 │                   │                    │
     │ 5. Redirect with auth code          │                    │
     │────────────────▶│                   │                    │
     │                 │                   │                    │
     │                 │ 6. Exchange code for token             │
     │                 │──────────────────▶│                    │
     │                 │                   │                    │
     │                 │                   │ 7. Verify with Google
     │                 │                   │───────────────────▶│
     │                 │                   │                    │
     │                 │                   │ 8. User info       │
     │                 │                   │◀───────────────────│
     │                 │                   │                    │
     │                 │ 9. Create/link user, return JWT        │
     │                 │◀──────────────────│                    │
     │                 │                   │                    │
     │ 10. Set JWT cookie                  │                    │
     │◀────────────────│                   │                    │
     │                 │                   │                    │
```

```java
@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam String code,
            HttpServletResponse response) {

        // 1. Exchange code for Google tokens
        GoogleTokenResponse googleTokens = googleOAuthService
            .exchangeCodeForTokens(code);

        // 2. Get user info from Google
        GoogleUserInfo googleUser = googleOAuthService
            .getUserInfo(googleTokens.getAccessToken());

        // 3. Find or create user in our system
        User user = userService.findOrCreateByGoogleId(
            googleUser.getId(),
            googleUser.getEmail(),
            googleUser.getName()
        );

        // 4. Generate our JWT tokens
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        // 5. Set tokens in HttpOnly cookies
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(3600)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // 6. Redirect to frontend
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, "/dashboard")
            .build();
    }
}
```

---

## PHẦN 8: KẾT LUẬN VÀ KHUYẾN NGHỊ

### 8.1. Tóm tắt đánh giá

Kiến trúc hiện tại (7 services):
- ✅ Đúng về lý thuyết microservices
- ❌ **OVER-ENGINEERING** cho quy mô platform giáo dục
- ❌ Chi phí và complexity cao không cần thiết
- ❌ Tight coupling tự nhiên giữa Main Class, CMC, Forum

### 8.2. Khuyến nghị

> **KHUYẾN NGHỊ: Áp dụng Phương án 2 (4 Services)**

| Service hiện tại | → | Service mới |
|------------------|---|-------------|
| Main Class + CMC + Forum | → | **Core Service** (Modular Monolith) |
| User Service | → | **User Service** (giữ nguyên) |
| Video + Streaming | → | **Media Service** |
| Frontend | → | **Frontend** (giữ nguyên) |

### 8.3. Lợi ích dự kiến

- ✅ Giảm 43% số containers (700 → 400 cho 100 instances)
- ✅ Giảm 60% network overhead giữa services
- ✅ Tiết kiệm ~$1,600/tháng chi phí infrastructure
- ✅ Đơn giản hóa deployment và monitoring
- ✅ Dễ debug hơn (ít cross-service calls)
- ✅ Vẫn giữ được tính modular trong code

### 8.4. Rủi ro

- ⚠️ Effort refactor: ~5-6 tuần
- ⚠️ Core Service trở nên lớn hơn
- ⚠️ Nếu cần scale module riêng sẽ khó hơn

### 8.5. Quyết định cuối cùng

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
*Cập nhật: 23/12/2025 - Bổ sung PHẦN 7: API Gateway và Authentication/Authorization*
