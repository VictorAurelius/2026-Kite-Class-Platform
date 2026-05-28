package com.kiteclass.core.module.lms.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.lms.dto.response.CourseProgressResponse;
import com.kiteclass.core.module.lms.dto.response.LessonProgressResponse;
import com.kiteclass.core.module.lms.service.LessonProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for lesson progress tracking operations.
 * Handles student progress through lessons and course-level progress.
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/lms/progress")
@RequiredArgsConstructor
@Tag(name = "LMS Progress", description = "Lesson and course progress tracking APIs")
public class LessonProgressController {

    private final LessonProgressService lessonProgressService;

    /**
     * Mark a lesson as completed for a student.
     * Idempotent operation - can be called multiple times safely.
     *
     * @param lessonId the lesson ID
     * @param userId student user ID
     * @return updated progress response
     */
    @PostMapping("/lessons/{lessonId}/complete")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Mark lesson as completed",
               description = "Idempotent operation. Creates progress record if not exists, "
                   + "updates if already exists. Publishes LessonCompletedEvent.")
    public ApiResponse<LessonProgressResponse> completeLesson(
            @PathVariable Long lessonId,
            @Parameter(description = "Student user ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("POST /api/v1/lms/progress/lessons/{}/complete - userId: {}", lessonId, userId);
        return ApiResponse.success(lessonProgressService.completeLesson(lessonId, userId));
    }

    /**
     * Get course progress for a student.
     * Calculates completion percentage based on completed vs total lessons.
     *
     * @param courseId the course ID
     * @param userId student user ID
     * @return course progress response
     */
    @GetMapping("/courses/{courseId}")
    @Operation(summary = "Get course progress",
               description = "Returns total lessons, completed lessons, and progress percentage.")
    public ApiResponse<CourseProgressResponse> getCourseProgress(
            @PathVariable Long courseId,
            @Parameter(description = "Student user ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("GET /api/v1/lms/progress/courses/{} - userId: {}", courseId, userId);
        return ApiResponse.success(lessonProgressService.getCourseProgress(courseId, userId));
    }

    /**
     * Get lesson progress for a student.
     *
     * @param lessonId the lesson ID
     * @param userId student user ID
     * @return lesson progress response (null if no progress exists)
     */
    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Get lesson progress",
               description = "Returns progress record for a specific lesson. Returns null if student hasn't started the lesson.")
    public ApiResponse<LessonProgressResponse> getLessonProgress(
            @PathVariable Long lessonId,
            @Parameter(description = "Student user ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("GET /api/v1/lms/progress/lessons/{} - userId: {}", lessonId, userId);
        return ApiResponse.success(lessonProgressService.getLessonProgress(lessonId, userId));
    }
}
