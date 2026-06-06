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
import java.time.OffsetDateTime;
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
    /**
     * GAP-922 fix (2026-06-04): rabbitTemplate retained for backward-compat injection
     * (callers may pass it from older tests + Spring DI); no longer used post-fix removed
     * double-publish in publishToQueue (eventEmitter.emit() already does fast-path).
     */
    @SuppressWarnings("unused")
    private final RabbitTemplate rabbitTemplate;
    private final EmailConfigProperties emailConfigProperties;
    private final SubscriptionEventEmitter eventEmitter;
    private final ObjectMapper objectMapper;

    @Value("${email.service.url:http://kitehub-email:8080}")
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
                // GAP-797: welcome.{txt,html} read recipientName + docsUrl +
                // unsubscribeUrl. Sender has no recipient-name param so the
                // template's `recipientName ?: organizationName` fallback applies
                // — pass organizationName explicitly as recipientName so the
                // greeting renders a real value instead of the bare fallback.
                .variables(Map.of(
                    "recipientName", organizationName,
                    "organizationName", organizationName,
                    "trialDays", trialDays,
                    "expiryDate", expiryDate,
                    "loginUrl", "https://kitehub.vn/login",
                    "docsUrl", "https://kitehub.me/help",
                    "unsubscribeUrl", "https://kitehub.me/unsubscribe"
                ))
                .build();

            dispatchEmail(instanceId, "welcome", request);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", to, e);
        }
    }

    /**
     * Send "tenant ready" email after KiteClass provisioning reaches DEPLOYED
     * (Wave provisioning-1 Bucket C — GAP-948).
     *
     * <p>Dispatched by {@link com.kitehub.subscription.consumer.TenantDeployedEventConsumer}
     * when kiteclass-core's {@code TenantProvisioningSaga} finishes (FrontendInstance
     * DEPLOYED) and the consumer-level orchestration publishes a {@code tenant.deployed}
     * event back. This is the recovery / trust-signal email the KC-1 pre-walk audit
     * flagged as missing (benchmark §A row 11: Stripe/Slack/Google Classroom ship a
     * welcome + setup-checklist email post-provision).</p>
     *
     * <p>Reuses {@link #dispatchEmail} — so it inherits the existing 3-retry +
     * {@code email.dlq} reliability path (per {@code EmailQueueConfig}) for free.
     * Best-effort: a send failure is logged and never blocks the caller.</p>
     *
     * @param instanceId Instance ID (idempotency tracking — at-most-once per day)
     * @param to Owner email (recipient — resolved from Instance.contactEmail)
     * @param organizationName Organization name (greeting + body)
     * @param subdomain Provisioned subdomain (drives dashboard URL)
     */
    public void sendTenantReadyEmail(UUID instanceId, String to, String organizationName,
                                     String subdomain) {
        if (alreadySentToday(instanceId, "tenant-ready", to)) {
            log.debug("Tenant-ready email already sent today to {}, skipping", to);
            return;
        }
        log.info("Sending tenant-ready email to {} (subdomain: {})", to, subdomain);

        try {
            String safeSubdomain = subdomain == null ? "" : subdomain;
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Trường học của bạn đã sẵn sàng trên KiteClass!")
                .templateName("tenant-ready")
                .variables(Map.of(
                    "recipientName", organizationName == null ? "bạn" : organizationName,
                    "organizationName", organizationName == null ? "" : organizationName,
                    "subdomain", safeSubdomain,
                    "dashboardUrl", String.format("https://%s.kiteclass.vn/dashboard", safeSubdomain),
                    "onboardingChecklistUrl", "https://kitehub.me/help/onboarding",
                    "supportUrl", "https://kitehub.me/support",
                    "unsubscribeUrl", "https://kitehub.me/unsubscribe"
                ))
                .build();

            dispatchEmail(instanceId, "tenant-ready", request);
        } catch (Exception e) {
            log.error("Failed to send tenant-ready email to {}", to, e);
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
     * Send subscription activation confirmation email (Wave flow-kh3-2, GAP-974).
     *
     * <p>Sent after a paid tier is activated on an already-existing subscription
     * (the upgrade flow of {@code applyPendingUpgrade}) — the create flow uses
     * {@link #sendSubscriptionCreatedEmail}. Best-effort: a send failure is logged
     * and never blocks the caller's transaction.</p>
     *
     * @param instanceId Instance ID
     * @param to Recipient email
     * @param organizationName Organization name
     * @param tier Activated subscription tier
     * @param expiresAt Subscription expiry date (display string)
     */
    public void sendSubscriptionActivatedEmail(UUID instanceId, String to, String organizationName,
                                               String tier, String expiresAt) {
        if (alreadySentToday(instanceId, "subscription-activated", to)) {
            log.debug("Subscription-activated email already sent today to {}, skipping", to);
            return;
        }
        log.info("Sending subscription activated email to {}", to);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("[KiteHub] Gói " + tier + " đã kích hoạt")
                .templateName("subscription-activated")
                .variables(Map.of(
                    "organizationName", organizationName,
                    "tier", tier,
                    "expiresAt", expiresAt == null ? "" : expiresAt,
                    "supportUrl", "https://kitehub.vn/dashboard"
                ))
                .build();

            dispatchEmail(instanceId, "subscription-activated", request);
        } catch (Exception e) {
            log.error("Failed to send subscription activated email to {}", to, e);
        }
    }

    /**
     * Send DSAR new-ticket alert to DPO inbox (PDPL Art 14 push notification).
     *
     * <p>Per `business-logic-review.md` BR-PDPL-DSAR-006 (config key
     * `kitehub.dsar.dpo-email`) and BR-PDPL-DSAR-002 (20-day SLA from Decree
     * 13/2023/NĐ-CP Art 19). Outbox-first via {@link #publishToQueue} per
     * `design-patterns.md` §3.5.1 Exception A — outbox is the reliability net.</p>
     *
     * @param ticketUuid DSAR ticket UUID
     * @param dpoEmail DPO recipient email (from `kitehub.dsar.dpo-email` config)
     * @param rightType DSAR right type (ACCESS/RECTIFICATION/ERASURE/PORTABILITY/RESTRICT/OBJECT)
     * @param requesterEmail Requester email (logged for ops triage)
     * @param slaDeadline 20-day SLA deadline per BR-PDPL-DSAR-002
     */
    public void sendDsarNewTicketDpoEmail(UUID ticketUuid, String dpoEmail, String rightType,
                                          String requesterEmail, OffsetDateTime slaDeadline) {
        if (alreadySentToday(null, "dsar-new-ticket-dpo", dpoEmail)) {
            log.info("Email already sent today: dsar-new-ticket-dpo to {}", dpoEmail);
            return;
        }

        log.info("Sending DSAR new-ticket alert to DPO {}: ticket={} rightType={}",
            dpoEmail, ticketUuid, rightType);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(dpoEmail)
                .subject(String.format("[DSAR] Yêu cầu mới (%s) - SLA hạn %s",
                    rightType, slaDeadline))
                .templateName("dsar-new-ticket-dpo")
                .variables(Map.of(
                    "ticketUuid", ticketUuid.toString(),
                    "rightType", rightType,
                    "requesterEmail", requesterEmail,
                    "slaDeadline", slaDeadline.toString(),
                    "dpoQueueUrl", "https://kitehub.vn/admin/dsar"
                ))
                .build();

            dispatchEmail(null, "dsar-new-ticket-dpo", request);
        } catch (Exception e) {
            log.error("Failed to send DSAR DPO alert to {}", dpoEmail, e);
        }
    }

    /**
     * Send DSAR acknowledgement to requester (PDPL Art 14 ticket confirmation).
     *
     * <p>Per BR-PDPL-DSAR-002 (20-day SLA) + BR-PDPL-DSAR-003 (identity verification flow).
     * Outbox-first via {@link #publishToQueue} per `design-patterns.md` §3.5.1
     * Exception A — outbox is the reliability net.</p>
     *
     * @param ticketUuid DSAR ticket UUID (also serves as status check token)
     * @param requesterEmail Requester recipient email
     * @param requesterName Requester full name
     * @param rightType DSAR right type
     * @param slaDeadline 20-day SLA deadline
     */
    public void sendDsarAcknowledgementEmail(UUID ticketUuid, String requesterEmail,
                                             String requesterName, String rightType,
                                             OffsetDateTime slaDeadline) {
        if (alreadySentToday(null, "dsar-acknowledgement-requester", requesterEmail)) {
            log.info("Email already sent today: dsar-acknowledgement-requester to {}", requesterEmail);
            return;
        }

        log.info("Sending DSAR acknowledgement to {}: ticket={} rightType={}",
            requesterEmail, ticketUuid, rightType);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(requesterEmail)
                .subject(String.format("Xác nhận yêu cầu DSAR - Mã ticket %s", ticketUuid))
                .templateName("dsar-acknowledgement-requester")
                .variables(Map.of(
                    "ticketUuid", ticketUuid.toString(),
                    "requesterName", requesterName,
                    "rightType", rightType,
                    "slaDeadline", slaDeadline.toString(),
                    "statusCheckUrl",
                        String.format("https://kitehub.vn/legal/data-rights/status?id=%s", ticketUuid)
                ))
                .build();

            dispatchEmail(null, "dsar-acknowledgement-requester", request);
        } catch (Exception e) {
            log.error("Failed to send DSAR acknowledgement to {}", requesterEmail, e);
        }
    }

    /**
     * Send admin-new-login-alert email (GAP-517 / OWASP A07 §2.5).
     *
     * <p>Dispatched by {@code AdminLoginAlertEventListener} when a PLATFORM_ADMIN
     * logs in from a fingerprint not seen within the last 24h. Best-effort —
     * email failure must NEVER propagate back to the login flow (caller wraps
     * this in try/catch).</p>
     *
     * <p>Template {@code admin-new-login-alert} configuration in Resend is an
     * ops follow-up (deploy gap); if absent, the underlying email-service will
     * log a warning and the call is a no-op.</p>
     *
     * @param to admin email address (recipient)
     * @param ip source IP of the new login
     * @param userAgent User-Agent string of the new login
     * @param loginAt timestamp of the new login (formatted for display)
     */
    public void sendAdminNewLoginAlert(String to, String ip, String userAgent,
                                       LocalDateTime loginAt) {
        log.info("Sending admin-new-login-alert to {} (ip={})", to, ip);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("[KiteHub] New admin login from unrecognized device")
                .templateName("admin-new-login-alert")
                .variables(Map.of(
                    "ip", ip == null ? "unknown" : ip,
                    "userAgent", userAgent == null ? "unknown" : userAgent,
                    "loginAt", loginAt == null ? "" : loginAt.toString(),
                    "supportUrl", "https://kitehub.me/support"
                ))
                .build();

            dispatchEmail(null, "admin-new-login-alert", request);
        } catch (Exception e) {
            log.warn("Failed to send admin-new-login-alert to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send beta-invite email (GAP-702 Wave 104 B1).
     *
     * <p>Dispatched by {@link com.kitehub.subscription.beta.service.BetaAccessService#approveRequest}
     * after the coordinator flips a {@code BetaAccessRequest} to APPROVED. Uses
     * the {@code beta-invite} template (HTML + plain-text siblings already shipped
     * under {@code kitehub-email/templates/emails/}) so the recipient gets the
     * 6-digit claim code (per GAP-388 388-B 2FA) without the raw UUID token in
     * the email body.</p>
     *
     * <p>Per {@code design-patterns.md §3.5.1 Exception A}: outbox-first via
     * {@link #publishToQueue} — the existing custom {@code beta.invite.sent}
     * outbox event from {@link com.kitehub.subscription.beta.service.BetaAccessService}
     * is retained for downstream consumers (audit / analytics) but the actual
     * email delivery now flows through the {@code email.queued} pipeline (sister
     * to all other transactional emails), restoring delivery after the missing
     * kitehub-email RabbitListener was discovered in Wave 103 verify.</p>
     *
     * <p>Best-effort — email failure must NEVER propagate back to the approve
     * transaction (caller wraps in try/catch); the {@code BetaAccessRequest}
     * row already persisted the claim code so the coordinator can re-send via
     * a follow-up endpoint if dispatch fails.</p>
     *
     * @param to recipient email
     * @param recipientName recipient full name (for greeting)
     * @param orgName organization name (optional — falls back to empty)
     * @param claimCode 6-digit claim code rendered in email body
     * @param signupUrl absolute signup page URL where invitee enters the claim code
     * @param expiresAt human-readable expiry (e.g. "Thứ Hai, 22/05/2026 lúc 14:30")
     */
    public void sendBetaInviteEmail(String to, String recipientName, String orgName,
                                    String claimCode, String signupUrl, String expiresAt) {
        log.info("Sending beta-invite email to {} (claimCode masked)", to);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject("Mã truy cập Beta KiteHub của bạn")
                .templateName("beta-invite")
                // GAP-797: template var-name contract — canonical set is
                // claimCode / inviteUrl / expiresAt (matches both beta-invite.txt
                // and beta-invite.html). Key MUST be `inviteUrl` (NOT `signupUrl`)
                // or Thymeleaf falls to the ?: default and the email ships without
                // the real signup link. unsubscribeUrl injected here because the
                // renderer only auto-injects `branding`.
                .variables(Map.of(
                    "recipientName", recipientName == null ? "bạn" : recipientName,
                    "orgName", orgName == null ? "" : orgName,
                    "claimCode", claimCode == null ? "" : claimCode,
                    "inviteUrl", signupUrl == null ? "" : signupUrl,
                    "expiresAt", expiresAt == null ? "" : expiresAt,
                    "unsubscribeUrl", "https://kitehub.me/unsubscribe"
                ))
                .build();

            dispatchEmail(null, "beta-invite", request);
        } catch (Exception e) {
            log.warn("Failed to send beta-invite email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send invite-staff email (GAP-561b Wave 80).
     *
     * <p>Dispatches {@code invite-staff} template with VN-locale variables.
     * Best-effort — email failure must NEVER propagate back to invitation
     * creation flow (caller wraps in try/catch). When dispatch fails, the
     * invitation row is still valid; the owner can re-send via the
     * {@code /api/v1/staff-invitations/{id}/resend} endpoint.</p>
     *
     * @param to recipient email
     * @param recipientName recipient full name (for greeting)
     * @param ownerName owner full name (issuer)
     * @param tenantName tenant display name (e.g. "Trung tâm Sky Education")
     * @param role role label rendered in email (defaults to STAFF)
     * @param inviteUrl absolute accept-invite URL (carries opaque HMAC token)
     * @param expiresAt human-readable expiry (e.g. "Thứ Hai, 22/05/2026")
     */
    public void sendInviteStaffEmail(String to, String recipientName, String ownerName,
                                     String tenantName, String role, String inviteUrl,
                                     String expiresAt) {
        log.info("Sending invite-staff email to {} for tenant {}", to, tenantName);

        try {
            EmailRequest request = EmailRequest.builder()
                .to(to)
                .subject(String.format("Bạn được mời tham gia %s trên KiteHub",
                    tenantName == null ? "trung tâm" : tenantName))
                .templateName("invite-staff")
                .variables(Map.of(
                    "recipientName", recipientName == null ? "bạn" : recipientName,
                    "ownerName", ownerName == null ? "Chủ trung tâm" : ownerName,
                    "tenantName", tenantName == null ? "KiteHub" : tenantName,
                    "role", role == null ? "STAFF" : role,
                    "inviteUrl", inviteUrl == null ? "" : inviteUrl,
                    "expiresAt", expiresAt == null ? "" : expiresAt
                ))
                .build();

            dispatchEmail(null, "invite-staff", request);
        } catch (Exception e) {
            log.warn("Failed to send invite-staff email to {}: {}", to, e.getMessage());
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

        // GAP-922 fix: eventEmitter.emit() ALREADY writes outbox row + does best-effort
        // fast-path convertAndSend internally (see SubscriptionEventEmitter line 88-100).
        // Previously, this method ALSO did rabbitTemplate.convertAndSend AGAIN immediately
        // after → 2 publishes per call → consumer fires 2x → 2 emails sent.
        //
        // Single emit() call is sufficient: outbox row + fast-path publish in one atomic
        // step. Dispatcher catches up if fast-path fails (rabbit down at dispatch moment).
        //
        // Per design-patterns.md §3.5.1 Exception A — "outbox is the reliability net".
        eventEmitter.emit(instanceId, EVENT_TYPE_EMAIL_QUEUED,
            EmailQueueConfig.EMAIL_ROUTING_KEY, payload);
        log.debug("Email event queued: type={}, to={}", emailType, request.getTo());
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
