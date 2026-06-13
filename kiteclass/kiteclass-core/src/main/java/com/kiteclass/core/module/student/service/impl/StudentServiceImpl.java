package com.kiteclass.core.module.student.service.impl;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.auth.service.AuthCredentialProvisioningService;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.mapper.StudentMapper;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of StudentService interface.
 *
 * <p>Handles:
 * <ul>
 *   <li>Business logic validation (email/phone uniqueness)</li>
 *   <li>Entity mapping via StudentMapper</li>
 *   <li>Caching with Redis</li>
 *   <li>Transaction management</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final AuthCredentialProvisioningService credentialProvisioning;

    /**
     * Tạo học viên mới.
     *
     * <p>Validates email and phone uniqueness before creating student.
     *
     * @param request Thông tin học viên cần tạo (name, email, phone, dateOfBirth, gender, address)
     * @return StudentResponse chứa thông tin học viên đã tạo
     * @throws DuplicateResourceException nếu email hoặc phone đã tồn tại trong hệ thống
     */
    @Override
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request, java.util.UUID tenantId) {
        log.info("Creating student with email: {}, tenantId: {}", request.email(), tenantId);

        // Validate email uniqueness within tenant (multi-tenant isolation)
        if (request.email() != null && studentRepository.existsByEmailAndInstanceIdAndDeletedFalse(request.email(), tenantId)) {
            log.warn("Duplicate student email within tenant: {}, tenantId: {}", request.email(), tenantId);
            throw new DuplicateResourceException("STUDENT_EMAIL_EXISTS", (Object) request.email());
        }

        // Validate phone uniqueness within tenant (GAP-799 — was global, leaked cross-tenant)
        if (request.phone() != null && studentRepository.existsByPhoneAndInstanceIdAndDeletedFalse(request.phone(), tenantId)) {
            log.warn("Duplicate student phone within tenant: {}, tenantId: {}", request.phone(), tenantId);
            throw new DuplicateResourceException("STUDENT_PHONE_EXISTS", (Object) request.phone());
        }

        Student student = studentMapper.toEntity(request);

        // CRITICAL: Set instanceId for multi-tenant isolation
        student.setInstanceId(tenantId);

        Student saved = studentRepository.save(student);

        log.info("Created student with ID: {}, instanceId: {}", saved.getId(), saved.getInstanceId());
        return studentMapper.toResponse(saved);
    }

    /**
     * Set/reset a student's KC-native login password (KC-9 student-auth, Wave auth-1
     * Hướng B — mirrors {@code TeacherServiceImpl.provisionCredential}, GAP-725).
     *
     * <p>Provisions an {@code auth_credentials} row (entityType=STUDENT,
     * entityId=student.id, email=student.email) so the student can log in via
     * {@code POST /api/v1/tenant-auth/login}. Upsert — re-invoking rotates the password.
     *
     * @param studentId   target student (tenant-scoped)
     * @param rawPassword the new password (validated at the controller)
     * @throws EntityNotFoundException if the student does not exist in this tenant
     * @throws BusinessException 400 {@code STUDENT_EMAIL_REQUIRED} if the student has no
     *         email — login is email-keyed, so a credential cannot be provisioned without one
     */
    @Override
    @Transactional
    public void provisionCredential(Long studentId, String rawPassword) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Student student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> {
                    log.warn("Student not found for credential provisioning, ID: {}", studentId);
                    return new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) studentId);
                });
        if (student.getEmail() == null || student.getEmail().isBlank()) {
            log.warn("Cannot provision login for student id={} — no email on record", studentId);
            throw new BusinessException("STUDENT_EMAIL_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        credentialProvisioning.setPassword(
                AuthCredentialProvisioningService.ROLE_STUDENT,
                student.getId(), student.getEmail(), tenantId, rawPassword);
        log.info("Provisioned login credential for student id={}, tenant={}", studentId, tenantId);
    }

    /**
     * Lấy thông tin chi tiết học viên theo ID.
     *
     * <p>Result is cached in Redis with key "students::{id}".
     *
     * @param id ID của học viên cần lấy thông tin
     * @return StudentResponse chứa thông tin chi tiết học viên
     * @throws EntityNotFoundException nếu không tìm thấy học viên với ID này
     */
    @Override
    @Transactional(readOnly = true)
    // GAP-043 (Wave 9.5-D) — sync=true prevents cache stampede on popular student lookups
    // (teacher dashboard, parent portal). 10 concurrent misses → 1 DB round-trip.
    @Cacheable(value = "students", keyGenerator = "multiTenantKeyGenerator")
    public StudentResponse getStudentById(Long id) {
        log.debug("Fetching student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) id);
                });

        return studentMapper.toResponse(student);
    }

    /**
     * Tìm kiếm danh sách học viên với phân trang.
     *
     * <p>Supports full-text search by name, email, phone and filtering by status.
     *
     * @param search Từ khóa tìm kiếm (name, email, hoặc phone), có thể null
     * @param status Trạng thái học viên (ACTIVE, INACTIVE, GRADUATED), có thể null
     * @param pageable Thông tin phân trang và sắp xếp
     * @return PageResponse chứa danh sách học viên và thông tin phân trang
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getStudents(String search, String status, Pageable pageable) {
        log.debug("Searching students with search='{}', status='{}', page={}", search, status, pageable.getPageNumber());

        // Validate status if provided (throws IllegalArgumentException if invalid)
        if (status != null && !status.isEmpty()) {
            StudentStatus.valueOf(status); // Just for validation
        }

        Page<Student> studentPage = studentRepository.findBySearchCriteria(search, status, pageable);

        Page<StudentResponse> responsePage = studentPage.map(studentMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    /**
     * Cập nhật thông tin học viên.
     *
     * <p>Validates email and phone uniqueness if they are changed.
     * Only non-null fields in request will be updated.
     *
     * @param id ID của học viên cần cập nhật
     * @param request Thông tin cần cập nhật (partial update, các field null sẽ được bỏ qua)
     * @return StudentResponse chứa thông tin học viên sau khi cập nhật
     * @throws EntityNotFoundException nếu không tìm thấy học viên với ID này
     * @throws DuplicateResourceException nếu email hoặc phone mới đã tồn tại
     */
    @Override
    @Transactional
    @CacheEvict(value = "students", keyGenerator = "multiTenantKeyGenerator")
    public StudentResponse updateStudent(Long id, UpdateStudentRequest request) {
        log.info("Updating student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) id);
                });

        // Validate email uniqueness within tenant if changed (multi-tenant isolation)
        if (request.email() != null && !request.email().equals(student.getEmail())) {
            if (studentRepository.existsByEmailAndInstanceIdAndDeletedFalse(request.email(), student.getInstanceId())) {
                log.warn("Duplicate student email within tenant: {}, tenantId: {}", request.email(), student.getInstanceId());
                throw new DuplicateResourceException("STUDENT_EMAIL_EXISTS", (Object) request.email());
            }
        }

        // Validate phone uniqueness within tenant if changed (GAP-799 — was global)
        if (request.phone() != null && !request.phone().equals(student.getPhone())) {
            if (studentRepository.existsByPhoneAndInstanceIdAndDeletedFalse(request.phone(), student.getInstanceId())) {
                log.warn("Duplicate student phone within tenant: {}, tenantId: {}", request.phone(), student.getInstanceId());
                throw new DuplicateResourceException("STUDENT_PHONE_EXISTS", (Object) request.phone());
            }
        }

        studentMapper.updateEntity(student, request);
        Student updated = studentRepository.save(student);

        log.info("Updated student with ID: {}", id);
        return studentMapper.toResponse(updated);
    }

    /**
     * Xóa học viên (soft delete).
     *
     * <p>Marks the student as deleted without physically removing from database.
     * The student will be excluded from all queries using deletedFalse filters.
     *
     * @param id ID của học viên cần xóa
     * @throws EntityNotFoundException nếu không tìm thấy học viên với ID này
     */
    @Override
    @Transactional
    @CacheEvict(value = "students", keyGenerator = "multiTenantKeyGenerator")
    public void deleteStudent(Long id) {
        log.info("Deleting student with ID: {}", id);

        Student student = studentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);
                    return new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) id);
                });

        student.markAsDeleted();
        studentRepository.save(student);

        // KC-9 student-auth parity with teacher (Wave auth-2, GAP-1013b): revoke the
        // student's KC-native login when the entity is soft-deleted so a deactivated
        // student cannot still log in. No-op when no credential was ever provisioned.
        credentialProvisioning.disableCredential(
                AuthCredentialProvisioningService.ROLE_STUDENT, student.getId());

        log.info("Deleted student with ID: {}", id);
    }
}
