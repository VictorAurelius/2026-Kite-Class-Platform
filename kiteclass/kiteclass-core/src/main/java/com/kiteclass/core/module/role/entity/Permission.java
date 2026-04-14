package com.kiteclass.core.module.role.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Permission — granular permission token assignable to roles.
 *
 * <p>Pre-defined system permissions seeded at startup. Examples:
 * <ul>
 *   <li>STUDENT_VIEW_ALL, STUDENT_EDIT_OWN, STUDENT_DELETE</li>
 *   <li>GRADE_EDIT_OWN, GRADE_EDIT_ALL, GRADE_FINALIZE</li>
 *   <li>PAYROLL_APPROVE, PAYROLL_VIEW</li>
 *   <li>USER_MANAGE, ROLE_ASSIGN</li>
 * </ul>
 *
 * <p>BR-PERM-001: Name globally unique (per instance).
 *
 * @since 3.15.0 (GAP-058, ADR-003)
 */
@Entity
@Table(
        name = "permissions",
        indexes = {
                @Index(name = "idx_permission_name", columnList = "instance_id,name", unique = true),
                @Index(name = "idx_permission_category", columnList = "category"),
                @Index(name = "idx_permission_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    /**
     * Grouping: STUDENT, TEACHER, GRADE, PAYROLL, BRANDING, USER, ROLE, ...
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * System permissions cannot be deleted by tenant.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;
}
