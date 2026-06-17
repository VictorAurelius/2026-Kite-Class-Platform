package com.kiteclass.core.module.student.dto;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.module.auth.AuthPasswordPolicy;
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
 *   <li>Initial password (optional): when present, auto-provisions a KC-native
 *       login credential for the student in the same create call (Wave flow-kc3,
 *       GAP-1277). Requires the student to have an email (login is email-keyed).
 *       Validated against {@link AuthPasswordPolicy} only when non-null.</li>
 * </ul>
 *
 * @param name        Student's full name (required)
 * @param email       Student's email address
 * @param phone       Student's phone number
 * @param dateOfBirth Student's date of birth
 * @param gender      Student's gender
 * @param address     Student's address
 * @param note        Additional notes
 * @param initialPassword Optional initial login password — when present, provisions
 *        a KC-native login credential at create time (opt-in). Null = no credential.
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

        @PastOrPresent(message = "Ngày sinh không thể là ngày trong tương lai")
        LocalDate dateOfBirth,

        Gender gender,

        @Size(max = 1000)
        String address,

        String note,

        @Size(min = AuthPasswordPolicy.MIN_LENGTH, max = AuthPasswordPolicy.MAX_LENGTH,
                message = "Mật khẩu phải từ 8-100 ký tự")
        @Pattern(regexp = AuthPasswordPolicy.PATTERN, message = AuthPasswordPolicy.MESSAGE)
        String initialPassword
) {
    /**
     * Backward-compatible convenience constructor (pre-{@code initialPassword} arity).
     * Delegates to the canonical constructor with {@code initialPassword = null} so
     * existing call sites (RowValidator bulk-import, seeders, tests) compile unchanged
     * and create a student WITHOUT auto-provisioning a login. Jackson binds request
     * bodies via the canonical (all-args) record constructor, so {@code initialPassword}
     * still arrives from JSON.
     */
    public CreateStudentRequest(String name, String email, String phone, LocalDate dateOfBirth,
                                Gender gender, String address, String note) {
        this(name, email, phone, dateOfBirth, gender, address, note, null);
    }
}
