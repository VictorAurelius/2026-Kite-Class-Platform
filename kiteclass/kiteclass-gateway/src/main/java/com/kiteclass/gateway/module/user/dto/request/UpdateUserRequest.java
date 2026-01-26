package com.kiteclass.gateway.module.user.dto.request;

import com.kiteclass.gateway.common.constant.UserStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing user.
 *
 * <p>All fields are optional for partial updates.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(max = 255, message = "{validation.max_length}")
    private String name;

    @Pattern(
        regexp = "^0\\d{9}$",
        message = "{validation.phone.invalid}"
    )
    private String phone;

    @Size(max = 500, message = "{validation.max_length}")
    private String avatarUrl;

    private UserStatus status;

    private List<Long> roleIds;
}
