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
     * Send trial midpoint engagement email.
     * Sent at the midpoint of the trial period (default: day 7).
     *
     * @param instanceId Instance ID for idempotency tracking
     * @param to Recipient email address
     * @param subdomain Instance subdomain
     */
    public void sendTrialMidpointEmail(UUID instanceId, String to, String subdomain) {
        if (alreadySentToday(instanceId, "trial-midpoint", to)) {
            log.info("Email already sent today: trial-midpoint to {}", to);
            return;
        }

        log.info("Sending trial midpoint email to {} (subdomain: {})", to, subdomain);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Bạn đang dùng KiteClass được nửa thời gian trial!")
                .templateName("trial-midpoint")
                .variables(Map.of(
                    "subdomain", subdomain,
                    "instanceName", subdomain,
                    "upgradeUrl", "https://kitehub.com/pricing",
                    "dashboardUrl", String.format("https://%s.kiteclass.vn/dashboard", subdomain)
                ))
                .build();

            sendEmailRequest(request);
            recordEmailSent(instanceId, "trial-midpoint", to);
        } catch (Exception e) {
            log.error("Failed to send trial midpoint email to {}", to, e);
        }
    }

    /**
     * Send onboarding tips email.
     * Sent ~24 hours after instance activation.
     *
     * @param instanceId Instance ID for idempotency tracking
     * @param to Recipient email address
     * @param subdomain Instance subdomain
     */
    public void sendOnboardingTipsEmail(UUID instanceId, String to, String subdomain) {
        if (alreadySentToday(instanceId, "onboarding-tips", to)) {
            log.info("Email already sent today: onboarding-tips to {}", to);
            return;
        }

        log.info("Sending onboarding tips email to {} (subdomain: {})", to, subdomain);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Bắt đầu với KiteClass - Các mẹo hữu ích cho bạn")
                .templateName("onboarding-tips")
                .variables(Map.of(
                    "subdomain", subdomain,
                    "instanceName", subdomain,
                    "dashboardUrl", String.format("https://%s.kiteclass.vn/dashboard", subdomain)
                ))
                .build();

            sendEmailRequest(request);
            recordEmailSent(instanceId, "onboarding-tips", to);
        } catch (Exception e) {
            log.error("Failed to send onboarding tips email to {}", to, e);
        }
    }

    /**
     * Send subscription expired notification email.
     * Sent when subscription expires and grace period has passed.
     *
     * @param instanceId Instance ID for idempotency tracking
     * @param to Recipient email address
     * @param subdomain Instance subdomain
     */
    public void sendSubscriptionExpiredEmail(UUID instanceId, String to, String subdomain) {
        if (alreadySentToday(instanceId, "subscription-expired", to)) {
            log.info("Email already sent today: subscription-expired to {}", to);
            return;
        }

        log.info("Sending subscription expired email to {} (subdomain: {})", to, subdomain);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Subscription KiteClass của bạn đã hết hạn")
                .templateName("subscription-expired")
                .variables(Map.of(
                    "subdomain", subdomain,
                    "instanceName", subdomain,
                    "renewUrl", "https://kitehub.com/subscription/renew"
                ))
                .build();

            sendEmailRequest(request);
            recordEmailSent(instanceId, "subscription-expired", to);
        } catch (Exception e) {
            log.error("Failed to send subscription expired email to {}", to, e);
        }
    }

    /**
     * Send data retention final warning email.
     * Sent 1 day before data deletion.
     *
     * @param instanceId Instance ID for idempotency tracking
     * @param to Recipient email address
     * @param subdomain Instance subdomain
     */
    public void sendDataRetentionFinalWarning(UUID instanceId, String to, String subdomain) {
        if (alreadySentToday(instanceId, "retention-final-warning", to)) {
            log.info("Email already sent today: retention-final-warning to {}", to);
            return;
        }

        log.info("Sending data retention final warning to {} (subdomain: {})", to, subdomain);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("KHẨN: Dữ liệu KiteClass của bạn sẽ bị xóa sau 24 giờ")
                .templateName("data-retention-final-warning")
                .variables(Map.of(
                    "subdomain", subdomain,
                    "instanceName", subdomain,
                    "renewUrl", "https://kitehub.com/subscription/renew"
                ))
                .build();

            sendEmailRequest(request);
            recordEmailSent(instanceId, "retention-final-warning", to);
        } catch (Exception e) {
            log.error("Failed to send data retention final warning email to {}", to, e);
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
     * Send welcome email after instance activation.
     *
     * @param instanceId Instance ID
     * @param to Recipient email
     * @param organizationName Organization name
     * @param trialDays Number of trial days
     * @param expiryDate Trial expiry date
     */
    public void sendWelcomeEmail(UUID instanceId, String to, String organizationName,
                                 int trialDays, String expiryDate) {
        log.info("Sending welcome email to {}", to);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Chào mừng bạn đến với KiteHub!")
                .templateName("welcome")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "trialDays", trialDays,
                    "expiryDate", expiryDate,
                    "loginUrl", "https://kitehub.vn/login"
                ))
                .build();

            sendEmailRequest(request);
            recordEmailSent(instanceId, "welcome", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", to, e);
        }
    }

    /**
     * Send subscription created confirmation email.
     *
     * @param instanceId Instance ID
     * @param to Recipient email
     * @param organizationName Organization name
     * @param tier Subscription tier
     * @param billingCycle Billing cycle
     */
    public void sendSubscriptionCreatedEmail(UUID instanceId, String to, String organizationName,
                                             String tier, String billingCycle) {
        log.info("Sending subscription created email to {}", to);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Subscription đã kích hoạt - " + organizationName)
                .templateName("subscription-created")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "tier", tier,
                    "billingCycle", billingCycle,
                    "dashboardUrl", "https://kitehub.vn/dashboard"
                ))
                .build();

            sendEmailRequest(request);
            recordEmailSent(instanceId, "subscription-created", to);
        } catch (Exception e) {
            log.error("Failed to send subscription created email to {}", to, e);
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
