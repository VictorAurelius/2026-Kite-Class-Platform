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
        // Tier-less overload (draft auto-create / legacy callers) → FREE/TEMPLATE path.
        return createJob(instanceId, organizationName, language, logoUrl, null);
    }

    /**
     * Create + enqueue a branding job carrying the subscription {@code tier}
     * (GAP-1117 / GAP-1119) so the processor routes TEMPLATE vs FULL_AI. Tier is
     * sourced from the gateway {@code X-Subscription-Tier} header (ADR-039);
     * {@code null} → FREE/TEMPLATE (FULL_AI only PREMIUM + ENTERPRISE).
     */
    public BrandingJob createJob(UUID instanceId, String organizationName, String language,
                                 String logoUrl, String tier) {
        log.info("Creating branding job for instance: {} (tier={})", instanceId, tier);

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
                logoUrl,
                job.getOrgType(), // GAP-1115 orgType (nullable)
                tier              // GAP-1117/1119 tier — drives TEMPLATE vs FULL_AI (null → FREE)
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
     * Create a wizard branding job for the Phase 1 MOCK provisioning flow
     * (GAP-1021 + GAP-272e). Distinct from {@link #createJob} which enqueues the
     * heavy async AI pipeline via the outbox/RabbitMQ {@code branding.job.queued}
     * topic — this method deliberately SKIPS that enqueue.
     *
     * <p><b>Instance binding (GAP-1021 runtime fix):</b> the job binds to the
     * caller's REAL instance ({@code instanceId} resolved from the JWT tenant
     * claim) — the wizard rebrands the owner's existing instance. The earlier
     * mock used a synthetic random UUID which violated the
     * {@code fk_branding_job_instance} FK → HTTP 500 in production Postgres.
     * Real per-tenant isolated infrastructure (per-tenant DB / MinIO bucket /
     * DNS subdomain) is still deferred to GAP-1055, and subdomain Host-render to
     * GAP-811/1077; deploy is driven by
     * {@link com.kitehub.branding.wizard.service.MockProvisioningService}.</p>
     *
     * <p>Drives the §6 instance lifecycle to {@code INITIALIZING} (NOT_STARTED →
     * INITIALIZING) so the deploy step (PROCESSING → GENERATING, COMPLETED →
     * DEPLOYED via {@link #updateJobProgress}) has a legal starting state.</p>
     *
     * @param instanceId caller's real instance (from JWT tenant claim) — FK target
     * @param organizationName tenant/center display name (drives preview palette)
     * @param language language code (defaults to {@code vi})
     * @param logoUrl optional uploaded logo URL
     * @param orgType wizard user-type axis (GAP-1115): SOLO_TEACHER / SMALL_CENTER /
     *                LARGE_CENTER — nullable for backward-compat
     * @return created job (status {@code QUEUED})
     */
    @Transactional
    public BrandingJob createWizardJob(UUID instanceId, String organizationName, String language,
                                       String logoUrl, String orgType) {
        log.info("Creating wizard branding job for instance: {} (orgType={})", instanceId, orgType);

        BrandingJob job = new BrandingJob();
        job.setInstanceId(instanceId);
        job.setOrganizationName(organizationName);
        job.setLanguage(language == null || language.isBlank() ? "vi" : language);
        job.setLogoUrl(logoUrl);
        job.setOrgType(orgType); // GAP-1115 — nullable user-type axis
        job.setStatus(JobStatus.QUEUED);
        job.setProgress(0);
        job.setCurrentStep("Queued");
        job.setRetryCount(0);
        job.setQueuedAt(LocalDateTime.now());

        job = jobRepository.save(job);

        // §6 lifecycle — state-aware target (GAP-1021 runtime fix): fresh/NOT_STARTED
        // instance → INITIALIZING; existing DEPLOYED instance (rebrand) → REGENERATING.
        // Via transitionInstance (tolerant wrapper) so an in-flight duplicate is a no-op
        // instead of an INVALID_TRANSITION 500. No AI pipeline enqueue (mock).
        Map<String, Object> meta = new HashMap<>();
        meta.put("jobId", job.getId());
        meta.put("organizationName", organizationName);
        meta.put("mock", true);
        transitionInstance(instanceId, resolveCreateTarget(instanceId), meta);

        log.info("Wizard job created: {} (instance {})", job.getId(), instanceId);
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
        BrandingInstanceState current = instanceStateRepository.findById(instanceId).orElse(null);
        LifecycleState from = current == null ? null : current.getState();
        if (from == target) {
            return; // same-state no-op
        }
        // GAP-1021 runtime fix: pre-validate reachability BEFORE calling transition.
        // Calling lifecycleService.transition (a nested @Transactional) with an
        // unreachable target throws IllegalStateException which marks the SHARED
        // parent transaction rollback-only; a catch here swallows the exception but
        // CANNOT clear the flag, so the parent commit then throws
        // UnexpectedRollbackException (audit-service-isolation.md §3.11 class). Skipping
        // the invalid call entirely avoids contaminating the parent txn — e.g. the
        // rebrand deploy path stays REGENERATING through PROCESSING (REGENERATING→
        // GENERATING is skipped) then REGENERATING→DEPLOYED at COMPLETED.
        if (!target.isReachableFrom(from)) {
            log.warn("Skipping lifecycle transition instance={} target={} from={}: not reachable",
                instanceId, target, from);
            return;
        }
        lifecycleService.transition(
            instanceId, target,
            InstanceLifecycleService.Actor.system("branding-job-service"),
            metadata);
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
