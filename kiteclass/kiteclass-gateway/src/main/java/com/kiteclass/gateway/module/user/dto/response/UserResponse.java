package com.kiteclass.gateway.module.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kiteclass.gateway.common.constant.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for User information.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private String avatarUrl;
    private UserStatus status;
    private Boolean emailVerified;
    private Instant lastLoginAt;
    private List<RoleResponse> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
