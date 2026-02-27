# Core Service - PR Implementation List

**Service**: kiteclass-core
**Version**: V4.1 (Bundled Model)
**Tech Stack**: Spring Boot 3.5.11, Java 17, PostgreSQL 15
**Total PRs**: 20 (15 original + 2 V4.1 LMS + 2 V4.1 Trial Learning + 1 V4.1 Storage) ⭐
**Completed**: 8 (40%)
**Planned**: 3 (PR 2.10.1 Storage, PR 2.13-2.14 Trial Learning)
**Status**: 🔄 Active development
**Last Updated**: 2026-02-27 ⭐

**Reference**:
- Technical plan: [`core-service-implementation.md`](../implementation/core-service-implementation.md)
- Master index: [`00-master-pr-index.md`](./00-master-pr-index.md)
- Database design: [`database-design.md`](../database/database-design.md)

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
- `CourseModule` (id, courseId, title, orderNumber, description, instanceId)
- `Lesson` (id, moduleId, title, content, videoUrl, isTrial, orderNumber, estimatedDuration, instanceId)
- `LearningResource` (id, lessonId, type, url, title, fileSize, instanceId)
- `LessonProgress` (id, userId, lessonId, completed, completedAt, progressPercent, instanceId)

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
- `LandingPage` (id, instanceId[unique], heroTitle, heroSubtitle, heroImageUrl, teacherBio, logoUrl, tagline, primaryColor, secondaryColor)
- `Lead` (id, instanceId, email, name, phone, source, status, courseInterestId, message, lastContactedAt)
- `ContactMessage` (id, instanceId, name, email, phone, message, isRead, readAt)

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

### PR 2.10.1: Storage & File Management Service ⭐ NEW
**Status**: Pending
**Dependencies**: PR 2.2 (Core Common Components) ✅
**Blocks**: PR 2.15 (Settings), PR 3.10 (Frontend Profile), PR 3.12 (Frontend Guest Pages)
**Estimated**: 2-3 weeks
**Priority**: 🔥 High (foundation for avatar, document, video uploads)

**Description**:
Implement comprehensive file storage service với S3-compatible storage (MinIO dev, AWS S3 prod), presigned URLs cho secure upload/download, storage quota tracking, và multi-tenant isolation.

**Features**:
- Direct client-to-S3 uploads via presigned URLs (bypass backend)
- Storage quota enforcement (Trial: 500MB, Basic: 5GB, Pro: 50GB)
- Multi-tenant isolation (bucket prefixes + instance_id)
- File lifecycle tracking (UPLOADING → PROCESSING → READY → FAILED)
- Access control (PRIVATE, COURSE, PUBLIC)
- Video metadata support (duration, resolution, codec)
- Soft delete với 30-day grace period
- Scheduled jobs: quota calculation, file retention cleanup

**Entities**:
- `UploadedFile` (id, instanceId, uploadedBy, fileType, originalFilename, storagePath, fileSizeBytes, mimeType, status, durationSeconds, resolution, videoCodec, accessLevel, relatedEntityType, relatedEntityId, deleted)
- `StorageQuota` (id, instanceId[unique], quotaBytes, usedBytes, lastCalculatedAt)

**Services**:
- `FileService` - Upload/download flows, validation, quota check
  - `InitiateUploadRequest initiateUpload(fileName, fileSize, fileType, mimeType)` → presigned upload URL (10min expiry)
  - `void completeUpload(fileId)` → mark file READY
  - `DownloadResponse generateDownloadUrl(fileId)` → presigned download URL (24h expiry)
  - `void softDelete(fileId)` → mark deleted=true
- `StorageQuotaService` - Quota calculation (scheduled), enforcement
  - `QuotaResponse getQuota(tenantId)` → current usage
  - `void recalculateQuotas()` → scheduled job (daily)
  - `boolean checkQuota(tenantId, fileSize)` → before upload
- `FileRetentionService` - Cleanup expired deleted files (scheduled)
  - `void cleanupExpiredFiles()` → delete files older than 30 days

**Controllers**:
- `FileController` - REST endpoints:
  - POST /api/v1/files/upload/initiate - Generate presigned upload URL
  - POST /api/v1/files/{id}/complete - Mark upload complete
  - GET /api/v1/files/{id}/download - Generate presigned download URL
  - DELETE /api/v1/files/{id} - Soft delete file
- `StorageQuotaController`:
  - GET /api/v1/storage/quota - Get tenant quota
  - POST /api/v1/storage/quota/recalculate - Manual recalculation (admin)

**DTOs**:
- `InitiateUploadRequest` (fileName, fileSize, fileType, mimeType)
- `InitiateUploadResponse` (uploadUrl, fileId, expiresIn)
- `CompleteUploadResponse` (fileId, status, downloadUrl)
- `DownloadResponse` (downloadUrl, expiresIn, fileName, fileSizeBytes, mimeType)
- `QuotaResponse` (quotaBytes, usedBytes, availableBytes, usagePercent)

**File Types**:
- AVATAR: max 10MB (image/png, image/jpeg, image/webp)
- DOCUMENT: max 50MB (application/pdf, .docx, .xlsx)
- VIDEO: max 2GB (video/mp4, video/webm)
- CERTIFICATE: max 5MB (application/pdf)
- ASSIGNMENT: max 50MB (application/pdf, .docx)

**Configuration**:
- S3Client bean (AWS SDK v2.20.26)
- S3Presigner bean
- Storage properties (endpoint, bucket, credentials, region)
- MinIO Docker service trong docker-compose.dev.yml
- Storage path format: `{tenant-id}/{file-type}/{uuid}.{ext}`

**Business Rules**:
- BR-STO-001: Quota check before generating presigned URL
- BR-STO-002: Files isolated by tenant (instance_id filter)
- BR-STO-003: Soft delete with 30-day retention
- BR-STO-004: Access control enforced on download
  - PRIVATE: Only uploaded_by user
  - COURSE: Teacher + enrolled students
  - PUBLIC: All authenticated users
- BR-STO-005: Video files require processing (status: UPLOADING → PROCESSING → READY)

**Testing**:
- 10+ unit tests minimum
- MinIO Testcontainer integration
- FileUploadIntegrationTest (full upload flow: initiate → upload → complete → download)
- StorageQuotaIntegrationTest (quota enforcement, exceeding limit)
- Multi-tenant isolation tests (cannot access other tenant's files)
- Presigned URL tests (expiry, invalid URLs)
- File retention tests (cleanup after 30 days)

**Database Migration**: `V13__create_file_storage_tables.sql`

**Documentation**:
- [`storage-service-design.md`](../implementation/storage-service-design.md) - Complete architecture, flows, testing guides
- Local testing guide (MinIO Console, curl examples)
- init-minio.sh script for local setup

**Acceptance Criteria**:
- [ ] Database migrations applied successfully
- [ ] Entities created với Hibernate filters
- [ ] FileService implements upload/download flows
- [ ] Presigned URLs generated correctly (10min upload, 24h download)
- [ ] Storage quota enforced before upload
- [ ] Multi-tenant isolation working (files isolated by instance_id)
- [ ] Integration tests passing (upload, download, quota, isolation)
- [ ] MinIO Testcontainer configured
- [ ] Local testing với MinIO Console successful
- [ ] Documentation complete

**Implementation Reference**: `documents/03-planning/implementation/storage-service-design.md` (3,623 lines)

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

## Phase 5: Trial Learning System (V4.1 Phase 2) ⭐ NEW

### PR 2.13: Trial Registration & Quota Management ⭐ NEW
**Status**: 📋 Planned (V4.1 Phase 2)
**Priority**: HIGH
**Estimated Effort**: 16-20 hours
**Dependencies**: Gateway PR 1.13 (TRIAL_USER support), Migration V12
**Blocks**: Frontend PR 3.13 (Trial UI)

#### Objective
Implement trial user registration, daily quota enforcement, and trial lesson access control.

#### Changes

**1. Lead Entity & Repository**

**File**: `com.kiteclass.core.module.lead.entity.Lead.java` (create)

```java
@Entity
@Table(name = "leads")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "instanceId", type = "uuid"))
@Filter(name = "tenantFilter", condition = "instance_id = :instanceId AND deleted = false")
public class Lead extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId; // FK to Gateway users.id

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private LeadSource source; // LANDING_PAGE, CONTACT_FORM, TRIAL_SIGNUP, REFERRAL

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeadStatus status; // NEW, CONTACTED, CONVERTED, LOST

    @Column(name = "course_interest_id")
    private Long courseInterestId;

    @Column(name = "registration_date", nullable = false)
    private Instant registrationDate;

    @Column(name = "converted_at")
    private Instant convertedAt;
}
```

**Repository**:
```java
public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Optional<Lead> findByEmailAndInstanceIdAndDeletedFalse(String email, UUID instanceId);
    Optional<Lead> findByUserIdAndDeletedFalse(UUID userId);
    List<Lead> findByInstanceIdAndStatusAndDeletedFalse(UUID instanceId, LeadStatus status, Pageable pageable);
}
```

**2. Trial Quota Entity & Repository**

**File**: `com.kiteclass.core.module.lead.entity.TrialQuota.java` (create)

```java
@Entity
@Table(name = "trial_quotas")
public class TrialQuota extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "quota_date", nullable = false)
    private LocalDate quotaDate;

    @Column(name = "lessons_accessed", nullable = false)
    private Integer lessonsAccessed = 0;

    @Column(name = "quota_limit", nullable = false)
    private Integer quotaLimit = 3; // Default 3 lessons/day
}
```

**Repository**:
```java
public interface TrialQuotaRepository extends JpaRepository<TrialQuota, UUID> {
    Optional<TrialQuota> findByUserIdAndQuotaDateAndInstanceId(UUID userId, LocalDate quotaDate, UUID instanceId);
}
```

**3. Lead Service**

**Interface**: `LeadService.java`
```java
public interface LeadService {
    LeadResponse registerForTrial(CreateLeadRequest request);
    LeadResponse getLeadByUserId(UUID userId);
}
```

**Implementation Highlights**:
- Check for duplicate email (per tenant)
- Call Gateway API to generate magic link
- Create Lead record (user_id set after magic link verification)
- Send email with magic link

**4. Trial Quota Service**

**Interface**: `TrialQuotaService.java`
```java
public interface TrialQuotaService {
    int checkAndIncrementQuota(UUID userId);
    QuotaStatus getQuotaStatus(UUID userId);
}
```

**Implementation Logic**:
```java
@Transactional
public int checkAndIncrementQuota(UUID userId) {
    UUID instanceId = TenantContext.getInstanceId();
    LocalDate today = LocalDate.now();

    // Find or create today's quota record
    TrialQuota quota = trialQuotaRepository
        .findByUserIdAndQuotaDateAndInstanceId(userId, today, instanceId)
        .orElseGet(() -> createNewQuota(userId, instanceId, today));

    // Check quota
    if (quota.getLessonsAccessed() >= quota.getQuotaLimit()) {
        throw new QuotaExceededException("TRIAL_QUOTA_EXCEEDED", quota.getQuotaLimit());
    }

    // Increment
    quota.setLessonsAccessed(quota.getLessonsAccessed() + 1);
    quota = trialQuotaRepository.save(quota);

    return quota.getQuotaLimit() - quota.getLessonsAccessed();
}
```

**5. Lesson Service Enhancement**

**Add method to LessonService**:
```java
/**
 * Get lesson details with access control
 * @param lessonId Lesson ID
 * @param userId User ID from JWT
 * @param userRole User role from JWT
 * @return Lesson details if user has access
 * @throws AccessDeniedException if trial user accessing paid lesson or quota exceeded
 */
LessonResponse getLessonWithAccessControl(Long lessonId, UUID userId, String userRole);
```

**Implementation**:
```java
@Override
public LessonResponse getLessonWithAccessControl(Long lessonId, UUID userId, String userRole) {
    Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
        .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", lessonId));

    // Trial user access control
    if ("TRIAL_USER".equals(userRole)) {
        // Check if lesson is trial-accessible
        if (!lesson.isTrialAccessible()) {
            throw new AccessDeniedException("TRIAL_USER_PAID_LESSON_ACCESS_DENIED");
        }

        // Check and increment quota
        int remainingQuota = trialQuotaService.checkAndIncrementQuota(userId);

        // Add quota info to response
        LessonResponse response = LessonMapper.toResponse(lesson);
        response.setRemainingQuota(remainingQuota);
        return response;
    }

    // Student/Teacher: check enrollment (existing logic)
    // ...

    return LessonMapper.toResponse(lesson);
}
```

**6. REST Endpoints**

**File**: `LeadController.java` (create)
```java
@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {

    @PostMapping("/register-trial")
    public ResponseEntity<LeadResponse> registerForTrial(@RequestBody @Valid CreateLeadRequest request) {
        LeadResponse response = leadService.registerForTrial(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TRIAL_USER')")
    public ResponseEntity<LeadResponse> getMyLead(@RequestHeader("X-User-Id") UUID userId) {
        LeadResponse response = leadService.getLeadByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/quota")
    @PreAuthorize("hasRole('TRIAL_USER')")
    public ResponseEntity<QuotaStatus> getQuotaStatus(@RequestHeader("X-User-Id") UUID userId) {
        QuotaStatus status = trialQuotaService.getQuotaStatus(userId);
        return ResponseEntity.ok(status);
    }
}
```

#### DTOs

**CreateLeadRequest**:
```java
public record CreateLeadRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    String name,

    @Size(max = 20)
    String phone,

    Long courseInterestId
) {}
```

**QuotaStatus**:
```java
public record QuotaStatus(
    int lessonsAccessed,
    int quotaLimit,
    int remaining,
    LocalDate quotaDate
) {}
```

#### Testing

**Unit Tests**:
- LeadServiceTest: Test registration, duplicate email check
- TrialQuotaServiceTest: Test quota enforcement, daily reset
- LessonServiceTest: Test trial access control

**Integration Tests**:
- Test trial registration flow end-to-end
- Test quota exceeded scenario
- Test multi-tenant isolation (trial user can't access other tenant's lessons)

#### Error Codes

- `LEAD_EMAIL_EXISTS`: Email already registered for trial
- `TRIAL_QUOTA_EXCEEDED`: Daily lesson quota (3) exceeded
- `TRIAL_USER_PAID_LESSON_ACCESS_DENIED`: Trial user trying to access paid lesson
- `LESSON_NOT_FOUND`: Lesson ID invalid

**Last Updated**: 2026-02-26

---

### PR 2.14: Lead to Student Conversion ⭐ NEW
**Status**: 📋 Planned (V4.1 Phase 2)
**Priority**: MEDIUM
**Estimated Effort**: 12-16 hours
**Dependencies**: Core PR 2.13 (Trial Registration)
**Blocks**: Frontend PR 3.14 (Conversion Flow UI)

#### Objective
Implement Lead→Student conversion workflow with payment verification and progress preservation.

#### Changes

**1. Payment Service Integration (Mock for Phase 1)**

**Interface**: `PaymentService.java` (create)
```java
public interface PaymentService {
    /**
     * Verify payment completed for user
     * @param userId User ID
     * @param courseId Course ID purchased
     * @return Payment details
     * @throws PaymentNotCompletedException if payment not found or incomplete
     */
    PaymentVerificationResponse verifyPayment(UUID userId, Long courseId);
}
```

**Mock Implementation (Phase 1)**:
```java
@Service
@Profile("!prod")
public class MockPaymentService implements PaymentService {

    @Override
    public PaymentVerificationResponse verifyPayment(UUID userId, Long courseId) {
        // Mock: Always return success for testing
        return new PaymentVerificationResponse(
            UUID.randomUUID(), // transactionId
            BigDecimal.valueOf(299000), // amount
            "VND",
            PaymentStatus.COMPLETED,
            Instant.now()
        );
    }
}
```

**2. Lead Service - Conversion Method**

**Add to LeadService interface**:
```java
/**
 * Convert lead to student after payment completion
 * @param leadId Lead ID
 * @param request Conversion request with payment info
 * @return Conversion result with new student role
 */
ConversionResponse convertToStudent(UUID leadId, ConvertLeadRequest request);
```

**Implementation**:
```java
@Override
@Transactional
public ConversionResponse convertToStudent(UUID leadId, @Valid ConvertLeadRequest request) {
    UUID instanceId = TenantContext.getInstanceId();

    // Get lead
    Lead lead = leadRepository.findByIdAndDeletedFalse(leadId)
        .orElseThrow(() -> new EntityNotFoundException("LEAD_NOT_FOUND", leadId));

    // Check lead status
    if (lead.getStatus() == LeadStatus.CONVERTED) {
        throw new ValidationException("LEAD_ALREADY_CONVERTED");
    }

    // Verify payment completed
    PaymentVerificationResponse payment = paymentService.verifyPayment(
        lead.getUserId(),
        request.getCourseId()
    );

    // Call Gateway API to update user role: TRIAL_USER → STUDENT
    gatewayClient.updateUserRole(lead.getUserId(), "STUDENT");

    // Update lead status
    lead.setStatus(LeadStatus.CONVERTED);
    lead.setConvertedAt(Instant.now());
    leadRepository.save(lead);

    // Create enrollment (existing enrollment service)
    EnrollmentResponse enrollment = enrollmentService.createEnrollment(
        new CreateEnrollmentRequest(
            null, // studentId not needed, use userId
            request.getCourseId(),
            null  // classId optional
        ),
        lead.getUserId() // Use existing user_id
    );

    return new ConversionResponse(
        lead.getUserId(),
        "STUDENT",
        enrollment.getId(),
        payment.getTransactionId()
    );
}
```

**3. Gateway Client**

**File**: `GatewayClient.java`

**Add method**:
```java
/**
 * Update user role in Gateway service
 * @param userId User ID
 * @param newRole New role (e.g., "STUDENT")
 */
void updateUserRole(UUID userId, String newRole);
```

**Implementation (using WebClient)**:
```java
@Override
public void updateUserRole(UUID userId, String newRole) {
    webClient.put()
        .uri("/api/v1/users/{userId}/role", userId)
        .bodyValue(Map.of("role", newRole))
        .retrieve()
        .bodyToMono(Void.class)
        .block();
}
```

**4. REST Endpoint**

**Add to LeadController**:
```java
@PostMapping("/{leadId}/convert")
@PreAuthorize("hasRole('TRIAL_USER')")
public ResponseEntity<ConversionResponse> convertToStudent(
    @PathVariable UUID leadId,
    @RequestBody @Valid ConvertLeadRequest request,
    @RequestHeader("X-User-Id") UUID userId
) {
    ConversionResponse response = leadService.convertToStudent(leadId, request);
    return ResponseEntity.ok(response);
}
```

#### DTOs

**ConvertLeadRequest**:
```java
public record ConvertLeadRequest(
    @NotNull(message = "Course ID is required")
    Long courseId,

    @NotNull(message = "Payment transaction ID is required")
    UUID paymentTransactionId
) {}
```

**ConversionResponse**:
```java
public record ConversionResponse(
    UUID userId,
    String newRole,
    Long enrollmentId,
    UUID paymentTransactionId,
    Instant convertedAt
) {}
```

#### Progress Preservation Logic

**Key insight**: Progress is preserved automatically because:
1. `lesson_progress` table uses `user_id` (not student_id)
2. Conversion keeps same `user_id`, only changes `role`
3. No data migration needed

**Verification query**:
```sql
-- After conversion, student can see all previous progress
SELECT lp.lesson_id, lp.progress_percent, lp.completed
FROM lesson_progress lp
WHERE lp.user_id = :userId; -- Same user_id before and after conversion
```

#### Testing

**Unit Tests**:
- LeadServiceTest: Test conversion success
- LeadServiceTest: Test conversion with payment failure
- LeadServiceTest: Test already converted lead

**Integration Tests**:
- Test full flow: trial registration → access 3 lessons → payment → conversion → verify progress preserved
- Test multi-tenant isolation

#### Error Codes

- `LEAD_NOT_FOUND`: Lead ID invalid
- `LEAD_ALREADY_CONVERTED`: Lead already converted to student
- `PAYMENT_NOT_COMPLETED`: Payment verification failed
- `GATEWAY_USER_UPDATE_FAILED`: Failed to update user role in Gateway

#### Design Notes

**Why UPDATE ROLE instead of CREATE NEW STUDENT?**
- Preserve progress (same user_id)
- Preserve audit trail (created_at, updated_at)
- Simpler implementation (no data migration)
- Lead record kept for analytics (conversion funnel tracking)

**Why keep Lead record after conversion?**
- Analytics: Track conversion rate, source attribution
- Audit trail: When/how user converted
- Sales reporting: Lead source performance

**Last Updated**: 2026-02-26

---

### PR 2.15: Settings & Preferences ⏳
**Status**: Pending (moved from PR 2.9, renumbered from 2.13)
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

### PR 2.16: Core Docker & Final Integration ⏳
**Status**: Pending (moved from PR 2.10, renumbered from 2.14)
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

**Total PRs**: 19 (2 new V4.1 Phase 2)
**Completed**: 8 (42%)
**In Progress**: 1 (PR 2.6)
**Planned**: 2 (PR 2.13-2.14 Trial Learning)
**Pending**: 8

**By Phase**:
- Phase 1 (Foundation): 2/2 ✅
- Phase 2 (Core Modules): 5/9 (56%)
- Phase 3 (V4.1 New Modules): 0/2 (0%)
- Phase 4 (Infrastructure): 2/4 (50%)
- Phase 5 (V4.1 Trial Learning): 0/2 (0%) ⭐ NEW

**Test Coverage**: 292 tests passing (260 unit + 32 integration)

**Next 3 PRs**:
1. PR 2.6: Enrollment Module (in progress)
2. PR 2.7: Attendance Module
3. PR 2.9: LMS Module (guest features)

---

**Last Updated**: 2026-02-26
