package com.kitehub.subscription.service;

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
public class EmailSenderService {

    private final RestTemplate restTemplate = new RestTemplate();

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
}
