package com.kitehub.email.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of a {@link NotificationChannel#send} attempt.
 *
 * <p>Channel-agnostic envelope; specific provider IDs (SES message id, Zalo trace
 * id, SMS reference) live in {@link #providerMessageId}. Status enum keeps the
 * caller decoupled from the underlying provider's status string.</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSendResult {

    public enum Status {
        SENT,
        MOCK,
        FAILED,
        SKIPPED_DISABLED_IN_PHASE_1
    }

    /**
     * Provider's message id (SES, Twilio, Zalo trace) — null on FAILED.
     */
    private String providerMessageId;

    private Status status;

    /**
     * UTC timestamp of the send attempt.
     */
    private LocalDateTime sentAt;

    /**
     * Error message — null on success.
     */
    private String errorMessage;

    /**
     * Channel that produced this result.
     */
    private String channel;
}
