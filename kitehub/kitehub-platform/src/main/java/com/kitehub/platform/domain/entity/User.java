package com.kitehub.platform.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity for KiteHub platform authentication.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    /**
     * Account lockout state (GAP-515 / OWASP A07).
     * Reset to 0 on successful login. Incremented per failed login.
     * When >= 5 within window, sets {@link #lockedUntil} with exponential backoff.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;

    /** Account is locked until this timestamp (UTC). NULL when not locked. */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /**
     * Number of times this account has hit the lockout threshold.
     * Used for exponential backoff: 1st = 15min, 2nd = 1hr, 3rd+ = 24hr.
     */
    @Column(name = "lockout_count", nullable = false)
    @Builder.Default
    private int lockoutCount = 0;

    /**
     * 2FA state (GAP-516 / OWASP A07 §2.4).
     * AES-encrypted base32 TOTP secret; NULL means user has not enrolled yet.
     * Encryption key: {@code kitehub.auth.totp.encryption-key} (Phase 1.5+ KMS).
     */
    @Column(name = "totp_secret_encrypted")
    private String totpSecretEncrypted;

    /** Timestamp of successful enroll-confirm. NULL = not enrolled. */
    @Column(name = "totp_enrolled_at")
    private LocalDateTime totpEnrolledAt;

    /**
     * When TRUE, the user MUST enroll 2FA before access tokens are issued.
     * Set TRUE for PLATFORM_ADMIN by the V37 migration.
     */
    @Column(name = "totp_required", nullable = false)
    @Builder.Default
    private boolean totpRequired = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
