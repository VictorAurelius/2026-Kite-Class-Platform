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

    @Value("${jwt.secret:kitehub-super-secret-key-that-is-at-least-256-bits-long-for-hs384-algorithm}")
    private String jwtSecret;

    /**
     * Initialize demo user for testing.
     */
    @PostConstruct
    public void initDemoUser() {
        // Create demo user if not exists
        String demoEmail = "demo@kitehub.com";
        if (!USER_STORE.containsKey(demoEmail)) {
            UUID demoUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            String passwordHash = passwordEncoder.encode("Demo@123");
            USER_STORE.put(demoEmail, new UserCredentials(
                demoUserId, demoEmail, "Demo Organization", passwordHash, "OWNER"
            ));
            log.info("Demo user created: {} / Demo@123", demoEmail);
        }
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
