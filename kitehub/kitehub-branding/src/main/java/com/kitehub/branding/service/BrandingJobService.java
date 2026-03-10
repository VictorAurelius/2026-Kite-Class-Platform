package com.kitehub.branding.service;

import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.dto.BrandingJobMessage;
import com.kitehub.branding.repository.BrandingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
    private final RabbitTemplate rabbitTemplate;

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

        // Publish to RabbitMQ
        BrandingJobMessage message = new BrandingJobMessage(
                job.getId(),
                instanceId,
                organizationName,
                language,
                logoUrl
        );

        rabbitTemplate.convertAndSend(
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
            log.error("Job {} failed: {}", jobId, errorMessage);
        });
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
