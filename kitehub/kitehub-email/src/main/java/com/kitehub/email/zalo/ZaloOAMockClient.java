package com.kitehub.email.zalo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic mock implementation of {@link ZaloOAClient}.
 *
 * <p>Default {@link ZaloOAClient} bean when {@code zalo.provider=mock} OR the
 * property is absent ({@code matchIfMissing = true}) — so the platform boots
 * with a working scaffold even when no Zalo credentials are configured.
 * Real HTTP integration ships in Wave 12+ as {@code ZaloOAHttpClient} guarded
 * by {@code @ConditionalOnProperty(... havingValue = "live")}.</p>
 *
 * <p><strong>Determinism contract:</strong>
 * <ul>
 *   <li>{@link #sendMessage} returns {@link ZaloSendResult.Status#MOCK} with a
 *       monotonically-increasing message id of shape {@code mock-zalo-N} —
 *       reproducible for IT assertions.</li>
 *   <li>{@link #verifyAccount} returns {@code true} (mock OA is always
 *       "verified").</li>
 *   <li>{@link #getDeliveryStatus} returns
 *       {@link ZaloOAClient.DeliveryStatus#DELIVERED} for ids previously
 *       issued by {@link #sendMessage} in this JVM lifetime, and
 *       {@link ZaloOAClient.DeliveryStatus#UNKNOWN} for any other id.</li>
 * </ul>
 * Counter resets on each Spring context creation so isolated
 * {@code @SpringBootTest} slices are deterministic.</p>
 *
 * <p>Invocations are logged at INFO so dev sessions can confirm Zalo paths
 * are exercised (per agent-action-bias.md style: empirical evidence over
 * guesswork).</p>
 *
 * @since Wave local-doable-11 Bucket B (GAP-063 Phase 1 scaffold)
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "zalo",
    name = "provider",
    havingValue = "mock",
    matchIfMissing = true
)
public class ZaloOAMockClient implements ZaloOAClient {

    private final AtomicLong counter = new AtomicLong(0);

    /**
     * Highest issued id in this JVM lifetime. Any id with a numeric suffix
     * ≤ this counter is considered "previously sent" by
     * {@link #getDeliveryStatus}.
     */
    private long highestIssued = 0L;

    @Override
    public ZaloSendResult sendMessage(String recipientUserId, ZaloMessage message) {
        if (recipientUserId == null || recipientUserId.isBlank()) {
            throw new IllegalArgumentException("recipientUserId must not be null or blank");
        }
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        long seq = counter.incrementAndGet();
        highestIssued = seq;
        String mockId = "mock-zalo-" + seq;

        log.info(
            "[ZaloOAMockClient] sendMessage recipient={} templateId={} locale={} bodyLen={} → mockId={}",
            recipientUserId,
            message.getTemplateId(),
            message.getLocale(),
            message.getBody() == null ? 0 : message.getBody().length(),
            mockId
        );

        return ZaloSendResult.builder()
            .providerMessageId(mockId)
            .status(ZaloSendResult.Status.MOCK)
            .sentAt(Instant.now())
            .build();
    }

    @Override
    public boolean verifyAccount() {
        log.info("[ZaloOAMockClient] verifyAccount → true (mock)");
        return true;
    }

    @Override
    public DeliveryStatus getDeliveryStatus(String providerMessageId) {
        if (providerMessageId == null || providerMessageId.isBlank()) {
            throw new IllegalArgumentException("providerMessageId must not be null or blank");
        }
        // Recognise ids previously issued by sendMessage in this JVM
        if (providerMessageId.startsWith("mock-zalo-")) {
            try {
                long seq = Long.parseLong(providerMessageId.substring("mock-zalo-".length()));
                if (seq > 0 && seq <= highestIssued) {
                    return DeliveryStatus.DELIVERED;
                }
            } catch (NumberFormatException ignored) {
                // fall through to UNKNOWN
            }
        }
        return DeliveryStatus.UNKNOWN;
    }
}
