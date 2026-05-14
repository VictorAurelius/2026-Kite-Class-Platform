package com.kitehub.subscription.idempotency.exception;

/**
 * Thrown when the same {@code Idempotency-Key} arrives with a different
 * request body than the cached invocation (Stripe semantics — 422).
 *
 * @since Wave 77 — GAP-536
 */
public class IdempotencyConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
