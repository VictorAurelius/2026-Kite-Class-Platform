package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.module.lms.dto.request.CreateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLearningResourceRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLessonRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateLessonRequest;
import com.kiteclass.core.module.lms.dto.response.CourseModuleDetailResponse;
import com.kiteclass.core.module.lms.dto.response.CourseModuleResponse;
import com.kiteclass.core.module.lms.dto.response.LearningResourceResponse;
import com.kiteclass.core.module.lms.dto.response.LessonDetailResponse;
import com.kiteclass.core.module.lms.dto.response.LessonResponse;

import java.util.List;

/**
 * Service interface for LMS (Learning Management System) operations.
 * Provides 3-tier course structure management (Course → Module → Lesson),
 * trial lesson access for guests, and CRUD operations for teachers.
 *
 * <p>Access Control Matrix:
 * <ul>
 *   <li>Guest (unauthenticated): View course structure + trial lessons only</li>
 *   <li>Student (enrolled): View all lessons + track progress</li>
 *   <li>Teacher (course owner): Full CRUD on modules/lessons</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
public interface LmsService {

    // ==================== Public Endpoints (Guest Access) ====================

    /**
     * Get course structure for guest users (unauthenticated).
     * Returns modules with TRIAL lessons only (BR-LMS-001).
     * Manual tenant context setting required (see implementation).
     *
     * @param courseId the course ID
     * @return list of modules with trial lessons
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.ValidationException if course not published
     */
    List<CourseModuleDetailResponse> getCourseStructurePublic(Long courseId);

    /**
     * Get lesson detail for guest users (unauthenticated).
     * Only allows access to TRIAL lessons (BR-LMS-001).
     *
     * @param lessonId the lesson ID
     * @return lesson detail with resources
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lesson not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if lesson is not trial
     */
    LessonDetailResponse getLessonPublic(Long lessonId);

    // ==================== Student Endpoints (Authenticated) ====================

    /**
     * Get course structure for enrolled student.
     * Returns modules with ALL lessons (trial + paid).
     * Requires active enrollment (BR-LMS-002).
     *
     * @param courseId the course ID
     * @param userId the student user ID
     * @return list of modules with all lessons
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if student not enrolled
     */
    List<CourseModuleDetailResponse> getCourseStructureForStudent(Long courseId, Long userId);

    /**
     * Get lesson detail for enrolled student.
     * Allows access to trial lessons OR paid lessons if enrolled (BR-LMS-002).
     *
     * @param lessonId the lesson ID
     * @param userId the student user ID
     * @return lesson detail with resources
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lesson not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if student not enrolled and lesson not trial
     */
    LessonDetailResponse getLessonForStudent(Long lessonId, Long userId);

    // ==================== Teacher Endpoints - Module CRUD ====================

    /**
     * Create a new module for a course (teacher only).
     * Only course owner can create modules.
     *
     * @param courseId the course ID
     * @param request the create module request
     * @param teacherId the teacher ID (must be course owner)
     * @return created module response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     * @throws com.kiteclass.core.common.exception.ValidationException if order number already exists
     */
    CourseModuleResponse createModule(Long courseId, CreateCourseModuleRequest request, Long teacherId);

    /**
     * Update an existing module (teacher only).
     * Only course owner can update modules.
     *
     * @param moduleId the module ID
     * @param request the update module request
     * @param teacherId the teacher ID (must be course owner)
     * @return updated module response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if module not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     * @throws com.kiteclass.core.common.exception.ValidationException if order number conflict
     */
    CourseModuleResponse updateModule(Long moduleId, UpdateCourseModuleRequest request, Long teacherId);

    /**
     * Delete a module (teacher only).
     * Only course owner can delete modules.
     * Cannot delete module if it has lessons (BR-LMS-007).
     *
     * @param moduleId the module ID
     * @param teacherId the teacher ID (must be course owner)
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if module not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     * @throws com.kiteclass.core.common.exception.ValidationException if module has lessons
     */
    void deleteModule(Long moduleId, Long teacherId);

    /**
     * Get module detail (teacher only).
     * Only course owner can view module details.
     *
     * @param moduleId the module ID
     * @param teacherId the teacher ID (must be course owner)
     * @return module detail with all lessons
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if module not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     */
    CourseModuleDetailResponse getModule(Long moduleId, Long teacherId);

    // ==================== Teacher Endpoints - Lesson CRUD ====================

    /**
     * Create a new lesson for a module (teacher only).
     * Only course owner can create lessons.
     *
     * @param moduleId the module ID
     * @param request the create lesson request
     * @param teacherId the teacher ID (must be course owner)
     * @return created lesson response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if module not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     * @throws com.kiteclass.core.common.exception.ValidationException if order number already exists
     */
    LessonResponse createLesson(Long moduleId, CreateLessonRequest request, Long teacherId);

    /**
     * Update an existing lesson (teacher only).
     * Only course owner can update lessons.
     *
     * @param lessonId the lesson ID
     * @param request the update lesson request
     * @param teacherId the teacher ID (must be course owner)
     * @return updated lesson response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lesson not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     * @throws com.kiteclass.core.common.exception.ValidationException if order number conflict
     */
    LessonResponse updateLesson(Long lessonId, UpdateLessonRequest request, Long teacherId);

    /**
     * Delete a lesson (teacher only).
     * Only course owner can delete lessons.
     *
     * @param lessonId the lesson ID
     * @param teacherId the teacher ID (must be course owner)
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lesson not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     */
    void deleteLesson(Long lessonId, Long teacherId);

    /**
     * Get lesson detail for teacher.
     * Only course owner can view lesson details.
     *
     * @param lessonId the lesson ID
     * @param teacherId the teacher ID (must be course owner)
     * @return lesson detail with resources
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lesson not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     */
    LessonDetailResponse getLessonForTeacher(Long lessonId, Long teacherId);

    // ==================== Teacher Endpoints - Learning Resource CRUD ====================

    /**
     * Add a learning resource to a lesson (teacher only).
     * Only course owner can add resources.
     *
     * @param lessonId the lesson ID
     * @param request the create resource request
     * @param teacherId the teacher ID (must be course owner)
     * @return created resource response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lesson not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     */
    LearningResourceResponse addResource(Long lessonId, CreateLearningResourceRequest request, Long teacherId);

    /**
     * Delete a learning resource (teacher only).
     * Only course owner can delete resources.
     *
     * @param resourceId the resource ID
     * @param teacherId the teacher ID (must be course owner)
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if resource not found
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException if teacher is not course owner
     */
    void deleteResource(Long resourceId, Long teacherId);
}
