package com.kiteclass.gateway.module.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new user.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255, message = "{validation.max_length}")
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(min = 8, max = 128, message = "Mật khẩu phải có từ 8-128 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường, và 1 chữ số"
    )
    private String password;

    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.max_length}")
    private String name;

    @Pattern(
        regexp = "^0\\d{9}$",
        message = "{validation.phone.invalid}"
    )
    private String phone;

    @Size(max = 500, message = "{validation.max_length}")
    private String avatarUrl;

    private List<Long> roleIds;
}
