package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.User;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.*;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Authentication service for KiteHub platform.
 * Uses PostgreSQL users table for persistent storage.
 * Supports email verification before DB provisioning.
 *
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final InstanceRepository instanceRepository;
    private final InstanceService instanceService;
    private final UserRepository userRepository;
    private final CaptchaService captchaService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret:#{null}}")
    private String jwtSecret;

    @Value("${kitehub.email-verification.enabled:true}")
    private boolean emailVerificationEnabled;

    @Value("${kitehub.email-verification.base-url:http://localhost:3001}")
    private String verificationBaseUrl;

    @PostConstruct
    public void validateConfig() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET is not configured! Set jwt.secret property or JWT_SECRET env var. " +
                "Generate with: openssl rand -base64 64");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters (256 bits)");
        }
        log.info("JWT secret configured (length: {} chars)", jwtSecret.length());
        log.info("Email verification: {}", emailVerificationEnabled ? "ENABLED" : "DISABLED");
        log.info("Users in DB: {}", userRepository.count());
    }

    /**
     * Register new user + instance.
     * If email verification enabled: User=UNVERIFIED, Instance=PENDING, no DB provisioned.
     * If disabled (local dev): User=VERIFIED, Instance=TRIAL, DB provisioned immediately.
     *
     * @param request Registration request
     * @return Registration response with tokens
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registering new instance: {}", request.getSubdomain());

        // Verify captcha token (if captcha is enabled)
        if (!captchaService.verifyCaptcha(request.getCaptchaToken())) {
            log.warn("Registration blocked: captcha verification failed for subdomain={}", request.getSubdomain());
            throw new IllegalArgumentException("Xác minh captcha thất bại. Vui lòng thử lại.");
        }

        if (userRepository.existsByEmail(request.getOwnerEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (instanceRepository.existsBySubdomainAndDeletedFalse(request.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists");
        }

        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();

        // Create user in DB
        User user = User.builder()
            .email(request.getOwnerEmail())
            .name(request.getOrganizationName())
            .passwordHash(passwordEncoder.encode(request.getOwnerPassword()))
            .role("OWNER")
            .emailVerified(!emailVerificationEnabled)
            .verificationToken(emailVerificationEnabled ? verificationToken : null)
            .tokenExpiresAt(emailVerificationEnabled ? LocalDateTime.now().plusHours(24) : null)
            .build();
        user = userRepository.save(user);

        InstanceResponse instance;
        if (emailVerificationEnabled) {
            // Create PENDING instance (no DB provisioned)
            instance = instanceService.createPendingInstance(
                request.getSubdomain(),
                request.getOrganizationName(),
                user.getId(),
                request.getOwnerEmail()
            );
            log.info("Created PENDING instance (awaiting email verification): {}", instance.getId());

            // Send verification email
            sendVerificationEmail(user.getEmail(), verificationToken);
        } else {
            // Local dev: create trial instance immediately
            CreateInstanceRequest instanceRequest = new CreateInstanceRequest();
            instanceRequest.setSubdomain(request.getSubdomain());
            instanceRequest.setOrganizationName(request.getOrganizationName());
            instanceRequest.setOwnerId(user.getId());
            instanceRequest.setContactEmail(request.getOwnerEmail());
            instanceRequest.setTier(PricingTier.FREE);
            instance = instanceService.createTrialInstance(instanceRequest);
        }

        String accessToken = generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = generateRefreshToken(user.getId());

        log.info("Registered user: {} instance: {}", user.getId(), instance.getId());

        return RegisterResponse.builder()
            .user(RegisterResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .instance(instance)
            .build();
    }

    /**
     * Verify email and activate instance (provision DB).
     *
     * @param token Verification token from email link
     * @return Login response with tokens
     */
    @Transactional
    public LoginResponse verifyEmail(String token) {
        log.info("Verifying email with token: {}...", token.substring(0, 8));

        User user = userRepository.findByVerificationToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn"));

        if (user.getTokenExpiresAt() != null && user.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token đã hết hạn. Vui lòng yêu cầu gửi lại email xác nhận.");
        }

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email đã được xác nhận trước đó");
        }

        // Mark user as verified
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiresAt(null);
        userRepository.save(user);

        // Activate PENDING instance → TRIAL + provision DB
        List<Instance> pendingInstances = instanceRepository.findByOwnerIdAndDeletedFalse(user.getId())
            .stream()
            .filter(i -> i.getStatus() == InstanceStatus.PENDING)
            .toList();

        for (Instance instance : pendingInstances) {
            instanceService.activatePendingInstance(instance.getId());
            log.info("Activated instance: {} for user: {}", instance.getId(), user.getEmail());
        }

        // Return login response
        List<InstanceResponse> instances = instanceService.getInstancesByOwner(user.getId());
        String accessToken = generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = generateRefreshToken(user.getId());

        log.info("Email verified for user: {}", user.getEmail());

        return LoginResponse.builder()
            .user(LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .instances(instances)
            .build();
    }

    /**
     * Resend verification email.
     *
     * @param email User email
     */
    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email đã được xác nhận");
        }

        // Generate new token
        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        user.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        sendVerificationEmail(email, newToken);
        log.info("Resent verification email to: {}", email);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        List<InstanceResponse> instances = instanceService.getInstancesByOwner(user.getId());

        String accessToken = generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = generateRefreshToken(user.getId());

        log.info("Login successful for user: {} (verified: {})", user.getId(), user.isEmailVerified());

        return LoginResponse.builder()
            .user(LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .instances(instances)
            .build();
    }

    public RefreshResponse refresh(String refreshToken) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();

            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                throw new IllegalArgumentException("Invalid token type");
            }

            UUID userId = UUID.fromString(claims.getSubject());

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String newAccessToken = generateAccessToken(user.getId(), user.getEmail(), user.getRole());
            String newRefreshToken = generateRefreshToken(user.getId());

            return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
    }

    @Transactional
    public void updateProfile(String email, String name, String phone) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (name != null) user.setName(name);
        if (phone != null) user.setPhone(phone);
        userRepository.save(user);
        log.info("Profile updated for: {}", email);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for: {}", email);
    }

    private void sendVerificationEmail(String email, String verificationCode) {
        String param = "token";
        String verifyUrl = verificationBaseUrl + "/verify-email?" + param + "=" + verificationCode;
        log.info("[EMAIL] Verification link for {}: {}", email, verifyUrl);
        // TODO: Call kitehub-email service REST API to send actual email
        // For now, log the URL (visible in docker logs)
    }

    private String generateAccessToken(UUID userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(24, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();
    }

    private String generateRefreshToken(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(7, ChronoUnit.DAYS)))
            .signWith(key)
            .compact();
    }
}
