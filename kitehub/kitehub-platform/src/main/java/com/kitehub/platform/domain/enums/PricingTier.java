package com.kitehub.platform.domain.enums;

import lombok.Getter;

/**
 * Pricing tier enum with limits.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Getter
public enum PricingTier {
    /**
     * Free tier: 10 students, 1 teacher, 500MB storage.
     */
    FREE(10, 1, 500, 0L),

    /**
     * Basic tier: 50 students, 5 teachers, 2GB storage - 500k VNĐ/month.
     */
    BASIC(50, 5, 2048, 500_000L),

    /**
     * Premium tier: 200 students, 20 teachers, 10GB storage - 1.5M VNĐ/month.
     */
    PREMIUM(200, 20, 10240, 1_500_000L),

    /**
     * Enterprise tier: Unlimited (custom pricing).
     */
    ENTERPRISE(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0L);

    private final int maxStudents;
    private final int maxTeachers;
    private final int storageLimitMB;
    private final long priceVND; // Monthly price in VNĐ (0 = custom pricing)

    PricingTier(int maxStudents, int maxTeachers, int storageLimitMB, long priceVND) {
        this.maxStudents = maxStudents;
        this.maxTeachers = maxTeachers;
        this.storageLimitMB = storageLimitMB;
        this.priceVND = priceVND;
    }

    /**
     * Check if this tier allows custom domain.
     *
     * @return true if PREMIUM or ENTERPRISE, false otherwise
     */
    public boolean allowsCustomDomain() {
        return this == PREMIUM || this == ENTERPRISE;
    }

    /**
     * Get annual price with 10% discount.
     *
     * @return Annual price in VNĐ
     */
    public long getAnnualPrice() {
        if (this == FREE || this == ENTERPRISE) {
            return 0L; // Free or custom pricing
        }
        // Annual = Monthly * 12 - 10% discount
        return (long) (priceVND * 12 * 0.9);
    }

    /**
     * Get price based on billing cycle.
     *
     * @param billingCycle Billing cycle
     * @return Price in VNĐ
     */
    public long getPrice(com.kitehub.platform.domain.enums.BillingCycle billingCycle) {
        return billingCycle == com.kitehub.platform.domain.enums.BillingCycle.ANNUALLY
            ? getAnnualPrice()
            : priceVND;
    }
}
