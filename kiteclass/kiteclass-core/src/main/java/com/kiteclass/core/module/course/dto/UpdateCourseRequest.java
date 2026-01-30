package com.kiteclass.core.module.course.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing course.
 *
 * <p>All fields are optional - only non-null fields will be updated.
 * Contains validation annotations to ensure data integrity when fields are provided.
 *
 * <p>Update restrictions based on course status (BR-COURSE-002):
 * <ul>
 *   <li>DRAFT: Can update all fields</li>
 *   <li>PUBLISHED: Can only update description, syllabus, objectives, price (with warning), coverImageUrl</li>
 *   <li>ARCHIVED: Read-only, no updates allowed</li>
 * </ul>
 *
 * <p>Note: Code and teacherId are typically not updated after creation.
 *
 * @param name Course name
 * @param description Course description
 * @param syllabus Course syllabus
 * @param objectives Learning objectives
 * @param prerequisites Course prerequisites
 * @param targetAudience Target audience description
 * @param durationWeeks Course duration in weeks
 * @param totalSessions Total number of sessions
 * @param price Course price in VND
 * @param coverImageUrl URL to course cover image
 * @author KiteClass Team
 * @since 2.4.0
 */
public record UpdateCourseRequest(
        @Size(min = 5, max = 200, message = "Tên khóa học phải từ 5-200 ký tự")
        String name,

        @Size(max = 5000, message = "Mô tả không được quá 5000 ký tự")
        String description,

        @Size(max = 10000, message = "Syllabus không được quá 10000 ký tự")
        String syllabus,

        @Size(max = 5000, message = "Mục tiêu học tập không được quá 5000 ký tự")
        String objectives,

        @Size(max = 2000, message = "Điều kiện tiên quyết không được quá 2000 ký tự")
        String prerequisites,

        @Size(max = 1000, message = "Đối tượng mục tiêu không được quá 1000 ký tự")
        String targetAudience,

        @Min(value = 1, message = "Thời lượng phải >= 1 tuần")
        Integer durationWeeks,

        @Min(value = 1, message = "Tổng số buổi học phải >= 1")
        Integer totalSessions,

        @DecimalMin(value = "0.0", message = "Giá khóa học phải >= 0")
        BigDecimal price,

        @Size(max = 500, message = "URL ảnh bìa không được quá 500 ký tự")
        String coverImageUrl
) {
}
