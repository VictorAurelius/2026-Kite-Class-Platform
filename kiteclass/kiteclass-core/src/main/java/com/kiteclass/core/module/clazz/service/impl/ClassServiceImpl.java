package com.kiteclass.core.module.clazz.service.impl;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.clazz.dto.*;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.mapper.ClassMapper;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.clazz.service.ClassService;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of ClassService for managing class lifecycle.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private static final String CLASS_NOT_FOUND = "CLASS_NOT_FOUND";
    private static final String COURSE_NOT_FOUND = "COURSE_NOT_FOUND";
    private static final String CLASS_CODE_EXISTS = "CLASS_CODE_EXISTS";
    private static final String CLASS_NAME_EXISTS = "CLASS_NAME_EXISTS";

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassRepository classRepository;
    private final ClassSessionRepository classSessionRepository;
    private final CourseRepository courseRepository;
    private final ClassMapper classMapper;

    @Override
    @Transactional
    public ClassResponse createClass(Long courseId, CreateClassRequest request) {
        log.info("Creating class: courseId={}, name={}", courseId, request.name());

        UUID tenantId = TenantContext.getCurrentTenant();

        // Validate course exists and is not ARCHIVED
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new EntityNotFoundException(COURSE_NOT_FOUND));

        if (CourseStatus.ARCHIVED.equals(course.getStatus())) {
            throw new BusinessException("CLASS_COURSE_ARCHIVED",
                    "Không thể tạo lớp học cho khóa học đã lưu trữ");
        }

        // Validate name uniqueness within course and tenant
        if (classRepository.existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(
                request.name(), courseId, tenantId)) {
            throw new DuplicateResourceException(CLASS_NAME_EXISTS, request.name());
        }

        // Validate dates
        validateDates(request.startDate(), request.endDate());

        // Build entity
        Class clazz = classMapper.toEntity(request);
        clazz.setCourseId(courseId);
        clazz.setStatus(ClassStatus.SCHEDULED);
        clazz.setCurrentEnrolled(0);
        clazz.setInstanceId(tenantId);

        // Apply defaults
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

        // COMPLETED/CANCELLED classes are read-only
        if (clazz.getStatus() == ClassStatus.COMPLETED || clazz.getStatus() == ClassStatus.CANCELLED) {
            throw new BusinessException("CLASS_READ_ONLY",
                    "Không thể chỉnh sửa lớp học đã " + clazz.getStatus().getDisplayNameVi());
        }

        // Update allowed fields
        if (request.description() != null) {
            clazz.setDescription(request.description());
        }
        if (request.locationDetail() != null) {
            clazz.setLocationDetail(request.locationDetail());
        }

        // Schedule-related fields: only for SCHEDULED classes
        if (clazz.getStatus() == ClassStatus.SCHEDULED) {
            if (request.name() != null && !request.name().equals(clazz.getName())) {
                // Check name uniqueness
                if (classRepository.existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(
                        request.name(), clazz.getCourseId(), tenantId)) {
                    throw new DuplicateResourceException(CLASS_NAME_EXISTS, request.name());
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
            // IN_PROGRESS: block schedule/date changes
            if (request.schedule() != null || request.startDate() != null || request.endDate() != null) {
                throw new BusinessException("CLASS_SCHEDULE_LOCKED",
                        "Không thể thay đổi lịch học của lớp đang diễn ra");
            }
        }

        // Max students: can increase anytime, can decrease only if >= current_enrolled
        if (request.maxStudents() != null) {
            if (request.maxStudents() < clazz.getCurrentEnrolled()) {
                throw new BusinessException("CLASS_CAPACITY_VIOLATION",
                        "Số học sinh tối đa không thể nhỏ hơn số học sinh hiện tại: "
                                + clazz.getCurrentEnrolled());
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
            throw new BusinessException("CLASS_CANNOT_START",
                    "Chỉ có thể bắt đầu lớp học ở trạng thái SCHEDULED. Trạng thái hiện tại: "
                            + clazz.getStatus().getDisplayNameVi());
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
            throw new BusinessException("CLASS_CANNOT_COMPLETE",
                    "Chỉ có thể hoàn thành lớp học đang diễn ra. Trạng thái hiện tại: "
                            + clazz.getStatus().getDisplayNameVi());
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
            throw new BusinessException("CLASS_CANNOT_CANCEL",
                    "Không thể hủy lớp học đã " + clazz.getStatus().getDisplayNameVi());
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

        if (!clazz.canDelete()) {
            if (clazz.getStatus() != ClassStatus.SCHEDULED) {
                throw new BusinessException("CLASS_CANNOT_DELETE",
                        "Chỉ có thể xóa lớp học ở trạng thái SCHEDULED");
            }
            throw new BusinessException("CLASS_HAS_STUDENTS",
                    "Không thể xóa lớp học đã có học sinh. Hãy hủy lớp học thay vì xóa");
        }

        // Soft delete sessions first
        classSessionRepository.softDeleteByClassId(classId);

        // Soft delete class
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
            // Use custom code - validate uniqueness
            code = request.customCode().toUpperCase();
            if (classRepository.existsByClassCodeAndDeletedFalse(code)) {
                throw new DuplicateResourceException(CLASS_CODE_EXISTS, code);
            }
        } else {
            // Auto-generate unique code
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
            throw new BusinessException("CLASS_NO_DATES",
                    "Lớp học phải có ngày bắt đầu và kết thúc trước khi tạo lịch học");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("CLASS_INVALID_TIME",
                    "Giờ kết thúc phải sau giờ bắt đầu");
        }

        UUID tenantId = TenantContext.getCurrentTenant();
        int maxSessionNumber = classSessionRepository.findMaxSessionNumberByClassId(classId);
        List<ClassSession> sessions = new ArrayList<>();

        LocalDate current = clazz.getStartDate();
        int sessionNumber = maxSessionNumber + 1;

        // Loop from startDate to endDate, generate a session for each matching day
        while (!current.isAfter(clazz.getEndDate())) {
            DayOfWeek currentDay = current.getDayOfWeek();
            if (request.daysOfWeek().contains(currentDay)) {
                ClassSession session = ClassSession.builder()
                        .classId(classId)
                        .sessionNumber(sessionNumber++)
                        .sessionDate(current)
                        .startTime(request.startTime())
                        .endTime(request.endTime())
                        .instanceId(tenantId)
                        .build();
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

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private Class findClassOrThrow(Long classId) {
        return classRepository.findByIdAndDeletedFalse(classId)
                .orElseThrow(() -> new EntityNotFoundException(CLASS_NOT_FOUND));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new BusinessException("CLASS_INVALID_DATES",
                    "Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            if (attempts++ > 20) {
                throw new BusinessException("CLASS_CODE_GENERATION_FAILED",
                        "Không thể tạo mã lớp học duy nhất. Vui lòng thử lại.");
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
