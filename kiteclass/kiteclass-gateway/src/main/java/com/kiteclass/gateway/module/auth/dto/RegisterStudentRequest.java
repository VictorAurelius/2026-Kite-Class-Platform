package com.kiteclass.gateway.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for student registration endpoint.
 *
 * <p>Endpoint: POST /api/v1/auth/register/student
 *
 * <p>This DTO contains both authentication credentials (email/password) and
 * student profile data. The registration flow will:
 * <ol>
 *   <li>Create User record in Gateway with hashed password</li>
 *   <li>Call Core service to create Student profile</li>
 *   <li>Link User.referenceId to Student.id</li>
 *   <li>Generate JWT tokens and return to client</li>
 * </ol>
 *
 * @param email       Student's email (used for authentication)
 * @param password    Student's password (will be hashed)
 * @param name        Student's full name
 * @param phone       Student's phone number (Vietnamese format)
 * @param dateOfBirth Student's date of birth
 * @param gender      Student's gender (MALE, FEMALE, OTHER)
 * @param address     Student's address
 * @author KiteClass Team
 * @since 1.8.0
 */
public record RegisterStudentRequest(
        @NotBlank(message = "Email là bắt buộc")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu là bắt buộc")
        @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8-100 ký tự")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
                message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt"
        )
        String password,

        @NotBlank(message = "Tên là bắt buộc")
        @Size(min = 2, max = 100, message = "Tên phải từ 2-100 ký tự")
        String name,

        @Pattern(
                regexp = "^0\\d{9}$",
                message = "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)"
        )
        String phone,

        LocalDate dateOfBirth,

        @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
        String gender,

        @Size(max = 1000, message = "Địa chỉ tối đa 1000 ký tự")
        String address
) {
}
