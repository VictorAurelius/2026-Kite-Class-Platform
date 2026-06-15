package com.kitehub.branding.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resolves the subscription tier of an instance SERVER-SIDE from the shared {@code kitehub}
 * database — NOT from the client-controlled {@code X-Subscription-Tier} request header.
 *
 * <p><strong>GAP-1020 (Part 2):</strong> AI-branding controllers read tier from a request header to
 * gate regenerate quota / AI rate-limit / input cap. Even though the gateway now strips a
 * client-supplied {@code X-Subscription-Tier} and re-injects the verified JWT {@code tier} claim
 * (anti-spoof), that claim is a login-time snapshot that goes stale on a mid-session tier change.
 * This resolver reads the authoritative current-effective tier ({@code instances.tier} — the
 * SUB-21 denormalized invariant kitehub-subscription's {@code TokenService} also uses) so a
 * downgraded instance cannot keep using a higher tier's quota until the next login.</p>
 *
 * <p>kitehub-branding shares the database with kitehub-subscription (validate-only DDL, no Instance
 * JPA entity), so the lookup is a scalar native query rather than a cross-service REST call —
 * avoiding an extra network hop + resilience surface while staying authoritative.</p>
 *
 * <p>Fail-safe: any lookup failure (no active subscription row, missing table in a test profile,
 * transient DB error) falls back to the gateway-trusted header value and ultimately {@code FREE} —
 * least privilege. It NEVER escalates to a higher tier on error.</p>
 */
@Slf4j
@Service
public class SubscriptionTierResolver {

    /** Least-privilege fallback when no authoritative tier can be resolved. */
    public static final String DEFAULT_TIER = "FREE";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Resolve the effective tier for entitlement decisions.
     *
     * @param instanceId           gateway-trusted instance id (data scope); may be {@code null}
     *                             for internal/unscoped calls
     * @param gatewayTrustedHeader the gateway-injected {@code X-Subscription-Tier} value — used only
     *                             as a fallback when the DB lookup yields nothing (it is itself
     *                             gateway-trusted, never the raw client value)
     * @return canonical upper-case tier; never escalates above the authoritative value on error
     */
    public String resolveEffectiveTier(UUID instanceId, String gatewayTrustedHeader) {
        if (instanceId != null) {
            String dbTier = resolveTier(instanceId);
            if (dbTier != null) {
                return normalize(dbTier);
            }
        }
        return normalize(gatewayTrustedHeader);
    }

    /**
     * Look up {@code instances.tier} for the given instance.
     *
     * @return the tier string, or {@code null} when not resolvable (caller falls back).
     */
    @Transactional(readOnly = true)
    public String resolveTier(UUID instanceId) {
        if (instanceId == null) {
            return null;
        }
        try {
            Object result = entityManager
                    .createNativeQuery("SELECT tier FROM instances WHERE id = :id")
                    .setParameter("id", instanceId)
                    .getSingleResult();
            return result != null ? result.toString() : null;
        } catch (RuntimeException ex) {
            // NoResultException (unknown instance) / missing table (test profile) / transient error.
            // Fail safe — return null so the caller falls back to least privilege.
            log.debug("Tier lookup for instance {} failed ({}); falling back", instanceId,
                    ex.getClass().getSimpleName());
            return null;
        }
    }

    private String normalize(String tier) {
        if (tier == null || tier.isBlank()) {
            return DEFAULT_TIER;
        }
        return tier.trim().toUpperCase();
    }
}
