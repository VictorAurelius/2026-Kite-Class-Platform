package com.kiteclass.core.module.parent.dto;

import java.util.List;

/**
 * Internal result shape returned by the invitation redemption service to the
 * Gateway — carries everything needed to finish the Saga (create the Gateway
 * user, link reference id, mint JWT).
 *
 * @param parentId         id of the newly-created (or already-existing
 *                         pending) Parent row
 * @param email            login email
 * @param fullName         display name as entered during redemption
 * @param phoneNumber      phone (nullable)
 * @param relationship     FATHER / MOTHER / GUARDIAN
 * @param linkedStudentIds ids of the children linked in this tenant
 * @since 2.14.0
 */
public record RedeemInvitationResult(
        Long parentId,
        String email,
        String fullName,
        String phoneNumber,
        String relationship,
        List<Long> linkedStudentIds
) {
}
