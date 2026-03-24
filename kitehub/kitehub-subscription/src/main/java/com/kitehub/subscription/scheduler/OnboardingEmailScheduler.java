package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler for sending onboarding tips emails.
 * Sends onboarding email ~24h after trial activation.
 * Runs hourly to catch instances in the 23-25h activation window.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingEmailScheduler {

    private final InstanceRepository instanceRepository;
    private final EmailServiceClient emailServiceClient;

    /**
     * Check and send onboarding emails for instances that activated 23-25 hours ago.
     * Runs every hour to find instances within the time window.
     * Uses alreadySentToday guard in EmailServiceClient for idempotency.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkAndSendOnboardingEmails() {
        log.info("Starting hourly onboarding email check");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusHours(25);
        LocalDateTime windowEnd = now.minusHours(23);

        List<Instance> trialInstances = instanceRepository
            .findByStatusAndDeletedFalse(InstanceStatus.TRIAL);

        int sent = 0;
        for (Instance instance : trialInstances) {
            try {
                if (isInOnboardingWindow(instance, windowStart, windowEnd)) {
                    if (instance.getContactEmail() == null) {
                        log.warn("Skipping onboarding email for instance {} - no contact email",
                            instance.getId());
                        continue;
                    }

                    emailServiceClient.sendOnboardingTipsEmail(
                        instance.getId(),
                        instance.getContactEmail(),
                        instance.getSubdomain()
                    );
                    sent++;
                    log.info("Onboarding email sent for instance {} (subdomain: {})",
                        instance.getId(), instance.getSubdomain());
                }
            } catch (Exception e) {
                log.error("Failed to send onboarding email for instance: {}", instance.getId(), e);
            }
        }

        log.info("Onboarding email check completed. Sent: {}", sent);
    }

    /**
     * Check if instance's trial start time falls within the onboarding window (23-25h after activation).
     *
     * @param instance Instance to check
     * @param windowStart Start of the time window (25h ago)
     * @param windowEnd End of the time window (23h ago)
     * @return true if instance should receive onboarding email
     */
    private boolean isInOnboardingWindow(Instance instance, LocalDateTime windowStart,
                                          LocalDateTime windowEnd) {
        LocalDateTime trialStartedAt = instance.getTrialStartedAt();
        if (trialStartedAt == null) {
            return false;
        }
        return trialStartedAt.isAfter(windowStart) && trialStartedAt.isBefore(windowEnd);
    }
}
