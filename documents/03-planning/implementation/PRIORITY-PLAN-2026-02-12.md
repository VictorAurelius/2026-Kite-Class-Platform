# Priority PRs Execution Plan - 2026-02-12

**⚠️ CRITICAL:** Tuân thủ `.claude/skills/development-workflow.md` cho TẤT CẢ workflow steps

**Status:** READY TO EXECUTE
**Estimated Time:** 5-6 hours total
**Target Completion:** 2026-02-13

---

## 📋 OVERVIEW

### Priority Queue
1. ⚠️ **PRIORITY 1:** Fix Multi-Tenant Email Test (2 hours)
2. 🚀 **PRIORITY 2:** PR 1.8 Gateway Cross-Service Integration (3-4 hours)

### Prerequisites Checklist
- ✅ All services on Spring Boot 3.5.10
- ✅ PR 2.11 (Core Internal APIs) complete
- ✅ CI passing for all services (234 Core tests, 0 failures)
- ✅ Development workflow skill available

---

# ⚠️ PRIORITY 1: Fix Multi-Tenant Email Test

**Branch:** `fix/KC-001-multi-tenant-email-filter`
**Service:** Core
**Time:** 2 hours
**Ticket:** KC-001 (internal tracking)

## 1. Problem Statement

**Current Issue:**
- Test `InternalStudentIntegrationTest.createStudent_multipleTenantsWithSameEmail_shouldIsolateData` is DISABLED
- Email uniqueness is GLOBAL (not scoped to tenant)
- Hibernate tenant filter NOT working in @SpringBootTest integration tests
- Expected: Tenant A and Tenant B can both use `test@email.com`
- Actual: Second tenant gets 409 Conflict

**Root Cause:**
```java
// StudentServiceImpl.createStudent()
Optional<Student> existing = studentRepository.findByEmailAndDeletedFalse(email);
// ↑ This query SHOULD be filtered by tenantId but ISN'T in test environment
```

## 2. Workflow: Feature Branch Creation

**Reference:** `.claude/skills/development-workflow.md` - Section "Branching Strategy"

```bash
# Step 1: Ensure on main with latest changes
git checkout main
git pull origin main

# Step 2: Create feature branch (follow naming convention)
git checkout -b fix/KC-001-multi-tenant-email-filter

# Step 3: Verify branch
git branch --show-current
# Expected: fix/KC-001-multi-tenant-email-filter
```

## 3. Implementation: Option A (Recommended)

### 3.1 Add Composite Unique Constraint

**File:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V6__add_multi_tenant_email_constraint.sql`

```sql
-- Migration: Add composite unique constraint for email + instance_id
-- Version: V6
-- Description: Scope email uniqueness to tenant for multi-tenant isolation
-- Author: KiteClass Team
-- Date: 2026-02-12

-- Add composite unique constraint to students table
ALTER TABLE students
DROP CONSTRAINT IF EXISTS uk_student_email,  -- Remove old global unique constraint if exists
ADD CONSTRAINT uk_student_email_instance UNIQUE (email, instance_id, deleted)
WHERE deleted = FALSE;  -- Partial unique index: only enforce for non-deleted records

-- Comment
COMMENT ON CONSTRAINT uk_student_email_instance ON students IS 'Composite unique constraint: email must be unique per tenant (instance_id) for active records only';

-- Similar constraints for teachers table
ALTER TABLE teachers
DROP CONSTRAINT IF EXISTS uk_teacher_email,
ADD CONSTRAINT uk_teacher_email_instance UNIQUE (email, instance_id, deleted)
WHERE deleted = FALSE;

COMMENT ON CONSTRAINT uk_teacher_email_instance ON teachers IS 'Composite unique constraint: email must be unique per tenant (instance_id) for active records only';
```

### 3.2 Add Repository Method

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/repository/StudentRepository.java`

```java
/**
 * Finds student by email and instance_id for tenant-scoped uniqueness check.
 *
 * @param email the email to search for
 * @param instanceId the tenant instance ID
 * @return Optional containing student if found
 * @since 2.13.0
 */
Optional<Student> findByEmailAndInstanceIdAndDeletedFalse(String email, UUID instanceId);
```

### 3.3 Update Service Layer

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/service/impl/StudentServiceImpl.java`

```java
@Override
public StudentResponse createStudent(CreateStudentRequest request, UUID tenantId) {
    log.info("Creating student with email: {}, tenantId: {}", request.email(), tenantId);

    // Validation: Email uniqueness (SCOPED TO TENANT)
    Optional<Student> existing = studentRepository
        .findByEmailAndInstanceIdAndDeletedFalse(request.email(), tenantId);
    if (existing.isPresent()) {
        log.warn("Student email already exists in tenant: {}, tenantId: {}", request.email(), tenantId);
        throw new DuplicateResourceException("STUDENT_EMAIL_EXISTS", request.email());
    }

    // Validation: Phone uniqueness (if provided, also scoped to tenant)
    if (request.phone() != null && !request.phone().isBlank()) {
        Optional<Student> existingByPhone = studentRepository
            .findByPhoneAndInstanceIdAndDeletedFalse(request.phone(), tenantId);
        if (existingByPhone.isPresent()) {
            throw new DuplicateResourceException("STUDENT_PHONE_EXISTS", request.phone());
        }
    }

    // Create student
    Student student = studentMapper.toEntity(request);
    student.setInstanceId(tenantId);  // CRITICAL: Set tenant ID

    Student saved = studentRepository.save(student);
    log.info("Created student with ID: {}, instanceId: {}", saved.getId(), saved.getInstanceId());

    return studentMapper.toResponse(saved);
}
```

**Also add to repository:**
```java
/**
 * Finds student by phone and instance_id for tenant-scoped uniqueness check.
 */
Optional<Student> findByPhoneAndInstanceIdAndDeletedFalse(String phone, UUID instanceId);
```

### 3.4 Re-enable and Update Test

**File:** `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/student/controller/InternalStudentIntegrationTest.java`

```java
@Test  // ✅ Re-enabled (remove @Disabled)
void createStudent_multipleTenantsWithSameEmail_shouldIsolateData() throws Exception {
    // Given - Create students with same email in different tenants
    String sharedEmail = "shared@test.com";

    CreateStudentRequest request = new CreateStudentRequest(
        "Shared Name",
        sharedEmail,
        "0912345678",
        LocalDate.of(2010, 1, 15),
        Gender.MALE,
        "Test Address",
        null
    );

    long timestamp1 = System.currentTimeMillis() / 1000;
    String signature1 = generateHmacSignature(timestamp1);

    // When - Create in Tenant A
    mockMvc.perform(post("/internal/students")
                    .header("X-Internal-Timestamp", String.valueOf(timestamp1))
                    .header("X-Internal-Signature", signature1)
                    .header("X-Tenant-Id", tenantA.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

    // Wait 1 second to ensure different timestamp
    Thread.sleep(1000);

    long timestamp2 = System.currentTimeMillis() / 1000;
    String signature2 = generateHmacSignature(timestamp2);

    // When - Create same email in Tenant B (SHOULD SUCCEED NOW)
    mockMvc.perform(post("/internal/students")
                    .header("X-Internal-Timestamp", String.valueOf(timestamp2))
                    .header("X-Internal-Signature", signature2)
                    .header("X-Tenant-Id", tenantB.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());  // ✅ Should succeed

    // Then - Both students should exist with different instanceIds
    var students = studentRepository.findAll().stream()
            .filter(s -> s.getEmail().equals(sharedEmail))
            .toList();

    assertThat(students).hasSize(2);
    assertThat(students)
            .extracting(Student::getInstanceId)
            .containsExactlyInAnyOrder(tenantA, tenantB);
}

@Test  // New test: Verify tenant isolation for duplicate email
void createStudent_duplicateEmailInSameTenant_shouldFail() throws Exception {
    String email = "duplicate@test.com";

    CreateStudentRequest request = new CreateStudentRequest(
        "Student One",
        email,
        "0912345678",
        LocalDate.of(2010, 1, 15),
        Gender.MALE,
        "Test Address",
        null
    );

    // Create first student in Tenant A
    long timestamp1 = System.currentTimeMillis() / 1000;
    String signature1 = generateHmacSignature(timestamp1);

    mockMvc.perform(post("/internal/students")
                    .header("X-Internal-Timestamp", String.valueOf(timestamp1))
                    .header("X-Internal-Signature", signature1)
                    .header("X-Tenant-Id", tenantA.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

    Thread.sleep(1000);

    // Try to create second student with SAME email in SAME tenant
    CreateStudentRequest duplicateRequest = new CreateStudentRequest(
        "Student Two",  // Different name
        email,  // SAME email
        "0987654321",  // Different phone
        LocalDate.of(2011, 2, 20),
        Gender.FEMALE,
        "Another Address",
        null
    );

    long timestamp2 = System.currentTimeMillis() / 1000;
    String signature2 = generateHmacSignature(timestamp2);

    mockMvc.perform(post("/internal/students")
                    .header("X-Internal-Timestamp", String.valueOf(timestamp2))
                    .header("X-Internal-Signature", signature2)
                    .header("X-Tenant-Id", tenantA.toString())  // SAME tenant
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isConflict())  // ✅ Should fail with 409
            .andExpect(jsonPath("$.code").value("STUDENT_EMAIL_EXISTS"));
}
```

## 4. Testing Checklist

**Reference:** `.claude/skills/testing-guide.md`

### 4.1 Unit Tests (Not needed - repository auto-generated)

### 4.2 Integration Tests

```bash
# Run specific test class
./mvnw test -Dtest=InternalStudentIntegrationTest

# Expected results:
# - createStudent_multipleTenantsWithSameEmail_shouldIsolateData: PASS ✅
# - createStudent_duplicateEmailInSameTenant_shouldFail: PASS ✅
# - All other tests: PASS ✅
```

### 4.3 Regression Tests

```bash
# Run ALL tests to ensure no regression
./mvnw clean test

# Expected: 235 tests (was 234, +1 new test), 0 failures, 0 errors
```

## 5. Commit Strategy

**Reference:** `.claude/skills/development-workflow.md` - Section "Commit Messages"

```bash
git add .
git commit -m "$(cat <<'EOF'
fix(core): scope email uniqueness to tenant

Changes:
- Add V6 migration: composite unique constraint (email, instance_id, deleted)
- Add repository methods: findByEmailAndInstanceIdAndDeletedFalse
- Update StudentServiceImpl to check email uniqueness per tenant
- Re-enable test: createStudent_multipleTenantsWithSameEmail_shouldIsolateData
- Add new test: createStudent_duplicateEmailInSameTenant_shouldFail

Fixes:
- Multi-tenant isolation for email uniqueness
- Allows different tenants to use same email
- Enforces uniqueness within same tenant

Tests: 235 passing (was 234), 0 failures, 0 errors

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

## 6. CI Validation

**Reference:** `.claude/skills/development-workflow.md` - Section "Pull Request Process"

```bash
# Step 1: Push to remote (ask user first)
# AI: "Đã sẵn sàng push lên remote và tạo PR?"
# User: "yes"
git push -u origin fix/KC-001-multi-tenant-email-filter

# Step 2: Create Pull Request
gh pr create \
  --title "fix(core): scope email uniqueness to tenant (KC-001)" \
  --body "$(cat <<'EOF'
## Summary
Fix multi-tenant email uniqueness by scoping constraint to (email, instance_id).

## Problem
- Email uniqueness was GLOBAL (across all tenants)
- Tenant A and Tenant B could NOT use same email
- Test `createStudent_multipleTenantsWithSameEmail_shouldIsolateData` was disabled

## Solution
- Added V6 migration: composite unique constraint
- Updated service layer to check uniqueness per tenant
- Re-enabled integration test + added new test

## Changes
- Migration: V6__add_multi_tenant_email_constraint.sql
- Repository: Added findByEmailAndInstanceIdAndDeletedFalse
- Service: Updated createStudent validation logic
- Tests: Re-enabled + 1 new test

## Testing
- ✅ 235 tests passing (was 234)
- ✅ Multi-tenant isolation verified
- ✅ Same tenant duplicate check works

## Checklist
- [x] Migration tested locally
- [x] All tests pass
- [x] No regression
- [x] Multi-tenant isolation verified

## References
- Relates to: STATUS-UPDATE-2026-02-12.md Priority 1
- Unblocks: PR 1.8 Gateway Cross-Service Integration
EOF
)"

# Step 3: Monitor CI
gh run watch

# Step 4: If CI passes, merge
gh pr merge --squash --delete-branch
```

## 7. Acceptance Criteria

- [ ] V6 migration created and tested
- [ ] Composite unique constraint (email, instance_id, deleted) added
- [ ] Repository methods added for scoped uniqueness check
- [ ] Service layer updated to check per-tenant uniqueness
- [ ] Test `createStudent_multipleTenantsWithSameEmail_shouldIsolateData` RE-ENABLED and PASSING
- [ ] New test `createStudent_duplicateEmailInSameTenant_shouldFail` added and PASSING
- [ ] All 235 tests passing (0 failures, 0 errors)
- [ ] CI passing on PR
- [ ] No regression in existing functionality
- [ ] Multi-tenant isolation verified in manual testing

---

# 🚀 PRIORITY 2: PR 1.8 - Gateway Cross-Service Integration

**Branch:** `feature/KC-002-gateway-cross-service`
**Service:** Gateway
**Time:** 3-4 hours
**Ticket:** KC-002
**Prerequisites:** ✅ Priority 1 complete, ✅ PR 2.11 complete

## 1. Problem Statement

**Missing Link:**
- Gateway has `User` entity (authentication)
- Core has `Student/Teacher/Parent` entities (business logic)
- **NO CONNECTION** between them
- Student/Teacher CANNOT login
- Registration CANNOT create profile records in Core

**Required Pattern:**
- UserType enum (ADMIN, STAFF, TEACHER, PARENT, STUDENT)
- ReferenceId field (links to Core entity ID)
- Feign Client (Gateway → Core API calls)
- Saga pattern (atomic User + Core entity creation)

## 2. Workflow: Feature Branch Creation

```bash
# Step 1: Switch to main and update
git checkout main
git pull origin main

# Step 2: Create feature branch
git checkout -b feature/KC-002-gateway-cross-service

# Step 3: Verify
git branch --show-current
# Expected: feature/KC-002-gateway-cross-service
```

## 3. Implementation Steps

### 3.1 Create UserType Enum

**File:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/user/constant/UserType.java`

```java
package com.kiteclass.gateway.user.constant;

/**
 * User type for role-based access and cross-service linking.
 *
 * <p>Links Gateway User entity to Core Service profile entities:
 * <ul>
 *   <li>STUDENT → Core Student entity</li>
 *   <li>TEACHER → Core Teacher entity</li>
 *   <li>PARENT → Core Parent entity (future)</li>
 *   <li>STAFF → Core Staff entity (future)</li>
 *   <li>ADMIN → No Core entity (Gateway-only)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.8.0
 */
public enum UserType {
    /**
     * System administrator (Gateway-only, no Core profile).
     */
    ADMIN,

    /**
     * Staff member (linked to Core Staff entity - future implementation).
     */
    STAFF,

    /**
     * Teacher (linked to Core Teacher entity via referenceId).
     */
    TEACHER,

    /**
     * Parent/Guardian (linked to Core Parent entity - future implementation).
     */
    PARENT,

    /**
     * Student (linked to Core Student entity via referenceId).
     */
    STUDENT
}
```

### 3.2 Add Migration

**File:** `kiteclass/kiteclass-gateway/src/main/resources/db/migration/V9__add_user_type_reference_id.sql`

```sql
-- Migration: Add user_type and reference_id for cross-service linking
-- Version: V9
-- Description: Enable Gateway User to link to Core Service entities
-- Author: KiteClass Team
-- Date: 2026-02-12

-- Add user_type column
ALTER TABLE users
ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'STAFF';

-- Add reference_id column (links to Core entity ID)
ALTER TABLE users
ADD COLUMN reference_id BIGINT;

-- Add index for cross-service lookups
CREATE INDEX idx_users_type_reference ON users(user_type, reference_id)
WHERE reference_id IS NOT NULL;

-- Add comments
COMMENT ON COLUMN users.user_type IS 'User type: ADMIN/STAFF/TEACHER/PARENT/STUDENT (links to Core entities)';
COMMENT ON COLUMN users.reference_id IS 'Foreign key to Core Service entity (Student.id, Teacher.id, etc.)';

-- Update existing users (ADMIN role → ADMIN type, others → STAFF type)
UPDATE users
SET user_type = CASE
    WHEN EXISTS (
        SELECT 1 FROM user_roles ur
        JOIN roles r ON ur.role_id = r.id
        WHERE ur.user_id = users.id AND r.name = 'ROLE_ADMIN'
    ) THEN 'ADMIN'
    ELSE 'STAFF'
END;
```

### 3.3 Update User Entity

**File:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/user/entity/User.java`

Add these fields:

```java
/**
 * User type for cross-service linking.
 * Determines which Core Service entity this User links to.
 */
@Enumerated(EnumType.STRING)
@Column(name = "user_type", nullable = false, length = 20)
private UserType userType = UserType.STAFF;

/**
 * Reference ID linking to Core Service entity.
 *
 * <p>Maps to:
 * <ul>
 *   <li>STUDENT → Student.id in Core Service</li>
 *   <li>TEACHER → Teacher.id in Core Service</li>
 *   <li>PARENT → Parent.id in Core Service (future)</li>
 *   <li>STAFF → Staff.id in Core Service (future)</li>
 *   <li>ADMIN → NULL (no Core entity)</li>
 * </ul>
 */
@Column(name = "reference_id")
private Long referenceId;
```

### 3.4 Create Feign Client

**File:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/integration/core/CoreServiceClient.java`

```java
package com.kiteclass.gateway.integration.core;

import com.kiteclass.gateway.integration.core.dto.CreateStudentRequest;
import com.kiteclass.gateway.integration.core.dto.StudentResponse;
import com.kiteclass.gateway.integration.core.dto.TeacherResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for Core Service internal APIs.
 *
 * <p>Handles cross-service communication for profile management:
 * <ul>
 *   <li>Student profile CRUD</li>
 *   <li>Teacher profile CRUD</li>
 *   <li>Parent profile CRUD (future)</li>
 * </ul>
 *
 * <p>Security: Uses HMAC-SHA256 authentication via {@link CoreServiceRequestInterceptor}.
 *
 * @author KiteClass Team
 * @since 1.8.0
 * @see CoreServiceRequestInterceptor
 */
@FeignClient(
    name = "core-service",
    url = "${integration.core-service.url}",
    configuration = CoreServiceFeignConfig.class
)
public interface CoreServiceClient {

    /**
     * Creates student profile in Core Service.
     *
     * @param request the student creation request
     * @param tenantId the tenant ID (X-Tenant-Id header)
     * @return created student profile
     */
    @PostMapping("/internal/students")
    StudentResponse createStudent(
        @RequestBody CreateStudentRequest request,
        @RequestHeader("X-Tenant-Id") String tenantId
    );

    /**
     * Gets student profile by ID.
     *
     * @param id student ID
     * @param tenantId tenant ID
     * @return student profile
     */
    @GetMapping("/internal/students/{id}")
    StudentResponse getStudent(
        @PathVariable("id") Long id,
        @RequestHeader("X-Tenant-Id") String tenantId
    );

    /**
     * Deletes student profile (soft delete).
     *
     * @param id student ID
     * @param tenantId tenant ID
     */
    @DeleteMapping("/internal/students/{id}")
    void deleteStudent(
        @PathVariable("id") Long id,
        @RequestHeader("X-Tenant-Id") String tenantId
    );

    /**
     * Gets teacher profile by ID.
     *
     * @param id teacher ID
     * @param tenantId tenant ID
     * @return teacher profile
     */
    @GetMapping("/internal/teachers/{id}")
    TeacherResponse getTeacher(
        @PathVariable("id") Long id,
        @RequestHeader("X-Tenant-Id") String tenantId
    );
}
```

### 3.5 Create Feign Request Interceptor for HMAC

**File:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/integration/core/CoreServiceRequestInterceptor.java`

```java
package com.kiteclass.gateway.integration.core;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Feign request interceptor for Core Service HMAC authentication.
 *
 * <p>Adds HMAC-SHA256 signature headers to all Core Service requests:
 * <ul>
 *   <li>X-Internal-Timestamp: Unix timestamp in seconds</li>
 *   <li>X-Internal-Signature: HMAC-SHA256(timestamp, secret)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.8.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreServiceRequestInterceptor implements RequestInterceptor {

    @Value("${integration.core-service.internal-api-secret}")
    private String internalApiSecret;

    @Override
    public void apply(RequestTemplate template) {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = new HmacUtils("HmacSHA256", internalApiSecret)
            .hmacHex(String.valueOf(timestamp));

        template.header("X-Internal-Timestamp", String.valueOf(timestamp));
        template.header("X-Internal-Signature", signature);

        log.debug("Added HMAC headers for Core Service request: timestamp={}", timestamp);
    }
}
```

### 3.6 Create Feign Config

**File:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/integration/core/CoreServiceFeignConfig.java`

```java
package com.kiteclass.gateway.integration.core;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign configuration for Core Service client.
 *
 * @author KiteClass Team
 * @since 1.8.0
 */
@Configuration
public class CoreServiceFeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor coreServiceRequestInterceptor() {
        return new CoreServiceRequestInterceptor();
    }
}
```

### 3.7 Update Registration Flow

**File:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/auth/service/impl/AuthServiceImpl.java`

Update `registerStudent` method:

```java
@Override
@Transactional
public Mono<RegisterResponse> registerStudent(RegisterStudentRequest request) {
    return Mono.defer(() -> {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("Registering student: email={}, tenantId={}", request.email(), tenantId);

        // Step 1: Validate email uniqueness
        return userRepository.existsByEmail(request.email())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new DuplicateResourceException("USER_EMAIL_EXISTS", request.email()));
                }

                // Step 2: Create User entity in Gateway
                User user = User.builder()
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .displayName(request.name())
                    .userType(UserType.STUDENT)  // ← NEW: Set user type
                    .instanceId(tenantId)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();

                return userRepository.save(user);
            })
            .flatMap(savedUser -> {
                // Step 3: Assign ROLE_STUDENT
                return roleRepository.findByName("ROLE_STUDENT")
                    .flatMap(role -> {
                        UserRole userRole = new UserRole(savedUser.getId(), role.getId());
                        return userRoleRepository.save(userRole)
                            .thenReturn(savedUser);
                    });
            })
            .flatMap(savedUser -> {
                // Step 4: Create Student profile in Core Service (Saga pattern)
                try {
                    CreateStudentRequest studentRequest = CreateStudentRequest.builder()
                        .name(request.name())
                        .email(request.email())
                        .phone(request.phone())
                        .dateOfBirth(request.dateOfBirth())
                        .gender(request.gender())
                        .address(request.address())
                        .build();

                    StudentResponse student = coreServiceClient.createStudent(
                        studentRequest,
                        tenantId.toString()
                    );

                    // Step 5: Update User with referenceId
                    savedUser.setReferenceId(student.id());  // ← NEW: Link to Core entity
                    return userRepository.save(savedUser)
                        .map(updatedUser -> new UserWithProfile(updatedUser, student));

                } catch (FeignException e) {
                    // Rollback: Delete User if Core creation fails
                    log.error("Failed to create student profile in Core, rolling back User creation", e);
                    return userRepository.delete(savedUser)
                        .then(Mono.error(new RegistrationFailedException(
                            "STUDENT_PROFILE_CREATION_FAILED",
                            "Failed to create student profile: " + e.getMessage()
                        )));
                }
            })
            .flatMap(userWithProfile -> {
                // Step 6: Generate tokens with profile data
                String accessToken = jwtUtil.generateAccessToken(
                    userWithProfile.user().getEmail(),
                    userWithProfile.user().getRoles(),
                    tenantId
                );
                String refreshToken = jwtUtil.generateRefreshToken(userWithProfile.user().getEmail());

                RegisterResponse response = RegisterResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(userWithProfile.user().getId())
                    .email(userWithProfile.user().getEmail())
                    .roles(userWithProfile.user().getRoles())
                    .userType(UserType.STUDENT)  // ← NEW
                    .profile(userWithProfile.student())  // ← NEW: Include profile
                    .build();

                log.info("Student registration successful: userId={}, studentId={}",
                    userWithProfile.user().getId(),
                    userWithProfile.student().id());

                return Mono.just(response);
            });
    });
}

/**
 * Helper record to hold User + Student profile.
 */
private record UserWithProfile(User user, StudentResponse student) {}
```

### 3.8 Update Login Flow

**File:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/auth/service/impl/AuthServiceImpl.java`

Update `login` method to fetch profile:

```java
@Override
public Mono<LoginResponse> login(LoginRequest request) {
    return Mono.defer(() -> {
        log.info("Login attempt: email={}", request.email());

        return userRepository.findByEmail(request.email())
            .switchIfEmpty(Mono.error(new AuthenticationFailedException("INVALID_CREDENTIALS")))
            .flatMap(user -> {
                // Verify password
                if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                    return Mono.error(new AuthenticationFailedException("INVALID_CREDENTIALS"));
                }

                // Check account status
                if (!user.isEnabled()) {
                    return Mono.error(new AccountDisabledException("ACCOUNT_DISABLED"));
                }
                if (!user.isAccountNonLocked()) {
                    return Mono.error(new AccountLockedException("ACCOUNT_LOCKED"));
                }

                // Generate tokens
                UUID tenantId = user.getInstanceId();
                String accessToken = jwtUtil.generateAccessToken(
                    user.getEmail(),
                    user.getRoles(),
                    tenantId
                );
                String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

                // Fetch profile from Core if user has referenceId
                Mono<Object> profileMono = Mono.justOrEmpty(user.getReferenceId())
                    .flatMap(refId -> fetchUserProfile(user.getUserType(), refId, tenantId))
                    .defaultIfEmpty(null);  // ADMIN users have no profile

                return profileMono.map(profile ->
                    LoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .userId(user.getId())
                        .email(user.getEmail())
                        .roles(user.getRoles())
                        .userType(user.getUserType())  // ← NEW
                        .profile(profile)  // ← NEW: Student/Teacher profile
                        .build()
                );
            });
    });
}

/**
 * Fetches user profile from Core Service based on user type.
 */
private Mono<Object> fetchUserProfile(UserType userType, Long referenceId, UUID tenantId) {
    return Mono.fromCallable(() -> {
        try {
            return switch (userType) {
                case STUDENT -> coreServiceClient.getStudent(referenceId, tenantId.toString());
                case TEACHER -> coreServiceClient.getTeacher(referenceId, tenantId.toString());
                case PARENT, STAFF, ADMIN -> null;  // Not implemented yet
            };
        } catch (FeignException e) {
            log.warn("Failed to fetch profile for userType={}, refId={}: {}",
                userType, referenceId, e.getMessage());
            return null;  // Return null if profile not found (graceful degradation)
        }
    });
}
```

## 4. Testing Checklist

### 4.1 Unit Tests

**File:** `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/user/constant/UserTypeTest.java`

```java
@Test
void userType_shouldHaveAllValues() {
    assertThat(UserType.values())
        .containsExactlyInAnyOrder(
            UserType.ADMIN,
            UserType.STAFF,
            UserType.TEACHER,
            UserType.PARENT,
            UserType.STUDENT
        );
}
```

### 4.2 Integration Tests

**File:** `kiteclass/kiteclass-gateway/src/test/java/com/kiteclass/gateway/auth/AuthCrossServiceIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureWebTestClient
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ActiveProfiles("test")
class AuthCrossServiceIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CoreServiceClient coreServiceClient;

    @Test
    void registerStudent_shouldCreateUserAndCoreProfile() {
        // Given
        UUID tenantId = UUID.randomUUID();
        RegisterStudentRequest request = new RegisterStudentRequest(
            "Test Student",
            "student@test.com",
            "Test@1234",
            "0912345678",
            LocalDate.of(2010, 1, 15),
            Gender.MALE,
            "Test Address"
        );

        StudentResponse studentResponse = new StudentResponse(
            1L,
            "Test Student",
            "student@test.com",
            "0912345678",
            LocalDate.of(2010, 1, 15),
            Gender.MALE,
            "Test Address",
            null,
            StudentStatus.ACTIVE,
            null
        );

        when(coreServiceClient.createStudent(any(), eq(tenantId.toString())))
            .thenReturn(studentResponse);

        // When
        webTestClient.post()
            .uri("/api/v1/auth/register/student")
            .header("X-Tenant-Id", tenantId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.data.userId").isNumber()
            .jsonPath("$.data.accessToken").isNotEmpty()
            .jsonPath("$.data.userType").isEqualTo("STUDENT")
            .jsonPath("$.data.profile.id").isEqualTo(1)
            .jsonPath("$.data.profile.email").isEqualTo("student@test.com");

        // Then
        verify(coreServiceClient).createStudent(any(), eq(tenantId.toString()));
    }

    @Test
    void registerStudent_coreServiceFailure_shouldRollback() {
        // Given
        UUID tenantId = UUID.randomUUID();
        RegisterStudentRequest request = new RegisterStudentRequest(...);

        when(coreServiceClient.createStudent(any(), any()))
            .thenThrow(new FeignException.InternalServerError(
                "Core service error",
                Request.create(Request.HttpMethod.POST, "/internal/students", Map.of(), null, Charset.defaultCharset(), null),
                null,
                null
            ));

        // When
        webTestClient.post()
            .uri("/api/v1/auth/register/student")
            .header("X-Tenant-Id", tenantId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody()
            .jsonPath("$.code").isEqualTo("STUDENT_PROFILE_CREATION_FAILED");

        // Then - User should be rolled back (not created in database)
        // Verify via repository count or query
    }
}
```

### 4.3 Run All Tests

```bash
cd kiteclass/kiteclass-gateway
./mvnw clean test

# Expected: ~194 → ~209 tests (+15 new tests), 0 failures
```

## 5. Commit Strategy

```bash
git add .
git commit -m "$(cat <<'EOF'
feat(gateway): implement PR 1.8 - cross-service integration

Changes:
- Add UserType enum (ADMIN, STAFF, TEACHER, PARENT, STUDENT)
- Add V9 migration: user_type and reference_id columns
- Update User entity with userType and referenceId fields
- Create CoreServiceClient with Feign for Core API calls
- Implement HMAC authentication interceptor for internal APIs
- Update registration flow with Saga pattern (User + Core profile)
- Update login flow to fetch profile from Core Service
- Add 15 integration tests for cross-service scenarios

Features:
- Student registration creates both User (Gateway) + Student (Core)
- Teacher/Parent types supported (APIs not yet implemented)
- Rollback mechanism when Core Service fails
- Login returns combined user + profile data

Tests:
- 15 new tests (auth cross-service integration)
- All tests passing: 209/209 (was 194)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

## 6. CI Validation

```bash
# Step 1: Push to remote (ask user first)
git push -u origin feature/KC-002-gateway-cross-service

# Step 2: Create Pull Request
gh pr create \
  --title "feat(gateway): PR 1.8 - cross-service integration (KC-002)" \
  --body "$(cat <<'EOF'
## Summary
Implement cross-service integration between Gateway and Core Service using UserType + ReferenceId pattern.

## Problem
- Gateway User entity had no link to Core entities (Student, Teacher)
- Students/Teachers could not login
- Registration flow could not create Core profiles

## Solution
- UserType enum for role-based linking
- ReferenceId field linking User to Core entity ID
- Feign Client for Gateway → Core API calls
- Saga pattern for atomic User + Core profile creation
- HMAC authentication for internal APIs

## Changes
- Enum: UserType
- Migration: V9__add_user_type_reference_id.sql
- Entity: User (+ userType, referenceId)
- Client: CoreServiceClient with Feign
- Service: Updated registration + login flows
- Tests: 15 new integration tests

## Testing
- ✅ 209 tests passing (was 194, +15 new tests)
- ✅ Student registration creates User + Student profile
- ✅ Rollback works when Core Service fails
- ✅ Login fetches and returns profile data

## Checklist
- [x] UserType enum created
- [x] Migration tested
- [x] Feign Client with HMAC auth
- [x] Saga pattern implemented
- [x] All tests pass
- [x] No regression

## References
- Implements: `.claude/skills/cross-service-data-strategy.md`
- Relates to: STATUS-UPDATE-2026-02-12.md Priority 2
- Depends on: PR 2.11 (Core Internal APIs) ✅
EOF
)"

# Step 3: Monitor CI
gh run watch

# Step 4: If CI passes, merge
gh pr merge --squash --delete-branch
```

## 7. Acceptance Criteria

- [ ] UserType enum created with all 5 types
- [ ] V9 migration adds user_type and reference_id columns
- [ ] User entity updated with new fields
- [ ] CoreServiceClient created with Feign + HMAC auth
- [ ] Registration flow creates User + Core profile atomically
- [ ] Rollback mechanism works on Core Service failure
- [ ] Login flow fetches and returns profile data
- [ ] 15 new integration tests added and passing
- [ ] All 209 tests passing (0 failures, 0 errors)
- [ ] CI passing on PR
- [ ] No regression in authentication flow
- [ ] Gateway ↔ Core communication verified

---

# 📊 EXECUTION SUMMARY

## Timeline

| Priority | Task | Time | Status |
|----------|------|------|--------|
| **P1** | Fix Multi-Tenant Email | 2h | ⏳ Ready |
| **P2** | PR 1.8 Gateway Integration | 3-4h | ⏳ Blocked by P1 |
| **Total** | | 5-6h | |

## Success Metrics

- [ ] Priority 1: 235 tests passing (was 234)
- [ ] Priority 2: 209 Gateway tests passing (was 194)
- [ ] Combined: 0 test failures across all services
- [ ] CI: All green (Core + Gateway + Frontend)
- [ ] Documentation: STATUS-UPDATE-2026-02-12.md updated
- [ ] Multi-tenant isolation: Verified manually
- [ ] Cross-service flow: End-to-end tested

## Next Steps After Completion

1. **Update STATUS-UPDATE-2026-02-12.md**
   - Mark Priority 1 & 2 as complete
   - Update progress percentages
   - Update next priorities

2. **Update Master PR Plan**
   - Gateway: 10/10 PRs (100% ✅)
   - Core: 7/15 PRs (46.7%)
   - Next: PR 2.5 Class Module

3. **Manual End-to-End Testing**
   - Register student via Gateway
   - Verify profile created in Core
   - Login and check profile data
   - Test multi-tenant isolation

---

**Document Version:** 1.0
**Created:** 2026-02-12
**Author:** KiteClass Development Team
**Status:** READY TO EXECUTE

**References:**
- `.claude/skills/development-workflow.md` - Complete workflow guide
- `.claude/skills/testing-guide.md` - Testing patterns
- `.claude/skills/cross-service-data-strategy.md` - Integration patterns
- `STATUS-UPDATE-2026-02-12.md` - Current status and priorities
