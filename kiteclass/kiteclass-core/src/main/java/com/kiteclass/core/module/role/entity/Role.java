package com.kiteclass.core.module.role.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Role — hierarchical RBAC role (Composite Pattern per ADR-003).
 *
 * <p>Forms tree via `parent` reference. Examples:
 * <pre>
 * TENANT_OWNER (level 1)
 *   PRINCIPAL (level 2)
 *     VICE_PRINCIPAL (level 3)
 *       DEPT_HEAD (level 4)
 *         SUBJECT_TEACHER (level 5)
 *         HOMEROOM_TEACHER (level 5)
 * </pre>
 *
 * <p>Permissions bundled per role. User có nhiều roles → union of permissions.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-ROLE-001: Name unique per tenant</li>
 *   <li>BR-ROLE-002: System roles pre-seeded + not deletable</li>
 *   <li>BR-ROLE-003: Level 1 (top) has no parent; cascades 2..10</li>
 *   <li>BR-ROLE-004: Cannot have circular parent references</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-058, ADR-003)
 */
@Entity
@Table(
        name = "roles",
        indexes = {
                @Index(name = "idx_role_name", columnList = "instance_id,name", unique = true),
                @Index(name = "idx_role_parent", columnList = "parent_id"),
                @Index(name = "idx_role_level", columnList = "level"),
                @Index(name = "idx_role_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Role parent;

    /**
     * Hierarchy level: 1=top (TENANT_OWNER), 10=lowest (STUDENT).
     * Used for sorting + display.
     */
    @Column(name = "level", nullable = false)
    @Builder.Default
    private Integer level = 5;

    /**
     * System roles pre-seeded; cannot be deleted by tenant.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Add permission (mutates the set).
     */
    public void grantPermission(Permission permission) {
        this.permissions.add(permission);
    }

    /**
     * Remove permission.
     */
    public void revokePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /**
     * Check if this role has specific permission by name.
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream().anyMatch(p -> p.getName().equals(permissionName));
    }
}
