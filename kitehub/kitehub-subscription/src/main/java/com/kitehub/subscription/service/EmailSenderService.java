package com.kitehub.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service to send emails via kitehub-email REST API.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderService {

    /**
     * Injected RestTemplate (configured with explicit connect/read timeouts via
     * {@link com.kitehub.subscription.config.RestTemplateConfig} — GAP-131 fix).
     * The previous {@code new RestTemplate()} field initializer bypassed the
     * bean and used JVM default infinite timeouts.
     */
    private final RestTemplate restTemplate;

    @Value("${kitehub.email-service.url:http://kitehub-email:8080}")
    private String emailServiceUrl;

    @Value("${kitehub.email-service.enabled:true}")
    private boolean emailEnabled;

    /**
     * Send verification email.
     *
     * @param to Recipient email
     * @param verifyUrl Verification URL
     */
    public void sendVerificationEmail(String to, String verifyUrl) {
        if (!emailEnabled) {
            log.info("[EMAIL-MOCK] Verification email for {}: {}", to, verifyUrl);
            return;
        }

        try {
            Map<String, Object> request = Map.of(
                    "to", to,
                    "subject", "Xác nhận email - KiteClass",
                    "templateName", "email-verification",
                    "variables", Map.of("verifyUrl", verifyUrl)
            );

            restTemplate.postForEntity(
                    emailServiceUrl + "/api/platform/emails/send",
                    request,
                    Map.class
            );

            log.info("Verification email sent to: {}", to);
        } catch (Exception e) {
            // Don't block registration if email fails
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            log.info("[EMAIL-FALLBACK] Verification link for {}: {}", to, verifyUrl);
        }
    }

    /**
     * Send password-reset email (GAP-548 / Wave 79 Bucket C).
     *
     * <p>Best-effort delivery — failure does not surface to caller so that the
     * {@code POST /api/auth/password-reset-request} endpoint can stay constant-time
     * regardless of email-service availability. Email-existence enumeration is
     * mitigated upstream by returning 202 unconditionally.</p>
     *
     * @param to recipient email
     * @param resetUrl absolute URL containing single-use token (TTL ~1h)
     */
    public void sendPasswordResetEmail(String to, String resetUrl) {
        if (!emailEnabled) {
            log.info("[EMAIL-MOCK] Password-reset email for {}: {}", to, resetUrl);
            return;
        }

        try {
            Map<String, Object> request = Map.of(
                    "to", to,
                    "subject", "Đặt lại mật khẩu - KiteHub",
                    "templateName", "password-reset",
                    "variables", Map.of("resetUrl", resetUrl)
            );

            restTemplate.postForEntity(
                    emailServiceUrl + "/api/platform/emails/send",
                    request,
                    Map.class
            );

            log.info("Password-reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password-reset email to {}: {}", to, e.getMessage());
            log.info("[EMAIL-FALLBACK] Password-reset link for {}: {}", to, resetUrl);
        }
    }
}
