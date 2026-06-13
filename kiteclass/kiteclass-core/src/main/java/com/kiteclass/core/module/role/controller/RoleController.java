package com.kiteclass.core.module.role.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.role.constant.SystemRoleTemplate;
import com.kiteclass.core.module.role.dto.AssignRoleRequest;
import com.kiteclass.core.module.role.dto.RoleTemplateResponse;
import com.kiteclass.core.module.role.dto.UserRoleAssignmentResponse;
import com.kiteclass.core.module.role.entity.Role;
import com.kiteclass.core.module.role.service.RoleSeederService;
import com.kiteclass.core.module.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for owner-shell RBAC management (GAP-1119 Bucket D).
 *
 * <p>Fixed-curated RBAC for Phase 1 BETA: an owner lists tenant users + their roles,
 * seeds the 5 system role templates, and assigns/revokes users to those templates.
 * There is NO permission-edit endpoint by design (deferred Phase 3).
 *
 * <p>Owner-authorized: class-level {@code @PreAuthorize} restricts every endpoint to
 * {@code OWNER}/{@code ADMIN}/{@code PLATFORM_ADMIN} gateway authorities (canonical
 * role literals — see PR audit). Tenant scope is enforced by the gateway
 * {@code X-Tenant-Id} header + Hibernate {@code tenantFilter}.
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN','PLATFORM_ADMIN')")
@Tag(name = "Role Management", description = "Owner-shell RBAC: role templates + user-role assignments")
public class RoleController {

    private final RoleService roleService;
    private final RoleSeederService roleSeederService;

    /**
     * List the 5 system role templates with their seed-state in this tenant.
     *
     * @return template list (OWNER/STAFF/TEACHER/PARENT/STUDENT)
     */
    @GetMapping("/templates")
    @Operation(summary = "List system role templates",
            description = "Returns the 5 fixed-curated role templates + whether each is seeded in this tenant.")
    public ApiResponse<List<RoleTemplateResponse>> getTemplates() {
        log.debug("GET /api/v1/roles/templates");
        return ApiResponse.success(roleSeederService.getTemplates());
    }

    /**
     * Seed (idempotent) the 5 system role templates for this tenant.
     *
     * @return template list after seeding
     */
    @PostMapping("/seed")
    @Operation(summary = "Seed system role templates",
            description = "Idempotently creates the 5 system role templates for the current tenant.")
    public ApiResponse<List<RoleTemplateResponse>> seedTemplates() {
        log.info("POST /api/v1/roles/seed");
        roleSeederService.seedSystemRoles();
        return ApiResponse.success(roleSeederService.getTemplates(), "System role templates seeded");
    }

    /**
     * List tenant users with their currently-assigned role names.
     *
     * @return per-user role assignment summary
     */
    @GetMapping("/assignments")
    @Operation(summary = "List tenant user-role assignments",
            description = "Returns each tenant user (by reference id) with the role names assigned to them.")
    public ApiResponse<List<UserRoleAssignmentResponse>> listAssignments() {
        log.debug("GET /api/v1/roles/assignments");
        return ApiResponse.success(roleService.listAssignments());
    }

    /**
     * Assign a user to one of the 5 system role templates (idempotent).
     *
     * <p>The template is seeded-or-resolved lazily, so the caller need not pre-seed.
     *
     * @param request {userId, roleName}
     * @return the user's assignment summary after the change
     */
    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign user to a role template",
            description = "Assigns a user to one of OWNER/STAFF/TEACHER/PARENT/STUDENT. Idempotent.")
    public ApiResponse<UserRoleAssignmentResponse> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        log.info("POST /api/v1/roles/assignments - userId: {}, role: {}", request.userId(), request.roleName());

        SystemRoleTemplate template = SystemRoleTemplate.fromName(request.roleName())
                .orElseThrow(() -> new ValidationException("INVALID_ROLE_NAME", request.roleName()));

        Role role = roleSeederService.getOrCreateSystemRole(template);
        roleService.assignRoleToUser(request.userId(), role.getId());

        return ApiResponse.success(
                roleService.getAssignmentForUser(request.userId()), "Role assigned successfully");
    }

    /**
     * Revoke a system-role-template assignment from a user (idempotent).
     *
     * @param userId   the user's numeric reference id
     * @param roleName the role template name to revoke
     * @return 204 No Content
     */
    @DeleteMapping("/assignments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a role template from a user",
            description = "Removes a user's assignment to one of the 5 role templates. Idempotent.")
    public ApiResponse<Void> revokeRole(
            @Parameter(description = "User reference id", required = true) @RequestParam Long userId,
            @Parameter(description = "Role template name", required = true) @RequestParam String roleName) {
        log.info("DELETE /api/v1/roles/assignments - userId: {}, role: {}", userId, roleName);

        // Validate it is one of the 5 templates (no-op revoke for unknown names is misleading).
        SystemRoleTemplate.fromName(roleName)
                .orElseThrow(() -> new ValidationException("INVALID_ROLE_NAME", roleName));

        roleService.revokeTemplateRoleByName(userId, roleName);
        return ApiResponse.success(null);
    }
}
