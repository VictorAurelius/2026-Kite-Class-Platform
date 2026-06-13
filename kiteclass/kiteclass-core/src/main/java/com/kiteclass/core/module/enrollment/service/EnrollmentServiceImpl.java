package com.kiteclass.core.module.enrollment.service;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.EnrollmentResponse;
import com.kiteclass.core.module.enrollment.dto.MyEnrollmentResponse;
import com.kiteclass.core.module.enrollment.dto.UpdateEnrollmentStatusRequest;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.mapper.EnrollmentMapper;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link EnrollmentService}.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EnrollmentResponse enrollStudent(CreateEnrollmentRequest request) {
        log.info("Enrolling student {} in class {}", request.getStudentId(), request.getClassId());

        // Validate student exists and is active
        Student student = studentRepository.findByIdAndDeletedFalse(request.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) request.getStudentId()));

        // Wave beta-readiness-1 Bucket B — capacity-race fix.
        // Use PESSIMISTIC_WRITE lock (SELECT FOR UPDATE) so concurrent enrollments
        // serialize at the DB level: each transaction acquires an exclusive row lock
        // on the Class row, reads the current counter, and either enrolls or gets
        // CLASS_FULL. No optimistic retry needed — first maxStudents requests succeed.
        Class clazz = classRepository.findByIdForEnrollmentWithLock(request.getClassId())
                .orElseThrow(() -> new EntityNotFoundException("CLASS_NOT_FOUND", (Object) request.getClassId()));

        // GAP-989: Class-status guard. A class can only accept enrollments while it is in
        // an active lifecycle state (SCHEDULED / IN_PROGRESS). Enrolling into a terminal
        // state (COMPLETED / CANCELLED) is rejected with HTTP 400 instead of silently
        // returning 201. Mirrors Class#canEnroll() lifecycle rule (BR-CLASS-006 family).
        if (clazz.getStatus() == ClassStatus.COMPLETED || clazz.getStatus() == ClassStatus.CANCELLED) {
            log.warn("Cannot enroll student {} into class {} with non-enrollable status {}",
                    request.getStudentId(), request.getClassId(), clazz.getStatus());
            throw new ValidationException("CLASS_NOT_ENROLLABLE",
                    request.getClassId(), clazz.getStatus().getDisplayNameVi());
        }

        // BR-ENROLL-002: Check for duplicate enrollment (regardless of status)
        if (enrollmentRepository.findByStudentIdAndClassIdAndDeletedFalse(
                request.getStudentId(),
                request.getClassId()).isPresent()) {
            log.warn("Student {} is already enrolled in class {}", request.getStudentId(), request.getClassId());
            throw new DuplicateResourceException("ENROLLMENT_DUPLICATE",
                    request.getStudentId(), request.getClassId());
        }

        // BR-ENROLL-001: Check class capacity via currentEnrolled counter on the locked Class row.
        // PESSIMISTIC_WRITE lock (SELECT FOR UPDATE) guarantees that this read-check-increment
        // sequence is atomic: only one transaction at a time can hold the lock, so the counter
        // read here reflects the true committed state. If full → CLASS_FULL (400).
        if (clazz.getCurrentEnrolled() >= clazz.getMaxStudents()) {
            log.warn("Class {} is at full capacity ({}/{})",
                    request.getClassId(), clazz.getCurrentEnrolled(), clazz.getMaxStudents());
            throw new ValidationException("CLASS_FULL",
                    request.getClassId(), clazz.getMaxStudents());
        }

        // Create enrollment entity
        Enrollment enrollment = enrollmentMapper.toEntity(request);
        enrollment.setInstanceId(student.getInstanceId()); // Multi-tenant

        // Set default discount percent if not provided
        if (enrollment.getDiscountPercent() == null) {
            enrollment.setDiscountPercent(BigDecimal.ZERO);
        }

        // BR-ENROLL-003: final_amount calculated automatically in @PrePersist
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // Increment denormalized counter on Class row. Safe because PESSIMISTIC_WRITE lock
        // above serializes concurrent enrollers — only one transaction sees and updates
        // the counter at a time.
        clazz.setCurrentEnrolled(clazz.getCurrentEnrolled() + 1);
        classRepository.save(clazz);

        log.info("Successfully enrolled student {} in class {} with enrollment ID {}",
                request.getStudentId(), request.getClassId(), savedEnrollment.getId());

        // Publish ENROLLMENT_CREATED event for invoice generation (PR 2.8)
        eventPublisher.publishEvent(
                new com.kiteclass.core.module.enrollment.event.EnrollmentCreatedEvent(
                        this, savedEnrollment));

        return enrollmentMapper.toResponse(savedEnrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long id) {
        log.debug("Fetching enrollment with ID: {}", id);

        // GAP-746: explicit tenant filter to defend against Hibernate tenantFilter not
        // being applied across REQUIRES_NEW / AFTER_COMMIT listener boundaries. Falls
        // back to legacy method when TenantContext is unset (e.g. system jobs).
        Enrollment enrollment = TenantContext.isSet()
                ? enrollmentRepository.findByIdAndInstanceIdAndDeletedFalse(id, TenantContext.getCurrentTenant())
                        .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) id))
                : enrollmentRepository.findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) id));

        return enrollmentMapper.toResponse(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getEnrollmentsByStudent(Long studentId, Pageable pageable) {
        log.debug("Fetching enrollments for student: {}", studentId);

        // Validate student exists
        if (!studentRepository.existsById(studentId)) {
            throw new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) studentId);
        }

        Page<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndDeletedFalse(
                studentId, pageable
        );

        return enrollments.map(enrollmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MyEnrollmentResponse> getMyEnrollments(Long studentId, Pageable pageable) {
        log.debug("Fetching self enrollments for student: {}", studentId);

        // Self-scoped query — studentId is the calling actor's own students.id.
        // Hibernate tenantFilter scopes this to the current tenant; the studentId
        // predicate scopes it to the single student → no cross-student leak.
        Page<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndDeletedFalse(
                studentId, pageable
        );

        // Batch-resolve class + course names to enrich the response WITHOUT N+1:
        // 1 query for the page's distinct classes, 1 for their distinct courses.
        // findAllById runs through the Hibernate tenantFilter (HQL/criteria), so
        // tenant isolation is preserved on the enrichment lookups too.
        List<Long> classIds = enrollments.getContent().stream()
                .map(Enrollment::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Class> classById = classIds.isEmpty()
                ? Map.of()
                : classRepository.findAllById(classIds).stream()
                        .collect(Collectors.toMap(Class::getId, Function.identity(), (a, b) -> a));

        List<Long> courseIds = classById.values().stream()
                .map(Class::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Course> courseById = courseIds.isEmpty()
                ? Map.of()
                : courseRepository.findAllById(courseIds).stream()
                        .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));

        return enrollments.map(enrollment -> toMyEnrollmentResponse(enrollment, classById, courseById));
    }

    /**
     * Builds an enriched {@link MyEnrollmentResponse} from an enrollment plus the
     * pre-fetched class/course lookup maps. Class/course names degrade to null if
     * the referenced row is absent (soft-deleted) — display-only, never a leak.
     */
    private MyEnrollmentResponse toMyEnrollmentResponse(
            Enrollment enrollment,
            Map<Long, Class> classById,
            Map<Long, Course> courseById) {
        Class clazz = classById.get(enrollment.getClassId());
        Long courseId = clazz != null ? clazz.getCourseId() : null;
        Course course = courseId != null ? courseById.get(courseId) : null;

        return MyEnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .classId(enrollment.getClassId())
                .className(clazz != null ? clazz.getName() : null)
                .courseId(courseId)
                .courseName(course != null ? course.getName() : null)
                .enrollmentDate(enrollment.getEnrollmentDate())
                .status(enrollment.getStatus())
                .tuitionAmount(enrollment.getTuitionAmount())
                .discountPercent(enrollment.getDiscountPercent())
                .finalAmount(enrollment.getFinalAmount())
                .notes(enrollment.getNotes())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getEnrollmentsByClass(Long classId, Pageable pageable) {
        log.debug("Fetching enrollments for class: {}", classId);

        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new EntityNotFoundException("CLASS_NOT_FOUND", (Object) classId);
        }

        Page<Enrollment> enrollments = enrollmentRepository.findByClassIdAndDeletedFalse(
                classId, pageable
        );

        return enrollments.map(enrollmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getEnrollmentsByClassAndStatus(
            Long classId,
            EnrollmentStatus status,
            Pageable pageable) {
        log.debug("Fetching enrollments for class {} with status {}", classId, status);

        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new EntityNotFoundException("CLASS_NOT_FOUND", (Object) classId);
        }

        Page<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatusAndDeletedFalse(
                classId, status, pageable
        );

        return enrollments.map(enrollmentMapper::toResponse);
    }

    @Override
    @Transactional
    public EnrollmentResponse updateEnrollmentStatus(
            Long id,
            UpdateEnrollmentStatusRequest request) {
        log.info("Updating enrollment {} status to {}", id, request.getStatus());

        // GAP-746: explicit tenant filter (see getEnrollmentById)
        Enrollment enrollment = TenantContext.isSet()
                ? enrollmentRepository.findByIdAndInstanceIdAndDeletedFalse(id, TenantContext.getCurrentTenant())
                        .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) id))
                : enrollmentRepository.findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) id));

        EnrollmentStatus oldStatus = enrollment.getStatus();
        enrollment.setStatus(request.getStatus());

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            enrollment.setNotes(request.getNotes());
        }

        Enrollment updated = enrollmentRepository.save(enrollment);

        log.info("Enrollment {} status updated from {} to {}",
                id, oldStatus, request.getStatus());

        return enrollmentMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public EnrollmentResponse withdrawStudent(Long id) {
        log.info("Withdrawing student from enrollment: {}", id);

        // GAP-746: explicit tenant filter (see getEnrollmentById)
        Enrollment enrollment = TenantContext.isSet()
                ? enrollmentRepository.findByIdAndInstanceIdAndDeletedFalse(id, TenantContext.getCurrentTenant())
                        .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) id))
                : enrollmentRepository.findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) id));

        if (enrollment.getStatus() == EnrollmentStatus.WITHDRAWN) {
            log.warn("Enrollment {} is already withdrawn", id);
            throw new ValidationException("ENROLLMENT_ALREADY_WITHDRAWN", id);
        }

        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        Enrollment updated = enrollmentRepository.save(enrollment);

        // Decrement denormalized counter on Class row. Guard against underflow (counter >= 1
        // before decrement; stale data edge case: log warning and skip decrement).
        classRepository.findByIdAndDeletedFalse(enrollment.getClassId()).ifPresent(clazz -> {
            if (clazz.getCurrentEnrolled() > 0) {
                clazz.setCurrentEnrolled(clazz.getCurrentEnrolled() - 1);
                classRepository.save(clazz);
                log.debug("Decremented currentEnrolled for class {} to {}",
                        clazz.getId(), clazz.getCurrentEnrolled());
            } else {
                log.warn("currentEnrolled already 0 for class {} — skipping decrement (data drift?)",
                        enrollment.getClassId());
            }
        });

        log.info("Student withdrawn from enrollment {}", id);

        return enrollmentMapper.toResponse(updated);
    }
}
