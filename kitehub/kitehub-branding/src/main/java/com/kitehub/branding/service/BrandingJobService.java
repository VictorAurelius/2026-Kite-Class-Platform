package com.kitehub.branding.service;

import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.dto.BrandingJobMessage;
import com.kitehub.branding.lifecycle.InstanceLifecycleService;
import com.kitehub.branding.lifecycle.LifecycleState;
import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
import com.kitehub.branding.outbox.BrandingEventEmitter;
import com.kitehub.branding.repository.BrandingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing branding jobs.
 * <p>
 * Handles job creation, queuing, status updates, and retrieval.
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandingJobService {

    private final BrandingJobRepository jobRepository;
    private final BrandingEventEmitter outboxEmitter;
    private final InstanceLifecycleService lifecycleService;
    private final BrandingInstanceStateRepository instanceStateRepository;

    /**
     * Create and queue a new branding job.
     *
     * @param instanceId instance ID
     * @param organizationName organization name
     * @param language language code
     * @param logoUrl logo S3 URL
     * @return created job
     */
    @Transactional
    public BrandingJob createJob(UUID instanceId, String organizationName, String language, String logoUrl) {
        log.info("Creating branding job for instance: {}", instanceId);

        // Create job entity
        BrandingJob job = new BrandingJob();
        job.setInstanceId(instanceId);
        job.setOrganizationName(organizationName);
        job.setLanguage(language);
        job.setLogoUrl(logoUrl);
        job.setStatus(JobStatus.QUEUED);
        job.setProgress(0);
        job.setCurrentStep("Queued");
        job.setRetryCount(0);
        job.setQueuedAt(LocalDateTime.now());

        job = jobRepository.save(job);

        // §6 lifecycle compliance hinge — drive instance lifecycle via service.
        // First job: NOT_STARTED → INITIALIZING. Subsequent on a DEPLOYED instance:
        // DEPLOYED → REGENERATING. FAILED → INITIALIZING (retry path) handled identically.
        LifecycleState target = resolveCreateTarget(instanceId);
        Map<String, Object> meta = new HashMap<>();
        meta.put("jobId", job.getId());
        meta.put("organizationName", organizationName);
        lifecycleService.transition(
            instanceId, target, InstanceLifecycleService.Actor.system("branding-job-service"), meta);

        BrandingJobMessage message = new BrandingJobMessage(
                job.getId(),
                instanceId,
                organizationName,
                language,
                logoUrl
        );

        // Per design-patterns.md §3.5.1: outbox-row first (reliability net),
        // then best-effort fast-path publish handled inside the emitter.
        outboxEmitter.emit(
                job.getId(),
                instanceId,
                "branding.job.queued",
                RabbitMQConfig.BRANDING_EXCHANGE,
                RabbitMQConfig.BRANDING_ROUTING_KEY,
                message
        );

        log.info("Branding job queued: {}", job.getId());
        return job;
    }

    /**
     * Get job by ID and instance ID.
     *
     * @param jobId job ID
     * @param instanceId instance ID
     * @return job or null
     */
    public BrandingJob getJob(UUID jobId, UUID instanceId) {
        return jobRepository.findByIdAndInstanceId(jobId, instanceId).orElse(null);
    }

    /**
     * Get all jobs for instance.
     *
     * @param instanceId instance ID
     * @return list of jobs
     */
    public List<BrandingJob> getJobsByInstance(UUID instanceId) {
        return jobRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId);
    }

    /**
     * Update job status and progress.
     *
     * @param jobId job ID
     * @param status new status
     * @param progress progress percentage (0-100)
     * @param currentStep current step description
     */
    @Transactional
    public void updateJobProgress(UUID jobId, JobStatus status, int progress, String currentStep) {
        jobRepository.findById(jobId).ifPresent(job -> {
            JobStatus previous = job.getStatus();
            job.setStatus(status);
            job.setProgress(progress);
            job.setCurrentStep(currentStep);

            if (status == JobStatus.PROCESSING && job.getStartedAt() == null) {
                job.setStartedAt(LocalDateTime.now());
            }

            if (status == JobStatus.COMPLETED || status == JobStatus.FAILED || status == JobStatus.CANCELLED) {
                job.setCompletedAt(LocalDateTime.now());
            }

            jobRepository.save(job);

            // §6 compliance hinge — only the service mutates lifecycle.
            // QUEUED → PROCESSING: instance INITIALIZING/REGENERATING → GENERATING.
            // PROCESSING → COMPLETED: instance GENERATING → DEPLOYED.
            if (previous != status) {
                if (status == JobStatus.PROCESSING) {
                    transitionInstance(job.getInstanceId(), LifecycleState.GENERATING,
                        Map.of("jobId", jobId, "step", currentStep));
                } else if (status == JobStatus.COMPLETED) {
                    transitionInstance(job.getInstanceId(), LifecycleState.DEPLOYED,
                        Map.of("jobId", jobId));
                }
            }
            log.debug("Job {} updated: status={}, progress={}%", jobId, status, progress);
        });
    }

    /**
     * Mark job as failed with error message.
     *
     * @param jobId job ID
     * @param errorMessage error message
     */
    @Transactional
    public void markJobFailed(UUID jobId, String errorMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            job.setRetryCount(job.getRetryCount() + 1);
            jobRepository.save(job);

            // §6 compliance hinge — failure path drives instance to FAILED.
            transitionInstance(job.getInstanceId(), LifecycleState.FAILED,
                Map.of("jobId", jobId, "reason", errorMessage == null ? "unknown" : errorMessage));

            log.error("Job {} failed: {}", jobId, errorMessage);
        });
    }

    /**
     * Resolve the correct target lifecycle state when a new job is created
     * for an instance, based on its current lifecycle state.
     */
    private LifecycleState resolveCreateTarget(UUID instanceId) {
        BrandingInstanceState current = instanceStateRepository.findById(instanceId).orElse(null);
        if (current == null || current.getState() == LifecycleState.NOT_STARTED) {
            return LifecycleState.INITIALIZING;
        }
        if (current.getState() == LifecycleState.DEPLOYED) {
            return LifecycleState.REGENERATING;
        }
        if (current.getState() == LifecycleState.FAILED) {
            return LifecycleState.INITIALIZING; // retry
        }
        // INITIALIZING / GENERATING / REGENERATING already in flight: re-emit INITIALIZING is invalid.
        // Treat duplicate create as no-op for lifecycle (job row still saved separately).
        return current.getState();
    }

    /**
     * Wrap lifecycle transition; tolerate idempotent same-state calls so the
     * job row commit is not aborted by a stale lifecycle write.
     */
    private void transitionInstance(UUID instanceId, LifecycleState target,
                                    Map<String, Object> metadata) {
        try {
            BrandingInstanceState current = instanceStateRepository.findById(instanceId).orElse(null);
            if (current != null && current.getState() == target) {
                return;
            }
            lifecycleService.transition(
                instanceId, target,
                InstanceLifecycleService.Actor.system("branding-job-service"),
                metadata);
        } catch (IllegalStateException ex) {
            // Log + swallow — invalid transition typically means a duplicate consumer
            // delivery or out-of-order event; do not break the job-row write.
            log.warn("Skipping lifecycle transition instance={} target={}: {}",
                instanceId, target, ex.getMessage());
        }
    }

    /**
     * Cancel job.
     *
     * @param jobId job ID
     * @param instanceId instance ID
     * @return true if cancelled, false if not found or already completed
     */
    @Transactional
    public boolean cancelJob(UUID jobId, UUID instanceId) {
        return jobRepository.findByIdAndInstanceId(jobId, instanceId)
                .map(job -> {
                    if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.PROCESSING) {
                        job.setStatus(JobStatus.CANCELLED);
                        job.setCompletedAt(LocalDateTime.now());
                        jobRepository.save(job);
                        log.info("Job {} cancelled", jobId);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Update generated assets (JSON).
     *
     * @param jobId job ID
     * @param assetsJson JSON string of generated assets
     */
    @Transactional
    public void updateGeneratedAssets(UUID jobId, String assetsJson) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setAssetsGenerated(assetsJson);
            jobRepository.save(job);
            log.info("Job {} assets updated", jobId);
        });
    }
}
