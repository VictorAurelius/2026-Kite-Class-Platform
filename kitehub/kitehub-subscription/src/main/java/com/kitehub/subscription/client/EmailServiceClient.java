package com.kitehub.subscription.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for KiteHub Email Service.
 * Sends emails via internal email service API.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailServiceClient {

    private final RestTemplate restTemplate;

    @Value("${email.service.url:http://localhost:8083}")
    private String emailServiceUrl;

    /**
     * Send trial expiration warning email.
     *
     * @param to Recipient email address
     * @param organizationName Organization name
     * @param daysRemaining Days remaining in trial
     */
    public void sendTrialExpirationWarning(String to, String organizationName, long daysRemaining) {
        log.info("Sending trial expiration warning to {}: {} days remaining", to, daysRemaining);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject(String.format("Your KiteClass trial expires in %d day%s",
                    daysRemaining, daysRemaining == 1 ? "" : "s"))
                .templateName("trial-expiration-warning")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "daysRemaining", daysRemaining,
                    "upgradeUrl", "https://kitehub.com/pricing"
                ))
                .build();

            sendEmailRequest(request);
        } catch (Exception e) {
            log.error("Failed to send trial expiration warning email to {}", to, e);
        }
    }

    /**
     * Send trial expired email.
     *
     * @param to Recipient email address
     * @param organizationName Organization name
     */
    public void sendTrialExpired(String to, String organizationName) {
        log.info("Sending trial expired notification to {}", to);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Your KiteClass trial has expired")
                .templateName("trial-expired")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "upgradeUrl", "https://kitehub.com/pricing"
                ))
                .build();

            sendEmailRequest(request);
        } catch (Exception e) {
            log.error("Failed to send trial expired email to {}", to, e);
        }
    }

    /**
     * Send subscription renewal reminder email.
     *
     * @param to Recipient email address
     * @param organizationName Organization name
     * @param daysUntilExpiration Days until subscription expires
     * @param tier Subscription tier
     * @param amountVnd Renewal amount in VND
     */
    public void sendRenewalReminder(String to, String organizationName,
                                    long daysUntilExpiration, String tier, long amountVnd) {
        log.info("Sending renewal reminder to {}: {} days until expiration", to, daysUntilExpiration);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject(String.format("Your KiteClass %s subscription renews in %d day%s",
                    tier, daysUntilExpiration, daysUntilExpiration == 1 ? "" : "s"))
                .templateName("subscription-renewal-reminder")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "daysUntilExpiration", daysUntilExpiration,
                    "tier", tier,
                    "amountVnd", String.format("%,d", amountVnd),
                    "paymentUrl", "https://kitehub.com/subscription/payment"
                ))
                .build();

            sendEmailRequest(request);
        } catch (Exception e) {
            log.error("Failed to send renewal reminder email to {}", to, e);
        }
    }

    /**
     * Send subscription suspended notification email.
     *
     * @param to Recipient email address
     * @param organizationName Organization name
     */
    public void sendSuspensionNotification(String to, String organizationName) {
        log.info("Sending suspension notification to {}", to);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Your KiteClass subscription has been suspended")
                .templateName("subscription-suspended")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "renewUrl", "https://kitehub.com/subscription/renew"
                ))
                .build();

            sendEmailRequest(request);
        } catch (Exception e) {
            log.error("Failed to send suspension notification email to {}", to, e);
        }
    }

    /**
     * Send email request to email service.
     *
     * @param request Email request
     */
    private void sendEmailRequest(EmailRequest request) {
        String url = emailServiceUrl + "/api/platform/emails/send";

        try {
            ResponseEntity<EmailResponse> response = restTemplate.postForEntity(
                url,
                request,
                EmailResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Email sent successfully to {}", request.getTo());
            } else {
                log.warn("Email service returned non-2xx status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to call email service at {}: {}", url, e.getMessage());
            throw e;
        }
    }

    /**
     * Email request DTO (matches KiteHub Email Service API).
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class EmailRequest {
        private String to;
        private String subject;
        private String templateName;
        private Map<String, Object> variables;
    }

    /**
     * Email response DTO (matches KiteHub Email Service API).
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class EmailResponse {
        private boolean success;
        private String messageId;
        private String error;
    }
}
