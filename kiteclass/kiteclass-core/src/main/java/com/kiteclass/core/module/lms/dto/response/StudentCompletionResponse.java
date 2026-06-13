package com.kiteclass.core.module.lms.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * Per-student completion summary within a course completion roster.
 *
 * @param userId             the student user ID
 * @param completedLessons   number of lessons the student completed
 * @param progressPercent    completedLessons / totalLessons * 100 (0..100, 2dp)
 * @param completedLessonIds IDs of the completed lessons
 * @author KiteClass Team
 * @since 2.9.0
 */
@Builder
public record StudentCompletionResponse(
        Long userId,
        long completedLessons,
        double progressPercent,
        List<Long> completedLessonIds
) {
}
