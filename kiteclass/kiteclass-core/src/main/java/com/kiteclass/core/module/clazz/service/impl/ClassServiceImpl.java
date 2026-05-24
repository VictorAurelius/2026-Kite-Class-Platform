package com.kiteclass.core.module.clazz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.clazz.dto.CancelClassRequest;
import com.kiteclass.core.module.clazz.dto.ClassCodeResponse;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.ClassSessionResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.dto.CreateScheduleRequest;
import com.kiteclass.core.module.clazz.dto.GenerateClassCodeRequest;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto;
import com.kiteclass.core.module.clazz.dto.RescheduleClassRequest;
import com.kiteclass.core.module.clazz.dto.UpdateClassRequest;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.event.ClassRescheduledEvent;
import com.kiteclass.core.module.clazz.mapper.ClassMapper;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.clazz.service.ClassService;
import com.kiteclass.core.module.clazz.service.RecurrenceService;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of ClassService for managing class lifecycle.
 *
 * <p>All error messages are resolved from messages.properties / messages_vi.properties
 * using error codes. No hard-coded messages in service layer.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassRepository classRepository;
    private final ClassSessionRepository classSessionRepository;
    private final CourseRepository courseRepository;
    private final ClassMapper classMapper;
    private final RecurrenceService recurrenceService;
    private final ObjectMapper objectMapper;
    private final OutboxEventWriter outboxEventWriter;

    @Override
    @Transactional
    public ClassResponse createClass(Long courseId, CreateClassRequest request) {
        log.info("Creating class: courseId={}, name={}", courseId, request.name());

        UUID tenantId = TenantContext.getCurrentTenant();

        // Validate course exists (BR-CLASS-001)
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) courseId));

        // Validate course is not ARCHIVED (BR-CLASS-001)
        if (CourseStatus.ARCHIVED.equals(course.getStatus())) {
            throw new ValidationException("CLASS_COURSE_ARCHIVED", new Object[0]);
        }

        // Validate name uniqueness within course + tenant
        if (classRepository.existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(
                request.name(), courseId, tenantId)) {
            throw new DuplicateResourceException("CLASS_NAME_EXISTS", (Object) request.name());
        }

        // Validate dates (BR-CLASS-005)
        validateDates(request.startDate(), request.endDate());

        // Build entity
        Class clazz = classMapper.toEntity(request);
        clazz.setCourseId(courseId);
        clazz.setStatus(ClassStatus.SCHEDULED);
        clazz.setCurrentEnrolled(0);
        clazz.setInstanceId(tenantId);

        // GAP-727: Set teacher_id from caller's user context.
        // AuthorizationBean.hasAccessToClass() query depends on this — without it,
        // every teacher is locked out of their own classes (NOT IDOR, full lock-out).
        // Nullable when caller is ADMIN (no single teacher owner — explicit assignment via separate endpoint).
        Long callerUserId = UserContext.getCurrentUser();
        if (callerUserId != null) {
            clazz.setTeacherId(callerUserId);
        }

        if (clazz.getLocationType() == null) {
            clazz.setLocationType(Class.LocationType.IN_PERSON);
        }
        if (clazz.getMaxStudents() == null) {
            clazz.setMaxStudents(30);
        }

        Class saved = classRepository.save(clazz);
        log.info("Class created: id={}, courseId={}", saved.getId(), courseId);

        return classMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ClassResponse updateClass(Long classId, UpdateClassRequest request) {
        log.info("Updating class: classId={}", classId);

        UUID tenantId = TenantContext.getCurrentTenant();
        Class clazz = findClassOrThrow(classId);

        // COMPLETED/CANCELLED are read-only (BR-CLASS-006)
        if (clazz.getStatus() == ClassStatus.COMPLETED || clazz.getStatus() == ClassStatus.CANCELLED) {
            throw new ValidationException("CLASS_READ_ONLY", clazz.getStatus());
        }

        // Always-allowed updates
        if (request.description() != null) {
            clazz.setDescription(request.description());
        }
        if (request.locationDetail() != null) {
            clazz.setLocationDetail(request.locationDetail());
        }

        if (clazz.getStatus() == ClassStatus.SCHEDULED) {
            // All fields editable when SCHEDULED
            if (request.name() != null && !request.name().equals(clazz.getName())) {
                if (classRepository.existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(
                        request.name(), clazz.getCourseId(), tenantId)) {
                    throw new DuplicateResourceException("CLASS_NAME_EXISTS", (Object) request.name());
                }
                clazz.setName(request.name());
            }
            if (request.schedule() != null) {
                clazz.setSchedule(request.schedule());
            }
            if (request.locationType() != null) {
                clazz.setLocationType(request.locationType());
            }
            if (request.startDate() != null) {
                clazz.setStartDate(request.startDate());
            }
            if (request.endDate() != null) {
                clazz.setEndDate(request.endDate());
            }
            validateDates(clazz.getStartDate(), clazz.getEndDate());
        } else {
            // IN_PROGRESS: schedule/dates are locked (BR-CLASS-006)
            if (request.schedule() != null || request.startDate() != null || request.endDate() != null) {
                throw new ValidationException("CLASS_SCHEDULE_LOCKED", new Object[0]);
            }
        }

        // Max students: can reduce only if still >= current_enrolled (BR-CLASS-003)
        if (request.maxStudents() != null) {
            if (request.maxStudents() < clazz.getCurrentEnrolled()) {
                throw new ValidationException("CLASS_CAPACITY_VIOLATION",
                        (Object) clazz.getCurrentEnrolled());
            }
            clazz.setMaxStudents(request.maxStudents());
        }

        Class saved = classRepository.save(clazz);
        log.info("Class updated: id={}", saved.getId());

        return classMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClass(Long classId) {
        return classMapper.toResponse(findClassOrThrow(classId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClassResponse> listClasses(Long courseId, int page, int size) {
        log.debug("Listing classes: courseId={}, page={}, size={}", courseId, page, size);

        Page<Class> classPage = classRepository.findByCourseIdAndDeletedFalse(
                courseId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        List<ClassResponse> content = classPage.getContent()
                .stream()
                .map(classMapper::toResponse)
                .toList();

        return PageResponse.of(
                content,
                classPage.getNumber(),
                classPage.getSize(),
                classPage.getTotalElements());
    }

    @Override
    @Transactional
    public ClassResponse startClass(Long classId) {
        log.info("Starting class: classId={}", classId);

        Class clazz = findClassOrThrow(classId);

        if (!clazz.canStart()) {
            throw new ValidationException("CLASS_CANNOT_START", clazz.getStatus());
        }

        clazz.setStatus(ClassStatus.IN_PROGRESS);
        clazz.setStartedAt(Instant.now());

        Class saved = classRepository.save(clazz);
        log.info("Class started: id={}", saved.getId());

        return classMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ClassResponse completeClass(Long classId) {
        log.info("Completing class: classId={}", classId);

        Class clazz = findClassOrThrow(classId);

        if (!clazz.canComplete()) {
            throw new ValidationException("CLASS_CANNOT_COMPLETE", clazz.getStatus());
        }

        clazz.setStatus(ClassStatus.COMPLETED);
        clazz.setCompletedAt(Instant.now());

        Class saved = classRepository.save(clazz);
        log.info("Class completed: id={}", saved.getId());

        return classMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ClassResponse cancelClass(Long classId, CancelClassRequest request) {
        log.info("Cancelling class: classId={}, reason={}", classId, request.reason());

        Class clazz = findClassOrThrow(classId);

        if (!clazz.canCancel()) {
            throw new ValidationException("CLASS_CANNOT_CANCEL", clazz.getStatus());
        }

        clazz.setStatus(ClassStatus.CANCELLED);
        clazz.setCancelledAt(Instant.now());

        Class saved = classRepository.save(clazz);
        log.info("Class cancelled: id={}, reason={}", saved.getId(), request.reason());

        return classMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteClass(Long classId) {
        log.info("Deleting class: classId={}", classId);

        Class clazz = findClassOrThrow(classId);

        if (clazz.getStatus() != ClassStatus.SCHEDULED) {
            throw new ValidationException("CLASS_CANNOT_DELETE", clazz.getStatus());
        }

        if (clazz.getCurrentEnrolled() > 0) {
            throw new ValidationException("CLASS_HAS_STUDENTS",
                    (Object) clazz.getCurrentEnrolled());
        }

        classSessionRepository.softDeleteByClassId(classId);
        clazz.markAsDeleted();
        classRepository.save(clazz);
        log.info("Class deleted: id={}", classId);
    }

    @Override
    @Transactional
    public ClassCodeResponse generateClassCode(Long classId, GenerateClassCodeRequest request) {
        log.info("Generating class code: classId={}", classId);

        Class clazz = findClassOrThrow(classId);

        String code;
        if (request.customCode() != null && !request.customCode().isBlank()) {
            code = request.customCode().toUpperCase();
            if (classRepository.existsByClassCodeAndDeletedFalse(code)) {
                throw new DuplicateResourceException("CLASS_CODE_EXISTS", (Object) code);
            }
        } else {
            code = generateUniqueCode();
        }

        clazz.setClassCode(code);
        clazz.setCodeExpiresAt(request.expiresAt());
        classRepository.save(clazz);

        log.info("Class code generated: classId={}, code={}", classId, code);
        return new ClassCodeResponse(code, request.expiresAt());
    }

    @Override
    @Transactional
    public List<ClassSessionResponse> createSchedule(Long classId, CreateScheduleRequest request) {
        log.info("Creating schedule: classId={}, days={}", classId, request.daysOfWeek());

        Class clazz = findClassOrThrow(classId);

        if (clazz.getStartDate() == null || clazz.getEndDate() == null) {
            throw new ValidationException("CLASS_NO_DATES", new Object[0]);
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ValidationException("CLASS_INVALID_TIME", new Object[0]);
        }

        int maxSessionNumber = classSessionRepository.findMaxSessionNumberByClassId(classId);
        List<ClassSession> sessions = new ArrayList<>();

        LocalDate current = clazz.getStartDate();
        int sessionNumber = maxSessionNumber + 1;

        while (!current.isAfter(clazz.getEndDate())) {
            DayOfWeek currentDay = current.getDayOfWeek();
            if (request.daysOfWeek().contains(currentDay)) {
                ClassSession session = ClassSession.builder()
                        .classId(classId)
                        .sessionNumber(sessionNumber++)
                        .sessionDate(current)
                        .startTime(request.startTime())
                        .endTime(request.endTime())
                        .build();
                // instanceId is auto-set by EntityPersistenceListener from TenantContext
                sessions.add(session);
            }
            current = current.plusDays(1);
        }

        List<ClassSession> saved = classSessionRepository.saveAll(sessions);
        log.info("Generated {} sessions for classId={}", saved.size(), classId);

        return saved.stream()
                .map(classMapper::toSessionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSessionResponse> listSessions(Long classId) {
        findClassOrThrow(classId);
        return classSessionRepository
                .findByClassIdAndDeletedFalseOrderBySessionNumberAsc(classId)
                .stream()
                .map(classMapper::toSessionResponse)
                .toList();
    }

    /**
     * GAP-290 Wave 18a — generate sessions from RFC 5545 RRULE subset.
     *
     * <p>Edit semantics (BR-CLASS-009 state machine):
     * <ol>
     *   <li>Persist new {@code recurrence_rule} JSONB on the class.</li>
     *   <li>Soft-delete future {@code SCHEDULED} sessions where
     *       {@code attendanceTaken == false}. Past sessions and any session with
     *       {@code attendanceTaken == true} are preserved.</li>
     *   <li>Plan new occurrences from {@code today} (or class.startDate, whichever
     *       is later) through {@code rule.until()}, skipping dates that match a
     *       preserved session's date (avoid duplicates).</li>
     *   <li>Persist new sessions; return the merged list (preserved + new) ordered
     *       by sessionNumber.</li>
     * </ol>
     */
    @Override
    @Transactional
    public List<ClassSessionResponse> generateSessionsFromRecurrence(Long classId, RecurrenceRuleDto rule) {
        log.info("Generating recurring sessions: classId={}, freq={}, byDay={}, until={}",
                classId, rule.freq(), rule.byDay(), rule.until());

        Class clazz = findClassOrThrow(classId);

        if (clazz.getStatus() == ClassStatus.COMPLETED || clazz.getStatus() == ClassStatus.CANCELLED) {
            throw new ValidationException("CLASS_RECURRENCE_LOCKED", clazz.getStatus());
        }

        // Persist the new rule on the class (idempotent — overwrites any prior rule).
        try {
            clazz.setRecurrenceRule(objectMapper.writeValueAsString(rule));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize recurrence rule for classId={}", classId, e);
            throw new ValidationException("RECURRENCE_SERIALIZATION_FAILED", new Object[0]);
        }

        // Pull current sessions; partition into preserved vs. regenerable.
        List<ClassSession> existing = classSessionRepository
                .findByClassIdAndDeletedFalseOrderBySessionNumberAsc(classId);
        LocalDate today = LocalDate.now();
        List<ClassSession> preserved = new ArrayList<>();
        List<ClassSession> regenerable = new ArrayList<>();
        for (ClassSession s : existing) {
            boolean isPast = s.getSessionDate().isBefore(today);
            boolean isAttended = Boolean.TRUE.equals(s.getAttendanceTaken());
            if (isPast || isAttended) {
                preserved.add(s);
            } else {
                regenerable.add(s);
            }
        }

        // Soft-delete regenerable sessions (so re-running is idempotent).
        for (ClassSession s : regenerable) {
            s.markAsDeleted();
        }
        if (!regenerable.isEmpty()) {
            classSessionRepository.saveAll(regenerable);
        }

        // Plan new sessions starting from max(today, class.startDate); skip dates
        // already owned by a preserved session.
        LocalDate planStart = (clazz.getStartDate() != null && clazz.getStartDate().isAfter(today))
                ? clazz.getStartDate() : today;
        java.util.Set<LocalDate> reservedDates = new java.util.HashSet<>();
        for (ClassSession s : preserved) {
            reservedDates.add(s.getSessionDate());
        }

        int maxNumber = preserved.stream()
                .mapToInt(ClassSession::getSessionNumber)
                .max()
                .orElse(0);

        List<ClassSession> planned = recurrenceService
                .buildSessions(classId, planStart, rule, maxNumber)
                .stream()
                .filter(s -> !reservedDates.contains(s.getSessionDate()))
                .toList();

        if (!planned.isEmpty()) {
            classSessionRepository.saveAll(planned);
        }
        classRepository.save(clazz);

        log.info("Recurrence applied: classId={}, preserved={}, regenerated={}, new={}",
                classId, preserved.size(), regenerable.size(), planned.size());

        // Return combined list ordered by sessionNumber.
        return classSessionRepository
                .findByClassIdAndDeletedFalseOrderBySessionNumberAsc(classId)
                .stream()
                .map(classMapper::toSessionResponse)
                .toList();
    }

    /**
     * Reschedules a class — preserves attendance + grade history, captures audit
     * columns, publishes Outbox event (Wave beta-readiness-4 Bucket D, GAP-291).
     *
     * <p>Per cross-bucket LOCKED decision §3.6, NO new ClassStatus enum is introduced —
     * the existing status (SCHEDULED) is preserved; only audit columns + date fields mutate.
     *
     * <p>Outbox event recipient lists (enrolledStudentIds, parentUserIds) ship EMPTY
     * in v1.0.0 — recipient lookup deferred to email consumer side (Phase 1.5+).
     */
    @Override
    @Transactional
    public ClassResponse rescheduleClass(Long classId, RescheduleClassRequest request) {
        log.info("Rescheduling class: classId={}, newStartDate={}, newEndDate={}, reason={}",
                classId, request.newStartDate(), request.newEndDate(), request.reasonCategory());

        Class clazz = findClassOrThrow(classId);

        // BR-CLASS-006: Only SCHEDULED classes can be rescheduled (preserves attendance history
        // for IN_PROGRESS; COMPLETED/CANCELLED are read-only by definition).
        if (!clazz.canEditSchedule()) {
            throw new ValidationException("CLASS_CANNOT_RESCHEDULE", clazz.getStatus());
        }

        // BR-CLASS-005: end_date must be after start_date.
        validateDates(request.newStartDate(), request.newEndDate());

        // Capture audit BEFORE mutation.
        LocalDate previousStartDate = clazz.getStartDate();
        LocalDate previousEndDate = clazz.getEndDate();

        // Mutate dates only — DO NOT change status per LOCKED decision §3.6.
        clazz.setStartDate(request.newStartDate());
        clazz.setEndDate(request.newEndDate());

        // Persist audit columns.
        Long callerUserId = UserContext.getCurrentUser();
        Instant now = Instant.now();
        clazz.setRescheduledByUserId(callerUserId);
        clazz.setRescheduledAt(now);
        clazz.setPreviousStartDate(previousStartDate);
        clazz.setPreviousEndDate(previousEndDate);
        clazz.setRescheduleReasonCategory(request.reasonCategory().name());
        clazz.setRescheduleReasonNotes(request.reasonNotes());

        Class saved = classRepository.save(clazz);

        // Publish Outbox event (same txn — outbox is the reliability net).
        // Recipient lists ship empty v1.0.0; consumer side performs lookup (Phase 1.5+).
        UUID tenantId = TenantContext.getCurrentTenant();
        ClassRescheduledEvent event = new ClassRescheduledEvent(
                saved.getId(),
                tenantId != null ? tenantId.toString() : null,
                null, // tenantName resolved by consumer (avoid coupling to tenant service here)
                saved.getName(),
                previousStartDate,
                saved.getStartDate(),
                previousEndDate,
                saved.getEndDate(),
                callerUserId,
                now,
                request.reasonCategory().name(),
                request.reasonNotes(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            outboxEventWriter.enqueue(
                    "class.rescheduled",
                    "Class",
                    String.valueOf(saved.getId()),
                    payloadJson
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ClassRescheduledEvent for classId={}", classId, e);
            throw new ValidationException("RESCHEDULE_EVENT_SERIALIZATION_FAILED", new Object[0]);
        }

        log.info("Class rescheduled: id={}, by={}, reason={}",
                saved.getId(), callerUserId, request.reasonCategory());

        return classMapper.toResponse(saved);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private Class findClassOrThrow(Long classId) {
        return classRepository.findByIdAndDeletedFalse(classId)
                .orElseThrow(() -> new EntityNotFoundException("CLASS_NOT_FOUND", (Object) classId));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new ValidationException("CLASS_INVALID_DATES", new Object[0]);
        }
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            if (attempts++ > MAX_CODE_GENERATION_ATTEMPTS) {
                throw new ValidationException("CLASS_CODE_GENERATION_FAILED", new Object[0]);
            }
            code = randomCode();
        } while (classRepository.existsByClassCodeAndDeletedFalse(code));
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
