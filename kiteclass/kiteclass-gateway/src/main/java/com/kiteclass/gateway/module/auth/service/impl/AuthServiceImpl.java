package com.kiteclass.gateway.module.auth.service.impl;

import com.kiteclass.gateway.common.constant.MessageCodes;
import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.common.exception.BusinessException;
import com.kiteclass.gateway.module.auth.dto.ForgotPasswordRequest;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.LoginResponse;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.auth.dto.ResetPasswordRequest;
import com.kiteclass.gateway.module.auth.entity.RefreshToken;
import com.kiteclass.gateway.module.auth.repository.RefreshTokenRepository;
import com.kiteclass.gateway.module.auth.service.AuthService;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import com.kiteclass.gateway.module.user.repository.UserRoleRepository;
import com.kiteclass.gateway.security.jwt.JwtProperties;
import com.kiteclass.gateway.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementation of authentication service.
 *
 * <p>Features:
 * <ul>
 *   <li>Email/password authentication</li>
 *   <li>JWT token generation</li>
 *   <li>Refresh token mechanism</li>
 *   <li>Failed login attempt tracking</li>
 *   <li>Account locking after max failed attempts</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    @Override
    @Transactional
    public Mono<LoginResponse> login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        return userRepository.findByEmailAndDeletedFalse(request.email())
                .switchIfEmpty(Mono.error(new BusinessException(
                        MessageCodes.AUTH_INVALID_CREDENTIALS,
                        HttpStatus.UNAUTHORIZED
                )))
                .flatMap(user -> validateUserAndPassword(user, request.password()))
                .flatMap(this::generateTokens)
                .doOnSuccess(response -> log.info("Login successful for user: {}", request.email()))
                .doOnError(e -> log.warn("Login failed for {}: {}", request.email(), e.getMessage()));
    }

    @Override
    @Transactional
    public Mono<LoginResponse> refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request");

        return refreshTokenRepository.findByToken(request.refreshToken())
                .switchIfEmpty(Mono.error(new BusinessException(
                        MessageCodes.AUTH_REFRESH_TOKEN_INVALID,
                        HttpStatus.UNAUTHORIZED
                )))
                .flatMap(token -> {
                    // Check if token is expired
                    if (token.getExpiresAt().isBefore(Instant.now())) {
                        return refreshTokenRepository.delete(token)
                                .then(Mono.error(new BusinessException(
                                        MessageCodes.AUTH_REFRESH_TOKEN_EXPIRED,
                                        HttpStatus.UNAUTHORIZED
                                )));
                    }

                    // Get user and validate status
                    return userRepository.findById(token.getUserId())
                            .filter(user -> Boolean.FALSE.equals(user.getDeleted()))
                            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    MessageCodes.AUTH_ACCOUNT_INACTIVE,
                                    HttpStatus.FORBIDDEN
                            )))
                            .flatMap(user -> {
                                // Delete old refresh token
                                return refreshTokenRepository.delete(token)
                                        .then(generateTokens(user));
                            });
                })
                .doOnSuccess(response -> log.info("Refresh token successful"))
                .doOnError(e -> log.warn("Refresh token failed: {}", e.getMessage()));
    }

    @Override
    @Transactional
    public Mono<Void> logout(String refreshToken) {
        log.info("Logout request");

        return refreshTokenRepository.findByToken(refreshToken)
                .flatMap(refreshTokenRepository::delete)
                .then()
                .doOnSuccess(v -> log.info("Logout successful"));
    }

    @Override
    public Mono<Void> forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.email());

        // TODO: Implement email sending with reset token
        // For now, just log and return success
        return userRepository.findByEmailAndDeletedFalse(request.email())
                .flatMap(user -> {
                    // Generate reset token and save to database
                    // Send email with reset link
                    log.info("Password reset email would be sent to: {}", request.email());
                    return Mono.empty();
                })
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request");

        // TODO: Validate reset token and update password
        // For now, just return success
        return Mono.empty();
    }

    /**
     * Validate user credentials and account status.
     *
     * @param user user entity
     * @param password plain text password
     * @return Mono of validated user
     */
    private Mono<User> validateUserAndPassword(User user, String password) {
        // Check if account is locked
        if (user.isLocked()) {
            long minutesRemaining = (user.getLockedUntil().toEpochMilli() - Instant.now().toEpochMilli()) / 60000;
            log.warn("Account locked for user: {} ({} minutes remaining)", user.getEmail(), minutesRemaining);
            return Mono.error(new BusinessException(
                    MessageCodes.AUTH_ACCOUNT_LOCKED,
                    HttpStatus.FORBIDDEN
            ));
        }

        // Check if account is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Account inactive for user: {} (status: {})", user.getEmail(), user.getStatus());
            return Mono.error(new BusinessException(
                    MessageCodes.AUTH_ACCOUNT_INACTIVE,
                    HttpStatus.FORBIDDEN
            ));
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return handleFailedLogin(user)
                    .then(Mono.error(new BusinessException(
                            MessageCodes.AUTH_INVALID_CREDENTIALS,
                            HttpStatus.UNAUTHORIZED
                    )));
        }

        // Reset failed attempts and update last login time on successful login
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());

        return userRepository.save(user);
    }

    /**
     * Handle failed login attempt.
     * Increment failed attempts counter and lock account if threshold exceeded.
     *
     * @param user user entity
     * @return Mono of updated user
     */
    private Mono<User> handleFailedLogin(User user) {
        int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60));
            log.warn("Account locked for user: {} after {} failed attempts", user.getEmail(), attempts);
        } else {
            log.warn("Failed login attempt {} of {} for user: {}", attempts, MAX_FAILED_ATTEMPTS, user.getEmail());
        }

        return userRepository.save(user);
    }

    /**
     * Generate access and refresh tokens for user.
     *
     * @param user user entity
     * @return Mono of login response with tokens
     */
    private Mono<LoginResponse> generateTokens(User user) {
        return userRoleRepository.findRolesByUserId(user.getId())
                .map(role -> role.getCode())
                .collectList()
                .flatMap(roles -> {
                    // Generate JWT tokens
                    String accessToken = jwtTokenProvider.generateAccessToken(
                            user.getId(),
                            user.getEmail(),
                            roles
                    );
                    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

                    // Save refresh token to database
                    RefreshToken tokenEntity = RefreshToken.builder()
                            .token(refreshToken)
                            .userId(user.getId())
                            .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
                            .createdAt(Instant.now())
                            .build();

                    return refreshTokenRepository.save(tokenEntity)
                            .map(saved -> LoginResponse.builder()
                                    .accessToken(accessToken)
                                    .refreshToken(refreshToken)
                                    .tokenType("Bearer")
                                    .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000) // seconds
                                    .user(LoginResponse.UserInfo.builder()
                                            .id(user.getId())
                                            .email(user.getEmail())
                                            .name(user.getName())
                                            .roles(roles)
                                            .build())
                                    .build());
                });
    }
}
