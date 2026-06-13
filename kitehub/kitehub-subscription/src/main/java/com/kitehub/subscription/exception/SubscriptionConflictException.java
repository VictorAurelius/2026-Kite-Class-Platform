package com.kitehub.subscription.exception;

/**
 * Raised when a subscription mutation conflicts with an in-flight state — e.g. a
 * create request for a different tier while a PENDING subscription already exists
 * for the same instance (GAP-1080 idempotency guard).
 *
 * <p>Mapped to HTTP 409 Conflict by {@code GlobalExceptionHandler}; distinct from
 * {@code IllegalArgumentException} (400) which is for malformed/invalid input.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public class SubscriptionConflictException extends RuntimeException {

    public SubscriptionConflictException(String message) {
        super(message);
    }
}
