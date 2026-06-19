package com.kitehub.subscription.notification.channel;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.billing.dto.ReceiptResponse;
import com.kitehub.subscription.notification.enums.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fans an {@link OwnerNotification} across registered {@link NotificationChannel} adapters
 * (GAP-1265). Email-not-the-only-channel abstraction: the durable IN_APP banner is always
 * persisted (fallback), and EMAIL is delivered when wired + a recipient/template is present.
 *
 * <p>Channels are auto-discovered via injected {@code List<NotificationChannel>} → indexed by
 * {@link NotificationChannelType}, so adding the deferred SMS / ZALO / PUSH adapters (GAP-063b)
 * is a drop-in: implement the port + register the bean. <strong>Zalo OA</strong> is intentionally
 * a documented stub here — no adapter is wired in Phase 1 (full Zalo Notification Service =
 * GAP-063 scope); if a ZALO channel is later registered the dispatcher picks it up automatically.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Slf4j
@Component
public class OwnerNotificationDispatcher {

    private final Map<NotificationChannelType, NotificationChannel> channels =
        new EnumMap<>(NotificationChannelType.class);

    /**
     * GAP-1414: canonical KiteHub public app base URL for owner-notification CTA links.
     * Single source of truth shared with EmailServiceClient + DomainService; env-overridable
     * via {@code KITEHUB_APP_BASE_URL} (Spring relaxed binding). Replaces hardcoded
     * {@code https://kitehub.me/billing*} literals.
     */
    @Value("${kitehub.app.base-url:https://kitehub.me}")
    private String appBaseUrl;

    public OwnerNotificationDispatcher(List<NotificationChannel> channelBeans) {
        for (NotificationChannel ch : channelBeans) {
            channels.put(ch.type(), ch);
        }
        log.info("OwnerNotificationDispatcher wired channels: {}", channels.keySet());
    }

    /**
     * Deliver across channels: persist the durable in-app banner first (reliability net), then
     * best-effort email. Never throws — a channel failure is logged, not propagated.
     *
     * @param n notification payload
     */
    public void notifyOwner(OwnerNotification n) {
        deliverVia(NotificationChannelType.IN_APP, n);
        deliverVia(NotificationChannelType.EMAIL, n);
        // ZALO / SMS / PUSH: deferred (GAP-063b). If registered, fan out here.
        deliverVia(NotificationChannelType.ZALO, n);
    }

    private void deliverVia(NotificationChannelType type, OwnerNotification n) {
        NotificationChannel ch = channels.get(type);
        if (ch == null) {
            return; // channel not wired in this phase (e.g. ZALO stub)
        }
        try {
            boolean delivered = ch.deliver(n);
            log.debug("Notification {} via {} → {}", n.getNotificationType(), type, delivered);
        } catch (Exception e) {
            log.warn("Channel {} threw for {}: {}", type, n.getNotificationType(), e.getMessage());
        }
    }

    // ---- convenience builders for the two Phase-1 owner notifications ----

    /**
     * Notify the owner their pending payment was confirmed (GAP-1257-BE), bundling the non-VAT
     * receipt summary (GAP-1266) into the email + in-app banner.
     *
     * @param instance owning instance (recipient + organization)
     * @param receipt  the freshly-generated receipt
     */
    public void sendPaymentConfirmed(Instance instance, ReceiptResponse receipt) {
        if (instance == null) {
            return;
        }
        String org = instance.getOrganizationName();
        long amount = receipt.getAmountVnd() != null ? receipt.getAmountVnd() : 0L;
        String amountStr = String.format("%,d", amount);
        OwnerNotification n = OwnerNotification.builder()
            .instanceId(instance.getId())
            .notificationType("payment-confirmed")
            .recipientEmail(instance.getContactEmail())
            .organizationName(org)
            .title("Thanh toán đã được xác nhận")
            .body("KiteHub đã xác nhận thanh toán " + amountStr + "đ của bạn. "
                + "Biên nhận: " + receipt.getReceiptNumber() + ".")
            .actionUrl(appBaseUrl + "/billing")
            .emailSubject("[KiteHub] Thanh toán đã được xác nhận - " + (org == null ? "" : org))
            .emailTemplate("payment-confirmed")
            .emailVariables(Map.of(
                "organizationName", org == null ? "" : org,
                "receiptNumber", receipt.getReceiptNumber() == null ? "" : receipt.getReceiptNumber(),
                "tier", receipt.getTier() == null ? "" : receipt.getTier(),
                "amountVnd", amountStr,
                "transactionId", receipt.getTransactionId() == null ? "" : receipt.getTransactionId(),
                "paidAt", receipt.getPaidAt() == null ? "" : receipt.getPaidAt().toString(),
                "billingUrl", appBaseUrl + "/billing"
            ))
            .build();
        notifyOwner(n);
    }

    /**
     * Win-back outreach after cancel/suspend (GAP-1263-BE), CTA → reactivate.
     *
     * <p>Provided as a seam for the suspend/cancel schedulers (BE-2/BE-3) to invoke — the
     * reactivation endpoint is the CTA target. Idempotency of the actual reactivation is handled
     * by {@code OwnerBillingService.reactivate}.</p>
     *
     * @param instance owning instance (recipient + organization)
     * @param voluntary {@code true} = owner cancelled; {@code false} = non-payment lapse
     */
    public void sendWinBack(Instance instance, boolean voluntary) {
        if (instance == null) {
            return;
        }
        String org = instance.getOrganizationName();
        String reason = voluntary
            ? "Gói đăng ký của bạn đã được hủy."
            : "Gói đăng ký của bạn đã hết hạn và bị tạm ngưng.";
        OwnerNotification n = OwnerNotification.builder()
            .instanceId(instance.getId())
            .notificationType("winback-reactivate")
            .recipientEmail(instance.getContactEmail())
            .organizationName(org)
            .title("Kích hoạt lại trung tâm của bạn")
            .body(reason + " Kích hoạt lại bất cứ lúc nào để tiếp tục sử dụng KiteHub.")
            .actionUrl(appBaseUrl + "/billing/reactivate")
            .emailSubject("[KiteHub] Kích hoạt lại " + (org == null ? "trung tâm của bạn" : org))
            .emailTemplate("winback-reactivate")
            .emailVariables(Map.of(
                "organizationName", org == null ? "" : org,
                "reason", reason,
                "voluntary", voluntary,
                "reactivateUrl", appBaseUrl + "/billing/reactivate"
            ))
            .build();
        notifyOwner(n);
    }
}
