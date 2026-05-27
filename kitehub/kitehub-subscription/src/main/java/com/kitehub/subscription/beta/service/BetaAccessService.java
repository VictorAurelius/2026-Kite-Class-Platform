package com.kitehub.subscription.beta.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.dto.BetaClaimCodeExchangeResponse;
import com.kitehub.subscription.beta.dto.BetaRejectCommand;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.dto.BetaSignupCommand;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Beta access request lifecycle service (GAP-372 Wave 33).
 *
 * <p>Encapsulates the 3-stage Phase 1 BETA invite flow + enforces status
 * transitions per {@link BetaAccessRequestStatus} state machine:</p>
 *
 * <ol>
 *   <li>{@link #submitRequest(BetaRequestDto)} — public submission, PENDING</li>
 *   <li>{@link #approveRequest(Long, BetaApproveCommand)} — coordinator approve;
 *       issues {@code inviteToken}, expiry +24h, publishes
 *       {@code beta.invite.sent} event via Outbox (consumed by kitehub-email)</li>
 *   <li>{@link #rejectRequest(Long, BetaRejectCommand)} — coordinator reject</li>
 *   <li>{@link #completeBetaSignup(BetaSignupCommand)} — invitee redeems token,
 *       row flipped to SIGNED_UP and token cleared</li>
 * </ol>
 *
 * <p>Per {@code design-patterns.md §3.5}, the {@code beta.invite.sent} event is
 * published via the per-module subscription outbox — no direct {@code rabbitTemplate.send}.</p>
 *
 * @since Wave 33 — GAP-372
 */
@Service
@Slf4j
public class BetaAccessService {

    /** 24h TTL per gap spec. */
    static final long INVITE_TOKEN_TTL_HOURS = 24L;

    /** Outbox event type for invite-issued events. */
    static final String EVENT_TYPE_INVITE_SENT = "beta.invite.sent";

    /** Outbox topic / routing key for kitehub-email consumer. */
    static final String TOPIC_INVITE_SENT = "email.beta.invite";

    /** Outbox event type for PDPL consent-given audit log (Wave 35 GAP-385). */
    static final String EVENT_TYPE_CONSENT_GIVEN = "beta.consent.given";

    /** Outbox topic / routing key for audit consumer (Wave 35 GAP-385). */
    static final String TOPIC_CONSENT_GIVEN = "audit.beta.consent";

    /** Counter name prefix for beta funnel metrics (GAP-387). */
    static final String METRIC_SIGNUP_REQUESTS = "beta_signup_requests_total";
    static final String METRIC_APPROVALS = "beta_signup_approvals_total";
    static final String METRIC_REJECTIONS = "beta_signup_rejections_total";
    static final String METRIC_HONEYPOT_REJECTIONS = "beta_honeypot_rejections_total";

    /** Per-email rate-limit rejection counter (GAP-388 Wave 36 Bucket A 388-C). */
    static final String METRIC_RATE_LIMIT_REJECTIONS = "beta_rate_limit_rejections_total";

    /** Per-email rate-limit window — 1 request per 24h per email. */
    static final Duration EMAIL_RATE_LIMIT_WINDOW = Duration.ofHours(24L);

    private static final SecureRandom CLAIM_CODE_RNG = new SecureRandom();

    /**
     * Pattern matching HTML tags / open-bracket sequences (matches anything
     * between {@code <} and {@code >}) — Wave 105 Bucket E0 Bug 2 stored-XSS
     * defense-in-depth for {@code name} + {@code orgName} + {@code referralSource}
     * free-text fields.
     */
    private static final java.util.regex.Pattern HTML_TAG_PATTERN =
            java.util.regex.Pattern.compile("<[^>]*>");

    /**
     * Sanitize free-text user input for stored-XSS defense-in-depth (Wave 105
     * Bucket E0 Bug 2 — failure-mode matrix A4).
     *
     * <p>React FE auto-escapes (defense layer 1). This method is layer 2 at
     * input boundary — strip any HTML tag sequences + apply Spring's
     * {@code HtmlUtils.htmlEscape(input, "UTF-8")} for &lt;/&gt;/&quot;/&apos;/&amp;
     * entity encoding ONLY. Result is safe to render verbatim in any HTML
     * context (admin panel, email template, audit log render).
     *
     * <p><b>VN UTF-8 preservation</b> (Wave 106 GAP-764 fix): the single-arg
     * {@code HtmlUtils.htmlEscape(input)} escapes ALL non-ASCII chars as
     * numeric character references — Vietnamese diacritics (â ê ô ữ etc.)
     * get corrupted into entities like {@code &acirc;}. The two-arg variant
     * with {@code "UTF-8"} encoding restricts escape scope to the 5 XSS chars
     * only, preserving Unicode raw. Reference: Spring HtmlUtils Javadoc.
     *
     * <p>Trade-offs: Jsoup with {@code Safelist.none()} would be more
     * comprehensive (handles obscure encoded vectors) but adds a heavy
     * dependency for 3 free-text fields. Phase 1 BETA scope: regex strip +
     * HtmlEscape (UTF-8 mode) is sufficient when paired with React auto-escape FE.
     *
     * @param input raw user input (may be null)
     * @return sanitized text safe for any HTML context, or null if input null
     */
    static String sanitizeFreeText(String input) {
        if (input == null) {
            return null;
        }
        // Strip HTML tag sequences first (defense against `<script>`, `<iframe>`,
        // `<img onerror=...>`, etc.)
        String stripped = HTML_TAG_PATTERN.matcher(input).replaceAll("");
        // Then HTML-entity-encode ONLY the 5 XSS chars `<`, `>`, `"`, `'`, `&`.
        // UTF-8 mode preserves Vietnamese diacritics raw (per GAP-764 fix Wave 106).
        return org.springframework.web.util.HtmlUtils.htmlEscape(stripped, "UTF-8");
    }

    private final BetaAccessRequestRepository repository;
    private final SubscriptionEventEmitter eventEmitter;
    private final MeterRegistry meterRegistry;

    /**
     * Email dispatch client (GAP-702 Wave 104 B1).
     *
     * <p>{@code @Autowired(required = false)} so unit tests not exercising the
     * email path can omit it. Production wiring always provides the bean.</p>
     */
    private final EmailServiceClient emailServiceClient;

    /**
     * Base URL for signup landing page where invitee enters claim code.
     *
     * <p>Sourced from {@code kitehub.beta.signup-base-url} config key with
     * production-safe default. Email body links to
     * {@code {base}/signup/beta?code=...} so the invitee lands on the prefilled
     * signup form.</p>
     */
    @Value("${kitehub.beta.signup-base-url:https://kitehub.me}")
    private String betaSignupBaseUrl;

    /**
     * Per-email rate-limit cache (GAP-388 Wave 36 Bucket A 388-C).
     *
     * <p>Caffeine in-memory cache mirrors {@link com.kitehub.subscription.config.CacheConfig}
     * pattern. Key = email (lower-cased); value = first-IP that hit the limit;
     * TTL = 24h per {@link #EMAIL_RATE_LIMIT_WINDOW}. Multi-pod coherence is a
     * follow-up gap (Redis migration mirrors GAP-132 — see service-class
     * javadoc + GAP-388 §388-C Log).</p>
     */
    private final Cache<String, String> emailRateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(EMAIL_RATE_LIMIT_WINDOW)
            .maximumSize(100_000)
            .build();

    public BetaAccessService(BetaAccessRequestRepository repository,
                             SubscriptionEventEmitter eventEmitter,
                             MeterRegistry meterRegistry,
                             @Autowired(required = false) EmailServiceClient emailServiceClient) {
        this.repository = repository;
        this.eventEmitter = eventEmitter;
        this.meterRegistry = meterRegistry;
        this.emailServiceClient = emailServiceClient;
    }

    /**
     * Resolve a {@link Counter} for the beta funnel events (GAP-387).
     *
     * <p>Counters are created lazily per-persona-tag because Bean Validation
     * enforces the persona vocabulary ({@code P1_SOLO_TEACHER} /
     * {@code P2_CENTER_OWNER}) — the dimension cardinality is bounded.</p>
     */
    private Counter signupCounter(String persona) {
        return Counter.builder(METRIC_SIGNUP_REQUESTS)
                .description("Total beta access requests submitted, tagged by persona")
                .tag("persona", normalizePersona(persona))
                .register(meterRegistry);
    }

    private Counter approvalCounter(String persona) {
        return Counter.builder(METRIC_APPROVALS)
                .description("Total beta access requests approved by coordinator, tagged by persona")
                .tag("persona", normalizePersona(persona))
                .register(meterRegistry);
    }

    private Counter rejectionCounter(String persona) {
        return Counter.builder(METRIC_REJECTIONS)
                .description("Total beta access requests rejected by coordinator, tagged by persona")
                .tag("persona", normalizePersona(persona))
                .register(meterRegistry);
    }

    private Counter honeypotCounter() {
        return Counter.builder(METRIC_HONEYPOT_REJECTIONS)
                .description("Total beta signup submissions silently rejected by honeypot anti-bot trap")
                .register(meterRegistry);
    }

    private Counter rateLimitCounter() {
        return Counter.builder(METRIC_RATE_LIMIT_REJECTIONS)
                .description("Total beta access requests rejected by per-email 24h rate limit (GAP-388)")
                .register(meterRegistry);
    }

    /** Defensive null/blank handling so unknown personas roll up under "unknown". */
    private static String normalizePersona(String persona) {
        return (persona == null || persona.isBlank()) ? "unknown" : persona;
    }

    /**
     * Submit a fresh beta access request.
     *
     * <p>Idempotency: a duplicate submit from the same email while an existing
     * PENDING row is open returns the existing row instead of creating a second
     * one (avoids inbox spam from honest mistakes; coordinator only sees one).</p>
     *
     * <p>Per-email rate limit (GAP-388 Wave 36 Bucket A 388-C): if a different
     * IP attempts the same email within 24h, throws
     * {@link BetaRateLimitExceededException} (mapped to HTTP 429 by controller).
     * Defends against bots cycling many unique emails through one IP.</p>
     */
    @Transactional
    public BetaAccessRequest submitRequest(BetaRequestDto dto) {
        return submitRequest(dto, null);
    }

    /**
     * Submit overload that accepts the originating IP for the per-email rate
     * limit. Controller passes the trusted IP (X-Forwarded-For chain resolved
     * upstream); tests may pass {@code null} to skip the IP-based audit log.
     */
    @Transactional
    public BetaAccessRequest submitRequest(BetaRequestDto dto, String ipAddress) {
        // 388-C: per-email 24h rate limit — reject 2nd attempt from different IP.
        String emailKey = dto.email().toLowerCase();
        String firstIp = emailRateLimitCache.getIfPresent(emailKey);
        if (firstIp != null && ipAddress != null && !firstIp.equals(ipAddress)) {
            rateLimitCounter().increment();
            log.warn("Beta access request rate-limited: email={} firstIp={} attemptIp={}",
                    dto.email(), firstIp, ipAddress);
            throw new BetaRateLimitExceededException(
                    "Email " + dto.email() + " rate-limited (24h window)");
        }

        Optional<BetaAccessRequest> existingPending = repository
                .findFirstByEmailAndStatusOrderByCreatedAtDesc(dto.email(), BetaAccessRequestStatus.PENDING);
        if (existingPending.isPresent()) {
            log.info("Duplicate PENDING beta request for {}, returning existing id={}",
                    dto.email(), existingPending.get().getId());
            return existingPending.get();
        }

        OffsetDateTime now = OffsetDateTime.now();
        // Wave 105 Bucket E0 Bug 2 — sanitize free-text fields to neutralize
        // stored XSS (failure-mode matrix A4). React FE auto-escapes (defense
        // layer 1); BE input sanitize = defense-in-depth (admin panel + email
        // template rendering paths must NOT trust raw user input).
        BetaAccessRequest entity = BetaAccessRequest.builder()
                .email(dto.email())
                .name(sanitizeFreeText(dto.name()))
                .orgName(sanitizeFreeText(dto.orgName()))
                .persona(dto.persona())
                .referralSource(sanitizeFreeText(dto.referralSource()))
                .status(BetaAccessRequestStatus.PENDING)
                .consentGiven(Boolean.TRUE.equals(dto.consentGiven()))
                .consentAt(now)
                .build();
        final BetaAccessRequest saved;
        try {
            saved = repository.saveAndFlush(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Wave 105 Bucket E0 Bug 3 + Bucket A A1 race-loser path — partial
            // unique index (V55 idx_beta_request_email_unique_pending) blocked
            // concurrent INSERT. Re-query and return the row that won the race;
            // same idempotency contract as the existence-check path above.
            log.info("Beta access request race-loser for email={} — returning winning PENDING row",
                    dto.email());
            return repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                    dto.email(), BetaAccessRequestStatus.PENDING)
                .orElseThrow(() -> ex); // re-throw if winning row vanished (shouldn't happen)
        }
        signupCounter(saved.getPersona()).increment();
        log.info("Beta access request submitted: id={} email={} persona={}",
                saved.getId(), saved.getEmail(), saved.getPersona());

        // PDPL 2023 Art 16 — emit consent-evidence audit event via outbox
        // (no direct rabbitTemplate.send per design-patterns.md §3.5).
        String consentPayload = String.format(
                "{\"requestId\":%d,\"email\":\"%s\",\"persona\":\"%s\",\"consentAt\":\"%s\"}",
                saved.getId(),
                SubscriptionEventEmitter.escape(saved.getEmail()),
                SubscriptionEventEmitter.escape(saved.getPersona()),
                saved.getConsentAt()
        );
        eventEmitter.emit((UUID) null, EVENT_TYPE_CONSENT_GIVEN, TOPIC_CONSENT_GIVEN, consentPayload);

        // 388-C: register the email/IP pair so subsequent same-email/different-IP
        // attempts trip the rate limit. Tests pass ipAddress=null and skip this.
        if (ipAddress != null) {
            emailRateLimitCache.put(emailKey, ipAddress);
        }

        return saved;
    }

    /**
     * Record a honeypot anti-bot rejection (GAP-387 funnel observability + GAP-388 388-A wire-up).
     *
     * <p>Bean Validation rejects honeypot-populated submissions before they
     * reach {@link #submitRequest(BetaRequestDto)} (see {@code BetaRequestDto}
     * {@code @Size(max = 0)}). The controller / exception handler can call this
     * method when it observes the validation failure to keep the bot-detection
     * counter visible in {@code /actuator/prometheus}.</p>
     *
     * <p>Wire-up from controller / @ExceptionHandler is tracked as part of the
     * same wave (Bucket A scope); the counter is exposed here so the metric
     * surface is owned by the service layer.</p>
     */
    public void recordHoneypotRejection() {
        recordHoneypotRejection(null, null);
    }

    /**
     * Record a honeypot rejection with audit context (GAP-388 388-A).
     *
     * <p>Wired from {@code BetaAccessController.handleValidationException} when
     * the {@code honeypot} field violates {@code @Size(max = 0)}. Captures the
     * attempted email + originating IP for post-incident triage / alert
     * investigation. Email and IP may be {@code null} when the controller
     * cannot extract them from the rejected payload.</p>
     */
    public void recordHoneypotRejection(String email, String ipAddress) {
        honeypotCounter().increment();
        log.warn("Beta access request rejected: honeypot field populated (likely bot) email={} ip={}",
                email == null ? "<unknown>" : email,
                ipAddress == null ? "<unknown>" : ipAddress);
    }

    /**
     * Exchange a 6-digit claim code for the underlying {@code invite_token}
     * UUID + pre-fill (GAP-388 388-B 2FA).
     *
     * <p>The signup form submits the code emailed to the invitee. Server
     * resolves the corresponding row and returns the UUID + pre-fill data.
     * Same lifecycle gating as {@link #validateToken(UUID)}: only APPROVED +
     * not-expired + not-already-signed-up rows succeed.</p>
     */
    public BetaClaimCodeExchangeResponse exchangeClaimCode(String claimCode) {
        Optional<BetaAccessRequest> opt = repository.findByClaimCode(claimCode);
        if (opt.isEmpty()) {
            return BetaClaimCodeExchangeResponse.invalid("CODE_NOT_FOUND");
        }
        BetaAccessRequest entity = opt.get();
        if (entity.getStatus() == BetaAccessRequestStatus.SIGNED_UP) {
            return BetaClaimCodeExchangeResponse.invalid("ALREADY_USED");
        }
        if (entity.getStatus() != BetaAccessRequestStatus.APPROVED) {
            return BetaClaimCodeExchangeResponse.invalid("CODE_NOT_FOUND");
        }
        if (entity.isTokenExpired()) {
            return BetaClaimCodeExchangeResponse.invalid("CODE_EXPIRED");
        }
        return BetaClaimCodeExchangeResponse.ok(
                entity.getInviteToken(),
                entity.getEmail(), entity.getName(), entity.getOrgName(), entity.getPersona());
    }

    /**
     * Generate a fresh 6-digit numeric claim code, retrying on uniqueness
     * collision (cardinality 10^6 — collisions only matter at high concurrent
     * APPROVED population which is bounded by the manual coordinator step).
     */
    private String generateUniqueClaimCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = String.format("%06d", CLAIM_CODE_RNG.nextInt(1_000_000));
            if (repository.findByClaimCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        // 5 collisions in a row is astronomically unlikely; surface as state.
        throw new IllegalStateException("Could not generate unique beta claim code after 5 attempts");
    }

    /** Paginated coordinator listing, default ordered by createdAt desc. */
    public Page<BetaAccessRequest> listByStatus(BetaAccessRequestStatus status, Pageable pageable) {
        return repository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    /**
     * Coordinator approve transition. Issues the {@code inviteToken} + publishes
     * the invite-sent event via Outbox in the same transaction (per
     * {@code design-patterns.md §3.5}).
     */
    @Transactional
    public BetaAccessRequest approveRequest(Long requestId, BetaApproveCommand cmd) {
        BetaAccessRequest entity = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Beta request not found: " + requestId));

        if (entity.getStatus() != BetaAccessRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot approve request in state " + entity.getStatus() + " (must be PENDING)");
        }

        OffsetDateTime now = OffsetDateTime.now();
        entity.setStatus(BetaAccessRequestStatus.APPROVED);
        entity.setApproverId(cmd.approverId());
        entity.setApprovedAt(now);
        entity.setInviteToken(UUID.randomUUID());
        entity.setInviteTokenExpiry(now.plusHours(INVITE_TOKEN_TTL_HOURS));
        entity.setInviteSentAt(now);
        // GAP-388 388-B: emit a 6-digit claim code instead of leaking the UUID
        // in the email body. Email shows the code; signup page exchanges it.
        entity.setClaimCode(generateUniqueClaimCode());

        BetaAccessRequest saved = repository.save(entity);

        // Outbox publish — consumed by kitehub-email to deliver invite link.
        // No direct rabbitTemplate.send (design-patterns.md §3.5).
        // Payload now includes claimCode (the user-facing 2FA code); inviteUrl on
        // the email side should NO LONGER include the UUID — it points to the
        // signup page that prompts for the code (GAP-388 388-B).
        String payload = String.format(
                "{\"requestId\":%d,\"email\":\"%s\",\"name\":\"%s\",\"orgName\":\"%s\",\"persona\":\"%s\",\"token\":\"%s\",\"claimCode\":\"%s\",\"expiresAt\":\"%s\"}",
                saved.getId(),
                SubscriptionEventEmitter.escape(saved.getEmail()),
                SubscriptionEventEmitter.escape(saved.getName()),
                SubscriptionEventEmitter.escape(saved.getOrgName()),
                SubscriptionEventEmitter.escape(saved.getPersona()),
                saved.getInviteToken(),
                saved.getClaimCode(),
                saved.getInviteTokenExpiry()
        );
        eventEmitter.emit((UUID) null, EVENT_TYPE_INVITE_SENT, TOPIC_INVITE_SENT, payload);

        // GAP-702 Wave 104 B1 — Wire the actual email-send path. The custom
        // `beta.invite.sent` outbox event above has no consumer in kitehub-email
        // (Wave 103 verify: 0 lines matching "Sending" after approve). The
        // working pipeline is the `email.queued` outbox event consumed by the
        // standard email dispatcher. Best-effort wrap per Exception A: failure
        // here does NOT roll back the approve transaction (the row is already
        // saved + the token is persisted, coordinator can re-send if needed).
        if (emailServiceClient != null) {
            try {
                String signupUrl = String.format("%s/signup/beta?code=%s",
                        trimTrailingSlash(betaSignupBaseUrl),
                        saved.getClaimCode());
                String expiresAt = formatExpiry(saved.getInviteTokenExpiry());
                emailServiceClient.sendBetaInviteEmail(
                        saved.getEmail(),
                        saved.getName(),
                        saved.getOrgName(),
                        saved.getClaimCode(),
                        signupUrl,
                        expiresAt);
            } catch (Exception emailEx) {
                // Outbox-first inside sendBetaInviteEmail handles reliability;
                // this catch protects the approve txn from any rare
                // dispatcher-side exception (e.g. ObjectMapper failure).
                log.warn("Beta invite email dispatch failed (request id={}, email={}): {}",
                        saved.getId(), saved.getEmail(), emailEx.getMessage());
            }
        } else {
            log.debug("EmailServiceClient not wired; beta-invite email skipped "
                    + "(test context). Request id={} claimCode={}",
                    saved.getId(), saved.getClaimCode());
        }

        approvalCounter(saved.getPersona()).increment();
        log.info("Beta access request approved: id={} email={} approver={}",
                saved.getId(), saved.getEmail(), cmd.approverId());
        return saved;
    }

    /** VN-style human date format for email body (per vn-localization-audit-checklist.md §1). */
    private static final DateTimeFormatter VN_EXPIRY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'lúc' HH:mm", Locale.forLanguageTag("vi-VN"));

    private static String formatExpiry(OffsetDateTime expiry) {
        if (expiry == null) {
            return "";
        }
        return VN_EXPIRY_FORMATTER.format(expiry);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Coordinator reject transition. Captures rejectionReason. */
    @Transactional
    public BetaAccessRequest rejectRequest(Long requestId, BetaRejectCommand cmd) {
        BetaAccessRequest entity = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Beta request not found: " + requestId));

        if (entity.getStatus() != BetaAccessRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot reject request in state " + entity.getStatus() + " (must be PENDING)");
        }

        entity.setStatus(BetaAccessRequestStatus.REJECTED);
        entity.setApproverId(cmd.approverId());
        entity.setRejectedAt(OffsetDateTime.now());
        entity.setRejectionReason(cmd.rejectionReason());
        BetaAccessRequest saved = repository.save(entity);
        rejectionCounter(saved.getPersona()).increment();
        log.info("Beta access request rejected: id={} email={} approver={}",
                saved.getId(), saved.getEmail(), cmd.approverId());
        return saved;
    }

    /**
     * Validate invite token at signup pre-fill time.
     *
     * <p>Returns a structured response so the FE can distinguish:
     * not-found / expired / already-used cases without leaking which (the
     * controller maps all to 404 in the response status; this DTO contains the
     * granular reason for the FE error message).</p>
     */
    public BetaTokenValidationResponse validateToken(UUID token) {
        Optional<BetaAccessRequest> opt = repository.findByInviteToken(token);
        if (opt.isEmpty()) {
            return BetaTokenValidationResponse.invalid("TOKEN_NOT_FOUND");
        }
        BetaAccessRequest entity = opt.get();
        if (entity.getStatus() == BetaAccessRequestStatus.SIGNED_UP) {
            return BetaTokenValidationResponse.invalid("ALREADY_USED");
        }
        if (entity.getStatus() != BetaAccessRequestStatus.APPROVED) {
            return BetaTokenValidationResponse.invalid("TOKEN_NOT_FOUND");
        }
        if (entity.isTokenExpired()) {
            return BetaTokenValidationResponse.invalid("TOKEN_EXPIRED");
        }
        return BetaTokenValidationResponse.ok(
                entity.getEmail(), entity.getName(), entity.getOrgName(), entity.getPersona());
    }

    /**
     * Complete the invitee signup — flips status to SIGNED_UP + clears token.
     *
     * <p>The actual tenant provisioning (subdomain reservation, owner-user
     * creation, password hashing) is delegated to the standard registration
     * service in a follow-up wire-up. This method is the gate that confirms a
     * valid token is being redeemed; the controller calls the registration
     * pipeline AFTER this returns success.</p>
     */
    @Transactional
    public BetaAccessRequest completeBetaSignup(BetaSignupCommand cmd) {
        BetaAccessRequest entity = repository.findByInviteToken(cmd.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));

        if (entity.getStatus() != BetaAccessRequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Cannot complete signup in state " + entity.getStatus() + " (must be APPROVED)");
        }
        if (entity.isTokenExpired()) {
            throw new IllegalStateException("Invite token expired");
        }

        entity.setStatus(BetaAccessRequestStatus.SIGNED_UP);
        entity.setInviteToken(null);
        entity.setInviteTokenExpiry(null);
        entity.setClaimCode(null);
        BetaAccessRequest saved = repository.save(entity);
        log.info("Beta signup completed: id={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    /** Single-row read used by coordinator detail view. */
    public Optional<BetaAccessRequest> getById(Long id) {
        return repository.findById(id);
    }

    /**
     * Rollback a SIGNED_UP transition back to APPROVED — used when downstream
     * registration provisioning fails after {@link #completeBetaSignup} returned
     * success (GAP-372 closure follow-up #1, Wave 45 Bucket A). Re-issues a
     * fresh invite token with a new 24h expiry so the invitee can retry.
     *
     * <p>This is intentionally a separate transactional method so the outer
     * controller can call it from a {@code catch} block without nesting it
     * inside the registration transaction.</p>
     *
     * @param id beta access request id returned from {@link #completeBetaSignup}
     */
    @Transactional
    public void rollbackSignup(Long id) {
        BetaAccessRequest entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Beta access request not found: " + id));
        if (entity.getStatus() != BetaAccessRequestStatus.SIGNED_UP) {
            log.warn("rollbackSignup skipped — request {} is in state {} (expected SIGNED_UP)",
                    id, entity.getStatus());
            return;
        }
        entity.setStatus(BetaAccessRequestStatus.APPROVED);
        entity.setInviteToken(UUID.randomUUID());
        entity.setInviteTokenExpiry(OffsetDateTime.now().plusHours(INVITE_TOKEN_TTL_HOURS));
        repository.save(entity);
        log.warn("Beta signup rolled back to APPROVED: id={} email={}", id, entity.getEmail());
    }
}
