# Student & Enrollment — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## Use Cases

### UC-STU-01: Create Student

**Actor:** Admin / Teacher
**Precondition:** User authenticated, has permission on instance

**Steps:**
1. FE: Display student creation form (name, email, phone, dateOfBirth, gender, address, note)
2. User: Fill required fields (name required per BR-STU-001)
3. System: Validate email unique within tenant (BR-STU-002)
4. System: Validate phone unique globally, 10 digits starting with 0 (BR-STU-003)
5. System: Set instance_id for multi-tenant isolation (BR-STU-006)
6. System: Save student with status ACTIVE (BR-STU-004)
7. FE: Redirect to student detail, show success toast

**Postcondition:** Student created with status ACTIVE, cached in Redis

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Name blank or > 100 chars | "Name is required" |
| 409 | Email already exists in tenant | "Email already exists" |
| 409 | Phone already exists | "Phone number already exists" |

---

### UC-STU-02: List & Search Students

**Actor:** Admin / Teacher
**Precondition:** User authenticated

**Steps:**
1. FE: Display student list with search bar and status filter
2. User: Optionally enter search keyword or select status filter
3. System: Query students filtered by instance_id (BR-STU-006), search, and status
4. System: Return paginated results (excludes soft-deleted per BR-STU-005)
5. FE: Render student table with pagination

**Postcondition:** Filtered student list displayed

---

### UC-STU-03: Update Student

**Actor:** Admin / Teacher
**Precondition:** Student exists and is not deleted

**Steps:**
1. FE: Display edit form pre-filled with current data
2. User: Modify fields (name, email, phone, dateOfBirth, gender, address, note)
3. System: Re-validate uniqueness for email (BR-STU-002) and phone (BR-STU-003)
4. System: Update student, invalidate cache
5. FE: Show success toast

**Postcondition:** Student updated, cache invalidated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Student not found or deleted | "Student not found" |
| 409 | Email conflict | "Email already exists" |

---

### UC-STU-04: Delete Student (Soft)

**Actor:** Admin
**Precondition:** Student exists

**Steps:**
1. User: Click delete on student row, confirm dialog
2. System: Set `deleted = true` (BR-STU-005), never hard delete
3. System: Invalidate cache
4. FE: Remove from list, show success toast

**Postcondition:** Student soft-deleted, excluded from future queries

---

### UC-STU-05: Enroll Student in Class

**Actor:** Admin / Teacher
**Precondition:** Student and class exist, class is not full

**Steps:**
1. FE: Display enrollment form (studentId, classId, tuitionAmount, discountPercent, notes)
2. User: Select student and class, enter tuition details
3. System: Validate no duplicate enrollment for same student + class (BR-ENROLL-002)
4. System: Check class capacity — active enrollments < maxStudents (BR-ENROLL-001)
5. System: Validate course is not ARCHIVED (BR-ENROLL-005)
6. System: Validate discount 0-100% (BR-ENROLL-004)
7. System: Auto-calculate finalAmount = tuitionAmount * (1 - discountPercent/100) (BR-ENROLL-003)
8. System: Save enrollment with status PENDING_PAYMENT (BR-ENROLL-006)
9. System: Publish EnrollmentCreatedEvent (triggers invoice generation)
10. FE: Show success toast with enrollment details

**Postcondition:** Enrollment created as PENDING_PAYMENT, event published

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Discount out of range | "Discount must be between 0 and 100" |
| 404 | Student or class not found | "Student/Class not found" |
| 409 | Already enrolled | "Student already enrolled in this class" |
| 409 | Class full | "Class has reached maximum capacity" |

---

### UC-STU-09: Thêm học sinh vào lớp qua dialog (single enroll UI) — GAP-1103

**Actor:** Admin / Teacher
**Precondition:** Lớp tồn tại + đang ở trạng thái cho phép ghi danh (SCHEDULED / IN_PROGRESS); học sinh đã tồn tại

**Steps:**
1. FE: Tại trang chi tiết lớp (`/classes/[id]`), bấm nút "Thêm học sinh vào lớp" → mở dialog
2. User: Tìm + chọn học sinh (search theo tên/email), nhập học phí, % giảm giá (0-100), ghi chú
3. System (FE): Validate học phí ≥ 0 + discount 0-100 trước khi gửi
4. FE: Gọi `POST /api/v1/enrollments` (tái dùng single-enroll — KHÔNG endpoint mới)
5. System (BE): Áp dụng BR-ENROLL-001..006 (capacity, duplicate, discount, status, finalAmount)
6. FE: Thành công → toast "Đã thêm học sinh vào lớp" + đóng dialog + invalidate query roster (refresh danh sách/điểm danh)

**Postcondition:** Enrollment tạo (PENDING_PAYMENT), roster lớp refresh

**Errors:**
| Code | Condition | FE behavior |
|------|-----------|-------------|
| 409 | Đã ghi danh (BR-ENROLL-002) | Toast lỗi với message BE (không bare-catch) |
| 409 | Lớp đầy (BR-ENROLL-001) / CLASS_FULL | Toast lỗi |
| 400 | Discount ngoài 0-100 | Toast lỗi |
| 404 | Học sinh / lớp không tồn tại | Toast lỗi |

---

### UC-STU-10: Tải template ghi danh hàng loạt — GAP-1104

**Actor:** Admin / Teacher
**Precondition:** User authenticated

**Steps:**
1. FE: Tại trang `/classes/[id]/bulk-enroll`, bấm "Tải template mẫu (.xlsx)"
2. FE: Gọi `GET /api/v1/enrollments/bulk-import/template` (blob)
3. System: Trả xlsx `mau-import-ghi-danh.xlsx` (sheet GhiDanh + HuongDan)
4. FE: Lưu file qua object URL

**Postcondition:** User có file mẫu để điền dữ liệu

---

### UC-STU-11: Ghi danh hàng loạt qua xlsx — GAP-1104

**Actor:** Admin / Teacher
**Precondition:** Có file xlsx đúng schema (`class_code` + email/phone + tuition); học sinh + lớp đã tồn tại trong tenant

**Steps:**
1. FE: Chọn file `.xlsx` (≤10MB, ≤1000 dòng) → bấm "Xem trước"
2. FE: Gọi `POST /api/v1/enrollments/bulk-import/preview` (multipart + `X-Tenant-Id`)
3. System (BE): Parse + resolve học sinh (email→phone) + lớp (class_code), tenant-scoped; validate field; phát hiện trùng trong file. KHÔNG ghi DB
4. FE: Hiển thị tổng/hợp lệ/lỗi + bảng lỗi 10 dòng đầu
5. User: Bấm "Xác nhận ghi danh" → `POST .../commit`
6. System (BE): Mỗi dòng hợp lệ gọi `enrollStudent` (BR-ENROLL-001..006, transaction riêng); skip-and-report dòng lỗi
7. FE: Toast kết quả `Đã ghi danh X/Y lượt (Z lỗi)` + bảng lỗi nếu có

**Postcondition:** Các dòng hợp lệ được ghi danh (PENDING_PAYMENT); dòng lỗi không ghi danh + được báo cáo

**Errors:** xem `api-contract.md` (400 parse/empty, 413 quá 1000 dòng, 415 sai định dạng) + lỗi từng dòng (không tìm thấy lớp/học sinh, đã ghi danh, lớp đầy)

---

### UC-STU-06: Update Enrollment Status

**Actor:** Admin / Teacher
**Precondition:** Enrollment exists

**Steps:**
1. User: Select new status (ACTIVE, COMPLETED, WITHDRAWN)
2. System: Validate status transition is valid
3. System: Update enrollment status
4. FE: Refresh enrollment list

**Postcondition:** Enrollment status updated

---

### UC-STU-07: Withdraw Student from Class

**Actor:** Admin / Student
**Precondition:** Enrollment exists and is not already WITHDRAWN

**Steps:**
1. User: Click withdraw, confirm dialog
2. System: Validate enrollment is not already WITHDRAWN
3. System: Set status to WITHDRAWN
4. FE: Show withdrawal confirmation

**Postcondition:** Enrollment marked WITHDRAWN, class capacity freed

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Already withdrawn | "Enrollment already withdrawn" |
| 404 | Enrollment not found | "Enrollment not found" |

---

### UC-STU-08: Internal Student Operations

**Actor:** System (KiteHub internal calls)
**Precondition:** Valid internal service authentication

**Steps:**
1. KiteHub calls GET /internal/students/{id} to fetch student data
2. KiteHub calls POST /internal/students to create student during provisioning
3. KiteHub calls DELETE /internal/students/{id} to soft-delete

**Postcondition:** Student data synchronized between KiteHub and KiteClass
