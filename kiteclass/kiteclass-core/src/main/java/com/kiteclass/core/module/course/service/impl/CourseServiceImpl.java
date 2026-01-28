package com.kiteclass.core.module.course.service.impl;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.constant.TeacherCourseRole;
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
import com.kiteclass.core.module.teacher.entity.TeacherCourse;
import com.kiteclass.core.module.teacher.repository.TeacherCourseRepository;
import com.kiteclass.core.module.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherCourseRepository teacherCourseRepository;
    private final CourseMapper courseMapper;

    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public CourseResponse createCourse(CreateCourseRequest request) {
        log.info("Creating course with code: {}", request.code());

        // BR-COURSE-001: Validate code uniqueness
        if (courseRepository.existsByCodeAndDeletedFalse(request.code())) {
            log.warn("Duplicate course code: {}", request.code());
            throw new DuplicateResourceException("code", request.code());
        }

        // Validate teacher exists and is active
        teacherRepository.findByIdAndDeletedFalse(request.teacherId())
                .orElseThrow(() -> {
                    log.warn("Teacher not found with ID: {}", request.teacherId());
                    return new EntityNotFoundException("Teacher", request.teacherId());
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "courses", key = "#id")
    public CourseResponse getCourseById(Long id) {
        log.debug("Fetching course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("Course", id);
                });

        return courseMapper.toResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getCourses(CourseSearchCriteria criteria) {
        log.debug("Searching courses with criteria: search='{}', status='{}', teacherId='{}', page={}",
                criteria.search(), criteria.status(), criteria.teacherId(), criteria.page());

        // Parse status
        CourseStatus courseStatus = criteria.status() != null ?
                CourseStatus.valueOf(criteria.status()) : null;

        // Parse sort
        Pageable pageable = createPageable(criteria);

        // Search courses
        Page<Course> coursePage = courseRepository.findBySearchCriteria(
                criteria.search(),
                courseStatus,
                criteria.teacherId(),
                pageable
        );

        Page<CourseResponse> responsePage = coursePage.map(courseMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {
        log.info("Updating course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("Course", id);
                });

        // BR-COURSE-002: Check status-based update restrictions
        validateUpdateAllowed(course, request);

        courseMapper.updateEntity(course, request);
        Course updated = courseRepository.save(course);

        log.info("Updated course with ID: {}", id);
        return courseMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public void deleteCourse(Long id) {
        log.info("Deleting course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("Course", id);
                });

        // Only DRAFT courses can be deleted
        if (!course.canBeDeleted()) {
            log.warn("Cannot delete course with status: {}", course.getStatus());
            throw new ValidationException(
                    "Không thể xóa khóa học với trạng thái " + course.getStatus() +
                    ". Chỉ có thể xóa khóa học ở trạng thái DRAFT."
            );
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

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public CourseResponse publishCourse(Long id) {
        log.info("Publishing course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("Course", id);
                });

        // Validate current status
        if (!CourseStatus.DRAFT.equals(course.getStatus())) {
            log.warn("Cannot publish course with status: {}", course.getStatus());
            throw new ValidationException(
                    "Chỉ có thể publish khóa học ở trạng thái DRAFT. Trạng thái hiện tại: " + course.getStatus()
            );
        }

        // Validate required fields for publishing
        validatePublishRequirements(course);

        // Change status to PUBLISHED
        course.setStatus(CourseStatus.PUBLISHED);
        Course published = courseRepository.save(course);

        log.info("Published course with ID: {}", id);
        return courseMapper.toResponse(published);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public CourseResponse archiveCourse(Long id) {
        log.info("Archiving course with ID: {}", id);

        Course course = courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID: {}", id);
                    return new EntityNotFoundException("Course", id);
                });

        // Validate current status
        if (!CourseStatus.PUBLISHED.equals(course.getStatus())) {
            log.warn("Cannot archive course with status: {}", course.getStatus());
            throw new ValidationException(
                    "Chỉ có thể archive khóa học ở trạng thái PUBLISHED. Trạng thái hiện tại: " + course.getStatus()
            );
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
            throw new ValidationException(
                    "Không thể cập nhật khóa học đã lưu trữ (ARCHIVED). Khóa học ở trạng thái này chỉ được đọc."
            );
        }

        // PUBLISHED courses have limited edits
        if (course.isPublished()) {
            // Check if trying to update restricted fields
            if (request.name() != null || request.durationWeeks() != null ||
                request.totalSessions() != null || request.prerequisites() != null ||
                request.targetAudience() != null) {
                throw new ValidationException(
                        "Khóa học đã PUBLISHED chỉ có thể cập nhật: description, syllabus, objectives, price, coverImageUrl. " +
                        "Không thể thay đổi: name, durationWeeks, totalSessions, prerequisites, targetAudience."
                );
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
        StringBuilder missingFields = new StringBuilder();

        if (course.getName() == null || course.getName().isBlank()) {
            missingFields.append("name, ");
        }
        if (course.getDescription() == null || course.getDescription().isBlank()) {
            missingFields.append("description, ");
        }
        if (course.getSyllabus() == null || course.getSyllabus().isBlank()) {
            missingFields.append("syllabus, ");
        }
        if (course.getObjectives() == null || course.getObjectives().isBlank()) {
            missingFields.append("objectives, ");
        }
        if (course.getDurationWeeks() == null || course.getDurationWeeks() <= 0) {
            missingFields.append("durationWeeks, ");
        }

        if (missingFields.length() > 0) {
            // Remove trailing comma and space
            String missing = missingFields.substring(0, missingFields.length() - 2);
            throw new ValidationException(
                    "Không thể publish khóa học. Thiếu các trường bắt buộc: " + missing
            );
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

        return PageRequest.of(criteria.page(), criteria.size(), Sort.by(direction, sortField));
    }
}
