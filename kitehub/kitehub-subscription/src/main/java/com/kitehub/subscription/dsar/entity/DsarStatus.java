package com.kitehub.subscription.dsar.entity;

/**
 * DSAR ticket lifecycle status.
 *
 * <pre>
 *   PENDING --(DPO triage)--> IN_REVIEW --(DPO closes)--> COMPLETED
 *                                                  |
 *                                                  +---> REJECTED (identity fail / out-of-scope)
 * </pre>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
public enum DsarStatus {
    PENDING,
    IN_REVIEW,
    COMPLETED,
    REJECTED
}
