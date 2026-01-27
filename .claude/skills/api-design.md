# Skill: API Design

Thiết kế API cho KiteClass Platform V3.1.

## Mô tả

Tài liệu định nghĩa tất cả REST API endpoints, bao gồm:
- URL patterns và HTTP methods
- Request/Response DTOs
- Error codes và messages
- Authentication/Authorization
- Pagination và filtering

## Trigger phrases

- "thiết kế api"
- "api endpoints"
- "request response"
- "error codes"
- "dto schema"

## API Conventions

### Base URLs
| Service | URL | Mô tả |
|---------|-----|-------|
| Gateway | `/api/v1/*` | Entry point cho tất cả requests |
| Auth | `/api/v1/auth/*` | Authentication endpoints |
| Core | `/api/v1/*` | Core business logic |
| Engagement | `/api/v1/gamification/*`, `/api/v1/forum/*` | Optional features |

### HTTP Methods
| Method | Mục đích | Idempotent |
|--------|----------|------------|
| GET | Lấy resource | ✅ |
| POST | Tạo resource mới | ❌ |
| PUT | Cập nhật toàn bộ resource | ✅ |
| PATCH | Cập nhật một phần resource | ✅ |
| DELETE | Xóa resource (soft delete) | ✅ |

### URL Naming
```
# Collection
GET    /api/v1/students          # Danh sách
POST   /api/v1/students          # Tạo mới

# Single resource
GET    /api/v1/students/{id}     # Chi tiết
PUT    /api/v1/students/{id}     # Cập nhật
DELETE /api/v1/students/{id}     # Xóa

# Nested resources
GET    /api/v1/classes/{id}/students        # Students trong class
POST   /api/v1/classes/{id}/attendance      # Điểm danh cho class

# Actions (verbs cho non-CRUD)
POST   /api/v1/invoices/{id}/send           # Gửi hóa đơn
POST   /api/v1/students/{id}/enroll         # Đăng ký học
```

---

## Authentication API

### POST /api/v1/auth/login
Đăng nhập và lấy JWT token.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "Nguyễn Văn A",
    "userType": "TEACHER",
    "roles": ["TEACHER"],
    "permissions": ["CLASS_VIEW", "ATTENDANCE_MARK"]
  },
  "profile": {
    "teacherId": 456,
    "department": "Toán học",
    "specialization": "Giải tích",
    "bio": "10 năm kinh nghiệm"
  }
}
```

**Note:**
- Nếu `userType = STUDENT`, `profile` sẽ có `studentId`, `dateOfBirth`, `status`
- Nếu `userType = PARENT`, `profile` sẽ có `parentId`, `children`
- Nếu `userType = ADMIN/STAFF`, `profile` sẽ là `null`

### POST /api/v1/auth/refresh
Refresh access token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### POST /api/v1/auth/logout
Đăng xuất và invalidate tokens.

### GET /api/v1/auth/me
Lấy thông tin user hiện tại.

---

## User Management API

### GET /api/v1/users
Danh sách users (Admin only).

**Query params:**
| Param | Type | Mô tả |
|-------|------|-------|
| page | int | Trang (default: 0) |
| size | int | Số items/trang (default: 20, max: 100) |
| sort | string | Sắp xếp: `name,asc` hoặc `createdAt,desc` |
| search | string | Tìm kiếm theo name, email |
| role | string | Filter theo role |
| status | string | Filter: `active`, `inactive` |

**Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "email": "teacher@example.com",
      "name": "Nguyễn Văn A",
      "phone": "0901234567",
      "avatar": "https://cdn.example.com/avatar.jpg",
      "roles": ["TEACHER"],
      "status": "active",
      "createdAt": "2025-01-15T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### POST /api/v1/users
Tạo user mới.

**Request:**
```json
{
  "email": "newuser@example.com",
  "name": "Trần Thị B",
  "phone": "0912345678",
  "password": "password123",
  "roles": ["TEACHER"]
}
```

### GET /api/v1/users/{id}
Chi tiết user.

### PUT /api/v1/users/{id}
Cập nhật user.

### DELETE /api/v1/users/{id}
Soft delete user.

---

## Student API

### GET /api/v1/students
Danh sách học viên.

**Query params:**
| Param | Type | Mô tả |
|-------|------|-------|
| classId | long | Filter theo lớp |
| parentId | long | Filter theo phụ huynh |
| status | string | `active`, `inactive`, `graduated` |

### POST /api/v1/students
Tạo học viên mới.

**Request:**
```json
{
  "name": "Nguyễn Văn C",
  "email": "student@example.com",
  "phone": "0923456789",
  "dateOfBirth": "2010-05-15",
  "address": "123 Đường ABC, Quận 1, TP.HCM",
  "parentId": 5,
  "note": "Học sinh giỏi toán"
}
```

### POST /api/v1/students/{id}/enroll
Đăng ký học viên vào lớp.

**Request:**
```json
{
  "classId": 10,
  "startDate": "2025-02-01",
  "tuitionAmount": 2000000,
  "discountPercent": 10,
  "note": "Giảm giá học sinh cũ"
}
```

---

## Class API

### GET /api/v1/classes
Danh sách lớp học.

### POST /api/v1/classes
Tạo lớp học mới.

**Request:**
```json
{
  "name": "Toán 10A - Sáng T2/T4/T6",
  "courseId": 5,
  "teacherId": 12,
  "roomId": 3,
  "maxStudents": 25,
  "tuitionFee": 2500000,
  "startDate": "2025-02-01",
  "endDate": "2025-05-31",
  "schedules": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "08:00",
      "endTime": "10:00"
    },
    {
      "dayOfWeek": "WEDNESDAY",
      "startTime": "08:00",
      "endTime": "10:00"
    },
    {
      "dayOfWeek": "FRIDAY",
      "startTime": "08:00",
      "endTime": "10:00"
    }
  ]
}
```

### GET /api/v1/classes/{id}/students
Danh sách học viên trong lớp.

### GET /api/v1/classes/{id}/sessions
Danh sách buổi học của lớp.

---

## Attendance API

### GET /api/v1/classes/{classId}/attendance
Lấy điểm danh theo lớp và ngày.

**Query params:**
| Param | Type | Mô tả |
|-------|------|-------|
| date | date | Ngày điểm danh (YYYY-MM-DD) |
| month | string | Tháng (YYYY-MM) |

### POST /api/v1/classes/{classId}/attendance
Điểm danh cho lớp.

**Request:**
```json
{
  "sessionId": 150,
  "date": "2025-02-10",
  "records": [
    {
      "studentId": 1,
      "status": "PRESENT",
      "note": ""
    },
    {
      "studentId": 2,
      "status": "ABSENT",
      "note": "Phụ huynh xin phép"
    },
    {
      "studentId": 3,
      "status": "LATE",
      "note": "Đến trễ 15 phút"
    }
  ]
}
```

**Attendance Status:**
| Status | Mô tả |
|--------|-------|
| PRESENT | Có mặt |
| ABSENT | Vắng |
| LATE | Đi trễ |
| EXCUSED | Có phép |

---

## Invoice & Payment API

### GET /api/v1/invoices
Danh sách hóa đơn.

**Query params:**
| Param | Type | Mô tả |
|-------|------|-------|
| studentId | long | Filter theo học viên |
| status | string | `draft`, `sent`, `paid`, `overdue`, `cancelled` |
| dueDateFrom | date | Hạn thanh toán từ |
| dueDateTo | date | Hạn thanh toán đến |

### POST /api/v1/invoices
Tạo hóa đơn.

**Request:**
```json
{
  "studentId": 1,
  "dueDate": "2025-02-28",
  "items": [
    {
      "description": "Học phí tháng 2/2025 - Toán 10A",
      "quantity": 1,
      "unitPrice": 2500000,
      "discountPercent": 10
    },
    {
      "description": "Tài liệu học tập",
      "quantity": 1,
      "unitPrice": 200000,
      "discountPercent": 0
    }
  ],
  "note": "Thanh toán trước ngày 28/02"
}
```

### POST /api/v1/invoices/{id}/send
Gửi hóa đơn cho phụ huynh (qua Zalo/Email).

### POST /api/v1/invoices/{id}/payments
Ghi nhận thanh toán.

**Request:**
```json
{
  "amount": 2450000,
  "method": "BANK_TRANSFER",
  "transactionRef": "FT25021012345",
  "paidAt": "2025-02-10T14:30:00Z",
  "note": "Chuyển khoản VCB"
}
```

**Payment Methods:**
| Method | Mô tả |
|--------|-------|
| CASH | Tiền mặt |
| BANK_TRANSFER | Chuyển khoản |
| MOMO | Ví MoMo |
| VNPAY | VNPay QR |
| ZALOPAY | ZaloPay |

---

## Gamification API

### GET /api/v1/students/{id}/points
Lấy điểm tích lũy của học viên.

**Response:**
```json
{
  "studentId": 1,
  "totalPoints": 1250,
  "currentLevel": 5,
  "nextLevelPoints": 1500,
  "badges": [
    {
      "id": 1,
      "name": "Chuyên cần",
      "icon": "🏆",
      "earnedAt": "2025-01-20T10:00:00Z"
    }
  ],
  "recentActivities": [
    {
      "type": "ATTENDANCE",
      "points": 10,
      "description": "Điểm danh đúng giờ",
      "createdAt": "2025-02-10T08:00:00Z"
    }
  ]
}
```

### GET /api/v1/classes/{id}/leaderboard
Bảng xếp hạng lớp học.

### POST /api/v1/rewards/{id}/redeem
Đổi điểm lấy phần thưởng.

---

## Parent Portal API

### GET /api/v1/parent/children
Danh sách con em của phụ huynh.

### GET /api/v1/parent/children/{id}/attendance
Lịch sử điểm danh của con.

### GET /api/v1/parent/children/{id}/grades
Điểm số của con.

### GET /api/v1/parent/invoices
Hóa đơn cần thanh toán.

### GET /api/v1/parent/notifications
Thông báo cho phụ huynh.

---

## Standard Response Format

### Success Response
```json
{
  "data": { ... },
  "message": "Thành công",
  "timestamp": "2025-02-10T10:30:00Z"
}
```

### Paginated Response
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

### Error Response
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dữ liệu không hợp lệ",
    "details": [
      {
        "field": "email",
        "message": "Email đã tồn tại"
      }
    ]
  },
  "timestamp": "2025-02-10T10:30:00Z",
  "path": "/api/v1/students"
}
```

---

## Error Codes

### HTTP Status Codes
| Code | Ý nghĩa | Khi nào dùng |
|------|---------|--------------|
| 200 | OK | Request thành công |
| 201 | Created | Tạo resource thành công |
| 204 | No Content | Xóa thành công |
| 400 | Bad Request | Dữ liệu không hợp lệ |
| 401 | Unauthorized | Chưa đăng nhập |
| 403 | Forbidden | Không có quyền |
| 404 | Not Found | Resource không tồn tại |
| 409 | Conflict | Xung đột dữ liệu |
| 422 | Unprocessable | Business logic error |
| 500 | Server Error | Lỗi server |

### Business Error Codes
| Code | Mô tả |
|------|-------|
| `AUTH_INVALID_CREDENTIALS` | Sai email/mật khẩu |
| `AUTH_TOKEN_EXPIRED` | Token hết hạn |
| `USER_NOT_FOUND` | Không tìm thấy user |
| `USER_EMAIL_EXISTS` | Email đã tồn tại |
| `STUDENT_ALREADY_ENROLLED` | Học viên đã đăng ký lớp này |
| `CLASS_FULL` | Lớp đã đủ học viên |
| `INVOICE_ALREADY_PAID` | Hóa đơn đã thanh toán |
| `INSUFFICIENT_POINTS` | Không đủ điểm đổi thưởng |

---

## Authentication Headers

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
X-Tenant-Id: 12345
Content-Type: application/json
Accept: application/json
```

---

## Service-to-Service Communication

### Gateway → Core Service

Gateway Service cần gọi Core Service để lấy business entity profiles.

#### GET /api/v1/students/{id}
**Truy cập:** Internal (từ Gateway Service)
**Mục đích:** Lấy student profile sau khi login

**Request Headers:**
```http
X-Service-Token: <internal-secret>
X-Request-Source: gateway
```

**Response (200):**
```json
{
  "data": {
    "id": 456,
    "name": "Nguyễn Văn An",
    "email": "student@example.com",
    "phone": "0912345678",
    "dateOfBirth": "2010-05-15",
    "gender": "MALE",
    "status": "ACTIVE",
    "avatarUrl": "https://cdn.example.com/student-456.jpg"
  }
}
```

#### GET /api/v1/teachers/{id}
**Truy cập:** Internal (từ Gateway Service)
**Mục đích:** Lấy teacher profile sau khi login

**Response (200):**
```json
{
  "data": {
    "id": 789,
    "name": "Trần Thị Bình",
    "email": "teacher@example.com",
    "department": "Toán học",
    "specialization": "Giải tích",
    "bio": "10 năm kinh nghiệm giảng dạy"
  }
}
```

#### GET /api/v1/parents/{id}
**Truy cập:** Internal (từ Gateway Service)
**Mục đích:** Lấy parent profile và danh sách children

**Response (200):**
```json
{
  "data": {
    "id": 123,
    "name": "Nguyễn Văn Cha",
    "email": "parent@example.com",
    "phone": "0909123456",
    "children": [
      {
        "studentId": 456,
        "studentName": "Nguyễn Văn An",
        "relationship": "father"
      }
    ]
  }
}
```

### Core → Gateway Service (Optional)

Core Service có thể gọi Gateway để verify permissions.

#### GET /api/v1/users/{id}/permissions
**Truy cập:** Internal (từ Core Service)
**Mục đích:** Verify user permissions

**Request Headers:**
```http
X-Service-Token: <internal-secret>
X-Request-Source: core
```

**Response (200):**
```json
{
  "data": {
    "userId": 1,
    "roles": ["TEACHER", "CLASS_MANAGER"],
    "permissions": [
      "CLASS:VIEW",
      "CLASS:MANAGE",
      "ATTENDANCE:MARK",
      "STUDENT:VIEW"
    ]
  }
}
```

### X-Headers Injected by Gateway

Gateway tự động thêm headers cho **tất cả requests** đến downstream services:

```http
X-User-Id: 123
X-User-Email: user@example.com
X-User-Roles: TEACHER,ADMIN
X-User-Type: TEACHER
X-Reference-Id: 456
X-Tenant-Id: abc
```

**Core Service sử dụng headers này:**

```java
@GetMapping("/students")
public PageResponse<StudentDTO> getStudents(
    @RequestHeader("X-User-Id") Long userId,
    @RequestHeader("X-User-Type") String userType,
    @RequestHeader("X-Reference-Id") Long referenceId,
    Pageable pageable
) {
    // Authorization logic
    if ("TEACHER".equals(userType)) {
        // Teacher chỉ xem students trong classes của mình
        return studentService.getStudentsByTeacher(referenceId, pageable);
    }
    // ...
}
```

### Service Authentication

**Cách 1: Shared Secret**
```yaml
# application.yml (Gateway & Core)
services:
  auth:
    internal-token: ${INTERNAL_SERVICE_TOKEN}
```

**Cách 2: mTLS (Mutual TLS)**
- Certificate-based authentication
- Sử dụng trong production

---

## Theme & Settings API

### GET /api/v1/settings/branding
Lấy cài đặt branding của instance.

**Response (200):**
```json
{
  "data": {
    "logoUrl": "https://cdn.kiteclass.com/tenant/123/logo.png",
    "faviconUrl": "https://cdn.kiteclass.com/tenant/123/favicon.ico",
    "primaryColor": "#0ea5e9",
    "secondaryColor": "#64748b",
    "displayName": "Trung tâm Toán Sáng Tạo",
    "themeTemplateId": "modern-light",
    "colorMode": "light"
  }
}
```

### PUT /api/v1/settings/branding
Cập nhật branding settings (Owner/Admin only).

**Request:**
```json
{
  "logoUrl": "https://cdn.kiteclass.com/tenant/123/logo.png",
  "faviconUrl": "https://cdn.kiteclass.com/tenant/123/favicon.ico",
  "primaryColor": "#0ea5e9",
  "secondaryColor": "#64748b",
  "displayName": "Trung tâm Toán Sáng Tạo"
}
```

### POST /api/v1/settings/branding/logo
Upload logo mới.

**Request:** `multipart/form-data`
| Field | Type | Mô tả |
|-------|------|-------|
| file | File | PNG/JPG/SVG, max 2MB |

**Response (200):**
```json
{
  "data": {
    "url": "https://cdn.kiteclass.com/tenant/123/logo.png",
    "width": 200,
    "height": 60
  }
}
```

### GET /api/v1/settings/theme
Lấy theme config đầy đủ (cho frontend).

**Response (200):**
```json
{
  "data": {
    "templateId": "modern-light",
    "colors": {
      "primary": {
        "50": "#f0f9ff",
        "500": "#0ea5e9",
        "900": "#0c4a6e"
      },
      "background": "#ffffff",
      "foreground": "#0f172a"
    },
    "typography": {
      "fontFamily": "Inter, system-ui, sans-serif",
      "fontSize": {
        "base": "1rem",
        "lg": "1.125rem"
      }
    },
    "borderRadius": "0.5rem",
    "shadows": {
      "sm": "0 1px 2px rgba(0,0,0,0.05)"
    }
  }
}
```

### GET /api/v1/users/me/preferences
Lấy user preferences (color mode, etc.).

**Response (200):**
```json
{
  "data": {
    "colorMode": "system",
    "language": "vi",
    "timezone": "Asia/Ho_Chi_Minh",
    "notifications": {
      "email": true,
      "push": true,
      "sms": false
    }
  }
}
```

### PATCH /api/v1/users/me/preferences
Cập nhật user preferences.

**Request:**
```json
{
  "colorMode": "dark",
  "language": "vi"
}
```

---

## KiteHub Theme API (SaaS Platform)

### GET /api/v1/hub/themes/templates
Danh sách theme templates có sẵn.

**Query params:**
| Param | Type | Mô tả |
|-------|------|-------|
| tier | string | Filter theo tier: `free`, `pro`, `enterprise` |

**Response (200):**
```json
{
  "content": [
    {
      "id": "modern-light",
      "name": "Modern Light",
      "tier": "free",
      "description": "Giao diện hiện đại, sáng sủa",
      "previewUrl": "https://themes.kiteclass.com/modern-light/preview.png",
      "colors": {
        "primary": "#0ea5e9",
        "background": "#ffffff"
      },
      "popularity": 1250
    },
    {
      "id": "playful-kids",
      "name": "Playful Kids",
      "tier": "pro",
      "description": "Màu sắc vui tươi cho trung tâm dạy trẻ em",
      "previewUrl": "https://themes.kiteclass.com/playful-kids/preview.png",
      "colors": {
        "primary": "#f59e0b",
        "background": "#fffbeb"
      },
      "popularity": 890
    }
  ],
  "totalElements": 15
}
```

### GET /api/v1/hub/themes/templates/{id}
Chi tiết theme template.

**Response (200):**
```json
{
  "data": {
    "id": "modern-light",
    "name": "Modern Light",
    "tier": "free",
    "description": "Giao diện hiện đại, sáng sủa, phù hợp với mọi loại trung tâm",
    "version": "1.2.0",
    "config": {
      "colors": { ... },
      "typography": { ... },
      "components": { ... }
    },
    "screenshots": [
      "https://themes.kiteclass.com/modern-light/dashboard.png",
      "https://themes.kiteclass.com/modern-light/students.png"
    ],
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-02-01T00:00:00Z"
  }
}
```

### PUT /api/v1/hub/instances/{id}/theme
Thay đổi theme cho instance (KiteHub Admin).

**Request:**
```json
{
  "templateId": "modern-light"
}
```

### POST /api/v1/hub/themes/templates (Enterprise Only)
Tạo custom theme template.

**Request:**
```json
{
  "name": "Custom Brand Theme",
  "config": {
    "colors": {
      "primary": { "500": "#custom" },
      "background": "#ffffff"
    },
    "typography": {
      "fontFamily": "Custom Font, sans-serif"
    }
  },
  "customCss": ".custom-class { ... }"
}
```

---

## Actions

### Xem chi tiết endpoint
Tham khảo file này hoặc OpenAPI spec.

### Test API
Sử dụng Postman collection hoặc Swagger UI tại `/swagger-ui.html`.
