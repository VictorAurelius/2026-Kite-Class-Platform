package com.kiteclass.gateway.service.dto;

/**
 * Parent profile response from Core service.
 *
 * <p>Simplified DTO for Gateway service to display parent profile information.
 * Fetched from Core service via Feign Client when user logs in with userType = PARENT.
 *
 * <p><b>Note:</b> Parent module not yet implemented in Core service (planned for future PR).
 * This DTO is a placeholder to support the cross-service architecture.
 *
 * @param id          Parent's unique identifier (matches User.referenceId)
 * @param name        Parent's full name
 * @param email       Parent's email address
 * @param phone       Parent's phone number
 * @param address     Parent's address
 * @param avatarUrl   URL to parent's avatar image
 * @param status      Parent's current status (ACTIVE, INACTIVE, etc.)
 * @author KiteClass Team
 * @since 1.8.0
 */
public record ParentProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String avatarUrl,
        String status
) {
}
