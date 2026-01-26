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
 * Refresh token entity for JWT token renewal.
 *
 * <p>Stores refresh tokens in database for validation and invalidation.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Table("refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    /**
     * Primary key.
     */
    @Id
    private Long id;

    /**
     * JWT refresh token string.
     */
    @Column("token")
    private String token;

    /**
     * User ID who owns this token.
     */
    @Column("user_id")
    private Long userId;

    /**
     * Token expiration timestamp.
     */
    @Column("expires_at")
    private Instant expiresAt;

    /**
     * Token creation timestamp.
     */
    @Column("created_at")
    private Instant createdAt;
}
