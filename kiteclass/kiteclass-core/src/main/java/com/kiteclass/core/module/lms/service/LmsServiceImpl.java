package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.lms.dto.request.CreateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLearningResourceRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLessonRequest;
import com.kiteclass.core.module.lms.dto.request.ReorderRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateLessonRequest;
import com.kiteclass.core.module.lms.dto.response.CompletionRosterResponse;
import com.kiteclass.core.module.lms.dto.response.CourseModuleDetailResponse;
import com.kiteclass.core.module.lms.dto.response.CourseModuleResponse;
import com.kiteclass.core.module.lms.dto.response.LearningResourceResponse;
import com.kiteclass.core.module.lms.dto.response.LessonDetailResponse;
import com.kiteclass.core.module.lms.dto.response.LessonResponse;
import com.kiteclass.core.module.lms.dto.response.StudentCompletionResponse;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.LearningResource;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.entity.LessonProgress;
import com.kiteclass.core.module.lms.mapper.LmsMapper;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
import com.kiteclass.core.module.lms.repository.LearningResourceRepository;
import com.kiteclass.core.module.lms.repository.LessonProgressRepository;
import com.kiteclass.core.module.lms.repository.LessonRepository;
import com.kiteclass.core.module.storage.dto.PresignedUploadRequest;
import com.kiteclass.core.module.storage.dto.PresignedUploadResponse;
import com.kiteclass.core.module.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Implementation of {@link LmsService}.
 * Handles 3-tier course structure (Course → Module → Lesson) with trial lesson access.
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsServiceImpl implements LmsService {

    private final CourseModuleRepository courseModuleRepository;
    private final LessonRepository lessonRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseRepository courseRepository;
    private final com.kiteclass.core.module.enrollment.repository.EnrollmentRepository enrollmentRepository;
    private final com.kiteclass.core.module.clazz.repository.ClassRepository classRepository;
    private final StorageService storageService;
    private final LmsMapper lmsMapper;

    // ==================== Public Endpoints (Guest Access) ====================

    @Override
    @Transactional(readOnly = true)
    public List<CourseModuleDetailResponse> getCourseStructurePublic(Long courseId) {
        log.info("Fetching public course structure for courseId: {}", courseId);

        // 1. Look up course WITHOUT tenant context (guest access)
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) courseId));

        // 2. Verify course is PUBLISHED (BR: only published courses visible to guests)
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            log.warn("Course {} is not published (status: {})", courseId, course.getStatus());
            throw new ValidationException("COURSE_NOT_PUBLISHED", courseId, course.getStatus());
        }

        // 3. Set tenant context manually for guest access
        UUID previousTenant = TenantContext.isSet() ? TenantContext.getCurrentTenant() : null;
        try {
            TenantContext.setCurrentTenant(course.getInstanceId());

            // 4. Fetch modules + TRIAL lessons only (BR-LMS-001)
            List<CourseModule> modules = courseModuleRepository
                    .findByCourseIdAndDeletedFalseOrderByOrderNumber(courseId);

            return modules.stream()
                    .map(module -> {
                        List<Lesson> trialLessons = lessonRepository
                                .findByModuleIdAndIsTrialTrueAndDeletedFalseOrderByOrderNumber(module.getId());
                        return buildModuleDetailResponse(module, trialLessons);
                    })
                    .toList();
        } finally {
            // 5. Restore previous context
            if (previousTenant != null) {
                TenantContext.setCurrentTenant(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonPublic(Long lessonId) {
        log.info("Fetching public lesson detail for lessonId: {}", lessonId);

        // GAP-1118: capture the prior tenant so the manual context set inside
        // findLessonWithTenantContext is always restored — otherwise the guest's
        // resolved instanceId leaks onto the pooled thread for the next request.
        UUID previousTenant = TenantContext.isSet() ? TenantContext.getCurrentTenant() : null;
        try {
            // Find lesson - manual tenant context handling
            Lesson lesson = findLessonWithTenantContext(lessonId);

            // BR-LMS-001: Only trial lessons accessible to guests
            if (!lesson.isTrialLesson()) {
                log.warn("Lesson {} is not a trial lesson, denying guest access", lessonId);
                throw new PermissionDeniedException("TRIAL_LESSON_REQUIRED", lessonId);
            }

            return buildLessonDetailResponse(lesson);
        } finally {
            // Restore previous context (or clear for guests) to prevent cross-tenant leak.
            if (previousTenant != null) {
                TenantContext.setCurrentTenant(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    // ==================== Student Endpoints (Authenticated) ====================

    @Override
    @Transactional(readOnly = true)
    public List<CourseModuleDetailResponse> getCourseStructureForStudent(Long courseId, Long userId) {
        log.info("Fetching course structure for student userId: {} courseId: {}", userId, courseId);

        // Students can see the full course OUTLINE before enrolling (better UX for
        // marketing/preview). BR-LMS-002: but paid lesson BODY (content + videoUrl)
        // is paywalled — only enrolled students receive it. Non-enrolled students get
        // metadata only for paid lessons; trial lessons always include full body.
        // GAP-1115: previously ALL lessons were returned with full content, leaking
        // paid material to non-enrolled students.
        boolean enrolled = isStudentEnrolledInCourse(userId, courseId);

        // Fetch modules + ALL lessons (outline visible regardless of enrollment)
        List<CourseModule> modules = courseModuleRepository
                .findByCourseIdAndDeletedFalseOrderByOrderNumber(courseId);

        return modules.stream()
                .map(module -> {
                    List<Lesson> allLessons = lessonRepository
                            .findByModuleIdAndDeletedFalseOrderByOrderNumber(module.getId());
                    return buildStudentModuleDetailResponse(module, allLessons, enrolled);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonForStudent(Long lessonId, Long userId) {
        log.info("Fetching lesson detail for student userId: {} lessonId: {}", userId, lessonId);

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));

        // If trial, allow access immediately
        if (lesson.isTrialLesson()) {
            log.debug("Lesson {} is trial, allowing access", lessonId);
            return buildLessonDetailResponse(lesson);
        }

        // If not trial, verify enrollment (BR-LMS-002)
        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId())
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) lesson.getModuleId()));
        verifyStudentEnrollment(userId, module.getCourseId());

        return buildLessonDetailResponse(lesson);
    }

    // ==================== Teacher Endpoints - Module CRUD ====================

    @Override
    @Transactional
    public CourseModuleResponse createModule(Long courseId, CreateCourseModuleRequest request, Long teacherId) {
        log.info("Creating module for courseId: {} by teacherId: {}", courseId, teacherId);

        // 1. Verify course exists and teacher is owner
        Course course = verifyCourseOwnership(courseId, teacherId);

        // 2. Validate order number uniqueness (BR-LMS-006)
        if (courseModuleRepository.existsByCourseIdAndOrderNumberAndDeletedFalse(
                courseId, request.orderNumber())) {
            throw new ValidationException("DUPLICATE_ORDER_NUMBER", courseId, request.orderNumber());
        }

        // 3. Create module
        CourseModule module = lmsMapper.toModuleEntity(request);
        module.setCourseId(courseId);
        module.setInstanceId(course.getInstanceId());

        CourseModule savedModule = courseModuleRepository.save(module);
        log.info("Created module {} for course {}", savedModule.getId(), courseId);

        return lmsMapper.toModuleResponse(savedModule);
    }

    @Override
    @Transactional
    public CourseModuleResponse updateModule(Long moduleId, UpdateCourseModuleRequest request, Long teacherId) {
        log.info("Updating moduleId: {} by teacherId: {}", moduleId, teacherId);

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) moduleId));

        // Verify teacher is course owner
        verifyCourseOwnership(module.getCourseId(), teacherId);

        // Validate order number uniqueness if changing (BR-LMS-006)
        if (request.orderNumber() != null &&
                !request.orderNumber().equals(module.getOrderNumber()) &&
                courseModuleRepository.existsByCourseIdAndOrderNumberAndIdNotAndDeletedFalse(
                        module.getCourseId(), request.orderNumber(), moduleId)) {
            throw new ValidationException("DUPLICATE_ORDER_NUMBER", module.getCourseId(), request.orderNumber());
        }

        // Update module
        lmsMapper.updateModuleEntity(module, request);
        CourseModule savedModule = courseModuleRepository.save(module);

        log.info("Updated module {}", moduleId);
        return lmsMapper.toModuleResponse(savedModule);
    }

    @Override
    @Transactional
    public void deleteModule(Long moduleId, Long teacherId) {
        log.info("Deleting moduleId: {} by teacherId: {}", moduleId, teacherId);

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) moduleId));

        // Verify teacher is course owner
        verifyCourseOwnership(module.getCourseId(), teacherId);

        // BR-LMS-007: Cannot delete module if it has lessons
        long lessonCount = lessonRepository.countByModuleIdAndDeletedFalse(moduleId);
        if (lessonCount > 0) {
            throw new ValidationException("MODULE_HAS_LESSONS", moduleId, lessonCount);
        }

        // Soft delete
        module.setDeleted(true);
        courseModuleRepository.save(module);

        log.info("Deleted module {}", moduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseModuleDetailResponse getModule(Long moduleId, Long teacherId) {
        log.info("Fetching module {} for teacher {}", moduleId, teacherId);

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) moduleId));

        // Verify teacher is course owner
        verifyCourseOwnership(module.getCourseId(), teacherId);

        List<Lesson> lessons = lessonRepository.findByModuleIdAndDeletedFalseOrderByOrderNumber(moduleId);
        return buildModuleDetailResponse(module, lessons);
    }

    // ==================== Teacher Endpoints - Lesson CRUD ====================

    @Override
    @Transactional
    public LessonResponse createLesson(Long moduleId, CreateLessonRequest request, Long teacherId) {
        log.info("Creating lesson for moduleId: {} by teacherId: {}", moduleId, teacherId);

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) moduleId));

        // Verify teacher is course owner
        Course course = verifyCourseOwnership(module.getCourseId(), teacherId);

        // Validate order number uniqueness (BR-LMS-008)
        if (lessonRepository.existsByModuleIdAndOrderNumberAndDeletedFalse(
                moduleId, request.orderNumber())) {
            throw new ValidationException("DUPLICATE_ORDER_NUMBER", moduleId, request.orderNumber());
        }

        // Create lesson
        Lesson lesson = lmsMapper.toLessonEntity(request);
        lesson.setModuleId(moduleId);
        lesson.setInstanceId(course.getInstanceId());
        if (lesson.getIsTrial() == null) {
            lesson.setIsTrial(false);  // Default to false
        }

        Lesson savedLesson = lessonRepository.save(lesson);
        log.info("Created lesson {} for module {}", savedLesson.getId(), moduleId);

        return lmsMapper.toLessonResponse(savedLesson);
    }

    @Override
    @Transactional
    public LessonResponse updateLesson(Long lessonId, UpdateLessonRequest request, Long teacherId) {
        log.info("Updating lessonId: {} by teacherId: {}", lessonId, teacherId);

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId())
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) lesson.getModuleId()));

        // Verify teacher is course owner
        verifyCourseOwnership(module.getCourseId(), teacherId);

        // Validate order number uniqueness if changing (BR-LMS-008)
        if (request.orderNumber() != null &&
                !request.orderNumber().equals(lesson.getOrderNumber()) &&
                lessonRepository.existsByModuleIdAndOrderNumberAndIdNotAndDeletedFalse(
                        lesson.getModuleId(), request.orderNumber(), lessonId)) {
            throw new ValidationException("DUPLICATE_ORDER_NUMBER", lesson.getModuleId(), request.orderNumber());
        }

        // Update lesson
        lmsMapper.updateLessonEntity(lesson, request);
        Lesson savedLesson = lessonRepository.save(lesson);

        log.info("Updated lesson {}", lessonId);
        return lmsMapper.toLessonResponse(savedLesson);
    }

    @Override
    @Transactional
    public void deleteLesson(Long lessonId, Long teacherId) {
        log.info("Deleting lessonId: {} by teacherId: {}", lessonId, teacherId);

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId())
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) lesson.getModuleId()));

        // Verify teacher is course owner
        verifyCourseOwnership(module.getCourseId(), teacherId);

        // Soft delete
        lesson.setDeleted(true);
        lessonRepository.save(lesson);

        log.info("Deleted lesson {}", lessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonForTeacher(Long lessonId, Long teacherId) {
        log.info("Fetching lesson {} for teacher {}", lessonId, teacherId);

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId())
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) lesson.getModuleId()));

        // Verify teacher is course owner
        verifyCourseOwnership(module.getCourseId(), teacherId);

        return buildLessonDetailResponse(lesson);
    }

    // ==================== Teacher Endpoints - Learning Resource CRUD ====================

    @Override
    @Transactional
    public LearningResourceResponse addResource(Long lessonId, CreateLearningResourceRequest request, Long teacherId) {
        log.info("Adding resource to lessonId: {} by teacherId: {}", lessonId, teacherId);

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId())
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) lesson.getModuleId()));

        // Verify teacher is course owner
        Course course = verifyCourseOwnership(module.getCourseId(), teacherId);

        // Create resource
        LearningResource resource = lmsMapper.toResourceEntity(request);
        resource.setLessonId(lessonId);
        resource.setInstanceId(course.getInstanceId());

        LearningResource savedResource = learningResourceRepository.save(resource);
        log.info("Added resource {} to lesson {}", savedResource.getId(), lessonId);

        return lmsMapper.toResourceResponse(savedResource);
    }

    @Override
    @Transactional
    public void deleteResource(Long resourceId, Long teacherId) {
        log.info("Deleting resourceId: {} by teacherId: {}", resourceId, teacherId);

        LearningResource resource = learningResourceRepository.findByIdAndDeletedFalse(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("RESOURCE_NOT_FOUND", (Object) resourceId));

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(resource.getLessonId())
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) resource.getLessonId()));

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId())
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) lesson.getModuleId()));

        // Verify teacher is course owner
        verifyCourseOwnership(module.getCourseId(), teacherId);

        // Soft delete
        resource.setDeleted(true);
        learningResourceRepository.save(resource);

        log.info("Deleted resource {}", resourceId);
    }

    // ==================== Teacher Endpoints - Reorder (drag-drop) ====================

    @Override
    @Transactional
    public List<CourseModuleResponse> reorderModules(Long courseId, ReorderRequest request, Long teacherId) {
        log.info("Reordering {} modules for courseId: {} by teacherId: {}",
                request.items().size(), courseId, teacherId);

        verifyCourseOwnership(courseId, teacherId);

        List<CourseModule> modules = courseModuleRepository
                .findByCourseIdAndDeletedFalseOrderByOrderNumber(courseId);
        Map<Long, CourseModule> byId = modules.stream()
                .collect(Collectors.toMap(CourseModule::getId, m -> m));

        validateReorderItems(request, byId.keySet(), "MODULE");

        // Two-phase swap: park every row at a (distinct) negative order number first,
        // flush, then set the final positive order numbers. This avoids transiently
        // violating the (course_id, order_number) unique constraint mid-swap.
        for (ReorderRequest.ReorderItem item : request.items()) {
            byId.get(item.id()).setOrderNumber(-item.orderNumber());
        }
        courseModuleRepository.saveAll(byId.values());
        courseModuleRepository.flush();

        for (ReorderRequest.ReorderItem item : request.items()) {
            byId.get(item.id()).setOrderNumber(item.orderNumber());
        }
        List<CourseModule> saved = courseModuleRepository.saveAll(byId.values());
        courseModuleRepository.flush();

        log.info("Reordered {} modules for course {}", saved.size(), courseId);
        return saved.stream()
                .sorted(Comparator.comparingInt(CourseModule::getOrderNumber))
                .map(lmsMapper::toModuleResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<LessonResponse> reorderLessons(Long moduleId, ReorderRequest request, Long teacherId) {
        log.info("Reordering {} lessons for moduleId: {} by teacherId: {}",
                request.items().size(), moduleId, teacherId);

        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) moduleId));
        verifyCourseOwnership(module.getCourseId(), teacherId);

        List<Lesson> lessons = lessonRepository.findByModuleIdAndDeletedFalseOrderByOrderNumber(moduleId);
        Map<Long, Lesson> byId = lessons.stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l));

        validateReorderItems(request, byId.keySet(), "LESSON");

        // Two-phase swap (see reorderModules) for the (module_id, order_number) constraint.
        for (ReorderRequest.ReorderItem item : request.items()) {
            byId.get(item.id()).setOrderNumber(-item.orderNumber());
        }
        lessonRepository.saveAll(byId.values());
        lessonRepository.flush();

        for (ReorderRequest.ReorderItem item : request.items()) {
            byId.get(item.id()).setOrderNumber(item.orderNumber());
        }
        List<Lesson> saved = lessonRepository.saveAll(byId.values());
        lessonRepository.flush();

        log.info("Reordered {} lessons for module {}", saved.size(), moduleId);
        return saved.stream()
                .sorted(Comparator.comparingInt(Lesson::getOrderNumber))
                .map(lmsMapper::toLessonResponse)
                .toList();
    }

    // ==================== Teacher Endpoints - Resource Upload (presigned) ====================

    @Override
    @Transactional
    public PresignedUploadResponse generateResourceUploadUrl(
            Long lessonId, PresignedUploadRequest request, Long teacherId) {
        log.info("Generating resource upload URL for lessonId: {} by teacherId: {}", lessonId, teacherId);

        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));
        CourseModule module = courseModuleRepository.findByIdAndDeletedFalse(lesson.getModuleId())
                .orElseThrow(() -> new EntityNotFoundException("MODULE_NOT_FOUND", (Object) lesson.getModuleId()));

        // Only the course owner may request an upload slot for a lesson resource.
        verifyCourseOwnership(module.getCourseId(), teacherId);

        UUID tenantId = TenantContext.getCurrentTenant();
        // Reuse the central storage pipeline (MIME whitelist + quota + presigned PUT,
        // 30-min TTL). The teacher PUTs the file + confirms via the storage API, then
        // POST /lessons/{lessonId}/resources persists the LearningResource metadata row.
        return storageService.generatePresignedUploadUrl(request, teacherId, tenantId);
    }

    // ==================== Teacher Endpoints - Completion Roster ====================

    @Override
    @Transactional(readOnly = true)
    public CompletionRosterResponse getCompletionRoster(Long courseId, Long teacherId) {
        log.info("Building completion roster for courseId: {} teacherId: {}", courseId, teacherId);

        verifyCourseOwnership(courseId, teacherId);

        long totalLessons = lessonRepository.countLessonsByCourseId(courseId);

        // Group completed progress rows by student (preserve first-seen order).
        Map<Long, List<Long>> completedByUser = lessonProgressRepository
                .findCompletedProgressByCourseId(courseId)
                .stream()
                .collect(Collectors.groupingBy(
                        LessonProgress::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(LessonProgress::getLessonId, Collectors.toList())));

        List<StudentCompletionResponse> students = completedByUser.entrySet().stream()
                .map(entry -> {
                    long completed = entry.getValue().size();
                    double pct = totalLessons == 0
                            ? 0.0
                            : Math.round((completed * 10000.0) / totalLessons) / 100.0;
                    return StudentCompletionResponse.builder()
                            .userId(entry.getKey())
                            .completedLessons(completed)
                            .progressPercent(pct)
                            .completedLessonIds(entry.getValue())
                            .build();
                })
                .toList();

        return CompletionRosterResponse.builder()
                .courseId(courseId)
                .totalLessons(totalLessons)
                .students(students)
                .build();
    }

    // ==================== Helper Methods ====================

    /**
     * Validates a batch reorder request against the live sibling set.
     *
     * <p>Requires the request to cover EXACTLY the live (non-deleted) sibling IDs with
     * distinct IDs AND distinct order numbers — the drag-drop FE always sends the full
     * ordered list, so partial reorders (which could clash with un-sent siblings on the
     * unique order-number constraint) are rejected.
     *
     * @param request     the reorder request
     * @param existingIds the live sibling IDs (modules of a course, or lessons of a module)
     * @param entityLabel "MODULE" or "LESSON" — surfaced in the error code args
     * @throws ValidationException if IDs duplicate, the set is incomplete, or order numbers clash
     */
    private void validateReorderItems(ReorderRequest request, Set<Long> existingIds, String entityLabel) {
        List<ReorderRequest.ReorderItem> items = request.items();

        Set<Long> requestedIds = items.stream()
                .map(ReorderRequest.ReorderItem::id)
                .collect(Collectors.toSet());
        if (requestedIds.size() != items.size()) {
            throw new ValidationException("REORDER_DUPLICATE_ID", entityLabel);
        }

        if (!requestedIds.equals(existingIds)) {
            throw new ValidationException(
                    "REORDER_INCOMPLETE_SET", entityLabel, existingIds.size(), requestedIds.size());
        }

        long distinctOrders = items.stream()
                .map(ReorderRequest.ReorderItem::orderNumber)
                .distinct()
                .count();
        if (distinctOrders != items.size()) {
            throw new ValidationException("REORDER_DUPLICATE_ORDER", entityLabel);
        }
    }

    /**
     * Verifies that the teacher is the owner of the course.
     *
     * @param courseId the course ID
     * @param teacherId the teacher ID
     * @return the course entity
     * @throws EntityNotFoundException if course not found
     * @throws PermissionDeniedException if teacher is not the course owner
     */
    private Course verifyCourseOwnership(Long courseId, Long teacherId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) courseId));

        if (!course.getTeacherId().equals(teacherId)) {
            log.warn("Teacher {} is not owner of course {} (owner: {})",
                    teacherId, courseId, course.getTeacherId());
            throw new PermissionDeniedException("COURSE_OWNER_ONLY", teacherId, courseId);
        }

        return course;
    }

    /**
     * Verifies that student has ACTIVE enrollment in ANY class of the course.
     * BR-LMS-002: Student must have active enrollment to access paid lessons.
     *
     * @param studentId the student user ID
     * @param courseId the course ID
     * @throws PermissionDeniedException if student not enrolled in any class
     */
    private void verifyStudentEnrollment(Long studentId, Long courseId) {
        log.debug("Verifying enrollment for student {} in course {}", studentId, courseId);

        if (!isStudentEnrolledInCourse(studentId, courseId)) {
            log.warn("Student {} not enrolled in course {}", studentId, courseId);
            throw new PermissionDeniedException("STUDENT_NOT_ENROLLED_IN_COURSE");
        }

        log.debug("Student {} has active enrollment in course {}", studentId, courseId);
    }

    /**
     * Checks (without throwing) whether a student has an ACTIVE enrollment in ANY class
     * of the course. BR-LMS-002. Used both by {@link #verifyStudentEnrollment} (which
     * throws on false) and by paywall content-stripping in
     * {@link #getCourseStructureForStudent}.
     *
     * @param studentId the student user ID
     * @param courseId the course ID
     * @return true if student has an ACTIVE enrollment in at least one class of the course
     */
    private boolean isStudentEnrolledInCourse(Long studentId, Long courseId) {
        // Find all classes for this course
        List<com.kiteclass.core.module.clazz.entity.Class> courseClasses = classRepository
                .findByCourseIdAndDeletedFalse(courseId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        if (courseClasses.isEmpty()) {
            return false;
        }

        // Extract class IDs
        List<Long> classIds = courseClasses.stream()
                .map(com.kiteclass.core.module.clazz.entity.Class::getId)
                .toList();

        // Check if student has ACTIVE enrollment in ANY class
        return classIds.stream()
                .anyMatch(classId -> enrollmentRepository
                    .existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                        studentId, classId, com.kiteclass.core.common.constant.EnrollmentStatus.ACTIVE));
    }

    /**
     * Finds a lesson and sets the tenant context (when absent) for guest access.
     *
     * <p>GAP-1118: this method only SETS the context; the caller MUST restore/clear it
     * in a finally block (see {@link #getLessonPublic}). The context cannot be restored
     * here because the subsequent resource lookup in {@code buildLessonDetailResponse}
     * still needs the tenant active.
     *
     * @param lessonId the lesson ID
     * @return the lesson entity
     */
    private Lesson findLessonWithTenantContext(Long lessonId) {
        // Try to find lesson first to get its instance ID
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));

        // Set tenant context if not already set
        if (!TenantContext.isSet()) {
            TenantContext.setCurrentTenant(lesson.getInstanceId());
        }

        // Re-query with tenant filter active
        return lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("LESSON_NOT_FOUND", (Object) lessonId));
    }

    /**
     * Builds CourseModuleDetailResponse with nested lessons.
     *
     * @param module the course module
     * @param lessons the lessons in the module
     * @return CourseModuleDetailResponse
     */
    private CourseModuleDetailResponse buildModuleDetailResponse(CourseModule module, List<Lesson> lessons) {
        return CourseModuleDetailResponse.builder()
                .id(module.getId())
                .courseId(module.getCourseId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderNumber(module.getOrderNumber())
                .lessons(lmsMapper.toLessonResponseList(lessons))
                .lessonCount(lessons.size())
                .createdAt(convertToLocalDateTime(module.getCreatedAt()))
                .updatedAt(convertToLocalDateTime(module.getUpdatedAt()))
                .build();
    }

    /**
     * Builds CourseModuleDetailResponse for a student, applying the BR-LMS-002 paywall
     * (GAP-1115). Enrolled students receive full lesson bodies. Non-enrolled students
     * receive full bodies ONLY for trial lessons; paid lessons are returned as metadata
     * only (content + videoUrl stripped to null).
     *
     * @param module the course module
     * @param lessons the lessons in the module
     * @param enrolled whether the requesting student has an active enrollment in the course
     * @return CourseModuleDetailResponse with paid content gated by enrollment
     */
    private CourseModuleDetailResponse buildStudentModuleDetailResponse(
            CourseModule module, List<Lesson> lessons, boolean enrolled) {
        List<LessonResponse> lessonResponses = lessons.stream()
                .map(lesson -> {
                    LessonResponse full = lmsMapper.toLessonResponse(lesson);
                    // Full body when enrolled OR when the lesson is a free trial preview.
                    if (enrolled || lesson.isTrialLesson()) {
                        return full;
                    }
                    // Paid lesson + not enrolled: strip the paid body, keep metadata only.
                    return stripPaidLessonBody(full);
                })
                .toList();

        return CourseModuleDetailResponse.builder()
                .id(module.getId())
                .courseId(module.getCourseId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderNumber(module.getOrderNumber())
                .lessons(lessonResponses)
                .lessonCount(lessonResponses.size())
                .createdAt(convertToLocalDateTime(module.getCreatedAt()))
                .updatedAt(convertToLocalDateTime(module.getUpdatedAt()))
                .build();
    }

    /**
     * Returns a copy of the given lesson response with the paid body (content + videoUrl)
     * removed. All metadata (title, order, isTrial, estimatedDuration, timestamps) is kept
     * so the FE can still render the locked outline. BR-LMS-002 / GAP-1115.
     *
     * @param full the fully-mapped lesson response
     * @return a metadata-only LessonResponse (content + videoUrl null)
     */
    private LessonResponse stripPaidLessonBody(LessonResponse full) {
        return LessonResponse.builder()
                .id(full.id())
                .moduleId(full.moduleId())
                .title(full.title())
                .content(null)   // paywalled — non-enrolled student
                .videoUrl(null)  // paywalled — non-enrolled student
                .isTrial(full.isTrial())
                .orderNumber(full.orderNumber())
                .estimatedDuration(full.estimatedDuration())
                .createdAt(full.createdAt())
                .updatedAt(full.updatedAt())
                .build();
    }

    /**
     * Builds LessonDetailResponse with nested resources.
     *
     * @param lesson the lesson
     * @return LessonDetailResponse
     */
    private LessonDetailResponse buildLessonDetailResponse(Lesson lesson) {
        List<LearningResource> resources = learningResourceRepository
                .findByLessonIdAndDeletedFalse(lesson.getId());

        return LessonDetailResponse.builder()
                .id(lesson.getId())
                .moduleId(lesson.getModuleId())
                .title(lesson.getTitle())
                .content(lesson.getContent())
                .videoUrl(lesson.getVideoUrl())
                .isTrial(lesson.getIsTrial())
                .orderNumber(lesson.getOrderNumber())
                .estimatedDuration(lesson.getEstimatedDuration())
                .resources(lmsMapper.toResourceResponseList(resources))
                .resourceCount(resources.size())
                .createdAt(convertToLocalDateTime(lesson.getCreatedAt()))
                .updatedAt(convertToLocalDateTime(lesson.getUpdatedAt()))
                .build();
    }

    /**
     * Converts Instant to LocalDateTime using system default timezone.
     *
     * @param instant the Instant to convert
     * @return LocalDateTime in system default timezone, or null if input is null
     */
    private LocalDateTime convertToLocalDateTime(java.time.Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
