package com.kiteclass.core.module.parent.entity;

import com.kiteclass.core.common.constant.ParentRelationship;
import com.kiteclass.core.common.constant.ParentStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Parent / guardian account.
 *
 * <p>A Parent represents the out-of-school contact for one or more students.
 * Identity credentials (password, JWT) live in the Gateway {@code users} table
 * — this row holds only the education-side profile. The two are linked by
 * {@code users.reference_id = parents.id} and {@code users.user_type = PARENT}
 * (see {@link com.kiteclass.gateway.common.constant.UserType}).
 *
 * <p>A Parent may be linked to multiple Students and vice versa (e.g., father
 * and mother both linked to the same child). The many-to-many is expressed via
 * {@link ParentStudentLink} rather than a direct {@code @ManyToMany} to retain
 * per-link metadata (primary vs. secondary, future: access scope).
 *
 * <p>Business rules (MVP):
 * <ul>
 *   <li>BR-PARENT-001: email unique within tenant ({@code uk_parents_email_tenant}).</li>
 *   <li>BR-PARENT-002: rows default to {@link ParentStatus#PENDING} until a
 *       gateway User is created via invitation redemption, after which the
 *       service transitions to {@link ParentStatus#ACTIVE}.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.14.0 (Wave 2 — GAP-052a)
 */
@Entity
@Table(
        name = "parents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_parents_email_tenant",
                        columnNames = {"instance_id", "email"}
                )
        },
        indexes = {
                @Index(name = "idx_parents_email", columnList = "email"),
                @Index(name = "idx_parents_instance", columnList = "instance_id"),
                @Index(name = "idx_parents_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parent extends BaseEntity {

    /**
     * Email address — also the login identifier on the Gateway side. Unique
     * per tenant to allow the same real person to be a parent in multiple
     * tenants (rare but legal).
     */
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Vietnamese phone number (10 digits starting with 0). Optional — parents
     * may choose to register with email only.
     */
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)"
    )
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /** Full display name (Vietnamese ordering kept in a single string). */
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Relationship to the linked student(s). */
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 20)
    @Builder.Default
    private ParentRelationship relationship = ParentRelationship.GUARDIAN;

    /** Account lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ParentStatus status = ParentStatus.PENDING;

    /**
     * Links to this parent's children. Read-only bag — mutations go through
     * {@link com.kiteclass.core.module.parent.service.ParentInvitationService}
     * so that audit + tenant filtering are preserved.
     */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ParentStudentLink> studentLinks = new HashSet<>();
}
