package com.kiteclass.gateway.module.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Password reset token entity for forgot password flow.
 *
 * <p>Stores password reset tokens in database for validation and one-time use.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Table("password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    /**
     * Primary key.
     */
    @Id
    private Long id;

    /**
     * Unique password reset token string (UUID).
     */
    @Column("token")
    private String token;

    /**
     * User ID who requested password reset.
     */
    @Column("user_id")
    private Long userId;

    /**
     * Token expiration timestamp (typically 1 hour from creation).
     */
    @Column("expires_at")
    private Instant expiresAt;

    /**
     * Token creation timestamp.
     */
    @Column("created_at")
    private Instant createdAt;

    /**
     * Timestamp when token was used to reset password (null if not used yet).
     * Prevents token reuse.
     */
    @Column("used_at")
    private Instant usedAt;

    /**
     * Check if token is expired.
     *
     * @return true if token is expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Check if token has been used.
     *
     * @return true if token has been used
     */
    public boolean isUsed() {
        return usedAt != null;
    }
}
