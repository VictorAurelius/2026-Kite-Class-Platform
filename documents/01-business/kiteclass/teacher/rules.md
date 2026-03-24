# Teacher — Business Rules

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## 1. Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-TCH-001 | Email unique per tenant | Checked within same `instance_id` |
| BR-TCH-002 | Name required | 2-100 characters, not blank |
| BR-TCH-003 | Only ACTIVE can be assigned | Cannot assign INACTIVE/ON_LEAVE teacher to new classes |
| BR-TCH-004 | ON_LEAVE keeps classes | Existing class assignments preserved during leave |
| BR-TCH-005 | Soft delete only | `deleted` flag, never hard delete |
| BR-TCH-006 | Multi-tenant isolation | All queries filtered by `instance_id` |
| BR-TCH-007 | OWNER bypasses class check | Users with OWNER role skip teacher_classes permission check |

**Teacher statuses:** ACTIVE, INACTIVE, ON_LEAVE

### Permission Model (2-Level Hierarchy)

**Level 1 — Course-level** (via `teacher_courses`):
| Role | Permissions |
|------|------------|
| CREATOR | Full control course + all its classes |
| INSTRUCTOR | Teach course + manage assigned classes |
| ASSISTANT | View only |

**Level 2 — Class-level** (via `teacher_classes`):
| Role | Permissions |
|------|------------|
| MAIN_TEACHER | Full control: attendance, assignments, grades, materials |
| ASSISTANT | Limited: view attendance, assist with assignments |

**Priority:** Course-level > Class-level. If teacher is CREATOR of course -> auto access all classes.

### Use Case Matrix

| Scenario | Permission Check |
|----------|-----------------|
| Language center (5+ teachers) | Check `teacher_classes` for each operation |
| Independent teacher (1 person) | OWNER role -> bypass all resource checks |

---

## 2. Flow

### Teacher Onboarding
1. Admin creates teacher profile (name, email, specialization)
2. Status = ACTIVE
3. Admin assigns teacher to course(s) via TeacherCourse
4. Admin assigns teacher to class(es) via TeacherClass
5. Teacher logs in, sees only assigned courses/classes

### Teaching Workflow
1. Teacher views assigned classes
2. For each class session:
   - Take attendance (Attendance Module)
   - Create/grade assignments (Assignment Module)
   - Enter midterm/final scores (Grade Module)
3. At course end: final grades auto-calculated

### Independent Teacher Flow
1. Owner creates account (has OWNER + TEACHER roles)
2. Owner creates courses -> auto CREATOR
3. Owner creates classes -> auto MAIN_TEACHER
4. No permission restrictions (OWNER bypass)

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| (Planned) Class assigned | class-assignment | Teacher email |
| (Planned) Schedule change | schedule-update | Teacher email |

> Email templates not yet implemented.

---

## 4. Config

| Key | Default | Description |
|-----|---------|-------------|
| `teacher.name.max-length` | `100` | Max name characters |
| `teacher.bio.max-length` | `2000` | Max bio characters |
| `teacher.qualification.max-length` | `200` | Max qualification text |
| `teacher.cache.name` | `teachers` | Redis cache name |
| `teacher.cache.key-generator` | `multiTenantKeyGenerator` | Tenant-aware cache |

### Database Indexes
- `idx_teachers_email` — Email lookup
- `idx_teachers_status` — Status filter
- `idx_teachers_specialization` — Specialization filter
- `idx_teacher_classes_teacher_id` — Classes per teacher
- `idx_teacher_classes_class_id` — Teachers per class
- `idx_teacher_courses_teacher_id` — Courses per teacher
- `idx_teacher_courses_course_id` — Teachers per course
- `uk_teacher_classes` — Unique (teacher_id, class_id)
- `uk_teacher_courses` — Unique (teacher_id, course_id)
