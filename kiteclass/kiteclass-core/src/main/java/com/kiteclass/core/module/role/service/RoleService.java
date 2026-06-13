package com.kiteclass.core.module.role.service;

import com.kiteclass.core.module.role.dto.UserRoleAssignmentResponse;
import com.kiteclass.core.module.role.entity.Permission;
import com.kiteclass.core.module.role.entity.Role;
import com.kiteclass.core.module.role.entity.UserRole;
import com.kiteclass.core.module.role.repository.PermissionRepository;
import com.kiteclass.core.module.role.repository.RoleRepository;
import com.kiteclass.core.module.role.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RoleService — manages hierarchical roles + user assignments.
 *
 * <p>Composite Pattern per ADR-003.
 *
 * @since 3.15.0 (GAP-058)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Create role với optional parent + permissions.
     */
    @Transactional
    public Role createRole(String name, String description, Integer level,
                            Long parentId, Set<Long> permissionIds) {
        if (roleRepository.existsByNameAndDeletedFalse(name)) {
            throw new IllegalArgumentException("Role with name '" + name + "' already exists");
        }
        if (level == null || level < 1 || level > 10) {
            throw new IllegalArgumentException("Role level must be between 1 and 10");
        }

        Role parent = null;
        if (parentId != null) {
            parent = roleRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent role not found"));
        }

        Set<Permission> permissions = new HashSet<>();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            permissions.addAll(permissionRepository.findAllById(permissionIds));
        }

        Role role = Role.builder()
                .name(name)
                .description(description)
                .parent(parent)
                .level(level)
                .isSystem(false)
                .permissions(permissions)
                .build();

        Role saved = roleRepository.save(role);
        log.info("Created role '{}' (level={}, parent={})", name, level, parentId);
        return saved;
    }

    /**
     * Assign role to user. Idempotent (no-op if already assigned).
     *
     * <p>Wave beta-prep-1 Bucket E — Path 5 role-grant race hardening: concurrent
     * grant calls that both pass the {@code existsByUserIdAndRoleIdAndDeletedFalse}
     * pre-check (race window before either commits) hit the partial UNIQUE index
     * {@code idx_ur_user_role ON user_roles(user_id, role_id) WHERE deleted=FALSE}.
     * 2nd save throws {@link org.springframework.dao.DataIntegrityViolationException}.
     * Without explicit catch, caller sees HTTP 500 instead of idempotent success.
     * Mitigation: catch + return the row inserted by the winner.</p>
     */
    @Transactional
    public UserRole assignRoleToUser(Long userId, Long roleId) {
        if (userRoleRepository.existsByUserIdAndRoleIdAndDeletedFalse(userId, roleId)) {
            return userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(userId, roleId).orElseThrow();
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .role(role)
                .build();

        try {
            return userRoleRepository.save(userRole);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Concurrent grant race: another transaction inserted the same (user_id, role_id)
            // row between our existsBy check and our save. Honor idempotency contract by
            // returning the row that won the race.
            return userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(userId, roleId)
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * Revoke role from user (soft delete).
     */
    @Transactional
    public void revokeRoleFromUser(Long userId, Long roleId) {
        userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(userId, roleId)
                .ifPresent(ur -> {
                    ur.setDeleted(true);
                    userRoleRepository.save(ur);
                });
    }

    /**
     * Get all roles assigned to user.
     */
    public List<Role> getUserRoles(Long userId) {
        return userRoleRepository.findByUserIdAndDeletedFalse(userId).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
    }

    /**
     * Get union of permissions for user across all their roles.
     */
    public Set<String> getUserPermissionNames(Long userId) {
        return getUserRoles(userId).stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Check if user has specific permission.
     */
    public boolean userHasPermission(Long userId, String permissionName) {
        return getUserPermissionNames(userId).contains(permissionName);
    }

    /**
     * Get all child roles of given parent.
     */
    public List<Role> getChildren(Long parentId) {
        return roleRepository.findByParentIdAndDeletedFalse(parentId);
    }

    /**
     * Grant permission to role.
     */
    @Transactional
    public Role grantPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

        role.grantPermission(permission);
        return roleRepository.save(role);
    }

    public Optional<Role> getById(Long id) {
        return roleRepository.findById(id);
    }

    // ==================== RBAC Bucket D — owner-shell role management (GAP-1119) ====================

    /**
     * List all role assignments in the current tenant grouped per user.
     *
     * <p>Tenant-scoped via the Hibernate {@code tenantFilter} on {@code user_roles}.
     * Each entry maps a numeric {@code user_id} → the role names assigned to it.
     *
     * @return per-user assignment summary (first-seen order preserved)
     */
    public List<UserRoleAssignmentResponse> listAssignments() {
        return userRoleRepository.findByDeletedFalse().stream()
                .collect(Collectors.groupingBy(
                        UserRole::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(ur -> ur.getRole().getName(), Collectors.toList())))
                .entrySet().stream()
                .map(entry -> new UserRoleAssignmentResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Get the current role-name assignments for a single user.
     *
     * @param userId the user's numeric reference id
     * @return assignment summary (empty role list if none)
     */
    public UserRoleAssignmentResponse getAssignmentForUser(Long userId) {
        List<String> roles = getUserRoles(userId).stream()
                .map(Role::getName)
                .toList();
        return new UserRoleAssignmentResponse(userId, roles);
    }

    /**
     * Revoke a role from a user by role NAME (idempotent — no-op if the role does not
     * exist in the tenant or the user is not assigned to it).
     *
     * @param userId   the user's numeric reference id
     * @param roleName the role name to revoke (e.g. "TEACHER")
     */
    @Transactional
    public void revokeTemplateRoleByName(Long userId, String roleName) {
        roleRepository.findByNameAndDeletedFalse(roleName)
                .ifPresent(role -> revokeRoleFromUser(userId, role.getId()));
    }
}
