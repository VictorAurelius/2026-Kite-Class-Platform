package com.kiteclass.core.module.student.dto;

import com.kiteclass.core.common.constant.Gender;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Request DTO for creating a new student.
 *
 * <p>Contains validation annotations to ensure data integrity:
 * <ul>
 *   <li>Name: required, 2-100 characters</li>
 *   <li>Email: valid email format, max 255 characters</li>
 *   <li>Phone: Vietnamese format (10 digits starting with 0)</li>
 *   <li>Address: max 1000 characters</li>
 * </ul>
 *
 * @param name        Student's full name (required)
 * @param email       Student's email address
 * @param phone       Student's phone number
 * @param dateOfBirth Student's date of birth
 * @param gender      Student's gender
 * @param address     Student's address
 * @param note        Additional notes
 * @author KiteClass Team
 * @since 2.3.0
 */
public record CreateStudentRequest(
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

        String note
) {
}
