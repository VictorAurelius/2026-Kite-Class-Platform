package com.kitehub.platform.domain.entity;

import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.VerticalType;
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
     * Subdomain for this instance (e.g., "customer1" for customer1.kitehub.me).
     */
    @NotBlank(message = "Subdomain is required")
    @Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain must contain only lowercase letters, numbers, and hyphens")
    @Column(name = "subdomain", length = 50, unique = true, nullable = false)
    private String subdomain;

    /**
     * Normalized URL/routing slug (GAP-535 Wave 77 + GAP-823 Wave local-doable-9).
     *
     * <p>Pipeline: NFC normalize → strip smart quotes (U+2018-U+201D) → Apache Commons
     * stripAccents (flatten VN diacritics) → lowercase → non-alphanumeric → '-' →
     * trim/collapse. See {@code TenantSlugNormalizer} for canonical normalization.</p>
     *
     * <p>Collision recovery is service-side: append {@code -1}/{@code -2}/... suffix
     * until {@link InstanceRepository#existsBySlugStartingWith(String)} returns false,
     * capped at 10 attempts (then 409 IllegalStateException).</p>
     *
     * <p>Mapped via V40 migration; NULL allowed for grandfathered rows pre-Wave-77
     * (those rows retain only subdomain). Unique partial index enforces uniqueness
     * only when set.</p>
     *
     * @since Wave 77 Bucket D (column) / Wave local-doable-9 Bucket B (entity field + wiring)
     */
    @Size(max = 120, message = "Slug must not exceed 120 characters")
    @Column(name = "slug", length = 120, unique = true)
    private String slug;

    /**
     * Domain status enum for custom domain verification lifecycle.
     *
     * <ul>
     *   <li>NONE: no custom domain set.</li>
     *   <li>PENDING_VERIFY: domain set, waiting for DNS TXT record verification.</li>
     *   <li>CERT_PROVISIONING: DNS TXT verified, SSL cert provisioning in progress
     *       (via Cloudflare for SaaS / ACM — added v1.1 per GAP-812 §Phần B + C).</li>
     *   <li>VERIFIED: DNS TXT confirmed AND cert provisioned, domain live.</li>
     *   <li>FAILED: verification failed (wrong record or timeout).</li>
     * </ul>
     *
     * <p>Transitions: NONE → PENDING_VERIFY → CERT_PROVISIONING → VERIFIED.
     * FAILED reachable from any non-terminal state; re-verify resets FAILED → PENDING_VERIFY.</p>
     */
    public enum DomainStatus {
        NONE, PENDING_VERIFY, CERT_PROVISIONING, VERIFIED, FAILED
    }

    /**
     * Custom domain (only for PREMIUM and ENTERPRISE tiers).
     */
    @Size(max = 255, message = "Custom domain must not exceed 255 characters")
    @Column(name = "custom_domain", length = 255)
    private String customDomain;

    /**
     * DNS verification token (format: kitehub-verify={uuid}).
     * Customer must add this as a TXT record to prove domain ownership.
     */
    @Size(max = 255, message = "Domain verify token must not exceed 255 characters")
    @Column(name = "domain_verify_token", length = 255)
    private String domainVerifyToken;

    /**
     * Timestamp when domain was successfully verified.
     */
    @Column(name = "domain_verified_at")
    private LocalDateTime domainVerifiedAt;

    /**
     * Current status of custom domain verification.
     * Default: NONE (no custom domain configured).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "domain_status", length = 50)
    private DomainStatus domainStatus = DomainStatus.NONE;

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
     * Contact email for instance owner.
     * Used for notifications (trial expiration, payment reminders, etc.).
     */
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

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
     * User preference: receive email notifications về instance activity.
     * Default true (preserves current behavior for existing users).
     *
     * @since GAP-098
     */
    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = true;

    /**
     * User preference: receive trial expiration reminder emails.
     * Default true.
     *
     * @since GAP-098
     */
    @Column(name = "trial_reminders", nullable = false)
    private boolean trialReminders = true;

    /**
     * In-flight trial-to-paid migration phase. NONE when no migration is running.
     *
     * <p>See rules.md §3 (trial-to-paid-migration) for the state machine. While
     * {@code migrationPhase != NONE && != COMPLETED} the migration is in flight;
     * user reads stay available (zero-downtime SLA T2P-02).</p>
     *
     * @since GAP-192
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "migration_phase", length = 32, nullable = false)
    private MigrationPhase migrationPhase = MigrationPhase.NONE;

    /**
     * Timestamp when current migration transitioned to INITIATED.
     * Null when no migration ever started or when last migration fully reset to NONE.
     *
     * @since GAP-192
     */
    @Column(name = "migration_started_at")
    private LocalDateTime migrationStartedAt;

    /**
     * Timestamp when migration reached COMPLETED (status flip from TRIAL to ACTIVE).
     * Used as the anchor for the 24h reversal window (T2P-04).
     *
     * @since GAP-192
     */
    @Column(name = "migration_completed_at")
    private LocalDateTime migrationCompletedAt;

    /**
     * Free-form reason for the most recent MIGRATION_FAILED or REVERSED transition.
     *
     * @since GAP-192
     */
    @Size(max = 500)
    @Column(name = "migration_failure_reason", length = 500)
    private String migrationFailureReason;

    /**
     * Operating-model discriminator (CENTER vs K12_SCHOOL).
     *
     * <p>CENTER (default) preserves legacy behaviour: per-day attendance,
     * single-subject grading. K12_SCHOOL switches kiteclass-core to
     * per-period attendance and multi-subject gradebook per TT 22/2021 +
     * TT 32/2018. Phase 1A (Wave 18b1, GAP-323) enforces the K-12 contract
     * in the service layer; Phase 1B will add per-table CHECK constraints.
     *
     * @since GAP-323 Phase 1A (Wave 18b1)
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vertical_type", length = 20, nullable = false)
    private VerticalType verticalType = VerticalType.CENTER;

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
     * Start trial period with configurable duration.
     *
     * @param durationDays number of days for the trial period
     */
    public void startTrial(int durationDays) {
        this.status = InstanceStatus.TRIAL;
        this.trialStartedAt = LocalDateTime.now();
        this.trialExpiresAt = LocalDateTime.now().plusDays(durationDays);
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
