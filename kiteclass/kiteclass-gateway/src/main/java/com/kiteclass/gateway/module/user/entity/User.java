package com.kiteclass.gateway.module.user.entity;

import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.common.constant.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * User entity representing user accounts in the system.
 *
 * <p>Features:
 * <ul>
 *   <li>Email-based authentication</li>
 *   <li>Password hashing (BCrypt)</li>
 *   <li>Account status management</li>
 *   <li>Failed login attempt tracking</li>
 *   <li>Soft delete support</li>
 *   <li><b>Cross-service linking:</b> userType + referenceId to Core entities</li>
 * </ul>
 *
 * <h3>Cross-Service Integration (Since 1.8.0):</h3>
 * <p>Users are linked to Core service entities via:</p>
 * <ul>
 *   <li><b>userType:</b> ADMIN, STAFF (internal), TEACHER, PARENT, STUDENT (external)</li>
 *   <li><b>referenceId:</b> Foreign key to Core entity (students.id, teachers.id, parents.id)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private Long id;

    @Column("email")
    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("name")
    private String name;

    @Column("phone")
    private String phone;

    @Column("avatar_url")
    private String avatarUrl;

    @Column("status")
    private UserStatus status;

    /**
     * Type of user account.
     * <p>Determines if user has a profile entity in Core service:
     * <ul>
     *   <li>ADMIN, STAFF: Internal staff, no Core entity (referenceId = NULL)</li>
     *   <li>TEACHER, PARENT, STUDENT: External users with Core entity (referenceId required)</li>
     * </ul>
     *
     * @since 1.8.0
     */
    @Column("user_type")
    @Builder.Default
    private UserType userType = UserType.ADMIN;

    /**
     * Foreign key reference to Core service entity.
     * <p>Links to:
     * <ul>
     *   <li>NULL for ADMIN, STAFF (no Core entity)</li>
     *   <li>students.id for STUDENT</li>
     *   <li>teachers.id for TEACHER</li>
     *   <li>parents.id for PARENT</li>
     * </ul>
     *
     * @since 1.8.0
     */
    @Column("reference_id")
    private Long referenceId;

    @Column("email_verified")
    private Boolean emailVerified;

    @Column("last_login_at")
    private Instant lastLoginAt;

    @Column("failed_login_attempts")
    private Integer failedLoginAttempts;

    @Column("locked_until")
    private Instant lockedUntil;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("deleted")
    private Boolean deleted;

    @Column("deleted_at")
    private Instant deletedAt;

    /**
     * Checks if the user account is locked.
     *
     * @return true if account is locked
     */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    /**
     * Checks if the user can login.
     *
     * @return true if user can login
     */
    public boolean canLogin() {
        return status == UserStatus.ACTIVE
            && !isLocked()
            && Boolean.TRUE.equals(deleted) == false;
    }
}
