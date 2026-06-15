package com.kitehub.branding.queue;

import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.dto.BrandingJobMessage;
import com.kitehub.branding.service.AIBrandingProcessor;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for branding jobs.
 * <p>
 * Listens to branding-jobs queue and processes jobs asynchronously.
 * Failed jobs are sent to Dead Letter Queue for manual inspection.
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrandingJobConsumer {

    private final BrandingJobService jobService;
    private final AIBrandingProcessor brandingProcessor;

    private static final int MAX_RETRIES = 3;

    /**
     * Process branding job from queue.
     *
     * @param message job message
     */
    @RabbitListener(queues = RabbitMQConfig.BRANDING_QUEUE)
    public void processJob(BrandingJobMessage message) {
        log.info("Received branding job: {}", message.getJobId());

        // GAP-1020 (Part 1) — background consumers carry no HTTP request, so bind the tenant from
        // the job's instanceId. Without it the TenantAwareDataSourceInterceptor would leave the RLS
        // GUC unset → default-deny under a non-superuser DB role (latent today: owner role bypasses).
        if (message.getInstanceId() != null) {
            TenantContext.setCurrentTenant(message.getInstanceId());
        }

        try {
            // Update status to PROCESSING
            jobService.updateJobProgress(
                    message.getJobId(),
                    JobStatus.PROCESSING,
                    0,
                    "Starting AI branding generation"
            );

            // Process the job
            brandingProcessor.processJob(message);

            // Mark as completed
            jobService.updateJobProgress(
                    message.getJobId(),
                    JobStatus.COMPLETED,
                    100,
                    "Branding generation completed"
            );

            log.info("Job {} completed successfully", message.getJobId());

        } catch (Exception e) {
            log.error("Job {} failed: {}", message.getJobId(), e.getMessage(), e);

            // Mark as failed
            jobService.markJobFailed(message.getJobId(), e.getMessage());

            // Re-throw to trigger DLQ routing
            throw new RuntimeException("Job processing failed: " + e.getMessage(), e);
        } finally {
            // GAP-1020 (Part 1) — clear so a pooled consumer thread never leaks tenant to next job.
            TenantContext.clear();
        }
    }

    /**
     * Handle failed jobs from DLQ.
     * <p>
     * This listener logs failed jobs for manual inspection.
     *
     * @param message job message
     */
    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void handleFailedJob(BrandingJobMessage message) {
        log.error("Job {} moved to DLQ after {} retries", message.getJobId(), MAX_RETRIES);
        // Additional alerting logic (email, Slack, PagerDuty) can be added here
    }
}
