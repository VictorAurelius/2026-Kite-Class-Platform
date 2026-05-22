package com.kitehub.subscription.audit.login;

import com.kitehub.platform.domain.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Per-login audit + new-fingerprint alert orchestrator (GAP-517 / OWASP A07 §2.5).
 *
 * <p>Workflow on every successful login:</p>
 * <ol>
 *   <li>Compute SHA-256 fingerprint of {@code ip || "|" || user_agent}</li>
 *   <li>Persist a {@link LoginAuditLog} row</li>
 *   <li>If user role = {@code PLATFORM_ADMIN}: check whether this fingerprint
 *       has been seen for this user within the last
 *       {@value #ALERT_COOLDOWN_HOURS}h</li>
 *   <li>If NEW + outside cooldown → publish {@link AdminLoginNewFingerprintEvent}
 *       and flip {@code alert_sent=true}</li>
 * </ol>
 *
 * <p>Why a Spring ApplicationEvent (not direct call)? Decouples the login
 * critical path from email I/O — see {@link AdminLoginAlertEventListener}
 * for the async @TransactionalEventListener consumer. Login response NEVER
 * waits on the alert.</p>
 *
 * @since 1.0.0 (Wave 72b Bucket C GAP-517)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAuditService {

    /** Cooldown window — same fingerprint within this many hours = no re-alert. */
    public static final int ALERT_COOLDOWN_HOURS = 24;

    /** Role string for PLATFORM_ADMIN per {@code users.role}. */
    public static final String PLATFORM_ADMIN_ROLE = "PLATFORM_ADMIN";

    private final LoginAuditLogRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Record a successful login + fire alert if PLATFORM_ADMIN + new fingerprint.
     *
     * <p>Invoked by {@code AuthService.login} after password verification but
     * BEFORE the JWT is returned. Never throws — audit failures are logged
     * and swallowed so they cannot block authentication.</p>
     *
     * <p>Runs in {@link Propagation#REQUIRES_NEW REQUIRES_NEW} so a SQL
     * failure here cannot poison the caller's transaction. Without this,
     * Spring marks the outer login transaction rollback-only when our
     * statement fails, causing the caller to throw {@code
     * UnexpectedRollbackException} at commit even though we caught the
     * exception locally (production 500 incident 2026-05-16).</p>
     *
     * @param user the user that just authenticated
     * @param request the incoming servlet request (used to extract IP + UA)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogin(User user, HttpServletRequest request) {
        try {
            String ip = extractClientIp(request);
            String userAgent = (request == null) ? null : request.getHeader("User-Agent");
            String fingerprint = computeFingerprint(ip, userAgent);
            LocalDateTime now = LocalDateTime.now();

            final LoginAuditLog row = repository.save(LoginAuditLog.builder()
                .userId(user.getId())
                .loginAt(now)
                .ip(ip)
                .userAgent(truncate(userAgent, 512))
                .fingerprintHash(fingerprint)
                .alertSent(false)
                .build());

            if (!PLATFORM_ADMIN_ROLE.equals(user.getRole())) {
                return; // non-admin: just audit, no alert
            }

            // Cooldown check: same fingerprint seen in last 24h? Skip alert.
            // Bounded to 1 row via Pageable so multi-row hit cannot emit
            // "Query did not return a unique result" WARN (GAP-707).
            LocalDateTime cooldownStart = now.minusHours(ALERT_COOLDOWN_HOURS);
            Optional<LoginAuditLog> prior = repository
                .findRecentByUserAndFingerprint(
                    user.getId(), fingerprint, cooldownStart, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                // Filter out the row we just inserted
                .filter(r -> !r.getId().equals(row.getId()));

            if (prior.isPresent()) {
                log.debug("Admin login fingerprint already seen within cooldown: userId={}",
                    user.getId());
                return;
            }

            // New fingerprint for PLATFORM_ADMIN → emit event + flip flag
            row.setAlertSent(true);
            row.setAlertSentAt(now);
            repository.save(row);

            eventPublisher.publishEvent(new AdminLoginNewFingerprintEvent(
                row.getId(), user.getId(), user.getEmail(), ip, userAgent));

            log.info("Admin new-fingerprint login event published: userId={} auditLogId={}",
                user.getId(), row.getId());
        } catch (Exception ex) {
            // Audit failure must NEVER block login. Log and continue.
            log.warn("LoginAuditService.recordLogin failed (login proceeds anyway): {}",
                ex.getMessage());
        }
    }

    /**
     * Compute hex SHA-256 of {@code ip || "|" || ua}. Returns null-safe placeholder
     * when both inputs are absent so we still write a row (no fingerprint match
     * across unknown sources).
     */
    static String computeFingerprint(String ip, String userAgent) {
        String input = (ip == null ? "" : ip) + "|" + (userAgent == null ? "" : userAgent);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed in every JRE; this is impossible.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // First IP is the original client; rest is proxy chain.
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
