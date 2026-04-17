# Teacher — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## Use Cases

### UC-TCH-01: Create Teacher

**Actor:** Admin / Owner
**Precondition:** User authenticated with admin or owner role

**Steps:**
1. FE: Display teacher creation form (name, email, phoneNumber, specialization, bio, qualification, experienceYears)
2. User: Fill required fields — name 2-100 chars (BR-TCH-002)
3. System: Validate email unique within tenant (BR-TCH-001)
4. System: Set instance_id for multi-tenant isolation (BR-TCH-006)
5. System: Save teacher with status ACTIVE
6. System: Cache teacher data in Redis
7. FE: Redirect to teacher detail, show success toast

**Postcondition:** Teacher created with ACTIVE status, cached

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Name blank or < 2 or > 100 chars | "Name must be between 2 and 100 characters" |
| 400 | Email invalid format | "Invalid email format" |
| 409 | Email exists in tenant | "Email already exists" |

---

### UC-TCH-02: List & Search Teachers

**Actor:** Admin / Owner
**Precondition:** User authenticated

**Steps:**
1. FE: Display teacher list with search bar, status filter, specialization filter
2. User: Optionally enter search keyword or select filters
3. System: Query teachers filtered by instance_id (BR-TCH-006)
4. System: Exclude soft-deleted records (BR-TCH-005)
5. System: Return paginated results
6. FE: Render teacher table with pagination

**Postcondition:** Filtered teacher list displayed

---

### UC-TCH-03: Search by Specialization

**Actor:** Admin / Owner
**Precondition:** User authenticated

**Steps:**
1. User: Enter specialization keyword in search
2. System: GET /api/v1/teachers/search?specialization={keyword}
3. System: Return teachers matching specialization within tenant
4. FE: Display filtered results

**Postcondition:** Teachers filtered by specialization

---

### UC-TCH-04: Update Teacher

**Actor:** Admin / Owner
**Precondition:** Teacher exists and is not deleted

**Steps:**
1. FE: Display edit form pre-filled with current data
2. User: Modify fields (name, email, phoneNumber, specialization, bio, qualification, experienceYears)
3. System: Re-validate email uniqueness within tenant (BR-TCH-001)
4. System: Update teacher record, invalidate cache; existing class assignments preserved if status changes to ON_LEAVE (BR-TCH-004)
5. FE: Show success toast

**Postcondition:** Teacher updated, cache invalidated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Teacher not found or deleted | "Teacher not found" |
| 409 | Email conflict | "Email already exists" |

---

### UC-TCH-05: Delete Teacher (Soft)

**Actor:** Admin / Owner
**Precondition:** Teacher exists

**Steps:**
1. User: Click delete on teacher row, confirm dialog
2. System: Set `deleted = true` (BR-TCH-005), never hard delete
3. System: Invalidate cache
4. FE: Remove from list, show success toast

**Postcondition:** Teacher soft-deleted, excluded from future queries

---

### UC-TCH-06: Assign Teacher to Course

**Actor:** Admin / Owner / Course CREATOR
**Precondition:** Teacher is ACTIVE (BR-TCH-003), course exists

**Steps:**
1. User: Select teacher and role (CREATOR, INSTRUCTOR, or ASSISTANT)
2. System: Validate teacher status is ACTIVE (BR-TCH-003)
3. System: Create TeacherCourse record with selected role
4. FE: Show teacher in course's teacher list

**Postcondition:** Teacher assigned to course with specified role

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Teacher not ACTIVE | "Only active teachers can be assigned" |
| 409 | Already assigned | "Teacher already assigned to this course" |

---

### UC-TCH-07: Assign Teacher to Class

**Actor:** Admin / Owner / Course CREATOR
**Precondition:** Teacher is ACTIVE (BR-TCH-003), class exists

**Steps:**
1. User: Select teacher and role (MAIN_TEACHER or ASSISTANT)
2. System: Validate teacher status is ACTIVE (BR-TCH-003)
3. System: If user has OWNER role, bypass permission checks (BR-TCH-007)
4. System: Create TeacherClass record with selected role
5. FE: Show teacher in class's teacher list

**Postcondition:** Teacher assigned to class with specified role

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Teacher INACTIVE or ON_LEAVE | "Only active teachers can be assigned to new classes" |
| 409 | Already assigned | "Teacher already assigned to this class" |

---

### UC-TCH-08: Independent Teacher Flow (Owner)

**Actor:** Owner (single-teacher scenario)
**Precondition:** User has OWNER + TEACHER roles

**Steps:**
1. Owner creates account — has OWNER role (BR-TCH-007)
2. Owner creates course → auto TeacherCourse(CREATOR)
3. Owner creates class → auto TeacherClass(MAIN_TEACHER)
4. System: OWNER bypasses all teacher_classes permission checks (BR-TCH-007)
5. Owner manages everything without explicit assignment

**Postcondition:** Owner has full access to all resources without permission restrictions

---

### UC-TCH-09: Internal Teacher Operations

**Actor:** System (KiteHub internal calls)
**Precondition:** Valid internal service authentication

**Steps:**
1. KiteHub calls GET /internal/teachers/{id} to fetch teacher data
2. KiteHub calls POST /internal/teachers to create teacher during instance provisioning
3. KiteHub calls DELETE /internal/teachers/{id} to soft-delete

**Postcondition:** Teacher data synchronized between KiteHub and KiteClass
