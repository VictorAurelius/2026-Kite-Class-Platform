# Module Authentication (Xác thực) - Gateway Service

**Phiên bản:** 2.0 (Tiếng Việt)
**Trạng thái:** ✅ ĐÃ TRIỂN KHAI (PR 1.4 - 2026-01-26)
**Cập nhật:** PR 1.5 - Tích hợp Email Service (2026-01-27)
**Nhánh:** feature/gateway
**Vị trí:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/module/auth/`

---

## 📝 Tổng Quan

Module Authentication cung cấp chức năng xác thực dựa trên JWT, quản lý token, và các tính năng bảo mật cho KiteClass Gateway.

### Tính năng chính

- ✅ **Xác thực JWT**: Access token (1 giờ) + Refresh token (7 ngày)
- ✅ **Quản lý phiên**: Lưu trữ refresh token trong database
- ✅ **Khóa tài khoản**: Tự động khóa sau 5 lần đăng nhập sai
- ✅ **Luân chuyển token**: Token rotation để tăng bảo mật
- ✅ **Reset mật khẩu**: Qua email với token có thời hạn (PR 1.5)
- ✅ **RBAC**: Role-Based Access Control với 5 roles hệ thống

### Bổ sung trong PR 1.5

- Password reset với email integration
- PasswordResetToken entity và repository
- Email service integration cho forgot-password flow

---

## 🏗️ Kiến Trúc

```
gateway/
├── security/                           # Các thành phần bảo mật
│   ├── jwt/
│   │   ├── JwtTokenProvider           # Tạo & xác thực JWT tokens
│   │   ├── JwtProperties              # Cấu hình JWT (secret, expiry)
│   │   └── TokenType                  # Enum: ACCESS, REFRESH
│   ├── UserPrincipal                  # Spring Security principal
│   └── SecurityContextRepository      # Load authentication từ JWT
│
├── filter/                             # Gateway filters
│   └── AuthenticationFilter           # Thêm headers cho downstream services
│
└── module/auth/                        # Auth module
    ├── entity/
    │   ├── RefreshToken               # Refresh tokens được lưu trong DB
    │   ├── PasswordResetToken         # Password reset tokens (PR 1.5)
    │   └── RolePermission             # Mapping role-permission
    │
    ├── repository/
    │   ├── RefreshTokenRepository
    │   ├── PasswordResetTokenRepository
    │   └── RolePermissionRepository
    │
    ├── dto/
    │   ├── LoginRequest/Response
    │   ├── RefreshTokenRequest
    │   └── ForgotPasswordRequest/ResetPasswordRequest
    │
    ├── service/
    │   └── AuthServiceImpl            # Logic login, logout, refresh, reset password
    │
    └── controller/
        └── AuthController             # 5 auth endpoints (+ forgot/reset password)
```

---

## 🔑 Cấu Trúc JWT Token

### Access Token (Token Truy Cập)

**Mục đích:** Xác thực các request đến API

**Thông tin:**
- **Type:** ACCESS
- **Thời hạn:** 1 giờ (3600000ms)
- **Thuật toán:** HS512 (HMAC-SHA512) với secret key (tối thiểu 512 bits)

**Claims (Thông tin trong token):**
```json
{
  "sub": "1",                           // User ID (Long)
  "email": "owner@kiteclass.local",     // User email
  "roles": ["OWNER", "ADMIN"],          // Danh sách mã roles (List<String>)
  "type": "ACCESS",                     // Loại token
  "iat": 1706371200,                    // Issued at timestamp (thời điểm tạo)
  "exp": 1706374800                     // Expiration timestamp (thời điểm hết hạn)
}
```

**Cách sử dụng:**
```bash
# Gửi trong header Authorization
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

---

### Refresh Token (Token Làm Mới)

**Mục đích:** Lấy access token mới khi token cũ hết hạn

**Thông tin:**
- **Type:** REFRESH
- **Thời hạn:** 7 ngày (604800000ms)
- **Lưu trữ:** Trong database (bảng `refresh_tokens`)

**Claims:**
```json
{
  "sub": "1",                           // User ID
  "type": "REFRESH",                    // Loại token
  "iat": 1706371200,                    // Issued at
  "exp": 1706976000                     // Expiration (7 ngày)
}
```

**Lý do lưu trong database:**
- Có thể thu hồi token (revoke) khi logout
- Kiểm soát phiên đăng nhập
- Token rotation (xóa token cũ khi tạo token mới)

---

## 🔄 Các Luồng Xác Thực

### 1️⃣ Luồng Đăng Nhập (Login Flow)

```
┌─────────┐               ┌─────────┐               ┌──────────┐
│ Client  │               │ Gateway │               │ Database │
└────┬────┘               └────┬────┘               └────┬─────┘
     │                         │                         │
     │─POST /auth/login───────>│                         │
     │  {email, password}      │                         │
     │                         │                         │
     │                         │──findByEmail()────────> │
     │                         │ <─User─────────────────│
     │                         │                         │
     │                         │──validatePassword()     │
     │                         │  (BCrypt compare)       │
     │                         │                         │
     │                         │──checkAccountStatus()   │
     │                         │  (ACTIVE? not deleted?) │
     │                         │                         │
     │                         │──checkLocked()          │
     │                         │  (lockedUntil check)    │
     │                         │                         │
     │                         │──generateTokens()       │
     │                         │  (Access + Refresh)     │
     │                         │                         │
     │                         │──saveRefreshToken()───> │
     │                         │──updateLastLogin()────> │
     │                         │──resetFailedAttempts()> │
     │                         │                         │
     │<─200 OK + tokens────────│                         │
     │  {accessToken,          │                         │
     │   refreshToken,         │                         │
     │   user: {id, email}}    │                         │
     │                         │                         │
```

**Chi tiết các bước:**

1. **Client gửi email + password**
   ```json
   POST /api/v1/auth/login
   {
     "email": "owner@kiteclass.local",
     "password": "Admin@123"
   }
   ```

2. **Gateway tìm User theo email**
   ```java
   User user = userRepository.findByEmailAndDeletedFalse(email)
       .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
   ```

3. **Xác thực password**
   ```java
   if (!passwordEncoder.matches(password, user.getPasswordHash())) {
       handleFailedLogin(user);  // Tăng failed_login_attempts
       throw new BadCredentialsException("Invalid credentials");
   }
   ```

4. **Kiểm tra trạng thái tài khoản**
   ```java
   if (!user.canLogin()) {
       throw new AccountStatusException("Account is locked or inactive");
   }
   ```

5. **Tạo tokens**
   ```java
   String accessToken = jwtTokenProvider.generateAccessToken(user);
   String refreshToken = jwtTokenProvider.generateRefreshToken(user);
   ```

6. **Lưu refresh token vào DB**
   ```java
   RefreshToken token = RefreshToken.builder()
       .token(refreshToken)
       .userId(user.getId())
       .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
       .build();
   refreshTokenRepository.save(token);
   ```

7. **Cập nhật thông tin user**
   ```java
   user.setLastLoginAt(Instant.now());
   user.setFailedLoginAttempts(0);
   user.setLockedUntil(null);
   userRepository.save(user);
   ```

8. **Trả về tokens cho client**
   ```json
   {
     "success": true,
     "data": {
       "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
       "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
       "tokenType": "Bearer",
       "expiresIn": 3600,
       "user": {
         "id": 1,
         "email": "owner@kiteclass.local",
         "name": "System Owner",
         "roles": ["OWNER"]
       }
     }
   }
   ```

---

### 2️⃣ Luồng Làm Mới Token (Refresh Token Flow)

```
┌─────────┐               ┌─────────┐               ┌──────────┐
│ Client  │               │ Gateway │               │ Database │
└────┬────┘               └────┬────┘               └────┬─────┘
     │                         │                         │
     │─POST /auth/refresh─────>│                         │
     │  {refreshToken}         │                         │
     │                         │                         │
     │                         │──findByToken()────────> │
     │                         │ <─RefreshToken─────────│
     │                         │                         │
     │                         │──checkExpiry()          │
     │                         │  (expires_at > now?)    │
     │                         │                         │
     │                         │──findUser()───────────> │
     │                         │ <─User─────────────────│
     │                         │                         │
     │                         │──checkUserStatus()      │
     │                         │  (ACTIVE? not deleted?) │
     │                         │                         │
     │                         │──deleteOldToken()─────> │
     │                         │  (Token rotation)       │
     │                         │                         │
     │                         │──generateNewTokens()    │
     │                         │──saveNewRefreshToken()>│
     │                         │                         │
     │<─200 OK + new tokens────│                         │
     │  {accessToken,          │                         │
     │   refreshToken}         │                         │
     │                         │                         │
```

**Lý do Token Rotation:**
- **Bảo mật:** Nếu refresh token bị đánh cắp, nó chỉ dùng được 1 lần
- **Phát hiện tấn công:** Nếu token cũ được dùng lại → biết có vấn đề
- **Giới hạn phiên:** Mỗi lần refresh tạo token mới với thời hạn mới

---

### 3️⃣ Luồng Truy Cập API Được Bảo Vệ

```
┌─────────┐          ┌─────────┐          ┌────────────┐
│ Client  │          │ Gateway │          │ Core Svc   │
└────┬────┘          └────┬────┘          └─────┬──────┘
     │                    │                      │
     │─GET /api/v1/students│                     │
     │  Authorization:     │                     │
     │  Bearer <token>     │                     │
     │                    │                      │
     │                    │──validateJWT()       │
     │                    │  (signature, expiry) │
     │                    │                      │
     │                    │──extractClaims()     │
     │                    │  (userId, roles)     │
     │                    │                      │
     │                    │──addHeaders()        │
     │                    │  X-User-Id: 1        │
     │                    │  X-User-Roles: OWNER │
     │                    │                      │
     │                    │──forward request────>│
     │                    │                      │
     │                    │                      │──processRequest()
     │                    │                      │  (use headers)
     │                    │                      │
     │                    │<─response────────────│
     │<─200 OK + data─────│                      │
     │                    │                      │
```

**Headers được thêm bởi AuthenticationFilter:**
- `X-User-Id`: ID của user đang đăng nhập
- `X-User-Roles`: Danh sách roles (cách nhau bằng dấu phẩy)

**Ví dụ sử dụng trong Core Service:**
```java
@GetMapping("/students")
public Mono<List<StudentResponse>> getStudents(
    @RequestHeader("X-User-Id") Long userId,
    @RequestHeader("X-User-Roles") String roles
) {
    // Kiểm tra quyền
    if (!roles.contains("ADMIN") && !roles.contains("OWNER")) {
        throw new ForbiddenException("Insufficient permissions");
    }

    // Xử lý logic
    return studentService.getAllStudents();
}
```

---

## 🛡️ Các Tính Năng Bảo Mật

### 1. Khóa Tài Khoản (Account Locking)

**Kích hoạt:** 5 lần đăng nhập sai liên tiếp

**Thời gian khóa:** 30 phút

**Cách hoạt động:**
```java
// Constants
MAX_FAILED_ATTEMPTS = 5
LOCK_DURATION_MINUTES = 30

// Khi đăng nhập sai
void handleFailedLogin(User user) {
    user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

    if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
        user.setLockedUntil(
            Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES)
        );

        // Gửi email thông báo (PR 1.5)
        emailService.sendAccountLockedEmail(
            user.getEmail(),
            user.getName(),
            LOCK_DURATION_MINUTES
        );
    }

    userRepository.save(user);
}

// Khi đăng nhập thành công
void handleSuccessfulLogin(User user) {
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    user.setLastLoginAt(Instant.now());
    userRepository.save(user);
}

// Kiểm tra khóa
boolean isLocked() {
    return lockedUntil != null && lockedUntil.isAfter(Instant.now());
}
```

**Ví dụ thực tế:**
```
10:00 - Đăng nhập sai lần 1 (failed_login_attempts = 1)
10:01 - Đăng nhập sai lần 2 (failed_login_attempts = 2)
10:02 - Đăng nhập sai lần 3 (failed_login_attempts = 3)
10:03 - Đăng nhập sai lần 4 (failed_login_attempts = 4)
10:04 - Đăng nhập sai lần 5 (failed_login_attempts = 5)
       → locked_until = 10:34 (30 phút sau)
       → Email thông báo được gửi

10:05 - Thử đăng nhập → Bị từ chối ("Account is locked")
10:20 - Thử đăng nhập → Vẫn bị từ chối
10:35 - Thử đăng nhập → OK (đã hết thời gian khóa)
       → failed_login_attempts = 0, locked_until = null
```

---

### 2. Luân Chuyển Token (Token Rotation)

**Mục đích:** Tăng bảo mật, phát hiện tấn công

**Cách hoạt động:**
1. Khi client gửi refresh token
2. Gateway xóa token cũ khỏi database
3. Tạo cặp token mới (access + refresh)
4. Lưu refresh token mới vào database
5. Trả về cặp token mới cho client

**Lợi ích:**
- **Giới hạn sử dụng:** Mỗi refresh token chỉ dùng được 1 lần
- **Phát hiện tấn công:** Nếu token cũ được dùng lại → biết có vấn đề
- **An toàn hơn:** Giảm thời gian token có thể bị lợi dụng

**Implementation:**
```java
public Mono<LoginResponse> refreshToken(String refreshToken) {
    // Tìm token trong DB
    return refreshTokenRepository.findByToken(refreshToken)
        .switchIfEmpty(Mono.error(new InvalidTokenException()))
        .flatMap(token -> {
            // Kiểm tra hết hạn
            if (token.isExpired()) {
                return refreshTokenRepository.delete(token)
                    .then(Mono.error(new ExpiredTokenException()));
            }

            // Lấy user
            return userRepository.findById(token.getUserId())
                .flatMap(user -> {
                    // Kiểm tra user status
                    if (!user.canLogin()) {
                        return Mono.error(new AccountStatusException());
                    }

                    // XÓA TOKEN CŨ (Token Rotation)
                    return refreshTokenRepository.delete(token)
                        .then(Mono.defer(() -> {
                            // Tạo tokens mới
                            String newAccessToken = generateAccessToken(user);
                            String newRefreshToken = generateRefreshToken(user);

                            // Lưu refresh token mới
                            RefreshToken newToken = RefreshToken.builder()
                                .token(newRefreshToken)
                                .userId(user.getId())
                                .expiresAt(calculateExpiry())
                                .build();

                            return refreshTokenRepository.save(newToken)
                                .thenReturn(
                                    LoginResponse.from(newAccessToken, newRefreshToken, user)
                                );
                        }));
                });
        });
}
```

---

### 3. Xác Thực Token (Token Validation)

**Các kiểm tra:**

1. **Chữ ký (Signature Verification)**
   ```java
   // Xác thực HMAC-SHA512 signature
   if (!isSignatureValid(token, secretKey)) {
       throw new InvalidTokenException("Invalid token signature");
   }
   ```

2. **Thời hạn (Expiration Check)**
   ```java
   if (claims.getExpiration().before(new Date())) {
       throw new ExpiredTokenException("Token has expired");
   }
   ```

3. **Loại token (Token Type)**
   ```java
   String tokenType = claims.get("type", String.class);
   if (!expectedType.equals(tokenType)) {
       throw new InvalidTokenException("Wrong token type");
   }
   ```

4. **Tính toàn vẹn claims (Claims Integrity)**
   ```java
   // Kiểm tra các claims bắt buộc
   if (claims.getSubject() == null) {
       throw new InvalidTokenException("Missing subject claim");
   }
   ```

---

### 4. RBAC (Role-Based Access Control)

**5 Roles hệ thống:**

| Role | Tên tiếng Việt | Mô tả | Quyền |
|------|---------------|-------|-------|
| **OWNER** | Chủ trung tâm | Quyền cao nhất | Toàn bộ hệ thống |
| **ADMIN** | Quản trị viên | Quản lý hệ thống | Users, Classes, Billing, Reports |
| **TEACHER** | Giáo viên | Quản lý lớp học | Assigned classes, Attendance, Grades |
| **STAFF** | Nhân viên | Quyền hạn chế | Based on assigned permissions |
| **PARENT** | Phụ huynh | Xem thông tin con | Children info, Invoices, Attendance |

**Cấu hình bảo mật (SecurityConfig):**
```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(exchanges -> exchanges
            // Public endpoints
            .pathMatchers("/api/v1/auth/**").permitAll()

            // User management
            .pathMatchers(HttpMethod.GET, "/api/v1/users/**")
                .hasAnyRole("ADMIN", "OWNER", "STAFF")
            .pathMatchers(HttpMethod.POST, "/api/v1/users/**")
                .hasAnyRole("ADMIN", "OWNER")
            .pathMatchers(HttpMethod.DELETE, "/api/v1/users/**")
                .hasRole("OWNER")

            // Student management
            .pathMatchers("/api/v1/students/**")
                .hasAnyRole("ADMIN", "OWNER", "TEACHER", "STAFF")

            // Billing (Invoices)
            .pathMatchers("/api/v1/invoices/**")
                .hasAnyRole("ADMIN", "OWNER", "STAFF")

            // Parent portal
            .pathMatchers("/api/v1/parent/**")
                .hasRole("PARENT")

            // All other requests must be authenticated
            .anyExchange().authenticated()
        )
        .build();
}
```

**Kiểm tra quyền trong code:**
```java
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public Mono<StudentResponse> createStudent(CreateStudentRequest request) {
    // Only ADMIN or OWNER can create students
}

@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'TEACHER')")
public Mono<List<StudentResponse>> listStudents() {
    // ADMIN, OWNER, or TEACHER can list students
}
```

---

## 🚪 API Endpoints

### 1. POST /api/v1/auth/login (Đăng Nhập)

**Authentication:** None (public)

**Request:**
```json
{
  "email": "owner@kiteclass.local",
  "password": "Admin@123"
}
```

**Response thành công (200 OK):**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJvd25lckBraXRlY2xhc3MubG9jYWwiLCJyb2xlcyI6WyJPV05FUiJdLCJ0eXBlIjoiQUNDRVNTIiwiaWF0IjoxNzA2MzcxMjAwLCJleHAiOjE3MDYzNzQ4MDB9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwidHlwZSI6IlJFRlJFU0giLCJpYXQiOjE3MDYzNzEyMDAsImV4cCI6MTcwNjk3NjAwMH0...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "owner@kiteclass.local",
      "name": "System Owner",
      "roles": ["OWNER"]
    }
  }
}
```

**Errors:**

| Status Code | Error Code | Mô tả |
|------------|-----------|-------|
| 401 | INVALID_CREDENTIALS | Email hoặc password không đúng |
| 403 | ACCOUNT_LOCKED | Tài khoản bị khóa (5 lần sai) |
| 403 | ACCOUNT_INACTIVE | Tài khoản chưa kích hoạt (status != ACTIVE) |

**Ví dụ lỗi:**
```json
{
  "success": false,
  "code": "ACCOUNT_LOCKED",
  "message": "Tài khoản bị khóa đến 14:30 do đăng nhập sai 5 lần",
  "timestamp": "2026-01-27T14:00:00Z"
}
```

---

### 2. POST /api/v1/auth/refresh (Làm Mới Token)

**Authentication:** None (public)

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response thành công (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",  // Token MỚI
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...", // Token MỚI
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "owner@kiteclass.local",
      "name": "System Owner",
      "roles": ["OWNER"]
    }
  }
}
```

**Errors:**
- 401: Token không hợp lệ hoặc đã hết hạn
- 401: Token đã được sử dụng (token rotation)

---

### 3. POST /api/v1/auth/logout (Đăng Xuất)

**Authentication:** None (public, nhưng cần refresh token)

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response:** 204 No Content

**Tác dụng:**
- Xóa refresh token khỏi database
- Access token vẫn còn valid cho đến khi hết hạn (1 giờ)
- Client nên xóa cả 2 tokens khỏi local storage

---

### 4. POST /api/v1/auth/forgot-password (Quên Mật Khẩu)

**Authentication:** None (public)

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu"
}
```

**Lưu ý bảo mật:**
- **Luôn trả về success** (không tiết lộ email có tồn tại hay không)
- Chỉ gửi email nếu user tồn tại và ACTIVE
- Token có thời hạn 1 giờ

**Luồng hoạt động:**
1. User nhập email
2. Hệ thống tìm user theo email
3. Nếu tìm thấy và status = ACTIVE:
   - Tạo reset token (UUID)
   - Lưu vào bảng `password_reset_tokens`
   - Gửi email với link reset
4. Trả về success (dù email có tồn tại hay không)

---

### 5. POST /api/v1/auth/reset-password (Đặt Lại Mật Khẩu)

**Authentication:** None (public, nhưng cần reset token)

**Request:**
```json
{
  "token": "uuid-token-from-email",
  "newPassword": "NewPassword@456"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Đặt lại mật khẩu thành công"
}
```

**Errors:**
- 400: Token không hợp lệ
- 400: Token đã hết hạn (1 giờ)
- 400: Token đã được sử dụng

**Tác dụng:**
1. Xác thực token
2. Đổi password mới (BCrypt hash)
3. Xóa tất cả refresh tokens (force logout all devices)
4. Đánh dấu token là đã sử dụng

---

## ⚙️ Cấu Hình

### application.yml

```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-super-secret-key-min-512-bits-long-for-hs512-algorithm-security}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:3600000}      # 1 giờ
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 ngày

# Email Configuration (PR 1.5)
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

email:
  from: ${EMAIL_FROM:KiteClass <noreply@kiteclass.com>}
  base-url: ${APP_BASE_URL:http://localhost:3000}
  reset-token-expiration: ${EMAIL_RESET_TOKEN_EXPIRATION:3600000}  # 1 giờ

# Security
spring:
  security:
    user:
      name: admin  # Bị bỏ qua (dùng JWT-based auth)
      password: admin
```

### Biến môi trường (Production)

```bash
# BẮT BUỘC - Phải đổi secret mặc định
export JWT_SECRET="production-secret-key-minimum-512-bits-long-change-this-in-production"

# TÙY CHỌN - Điều chỉnh thời hạn token
export JWT_ACCESS_EXPIRATION=7200000      # 2 giờ
export JWT_REFRESH_EXPIRATION=1209600000  # 14 ngày

# Email (PR 1.5)
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"  # Gmail App Password (16 ký tự)
export EMAIL_FROM="KiteClass <noreply@kiteclass.com>"
export APP_BASE_URL="https://kiteclass.com"
```

**⚠️ Cảnh báo bảo mật:**
- Secret mặc định CHỈ dùng cho development
- **BẮT BUỘC** phải set `JWT_SECRET` trong production
- Secret phải ≥ 512 bits (64 ký tự) cho HS512

---

## 💾 Database Schema

### Bảng `refresh_tokens`

**Mục đích:** Lưu trữ refresh tokens

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,         -- JWT refresh token
    user_id BIGINT NOT NULL,                    -- User sở hữu token
    expires_at TIMESTAMP NOT NULL,              -- Thời điểm hết hạn
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

**Ví dụ dữ liệu:**
```
id | token                  | user_id | expires_at           | created_at
---|------------------------|---------|---------------------|---------------------
1  | eyJhbGciOiJIUzUxMiJ9... | 1       | 2026-02-03 10:00:00 | 2026-01-27 10:00:00
2  | eyJhbGciOiJIUzUxMiJ9... | 2       | 2026-02-03 11:30:00 | 2026-01-27 11:30:00
```

---

### Bảng `password_reset_tokens` (PR 1.5)

**Mục đích:** Lưu trữ tokens để reset password

```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,         -- UUID reset token
    user_id BIGINT NOT NULL,                    -- User yêu cầu reset
    expires_at TIMESTAMP NOT NULL,              -- Hết hạn sau 1 giờ
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,                     -- Null = chưa dùng

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_used_at ON password_reset_tokens(used_at);
```

**Ví dụ dữ liệu:**
```
id | token                                | user_id | expires_at           | used_at
---|--------------------------------------|---------|---------------------|---------------------
1  | a3f5c9d2-1234-5678-9abc-def123456789 | 3       | 2026-01-27 15:00:00 | 2026-01-27 14:30:00
2  | b7e2a1c8-5678-1234-abcd-123456789def | 4       | 2026-01-27 16:00:00 | NULL (chưa dùng)
```

---

### Bảng `role_permissions`

**Mục đích:** Mapping giữa roles và permissions

```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);
```

---

## 🔗 Mối Quan Hệ Giữa User (Gateway) và Business Entities (Core)

### ⚠️ VẤN ĐỀ HIỆN TẠI: Chưa Có Thiết Kế Rõ Ràng

**Hiện trạng:**
- **Gateway Service**: Có `User` entity với roles (OWNER, ADMIN, TEACHER, STAFF, PARENT)
- **Core Service**: Có `Student`, `Teacher`, `Parent` entities (business logic)
- **Vấn đề**: **KHÔNG CÓ liên kết** giữa User và Student/Teacher/Parent

**Câu hỏi cần trả lời:**
1. Làm sao một **Student** login vào hệ thống?
2. Làm sao một **Teacher** login và quản lý lớp của mình?
3. Làm sao một **Parent** login và xem thông tin con?
4. Làm sao phân biệt User là Student, Teacher hay Parent?

---

### 🏗️ Các Giải Pháp Thiết Kế

#### Giải Pháp 1: User Có Type + Reference ID ⭐ (Đề xuất)

**Thiết kế:**

**User entity (Gateway):**
```java
@Entity
@Table("users")
public class User {
    @Id
    private Long id;

    private String email;
    private String passwordHash;
    private String name;

    // THÊM CÁC FIELD NÀY
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 20)
    private UserType userType;  // STUDENT, TEACHER, PARENT, STAFF, ADMIN

    @Column(name = "reference_id")
    private Long referenceId;    // ID của Student/Teacher/Parent trong Core

    // ... other fields
}

public enum UserType {
    ADMIN,      // Admin/Owner - không có referenceId
    STAFF,      // Nhân viên - không có referenceId
    TEACHER,    // Giáo viên - referenceId = teacherId trong Core
    PARENT,     // Phụ huynh - referenceId = parentId trong Core
    STUDENT     // Học viên - referenceId = studentId trong Core
}
```

**Student entity (Core) - KHÔNG THAY ĐỔI:**
```java
@Entity
public class Student {
    @Id
    private Long id;

    private String name;
    private String email;
    private String phone;
    // ... no userId field needed
}
```

**Luồng tạo Student có login:**
```java
// 1. Tạo User trong Gateway (authentication)
User user = User.builder()
    .email("student@example.com")
    .passwordHash(bcrypt("password"))
    .name("Nguyễn Văn An")
    .userType(UserType.STUDENT)
    .status(UserStatus.ACTIVE)
    .build();
User savedUser = userRepository.save(user);  // Gateway DB

// 2. Tạo Student trong Core (business logic)
Student student = Student.builder()
    .name("Nguyễn Văn An")
    .email("student@example.com")
    .phone("0912345678")
    .status(StudentStatus.ACTIVE)
    .build();
Student savedStudent = studentRepository.save(student);  // Core DB

// 3. Cập nhật User với referenceId
savedUser.setReferenceId(savedStudent.getId());
userRepository.save(savedUser);  // Gateway DB
```

**Khi login, lấy thông tin Student:**
```java
// User login
User user = authenticate(email, password);

if (user.getUserType() == UserType.STUDENT) {
    // Call Core Service để lấy Student
    Student student = coreService.getStudentById(user.getReferenceId());

    // Trả về combined info
    return LoginResponse.builder()
        .user(user)
        .student(student)
        .build();
}
```

**Ưu điểm:**
- ✅ Tách biệt authentication (Gateway) và business logic (Core)
- ✅ User có thể login với email/password
- ✅ Dễ xác định loại user (student, teacher, parent)
- ✅ Không cần thay đổi Student/Teacher/Parent entities

**Nhược điểm:**
- ❌ Cần sync 2 databases (Gateway + Core)
- ❌ Phức tạp hơn khi tạo user mới
- ❌ Không có foreign key constraint (distributed system)

---

#### Giải Pháp 2: Student/Teacher/Parent Có userId

**Thiết kế:**

**Student entity (Core) - THÊM userId:**
```java
@Entity
public class Student {
    @Id
    private Long id;

    // THÊM FIELD NÀY
    @Column(name = "user_id", unique = true)
    private Long userId;  // Link đến User trong Gateway

    private String name;
    private String email;
    // ... other fields
}
```

**Teacher entity (Core):**
```java
@Entity
public class Teacher {
    @Id
    private Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    private String name;
    // ...
}
```

**Luồng tạo Student có login:**
```java
// 1. Tạo User trong Gateway
User user = User.builder()
    .email("student@example.com")
    .passwordHash(bcrypt("password"))
    .name("Nguyễn Văn An")
    .build();
User savedUser = userRepository.save(user);

// 2. Tạo Student trong Core với userId
Student student = Student.builder()
    .userId(savedUser.getId())  // Link đến User
    .name("Nguyễn Văn An")
    .email("student@example.com")
    .build();
studentRepository.save(student);
```

**Khi login:**
```java
// User login
User user = authenticate(email, password);

// Tìm Student bằng userId
Student student = studentRepository.findByUserId(user.getId());

return LoginResponse.builder()
    .user(user)
    .student(student)
    .build();
```

**Ưu điểm:**
- ✅ Rõ ràng: Student có userId để login
- ✅ Dễ truy vấn: findByUserId()

**Nhược điểm:**
- ❌ Core service phụ thuộc vào Gateway (userId reference)
- ❌ Không có foreign key constraint (distributed)
- ❌ Nếu xóa User, Student vẫn còn (orphan data)

---

#### Giải Pháp 3: Link Qua Email (Loose Coupling)

**Thiết kế:**

**KHÔNG thêm field mới**, chỉ dùng email để link:
```java
// User (Gateway)
User user = User.builder()
    .email("student@example.com")
    .build();

// Student (Core)
Student student = Student.builder()
    .email("student@example.com")  // Same email
    .build();
```

**Khi login:**
```java
// User login
User user = authenticate(email, password);

// Tìm Student bằng email
Student student = studentRepository.findByEmailAndDeletedFalse(user.getEmail());

if (student != null) {
    return LoginResponse.builder()
        .user(user)
        .student(student)
        .build();
}
```

**Ưu điểm:**
- ✅ Đơn giản nhất
- ✅ Không cần thay đổi entities
- ✅ Loose coupling (Gateway và Core độc lập)

**Nhược điểm:**
- ❌ Không rõ ràng: Student này có login hay không?
- ❌ Nếu đổi email ở User, phải đổi ở Student (sync issue)
- ❌ Không biết User này là Student/Teacher/Parent

---

#### Giải Pháp 4: Chỉ Admin/Teacher/Staff Login

**Thiết kế:**

**Student KHÔNG CÓ login** - chỉ là data

**Chỉ có User accounts cho:**
- OWNER (chủ trung tâm)
- ADMIN (quản trị viên)
- TEACHER (giáo viên)
- STAFF (nhân viên)
- PARENT (phụ huynh)

**Student không login**, chỉ được quản lý bởi các user trên.

**Teacher có User account:**
```java
// User (Gateway) - Teacher account
User teacherUser = User.builder()
    .email("teacher@example.com")
    .passwordHash(bcrypt("password"))
    .name("Nguyễn Văn Giáo")
    .build();

// Teacher (Core) - Business entity
Teacher teacher = Teacher.builder()
    .userId(teacherUser.getId())
    .name("Nguyễn Văn Giáo")
    .build();
```

**Ưu điểm:**
- ✅ Đơn giản: Student không cần login
- ✅ Phù hợp với trung tâm nhỏ

**Nhược điểm:**
- ❌ Student không thể tự xem thông tin
- ❌ Không có student portal

---

### 📊 So Sánh Các Giải Pháp

| Tiêu chí | Giải pháp 1<br/>(UserType + ReferenceId) | Giải pháp 2<br/>(Student.userId) | Giải pháp 3<br/>(Link qua email) | Giải pháp 4<br/>(Student không login) |
|----------|------------------------------------------|-----------------------------------|----------------------------------|--------------------------------------|
| **Độ phức tạp** | Trung bình | Trung bình | Thấp | Thấp |
| **Tách biệt services** | Tốt | Trung bình | Tốt | Tốt |
| **Rõ ràng** | Tốt | Tốt | Kém | Tốt |
| **Student login** | ✅ Có | ✅ Có | ✅ Có | ❌ Không |
| **Sync 2 DBs** | ⚠️ Cần sync | ⚠️ Cần sync | ⚠️ Cần sync (email) | ⚠️ Cần sync |
| **Foreign key** | ❌ Không | ❌ Không | ❌ Không | ❌ Không |
| **Khuyến nghị** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ (nhỏ) |

---

### 💡 Khuyến Nghị Triển Khai

**Chọn Giải Pháp 1: UserType + ReferenceId**

**Lý do:**
- ✅ Rõ ràng, dễ hiểu
- ✅ Tách biệt services (Gateway vs Core)
- ✅ Linh hoạt mở rộng
- ✅ Student/Teacher/Parent đều có thể login
- ✅ Phù hợp với kiến trúc microservices

**Implementation steps:**

**Bước 1: Thêm UserType và referenceId vào User (Gateway)**

Migration:
```sql
-- V6__add_user_type_and_reference_id.sql
ALTER TABLE users
    ADD COLUMN user_type VARCHAR(20) DEFAULT 'ADMIN',
    ADD COLUMN reference_id BIGINT NULL;

CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_reference_id ON users(reference_id);

COMMENT ON COLUMN users.user_type IS 'Type of user: ADMIN, STAFF, TEACHER, PARENT, STUDENT';
COMMENT ON COLUMN users.reference_id IS 'ID of related entity in Core service (Student/Teacher/Parent ID)';
```

**Bước 2: Update User entity (Gateway)**
```java
@Entity
@Table("users")
public class User {
    // ... existing fields

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 20)
    private UserType userType = UserType.ADMIN;

    @Column(name = "reference_id")
    private Long referenceId;

    public boolean isStudent() {
        return userType == UserType.STUDENT;
    }

    public boolean isTeacher() {
        return userType == UserType.TEACHER;
    }

    public boolean isParent() {
        return userType == UserType.PARENT;
    }
}
```

**Bước 3: Create service để tạo Student với User account**
```java
@Service
public class StudentAccountService {

    private final UserService userService;
    private final StudentService studentService;

    @Transactional
    public StudentAccountResponse createStudentWithAccount(
        CreateStudentAccountRequest request
    ) {
        // 1. Create User in Gateway
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .phone(request.getPhone())
            .userType(UserType.STUDENT)
            .status(UserStatus.ACTIVE)
            .build();
        User savedUser = userService.createUser(user);

        // 2. Create Student in Core (via REST call)
        CreateStudentRequest studentReq = CreateStudentRequest.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .dateOfBirth(request.getDateOfBirth())
            .status(StudentStatus.ACTIVE)
            .build();
        StudentResponse student = coreServiceClient.createStudent(studentReq);

        // 3. Update User with referenceId
        savedUser.setReferenceId(student.getId());
        userService.updateUser(savedUser);

        return StudentAccountResponse.builder()
            .userId(savedUser.getId())
            .studentId(student.getId())
            .email(savedUser.getEmail())
            .name(savedUser.getName())
            .build();
    }
}
```

**Bước 4: Update Login Response**
```java
@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;

    // User info
    private Long userId;
    private String email;
    private String name;
    private List<String> roles;

    // NEW: Business entity info
    private UserType userType;
    private Long referenceId;

    // Optional: Embedded student/teacher/parent info
    private Object profile;  // StudentResponse or TeacherResponse or ParentResponse
}
```

---

## 🧪 Testing

### Unit Tests (30+ tests)

**JwtTokenProviderTest:**
- Token generation (access, refresh)
- Token validation
- Claims extraction
- Expiration handling

**AuthServiceTest:**
- Login success/failure
- Account locking
- Refresh token flow
- Logout
- Password reset flow

**AuthControllerTest:**
- All endpoints
- Validation errors
- HTTP status codes

### Manual Testing

```bash
# Start application
./mvnw spring-boot:run

# Run automated test script
./test-auth-flow.sh
```

---

## 👤 Tài Khoản Mặc Định

**Owner Account:**
- Email: `owner@kiteclass.local`
- Password: `Admin@123`
- Roles: OWNER (quyền cao nhất)
- Được tạo bởi: V4 database migration

---

## 🔧 Gateway Filter Cho Downstream Services

**AuthenticationFilter** xác thực JWT và thêm headers:

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: core-students
          uri: ${CORE_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/v1/students/**
          filters:
            - AuthenticationFilter  # <-- Filter này
```

**Headers được thêm:**
- `X-User-Id`: User ID từ JWT (Long)
- `X-User-Roles`: Danh sách role codes (String, cách nhau bằng dấu phẩy)

**Sử dụng trong Core Service:**
```java
@GetMapping("/students")
public Mono<List<StudentResponse>> getStudents(
    @RequestHeader("X-User-Id") Long userId,
    @RequestHeader("X-User-Roles") String roles
) {
    // Sử dụng userId và roles để authorization
    if (!roles.contains("ADMIN") && !roles.contains("OWNER")) {
        throw new ForbiddenException("Không đủ quyền");
    }

    return studentService.getAllStudents();
}
```

---

## 🐛 Các Vấn Đề Thường Gặp & Giải Pháp

### 1. "JAVA_HOME not defined"
```bash
./setup-java.sh
source ~/.bashrc
```

### 2. "JWT secret too short"
```bash
export JWT_SECRET="your-secret-key-must-be-at-least-512-bits-64-chars-long-here"
```

### 3. "Account locked"
Đợi 30 phút hoặc reset thủ công trong database:
```sql
UPDATE users
SET failed_login_attempts = 0, locked_until = NULL
WHERE email = 'user@example.com';
```

### 4. "Refresh token invalid"
**Nguyên nhân:**
- Token đã hết hạn (7 ngày)
- Token đã được sử dụng (token rotation)
- Token đã bị xóa (logout)

**Giải pháp:**
- Đăng nhập lại để lấy token mới

### 5. "Email not sending" (PR 1.5)
**Kiểm tra:**
```bash
# Check SMTP credentials
echo $MAIL_USERNAME
echo $MAIL_PASSWORD

# Test connection
telnet smtp.gmail.com 587
```

**Gmail setup:**
1. Enable 2-Factor Authentication
2. Generate App Password (16 characters)
3. Use App Password (không phải password thông thường)

---

## 🚀 Tính Năng Tương Lai

- [ ] Email verification for new users
- [ ] Token blacklist (logout trước khi hết hạn)
- [ ] Rate limiting per user
- [ ] Permission-based access control (chi tiết hơn roles)
- [ ] OAuth2 integration (Google, Facebook)
- [ ] Two-factor authentication (2FA)
- [ ] Session management (quản lý nhiều devices)
- [ ] **Triển khai đầy đủ User ↔ Student/Teacher/Parent linking** ⚠️ (quan trọng)

---

## 📚 Tài Liệu Liên Quan

**Trong project:**
- [Email Service](email-service.md) - Password reset email integration (PR 1.5)
- [Student Module](student-module.md) - Student business logic (Core)
- [Testing Guide](testing-guide.md) - Unit & integration test patterns
- [API Design](api-design.md) - REST endpoint conventions

**Code:**
- `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/module/auth/`
- `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/security/`

---

**Cập nhật:** 2026-01-27 (PR 1.5)
**Tác giả:** KiteClass Team (VictorAurelius + Claude Sonnet 4.5)
**Phiên bản:** 2.0 (Tiếng Việt)

