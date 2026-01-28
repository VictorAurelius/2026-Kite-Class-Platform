# Teacher Module - Business Logic

**Service:** kiteclass-core
**Module:** Teacher Management
**Priority:** P0 (BLOCKING - Required for PR 1.8 completion)
**Status:** Design Phase
**Version:** 1.0.0
**Date:** 2026-01-28

---

## 📋 1. Tổng Quan Module

### Mục đích

Module Teacher quản lý thông tin giáo viên và phân quyền truy cập lớp học trong hệ thống KiteClass.

**Vai trò trong hệ thống:**
- Lưu trữ profile và thông tin nghiệp vụ của giáo viên
- Quản lý assignment giáo viên với lớp học (teacher-class mapping)
- Hỗ trợ 2 use cases chính:
  1. **Language Center:** Nhiều teachers, mỗi teacher chỉ quản lý các classes được assign
  2. **Independent Teacher:** 1 person vừa OWNER vừa TEACHER, full access tất cả classes

### Phạm vi (Scope)

**Trong phạm vi:**
- ✅ CRUD operations cho Teacher entity
- ✅ Teacher-Class assignment management
- ✅ Permission model: Resource-level access control
- ✅ Internal APIs cho Gateway (profile fetching)
- ✅ Teacher specialization và qualification tracking
- ✅ Teacher schedule và availability

**Ngoài phạm vi:**
- ❌ Payroll và salary management (sẽ có module riêng)
- ❌ Performance evaluation (future module)
- ❌ Teacher training và development (future)

### Business Context

**Use Case 1: Trung Tâm Tiếng Anh (Language Center)**
```
Structure:
- 1 OWNER/ADMIN: Full access 30 classes
- 5 TEACHERS:
  - Teacher A manages: Class 1, 2, 3 (assigned)
  - Teacher B manages: Class 4, 5, 6 (assigned)
  - Teacher C manages: Class 7, 8, 9 (assigned)
  - Teacher D manages: Class 10, 11, 12 (assigned)
  - Teacher E manages: Class 13, 14, 15 (assigned)

Permissions:
- Teacher A CAN: View/Edit Class 1, 2, 3 ONLY
- Teacher A CANNOT: Access Class 4+ (not assigned)
- OWNER/ADMIN: Full access all 30 classes

Implementation:
- teacher_classes table controls which classes teacher can access
- Permission check on every class operation
```

**Use Case 2: Giáo Viên Độc Lập (Independent Teacher)**
```
Structure:
- 1 person: Vừa OWNER vừa TEACHER
- Self-manage 3-5 classes
- Không cần complex permissions

Permissions:
- User has roles: [OWNER, TEACHER]
- OWNER role → Bypass all resource-level checks
- Full access to all classes

Implementation:
- Check roles in Gateway
- If OWNER → Skip teacher_classes check
- Simple and flexible
```

### Priority

- **Priority:** P0 (Critical)
- **Reason:**
  - BLOCKING PR 1.8 (Teacher profile fetching)
  - Required before Class Module (Class entity references teacher_id)
  - Core business entity

---

## 🏗️ 2. Thực Thể Nghiệp Vụ

### 2.1. Teacher Entity

**Table:** `teachers`

**Mô tả:** Lưu trữ thông tin profile và nghiệp vụ của giáo viên.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| name | VARCHAR(100) | NO | Tên giáo viên | 2-100 chars |
| email | VARCHAR(255) | NO | Email (unique) | Valid email format |
| phone_number | VARCHAR(20) | YES | Số điện thoại | 10-15 digits |
| specialization | VARCHAR(100) | YES | Chuyên môn (English, Math, etc.) | Max 100 chars |
| bio | TEXT | YES | Giới thiệu bản thân | Max 2000 chars |
| qualification | VARCHAR(200) | YES | Trình độ (Bachelor, Master, etc.) | Max 200 chars |
| experience_years | INT | YES | Số năm kinh nghiệm | >= 0 |
| avatar_url | VARCHAR(500) | YES | URL ảnh đại diện | Valid URL |
| status | VARCHAR(20) | NO | Trạng thái (ACTIVE, INACTIVE, ON_LEAVE) | Enum |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian cập nhật | Auto-update |
| created_by | VARCHAR(100) | YES | Người tạo | - |
| updated_by | VARCHAR(100) | YES | Người cập nhật | - |

**Indexes:**
```sql
CREATE INDEX idx_teachers_email ON teachers(email);
CREATE INDEX idx_teachers_status ON teachers(status);
CREATE INDEX idx_teachers_specialization ON teachers(specialization);
```

**Status Values:**
- `ACTIVE`: Đang hoạt động, có thể assign classes
- `INACTIVE`: Tạm ngưng, không assign classes mới
- `ON_LEAVE`: Nghỉ phép, classes hiện tại vẫn giữ

### 2.2. TeacherClass Entity (Assignment)

**Table:** `teacher_classes`

**Mô tả:** Quản lý assignment giữa giáo viên và lớp học. Table này **CONTROLS** permissions - teacher chỉ có thể access classes có trong table này.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| teacher_id | BIGINT | NO | FK to teachers.id | Must exist |
| class_id | BIGINT | NO | FK to classes.id | Must exist |
| role | VARCHAR(20) | NO | Role trong class (MAIN_TEACHER, ASSISTANT) | Enum |
| assigned_at | TIMESTAMP | NO | Thời gian assign | Auto-set |
| assigned_by | VARCHAR(100) | YES | Người assign | - |

**Constraints:**
```sql
UNIQUE (teacher_id, class_id) -- One teacher can't be assigned to same class twice
FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
```

**Indexes:**
```sql
CREATE INDEX idx_teacher_classes_teacher_id ON teacher_classes(teacher_id);
CREATE INDEX idx_teacher_classes_class_id ON teacher_classes(class_id);
```

**Role Values:**
- `MAIN_TEACHER`: Giáo viên chính, full control class
- `ASSISTANT`: Giáo viên phụ, limited permissions

---

## 📐 3. Quy Tắc Kinh Doanh

### BR-TEACHER-001: Email Phải Duy Nhất

**Mô tả:** Mỗi giáo viên phải có email duy nhất trong hệ thống.

**Lý do:** Email dùng để liên lạc và liên kết với Gateway User.

**Validation:**
```java
boolean exists = teacherRepository.existsByEmail(email);
if (exists) {
    throw new DuplicateResourceException("email", email);
}
```

---

### BR-TEACHER-002: Teacher Phải Có Specialization

**Mô tả:** Giáo viên nên có specialization (chuyên môn) để dễ search và assign.

**Lý do:** Giúp admin/owner tìm đúng teacher cho từng môn học.

**Note:** Không bắt buộc (nullable) nhưng recommended.

---

### BR-TEACHER-003: Teacher Có Thể Được Assign Nhiều Classes

**Mô tả:** Một teacher có thể dạy nhiều classes đồng thời.

**Lý do:** Real-world scenario - teacher thường dạy nhiều lớp.

**Implementation:** Multiple records trong teacher_classes table.

---

### BR-TEACHER-004: Class Phải Có Ít Nhất 1 Main Teacher

**Mô tả:** Mỗi class phải có ít nhất 1 teacher với role = MAIN_TEACHER.

**Lý do:** Class không thể hoạt động không có giáo viên chính.

**Validation:**
- Khi remove teacher: Check nếu là MAIN_TEACHER cuối cùng → Không cho remove
- Khi create class: Phải assign ít nhất 1 MAIN_TEACHER

---

### BR-TEACHER-005: Chỉ ACTIVE Teachers Mới Được Assign Classes Mới

**Mô tả:** Chỉ teachers có status = ACTIVE mới có thể được assign vào classes mới.

**Lý do:** INACTIVE hoặc ON_LEAVE teachers không nên nhận thêm công việc.

**Note:** Teachers đã assign từ trước vẫn giữ assignments khi chuyển sang INACTIVE/ON_LEAVE.

---

## 🎯 4. Use Cases

### UC-TEACHER-001: Tạo Teacher Profile

**Người thực hiện:** ADMIN/OWNER

**Điều kiện trước:**
- User đã đăng nhập với role ADMIN/OWNER

**Luồng chính:**

1. Admin truy cập Teacher Management
2. Admin click "Thêm giáo viên"
3. Frontend hiển thị form:
   - Name (required)
   - Email (required)
   - Phone number
   - Specialization (e.g., "English", "Math")
   - Qualification (e.g., "Bachelor's in Education")
   - Experience years
   - Bio
4. Admin điền thông tin và submit
5. Frontend gửi POST `/api/v1/teachers`
6. Hệ thống validate:
   - **BR-TEACHER-001:** Email unique
   - Name không rỗng
   - Email valid format
7. Hệ thống tạo Teacher:
   - status = ACTIVE (mặc định)
8. Hệ thống lưu vào database
9. Hệ thống trả về HTTP 201 Created với TeacherResponse
10. Frontend hiển thị: "Thêm giáo viên thành công"

**Luồng thay thế:**

**AF1 - Email trùng:**
- Tại bước 6, email đã tồn tại
- Trả về HTTP 409 Conflict
- Message: "Email '{email}' đã được sử dụng"

**Kết quả:**
- Teacher profile được tạo
- Status = ACTIVE
- Sẵn sàng được assign vào classes

---

### UC-TEACHER-002: Assign Teacher Vào Class

**Người thực hiện:** ADMIN/OWNER

**Mục đích:** Phân công giáo viên vào lớp học cụ thể

**Điều kiện trước:**
- Teacher tồn tại và status = ACTIVE
- Class tồn tại

**Luồng chính:**

1. Admin truy cập Class Detail page
2. Admin click "Assign Teacher"
3. Frontend hiển thị form:
   - Select Teacher (dropdown list ACTIVE teachers)
   - Select Role (MAIN_TEACHER / ASSISTANT)
4. Admin chọn và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/teachers`
   ```json
   {
     "teacherId": 5,
     "role": "MAIN_TEACHER"
   }
   ```
6. Hệ thống validate:
   - Teacher tồn tại và ACTIVE (BR-TEACHER-005)
   - Class tồn tại
   - Chưa có assignment này (unique constraint)
7. Hệ thống tạo TeacherClass record
8. Hệ thống trả về HTTP 201 Created
9. Frontend hiển thị: "Assign teacher thành công"
10. Teacher xuất hiện trong class roster

**Luồng thay thế:**

**AF1 - Teacher đã được assign:**
- Tại bước 6, already exists trong teacher_classes
- Trả về HTTP 409 Conflict
- Message: "Teacher đã được assign vào class này"

**AF2 - Teacher không ACTIVE:**
- Tại bước 6, teacher.status != ACTIVE
- Trả về HTTP 400 Bad Request
- Message: "Chỉ ACTIVE teachers mới có thể assign vào class"

**Kết quả:**
- Teacher được assign vào class
- Teacher có quyền access class này
- Teacher xuất hiện trong class detail

---

### UC-TEACHER-003: Remove Teacher Khỏi Class

**Người thực hiện:** ADMIN/OWNER

**Điều kiện trước:**
- Teacher đã được assign vào class

**Luồng chính:**

1. Admin truy cập Class Detail → Teacher list
2. Admin click "Remove" bên teacher cần remove
3. Frontend hiển thị confirmation dialog
4. Admin confirm
5. Frontend gửi DELETE `/api/v1/classes/{classId}/teachers/{teacherId}`
6. Hệ thống validate:
   - **BR-TEACHER-004:** Nếu là MAIN_TEACHER cuối → Không cho remove
7. Hệ thống xóa TeacherClass record
8. Hệ thống trả về HTTP 204 No Content
9. Frontend update UI: Teacher biến mất khỏi roster

**Luồng thay thế:**

**AF1 - MAIN_TEACHER cuối cùng:**
- Tại bước 6, teacher là MAIN_TEACHER duy nhất
- Trả về HTTP 409 Conflict
- Message: "Không thể remove MAIN_TEACHER cuối cùng. Phải có ít nhất 1 MAIN_TEACHER."

**Kết quả:**
- Teacher bị remove khỏi class
- Teacher KHÔNG CÒN quyền access class này
- Class roster updated

---

### UC-TEACHER-004: Get Teacher Classes (For Permission Check)

**Người thực hiện:** System (Internal API)

**Mục đích:** Lấy danh sách classes mà teacher được assign (cho permission check)

**Điều kiện trước:**
- Teacher ID hợp lệ

**Luồng chính:**

1. Hệ thống nhận request GET `/api/v1/teachers/{teacherId}/classes`
2. Hệ thống query teacher_classes table:
   ```sql
   SELECT class_id, role
   FROM teacher_classes
   WHERE teacher_id = :teacherId
   ```
3. Hệ thống trả về list of ClassAssignment
4. System sử dụng list này để check permissions

**Response Example:**
```json
{
  "success": true,
  "data": [
    {
      "classId": 1,
      "className": "English Beginner A1",
      "role": "MAIN_TEACHER",
      "assignedAt": "2026-01-15T10:00:00Z"
    },
    {
      "classId": 2,
      "className": "English Intermediate B1",
      "role": "MAIN_TEACHER",
      "assignedAt": "2026-01-20T14:30:00Z"
    }
  ]
}
```

**Sử dụng trong Permission Check:**
```java
// Use Case 1: Language Center
if (user.hasRole("OWNER") || user.hasRole("ADMIN")) {
    return true; // Full access
}

if (user.getUserType() == UserType.TEACHER) {
    Long teacherId = user.getReferenceId();

    // Get assigned classes
    List<Long> assignedClassIds = teacherService
        .getAssignedClassIds(teacherId);

    // Check if teacher can access this class
    return assignedClassIds.contains(classId);
}
```

---

## 🔐 5. Permission Model

### Architecture Overview

```
┌─────────────────────────────────────────────────┐
│            Gateway (Authentication)              │
├─────────────────────────────────────────────────┤
│                                                  │
│  users table:                                    │
│  - user_type: TEACHER                           │
│  - reference_id: 5 (→ teachers.id in Core)     │
│  - roles: [OWNER, TEACHER] or [TEACHER]        │
│                                                  │
└─────────────────────┬───────────────────────────┘
                      │
                      │ Permission Check
                      ↓
┌─────────────────────────────────────────────────┐
│              Core (Business Logic)               │
├─────────────────────────────────────────────────┤
│                                                  │
│  teachers table:                                 │
│  - id: 5                                        │
│  - name, email, specialization                  │
│                                                  │
│  teacher_classes table (CONTROLS ACCESS):       │
│  - teacher_id: 5, class_id: 1                  │
│  - teacher_id: 5, class_id: 2                  │
│  - teacher_id: 5, class_id: 3                  │
│                                                  │
│  → Teacher 5 CAN access: Class 1, 2, 3 ONLY    │
│  → Teacher 5 CANNOT access: Class 4+            │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Use Case 1: Language Center (Multiple Teachers)

**Scenario:**
- Center có 5 teachers
- Mỗi teacher chỉ access các classes được assign

**Permission Implementation:**

```java
@Service
public class PermissionService {

    public boolean canAccessClass(User user, Long classId) {
        // Check if user is OWNER/ADMIN
        if (user.hasRole("OWNER") || user.hasRole("ADMIN")) {
            return true; // Full access
        }

        // Check if user is TEACHER
        if (user.getUserType() == UserType.TEACHER) {
            Long teacherId = user.getReferenceId();

            // Query teacher_classes table
            boolean assigned = teacherClassRepository
                .existsByTeacherIdAndClassId(teacherId, classId);

            return assigned; // Only access assigned classes
        }

        return false;
    }

    public boolean canModifyClass(User user, Long classId) {
        // Same as canAccessClass but also check role
        if (!canAccessClass(user, classId)) {
            return false;
        }

        // If ASSISTANT, can view but not modify
        if (user.getUserType() == UserType.TEACHER) {
            Long teacherId = user.getReferenceId();
            Optional<TeacherClass> assignment = teacherClassRepository
                .findByTeacherIdAndClassId(teacherId, classId);

            if (assignment.isPresent()) {
                return assignment.get().getRole() == TeacherRole.MAIN_TEACHER;
            }
        }

        return true; // OWNER/ADMIN can modify
    }
}
```

**Example:**
```java
User teacherA = getCurrentUser(); // Teacher A
// teacherA.userType = TEACHER
// teacherA.referenceId = 5
// teacherA.roles = [TEACHER]

// Teacher A tries to access Class 1
permissionService.canAccessClass(teacherA, 1L);
→ Check teacher_classes: (teacher_id=5, class_id=1) exists? YES
→ Return true ✅

// Teacher A tries to access Class 10
permissionService.canAccessClass(teacherA, 10L);
→ Check teacher_classes: (teacher_id=5, class_id=10) exists? NO
→ Return false ❌ Access Denied
```

### Use Case 2: Independent Teacher (OWNER + TEACHER)

**Scenario:**
- 1 person vừa là OWNER vừa là TEACHER
- Full access tất cả classes

**Permission Implementation:**

```java
User independentTeacher = getCurrentUser();
// independentTeacher.userType = TEACHER
// independentTeacher.referenceId = 1
// independentTeacher.roles = [OWNER, TEACHER] ← Has OWNER role

// Independent teacher tries to access ANY class
permissionService.canAccessClass(independentTeacher, anyClassId);
→ Check roles: Has OWNER? YES
→ Return true immediately ✅ (Bypass teacher_classes check)
→ Full access to all classes
```

**Benefits:**
- ✅ Simple: Just add OWNER role to user
- ✅ No need to create teacher_classes records for every class
- ✅ Scales: Works for 1 class or 100 classes
- ✅ Flexible: Can switch between modes by adding/removing OWNER role

---

## 🔗 6. Integration với Gateway

### Gateway → Core Communication

**Scenario:** User login, Gateway fetch teacher profile

**Flow:**

```
1. User login ở Gateway
2. Gateway validates credentials
3. Gateway checks: user.userType == TEACHER
4. Gateway gets: user.referenceId = 5
5. Gateway calls Core:
   GET /internal/teachers/5
   Header: X-Internal-Request: true
6. Core returns TeacherProfileResponse
7. Gateway includes profile trong LoginResponse
```

**Core Internal API:**

```java
@RestController
@RequestMapping("/internal/teachers")
public class InternalTeacherController {

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherProfileResponse>>
            getTeacher(@PathVariable Long id) {

        Teacher teacher = teacherService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));

        TeacherProfileResponse response = new TeacherProfileResponse(
            teacher.getId(),
            teacher.getName(),
            teacher.getEmail(),
            teacher.getPhoneNumber(),
            teacher.getSpecialization(),
            teacher.getAvatarUrl(),
            teacher.getStatus().name()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

**Gateway DTO:**

```java
public record TeacherProfileResponse(
    Long id,
    String name,
    String email,
    String phoneNumber,
    String specialization,
    String avatarUrl,
    String status
) {}
```

---

## 📊 7. Summary

### Entities
- ✅ **Teacher:** Profile và thông tin nghiệp vụ
- ✅ **TeacherClass:** Assignment và permission control

### Business Rules
- ✅ BR-TEACHER-001: Email unique
- ✅ BR-TEACHER-002: Should have specialization
- ✅ BR-TEACHER-003: Can assign nhiều classes
- ✅ BR-TEACHER-004: Class phải có ít nhất 1 MAIN_TEACHER
- ✅ BR-TEACHER-005: Chỉ ACTIVE teachers assign được

### Use Cases
- ✅ UC-TEACHER-001: Create Teacher Profile
- ✅ UC-TEACHER-002: Assign Teacher to Class
- ✅ UC-TEACHER-003: Remove Teacher from Class
- ✅ UC-TEACHER-004: Get Teacher Classes (Permission)

### Permission Model
- ✅ Support Use Case 1: Language Center (resource-level permissions)
- ✅ Support Use Case 2: Independent Teacher (OWNER bypass)
- ✅ Scalable và flexible

### Integration
- ✅ Internal API cho Gateway profile fetching
- ✅ Clear separation: Gateway (auth) vs Core (business)

---

## 🚀 Next Steps

**Sau khi document này được approve:**

1. **Create PR 2.3.1: Teacher Module**
   - Implement Teacher entity
   - Implement TeacherClass entity
   - Implement repositories
   - Implement services
   - Implement internal API
   - Write tests (unit + integration)

2. **Update PR 1.8 (Gateway)**
   - Uncomment `ProfileFetcher.fetchTeacherProfile()` lines 136-137
   - Test teacher login with profile
   - Add integration tests

3. **Update Class Module**
   - Add teacher_id FK to classes table
   - Update Class entity với Teacher relationship

---

**Author:** VictorAurelius + Claude Sonnet 4.5
**Created:** 2026-01-28
**Status:** Ready for Review
**Next:** Course Module business logic
