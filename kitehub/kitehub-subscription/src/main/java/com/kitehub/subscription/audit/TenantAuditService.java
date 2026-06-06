package com.kitehub.subscription.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tenant-lifecycle audit writer (GAP-949, Wave provisioning-1 Bucket B).
 *
 * <p>Persists an {@link AdminAuditLog} row for tenant-provisioning lifecycle
 * events (PDPL Art 11 + OWASP A09 audit trail). Before this, beta-invite tenant
 * creation only emitted {@code log.info(...)} — no answerable "tenant X
 * provisioned when / via which beta-invite / from which IP / which user-agent"
 * forensic trail.</p>
 *
 * <p>Each method runs in {@link Propagation#REQUIRES_NEW REQUIRES_NEW} +
 * try/catch per {@code .claude/rules/audit-service-isolation.md} §1 — an audit
 * write failure (SQL error, constraint violation, lock timeout) MUST NOT poison
 * the caller's registration/deletion transaction. Both layers are required:
 * REQUIRES_NEW isolates the physical transaction so the failure can never set
 * rollback-only on the parent; the catch keeps the caller from seeing a checked
 * failure (cf. 2026-05-16 admin-login 500 incident — default propagation +
 * try/catch still threw {@code UnexpectedRollbackException}).</p>
 *
 * <p>Designed to host the full tenant-lifecycle audit surface. Bucket G adds
 * {@code recordTenantDeleted(...)} to this same class so the two compose cleanly
 * under one tenant-audit helper (consistent {@code recordTenant*} naming).</p>
 *
 * @since 1.0.0 (Wave provisioning-1 Bucket B GAP-949)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAuditService {

    /** Audit action for a successful tenant provisioning (beta-invite registration). */
    public static final String ACTION_TENANT_PROVISIONED = "TENANT_PROVISIONED";
    /** Semantic resource type for tenant-scoped audit rows ({@code target_resource_type}). */
    static final String RESOURCE_TYPE_TENANT = "tenant";
    /** JPA entity type backing a tenant (the platform {@code Instance}). */
    static final String ENTITY_TYPE_INSTANCE = "Instance";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AdminAuditLogRepository repository;

    /**
     * Record a {@code TENANT_PROVISIONED} audit row after a successful beta-invite
     * tenant provisioning (GAP-949).
     *
     * <p>Best-effort + isolated per {@code audit-service-isolation.md} §1 — never
     * throws; an audit failure leaves the caller's registration intact.</p>
     *
     * <p>The acting principal ({@code admin_user_id}) is recorded as the newly
     * provisioned tenant owner — this is self-service provisioning via an
     * operator-approved beta invite, so there is no separate admin in the
     * security context. {@code admin_user_id} is {@code NOT NULL} + FK to
     * {@code users(id)}.</p>
     *
     * @param tenantId   provisioned tenant/instance id (target)
     * @param ownerId    tenant-owner user id (recorded as the acting principal)
     * @param ownerEmail tenant-owner email (payload — operator-approved beta data)
     * @param subdomain  requested tenant subdomain (payload)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTenantProvisioned(UUID tenantId, UUID ownerId,
                                        String ownerEmail, String subdomain) {
        try {
            AdminAuditLog.AdminAuditLogBuilder builder = AdminAuditLog.builder()
                .adminUserId(ownerId)
                .action(ACTION_TENANT_PROVISIONED)
                .targetEntityType(ENTITY_TYPE_INSTANCE)
                .targetEntityId(tenantId != null ? truncate(tenantId.toString(), 128) : null)
                .targetResourceType(RESOURCE_TYPE_TENANT)
                .targetResourceId(tenantId != null ? truncate("tenant/" + tenantId, 256) : null)
                .payloadJson(buildPayloadJson(tenantId, ownerEmail, subdomain))
                .success(true)
                .createdAt(LocalDateTime.now());

            populateRequest(builder);
            repository.save(builder.build());
            log.info("Audit row written: action={} tenantId={} subdomain={}",
                ACTION_TENANT_PROVISIONED, tenantId, subdomain);
        } catch (Exception ex) {
            // Audit write must NEVER fail tenant provisioning. Log + continue.
            log.warn("TenantAuditService.recordTenantProvisioned failed "
                + "(provisioning proceeds anyway): {}", ex.getMessage());
        }
    }

    /**
     * Best-effort capture of request provenance (IP + user-agent + correlation id)
     * from the current servlet request, mirroring {@link AdminAuditAspect}. No-op
     * when invoked outside a request scope (e.g. async / scheduled callers).
     */
    private void populateRequest(AdminAuditLog.AdminAuditLogBuilder builder) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            return;
        }
        HttpServletRequest req = sra.getRequest();
        builder.requestIp(extractClientIp(req));
        builder.userAgent(truncate(req.getHeader("User-Agent"), 512));
        String requestId = req.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = req.getHeader("traceparent");
        }
        if (requestId != null && !requestId.isBlank()) {
            builder.requestId(truncate(requestId, 64));
        }
    }

    private String extractClientIp(HttpServletRequest req) {
        // Trust X-Forwarded-For only behind the gateway; first IP in the list is client.
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) {
                return truncate(first, 64);
            }
        }
        return truncate(req.getRemoteAddr(), 64);
    }

    private String buildPayloadJson(UUID tenantId, String ownerEmail, String subdomain) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId != null ? tenantId.toString() : null);
        payload.put("subdomain", subdomain);
        payload.put("ownerEmail", ownerEmail);
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize tenant audit payload — falling back: {}", ex.getMessage());
            return "{\"_serialization_error\":\"" + ex.getClass().getSimpleName() + "\"}";
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
