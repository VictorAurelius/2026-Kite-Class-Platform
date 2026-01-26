package com.kiteclass.gateway.module.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * UserRole join entity for many-to-many relationship between users and roles.
 *
 * <p>Tracks role assignments with metadata about when and by whom
 * the role was assigned.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_roles")
public class UserRole {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("role_id")
    private Long roleId;

    @Column("assigned_at")
    private Instant assignedAt;

    @Column("assigned_by")
    private Long assignedBy;
}
