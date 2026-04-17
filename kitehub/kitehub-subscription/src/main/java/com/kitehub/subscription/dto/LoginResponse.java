package com.kitehub.subscription.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Login response DTO.
 *
 * @since 1.0.0
 */
@Data
@Builder
public class LoginResponse {
    private UserInfo user;
    private String accessToken;
    private String refreshToken;
    private List<InstanceResponse> instances;

    /**
     * User information.
     */
    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String email;
        private String name;
        private String role;
    }
}
