package com.kitehub.subscription.service;

import com.kitehub.subscription.audit.TenantAuditService;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin-triggered tenant-provisioning retry (GAP-953, UC-PROV-05, Wave provisioning-1 Bucket E).
 *
 * <p>When a tenant's KiteClass {@code FrontendInstance} ends up FAILED/stuck, a PLATFORM_ADMIN
 * can manually re-run provisioning instead of SSH-ing into RDS to flip the status by hand.
 * Mechanism mirrors the keystone {@code tenant.created} publish in
 * {@link AuthService#registerWithBetaInvite} (GAP-945 Bucket A): re-publishing the
 * {@code tenant.created} event re-drives kiteclass-core's {@code TenantProvisioningSaga} for the
 * affected instance. The saga detects the existing FAILED {@code FrontendInstance} (still present,
 * {@code deleted=false}) by slug and routes it through {@code InstanceLifecycleService.retry()}
 * (FAILED → INITIALIZING) rather than {@code initiate()} — which would otherwise slug-collision
 * throw. An already-DEPLOYED or in-flight instance is an idempotent no-op (no re-provision).</p>
 *
 * <p>The re-publish payload reuses the same field shape (tenantId/slug/audience/tone) the saga's
 * {@code TenantCreatedEvent} expects, composed via {@link SubscriptionEventEmitter#escape(String)}.
 * The emitter is outbox-backed (reliability) + fast-path (low latency) per
 * {@code design-patterns.md} §3.5.1.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-953)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTenantProvisioningService {

    /** Routing key / outbox topic for the {@code tenant.created} cross-service event (GAP-945). */
    static final String TENANT_CREATED_TOPIC = "tenant.created";
    /** Outbox event type label (matches {@link AuthService} keystone publish). */
    static final String TENANT_CREATED_EVENT = "TENANT_CREATED";
    /** Default audience/tone for the saga payload (matches {@link AuthService} defaults). */
    private static final String DEFAULT_AUDIENCE = "education";
    private static final String DEFAULT_TONE = "professional";

    private final InstanceService instanceService;
    private final SubscriptionEventEmitter tenantEventEmitter;
    private final TenantAuditService tenantAuditService;

    /**
     * Re-trigger tenant provisioning for {@code instanceId} (UC-PROV-05).
     *
     * <p>Looks up the instance (throws {@link jakarta.persistence.EntityNotFoundException} → 404
     * if missing/deleted), re-publishes {@code tenant.created} to re-drive the provisioning saga,
     * then writes a best-effort audit row. The outbox row written by the emitter participates in
     * this transaction (reliability net); the audit write is isolated via {@code REQUIRES_NEW}.</p>
     *
     * @param instanceId  tenant/instance to retry
     * @param adminUserId acting PLATFORM_ADMIN user id (gateway {@code X-User-Id}; nullable)
     * @param reason      optional admin-supplied reason (audit payload)
     * @return the current instance state (post re-publish)
     */
    @Transactional
    public InstanceResponse retryProvisioning(UUID instanceId, UUID adminUserId, String reason) {
        // 404 propagation via GlobalExceptionHandler when the instance is missing/deleted.
        InstanceResponse instance = instanceService.getInstanceById(instanceId);

        String slug = instance.getSlug() != null ? instance.getSlug() : instance.getSubdomain();
        String payloadJson = "{"
                + "\"tenantId\":\"" + SubscriptionEventEmitter.escape(String.valueOf(instance.getId())) + "\","
                + "\"slug\":\"" + SubscriptionEventEmitter.escape(slug) + "\","
                + "\"audience\":\"" + SubscriptionEventEmitter.escape(DEFAULT_AUDIENCE) + "\","
                + "\"tone\":\"" + SubscriptionEventEmitter.escape(DEFAULT_TONE) + "\"}";
        tenantEventEmitter.emit(instance.getId(), TENANT_CREATED_EVENT, TENANT_CREATED_TOPIC, payloadJson);
        log.info("Admin retry-provisioning: re-published tenant.created for instance {} slug {} (admin {})",
                instance.getId(), slug, adminUserId);

        // Best-effort audit (REQUIRES_NEW + try/catch inside the service isolates failures).
        tenantAuditService.recordTenantRetryRequested(instance.getId(), adminUserId, reason);

        return instance;
    }
}
