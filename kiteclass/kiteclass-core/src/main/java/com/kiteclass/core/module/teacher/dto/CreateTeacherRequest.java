package com.kiteclass.core.module.teacher.dto;

import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new teacher.
 *
 * <p>Contains validation annotations to ensure data integrity:
 * <ul>
 *   <li>Name: required, 2-100 characters</li>
 *   <li>Email: required, valid email format, max 255 characters</li>
 *   <li>Phone: Vietnamese format (10 digits starting with 0)</li>
 *   <li>Specialization: max 100 characters</li>
 *   <li>Bio: max 2000 characters</li>
 *   <li>Qualification: max 200 characters</li>
 *   <li>Experience years: >= 0</li>
 * </ul>
 *
 * @param name Teacher's full name (required)
 * @param email Teacher's email address (required)
 * @param phoneNumber Teacher's phone number
 * @param specialization Teacher's specialization
 * @param bio Teacher's biography
 * @param qualification Teacher's qualification
 * @param experienceYears Years of teaching experience
 * @author KiteClass Team
 * @since 2.3.1
 */
public record CreateTeacherRequest(
        @NotBlank(message = "Tên là bắt buộc")
        @Size(min = 2, max = 100, message = "Tên phải từ 2-100 ký tự")
        String name,

        @NotBlank(message = "Email là bắt buộc")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)")
        String phoneNumber,

        @Size(max = 100, message = "Chuyên môn không được quá 100 ký tự")
        String specialization,

        @Size(max = 2000, message = "Giới thiệu không được quá 2000 ký tự")
        String bio,

        @Size(max = 200, message = "Trình độ không được quá 200 ký tự")
        String qualification,

        @Min(value = 0, message = "Số năm kinh nghiệm phải >= 0")
        Integer experienceYears
) {
}
