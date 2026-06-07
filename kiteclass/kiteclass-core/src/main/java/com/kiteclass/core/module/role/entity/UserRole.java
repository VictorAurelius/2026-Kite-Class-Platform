package com.kiteclass.core.module.role.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * UserRole — assignment of Role to User.
 *
 * <p>Many-to-many: 1 user có thể có nhiều roles (e.g., Teacher + Dept Head).
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-UR-001: Unique (userId, roleId) — 1 assignment per user-role pair</li>
 *   <li>BR-UR-002: assignedAt recorded for audit</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-058, ADR-003)
 */
@Entity
@Table(
        name = "user_roles",
        indexes = {
                @Index(name = "idx_ur_user_role", columnList = "user_id,role_id", unique = true),
                @Index(name = "idx_ur_role", columnList = "role_id"),
                @Index(name = "idx_ur_instance_id", columnList = "instance_id"),
                @Index(name = "idx_ur_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    // GAP-877: actor X-User-Id is a UUID (GAP-795); column retyped VARCHAR -> uuid in V94.
    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "notes", length = 500)
    private String notes;
}
