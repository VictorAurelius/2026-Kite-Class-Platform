package com.kitehub.subscription.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.entity.EmailSentLog;
import com.kitehub.subscription.config.EmailConfigProperties;
import com.kitehub.subscription.config.EmailQueueConfig;
import com.kitehub.subscription.dto.EmailEvent;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Client for KiteHub Email Service.
 * <p>
 * Supports two modes controlled by kitehub.email.use-queue:
 * - Queue mode (default, true): publishes EmailEvent to RabbitMQ for async delivery with retry
 * - Direct mode (false): sends via synchronous HTTP POST (for dev/testing without RabbitMQ)
 * <p>
 * Includes idempotency check (alreadySentToday) to prevent duplicate emails per day.
 * The dedup check runs BEFORE publishing/sending regardless of mode.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Transactional
public class EmailServiceClient {

    /** Outbox event-type label for queued email events. Stable contract with consumer + dispatcher. */
    public static final String EVENT_TYPE_EMAIL_QUEUED = "email.queued";

    private final RestTemplate restTemplate;
    private final EmailSentLogRepository emailSentLogRepository;
    private final RabbitTemplate rabbitTemplate;
    private final EmailConfigProperties emailConfigProperties;
    private final SubscriptionEventEmitter eventEmitter;
    private final ObjectMapper objectMapper;

    @Value("${email.service.url:http://localhost:8083}")
    private String emailServiceUrl;

    @Value("${kitehub.email.use-queue:true}")
    private boolean useQueue;

    public EmailServiceClient(RestTemplate restTemplate,
                              EmailSentLogRepository emailSentLogRepository,
                              RabbitTemplate rabbitTemplate,
                              EmailConfigProperties emailConfigProperties,
                              SubscriptionEventEmitter eventEmitter,
                              ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.emailSentLogRepository = emailSentLogRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.emailConfigProperties = emailConfigProperties;
        this.eventEmitter = eventEmitter;
        this.objectMapper = objectMapper;
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

            dispatchEmail(instanceId, "trial-warning", request);
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

            dispatchEmail(instanceId, "trial-expired", request);
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

            dispatchEmail(instanceId, "renewal-reminder", request);
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

            dispatchEmail(instanceId, "suspension-notification", request);
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

            dispatchEmail(instanceId, "retention-warning", request);
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

            dispatchEmail(instanceId, "trial-midpoint", request);
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

            dispatchEmail(instanceId, "onboarding-tips", request);
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

            dispatchEmail(instanceId, "subscription-expired", request);
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

            dispatchEmail(instanceId, "retention-final-warning", request);
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

            dispatchEmail(instanceId, "data-deleted", request);
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
        if (alreadySentToday(instanceId, "welcome", to)) {
            log.debug("Welcome email already sent today to {}, skipping", to);
            return;
        }
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

            dispatchEmail(instanceId, "welcome", request);
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
        if (alreadySentToday(instanceId, "subscription-created", to)) {
            log.debug("Subscription-created email already sent today to {}, skipping", to);
            return;
        }
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

            dispatchEmail(instanceId, "subscription-created", request);
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
     * Check if a specific email type is enabled via admin toggles.
     * Types not present in the toggles map default to enabled (true).
     *
     * @param emailType email type to check
     * @return true if enabled, false if disabled by admin
     */
    private boolean isEmailTypeEnabled(String emailType) {
        return emailConfigProperties.getTypeToggles().getOrDefault(emailType, true);
    }

    /**
     * Dispatch email via queue or direct HTTP based on configuration.
     * Checks email type toggle before dispatching; skips if disabled by admin.
     * Records email intent in sent log after dispatching.
     *
     * @param instanceId Instance ID (nullable)
     * @param emailType Email type for tracking
     * @param request Email request details
     */
    private void dispatchEmail(UUID instanceId, String emailType, EmailRequest request) {
        if (!isEmailTypeEnabled(emailType)) {
            log.warn("Email type '{}' is disabled by admin config, skipping send to {}",
                emailType, request.getTo());
            return;
        }

        if (useQueue) {
            publishToQueue(instanceId, emailType, request);
        } else {
            sendEmailRequestDirect(request);
        }
        recordEmailSent(instanceId, emailType, request.getTo());
    }

    /**
     * Outbox-first publish per design-patterns.md §3.5.1 Exception A:
     * write the event to {@code subscription_outbox} (reliability net), then attempt
     * a best-effort fast-path {@code rabbitTemplate.convertAndSend} so currently-online
     * consumers receive the email without waiting for the dispatcher poll.
     *
     * @param instanceId Instance ID (nullable for system-level / pre-provisioning emails)
     * @param emailType Email type
     * @param request Email request
     */
    private void publishToQueue(UUID instanceId, String emailType, EmailRequest request) {
        EmailEvent event = EmailEvent.builder()
                .instanceId(instanceId)
                .to(request.getTo())
                .subject(request.getSubject())
                .templateName(request.getTemplateName())
                .variables(request.getVariables())
                .emailType(emailType)
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            // Drop the outbox row rather than ship a malformed payload — caller logs+continues.
            // This is a programmer error (Map<String,Object> with non-serializable value), not a runtime fault.
            throw new IllegalStateException("Failed to serialize EmailEvent for outbox", ex);
        }

        eventEmitter.emit(instanceId, EVENT_TYPE_EMAIL_QUEUED,
            EmailQueueConfig.EMAIL_ROUTING_KEY, payload);

        try {
            // Best-effort fast-path — outbox is the reliability net.
            rabbitTemplate.convertAndSend(
                    EmailQueueConfig.EMAIL_EXCHANGE,
                    EmailQueueConfig.EMAIL_ROUTING_KEY,
                    event
            );
            log.debug("Email event published to queue: type={}, to={}", emailType, request.getTo());
        } catch (Exception ex) {
            log.warn("Direct email publish failed (type={}, to={}) — outbox will retry: {}",
                emailType, request.getTo(), ex.getMessage());
        }
    }

    /**
     * Send email request directly via HTTP (fallback for dev/testing without RabbitMQ).
     *
     * @param request Email request
     */
    private void sendEmailRequestDirect(EmailRequest request) {
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
     * Record that an email was sent (or queued).
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
