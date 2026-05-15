package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.dto.TierChangeRequest;
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

    private final SubscriptionService subscriptionService;
    private final SubscriptionRenewalService renewalService;

    /**
     * Create a new subscription.
     *
     * @param request Create subscription request
     * @return Created subscription response
     */
    @PostMapping
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<SubscriptionResponse> createSubscription(
        @Valid @RequestBody CreateSubscriptionRequest request
    ) {
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
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable UUID id) {
        SubscriptionResponse response = subscriptionService.getSubscription(id);
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
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(@PathVariable UUID instanceId) {
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
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByInstance(@PathVariable UUID instanceId) {
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
        @PathVariable UUID id,
        @Valid @RequestBody TierChangeRequest request
    ) {
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
        @PathVariable UUID id,
        @Valid @RequestBody TierChangeRequest request
    ) {
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
        @PathVariable UUID id,
        @RequestParam(defaultValue = "false") boolean immediate
    ) {
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
    public ResponseEntity<Void> renewSubscription(@PathVariable UUID id) {
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
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<List<SubscriptionResponse>> getExpiringSubscriptions() {
        List<SubscriptionResponse> responses = subscriptionService.getExpiringSubscriptions();
        return ResponseEntity.ok(responses);
    }
}
