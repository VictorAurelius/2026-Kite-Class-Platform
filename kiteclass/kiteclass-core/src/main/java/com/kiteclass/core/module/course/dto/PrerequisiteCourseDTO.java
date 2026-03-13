package com.kiteclass.core.module.course.dto;

/**
 * DTO for prerequisite course information.
 *
 * <p>Used in CourseResponse to show minimal info about prerequisite courses.
 *
 * @param id Course ID
 * @param name Course name
 * @param code Course code
 * @author KiteClass Team
 * @since 2.5.0
 */
public record PrerequisiteCourseDTO(
        Long id,
        String name,
        String code
) {
}
