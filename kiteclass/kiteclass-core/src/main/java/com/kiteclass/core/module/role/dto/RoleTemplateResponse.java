package com.kiteclass.core.module.role.dto;

/**
 * One of the 5 seeded system role templates (GAP-1119) plus its current
 * seed-state in the tenant.
 *
 * @param name        template name (OWNER/STAFF/TEACHER/PARENT/STUDENT)
 * @param level       role-hierarchy level (1-10, ADR-003)
 * @param description Vietnamese description of the role scope
 * @param roleId      seeded {@code roles.id} for this tenant, or {@code null} if not yet seeded
 * @param seeded      whether the template row exists in this tenant
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */
public record RoleTemplateResponse(
        String name,
        int level,
        String description,
        Long roleId,
        boolean seeded
) {
}
