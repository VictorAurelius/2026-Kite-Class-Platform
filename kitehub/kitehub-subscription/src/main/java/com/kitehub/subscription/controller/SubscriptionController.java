package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.CreateSubscriptionRequest;
import com.kitehub.subscription.dto.SubscriptionResponse;
import com.kitehub.subscription.dto.TierChangeRequest;
import com.kitehub.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for subscription management.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/platform/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Create a new subscription.
     *
     * @param request Create subscription request
     * @return Created subscription response
     */
    @PostMapping
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
    public ResponseEntity<Void> cancelSubscription(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "false") boolean immediate
    ) {
        subscriptionService.cancelSubscription(id, immediate);
        return ResponseEntity.noContent().build();
    }
}
