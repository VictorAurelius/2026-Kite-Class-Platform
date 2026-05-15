package com.kitehub.subscription.impersonation;

import com.kitehub.subscription.impersonation.dto.ImpersonationEndResponse;
import com.kitehub.subscription.impersonation.dto.ImpersonationStartResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.JwtKeyService;
import com.kitehub.platform.domain.entity.Instance;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service for admin "View as tenant" impersonation (GAP-040 Wave 79 F-bis).
 *
 * <p>Workflow:
 * <ol>
 *   <li>{@link #startImpersonation} — validates target tenant exists, closes any
 *       stale active session for this admin (auto-timeout it), inserts a new
 *       audit row, mints a scoped JWT bearing {@code tenant_id} +
 *       {@code impersonated_by} claims with a 30-second TTL.</li>
 *   <li>{@link #endImpersonation} — admin clicks "Thoát ra"; row updated with
 *       {@link ImpersonationAuditEntry.EndedReason#MANUAL_EXIT}.</li>
 *   <li>{@link #expireStaleSessions} — scheduled sweep marks rows older than
 *       the TTL as {@link ImpersonationAuditEntry.EndedReason#AUTO_TIMEOUT}.
 *       Defensive — JWT expiry is the primary timeout; this keeps the audit
 *       log consistent if the admin never clicks "Thoát ra".</li>
 * </ol>
 *
 * <p>Audit-log discipline (GAP-040 AC): the audit row is INSERTED in the same
 * transaction as the JWT mint; if the row fails to persist, the JWT must NOT
 * be returned. The {@code @Transactional} boundary on {@link #startImpersonation}
 * enforces this.</p>
 *
 * @since Wave 79 (GAP-040)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImpersonationService {

    /**
     * Hard timeout for impersonation sessions. Non-extendable per GAP-040 AC —
     * admin must explicitly re-impersonate after expiry.
     */
    public static final Duration SESSION_TTL = Duration.ofSeconds(30);

    private final ImpersonationAuditRepository auditRepository;
    private final InstanceRepository instanceRepository;
    private final JwtKeyService jwtKeyService;

    @Value("${kitehub.impersonation.issuer:kitehub-subscription}")
    private String issuer;

    /**
     * Start an impersonation session.
     *
     * @param adminUserId  the platform admin's user id (from {@code SecurityContext})
     * @param tenantSlug   target tenant slug (e.g. "acme-school")
     * @param requestIp    admin's IP (audit)
     * @param userAgent    admin's UA (audit)
     * @return start response with scoped JWT + audit-log row id
     * @throws EntityNotFoundException if tenant slug doesn't resolve
     */
    @Transactional
    public ImpersonationStartResponse startImpersonation(
            UUID adminUserId,
            String tenantSlug,
            String requestIp,
            String userAgent) {

        if (adminUserId == null) {
            throw new IllegalArgumentException("adminUserId required");
        }
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new IllegalArgumentException("tenantSlug required");
        }

        Instance tenant = instanceRepository.findBySubdomainAndDeletedFalse(tenantSlug)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tenant not found for slug: " + tenantSlug));

        // Defensive — close any stale active session for this admin
        auditRepository.findActiveSession(adminUserId).ifPresent(stale -> {
            log.warn("Auto-closing stale impersonation session id={} for admin={} before starting new one",
                    stale.getId(), adminUserId);
            stale.setEndedAt(OffsetDateTime.now());
            stale.setEndedReason(ImpersonationAuditEntry.EndedReason.AUTO_TIMEOUT);
            auditRepository.save(stale);
        });

        OffsetDateTime startedAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = startedAt.plus(SESSION_TTL);

        ImpersonationAuditEntry entry = ImpersonationAuditEntry.builder()
                .adminUserId(adminUserId)
                .tenantId(tenant.getId())
                .tenantSlug(tenantSlug)
                .startedAt(startedAt)
                .requestIp(requestIp)
                .userAgent(userAgent)
                .createdAt(startedAt)
                .build();

        // Transaction binding (per GAP-040 AC): persist audit row BEFORE
        // minting the JWT. If save throws, no token escapes.
        ImpersonationAuditEntry saved = auditRepository.save(entry);

        String token = mintImpersonationToken(adminUserId, tenant.getId(), tenantSlug, startedAt, expiresAt);

        log.info("Impersonation started: sessionId={} admin={} tenant={} ({}); expires_at={}",
                saved.getId(), adminUserId, tenant.getId(), tenantSlug, expiresAt);

        return new ImpersonationStartResponse(
                saved.getId(),
                token,
                tenant.getId(),
                tenantSlug,
                expiresAt);
    }

    /**
     * End the admin's currently active impersonation session.
     *
     * @return end-response describing the row that was just closed
     * @throws EntityNotFoundException if no active session exists for the admin
     */
    @Transactional
    public ImpersonationEndResponse endImpersonation(UUID adminUserId) {
        ImpersonationAuditEntry active = auditRepository.findActiveSession(adminUserId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No active impersonation session for admin: " + adminUserId));

        OffsetDateTime now = OffsetDateTime.now();
        active.setEndedAt(now);
        active.setEndedReason(ImpersonationAuditEntry.EndedReason.MANUAL_EXIT);
        ImpersonationAuditEntry closed = auditRepository.save(active);

        log.info("Impersonation ended (MANUAL_EXIT): sessionId={} admin={} tenant={} duration_ms={}",
                closed.getId(), adminUserId, closed.getTenantId(),
                Duration.between(closed.getStartedAt(), now).toMillis());

        return new ImpersonationEndResponse(
                closed.getId(),
                closed.getEndedAt(),
                closed.getEndedReason());
    }

    /**
     * Sweep job — marks rows older than the TTL as AUTO_TIMEOUT.
     *
     * <p>Runs every 30s; the JWT expiry is the primary security gate, this
     * keeps the audit log consistent if the admin never clicks "Thoát ra".</p>
     */
    @Scheduled(fixedDelay = 30_000L)
    @Transactional
    public void expireStaleSessions() {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(SESSION_TTL);
        List<ImpersonationAuditEntry> stale = auditRepository.findExpiredActiveSessions(cutoff);
        if (stale.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (ImpersonationAuditEntry e : stale) {
            e.setEndedAt(now);
            e.setEndedReason(ImpersonationAuditEntry.EndedReason.AUTO_TIMEOUT);
        }
        auditRepository.saveAll(stale);
        log.info("Auto-expired {} impersonation session(s) at cutoff={}", stale.size(), cutoff);
    }

    public Page<ImpersonationAuditEntry> listAuditLog(Pageable pageable) {
        return auditRepository.findAllByOrderByStartedAtDesc(pageable);
    }

    private String mintImpersonationToken(
            UUID adminUserId,
            UUID tenantId,
            String tenantSlug,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt) {

        SecretKey key = jwtKeyService.signingKey();
        return Jwts.builder()
                .issuer(issuer)
                .subject(tenantId.toString())
                .claim("type", "impersonation")
                .claim("tenant_id", tenantId.toString())
                .claim("tenant_slug", tenantSlug)
                .claim("impersonated_by", adminUserId.toString())
                .issuedAt(Date.from(issuedAt.toInstant()))
                .expiration(Date.from(expiresAt.toInstant()))
                .signWith(key)
                .compact();
    }
}
