package com.kitehub.subscription.beta.exception;

/**
 * Thrown when a beta invite token / claim code is consumed more than once.
 *
 * <p>GAP-534 Wave 77 Bucket D — single-use enforcement. The service layer
 * runs an atomic {@code UPDATE ... SET used_at = NOW() WHERE used_at IS NULL}
 * on the {@code beta_access_request} row. Row-count == 0 means the row was
 * already consumed; the service throws this exception, the controller maps
 * it to HTTP 409 Conflict, and the audit log records the reuse attempt with
 * originating IP / user-agent for forensic triage.</p>
 *
 * <p>Surface: the user-facing message is intentionally generic ("Link đã
 * được sử dụng. Vui lòng liên hệ support."). Forensic detail goes to the
 * structured audit log, not the response body.</p>
 *
 * @since Wave 77 — GAP-534
 */
public class InviteTokenAlreadyUsedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Identifier of the token whose reuse was attempted (UUID string or claim-code). */
    private final String tokenIdentifier;

    public InviteTokenAlreadyUsedException(String tokenIdentifier, String message) {
        super(message);
        this.tokenIdentifier = tokenIdentifier;
    }

    public String getTokenIdentifier() {
        return tokenIdentifier;
    }
}
