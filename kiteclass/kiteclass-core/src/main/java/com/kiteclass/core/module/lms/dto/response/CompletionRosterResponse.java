package com.kiteclass.core.module.lms.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * Completion roster for a course — teacher view of which students completed
 * which lessons (BR-LMS-016..020 aggregate).
 *
 * @param courseId     the course ID
 * @param totalLessons total non-deleted lessons in the course
 * @param students     per-student completion summary (only students with ≥1 completion)
 * @author KiteClass Team
 * @since 2.9.0
 */
@Builder
public record CompletionRosterResponse(
        Long courseId,
        long totalLessons,
        List<StudentCompletionResponse> students
) {
}
