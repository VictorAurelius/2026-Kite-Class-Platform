# Core Service - PR Implementation List

**Service**: kiteclass-core
**Tech Stack**: Spring Boot 3.5.11, Java 17, PostgreSQL 15
**Total PRs**: 17
**Completed**: 8 (47%)
**Status**: 🔄 Active development

**Reference**:
- Technical plan: [`core-service-implementation.md`](../implementation/core-service-implementation.md)
- Master index: [`00-master-pr-index.md`](./00-master-pr-index.md)

---

## ✅ Phase 1: Foundation (COMPLETE)

### PR 2.1: Core Project Setup ✅
**Status**: Complete
**Branch**: `feature/core`
**Description**: Initialize Spring Boot project with multi-tenant architecture

**Tasks**:
- Maven project with Spring Boot 3.5.11
- BaseEntity with audit fields (createdAt, updatedAt, createdBy, updatedBy)
- Soft delete support (deleted flag)
- Hibernate multi-tenant filters
- PostgreSQL + Flyway migrations
- Redis caching configuration
- RabbitMQ event bus setup
- Common DTOs (ApiResponse, PageResponse, ErrorResponse)
- Exception handling (GlobalExceptionHandler)
- JUnit 5 + Testcontainers setup

**Testing**:
- ApplicationContext loads successfully
- Database migrations run
- Multi-tenant filter works

---

### PR 2.2: Core Common Components ✅
**Status**: Complete
**Description**: Shared utilities, enums, validators

**Tasks**:
- Enums: StudentStatus, TeacherStatus, CourseStatus, ClassStatus, EnrollmentStatus, AttendanceStatus
- Validators: Email, phone, date range
- Mappers: MapStruct configuration
- Messages.properties (Vietnamese + English)
- Constants: Pagination defaults, date formats
- Audit configuration (JpaAuditing)

**Testing**:
- Enum serialization/deserialization
- Validator logic
- Message source works

---

## ✅ Phase 2: Core Modules (8/9 COMPLETE)

### PR 2.3: Student Module ✅
**Status**: Complete (KC-001, merged 2026-02-19)
**Tests**: 8 unit tests

**Features**:
- CRUD operations for students
- Multi-tenant isolation
- Soft delete
- Search with pagination
- Validation (email, phone, date of birth)
- Status management (ACTIVE, INACTIVE, GRADUATED, DROPPED)

**Entities**: `Student`
**Endpoints**:
- POST /api/v1/students - Create student
- GET /api/v1/students - List with search/filter
- GET /api/v1/students/{id} - Get by ID
- PUT /api/v1/students/{id} - Update student
- DELETE /api/v1/students/{id} - Soft delete

---

### PR 2.3.1: Teacher Module ✅
**Status**: Complete (PR-REVIEW-1.1, merged 2026-02-20)
**Tests**: 8 unit tests

**Features**:
- CRUD for teachers
- Status management (ACTIVE, ON_LEAVE, TERMINATED)
- Specialization tracking
- Experience years
- Multi-tenant + soft delete

**Entities**: `Teacher`
**Endpoints**:
- POST /api/v1/teachers - Create teacher
- GET /api/v1/teachers - List with filters
- GET /api/v1/teachers/{id} - Get by ID
- PUT /api/v1/teachers/{id} - Update teacher
- DELETE /api/v1/teachers/{id} - Soft delete

---

### PR 2.4: Course Module ✅
**Status**: Complete (PR-REVIEW-1.2, merged 2026-02-21)
**Tests**: 10 unit tests

**Features**:
- Course template management
- Lifecycle: DRAFT → PUBLISHED → ARCHIVED
- Syllabus management
- Default tuition fee
- Cannot delete PUBLISHED courses (must archive first)
- Multi-tenant + soft delete

**Entities**: `Course`
**Endpoints**:
- POST /api/v1/courses - Create course (DRAFT)
- GET /api/v1/courses - List with status filter
- GET /api/v1/courses/{id} - Get by ID
- PATCH /api/v1/courses/{id} - Update course
- POST /api/v1/courses/{id}/publish - Publish (DRAFT → PUBLISHED)
- POST /api/v1/courses/{id}/archive - Archive (PUBLISHED → ARCHIVED)
- DELETE /api/v1/courses/{id} - Soft delete (only DRAFT)

---

### PR 2.5: Class Module ✅
**Status**: Complete (KC-003, merged 2026-02-22)
**Tests**: 42 tests (32 unit + 10 integration)

**Features**:
- Class creation from Course template
- Lifecycle: SCHEDULED → IN_PROGRESS → COMPLETED → CANCELLED
- Class code auto-generation (prefix + random)
- Schedule management (recurring patterns)
- Session management (auto-generate from schedule)
- Enrollment capacity tracking
- Teacher assignment
- Multi-tenant + soft delete

**Entities**: `Class`, `ClassSchedule`, `ClassSession`
**Endpoints**:
- POST /api/v1/classes - Create class
- GET /api/v1/classes - List with filters (status, teacher, course)
- GET /api/v1/classes/{id} - Get by ID with schedules
- PUT /api/v1/classes/{id} - Update class
- POST /api/v1/classes/{id}/start - Start class (SCHEDULED → IN_PROGRESS)
- POST /api/v1/classes/{id}/complete - Complete class
- POST /api/v1/classes/{id}/cancel - Cancel class
- DELETE /api/v1/classes/{id} - Soft delete

---

### PR 2.6: Enrollment Module ⏳
**Status**: In Progress (PR created, tests written)
**Dependencies**: Student ✅, Class ✅
**Priority**: 🔥 NEXT

**Features**:
- Student enrollment in classes
- Status: ACTIVE, PENDING_PAYMENT, COMPLETED, WITHDRAWN, CANCELLED
- Financial tracking (tuition, discount, final amount)
- Auto-calculate final amount on create/update
- Business rules:
  - BR-ENROLL-001: Cannot enroll if class is full
  - BR-ENROLL-002: Cannot enroll same student twice in same class
- Multi-tenant + soft delete

**Entities**: `Enrollment`
**Endpoints**:
- POST /api/v1/enrollments - Enroll student
- GET /api/v1/enrollments - List with filters
- GET /api/v1/enrollments/{id} - Get by ID
- GET /api/v1/students/{id}/enrollments - Get student's enrollments
- GET /api/v1/classes/{id}/enrollments - Get class enrollments
- PUT /api/v1/enrollments/{id}/status - Update status
- POST /api/v1/enrollments/{id}/withdraw - Withdraw student

**Testing**:
- 8 unit tests minimum
- Integration test: Full enrollment workflow
- Multi-tenant isolation test
- Capacity check test
- Duplicate enrollment prevention test

---

### PR 2.7: Attendance Module ⏳
**Status**: Pending
**Dependencies**: Class ✅, ClassSession ✅, Student ✅
**Estimated**: 3-4 days

**Features**:
- Mark attendance for class sessions
- Status: PRESENT, ABSENT, LATE, EXCUSED
- Bulk attendance marking
- Attendance statistics per student/class
- Late check-in detection
- Gamification points (+10 for PRESENT, -10 for ABSENT)
- Parent notifications for absences

**Entities**: `Attendance`
**Endpoints**:
- POST /api/v1/attendance - Mark attendance (bulk)
- GET /api/v1/sessions/{id}/attendance - Get session attendance
- GET /api/v1/students/{id}/attendance - Get student attendance history
- GET /api/v1/classes/{id}/attendance/stats - Attendance statistics
- PUT /api/v1/attendance/{id} - Update attendance record

---

### PR 2.7.1: Assignment Module ⏳
**Status**: Pending
**Dependencies**: Class ✅, Student ✅
**Estimated**: 3-4 days

**Features**:
- Create assignments for classes
- Assignment types: HOMEWORK, QUIZ, PROJECT, EXAM
- Due date tracking
- Submission management
- Grading workflow
- Late submission handling

**Entities**: `Assignment`, `Submission`
**Endpoints**:
- POST /api/v1/assignments - Create assignment
- GET /api/v1/classes/{id}/assignments - List class assignments
- POST /api/v1/assignments/{id}/submissions - Submit assignment
- GET /api/v1/assignments/{id}/submissions - List submissions
- PUT /api/v1/submissions/{id}/grade - Grade submission

---

### PR 2.7.2: Grade Module ⏳
**Status**: Pending
**Dependencies**: Class ✅, Student ✅, Assignment ✅
**Estimated**: 2-3 days

**Features**:
- Grade tracking per student/class
- Grade categories (MIDTERM, FINAL, QUIZ, ASSIGNMENT)
- Weighted average calculation
- Grade reports
- Transcript generation

**Entities**: `Grade`, `GradeCategory`
**Endpoints**:
- POST /api/v1/grades - Record grade
- GET /api/v1/students/{id}/grades - Get student grades
- GET /api/v1/classes/{id}/grades - Get class gradebook
- GET /api/v1/students/{id}/transcript - Generate transcript

---

### PR 2.8: Invoice Module ⏳
**Status**: Pending
**Dependencies**: Student ✅, Enrollment ✅
**Estimated**: 4-5 days

**Features**:
- Generate invoices for enrollments
- Invoice status: DRAFT, SENT, PAID, PARTIAL, OVERDUE, CANCELLED
- Invoice items (line items)
- Payment tracking
- Due date management
- Overdue notifications

**Entities**: `Invoice`, `InvoiceItem`
**Endpoints**:
- POST /api/v1/invoices - Create invoice
- GET /api/v1/invoices - List with filters
- GET /api/v1/students/{id}/invoices - Student invoices
- PUT /api/v1/invoices/{id} - Update invoice
- POST /api/v1/invoices/{id}/send - Send to student/parent

---

### PR 2.8.1: Payment Module ⏳
**Status**: Pending
**Dependencies**: Invoice ✅
**Estimated**: 3-4 days

**Features**:
- Record payments for invoices
- Payment methods: CASH, BANK_TRANSFER, MOMO, VNPAY, ZALOPAY
- Partial payment support
- Payment reconciliation
- Receipt generation

**Entities**: `Payment`
**Endpoints**:
- POST /api/v1/payments - Record payment
- GET /api/v1/invoices/{id}/payments - List invoice payments
- GET /api/v1/payments/{id} - Get payment details
- POST /api/v1/payments/{id}/receipt - Generate receipt

---

## ⭐ Phase 3: V4.1 New Modules (BUNDLED MODEL)

### PR 2.9: LMS Module ⭐ NEW
**Status**: Pending
**Dependencies**: Course ✅
**Estimated**: 2-3 weeks
**Priority**: 🔥 High (guest-facing features)

**Features**:
- Course structure: CourseModule → Lesson → LearningResource
- Trial lesson access control (isTrial flag)
- Guest can view trial lessons without enrollment
- Student learning progress tracking
- Sequential lesson unlocking
- Video integration (link to Media Service)
- Quiz engine (optional, Phase 2)

**Entities**:
- `CourseModule` (id, courseId, title, order, description)
- `Lesson` (id, moduleId, title, content, videoUrl, isTrial, order)
- `LearningResource` (id, lessonId, type, url, title)
- `LessonProgress` (id, userId, lessonId, completed, completedAt)
- `CourseProgress` (id, userId, courseId, progress, lastAccessedAt)

**Endpoints**:
- **Guest/Public**:
  - GET /api/v1/courses/{id}/modules - View course structure
  - GET /api/v1/lessons/{id} - View lesson (check isTrial or enrollment)
- **Student**:
  - POST /api/v1/lessons/{id}/complete - Mark lesson complete
  - GET /api/v1/courses/{id}/progress - Get learning progress
- **Teacher/Admin**:
  - POST /api/v1/courses/{id}/modules - Create module
  - PUT /api/v1/modules/{id} - Update module
  - DELETE /api/v1/modules/{id} - Delete module
  - POST /api/v1/modules/{id}/lessons - Add lesson
  - PUT /api/v1/lessons/{id} - Update lesson (toggle isTrial)
  - DELETE /api/v1/lessons/{id} - Delete lesson

**Business Rules**:
- BR-LMS-001: Guest can only access lessons where isTrial=true
- BR-LMS-002: Student must have active enrollment to access paid lessons
- BR-LMS-003: Lesson progress auto-saves on completion
- BR-LMS-004: Course progress = (completed lessons / total lessons) * 100

**Testing**:
- 8 unit tests minimum
- Integration test: Guest accesses trial lesson
- Integration test: Student completes lesson, progress updates
- Integration test: Access control (guest cannot access paid lesson)

**Database Migration**: `V9__create_lms_tables.sql`

**Reference**: UC-LMS-01 to UC-LMS-04 in service-use-cases-v3.md

---

### PR 2.10: Marketing Module ⭐ NEW
**Status**: Pending
**Dependencies**: Teacher ✅, Course ✅
**Estimated**: 1-2 weeks
**Priority**: 🔥 High (guest-facing features)

**Features**:
- Landing page content management (per tenant)
- Lead capture and management
- Contact form processing
- Lead qualification workflow
- Email notifications to teacher

**Entities**:
- `LandingPageContent` (id, tenantId, teacherBio, heroImage, tagline)
- `CourseHighlight` (id, contentId, courseId, order) - Featured courses
- `Lead` (id, tenantId, email, name, phone, source, status, courseInterest)
- `ContactMessage` (id, tenantId, name, email, message, createdAt)

**Endpoints**:
- **Public**:
  - GET /api/v1/tenants/{id}/landing - Get landing page content
  - POST /api/v1/leads - Register for trial
  - POST /api/v1/contact - Send contact message
- **Admin/Teacher**:
  - PUT /api/v1/tenants/{id}/landing - Update landing content
  - GET /api/v1/leads - List leads (filter, pagination)
  - PUT /api/v1/leads/{id} - Update lead status
  - GET /api/v1/contact-messages - List contact messages

**Lead Status Workflow**:
- NEW → CONTACTED → CONVERTED → LOST

**Business Rules**:
- BR-MKT-001: Each tenant has one landing page
- BR-MKT-002: Lead email must be unique per tenant
- BR-MKT-003: Contact message triggers email to teacher
- BR-MKT-004: Lead creation sends confirmation email to guest

**Testing**:
- 8 unit tests minimum
- Integration test: Guest registers for trial
- Integration test: Teacher updates landing page
- Integration test: Contact form sends email

**Database Migration**: `V10__create_marketing_tables.sql`

**Reference**: UC-MKT-01 to UC-MKT-04 in service-use-cases-v3.md

**AI Branding (Phase 2 - Future)**:
- Logo generation (DALL-E integration)
- Tagline generation (GPT-4 integration)
- Color scheme suggestions

---

## Phase 4: Infrastructure & Integration

### PR 2.11: Internal APIs for Gateway ✅
**Status**: Complete
**Description**: Cross-service APIs for Gateway to call

**Endpoints**:
- GET /internal/students/{id} - Retrieve student profile
- POST /internal/students - Create student during registration
- DELETE /internal/students/{id} - Soft delete student
- (Similar for Teacher, Course, etc.)

**Security**: HMAC-SHA256 signature verification

---

### PR 2.12: Spring Boot 3.5.10 Upgrade ✅
**Status**: Complete (PR-REVIEW-2.5)
**Description**: Infrastructure upgrade

**Tasks**:
- Upgrade Spring Boot 3.4.1 → 3.5.10
- Upgrade Spring Cloud 2024.0.1 → 2025.0.0
- Fix Security DSL deprecation (Lambda DSL)
- Migrate Testcontainers tests
- Create core-ci.yml workflow

---

### PR 2.13: Settings & Preferences ⏳
**Status**: Pending (moved from PR 2.9)
**Dependencies**: All modules complete
**Estimated**: 1-2 weeks

**Features**:
- System settings (date format, timezone, language)
- User preferences
- Notification settings
- Email templates customization
- Feature flags

**Entities**: `SystemSetting`, `UserPreference`

---

### PR 2.14: Core Docker & Final Integration ⏳
**Status**: Pending (moved from PR 2.10)
**Dependencies**: All PRs complete
**Estimated**: 3-4 days

**Tasks**:
- Dockerfile optimization
- docker-compose.yml for local dev
- Integration tests for all modules
- Performance testing
- Documentation update
- Deployment guide

---

## 📊 Summary

**Total PRs**: 17
**Completed**: 8 (47%)
**In Progress**: 1 (PR 2.6)
**Pending**: 8

**By Phase**:
- Phase 1 (Foundation): 2/2 ✅
- Phase 2 (Core Modules): 5/9 (56%)
- Phase 3 (V4.1 New Modules): 0/2 (0%)
- Phase 4 (Infrastructure): 2/4 (50%)

**Test Coverage**: 292 tests passing (260 unit + 32 integration)

**Next 3 PRs**:
1. PR 2.6: Enrollment Module (in progress)
2. PR 2.7: Attendance Module
3. PR 2.9: LMS Module (guest features)

---

**Last Updated**: 2026-02-26
