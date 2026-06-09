package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.repository.InstanceRepository;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating JWT access + refresh tokens.
 *
 * <p>Since Wave 72a GAP-520, signing delegates to {@link JwtKeyService} so the
 * dual-key rotation policy applies uniformly across every issuer in the
 * subscription service.</p>
 *
 * <p>Since Wave 104 Bucket A (GAP-704), access tokens carry a {@code tenantId}
 * claim for tenant-scoped roles (OWNER/TEACHER/PARENT/STUDENT). PLATFORM_ADMIN
 * is tenant-agnostic and never receives the claim. Tenant binding for OWNER
 * lives in {@code instances.owner_id} (per current Phase 1 BETA schema).</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class TokenService {

    private final JwtKeyService jwtKeyService;
    private final InstanceRepository instanceRepository;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public TokenService(
        JwtKeyService jwtKeyService,
        InstanceRepository instanceRepository,
        @Value("${jwt.access-token-expiration:86400000}") long accessTokenExpiration,
        @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration
    ) {
        this.jwtKeyService = jwtKeyService;
        this.instanceRepository = instanceRepository;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Generate access token for user. Enriches with {@code tenantId} claim for
     * tenant-scoped roles per GAP-704 (Wave 104 Bucket A).
     *
     * @param userId user UUID
     * @param email user email
     * @param role user role (uppercase canonical, e.g. {@code OWNER} / {@code PLATFORM_ADMIN})
     * @return JWT access token
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        JwtBuilder builder = Jwts.builder()
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
            .issuedAt(now)
            .expiration(expiration)
            .signWith(jwtKeyService.signingKey())
            .compact();
    }

    /**
     * Resolve the subscription tier for the {@code tier} JWT claim (GAP-1020).
     *
     * <p>{@code instances.tier} is the current-effective tier (SUB-21 invariant,
     * V68 backfill) so no extra resolution is needed beyond reading the bound
     * instance. OWNER resolves via {@code instances.owner_id}; non-OWNER roles
     * (and OWNERs with no resolvable instance) default to {@code FREE} — the safe
     * least-privilege fallback for tier-gated features.</p>
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
     * Resolve tenant binding for the {@code tenantId} JWT claim (mirror of
     * {@code AuthService.resolveTenantIdForRole}). PLATFORM_ADMIN returns null
     * (tenant-agnostic); OWNER queries {@code instances.owner_id}; other
     * tenant-scoped roles return null until their auth paths land.
     */
    private UUID resolveTenantIdForRole(UUID userId, String role) {
        if (role == null || "PLATFORM_ADMIN".equals(role) || "ADMIN".equals(role)) {
            return null;
        }
        if ("OWNER".equals(role)) {
            return instanceRepository.findByOwnerIdAndDeletedFalse(userId).stream()
                .findFirst()
                .map(Instance::getId)
                .orElse(null);
        }
        return null;
    }

    /**
     * Generate refresh token for user.
     *
     * @param userId user UUID
     * @return JWT refresh token
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
            .subject(userId.toString())
            .claims(Map.of("type", "refresh"))
            .issuedAt(now)
            .expiration(expiration)
            .signWith(jwtKeyService.signingKey())
            .compact();
    }
}
