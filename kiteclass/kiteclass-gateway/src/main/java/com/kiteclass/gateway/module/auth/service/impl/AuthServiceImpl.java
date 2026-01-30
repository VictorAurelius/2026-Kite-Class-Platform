package com.kiteclass.gateway.module.auth.service.impl;

import com.kiteclass.gateway.common.constant.MessageCodes;
import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.common.constant.UserType;
import com.kiteclass.gateway.common.exception.BusinessException;
import com.kiteclass.gateway.config.EmailProperties;
import com.kiteclass.gateway.module.auth.dto.ForgotPasswordRequest;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.LoginResponse;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.auth.dto.RegisterStudentRequest;
import com.kiteclass.gateway.module.auth.dto.RegisterResponse;
import com.kiteclass.gateway.module.auth.dto.ResetPasswordRequest;
import com.kiteclass.gateway.module.auth.entity.PasswordResetToken;
import com.kiteclass.gateway.module.auth.entity.RefreshToken;
import com.kiteclass.gateway.module.auth.repository.PasswordResetTokenRepository;
import com.kiteclass.gateway.module.auth.repository.RefreshTokenRepository;
import com.kiteclass.gateway.module.auth.service.AuthService;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import com.kiteclass.gateway.module.user.repository.UserRoleRepository;
import com.kiteclass.gateway.security.jwt.JwtProperties;
import com.kiteclass.gateway.security.jwt.JwtTokenProvider;
import com.kiteclass.gateway.service.CoreServiceClient;
import com.kiteclass.gateway.service.EmailService;
import com.kiteclass.gateway.service.ProfileFetcher;
import com.kiteclass.gateway.service.dto.CreateStudentInternalRequest;
import com.kiteclass.gateway.service.dto.StudentProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

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
 *   <li>Cross-service profile fetching from Core (since 1.8.0)</li>
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final EmailProperties emailProperties;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ProfileFetcher profileFetcher;
    private final CoreServiceClient coreServiceClient;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;
    private static final String INTERNAL_HEADER = "true";

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
    @Transactional
    public Mono<Void> forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.email());

        return userRepository.findByEmailAndDeletedFalse(request.email())
                .flatMap(user -> {
                    // Check if account is active
                    if (user.getStatus() != UserStatus.ACTIVE) {
                        log.warn("Password reset requested for inactive account: {}", request.email());
                        // Don't reveal account status for security reasons, just return success
                        return Mono.empty();
                    }

                    // Generate unique reset token
                    String resetToken = UUID.randomUUID().toString();

                    // Create token entity
                    PasswordResetToken tokenEntity = PasswordResetToken.builder()
                            .token(resetToken)
                            .userId(user.getId())
                            .expiresAt(Instant.now().plusMillis(emailProperties.getResetTokenExpiration()))
                            .createdAt(Instant.now())
                            .build();

                    // Delete any existing reset tokens for this user
                    return passwordResetTokenRepository.deleteByUserId(user.getId())
                            .then(passwordResetTokenRepository.save(tokenEntity))
                            .flatMap(savedToken -> {
                                // Send password reset email
                                return emailService.sendPasswordResetEmail(
                                        user.getEmail(),
                                        user.getName(),
                                        resetToken
                                );
                            })
                            .doOnSuccess(v -> log.info("Password reset email sent to: {}", request.email()))
                            .onErrorResume(e -> {
                                log.error("Failed to send password reset email to: {}", request.email(), e);
                                // Don't fail the request if email sending fails
                                // The token is still valid in database
                                return Mono.empty();
                            });
                })
                // Always return success (even if email not found) for security
                .then()
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Password reset requested for non-existent email: {}", request.email());
                    return Mono.empty();
                }));
    }

    @Override
    @Transactional
    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request with token");

        return passwordResetTokenRepository.findByToken(request.token())
                .switchIfEmpty(Mono.error(new BusinessException(
                        MessageCodes.PASSWORD_RESET_TOKEN_INVALID,
                        HttpStatus.BAD_REQUEST
                )))
                .flatMap(token -> {
                    // Check if token is expired
                    if (token.isExpired()) {
                        return passwordResetTokenRepository.delete(token)
                                .then(Mono.error(new BusinessException(
                                        MessageCodes.PASSWORD_RESET_TOKEN_EXPIRED,
                                        HttpStatus.BAD_REQUEST
                                )));
                    }

                    // Check if token has been used
                    if (token.isUsed()) {
                        return Mono.error(new BusinessException(
                                MessageCodes.PASSWORD_RESET_TOKEN_USED,
                                HttpStatus.BAD_REQUEST
                        ));
                    }

                    // Get user and update password
                    return userRepository.findById(token.getUserId())
                            .filter(user -> Boolean.FALSE.equals(user.getDeleted()))
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    MessageCodes.USER_NOT_FOUND,
                                    HttpStatus.NOT_FOUND
                            )))
                            .flatMap(user -> {
                                // Update password
                                user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

                                // Reset failed login attempts if any
                                user.setFailedLoginAttempts(0);
                                user.setLockedUntil(null);

                                // Save user
                                return userRepository.save(user)
                                        .then(Mono.defer(() -> {
                                            // Mark token as used
                                            token.setUsedAt(Instant.now());
                                            return passwordResetTokenRepository.save(token);
                                        }))
                                        .then(Mono.defer(() -> {
                                            // Delete all refresh tokens for security
                                            log.info("Invalidating all refresh tokens for user: {}", user.getId());
                                            return refreshTokenRepository.deleteByUserId(user.getId());
                                        }))
                                        .then();
                            });
                })
                .doOnSuccess(v -> log.info("Password reset successful"))
                .doOnError(e -> log.warn("Password reset failed: {}", e.getMessage()));
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
     * <p>Additionally fetches user profile from Core service based on userType:
     * <ul>
     *   <li>STUDENT → StudentProfileResponse</li>
     *   <li>TEACHER → TeacherProfileResponse (placeholder)</li>
     *   <li>PARENT → ParentProfileResponse (placeholder)</li>
     *   <li>ADMIN/STAFF → null (no Core profile)</li>
     * </ul>
     *
     * @param user user entity
     * @return Mono of login response with tokens and profile
     * @since 1.8.0 (profile fetching added)
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

                    // Fetch user profile from Core service (if applicable)
                    Object profile = profileFetcher.fetchProfile(user.getUserType(), user.getReferenceId());

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
                                            .profile(profile)
                                            .build())
                                    .build());
                });
    }

    @Override
    @Transactional
    public Mono<RegisterResponse> registerStudent(RegisterStudentRequest request) {
        log.info("Student registration request for email: {}", request.email());

        // Step 1: Check if email already exists
        return userRepository.existsByEmailAndDeletedFalse(request.email())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Registration failed: Email already exists: {}", request.email());
                        return Mono.error(new BusinessException(
                                MessageCodes.USER_EMAIL_EXISTS,
                                HttpStatus.CONFLICT
                        ));
                    }

                    // Step 2: Create User in Gateway (without referenceId yet)
                    User user = User.builder()
                            .email(request.email())
                            .passwordHash(passwordEncoder.encode(request.password()))
                            .name(request.name())
                            .phone(request.phone())
                            .userType(UserType.STUDENT)
                            .status(UserStatus.ACTIVE)
                            .emailVerified(false)
                            .failedLoginAttempts(0)
                            .deleted(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    log.debug("Creating User in Gateway for email: {}", request.email());

                    return userRepository.save(user)
                            .flatMap(savedUser -> {
                                log.info("User created in Gateway: id={}, email={}",
                                        savedUser.getId(), savedUser.getEmail());

                                // Step 3: Create Student in Core service
                                CreateStudentInternalRequest coreRequest = new CreateStudentInternalRequest(
                                        request.name(),
                                        request.email(),
                                        request.phone(),
                                        request.dateOfBirth(),
                                        request.gender(),
                                        request.address(),
                                        null // note
                                );

                                log.debug("Calling Core service to create Student for email: {}",
                                        request.email());

                                return coreServiceClient.createStudent(coreRequest, INTERNAL_HEADER)
                                        .flatMap(response -> {
                                            StudentProfileResponse profile = response.getData();
                                            log.info("Student created in Core: id={}, email={}",
                                                    profile.id(), profile.email());

                                            // Step 4: Update User with referenceId
                                            savedUser.setReferenceId(profile.id());
                                            savedUser.setUpdatedAt(Instant.now());

                                            return userRepository.save(savedUser)
                                                    .flatMap(linkedUser -> {
                                                        log.info("User linked to Student: userId={}, " +
                                                                        "studentId={}",
                                                                linkedUser.getId(),
                                                                linkedUser.getReferenceId());

                                                        // Step 5: Generate JWT tokens
                                                        return generateRegistrationTokens(linkedUser);
                                                    });
                                        })
                                        .onErrorResume(error -> {
                                            // Saga rollback: Delete User if Student creation failed
                                            log.error("Failed to create Student in Core, rolling back " +
                                                            "User: userId={}, email={}",
                                                    savedUser.getId(), savedUser.getEmail(), error);

                                            return userRepository.deleteById(savedUser.getId())
                                                    .then(Mono.error(new BusinessException(
                                                            MessageCodes.AUTH_REGISTRATION_FAILED,
                                                            HttpStatus.INTERNAL_SERVER_ERROR
                                                    )));
                                        });
                            });
                })
                .doOnSuccess(response ->
                        log.info("Student registration successful: userId={}, studentId={}",
                                response.user().id(), response.user().referenceId()))
                .doOnError(e ->
                        log.error("Student registration failed for email: {}", request.email(), e));
    }

    /**
     * Generate access and refresh tokens for newly registered student.
     *
     * @param user the registered user
     * @return Mono of RegisterResponse with tokens
     */
    private Mono<RegisterResponse> generateRegistrationTokens(User user) {
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
                            .expiresAt(Instant.now().plusMillis(
                                    jwtProperties.getRefreshTokenExpiration()))
                            .createdAt(Instant.now())
                            .build();

                    return refreshTokenRepository.save(tokenEntity)
                            .map(saved -> RegisterResponse.builder()
                                    .accessToken(accessToken)
                                    .refreshToken(refreshToken)
                                    .tokenType("Bearer")
                                    .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                                    .user(RegisterResponse.UserInfo.builder()
                                            .id(user.getId())
                                            .email(user.getEmail())
                                            .name(user.getName())
                                            .userType(user.getUserType().name())
                                            .referenceId(user.getReferenceId())
                                            .build())
                                    .build());
                });
    }
}
