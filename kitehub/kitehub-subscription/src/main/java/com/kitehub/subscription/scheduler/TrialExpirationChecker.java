package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.service.TrialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job to check and handle trial expirations.
 * Runs daily at 8:00 AM.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrialExpirationChecker {

    private final InstanceRepository instanceRepository;
    private final TrialService trialService;
    private final EmailServiceClient emailServiceClient;
    private final TrialConfig trialConfig;

    /**
     * Check all trial instances and suspend expired ones.
     * Also sends warning notifications for trials expiring soon.
     * Runs daily at 8:00 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkExpiredTrials() {
        log.info("Starting daily trial expiration check");

        LocalDateTime now = LocalDateTime.now();

        // Find all expired trials
        List<Instance> expiredTrials = instanceRepository.findExpiredTrials(now);
        log.info("Found {} expired trials", expiredTrials.size());

        int suspendedCount = 0;
        for (Instance instance : expiredTrials) {
            try {
                trialService.suspendExpiredTrial(instance.getId());
                suspendedCount++;

                // Send trial expired email notification
                emailServiceClient.sendTrialExpired(
                    instance.getContactEmail(),
                    instance.getOrganizationName()
                );
                log.info("Trial expired notification sent to instance: {} (subdomain: {})",
                    instance.getId(), instance.getSubdomain());

            } catch (Exception e) {
                log.error("Failed to suspend expired trial for instance: {}", instance.getId(), e);
            }
        }

        log.info("Suspended {} expired trials", suspendedCount);

        // Check for trials needing warnings
        checkTrialWarnings();

        log.info("Daily trial expiration check completed");
    }

    /**
     * Check trials that need warning notifications.
     * Sends warnings at 3 days, 1 day before expiration.
     * Also sends midpoint engagement email at configured midpoint day.
     */
    private void checkTrialWarnings() {
        log.debug("Checking for trials needing warnings");

        List<Instance> activeTrials = instanceRepository.findByStatusAndDeletedFalse(InstanceStatus.TRIAL);

        int warningsSent = 0;
        int midpointEmailsSent = 0;

        for (Instance instance : activeTrials) {
            long daysLeft = instance.getTrialDaysLeft();

            if (shouldSendWarning(daysLeft)) {
                String warningType = getWarningType(daysLeft);

                // Send trial expiration warning email
                emailServiceClient.sendTrialExpirationWarning(
                    instance.getContactEmail(),
                    instance.getOrganizationName(),
                    daysLeft
                );
                log.info("Trial warning sent: {} for instance: {} (subdomain: {}, days left: {})",
                    warningType, instance.getId(), instance.getSubdomain(), daysLeft);

                warningsSent++;
            }

            // Check midpoint engagement email
            if (shouldSendMidpointEmail(instance) && instance.getContactEmail() != null) {
                emailServiceClient.sendTrialMidpointEmail(
                    instance.getId(),
                    instance.getContactEmail(),
                    instance.getSubdomain()
                );
                log.info("Trial midpoint email sent for instance: {} (subdomain: {})",
                    instance.getId(), instance.getSubdomain());
                midpointEmailsSent++;
            }
        }

        log.info("Identified {} trials needing warnings, {} midpoint emails sent",
            warningsSent, midpointEmailsSent);
    }

    /**
     * Check if the midpoint engagement email should be sent.
     * Sends when the instance has been in trial for exactly midpointDay days.
     *
     * @param instance Trial instance to check
     * @return true if midpoint email should be sent
     */
    private boolean shouldSendMidpointEmail(Instance instance) {
        if (instance.getTrialStartedAt() == null) {
            return false;
        }
        long daysSinceStart = ChronoUnit.DAYS.between(instance.getTrialStartedAt(), LocalDateTime.now());
        return daysSinceStart == trialConfig.getMidpointDay();
    }

    /**
     * Check if warning should be sent based on days left.
     *
     * @param daysLeft Number of days left in trial
     * @return true if warning should be sent
     */
    private boolean shouldSendWarning(long daysLeft) {
        // Send warnings at configurable days before expiration
        return trialConfig.getWarningDays().contains((int) daysLeft);
    }

    /**
     * Get warning type based on days left.
     *
     * @param daysLeft Number of days left in trial
     * @return Warning type description
     */
    private String getWarningType(long daysLeft) {
        if (daysLeft == 1) {
            return "LAST_DAY"; // "Last day of your trial"
        } else if (daysLeft <= 3) {
            return "ENDING_SOON"; // "Your trial is ending in 3 days"
        } else {
            return "NONE";
        }
    }

    /**
     * Manually trigger trial expiration check.
     * Useful for testing or manual operations.
     */
    public void triggerManualCheck() {
        log.info("Manual trial expiration check triggered");
        checkExpiredTrials();
    }
}
