package com.kitehub.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for server-side logout (GAP-1075). Carries the refresh token to revoke
 * (add to the Redis blacklist). The access token is stateless and expires naturally.
 *
 * @since GAP-1075
 */
@Data
public class LogoutRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
