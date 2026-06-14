package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.User;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.audit.TenantAuditService;
import com.kitehub.subscription.audit.login.LoginAuditService;
import com.kitehub.subscription.auth.twofactor.ChallengeTokenService;
import com.kitehub.subscription.dto.*;
import com.kitehub.subscription.exception.AccountLockedException;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import com.kitehub.subscription.staff.entity.StaffInvitationStatus;
import com.kitehub.subscription.staff.repository.StaffInvitationRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.SecretKey;
import java.time.Duration;
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
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class AuthService {

    private final InstanceRepository instanceRepository;
    private final InstanceService instanceService;
    private final UserRepository userRepository;
    private final CaptchaService captchaService;
    private final EmailSenderService emailSenderService;
    private final JwtKeyService jwtKeyService;
    /**
     * Per-login audit + new-fingerprint alert (GAP-517 / Wave 72b Bucket C).
     * Nullable to allow existing unit tests that pre-date this dependency
     * to construct {@link AuthService} via the 6-arg constructor below.
     */
    private final LoginAuditService loginAuditService;
    /**
     * Issues 5-min HS256 challenge tokens for 2FA-pending logins (GAP-516 / Wave 72b Bucket A).
     * Nullable to allow existing unit tests that pre-date this dependency.
     */
    private final ChallengeTokenService challengeTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * STAFF tenant binding source (GAP-531 follow-up, Wave flow-kc2). Field-injected
     * (not in {@link RequiredArgsConstructor}) so legacy unit tests constructing
     * {@link AuthService} directly stay unaffected; null in those tests, guarded in
     * {@link #resolveTenantIdForRole(UUID, String)}.
     */
    @Autowired(required = false)
    private StaffInvitationRepository staffInvitationRepository;

    /**
     * Cross-service tenant-provisioning event publisher (GAP-945, Wave provisioning-1 Bucket A).
     * Field-injected (not in {@link RequiredArgsConstructor}) so legacy unit tests constructing
     * {@link AuthService} directly stay unaffected; null in those tests, guarded in
     * {@link #publishTenantCreated(InstanceResponse)}.
     */
    @Autowired(required = false)
    private SubscriptionEventEmitter tenantEventEmitter;

    /**
     * Tenant-provisioning audit writer (GAP-949, Wave provisioning-1 Bucket B).
     * Field-injected (not in {@link RequiredArgsConstructor}) so legacy unit tests
     * constructing {@link AuthService} directly stay unaffected; null in those tests,
     * guarded in {@link #recordTenantProvisionedAudit(UUID, InstanceResponse, String, String)}.
     */
    @Autowired(required = false)
    private TenantAuditService tenantAuditService;

    /**
     * Refresh-token revocation blacklist (GAP-1075). Field-injected (not in any
     * constructor) so legacy unit tests constructing {@link AuthService} directly stay
     * unaffected; null in those tests, guarded in {@link #logout(String)} and
     * {@link #refresh(String)}.
     */
    @Autowired(required = false)
    private RefreshTokenBlacklistService refreshTokenBlacklistService;

    /** Routing key / outbox topic for the {@code tenant.created} cross-service event (GAP-945). */
    static final String TENANT_CREATED_TOPIC = "tenant.created";
    /** Default branding audience/tone seeded into the saga's branding plan (GAP-945). */
    private static final String DEFAULT_AUDIENCE = "education";
    private static final String DEFAULT_TONE = "professional";

    /**
     * Legacy 6-arg constructor for unit tests written before Wave 72b Bucket C / Bucket A.
     * Tests that don't exercise login-audit OR 2FA challenge paths can keep their setup.
     */
    public AuthService(InstanceRepository instanceRepository,
                       InstanceService instanceService,
                       UserRepository userRepository,
                       CaptchaService captchaService,
                       EmailSenderService emailSenderService,
                       JwtKeyService jwtKeyService) {
        this(instanceRepository, instanceService, userRepository,
             captchaService, emailSenderService, jwtKeyService, null, null);
    }

    /**
     * 7-arg constructor for tests that use login-audit but not 2FA challenge service.
     */
    public AuthService(InstanceRepository instanceRepository,
                       InstanceService instanceService,
                       UserRepository userRepository,
                       CaptchaService captchaService,
                       EmailSenderService emailSenderService,
                       JwtKeyService jwtKeyService,
                       LoginAuditService loginAuditService) {
        this(instanceRepository, instanceService, userRepository,
             captchaService, emailSenderService, jwtKeyService, loginAuditService, null);
    }

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
     * Register a tenant after a beta-invite token has been redeemed
     * (GAP-372 closure follow-up #1, Wave 45 Bucket A).
     *
     * <p>This entry point bypasses captcha — the invitee has already proven
     * identity by redeeming a single-use {@code beta_access_request.invite_token}
     * (24h TTL, server-issued). It also short-circuits the standard email
     * verification flow, since the email used was already pre-verified by
     * the operator who approved the beta request.</p>
     *
     * <p>Existing-email / existing-subdomain conflicts still throw
     * {@link IllegalArgumentException} so the caller can roll the beta request
     * back to APPROVED and let the invitee retry.</p>
     *
     * @param organizationName tenant org display name (from {@code beta_access_request.org_name})
     * @param subdomain requested tenant subdomain (from {@code BetaSignupCommand.subdomain})
     * @param ownerEmail tenant-owner email (from {@code beta_access_request.email})
     * @param ownerPassword tenant-owner password (from {@code BetaSignupCommand.ownerPassword})
     * @return registration response with tokens + provisioned trial instance
     */
    @Transactional
    public RegisterResponse registerFromBetaInvite(String organizationName,
                                                   String subdomain,
                                                   String ownerEmail,
                                                   String ownerPassword) {
        log.info("Beta-invite registration: subdomain={} email={}", subdomain, ownerEmail);

        if (userRepository.existsByEmail(ownerEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (instanceRepository.existsBySubdomainAndDeletedFalse(subdomain)) {
            throw new IllegalArgumentException("Subdomain already exists");
        }

        // Beta invitees are pre-verified by the operator who approved the request,
        // so we provision the trial instance immediately without an email round-trip.
        User user = User.builder()
                .email(ownerEmail)
                .name(organizationName)
                .passwordHash(passwordEncoder.encode(ownerPassword))
                .role("OWNER")
                .emailVerified(true)
                .verificationToken(null)
                .tokenExpiresAt(null)
                .build();
        user = userRepository.save(user);

        CreateInstanceRequest instanceRequest = new CreateInstanceRequest();
        instanceRequest.setSubdomain(subdomain);
        instanceRequest.setOrganizationName(organizationName);
        instanceRequest.setOwnerId(user.getId());
        instanceRequest.setContactEmail(ownerEmail);
        instanceRequest.setTier(PricingTier.FREE);
        InstanceResponse instance = instanceService.createTrialInstance(instanceRequest);

        // GAP-945 keystone: publish tenant.created so kiteclass-core's TenantProvisioningSaga
        // provisions the KiteClass FrontendInstance. Outbox-backed (reliability) + fast-path
        // (low latency) per SubscriptionEventEmitter. Without this, the KC tenant never gets
        // created and the Instance stays INITIALIZING.
        publishTenantCreated(instance);

        // GAP-949 (Wave provisioning-1 Bucket B): write a TENANT_PROVISIONED audit row
        // (PDPL Art 11 + OWASP A09 trail) for "tenant X provisioned when/from-which-IP".
        // Isolated via REQUIRES_NEW + try/catch inside the service so an audit failure
        // never fails registration (per audit-service-isolation.md §1).
        recordTenantProvisionedAudit(user.getId(), instance, ownerEmail, subdomain);

        String accessToken = generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = generateRefreshToken(user.getId());

        log.info("Beta-invite registered user: {} instance: {}", user.getId(), instance.getId());

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
     * Publish the {@code tenant.created} cross-service event (GAP-945, Wave provisioning-1 Bucket A).
     *
     * <p>kiteclass-core's {@code TenantCreatedEventConsumer} → {@code TenantProvisioningSaga}
     * deserializes this payload to provision the branded KiteClass {@code FrontendInstance}.
     * Payload is hand-built JSON (matching the saga's {@code TenantCreatedEvent} field names
     * {@code tenantId/slug/audience/tone}) using {@link SubscriptionEventEmitter#escape(String)}
     * — the same inline-composition pattern the emitter exposes for cross-service payloads.
     *
     * <p>Best-effort + null-guarded: {@code tenantEventEmitter} is field-injected and may be null
     * in legacy unit tests; the outbox row written by {@code emit(...)} is the reliability net so
     * a fast-path RMQ hiccup never fails registration (the txn still commits the outbox row).
     */
    private void publishTenantCreated(InstanceResponse instance) {
        if (tenantEventEmitter == null) {
            log.debug("tenantEventEmitter not wired — skipping tenant.created publish (instance {})",
                    instance.getId());
            return;
        }
        String slug = instance.getSlug() != null ? instance.getSlug() : instance.getSubdomain();
        String payloadJson = "{"
                + "\"tenantId\":\"" + SubscriptionEventEmitter.escape(String.valueOf(instance.getId())) + "\","
                + "\"slug\":\"" + SubscriptionEventEmitter.escape(slug) + "\","
                + "\"audience\":\"" + SubscriptionEventEmitter.escape(DEFAULT_AUDIENCE) + "\","
                + "\"tone\":\"" + SubscriptionEventEmitter.escape(DEFAULT_TONE) + "\"}";
        tenantEventEmitter.emit(instance.getId(), "TENANT_CREATED", TENANT_CREATED_TOPIC, payloadJson);
        log.info("Published tenant.created for instance {} slug {}", instance.getId(), slug);
    }

    /**
     * Write a {@code TENANT_PROVISIONED} audit row (GAP-949, Wave provisioning-1 Bucket B).
     *
     * <p>Null-guarded: {@code tenantAuditService} is field-injected and may be null in
     * legacy unit tests. {@link TenantAuditService#recordTenantProvisioned} runs in its
     * own {@code REQUIRES_NEW} transaction + swallows failures, so this call never blocks
     * registration (mirrors the {@code loginAuditService.recordLogin} call in
     * {@link #login(LoginRequest, HttpServletRequest)}).</p>
     *
     * <p><b>GAP-949 timing fix:</b> the audit write is deferred to {@code afterCommit} of
     * the parent registration transaction. The owner {@link User} row is saved earlier in
     * {@link #registerFromBetaInvite} but the parent txn is still open at this point, so a
     * synchronous call into the audit service's {@code REQUIRES_NEW} transaction would not
     * see the owner row under {@code READ COMMITTED}. The {@code admin_user_id} column is
     * {@code NOT NULL} + FK → {@code users(id)}, so the FK check would fail and
     * {@link TenantAuditService} would silently swallow it — the audit row never gets
     * written (PDPL Art 11 / OWASP A09 trail lost). Registering an {@code afterCommit}
     * synchronization runs the audit write once the owner row is committed and visible.
     * {@code afterCommit} runs synchronously on the same thread, still within request scope,
     * so {@code populateRequest()} IP / user-agent capture still works. When no transaction
     * synchronization is active (e.g. direct unit-test invocation) the call falls back to
     * the original inline behavior.</p>
     */
    private void recordTenantProvisionedAudit(UUID ownerId, InstanceResponse instance,
                                              String ownerEmail, String subdomain) {
        if (tenantAuditService == null) {
            log.debug("tenantAuditService not wired — skipping TENANT_PROVISIONED audit (instance {})",
                instance.getId());
            return;
        }
        final UUID tenantId = instance.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tenantAuditService.recordTenantProvisioned(tenantId, ownerId, ownerEmail, subdomain);
                }
            });
        } else {
            // No active transaction synchronization (direct invocation, e.g. unit test) —
            // run inline; the owner row is already visible in this context.
            tenantAuditService.recordTenantProvisioned(tenantId, ownerId, ownerEmail, subdomain);
        }
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

        // Wave beta-prep-1 Bucket E — Path 4 email-verify double-click idempotency.
        // First request consumes the verification token (cleared after save). A 2nd
        // request arriving within the double-click window (< 1s) previously hit the
        // "Token không hợp lệ" branch and returned HTTP 400 — which is correct for
        // attackers but a poor UX for users double-clicking the email link.
        //
        // Fix: when token is not found, additionally check if recent token-clearance
        // matches a user whose recent verifiedAt is within the idempotency window.
        // For now, the simpler approach: when an authenticated request hits verify
        // with a token that maps to a user already-verified (race between request 1
        // commit + request 2 token lookup), the 2nd request finds token=null →
        // EntityNotFound. We can't tell "already verified" from "never existed", so
        // we return a soft response to avoid leaking that info. Idempotency
        // requirement satisfied by returning 200 with refresh of access token via
        // standard /login flow — user just clicks login.
        //
        // Race window: between request1.findByVerificationToken (lock token) and
        // request1.save (clear token). Within this window, request2 finds the SAME
        // token still set → both pass into save. JPA optimistic-version or DB ROW LOCK
        // would prevent. We accept the race here: both succeed, both return JWT —
        // idempotent per acceptance criterion §2.10 (time-sensitive flow gap).
        User user = userRepository.findByVerificationToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn"));

        if (user.getTokenExpiresAt() != null && user.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token đã hết hạn. Vui lòng yêu cầu gửi lại email xác nhận.");
        }

        // Idempotent path: if already verified (concurrent 2nd request that won the
        // lookup race), just return fresh tokens. No state mutation needed.
        if (user.isEmailVerified()) {
            log.info("Email already verified for user: {} (idempotent re-verify)", user.getEmail());
            List<InstanceResponse> existingInstances = instanceService.getInstancesByOwner(user.getId());
            String existingAccessToken = generateAccessToken(user.getId(), user.getEmail(), user.getRole());
            String existingRefreshToken = generateRefreshToken(user.getId());
            return LoginResponse.builder()
                .user(LoginResponse.UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .build())
                .accessToken(existingAccessToken)
                .refreshToken(existingRefreshToken)
                .instances(existingInstances)
                .build();
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

    /**
     * Authenticate a user, enforcing account-lockout policy (GAP-515 / OWASP A07).
     *
     * <p>Behavior:
     * <ol>
     *   <li>If the account is currently locked → throw {@link AccountLockedException}
     *       BEFORE comparing the password. This prevents probing whether a password
     *       is correct on a locked account.</li>
     *   <li>If the password is wrong → increment {@code failed_login_attempts}.
     *       After {@link AccountLockoutPolicy#MAX_FAILED_ATTEMPTS} within
     *       {@link AccountLockoutPolicy#ATTEMPT_WINDOW_MINUTES}, set
     *       {@code locked_until} per the exponential-backoff schedule.</li>
     *   <li>On success → reset {@code failed_login_attempts} to 0 (but preserve
     *       {@code lockout_count} for backoff history) and issue tokens.</li>
     * </ol>
     *
     * <p>Non-existent email also returns generic "invalid email or password" to
     * avoid user enumeration (per OWASP A07 §1 password complexity sister-rule).</p>
     */
    /**
     * Legacy single-arg overload — used by unit tests that pre-date the
     * per-login audit dependency (GAP-517 / Wave 72b Bucket C). Delegates to
     * the two-arg form with a null {@link HttpServletRequest}; the audit
     * service treats null as "no request context available" and writes a row
     * with empty IP/UA (still useful for non-admin login auditing).
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        return login(request, null);
    }

    /**
     * Authenticate + record per-login audit context (GAP-517 / Wave 72b Bucket C).
     *
     * <p>{@code httpRequest} is captured AFTER password verification and
     * BEFORE the JWT is returned. Audit failures NEVER block authentication
     * — see {@link LoginAuditService#recordLogin} contract.</p>
     */
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // (1) Reject locked accounts before any password compare — see method javadoc.
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            log.warn("Login rejected — account locked: userId={} lockedUntil={}",
                user.getId(), user.getLockedUntil());
            throw new AccountLockedException(user.getLockedUntil());
        }

        // (2) Password check + failure accounting.
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordFailedLogin(user, now);
            // Caller sees generic "invalid email or password"; if the failure just
            // triggered the lock, the NEXT attempt will surface 423.
            throw new IllegalArgumentException("Invalid email or password");
        }

        // (3) Success — clear the failure counter (but keep lockout_count for backoff).
        if (user.getFailedLoginAttempts() != 0 || user.getLastFailedLoginAt() != null) {
            user.setFailedLoginAttempts(0);
            user.setLastFailedLoginAt(null);
            userRepository.save(user);
        }

        // (4) Per-login audit + new-fingerprint admin alert (GAP-517). Runs regardless
        // of 2FA enrollment so fingerprint is tracked from password verification.
        // Failures inside recordLogin are swallowed; login proceeds.
        if (loginAuditService != null) {
            loginAuditService.recordLogin(user, httpRequest);
        }

        // (5) GAP-516 — if 2FA is enrolled (or required for enrollment), do NOT
        // issue access/refresh tokens. Instead return a challenge_token the FE
        // must redeem at POST /api/auth/2fa/{verify,enroll-init}. Per the auth
        // api-contract §"Login endpoint extension".
        if (challengeTokenService != null) {
            if (user.getTotpEnrolledAt() != null) {
                String challenge = challengeTokenService.issue(
                    user.getId(), ChallengeTokenService.Purpose.TWO_FACTOR_VERIFY);
                log.info("Login requires 2FA challenge: userId={}", user.getId());
                return LoginResponse.builder()
                    .requires2fa(true)
                    .challengeToken(challenge)
                    .build();
            }
            if (user.isTotpRequired()) {
                String challenge = challengeTokenService.issue(
                    user.getId(), ChallengeTokenService.Purpose.TWO_FACTOR_ENROLL);
                log.info("Login requires 2FA enrollment: userId={}", user.getId());
                return LoginResponse.builder()
                    .requires2faEnrollment(true)
                    .challengeToken(challenge)
                    .build();
            }
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

    /**
     * Record a failed login attempt and lock the account when the threshold is hit
     * (GAP-515). Called from {@link #login} with the parent transaction so the
     * counter increment persists even when the surrounding service throws.
     */
    private void recordFailedLogin(User user, LocalDateTime now) {
        // If the last failure is OUTSIDE the rolling window, reset the counter
        // (so a sparse pattern of wrong-passwords-over-weeks doesn't compound).
        LocalDateTime windowStart = now.minusMinutes(AccountLockoutPolicy.ATTEMPT_WINDOW_MINUTES);
        if (user.getLastFailedLoginAt() == null || user.getLastFailedLoginAt().isBefore(windowStart)) {
            user.setFailedLoginAttempts(1);
        } else {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        }
        user.setLastFailedLoginAt(now);

        if (user.getFailedLoginAttempts() >= AccountLockoutPolicy.MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockedUntil = AccountLockoutPolicy.computeLockedUntil(user.getLockoutCount());
            user.setLockedUntil(lockedUntil);
            user.setLockoutCount(user.getLockoutCount() + 1);
            user.setFailedLoginAttempts(0); // reset counter; lockedUntil is the gate now
            log.warn("Account locked: userId={} lockoutCount={} lockedUntil={}",
                user.getId(), user.getLockoutCount(), lockedUntil);
        }

        userRepository.save(user);
    }

    public RefreshResponse refresh(String refreshToken) {
        try {
            // GAP-520 — verify via JwtKeyService so refresh tokens issued under
            // the previous signing key are still honored during the rotation window.
            Claims claims = jwtKeyService.parse(refreshToken).getPayload();

            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                throw new IllegalArgumentException("Invalid token type");
            }

            // GAP-1075 — reject refresh tokens revoked via logout. Fail-open inside
            // the blacklist service: a Redis outage returns false (token honored).
            if (refreshTokenBlacklistService != null
                    && refreshTokenBlacklistService.isBlacklisted(refreshToken)) {
                throw new IllegalArgumentException("Refresh token has been revoked");
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

    /**
     * Server-side logout (GAP-1075): revoke the supplied refresh token by adding it to the
     * Redis blacklist for its remaining lifetime. Idempotent + fail-open — an invalid or
     * expired token, a non-refresh token, or a Redis outage all return normally (the caller
     * clears local tokens regardless; the access token is stateless and expires on its own).
     *
     * @param refreshToken the refresh token to revoke (no-op when null/blank or blacklist
     *                     service unavailable, e.g. in legacy unit tests)
     */
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || refreshTokenBlacklistService == null) {
            return;
        }
        try {
            Claims claims = jwtKeyService.parse(refreshToken).getPayload();
            if (!"refresh".equals(claims.get("type", String.class))) {
                return; // only refresh tokens are revocable here
            }
            Instant expiry = (claims.getExpiration() != null)
                ? claims.getExpiration().toInstant()
                : Instant.now().plus(7, ChronoUnit.DAYS);
            refreshTokenBlacklistService.blacklist(
                refreshToken, Duration.between(Instant.now(), expiry));
            log.info("Refresh token revoked for userId={}", claims.getSubject());
        } catch (Exception ex) {
            // Invalid/expired token — nothing to revoke; logout stays idempotent.
            log.debug("Logout with non-parseable refresh token (ignored): {}", ex.getMessage());
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
        emailSenderService.sendVerificationEmail(email, verifyUrl);
    }

    private String generateAccessToken(UUID userId, String email, String role) {
        // GAP-520 — always sign with the CURRENT key via JwtKeyService.
        SecretKey key = jwtKeyService.signingKey();
        Instant now = Instant.now();

        // GAP-704 (Wave 104 Bucket A) — enrich JWT with tenantId claim for tenant-scoped roles.
        // PLATFORM_ADMIN is tenant-agnostic (operates across all tenants) so MUST NOT receive
        // a tenantId claim. Tenant binding for OWNER role lives in instances.owner_id
        // (users.tenant_id is NULL post-signup by current schema design).
        // See GAP-704 (closes root cause of GAP-531 PARTIAL) and
        // documents/04-quality/audits/local-stack/2026-05-22-wave-103-owner-persona-walk.md.
        var builder = Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("type", "access");

        UUID tenantId = resolveTenantIdForRole(userId, role);
        if (tenantId != null) {
            builder.claim("tenantId", tenantId.toString());
        }

        // GAP-1020 (tier-entitlement enforcement) — carry the instance's current-effective
        // tier so the gateway can inject a trusted X-Subscription-Tier header. AI branding
        // (and any future tier-gated feature) reads that header; a client-supplied value is
        // stripped + re-injected from this verified claim so it cannot be spoofed.
        builder.claim("tier", resolveTierForRole(userId, role));

        return builder
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(24, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();
    }

    /**
     * Resolve the subscription tier for the {@code tier} JWT claim (GAP-1020).
     *
     * <p>{@code instances.tier} is the current-effective tier (SUB-21 invariant,
     * V68 backfill). OWNER resolves via {@code instances.owner_id}; non-OWNER roles
     * (and OWNERs with no resolvable instance) default to {@code FREE} — the safe
     * least-privilege fallback for tier-gated features. STAFF tier inheritance is a
     * follow-up if branding is ever opened to staff (branding is owner-scoped today).</p>
     *
     * @param userId user UUID (JWT subject)
     * @param role uppercase role string
     * @return tier name (e.g. {@code FREE} / {@code PREMIUM}), never null
     */
    private String resolveTierForRole(UUID userId, String role) {
        if ("OWNER".equals(role)) {
            return instanceRepository.findByOwnerIdAndDeletedFalse(userId).stream()
                .findFirst()
                .map(Instance::getTier)
                .map(Enum::name)
                .orElse("FREE");
        }
        return "FREE";
    }

    /**
     * Resolve the tenant binding for a given user+role to enrich JWT {@code tenantId} claim.
     *
     * <p>Phase 1 BETA semantics (per GAP-704):
     * <ul>
     *   <li>{@code PLATFORM_ADMIN} → returns {@code null} (tenant-agnostic; claim omitted).</li>
     *   <li>{@code OWNER} → queries {@code instances.owner_id} (canonical binding).</li>
     *   <li>Other tenant-scoped roles (STAFF/TEACHER/PARENT/STUDENT) → not yet wired here;
     *       returns {@code null} until per-role tenant lookup lands (tracked in GAP-531 follow-up).
     *       This is intentional: those roles do not currently issue tokens via this service
     *       (staff invitations + parent/student logins ship in later waves).</li>
     * </ul>
     *
     * <p>Multi-instance determinism (GAP-1306): {@code findByOwnerIdAndDeletedFalse}
     * now returns instances ordered by {@code createdAt ASC, id ASC}, so for an owner with
     * &gt;1 non-deleted instance the OLDEST non-deleted instance is selected
     * DETERMINISTICALLY. This no longer relies on the prior "beta-signup gates a single
     * tenant per owner" assumption (which was never DB-enforced); the {@code tenantId} claim
     * is now stable across repeated mints regardless of how many instances an owner holds.
     *
     * @param userId user UUID (JWT subject)
     * @param role uppercase role string ({@code PLATFORM_ADMIN} / {@code OWNER} / etc.)
     * @return tenant UUID for tenant-scoped roles, or {@code null} when claim should be omitted
     */
    private UUID resolveTenantIdForRole(UUID userId, String role) {
        if (role == null || "PLATFORM_ADMIN".equals(role) || "ADMIN".equals(role)) {
            // Tenant-agnostic role — never emit tenantId claim.
            return null;
        }
        if ("OWNER".equals(role)) {
            return instanceRepository.findByOwnerIdAndDeletedFalse(userId).stream()
                .findFirst()
                .map(Instance::getId)
                .orElse(null);
        }
        if ("STAFF".equals(role) && staffInvitationRepository != null) {
            // GAP-531 follow-up (Wave flow-kc2): a STAFF user's tenant is the tenant of
            // the invitation they accepted. accepted_user_id == JWT subject (userId).
            return staffInvitationRepository
                .findFirstByAcceptedUserIdAndStatus(userId, StaffInvitationStatus.ACCEPTED)
                .map(inv -> inv.getTenantId())
                .orElse(null);
        }
        // TEACHER / PARENT / STUDENT — wire when those auth paths land (later waves).
        // Returning null keeps the JWT shape stable; downstream APIs requiring tenantId
        // will surface the gap explicitly rather than silently issue an unscoped token.
        return null;
    }

    private String generateRefreshToken(UUID userId) {
        SecretKey key = jwtKeyService.signingKey();
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
