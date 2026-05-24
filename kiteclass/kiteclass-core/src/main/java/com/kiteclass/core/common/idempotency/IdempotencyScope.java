package com.kiteclass.core.common.idempotency;

/**
 * Wave beta-readiness-2 Bucket A — Idempotency scope discriminator (GAP-730).
 *
 * <p>Lets the same client-supplied {@code Idempotency-Key} header value be
 * reused across disjoint domains without collision. Scope is bound to the
 * controller path the request hit (e.g. {@code POST /api/v1/enrollments}
 * → {@link #ENROLLMENT}).
 *
 * <p>Stored as {@code VARCHAR(32)} in {@code idempotency_keys.scope} per
 * V66 migration. Adding a new scope requires bumping the migration's
 * column comment and the {@code IdempotencyService} call sites.
 *
 * @since 3.1.0 (Wave beta-readiness-2 Bucket A)
 */
public enum IdempotencyScope {

    /**
     * Tenant signup / new user registration. Maps to
     * {@code POST /api/auth/register} (cross-module — kitehub-subscription).
     */
    SIGNUP,

    /**
     * Student-to-class enrollment. Maps to
     * {@code POST /api/v1/enrollments} (kiteclass-core EnrollmentController).
     */
    ENROLLMENT,

    /**
     * Beta access request (Wave 33 GAP-372 beta tenant invite). Maps to
     * {@code POST /api/v1/auth/request-beta-access} (cross-module).
     */
    BETA_REQUEST,

    /**
     * Parent payment (Wave 105 Bucket D precedent). Already covered by
     * {@code PaymentIdempotencyService} in the parent payment module —
     * this enum value is reserved so future migration can converge the
     * two tables into one.
     */
    PAYMENT
}
