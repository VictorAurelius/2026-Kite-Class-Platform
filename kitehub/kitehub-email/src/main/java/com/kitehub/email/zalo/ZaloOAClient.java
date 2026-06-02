package com.kitehub.email.zalo;

/**
 * Strategy Pattern — Zalo Official Account (OA) outbound messaging contract.
 *
 * <p>Provider-agnostic surface that the future {@code ZaloNotificationChannel}
 * (per {@link com.kitehub.email.api.NotificationChannel}) will adapt into the
 * platform-wide notification dispatch. Direct calls to the Zalo OA REST API
 * are BANNED outside an implementation of this interface — same isolation rule
 * as {@code AIClient} for AI providers and {@code EmailSender} for email
 * providers (per design-patterns.md §2 + §3.4 Direct External API Coupling).</p>
 *
 * <p><strong>Implementations:</strong>
 * <ul>
 *   <li>{@link ZaloOAMockClient} (Wave local-doable-11 Bucket B — GAP-063 Phase 1)
 *       — deterministic canned responses; default when
 *       {@code zalo.provider=mock} or property absent.</li>
 *   <li>{@code ZaloOAHttpClient} (Wave 12+ follow-up) — real OAuth2 +
 *       {@code openapi.zalo.me} HTTP integration; requires verified Zalo OA
 *       business account.</li>
 * </ul>
 *
 * <p><strong>Phase 1 scaffold scope (Wave local-doable-11 Bucket B):</strong>
 * interface + DTO + mock impl + config + IT. Live verification against real
 * Zalo OA is intentionally deferred to Wave 12+ (requires Zalo business
 * account verification process documented in
 * {@code documents/05-guides/operations/zalo-oa-setup-runbook.md} —
 * Bucket C deliverable).</p>
 *
 * @since Wave local-doable-11 Bucket B (GAP-063 Phase 1 scaffold)
 */
public interface ZaloOAClient {

    /**
     * Send a message to a single Zalo OA follower.
     *
     * @param recipientUserId Zalo user-id of the recipient (the platform
     *                        previously persisted via OA Follow webhook).
     *                        Must not be {@code null} or blank.
     * @param message         outbound message envelope. Must not be {@code null}.
     * @return result envelope — never {@code null}.
     */
    ZaloSendResult sendMessage(String recipientUserId, ZaloMessage message);

    /**
     * Verify the configured OA account credentials are usable. The live
     * implementation pings {@code /oa/profile} or equivalent; the mock returns
     * {@code true} unless explicitly disabled.
     *
     * @return {@code true} if the configured credentials authenticate
     *         successfully against the provider.
     */
    boolean verifyAccount();

    /**
     * Look up the delivery status of a previously-sent message.
     *
     * @param providerMessageId id returned from a prior {@link #sendMessage}
     *                          call. Must not be {@code null} or blank.
     * @return current delivery status — never {@code null}.
     */
    DeliveryStatus getDeliveryStatus(String providerMessageId);

    /**
     * Provider-agnostic delivery status. Maps live Zalo states + mock states
     * into a domain enum so callers (notification dispatcher, reporting UI)
     * never depend on the vendor's string codes.
     */
    enum DeliveryStatus {
        /** Message accepted by provider; downstream delivery in progress. */
        PENDING,
        /** Confirmed delivered to recipient device. */
        DELIVERED,
        /** Recipient opened / read (when provider reports read receipts). */
        READ,
        /** Permanent failure — recipient invalid, blocked, opted-out. */
        FAILED,
        /** Unknown id (never seen by provider or expired from store). */
        UNKNOWN
    }
}
