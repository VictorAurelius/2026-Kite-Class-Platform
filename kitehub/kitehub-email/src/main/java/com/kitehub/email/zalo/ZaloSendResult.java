package com.kitehub.email.zalo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result envelope for {@link ZaloOAClient#sendMessage}.
 *
 * <p>Channel-agnostic shape mirroring
 * {@link com.kitehub.email.api.NotificationSendResult} so a future
 * {@code ZaloNotificationChannel} adapter can lift this into the platform-wide
 * {@code NotificationChannel} surface without translation churn.</p>
 *
 * <p>{@link Status#MOCK} indicates the mock client produced the result — useful
 * for tests asserting that the mock path was exercised rather than a live HTTP
 * call.</p>
 *
 * @since Wave local-doable-11 Bucket B (GAP-063 Phase 1 scaffold)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZaloSendResult {

    public enum Status {
        /** Live API returned success. */
        SENT,
        /** Mock client returned canned success — never executes HTTP. */
        MOCK,
        /** Live API returned non-success status code or threw. */
        FAILED,
        /** Provider disabled by config (e.g. {@code zalo.provider=disabled}). */
        SKIPPED
    }

    /**
     * Zalo OA message id (live) or a deterministic mock id (mock). Never
     * {@code null} on {@link Status#SENT} / {@link Status#MOCK}.
     */
    private String providerMessageId;

    private Status status;

    /** UTC instant of the send attempt. */
    private Instant sentAt;

    /** Error message — non-null only when {@link Status#FAILED}. */
    private String errorMessage;
}
