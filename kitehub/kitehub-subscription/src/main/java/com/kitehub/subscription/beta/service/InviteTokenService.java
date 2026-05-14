package com.kitehub.subscription.beta.service;

import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.exception.InviteTokenAlreadyUsedException;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Invite token single-use enforcement (GAP-534 Wave 77 Bucket D).
 *
 * <p>Wave 77 outside-in failure-mode matrix F1 (P0): invite email forwarded
 * → 2nd recipient validates token → beta slot hijack. This service enforces
 * single-use semantics on the {@code beta_access_request.invite_token} +
 * {@code claim_code} surface by performing an atomic
 * {@code UPDATE ... SET used_at = NOW() WHERE used_at IS NULL} against the
 * row. The {@code UPDATE} returns 1 on first success and 0 on every
 * subsequent attempt, eliminating the race window that a SELECT-then-UPDATE
 * pattern would leave open.</p>
 *
 * <p>The audit log entry on reuse-attempt records the originating IP +
 * user-agent so admin can investigate post-incident (link forwarded vs
 * link-leaked-to-attacker). Prometheus counter
 * {@code kitehub_invite_token_reuse_attempts_total} surfaces the rate to
 * alerting.</p>
 *
 * <p>Compatibility note: pre-existing {@code BetaAccessService} flows that
 * called {@code validateToken} / {@code exchangeClaimCode} continue to work
 * unchanged — those paths inspect the lifecycle ({@code APPROVED} +
 * non-expired + not-{@code SIGNED_UP}) but do NOT consume the token. The
 * actual signup-completion call now routes through
 * {@link #validateAndConsume(UUID, String, String)} which adds the
 * single-use gate on top of the existing lifecycle checks.</p>
 *
 * @since Wave 77 — GAP-534
 */
@Service
@Slf4j
public class InviteTokenService {

    static final String METRIC_REUSE_ATTEMPTS = "kitehub_invite_token_reuse_attempts_total";

    /** User-facing error message (Vietnamese per dev-readable-doc-language.md). */
    private static final String USER_FACING_MESSAGE =
            "Link đã được sử dụng. Vui lòng liên hệ support.";

    private final BetaAccessRequestRepository repository;
    private final MeterRegistry meterRegistry;

    public InviteTokenService(BetaAccessRequestRepository repository,
                              MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Atomically validate + consume an invite token.
     *
     * <p>Behavior matrix:
     * <ul>
     *   <li>Token not found / not APPROVED / expired → throws
     *       {@link IllegalStateException} (controller maps to 404/410)</li>
     *   <li>Token already consumed ({@code used_at} non-null) → increments
     *       reuse-attempt counter + WARN logs IP/UA + throws
     *       {@link InviteTokenAlreadyUsedException} (controller maps to 409)</li>
     *   <li>Token valid + first use → atomically sets {@code used_at},
     *       {@code consumed_ip}, {@code consumed_user_agent}; returns the
     *       refreshed entity so caller can proceed with signup flow</li>
     * </ul></p>
     *
     * <p>The atomic UPDATE is performed via repository custom-query rather
     * than read-modify-write to eliminate the race window between two
     * concurrent SELECT calls.</p>
     *
     * @param token the invite-token UUID supplied by signup form
     * @param ipAddress originating IP (X-Forwarded-For resolved by gateway), may be null in tests
     * @param userAgent UA string from request header, may be null
     * @return the consumed row, refreshed with used_at + audit columns populated
     * @throws InviteTokenAlreadyUsedException if used_at is already set (single-use violation)
     * @throws IllegalStateException if token lifecycle invalid (not found / expired / wrong status)
     */
    @Transactional
    public BetaAccessRequest validateAndConsume(UUID token, String ipAddress, String userAgent) {
        Optional<BetaAccessRequest> opt = repository.findByInviteToken(token);
        if (opt.isEmpty()) {
            throw new IllegalStateException("Invite token not found");
        }

        BetaAccessRequest entity = opt.get();

        // Lifecycle gates — fail-fast before attempting the consume UPDATE so
        // we don't produce a successful UPDATE on a non-APPROVED row.
        if (entity.getStatus() != BetaAccessRequestStatus.APPROVED) {
            throw new IllegalStateException("Invite token not in APPROVED state");
        }
        if (entity.isTokenExpired()) {
            throw new IllegalStateException("Invite token expired");
        }

        // Atomic single-use enforcement. Repository performs:
        //   UPDATE beta_access_request
        //   SET used_at = NOW(), consumed_ip = ?, consumed_user_agent = ?
        //   WHERE invite_token = ? AND used_at IS NULL
        // row-count == 1 → first consume; row-count == 0 → reuse attempt.
        int rowsUpdated = repository.consumeInviteToken(
                token,
                OffsetDateTime.now(),
                truncate(ipAddress, 45),
                truncate(userAgent, 512));

        if (rowsUpdated == 0) {
            // Reuse attempt — audit + counter + 409.
            reuseAttemptCounter().increment();
            log.warn("invite_token_reuse_attempt token={} previous_used_at={} attempt_ip={} attempt_user_agent={}",
                    hashForLog(token.toString()),
                    entity.getUsedAt(),
                    ipAddress == null ? "<unknown>" : ipAddress,
                    truncate(userAgent, 200));
            throw new InviteTokenAlreadyUsedException(token.toString(), USER_FACING_MESSAGE);
        }

        // Refresh entity so caller observes the new used_at / consumed_* columns.
        return repository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalStateException(
                        "Invite token disappeared between consume + refresh — concurrency bug"));
    }

    /**
     * Counter for reuse-attempt alerting. Lazy-created via Micrometer.
     */
    private Counter reuseAttemptCounter() {
        return Counter.builder(METRIC_REUSE_ATTEMPTS)
                .description("Total invite-token reuse attempts blocked by single-use enforcement (GAP-534)")
                .register(meterRegistry);
    }

    /**
     * Hashed token id for audit log (avoid logging raw UUID even though it's
     * already consumed — defense in depth against log scraping).
     */
    static String hashForLog(String token) {
        if (token == null) {
            return "<null>";
        }
        int hash = token.hashCode();
        return "tk_" + Integer.toHexString(hash);
    }

    static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
