package com.kiteclass.gateway.module.auth.repository;

import com.kiteclass.gateway.module.auth.entity.RefreshToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository for refresh token operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Repository
public interface RefreshTokenRepository extends R2dbcRepository<RefreshToken, Long> {

    /**
     * Find refresh token by token string.
     *
     * @param token token string
     * @return Mono of RefreshToken
     */
    Mono<RefreshToken> findByToken(String token);

    /**
     * Find all refresh tokens for a user.
     *
     * @param userId user ID
     * @return Flux of RefreshTokens
     */
    Flux<RefreshToken> findByUserId(Long userId);

    /**
     * Delete all refresh tokens for a user.
     *
     * @param userId user ID
     * @return Mono of Void
     */
    Mono<Void> deleteByUserId(Long userId);

    /**
     * Delete expired refresh tokens.
     *
     * @param now current timestamp
     * @return Mono of Void
     */
    @Query("DELETE FROM refresh_tokens WHERE expires_at < :now")
    Mono<Void> deleteExpiredTokens(Instant now);
}
