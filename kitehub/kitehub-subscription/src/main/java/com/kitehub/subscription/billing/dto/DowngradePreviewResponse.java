package com.kitehub.subscription.billing.dto;

import com.kitehub.platform.domain.enums.PricingTier;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Over-cap impact preview for a tier downgrade (GAP-1261).
 * FE consumes {@code GET /api/platform/subscriptions/instance/{id}/downgrade-preview?targetTier=BASIC}
 * to warn the owner what entitlement caps shrink + what features are lost before they confirm.
 *
 * <p><strong>Phase 1 BETA data-source note:</strong> live usage counters (students enrolled,
 * storage consumed) live in the per-tenant kiteclass-core database, NOT in kitehub-subscription.
 * This preview therefore compares the <em>entitlement caps</em> of the current vs target tier
 * (from {@link PricingTier}) and flags the real custom-domain loss (read from
 * {@code instances.custom_domain}). The owner compares the shrunk caps against their own
 * known usage. {@code usageDataNote} documents this for the FE.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Data
@Builder
public class DowngradePreviewResponse {

    private PricingTier currentTier;
    private PricingTier targetTier;

    private int currentMaxStudents;
    private int targetMaxStudents;

    private int currentMaxTeachers;
    private int targetMaxTeachers;

    private int currentStorageMb;
    private int targetStorageMb;

    /** Current tier permits a custom domain (PREMIUM/ENTERPRISE). */
    private boolean customDomainCurrentlyAllowed;
    /** Target tier permits a custom domain. */
    private boolean customDomainTargetAllowed;
    /** Downgrade disables the custom-domain entitlement (allowed → not allowed). */
    private boolean customDomainWillBeDisabled;
    /** The instance actually has a custom domain configured that would stop working. */
    private boolean hasActiveCustomDomain;

    /** Human-readable Vietnamese warnings for the confirm dialog. */
    private List<String> warnings;

    /** Disclaimer about the entitlement-cap (not live-usage) data source. */
    private String usageDataNote;
}
