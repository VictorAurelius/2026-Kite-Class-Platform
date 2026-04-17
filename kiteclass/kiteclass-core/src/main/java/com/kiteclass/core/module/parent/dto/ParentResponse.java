package com.kiteclass.core.module.parent.dto;

/**
 * Self-profile response for the parent dashboard.
 *
 * @param id           parent id
 * @param fullName     display name
 * @param email        login email
 * @param phoneNumber  Vietnamese phone (may be null)
 * @param relationship FATHER / MOTHER / GUARDIAN
 * @param status       account lifecycle status
 * @since 2.14.0
 */
public record ParentResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        String relationship,
        String status
) {
}
