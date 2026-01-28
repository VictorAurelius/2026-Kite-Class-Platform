package com.kiteclass.core.module.teacher.dto;

import com.kiteclass.core.common.constant.TeacherStatus;
import jakarta.validation.constraints.*;

/**
 * Request DTO for updating an existing teacher.
 *
 * <p>All fields are optional - only non-null fields will be updated.
 * Contains validation annotations to ensure data integrity when fields are provided.
 *
 * @param name Teacher's full name
 * @param email Teacher's email address
 * @param phoneNumber Teacher's phone number
 * @param specialization Teacher's specialization
 * @param bio Teacher's biography
 * @param qualification Teacher's qualification
 * @param experienceYears Years of teaching experience
 * @param status Teacher's status
 * @author KiteClass Team
 * @since 2.3.1
 */
public record UpdateTeacherRequest(
        @Size(min = 2, max = 100, message = "Tên phải từ 2-100 ký tự")
        String name,

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
        Integer experienceYears,

        TeacherStatus status
) {
}
