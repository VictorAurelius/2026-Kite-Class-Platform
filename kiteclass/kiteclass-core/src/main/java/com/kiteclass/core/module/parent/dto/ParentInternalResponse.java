package com.kiteclass.core.module.parent.dto;

import java.util.List;

/**
 * Internal (gateway-only) projection of a parent profile.
 *
 * <p>Returned by {@code GET /internal/parents/{id}} so that the Gateway can
 * populate the {@code linked_student_ids} JWT claim and expose the parent's
 * display profile in the login response payload.
 *
 * @param id               parent id
 * @param email            login email
 * @param fullName         display name
 * @param phoneNumber      phone (nullable)
 * @param relationship     FATHER / MOTHER / GUARDIAN
 * @param status           PENDING / ACTIVE / INACTIVE
 * @param linkedStudentIds ids of this parent's children in the current tenant
 * @since 2.14.0
 */
public record ParentInternalResponse(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String relationship,
        String status,
        List<Long> linkedStudentIds
) {
}
