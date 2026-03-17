package com.kitehub.subscription.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Self-service registration response DTO.
 *
 * @since 1.0.0
 */
@Data
@Builder
public class RegisterResponse {
    private UserInfo user;
    private String accessToken;
    private String refreshToken;
    private InstanceResponse instance;

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
