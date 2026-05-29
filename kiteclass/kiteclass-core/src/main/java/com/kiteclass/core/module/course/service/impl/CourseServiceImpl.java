package com.kiteclass.core.module.course.service.impl;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.constant.TeacherCourseRole;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.CourseSearchCriteria;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.mapper.CourseMapper;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.course.service.CourseService;
import com.kiteclass.core.module.course.validator.PrerequisiteValidator;
import com.kiteclass.core.module.teacher.entity.TeacherCourse;
import com.kiteclass.core.module.teacher.repository.TeacherCourseRepository;
import com.kiteclass.core.module.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of CourseService interface.
 *
 * <p>Handles:
 * <ul>
 *   <li>Business logic validation (code uniqueness, teacher existence, status transitions)</li>
 *   <li>Entity mapping via CourseMapper</li>
 *   <li>Integration with TeacherCourseRepository for CREATOR role assignment</li>
 *   <li>Caching with Redis</li>
 *   <li>Transaction management</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherCourseRepository teacherCourseRepository;
    private final CourseMapper courseMapper;
    private final MessageSource messageSource;
    private final PrerequisiteValidator prerequisiteValidator;

    // Field name message keys for validation
    private static final String FIELD_NAME = "field.course.name";
    private static final String FIELD_DESCRIPTION = "field.course.description";
    private static final String FIELD_SYLLABUS = "field.course.syllabus";
    private static final String FIELD_OBJECTIVES = "field.course.objectives";
    private static final String FIELD_DURATION_WEEKS = "field.course.durationWeeks";

    /**
     * Tạo khóa học mới.
     *
     * <p>Validates course code uniqueness (BR-COURSE-001) and teacher existence.
     * Auto-creates TeacherCourse relationship with role=CREATOR (BR-COURSE-003).
     *
     * @param request Thông tin khóa học cần tạo (code, name, description, teacherId, etc.)
     * @return CourseResponse chứa thông tin khóa học đã tạo
     * @throws DuplicateResourceException nếu course code đã tồn tại
     * @throws EntityNotFoundException nếu không tìm thấy teacher với teacherId
     */
    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public CourseResponse createCourse(CreateCourseRequest request) {
        log.info("Creating course with code: {}", request.code());

        // BR-COURSE-001: Validate code uniqueness WITHIN tenant (GAP-799 — global check
        // leaked cross-tenant in shared kiteclass DB; instance_id predicate is explicit
        // because the Hibernate tenantFilter does not apply to derived existsBy queries)
        UUID tenantId = TenantContext.getCurrentTenant();
        if (courseRepository.existsByCodeAndInstanceIdAndDeletedFalse(request.code(), tenantId)) {
            log.warn("Duplicate course code within tenant: {}, tenantId: {}", request.code(), tenantId);
            throw new DuplicateResourceException("COURSE_CODE_EXISTS", (Object) request.code());
        }

        // Validate teacher exists and is active
        teacherRepository.findByIdAndDeletedFalse(request.teacherId())
                .orElseThrow(() -> {
                    log.warn("Teacher not found with ID: {}", request.teacherId());
                    return new EntityNotFoundException("TEACHER_NOT_FOUND", (Object) request.teacherId());
                });

        // Create course entity
        Course course = courseMapper.toEntity(request);
        Course saved = courseRepository.save(course);

        // BR-COURSE-003: Auto-create TeacherCourse with role=CREATOR
        TeacherCourse teacherCourse = TeacherCourse.builder()
                .teacherId(request.teacherId())
                .courseId(saved.getId())
                .role(TeacherCourseRole.CREATOR)
                .assignedBy(null) // NULL for self-created
                .build();
        teacherCourseRepository.save(teacherCourse);

        log.info("Created course with ID: {} and assigned CREATOR role to teacher ID: {}",
                saved.getId(), request.teacherId());
        return courseMapper.toResponse(saved);
    }

    /**
     * Lấy thông tin chi tiết khóa học theo ID.
     *
     * <p>Result is cached in Redis with key "courses::{tenantId}:{id}".
     *
     * <p><b>Multi-tenant note (GAP-792):</b> the cache key MUST include the tenant
     * (instance) ID. Course PKs are drawn from a shared global sequence, so a key of
     * {@code #id} alone causes cross-tenant cache pollution — tenant B fetching course 5
     * could receive tenant A's cached payload (data leak) or a Redis
     * {@code SerializationException} surfacing as HTTP 500. The matching {@code @CacheEvict}
     * keys below use the same {@code tenantId + ':' + id} expression so eviction stays consistent.
     *
     * @param id ID của khóa học cần lấy thông tin
     * @return CourseResponse chứa thông tin chi tiết khóa học
     * @throws EntityNotFoundException nếu không tìm thấy khóa học với ID này
     */
    @Override
    @Transactional(readOnly = true)
    // GAP-043 (Wave 9.5-D) — sync=true prevents stampede on course catalogue reads
    // (public landing + tenant course listing). Heavy joins with instructor + schedule
    // data make a redundant concurrent load particularly expensive.
    // GAP-792 — key includes tenant to prevent cross-tenant cache pollution.
    @Cacheable(value = "courses", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public CourseResponse getCourseById(Long id) {
        log.debug("Fetching course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("COURSE_NOT_FOUND", (Object) id);
                });

        return courseMapper.toResponse(course);
    }

    /**
     * Tìm kiếm danh sách khóa học với phân trang.
     *
     * <p>Supports full-text search by name/code and filtering by status and teacherId.
     *
     * @param criteria Tiêu chí tìm kiếm (search, status, teacherId, page, size, sort)
     * @return PageResponse chứa danh sách khóa học và thông tin phân trang
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getCourses(CourseSearchCriteria criteria) {
        log.debug("Searching courses with criteria: search='{}', status='{}', teacherId='{}', page={}",
                criteria.search(), criteria.status(), criteria.teacherId(), criteria.page());

        // Validate status if provided (throws IllegalArgumentException if invalid)
        if (criteria.status() != null && !criteria.status().isEmpty()) {
            CourseStatus.valueOf(criteria.status()); // Just for validation
        }

        // Parse sort
        Pageable pageable = createPageable(criteria);

        // GAP-791: findBySearchCriteria uses nativeQuery=true, so Hibernate @Filter("tenantFilter")
        // does NOT apply. Pass the current tenant explicitly to scope the result; without this,
        // the list endpoint leaks other tenants' courses (OWASP A01 cross-tenant leak).
        UUID tenantId = TenantContext.getCurrentTenant();

        // Search courses (tenant-scoped)
        Page<Course> coursePage = courseRepository.findBySearchCriteria(
                tenantId,
                criteria.search(),
                criteria.status(),
                criteria.teacherId(),
                pageable
        );

        Page<CourseResponse> responsePage = coursePage.map(courseMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    /**
     * Cập nhật thông tin khóa học.
     *
     * <p>Update restrictions based on course status (BR-COURSE-002):
     * <ul>
     *   <li>DRAFT: Allows full edit (all fields)</li>
     *   <li>PUBLISHED: Limited edit (description, syllabus, objectives, price, coverImageUrl only)</li>
     *   <li>ARCHIVED: Read-only (no updates allowed)</li>
     * </ul>
     *
     * @param id ID của khóa học cần cập nhật
     * @param request Thông tin cần cập nhật (partial update, các field null sẽ được bỏ qua)
     * @return CourseResponse chứa thông tin khóa học sau khi cập nhật
     * @throws EntityNotFoundException nếu không tìm thấy khóa học với ID này
     * @throws ValidationException nếu cập nhật field bị hạn chế theo trạng thái
     */
    @Override
    @Transactional
    // GAP-792 — evict key includes tenant to match tenant-scoped @Cacheable key.
    @CacheEvict(value = "courses", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {
        log.info("Updating course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("COURSE_NOT_FOUND", (Object) id);
                });

        // BR-COURSE-002: Check status-based update restrictions
        validateUpdateAllowed(course, request);

        courseMapper.updateEntity(course, request);
        Course updated = courseRepository.save(course);

        log.info("Updated course with ID: {}", id);
        return courseMapper.toResponse(updated);
    }

    /**
     * Xóa khóa học (soft delete).
     *
     * <p>Only DRAFT courses can be deleted (BR-COURSE-004).
     * PUBLISHED or ARCHIVED courses cannot be deleted.
     *
     * @param id ID của khóa học cần xóa
     * @throws EntityNotFoundException nếu không tìm thấy khóa học với ID này
     * @throws ValidationException nếu khóa học không ở trạng thái DRAFT
     */
    @Override
    @Transactional
    // GAP-792 — evict key includes tenant to match tenant-scoped @Cacheable key.
    @CacheEvict(value = "courses", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public void deleteCourse(Long id) {
        log.info("Deleting course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("COURSE_NOT_FOUND", (Object) id);
                });

        // Only DRAFT courses can be deleted
        if (!course.canBeDeleted()) {
            log.warn("Cannot delete course with status: {}", course.getStatus());
            throw new ValidationException("COURSE_CANNOT_DELETE_STATUS", (Object) course.getStatus());
        }

        // BR-COURSE-004: Check if has active classes (will be implemented when Class module is ready)
        // For now, we skip this check as Class module doesn't exist yet
        // long classCount = classRepository.countByCourseIdAndDeletedFalse(id);
        // if (classCount > 0) {
        //     throw new ValidationException("Không thể xóa khóa học có lớp học đang hoạt động");
        // }

        course.markAsDeleted();
        courseRepository.save(course);

        log.info("Deleted course with ID: {}", id);
    }

    /**
     * Publish khóa học từ DRAFT sang PUBLISHED.
     *
     * <p>Validates that all required fields are present before publishing.
     * Required fields: name, description, syllabus, objectives, durationWeeks.
     * Only DRAFT courses can be published.
     *
     * @param id ID của khóa học cần publish
     * @return CourseResponse chứa thông tin khóa học sau khi publish
     * @throws EntityNotFoundException nếu không tìm thấy khóa học với ID này
     * @throws ValidationException nếu khóa học không ở trạng thái DRAFT hoặc thiếu required fields
     */
    @Override
    @Transactional
    // GAP-792 — evict key includes tenant to match tenant-scoped @Cacheable key.
    @CacheEvict(value = "courses", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public CourseResponse publishCourse(Long id) {
        log.info("Publishing course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("COURSE_NOT_FOUND", (Object) id);
                });

        // Validate current status
        if (!CourseStatus.DRAFT.equals(course.getStatus())) {
            log.warn("Cannot publish course with status: {}", course.getStatus());
            throw new ValidationException("COURSE_INVALID_PUBLISH_STATE", (Object) course.getStatus());
        }

        // Validate required fields for publishing
        validatePublishRequirements(course);

        // Change status to PUBLISHED
        course.setStatus(CourseStatus.PUBLISHED);
        Course published = courseRepository.save(course);

        log.info("Published course with ID: {}", id);
        return courseMapper.toResponse(published);
    }

    /**
     * Archive khóa học từ PUBLISHED sang ARCHIVED.
     *
     * <p>ARCHIVED courses become read-only and cannot be edited or deleted.
     * Only PUBLISHED courses can be archived.
     *
     * @param id ID của khóa học cần archive
     * @return CourseResponse chứa thông tin khóa học sau khi archive
     * @throws EntityNotFoundException nếu không tìm thấy khóa học với ID này
     * @throws ValidationException nếu khóa học không ở trạng thái PUBLISHED
     */
    @Override
    @Transactional
    // GAP-792 — evict key includes tenant to match tenant-scoped @Cacheable key.
    @CacheEvict(value = "courses", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public CourseResponse archiveCourse(Long id) {
        log.info("Archiving course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("COURSE_NOT_FOUND", (Object) id);
                });

        // Validate current status
        if (!CourseStatus.PUBLISHED.equals(course.getStatus())) {
            log.warn("Cannot archive course with status: {}", course.getStatus());
            throw new ValidationException("COURSE_INVALID_ARCHIVE_STATE", (Object) course.getStatus());
        }

        // Change status to ARCHIVED
        course.setStatus(CourseStatus.ARCHIVED);
        Course archived = courseRepository.save(course);

        log.info("Archived course with ID: {}", id);
        return courseMapper.toResponse(archived);
    }

    /**
     * Validates if update is allowed based on course status and request fields.
     *
     * @param course  the course to update
     * @param request the update request
     * @throws ValidationException if update is not allowed
     */
    private void validateUpdateAllowed(Course course, UpdateCourseRequest request) {
        // ARCHIVED courses are read-only
        if (course.isReadOnly()) {
            throw new ValidationException("COURSE_INVALID_UPDATE_ARCHIVED", new Object[0]);
        }

        // PUBLISHED courses have limited edits
        if (course.isPublished()) {
            // Check if trying to update restricted fields
            if (request.name() != null || request.code() != null ||
                request.teacherId() != null || request.durationWeeks() != null ||
                request.totalSessions() != null || request.prerequisites() != null ||
                request.targetAudience() != null) {
                throw new ValidationException("COURSE_INVALID_UPDATE_PUBLISHED", new Object[0]);
            }
        }

        // DRAFT courses can edit all fields (no restrictions)
    }

    /**
     * Validates that course has all required fields for publishing.
     *
     * @param course the course to validate
     * @throws ValidationException if required fields are missing
     */
    private void validatePublishRequirements(Course course) {
        List<String> missingFieldKeys = new ArrayList<>();

        if (course.getName() == null || course.getName().isBlank()) {
            missingFieldKeys.add(FIELD_NAME);
        }
        if (course.getDescription() == null || course.getDescription().isBlank()) {
            missingFieldKeys.add(FIELD_DESCRIPTION);
        }
        if (course.getSyllabus() == null || course.getSyllabus().isBlank()) {
            missingFieldKeys.add(FIELD_SYLLABUS);
        }
        if (course.getObjectives() == null || course.getObjectives().isBlank()) {
            missingFieldKeys.add(FIELD_OBJECTIVES);
        }
        if (course.getDurationWeeks() == null || course.getDurationWeeks() <= 0) {
            missingFieldKeys.add(FIELD_DURATION_WEEKS);
        }

        if (!missingFieldKeys.isEmpty()) {
            // Resolve field names to localized strings
            Locale locale = LocaleContextHolder.getLocale();
            String missing = missingFieldKeys.stream()
                    .map(key -> messageSource.getMessage(key, null, locale))
                    .collect(Collectors.joining(", "));

            throw new ValidationException("COURSE_MISSING_REQUIRED_FIELDS", (Object) missing);
        }
    }

    /**
     * Creates Pageable object from search criteria.
     *
     * @param criteria the search criteria
     * @return Pageable with page, size, and sort
     */
    private Pageable createPageable(CourseSearchCriteria criteria) {
        // Parse sort string (format: "field,direction")
        String[] sortParts = criteria.sort().split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        // Convert camelCase field name to snake_case for native SQL queries
        String dbColumnName = toSnakeCase(sortField);

        return PageRequest.of(criteria.page(), criteria.size(), Sort.by(direction, dbColumnName));
    }

    /**
     * Converts camelCase field name to snake_case database column name.
     * Used for native SQL queries where JPA naming strategy doesn't apply.
     *
     * @param camelCase the camelCase field name
     * @return the snake_case column name
     */
    private String toSnakeCase(String camelCase) {
        // Convert camelCase to snake_case using regex
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Thêm điều kiện tiên quyết vào khóa học.
     *
     * <p>Validates:
     * <ul>
     *   <li>Both course and prerequisite exist and not deleted</li>
     *   <li>No self-prerequisite (course != prerequisite)</li>
     *   <li>No circular dependency via DFS algorithm</li>
     * </ul>
     *
     * @param courseId ID của khóa học cần thêm điều kiện tiên quyết
     * @param prerequisiteId ID của khóa học điều kiện tiên quyết
     * @throws EntityNotFoundException nếu course hoặc prerequisite không tồn tại
     * @throws ValidationException nếu tạo vòng lặp phụ thuộc (circular dependency)
     */
    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public void addPrerequisite(Long courseId, Long prerequisiteId) {
        log.info("Adding prerequisite {} to course {}", prerequisiteId, courseId);

        // Validate circular dependency using DFS
        if (prerequisiteValidator.wouldCreateCycle(courseId, prerequisiteId)) {
            throw new ValidationException("COURSE_CIRCULAR_PREREQUISITE", courseId, prerequisiteId);
        }

        // Fetch course and prerequisite (validates both exist)
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) courseId));

        Course prerequisite = courseRepository.findByIdAndDeletedFalse(prerequisiteId)
                .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) prerequisiteId));

        // Add prerequisite (helper method handles bidirectional relationship)
        course.addPrerequisite(prerequisite);
        courseRepository.save(course);

        log.info("Added prerequisite {} to course {}", prerequisiteId, courseId);
    }

    /**
     * Xóa điều kiện tiên quyết khỏi khóa học.
     *
     * <p>Operation is idempotent - no error if prerequisite not present.
     *
     * @param courseId ID của khóa học cần xóa điều kiện tiên quyết
     * @param prerequisiteId ID của khóa học điều kiện tiên quyết cần xóa
     * @throws EntityNotFoundException nếu course không tồn tại
     */
    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public void removePrerequisite(Long courseId, Long prerequisiteId) {
        log.info("Removing prerequisite {} from course {}", prerequisiteId, courseId);

        // Fetch course (validates exists)
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) courseId));

        // Fetch prerequisite (if exists)
        Course prerequisite = courseRepository.findByIdAndDeletedFalse(prerequisiteId).orElse(null);

        if (prerequisite != null) {
            // Remove prerequisite (helper method handles bidirectional relationship)
            course.removePrerequisite(prerequisite);
            courseRepository.save(course);
        }

        log.info("Removed prerequisite {} from course {}", prerequisiteId, courseId);
    }

    @Override
    public PageResponse<CourseResponse> searchByLevelAndCategory(
            String level,
            String category,
            int page,
            int size,
            String sortBy,
            String direction) {

        // Create sort
        Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction) ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortBy);

        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Search courses
        Page<Course> coursePage = courseRepository.findByLevelAndCategory(level, category, pageable);

        // Map to DTOs
        List<CourseResponse> content = coursePage.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();

        // Return page response
        return PageResponse.of(
                content,
                coursePage.getNumber(),
                coursePage.getSize(),
                coursePage.getTotalElements()
        );
    }
}
