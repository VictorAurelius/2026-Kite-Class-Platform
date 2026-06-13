package com.kiteclass.core.module.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to assign a user to one of the 5 seeded system role templates
 * (GAP-1119 Bucket D).
 *
 * <p>Assignment is by template NAME (not roleId) so the FE owner-shell doesn't need
 * to resolve seeded role ids; the server seeds-or-resolves the template lazily.
 *
 * @param userId   the user's numeric reference id to assign
 * @param roleName one of OWNER/STAFF/TEACHER/PARENT/STUDENT
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */
public record AssignRoleRequest(
        @NotNull(message = "userId là bắt buộc")
        Long userId,

        @NotBlank(message = "roleName là bắt buộc")
        String roleName
) {
}
