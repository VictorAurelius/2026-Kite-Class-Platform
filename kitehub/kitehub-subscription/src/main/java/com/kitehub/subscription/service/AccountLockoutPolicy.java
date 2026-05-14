package com.kitehub.subscription.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Account-lockout policy constants + lockout-duration calculator (GAP-515 / OWASP A07).
 *
 * <p>Triggers a lockout after {@link #MAX_FAILED_ATTEMPTS} failed logins within
 * {@link #ATTEMPT_WINDOW_MINUTES}. Uses exponential backoff based on the user's
 * {@code lockoutCount} so repeat-offenders escalate quickly.</p>
 *
 * <p>Values are intentionally hardcoded constants (not @Value config) for v1 —
 * the cost of mistakenly relaxing these via env var outweighs flex benefits.
 * If tuning becomes necessary, promote to {@code application.yml} keys
 * {@code kitehub.auth.lockout.*} in a follow-up gap.</p>
 *
 * @since 1.0.0 (Wave 72a GAP-515)
 */
public final class AccountLockoutPolicy {

    /** Failed login attempts within {@link #ATTEMPT_WINDOW_MINUTES} that trigger a lock. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /** Rolling window over which failed attempts accumulate before the counter resets. */
    public static final int ATTEMPT_WINDOW_MINUTES = 15;

    private AccountLockoutPolicy() {}

    /**
     * Compute the lockout expiry timestamp for the given lockout-count history.
     *
     * <p>Schedule (per Wave 72a §1 GAP-515):
     * <ul>
     *   <li>1st lockout (lockoutCount=0 → becomes 1) → 15 minutes</li>
     *   <li>2nd lockout (lockoutCount=1 → becomes 2) → 1 hour</li>
     *   <li>3rd lockout (lockoutCount=2 → becomes 3) → 24 hours</li>
     *   <li>4th+ lockout → 24 hours (capped — escalation to MFA enrollment is
     *       handled by GAP-517 follow-up, not by extending the timer)</li>
     * </ul>
     *
     * @param priorLockoutCount value of {@code users.lockout_count} BEFORE this lockout
     * @return absolute lockedUntil timestamp (UTC, server local)
     */
    public static LocalDateTime computeLockedUntil(int priorLockoutCount) {
        Duration duration = switch (priorLockoutCount) {
            case 0 -> Duration.ofMinutes(15);
            case 1 -> Duration.ofHours(1);
            default -> Duration.ofHours(24);
        };
        return LocalDateTime.now().plus(duration);
    }
}
