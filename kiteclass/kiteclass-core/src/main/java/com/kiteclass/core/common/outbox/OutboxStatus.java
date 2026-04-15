package com.kiteclass.core.common.outbox;

/**
 * Outbox event delivery state.
 *
 * <p>State machine: PENDING → PUBLISHED (happy path) or PENDING → FAILED (after
 * max retries). Once PUBLISHED or FAILED, rows are terminal (publisher skips).
 *
 * @since 3.17.0 (Wave 3 Sub-PR 3.1, ADR-007)
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
