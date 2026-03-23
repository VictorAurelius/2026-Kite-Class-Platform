package com.kitehub.subscription.client;

import com.kitehub.platform.domain.entity.EmailSentLog;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Client for KiteHub Email Service.
 * Sends emails via internal email service API.
 * Includes idempotency check to prevent duplicate emails per day.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class EmailServiceClient {

    private final RestTemplate restTemplate;
    private final EmailSentLogRepository emailSentLogRepository;

    @Value("${email.service.url:http://localhost:8083}")
    private String emailServiceUrl;

    public EmailServiceClient(RestTemplate restTemplate,
                              EmailSentLogRepository emailSentLogRepository) {
        this.restTemplate = restTemplate;
        this.emailSentLogRepository = emailSentLogRepository;
    }

    /**
     * Send trial expiration warning email.
     *
     * @param to Recipient email address
     * @param organizationName Organization name
     * @param daysRemaining Days remaining in trial
     */
    public void sendTrialExpirationWarning(String to, String organizationName, long daysRemaining) {
        sendTrialExpirationWarning(null, to, organizationName, daysRemaining);
    }

    /**
     * Send trial expiration warning email with instance context.
     *
     * @param instanceId Instance ID for idempotency tracking
     * @param to Recipient email address
     * @param organizationName Organization name
     * @param daysRemaining Days remaining in trial
     */
    public void sendTrialExpirationWarning(UUID instanceId, String to, String organizationName,
                                           long daysRemaining) {
        if (alreadySentToday(instanceId, "trial-warning", to)) {
            log.info("Email already sent today: trial-warning to {}", to);
            return;
        }

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
            recordEmailSent(instanceId, "trial-warning", to);
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
        sendTrialExpired(null, to, organizationName);
    }

    /**
     * Send trial expired email with instance context.
     *
     * @param instanceId Instance ID for idempotency tracking
     * @param to Recipient email address
     * @param organizationName Organization name
     */
    public void sendTrialExpired(UUID instanceId, String to, String organizationName) {
        if (alreadySentToday(instanceId, "trial-expired", to)) {
            log.info("Email already sent today: trial-expired to {}", to);
            return;
        }

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
            recordEmailSent(instanceId, "trial-expired", to);
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
        sendRenewalReminder(null, to, organizationName, daysUntilExpiration, tier, amountVnd);
    }

    /**
     * Send subscription renewal reminder email with instance context.
     *
     * @param instanceId Instance ID for idempotency tracking
     * @param to Recipient email address
     * @param organizationName Organization name
     * @param daysUntilExpiration Days until subscription expires
     * @param tier Subscription tier
     * @param amountVnd Renewal amount in VND
     */
    public void sendRenewalReminder(UUID instanceId, String to, String organizationName,
                                    long daysUntilExpiration, String tier, long amountVnd) {
        if (alreadySentToday(instanceId, "renewal-reminder", to)) {
            log.info("Email already sent today: renewal-reminder to {}", to);
            return;
        }

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
            recordEmailSent(instanceId, "renewal-reminder", to);
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
        sendSuspensionNotification(null, to, organizationName);
    }

    /**
     * Send subscription suspended notification email with instance context.
     *
     * @param instanceId Instance ID for idempotency tracking
     * @param to Recipient email address
     * @param organizationName Organization name
     */
    public void sendSuspensionNotification(UUID instanceId, String to, String organizationName) {
        if (alreadySentToday(instanceId, "suspension-notification", to)) {
            log.info("Email already sent today: suspension-notification to {}", to);
            return;
        }

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
            recordEmailSent(instanceId, "suspension-notification", to);
        } catch (Exception e) {
            log.error("Failed to send suspension notification email to {}", to, e);
        }
    }

    /**
     * Send data retention warning email.
     *
     * @param instanceId Instance ID
     * @param to Recipient email address
     * @param organizationName Organization name
     * @param daysLeft Days left before data deletion
     */
    public void sendRetentionWarning(UUID instanceId, String to, String organizationName,
                                     long daysLeft) {
        if (alreadySentToday(instanceId, "retention-warning", to)) {
            log.info("Email already sent today: retention-warning to {}", to);
            return;
        }

        log.info("Sending data retention warning to {}: {} days left", to, daysLeft);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject(String.format("Your KiteClass data will be deleted in %d day%s",
                    daysLeft, daysLeft == 1 ? "" : "s"))
                .templateName("data-retention-warning")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "daysLeft", daysLeft,
                    "renewUrl", "https://kitehub.com/subscription/renew"
                ))
                .build();

            sendEmailRequest(request);
            recordEmailSent(instanceId, "retention-warning", to);
        } catch (Exception e) {
            log.error("Failed to send retention warning email to {}", to, e);
        }
    }

    /**
     * Send data deleted notification email.
     *
     * @param instanceId Instance ID
     * @param to Recipient email address
     * @param organizationName Organization name
     */
    public void sendDataDeletedNotification(UUID instanceId, String to,
                                            String organizationName) {
        if (alreadySentToday(instanceId, "data-deleted", to)) {
            log.info("Email already sent today: data-deleted to {}", to);
            return;
        }

        log.info("Sending data deleted notification to {}", to);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Your KiteClass data has been deleted")
                .templateName("data-deleted")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "contactUrl", "https://kitehub.com/contact"
                ))
                .build();

            sendEmailRequest(request);
            recordEmailSent(instanceId, "data-deleted", to);
        } catch (Exception e) {
            log.error("Failed to send data deleted notification email to {}", to, e);
        }
    }

    /**
     * Check if an email of the given type was already sent today.
     *
     * @param instanceId Instance ID (nullable)
     * @param emailType Email type
     * @param recipient Recipient email
     * @return true if already sent today
     */
    boolean alreadySentToday(UUID instanceId, String emailType, String recipient) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
            instanceId, emailType, recipient, startOfDay, endOfDay);
    }

    /**
     * Record that an email was sent.
     *
     * @param instanceId Instance ID (nullable)
     * @param emailType Email type
     * @param recipient Recipient email
     */
    private void recordEmailSent(UUID instanceId, String emailType, String recipient) {
        emailSentLogRepository.save(EmailSentLog.builder()
            .instanceId(instanceId)
            .emailType(emailType)
            .recipient(recipient)
            .sentAt(LocalDateTime.now())
            .build());
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
    static class EmailRequest {
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
    static class EmailResponse {
        private boolean success;
        private String messageId;
        private String error;
    }
}
