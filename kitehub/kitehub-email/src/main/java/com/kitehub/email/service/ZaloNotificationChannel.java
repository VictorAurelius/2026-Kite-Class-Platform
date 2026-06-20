package com.kitehub.email.service;

import com.kitehub.email.api.NotificationChannel;
import com.kitehub.email.api.NotificationContext;
import com.kitehub.email.api.NotificationSendResult;
import com.kitehub.email.zalo.ZaloMessage;
import com.kitehub.email.zalo.ZaloOAClient;
import com.kitehub.email.zalo.ZaloOAConfig;
import com.kitehub.email.zalo.ZaloSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * ZALO channel adapter for the platform-wide {@link NotificationChannel} seam
 * (GAP-063 Phase 1 — Wave local-doable-11 Bucket B).
 *
 * <p>Bridges the channel-agnostic {@link NotificationChannel#send} surface (the same
 * contract {@link SESEmailService} implements for EMAIL) down to the provider-agnostic
 * {@link ZaloOAClient} strategy. In Phase 1 the only {@link ZaloOAClient} bean is
 * {@code ZaloOAMockClient} ({@code zalo.provider=mock}, default), so dispatch is
 * deterministic and performs NO real Zalo OA HTTP call — the slot is wired and ready
 * for the Phase 2 live ZNS client ({@code ZaloOAHttpClient}) to drop in behind the
 * same {@link ZaloOAClient} interface without touching this adapter.</p>
 *
 * <p>Phase 1 mock — Phase 2 live ZNS per GAP-063.</p>
 *
 * <p><strong>Bean wiring</strong> mirrors {@link EmailProviderRouter}'s null-safe
 * optional-backend pattern: the {@link ZaloOAClient} is injected via
 * {@link ObjectProvider} so a misconfigured {@code zalo.provider=live} (no live client
 * present yet in Phase 1) degrades to a {@code SKIPPED} result rather than a startup
 * failure. The channel itself is gated by {@code zalo.enabled} (default {@code true},
 * i.e. mock mode ON) so it can be switched off wholesale without removing the client.</p>
 *
 * @since Wave local-doable-11 Bucket B (GAP-063 Phase 1 scaffold)
 */
@Slf4j
@Service
@ConditionalOnProperty(
    prefix = "zalo",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ZaloNotificationChannel implements NotificationChannel {

    /**
     * Channel identifier per BR-NOTIF-001 — matches
     * {@code NotificationChannelType.ZALO} in kitehub-subscription. Kept as a plain
     * string (no cross-module dependency) exactly like {@link SESEmailService#CHANNEL_NAME}.
     */
    public static final String CHANNEL_NAME = "ZALO";

    private final ObjectProvider<ZaloOAClient> zaloClientProvider;
    private final ZaloOAConfig.ZaloProperties properties;

    public ZaloNotificationChannel(
            ObjectProvider<ZaloOAClient> zaloClientProvider,
            ZaloOAConfig.ZaloProperties properties) {
        this.zaloClientProvider = zaloClientProvider;
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     *
     * <p>ZALO channel implementation. {@code recipient} is the Zalo OA follower user-id;
     * {@code message} is the plain-text body; {@code ctx.templateName} (when set) is mapped
     * through {@code zalo.zns-template-ids} to the vendor ZNS template id (falling back to
     * the raw template name when unmapped). Delegates to the active {@link ZaloOAClient}
     * — the mock in Phase 1 — and lifts its {@link ZaloSendResult} into the channel-agnostic
     * {@link NotificationSendResult}.</p>
     */
    @Override
    public NotificationSendResult send(String recipient, String message, NotificationContext ctx) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be null or blank");
        }

        ZaloOAClient client = zaloClientProvider.getIfAvailable();
        if (client == null) {
            // No Zalo client wired (e.g. zalo.provider=live with no live impl yet in
            // Phase 1). Skip rather than fail — mirrors EmailProviderRouter fallback.
            log.warn("[ZaloNotificationChannel] no ZaloOAClient available — skipping send to {}", recipient);
            return NotificationSendResult.builder()
                    .status(NotificationSendResult.Status.SKIPPED_DISABLED_IN_PHASE_1)
                    .sentAt(LocalDateTime.now())
                    .channel(CHANNEL_NAME)
                    .build();
        }

        // Tolerate a null context per the interface javadoc.
        NotificationContext context = ctx != null
                ? ctx
                : NotificationContext.builder().build();

        String znsTemplateId = resolveZnsTemplateId(context.getTemplateName());
        ZaloMessage zaloMessage = ZaloMessage.builder()
                .body(message)
                .templateId(znsTemplateId)
                .locale(context.getLocale())
                .build();

        try {
            ZaloSendResult result = client.sendMessage(recipient, zaloMessage);
            return toNotificationResult(result);
        } catch (Exception e) {
            log.warn("[ZaloNotificationChannel] send to {} failed: {}", recipient, e.getMessage());
            return NotificationSendResult.builder()
                    .status(NotificationSendResult.Status.FAILED)
                    .sentAt(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .channel(CHANNEL_NAME)
                    .build();
        }
    }

    @Override
    public String channelName() {
        return CHANNEL_NAME;
    }

    /**
     * Map a logical template name to the configured ZNS template id
     * ({@code zalo.zns-template-ids}). Returns {@code null} for a null/blank name and
     * echoes the name back when no explicit mapping exists (the mock ignores the id;
     * the live adapter chooses the matching ZNS template variant).
     */
    private String resolveZnsTemplateId(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return null;
        }
        return properties.getZnsTemplateIds().getOrDefault(templateName, templateName);
    }

    /**
     * Lift the Zalo-specific {@link ZaloSendResult} into the channel-agnostic
     * {@link NotificationSendResult} so callers never depend on the vendor envelope.
     */
    private NotificationSendResult toNotificationResult(ZaloSendResult result) {
        NotificationSendResult.Status mapped;
        switch (result.getStatus() == null ? ZaloSendResult.Status.FAILED : result.getStatus()) {
            case SENT:
                mapped = NotificationSendResult.Status.SENT;
                break;
            case MOCK:
                mapped = NotificationSendResult.Status.MOCK;
                break;
            case SKIPPED:
                mapped = NotificationSendResult.Status.SKIPPED_DISABLED_IN_PHASE_1;
                break;
            case FAILED:
            default:
                mapped = NotificationSendResult.Status.FAILED;
                break;
        }
        LocalDateTime sentAt = result.getSentAt() != null
                ? LocalDateTime.ofInstant(result.getSentAt(), ZoneOffset.UTC)
                : LocalDateTime.now();
        return NotificationSendResult.builder()
                .providerMessageId(result.getProviderMessageId())
                .status(mapped)
                .sentAt(sentAt)
                .errorMessage(result.getErrorMessage())
                .channel(CHANNEL_NAME)
                .build();
    }
}
