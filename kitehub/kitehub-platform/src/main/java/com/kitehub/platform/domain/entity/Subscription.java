package com.kitehub.platform.domain.entity;

import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription entity for managing paid subscriptions.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription extends BaseEntity {

    /**
     * Instance ID (FK to instances table).
     */
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    /**
     * Pricing tier.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private PricingTier tier;

    /**
     * Billing cycle (MONTHLY, ANNUALLY).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    /**
     * Subscription price in VND.
     */
    @Column(name = "price_vnd", nullable = false)
    private Long priceVnd;

    /**
     * Subscription status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    /**
     * Subscription start date.
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * Subscription expiration date.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Auto-renewal flag.
     */
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;

    /**
     * Pending tier for downgrade (applied at end of billing cycle).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pending_tier", length = 20)
    private PricingTier pendingTier;

    /**
     * Payment ID for pending tier upgrade (prorated payment).
     */
    @Column(name = "pending_payment_id")
    private UUID pendingPaymentId;

    /**
     * Optimistic-lock version (GAP-895). Guards auto-renew cron vs admin manual extend race.
     * Field-level @Version (not BaseEntity) so the column set matches V59 exactly — instances
     * table is out of Bucket C-KH scope and has no version column.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Check if subscription is active.
     *
     * @return true if active and not expired
     */
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE
            && expiresAt != null
            && LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Check if subscription has expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return status == SubscriptionStatus.EXPIRED
            || (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }

    /**
     * Cancel subscription (mark as cancelled).
     */
    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
    }

    /**
     * Expire subscription (mark as expired).
     */
    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    /**
     * Suspend subscription.
     */
    public void suspend() {
        this.status = SubscriptionStatus.SUSPENDED;
    }

    /**
     * Renew subscription for another billing cycle.
     *
     * @param newExpiresAt New expiration date
     */
    public void renew(LocalDateTime newExpiresAt) {
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = this.expiresAt; // Start from previous expiry
        this.expiresAt = newExpiresAt;
    }
}
