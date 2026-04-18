package com.kitehub.branding.queue;

/**
 * Priority level for AI jobs — maps a pricing tier to a dedicated queue and
 * weighted-round-robin slot.
 *
 * <p>Wave 3 Phase 1 (GAP-005a): 3-tier priority queues with weighted scheduling
 * (ENTERPRISE:PRO:FREE = 3:2:1). Horizontal scaling is deferred to Phase 2.</p>
 *
 * <h3>Mapping</h3>
 * <ul>
 *   <li>{@link #ENTERPRISE} — Enterprise subscription tier, dedicated capacity.</li>
 *   <li>{@link #PRO} — PREMIUM and BASIC tiers, shared pool with priority.</li>
 *   <li>{@link #FREE} — Free / Trial tier, degrades to template fallback on backpressure.</li>
 * </ul>
 *
 * @since 1.0
 */
public enum AIJobPriority {

    /**
     * Enterprise tier — dedicated workers, highest weight (3).
     */
    ENTERPRISE(3),

    /**
     * Pro tier (PREMIUM + BASIC) — shared pool, medium weight (2).
     */
    PRO(2),

    /**
     * Free tier — lowest weight (1), degrades to template fallback under backpressure.
     */
    FREE(1);

    private final int weight;

    AIJobPriority(int weight) {
        this.weight = weight;
    }

    /**
     * @return weighted-round-robin weight (ENTERPRISE=3, PRO=2, FREE=1)
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Map a pricing tier string to the corresponding priority.
     *
     * <p>Unknown / null tiers map to {@link #FREE} as the safe default.</p>
     *
     * @param tier subscription tier name (case-insensitive, may be null)
     * @return matching priority
     */
    public static AIJobPriority fromTier(String tier) {
        if (tier == null) {
            return FREE;
        }
        return switch (tier.trim().toUpperCase()) {
            case "ENTERPRISE" -> ENTERPRISE;
            case "PREMIUM", "BASIC", "PRO" -> PRO;
            case "FREE", "TRIAL" -> FREE;
            default -> FREE;
        };
    }
}
