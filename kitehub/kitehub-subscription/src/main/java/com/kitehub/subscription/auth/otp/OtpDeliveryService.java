package com.kitehub.subscription.auth.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Delivers a freshly-minted OTP to the user's phone (GAP-286 — mobile signup).
 *
 * <p><strong>Phase 1 = MOCK only.</strong> There is no clean in-module path to push a
 * phone OTP: {@code OwnerNotificationDispatcher} is {@code Instance}/email-template
 * centric and the ZALO channel ({@link com.kitehub.subscription.notification.enums.NotificationChannelType#ZALO})
 * is an un-wired documented stub (GAP-063b). So in mock mode this adapter simply logs
 * the code at INFO and performs <em>no</em> outbound send. No live vendor API is ever
 * called from here.</p>
 *
 * <p>Mock mode is controlled by {@code kitehub.auth.signup-otp.mock-enabled} (default
 * {@code true}). When disabled, this adapter logs a warning and returns {@code false}
 * (no live channel wired yet) — it still never calls a vendor.</p>
 *
 * <p>TODO Phase 2: wire live ZNS via the kitehub-email Zalo Notification Service channel
 * (GAP-063b) — register a ZALO {@code NotificationChannel} adapter and dispatch through it.</p>
 *
 * @since GAP-286 (mobile signup OTP)
 */
@Service
@Slf4j
public class OtpDeliveryService {

    private final boolean mockEnabled;

    public OtpDeliveryService(
        @Value("${kitehub.auth.signup-otp.mock-enabled:true}") boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    /**
     * "Deliver" the OTP. In mock mode logs the code at INFO and returns {@code true}.
     *
     * @param phone   destination phone number
     * @param code    the 6-digit plaintext OTP (logged ONLY in mock mode)
     * @param channel requested delivery channel (e.g. ZALO)
     * @return {@code true} when handled in mock mode; {@code false} when no live channel is wired
     */
    public boolean deliver(String phone, String code, String channel) {
        if (mockEnabled) {
            // MOCK: log the code so devs can complete the flow locally. NEVER do this
            // once a live channel is wired (would leak the OTP to logs).
            log.info("[OTP-MOCK] OTP for {} via {}: {} (mock — not sent)", phone, channel, code);
            return true;
        }
        // No live phone-OTP channel is wired in Phase 1.
        // TODO Phase 2: wire live ZNS via kitehub-email channel (GAP-063b).
        log.warn("[OTP] Live OTP delivery not wired (channel={}); no message sent. "
            + "TODO Phase 2: wire live ZNS via kitehub-email channel.", channel);
        return false;
    }
}
