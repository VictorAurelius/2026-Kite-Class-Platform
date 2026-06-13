package com.kitehub.subscription.controller;

import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.billing.dto.DowngradePreviewResponse;
import com.kitehub.subscription.billing.dto.PendingPaymentStatusResponse;
import com.kitehub.subscription.billing.dto.ReactivateResponse;
import com.kitehub.subscription.billing.service.OwnerBillingService;
import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.dto.TierChangeRequest;
import com.kitehub.subscription.security.TenantOwnershipGuard;
import com.kitehub.subscription.service.SubscriptionRenewalService;
import com.kitehub.subscription.service.SubscriptionService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for subscription management.
 *
 * <p>SLO Tier B (lifecycle reads + writes; class-level tag uses Tier B as
 * the dominant pattern). See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/platform/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription lifecycle, tier changes, and renewal management")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-b", "controller", "subscription"})
public class SubscriptionController {

    /**
     * GAP-562b (Wave 80 Bucket C): subscription mutations require OWNER role.
     * Read endpoints accept OWNER + STAFF + legacy admin aliases. Wave 81
     * cutoff (2026-06-14) collapses to canonical OWNER per
     * {@code com.kitehub.subscription.auth.role.PlatformRole}.
     */
    static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    static final String OWNER_OR_STAFF_AUTHZ =
            "hasAnyRole('OWNER','STAFF','PLATFORM_ADMIN','ADMIN')";

    /**
     * GAP-1015 (Wave security-2 Bucket B): {@code GET /expiring} returns expiring
     * subscriptions across ALL tenants — a cross-tenant data leak when exposed to an
     * OWNER/STAFF. It is an operational/renewal-reminder view with no owner-facing FE
     * caller, so it is restricted to platform admins.
     */
    static final String ADMIN_ONLY_AUTHZ =
            "hasAnyRole('PLATFORM_ADMIN','ADMIN')";

    private final SubscriptionService subscriptionService;
    private final SubscriptionRenewalService renewalService;
    private final OwnerBillingService ownerBillingService;

    /**
     * GAP-1015: resolve the subscription's owning instance and verify it belongs to the
     * caller's tenant (gateway-trusted {@code X-Tenant-Id}). Platform admins bypass.
     * Used by the {@code /{id}} lifecycle endpoints which only receive the subscription id.
     */
    private void requireOwnedSubscription(UUID subscriptionId, String tenantHeader) {
        SubscriptionResponse existing = subscriptionService.getSubscription(subscriptionId);
        TenantOwnershipGuard.requireOwnership(existing.getInstanceId(), tenantHeader);
    }

    /**
     * Create a new subscription.
     *
     * @param request Create subscription request
     * @return Created subscription response
     */
    @PostMapping
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<SubscriptionResponse> createSubscription(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        // GAP-1015: bind the create-request instanceId to the caller's tenant — an OWNER
        // must not create a subscription for an instance they do not own.
        TenantOwnershipGuard.requireOwnership(request.getInstanceId(), tenantHeader);
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get subscription by ID.
     *
     * @param id Subscription UUID
     * @return Subscription response
     */
    @GetMapping("/{id}")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<SubscriptionResponse> getSubscription(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id
    ) {
        SubscriptionResponse response = subscriptionService.getSubscription(id);
        // GAP-1015: deny before returning when the subscription belongs to another tenant.
        TenantOwnershipGuard.requireOwnership(response.getInstanceId(), tenantHeader);
        return ResponseEntity.ok(response);
    }

    /**
     * Get active subscription for instance.
     *
     * @param instanceId Instance UUID
     * @return Subscription response
     */
    @GetMapping("/instance/{instanceId}/active")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID instanceId
    ) {
        // GAP-1015: the instance path id IS the tenant scope — bind it to the caller.
        TenantOwnershipGuard.requireOwnership(instanceId, tenantHeader);
        SubscriptionResponse response = subscriptionService.getActiveSubscription(instanceId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all subscriptions for instance.
     *
     * @param instanceId Instance UUID
     * @return List of subscription responses
     */
    @GetMapping("/instance/{instanceId}")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByInstance(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID instanceId
    ) {
        // GAP-1015: the instance path id IS the tenant scope — bind it to the caller.
        TenantOwnershipGuard.requireOwnership(instanceId, tenantHeader);
        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsByInstance(instanceId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Upgrade subscription to higher tier.
     *
     * @param id Subscription UUID
     * @param request Tier change request
     * @return Updated subscription response
     */
    @PatchMapping("/{id}/upgrade")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<SubscriptionResponse> upgradeSubscription(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id,
        @Valid @RequestBody TierChangeRequest request
    ) {
        requireOwnedSubscription(id, tenantHeader);
        SubscriptionResponse response = subscriptionService.upgradeSubscription(id, request.getNewTier());
        return ResponseEntity.ok(response);
    }

    /**
     * Downgrade subscription to lower tier.
     *
     * @param id Subscription UUID
     * @param request Tier change request
     * @return Updated subscription response
     */
    @PatchMapping("/{id}/downgrade")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<SubscriptionResponse> downgradeSubscription(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id,
        @Valid @RequestBody TierChangeRequest request
    ) {
        requireOwnedSubscription(id, tenantHeader);
        SubscriptionResponse response = subscriptionService.downgradeSubscription(id, request.getNewTier());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel subscription.
     *
     * @param id Subscription UUID
     * @param immediate If true, cancel immediately. If false, cancel at end of cycle.
     * @return No content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<Void> cancelSubscription(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id,
        @RequestParam(defaultValue = "false") boolean immediate
    ) {
        requireOwnedSubscription(id, tenantHeader);
        subscriptionService.cancelSubscription(id, immediate);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually renew subscription.
     * Creates new billing cycle and reactivates suspended instance if needed.
     *
     * @param id Subscription UUID
     * @return No content
     */
    @PostMapping("/{id}/renew")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<Void> renewSubscription(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID id
    ) {
        requireOwnedSubscription(id, tenantHeader);
        renewalService.manualRenewal(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get expiring subscriptions.
     * Returns subscriptions expiring within the next 30 days.
     *
     * @return List of expiring subscription responses
     */
    @GetMapping("/expiring")
    @PreAuthorize(ADMIN_ONLY_AUTHZ)
    public ResponseEntity<List<SubscriptionResponse>> getExpiringSubscriptions() {
        List<SubscriptionResponse> responses = subscriptionService.getExpiringSubscriptions();
        return ResponseEntity.ok(responses);
    }

    /**
     * Pending-payment status for the instance (GAP-1257-BE). FE polls this for the
     * "đang chờ xác nhận" screen while waiting for the platform admin to reconcile the
     * VietQR transfer (SUB-19).
     *
     * @param instanceId instance UUID
     * @return pending-payment status (hasPendingPayment=false when none is in flight)
     */
    @GetMapping("/instance/{instanceId}/pending-payment-status")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<PendingPaymentStatusResponse> getPendingPaymentStatus(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID instanceId
    ) {
        // GAP-1015: instance path id IS the tenant scope — bind it to the caller.
        TenantOwnershipGuard.requireOwnership(instanceId, tenantHeader);
        return ResponseEntity.ok(ownerBillingService.getPendingPaymentStatus(instanceId));
    }

    /**
     * Over-cap impact preview for a tier downgrade (GAP-1261). FE shows the owner what
     * entitlement caps shrink + which features are lost before they confirm the downgrade.
     *
     * @param instanceId instance UUID
     * @param targetTier the lower tier being considered
     * @return downgrade preview
     */
    @GetMapping("/instance/{instanceId}/downgrade-preview")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<DowngradePreviewResponse> getDowngradePreview(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID instanceId,
        @RequestParam PricingTier targetTier
    ) {
        TenantOwnershipGuard.requireOwnership(instanceId, tenantHeader);
        return ResponseEntity.ok(ownerBillingService.getDowngradePreview(instanceId, targetTier));
    }

    /**
     * Win-back reactivation for a SUSPENDED/cancelled instance (GAP-1263-BE). Idempotent;
     * fraud/admin tombstones (PURGED/DELETED) return 409 — contact support.
     *
     * @param instanceId instance UUID
     * @return reactivation outcome (PAYMENT_REQUIRED / ALREADY_ACTIVE / NO_SUBSCRIPTION)
     */
    @PostMapping("/instance/{instanceId}/reactivate")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<ReactivateResponse> reactivate(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID instanceId
    ) {
        TenantOwnershipGuard.requireOwnership(instanceId, tenantHeader);
        return ResponseEntity.ok(ownerBillingService.reactivate(instanceId));
    }
}
