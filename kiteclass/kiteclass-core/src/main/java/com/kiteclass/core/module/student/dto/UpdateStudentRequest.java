package com.kiteclass.core.module.student.dto;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for updating an existing student.
 *
 * <p>Contains validation annotations similar to CreateStudentRequest,
 * but also includes status field for updating student status.
 *
 * @param name        Student's full name (required)
 * @param email       Student's email address
 * @param phone       Student's phone number
 * @param dateOfBirth Student's date of birth
 * @param gender      Student's gender
 * @param address     Student's address
 * @param status      Student's current status
 * @param note        Additional notes
 * @author KiteClass Team
 * @since 2.3.0
 */
public record UpdateStudentRequest(
        @NotBlank(message = "Tên là bắt buộc")
        @Size(min = 2, max = 100, message = "Tên phải từ 2-100 ký tự")
        String name,

        @Email(message = "Email không hợp lệ")
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)")
        String phone,

        LocalDate dateOfBirth,

        Gender gender,

        @Size(max = 1000)
        String address,

        StudentStatus status,

        String note
) {
}
