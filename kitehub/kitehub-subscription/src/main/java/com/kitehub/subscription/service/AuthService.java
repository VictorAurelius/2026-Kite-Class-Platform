package com.kitehub.subscription.service;

import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.*;
import com.kitehub.subscription.repository.InstanceRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication service for KiteHub platform.
 * Handles user registration, login, and JWT token management.
 *
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final InstanceRepository instanceRepository;
    private final InstanceService instanceService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // In-memory user storage (for demo - should be replaced with proper user table)
    private static final Map<String, UserCredentials> USER_STORE = new ConcurrentHashMap<>();

    @Value("${jwt.secret:#{null}}")
    private String jwtSecret;

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
    }

    /**
     * Register a new instance with owner account.
     *
     * @param request registration request
     * @return registration response with tokens
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registering new instance: {}", request.getSubdomain());

        // Check if email already registered
        if (USER_STORE.containsKey(request.getOwnerEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Check subdomain availability
        if (instanceRepository.existsBySubdomainAndDeletedFalse(request.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists");
        }

        // Create user
        UUID userId = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode(request.getOwnerPassword());
        USER_STORE.put(request.getOwnerEmail(), new UserCredentials(
            userId, request.getOwnerEmail(), request.getOrganizationName(), passwordHash, "OWNER"
        ));

        // Create instance
        CreateInstanceRequest instanceRequest = new CreateInstanceRequest();
        instanceRequest.setSubdomain(request.getSubdomain());
        instanceRequest.setOrganizationName(request.getOrganizationName());
        instanceRequest.setOwnerId(userId);
        instanceRequest.setContactEmail(request.getOwnerEmail());
        instanceRequest.setTier(PricingTier.FREE);

        InstanceResponse instance = instanceService.createTrialInstance(instanceRequest);

        // Generate tokens
        String accessToken = generateAccessToken(userId, request.getOwnerEmail(), "OWNER");
        String refreshToken = generateRefreshToken(userId);

        log.info("Registered new instance: {} for user: {}", instance.getId(), userId);

        return RegisterResponse.builder()
            .user(RegisterResponse.UserInfo.builder()
                .id(userId)
                .email(request.getOwnerEmail())
                .name(request.getOrganizationName())
                .role("OWNER")
                .build())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .instance(instance)
            .build();
    }

    /**
     * Login with email and password.
     *
     * @param request login request
     * @return login response with tokens
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());

        UserCredentials user = USER_STORE.get(request.getEmail());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.passwordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Get user's instances
        List<InstanceResponse> instances = instanceService.getInstancesByOwner(user.id());

        // Generate tokens
        String accessToken = generateAccessToken(user.id(), user.email(), user.role());
        String refreshToken = generateRefreshToken(user.id());

        log.info("Login successful for user: {}", user.id());

        return LoginResponse.builder()
            .user(LoginResponse.UserInfo.builder()
                .id(user.id())
                .email(user.email())
                .name(user.name())
                .role(user.role())
                .build())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .instances(instances)
            .build();
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param refreshToken the refresh token
     * @return new access and refresh tokens
     */
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

            // Find user by ID
            UserCredentials user = USER_STORE.values().stream()
                .filter(u -> u.id().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String newAccessToken = generateAccessToken(user.id(), user.email(), user.role());
            String newRefreshToken = generateRefreshToken(user.id());

            return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
    }

    /**
     * Update user profile.
     */
    public void updateProfile(String email, String name, String phone) {
        UserCredentials user = USER_STORE.get(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        USER_STORE.put(email, new UserCredentials(user.id(), user.email(), name != null ? name : user.name(), user.passwordHash(), user.role()));
        log.info("Profile updated for: {}", email);
    }

    /**
     * Change user password.
     */
    public void changePassword(String email, String currentPassword, String newPassword) {
        UserCredentials user = USER_STORE.get(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        String newHash = passwordEncoder.encode(newPassword);
        USER_STORE.put(email, new UserCredentials(user.id(), user.email(), user.name(), newHash, user.role()));
        log.info("Password changed for: {}", email);
    }

    /**
     * Generate JWT access token.
     */
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

    /**
     * Generate JWT refresh token.
     */
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

    /**
     * Internal record for storing user credentials.
     */
    private record UserCredentials(UUID id, String email, String name, String passwordHash, String role) {}
}
