package com.kiteclass.gateway.module.auth.repository;

import com.kiteclass.gateway.module.auth.entity.PasswordResetToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository for password reset token operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Repository
public interface PasswordResetTokenRepository extends R2dbcRepository<PasswordResetToken, Long> {

    /**
     * Find password reset token by token string.
     *
     * @param token token string
     * @return Mono of PasswordResetToken
     */
    Mono<PasswordResetToken> findByToken(String token);

    /**
     * Find all password reset tokens for a user.
     *
     * @param userId user ID
     * @return Flux of PasswordResetTokens
     */
    Flux<PasswordResetToken> findByUserId(Long userId);

    /**
     * Delete all password reset tokens for a user.
     *
     * @param userId user ID
     * @return Mono of Void
     */
    Mono<Void> deleteByUserId(Long userId);

    /**
     * Delete expired password reset tokens.
     *
     * @param now current timestamp
     * @return Mono of Void
     */
    @Query("DELETE FROM password_reset_tokens WHERE expires_at < :now")
    Mono<Void> deleteExpiredTokens(Instant now);

    /**
     * Delete used password reset tokens.
     *
     * @return Mono of Void
     */
    @Query("DELETE FROM password_reset_tokens WHERE used_at IS NOT NULL")
    Mono<Void> deleteUsedTokens();
}
