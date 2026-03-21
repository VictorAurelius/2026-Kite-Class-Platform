package com.kitehub.subscription.dto;

import lombok.Data;

/**
 * Request DTO for token refresh.
 *
 * @since 1.0.0
 */
@Data
public class RefreshRequest {
    private String refreshToken;
}
