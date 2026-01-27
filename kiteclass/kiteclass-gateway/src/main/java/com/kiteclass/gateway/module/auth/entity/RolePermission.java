package com.kiteclass.gateway.module.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Role-Permission join entity.
 *
 * <p>Maps which permissions belong to each role.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Table("role_permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    /**
     * Role ID.
     */
    @Column("role_id")
    private Long roleId;

    /**
     * Permission ID.
     */
    @Column("permission_id")
    private Long permissionId;

    /**
     * Assignment timestamp.
     */
    @Column("created_at")
    private Instant createdAt;
}
