package com.kiteclass.core.module.course.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new course.
 *
 * <p>Contains validation annotations to ensure data integrity:
 * <ul>
 *   <li>Name: required, 5-200 characters</li>
 *   <li>Code: required, 3-50 characters, unique</li>
 *   <li>Description: max 5000 characters</li>
 *   <li>Syllabus: max 10000 characters</li>
 *   <li>Objectives: max 5000 characters</li>
 *   <li>Prerequisites: max 2000 characters</li>
 *   <li>Target audience: max 1000 characters</li>
 *   <li>Teacher ID: required (course creator)</li>
 *   <li>Duration weeks: >= 1 if provided</li>
 *   <li>Total sessions: >= 1 if provided</li>
 *   <li>Price: >= 0 if provided</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-COURSE-001: Code must be unique across all courses</li>
 *   <li>BR-COURSE-003: TeacherCourse with role=CREATOR will be auto-created</li>
 * </ul>
 *
 * @param name Course name (required)
 * @param code Course code (required, unique)
 * @param description Course description
 * @param syllabus Course syllabus
 * @param objectives Learning objectives
 * @param prerequisites Course prerequisites
 * @param targetAudience Target audience description
 * @param teacherId Teacher ID who creates the course (required)
 * @param durationWeeks Course duration in weeks
 * @param totalSessions Total number of sessions
 * @param price Course price in VND
 * @author KiteClass Team
 * @since 2.4.0
 */
public record CreateCourseRequest(
        @NotBlank(message = "Tên khóa học là bắt buộc")
        @Size(min = 5, max = 200, message = "Tên khóa học phải từ 5-200 ký tự")
        String name,

        @NotBlank(message = "Mã khóa học là bắt buộc")
        @Size(min = 3, max = 50, message = "Mã khóa học phải từ 3-50 ký tự")
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "Mã khóa học chỉ được chứa chữ in hoa, số và dấu gạch ngang")
        String code,

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

        @NotNull(message = "ID giảng viên là bắt buộc")
        Long teacherId,

        @Min(value = 1, message = "Thời lượng phải >= 1 tuần")
        Integer durationWeeks,

        @Min(value = 1, message = "Tổng số buổi học phải >= 1")
        Integer totalSessions,

        @DecimalMin(value = "0.0", message = "Giá khóa học phải >= 0")
        BigDecimal price
) {
}
