package com.kiteclass.gateway.module.auth.repository;

import com.kiteclass.gateway.module.auth.entity.RolePermission;
import com.kiteclass.gateway.module.user.entity.Permission;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for role-permission operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Repository
public interface RolePermissionRepository extends R2dbcRepository<RolePermission, Long> {

    /**
     * Find all permissions for a role.
     *
     * @param roleId role ID
     * @return Flux of Permissions
     */
    @Query("""
        SELECT p.* FROM permissions p
        INNER JOIN role_permissions rp ON p.id = rp.permission_id
        WHERE rp.role_id = :roleId
        """)
    Flux<Permission> findPermissionsByRoleId(Long roleId);

    /**
     * Find all permissions for a user (via their roles).
     *
     * @param userId user ID
     * @return Flux of Permissions
     */
    @Query("""
        SELECT DISTINCT p.* FROM permissions p
        INNER JOIN role_permissions rp ON p.id = rp.permission_id
        INNER JOIN user_roles ur ON rp.role_id = ur.role_id
        WHERE ur.user_id = :userId
        """)
    Flux<Permission> findPermissionsByUserId(Long userId);
}
