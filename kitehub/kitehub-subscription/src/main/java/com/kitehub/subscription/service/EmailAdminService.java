package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.EmailSentLog;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.EmailConfigProperties;
import com.kitehub.subscription.dto.EmailConfigResponse;
import com.kitehub.subscription.dto.EmailHistoryResponse;
import com.kitehub.subscription.dto.EmailStatsResponse;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for admin email monitoring and control operations.
 * <p>
 * Provides:
 * - Email history browsing with pagination and filters
 * - Aggregate statistics (sent counts, failure counts, by type)
 * - Email type toggle configuration (enable/disable specific email types)
 * - Manual email triggering for specific instances
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAdminService {

    private final EmailSentLogRepository emailSentLogRepository;
    private final EmailConfigProperties emailConfigProperties;
    private final EmailServiceClient emailServiceClient;
    private final InstanceRepository instanceRepository;

    /**
     * Get paginated email history with optional filters.
     *
     * @param instanceId filter by instance (nullable for all instances)
     * @param emailType  filter by email type pattern (nullable for all types)
     * @param from       start of time range (nullable, defaults to 30 days ago)
     * @param to         end of time range (nullable, defaults to now)
     * @param page       page number (0-based)
     * @param size       page size
     * @return paginated email history
     */
    @Transactional(readOnly = true)
    public Page<EmailHistoryResponse> getEmailHistory(UUID instanceId, String emailType,
                                                       LocalDateTime from, LocalDateTime to,
                                                       int page, int size) {
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));

        Page<EmailSentLog> logs;

        if (instanceId != null && emailType != null && !emailType.isBlank()) {
            logs = emailSentLogRepository.findByInstanceIdAndEmailTypeContainingAndSentAtBetween(
                instanceId, emailType, effectiveFrom, effectiveTo, pageable);
        } else if (instanceId != null) {
            logs = emailSentLogRepository.findByInstanceIdAndSentAtBetween(
                instanceId, effectiveFrom, effectiveTo, pageable);
        } else {
            logs = emailSentLogRepository.findBySentAtBetween(
                effectiveFrom, effectiveTo, pageable);
        }

        return logs.map(this::toHistoryResponse);
    }

    /**
     * Get aggregate email statistics for admin dashboard.
     *
     * @return email stats (sent today, this week, failures, by type)
     */
    @Transactional(readOnly = true)
    public EmailStatsResponse getEmailStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LocalDateTime startOfWeek = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay();

        long totalSentToday = emailSentLogRepository.countBySentAtBetween(startOfDay, endOfDay);
        long totalSentThisWeek = emailSentLogRepository.countBySentAtBetween(startOfWeek, endOfDay);
        long failedToday = emailSentLogRepository.countByEmailTypeContainingAndSentAtBetween(
            ":FAILED", startOfDay, endOfDay);

        // Group by email type for today
        List<Object[]> grouped = emailSentLogRepository.countByEmailTypeGrouped(startOfDay, endOfDay);
        Map<String, Long> countByType = new LinkedHashMap<>();
        for (Object[] row : grouped) {
            countByType.put((String) row[0], (Long) row[1]);
        }

        return EmailStatsResponse.builder()
            .totalSentToday(totalSentToday)
            .totalSentThisWeek(totalSentThisWeek)
            .failedToday(failedToday)
            .countByType(countByType)
            .build();
    }

    /**
     * Get current email configuration (queue mode + type toggles).
     *
     * @return email config state
     */
    public EmailConfigResponse getEmailConfig() {
        return EmailConfigResponse.builder()
            .queueEnabled(emailConfigProperties.isUseQueue())
            .emailTypeToggles(new LinkedHashMap<>(emailConfigProperties.getTypeToggles()))
            .build();
    }

    /**
     * Update email type toggles at runtime (in-memory).
     * <p>
     * Note: This updates the in-memory config only. To persist across restarts,
     * update application.yml or environment variables.
     *
     * @param toggles map of email type to enabled/disabled
     * @return updated email config
     */
    public EmailConfigResponse updateEmailConfig(Map<String, Boolean> toggles) {
        log.info("Admin updating email config toggles: {}", toggles);
        emailConfigProperties.getTypeToggles().putAll(toggles);
        return getEmailConfig();
    }

    /**
     * Manually trigger a specific email type for an instance.
     * Used by admin to re-send or force-send an email.
     *
     * @param instanceId target instance ID
     * @param emailType  email type to trigger
     * @throws EntityNotFoundException if instance not found
     * @throws IllegalArgumentException if email type is not recognized
     */
    public void triggerEmail(UUID instanceId, String emailType) {
        log.info("Admin manually triggering email: type={}, instanceId={}", emailType, instanceId);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + instanceId));

        // Idempotency: check if same email already sent today for this instance
        String recipient = instance.getContactEmail();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        boolean alreadySent = emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                instanceId, emailType, recipient, startOfDay, endOfDay);
        if (alreadySent) {
            throw new IllegalStateException(
                    "Email type '" + emailType + "' already sent today for instance " + instanceId);
        }
        String orgName = instance.getOrganizationName();

        switch (emailType) {
            case "trial-warning" -> emailServiceClient.sendTrialExpirationWarning(
                instanceId, recipient, orgName, 1);
            case "trial-expired" -> emailServiceClient.sendTrialExpired(
                instanceId, recipient, orgName);
            case "renewal-reminder" -> emailServiceClient.sendRenewalReminder(
                instanceId, recipient, orgName, 1, "UNKNOWN", 0);
            case "suspension-notification" -> emailServiceClient.sendSuspensionNotification(
                instanceId, recipient, orgName);
            case "retention-warning" -> emailServiceClient.sendRetentionWarning(
                instanceId, recipient, orgName, 7);
            case "retention-final-warning" -> emailServiceClient.sendDataRetentionFinalWarning(
                instanceId, recipient, instance.getSubdomain());
            case "data-deleted" -> emailServiceClient.sendDataDeletedNotification(
                instanceId, recipient, orgName);
            case "welcome" -> emailServiceClient.sendWelcomeEmail(
                instanceId, recipient, orgName, 14, "N/A");
            case "trial-midpoint" -> emailServiceClient.sendTrialMidpointEmail(
                instanceId, recipient, instance.getSubdomain());
            case "onboarding-tips" -> emailServiceClient.sendOnboardingTipsEmail(
                instanceId, recipient, instance.getSubdomain());
            case "subscription-expired" -> emailServiceClient.sendSubscriptionExpiredEmail(
                instanceId, recipient, instance.getSubdomain());
            case "subscription-created" -> emailServiceClient.sendSubscriptionCreatedEmail(
                instanceId, recipient, orgName, "UNKNOWN", "UNKNOWN");
            default -> throw new IllegalArgumentException("Unknown email type: " + emailType);
        }

        log.info("Admin triggered email successfully: type={}, instanceId={}, to={}",
            emailType, instanceId, recipient);
    }

    /**
     * Convert EmailSentLog entity to EmailHistoryResponse DTO.
     * Derives status from emailType (":FAILED" suffix = FAILED, else SUCCESS).
     */
    private EmailHistoryResponse toHistoryResponse(EmailSentLog log) {
        String emailType = log.getEmailType();
        String status;
        String displayType;

        if (emailType != null && emailType.endsWith(":FAILED")) {
            status = "FAILED";
            displayType = emailType.replace(":FAILED", "");
        } else {
            status = "SUCCESS";
            displayType = emailType;
        }

        return EmailHistoryResponse.builder()
            .id(log.getId())
            .instanceId(log.getInstanceId())
            .emailType(displayType)
            .recipient(log.getRecipient())
            .sentAt(log.getSentAt())
            .status(status)
            .build();
    }
}
