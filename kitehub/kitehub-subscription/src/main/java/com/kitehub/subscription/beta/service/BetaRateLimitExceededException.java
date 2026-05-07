package com.kitehub.subscription.beta.service;

/**
 * Thrown by {@code BetaAccessService.submitRequest} when the per-email 24h
 * rate limit (GAP-388 Wave 36 Bucket A 388-C) trips. Mapped to HTTP 429 by
 * {@code BetaAccessController.handleRateLimit}.
 *
 * @since Wave 36 — GAP-388
 */
public class BetaRateLimitExceededException extends RuntimeException {
    public BetaRateLimitExceededException(String message) {
        super(message);
    }
}
