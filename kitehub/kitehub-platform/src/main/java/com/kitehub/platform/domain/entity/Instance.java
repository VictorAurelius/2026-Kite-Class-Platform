package com.kitehub.platform.domain.entity;

import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Instance entity representing a KiteClass instance.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Getter
@Setter
@Entity
@Table(name = "instances", indexes = {
    @Index(name = "idx_instances_subdomain", columnList = "subdomain"),
    @Index(name = "idx_instances_owner", columnList = "owner_id"),
    @Index(name = "idx_instances_status", columnList = "status")
})
public class Instance extends BaseEntity {

    /**
     * Subdomain for this instance (e.g., "customer1" for customer1.kiteclass.com).
     */
    @NotBlank(message = "Subdomain is required")
    @Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain must contain only lowercase letters, numbers, and hyphens")
    @Column(name = "subdomain", length = 50, unique = true, nullable = false)
    private String subdomain;

    /**
     * Custom domain (only for PREMIUM and ENTERPRISE tiers).
     */
    @Size(max = 255, message = "Custom domain must not exceed 255 characters")
    @Column(name = "custom_domain", length = 255)
    private String customDomain;

    /**
     * Organization name.
     */
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 200, message = "Organization name must be between 2 and 200 characters")
    @Column(name = "organization_name", length = 200, nullable = false)
    private String organizationName;

    /**
     * Owner ID (CENTER_OWNER user UUID).
     */
    @NotNull(message = "Owner ID is required")
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /**
     * Pricing tier.
     */
    @NotNull(message = "Tier is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 20, nullable = false)
    private PricingTier tier;

    /**
     * Instance status.
     */
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InstanceStatus status;

    /**
     * Database URL for this instance.
     */
    @NotBlank(message = "Database URL is required")
    @Size(max = 500, message = "Database URL must not exceed 500 characters")
    @Column(name = "database_url", length = 500, nullable = false)
    private String databaseUrl;

    /**
     * Database username.
     */
    @NotBlank(message = "Database username is required")
    @Size(max = 100, message = "Database username must not exceed 100 characters")
    @Column(name = "database_username", length = 100, nullable = false)
    private String databaseUsername;

    /**
     * Encrypted database password.
     */
    @NotBlank(message = "Database password is required")
    @Size(max = 255, message = "Database password must not exceed 255 characters")
    @Column(name = "database_password", length = 255, nullable = false)
    private String databasePassword;

    /**
     * Trial start date.
     */
    @Column(name = "trial_started_at")
    private LocalDateTime trialStartedAt;

    /**
     * Trial expiration date (14 days from start).
     */
    @Column(name = "trial_expires_at")
    private LocalDateTime trialExpiresAt;

    /**
     * Subscription ID (reference to Subscription entity - PR 4.4).
     */
    @Column(name = "subscription_id")
    private UUID subscriptionId;

    /**
     * Subscription expiration date.
     */
    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    /**
     * Check if this instance is on trial.
     *
     * @return true if status is TRIAL and not expired, false otherwise
     */
    public boolean isOnTrial() {
        return status == InstanceStatus.TRIAL
            && trialExpiresAt != null
            && LocalDateTime.now().isBefore(trialExpiresAt);
    }

    /**
     * Get trial days left.
     *
     * @return days left in trial period, or 0 if not on trial or expired
     */
    public long getTrialDaysLeft() {
        // Check status and expiry date
        if (status != InstanceStatus.TRIAL || trialExpiresAt == null) {
            return 0;
        }

        // Use date comparison (not datetime) to avoid timing precision issues
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = trialExpiresAt.toLocalDate();

        long days = ChronoUnit.DAYS.between(today, expiryDate);
        return Math.max(0, days);
    }

    /**
     * Check if this instance is active.
     *
     * @return true if status is ACTIVE or on valid trial, false otherwise
     */
    public boolean isActive() {
        if (status == InstanceStatus.ACTIVE) {
            return subscriptionExpiresAt == null || LocalDateTime.now().isBefore(subscriptionExpiresAt);
        }
        return isOnTrial();
    }

    /**
     * Start trial period (14 days).
     */
    public void startTrial() {
        this.status = InstanceStatus.TRIAL;
        this.trialStartedAt = LocalDateTime.now();
        this.trialExpiresAt = LocalDateTime.now().plusDays(14);
    }

    /**
     * Activate paid subscription.
     *
     * @param expiresAt subscription expiration date
     */
    public void activateSubscription(LocalDateTime expiresAt) {
        this.status = InstanceStatus.ACTIVE;
        this.subscriptionExpiresAt = expiresAt;
    }

    /**
     * Suspend instance (trial expired or subscription lapsed).
     */
    public void suspend() {
        this.status = InstanceStatus.SUSPENDED;
    }

    /**
     * Check if custom domain is allowed for this tier.
     *
     * @return true if custom domain allowed, false otherwise
     */
    public boolean canUseCustomDomain() {
        return tier.allowsCustomDomain();
    }
}
