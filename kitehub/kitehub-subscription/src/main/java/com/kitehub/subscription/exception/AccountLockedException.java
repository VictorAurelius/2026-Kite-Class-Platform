package com.kitehub.subscription.exception;

import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Thrown when a user attempts to log in to a locked account (GAP-515 / OWASP A07).
 *
 * <p>Maps to HTTP 423 Locked + a {@code Retry-After} header (seconds until
 * {@link #lockedUntil}). The exception is intentionally distinct from
 * {@link IllegalArgumentException} (invalid credentials) so that the caller
 * cannot probe lockout state by spamming wrong passwords on already-locked
 * accounts — the server returns 423 immediately without touching the password
 * compare path.</p>
 *
 * @since 1.0.0 (Wave 72a GAP-515)
 */
@Getter
public class AccountLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("Account is locked until " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    /**
     * @return seconds remaining until the lock expires (rounded up, min 1)
     */
    public long retryAfterSeconds() {
        long secs = Duration.between(LocalDateTime.now(), lockedUntil).getSeconds();
        return Math.max(1, secs);
    }
}
