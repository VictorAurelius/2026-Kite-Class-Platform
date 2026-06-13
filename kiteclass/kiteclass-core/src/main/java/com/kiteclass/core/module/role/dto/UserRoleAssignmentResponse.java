package com.kiteclass.core.module.role.dto;

import java.util.List;

/**
 * A tenant user together with the role names currently assigned to them.
 *
 * <p>Used by the owner-shell RBAC management screen (GAP-1119 Bucket D) — "list
 * tenant users with their current role". A "user" here is identified by the numeric
 * {@code user_roles.user_id}; KiteClass has no central user table (teacher/parent/
 * student each have their own), so the roster is keyed by that reference id.
 *
 * @param userId the user's numeric reference id ({@code user_roles.user_id})
 * @param roles  the role names assigned to the user (e.g. ["TEACHER"])
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */
public record UserRoleAssignmentResponse(
        Long userId,
        List<String> roles
) {
}
