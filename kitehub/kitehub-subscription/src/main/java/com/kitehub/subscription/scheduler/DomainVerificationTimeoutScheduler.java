package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.config.DomainVerificationConfig;
import com.kitehub.subscription.repository.DomainVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled sweep that closes the custom-domain verification state machine's timeout edge
 * (GAP-1024, BR-DOMAIN-003): an instance left in {@code PENDING_VERIFY} for longer than the
 * configured timeout (the tenant never added the DNS TXT record) is moved to {@code FAILED}
 * so it stops sitting in limbo forever and the tenant can re-initiate (BR-DOMAIN-004).
 *
 * <p>Runs hourly. "Stale" is measured from {@code updatedAt} (BaseEntity {@code @LastModifiedDate})
 * — the last time the instance row changed. A tenant actively polling {@code verifyCustomDomain}
 * keeps the clock warm; an abandoned verification ages out after {@code timeout-hours}.
 * NOTE: {@code updatedAt} is bumped by ANY instance write, so this is a "no recent activity"
 * timeout rather than a strict "since initiate" timeout. A dedicated
 * {@code domainVerifyInitiatedAt} column would make it exact but requires an instances-table
 * migration (triad per {@code instances-table-triad-discipline.md}) — deferred to Phase 1.5+.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainVerificationTimeoutScheduler {

    private final DomainVerificationRepository domainVerificationRepository;
    private final DomainVerificationConfig domainVerificationConfig;

    /**
     * Sweep stale PENDING_VERIFY instances → FAILED. Runs at the top of every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireStalePendingVerifications() {
        int timeoutHours = domainVerificationConfig.getTimeoutHours();
        LocalDateTime threshold = LocalDateTime.now().minusHours(timeoutHours);

        List<Instance> stale = domainVerificationRepository
            .findByDomainStatusAndUpdatedAtBeforeAndDeletedFalse(
                Instance.DomainStatus.PENDING_VERIFY, threshold);

        if (stale.isEmpty()) {
            log.debug("Domain verification timeout sweep: no stale PENDING_VERIFY instances (timeout={}h)",
                timeoutHours);
            return;
        }

        for (Instance instance : stale) {
            instance.setDomainStatus(Instance.DomainStatus.FAILED);
            domainVerificationRepository.save(instance);
            log.info("Domain verification timed out for instance '{}' (domain '{}', no TXT in {}h) → FAILED",
                instance.getId(), instance.getCustomDomain(), timeoutHours);
        }

        log.info("Domain verification timeout sweep: {} instance(s) marked FAILED (timeout={}h)",
            stale.size(), timeoutHours);
    }

    /**
     * Manually trigger the timeout sweep (testing / ops). Delegates to the scheduled method.
     */
    public void triggerManualSweep() {
        log.info("Manual domain verification timeout sweep triggered");
        expireStalePendingVerifications();
    }
}
