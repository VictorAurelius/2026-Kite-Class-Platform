package com.kiteclass.core.module.instance.service;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * InstanceLifecycleService — single authority for {@link FrontendInstance} state transitions.
 *
 * <p>State Pattern per ADR-004. All transitions go through {@link FrontendInstance#transitionTo}
 * which delegates to {@link FrontendInstanceStatus#canTransitionTo}.
 *
 * <p>Controllers and other services MUST NOT set status directly — always call one of the
 * semantic methods here ({@link #initiate}, {@link #markInfrastructureReady},
 * {@link #markBrandingCompleted}, {@link #rebrand}, {@link #markFailed}).
 *
 * @since 3.15.0 (GAP-009, ADR-004)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstanceLifecycleService {

    /**
     * Max consecutive failed attempts before we stop auto-retry and mark abandoned.
     */
    public static final int MAX_RETRIES = 3;

    private final FrontendInstanceRepository repository;

    /**
     * Create a new instance and begin provisioning.
     * Transition: NOT_STARTED -> INITIALIZING.
     */
    @Transactional
    public FrontendInstance initiate(String tenantId, String slug) {
        if (repository.existsBySlugAndDeletedFalse(slug)) {
            throw new IllegalArgumentException("Slug already in use: " + slug);
        }
        FrontendInstance instance = FrontendInstance.builder()
                .tenantId(tenantId)
                .slug(slug)
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0)
                .brandingVersion(0)
                .build();
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        FrontendInstance saved = repository.save(instance);
        log.info("Initiated instance tenant={} slug={} id={}", tenantId, slug, saved.getId());
        return saved;
    }

    /**
     * Infrastructure (DB schema, storage, DNS) ready — start branding generation.
     * Transition: INITIALIZING -> GENERATING.
     */
    @Transactional
    public FrontendInstance markInfrastructureReady(Long instanceId) {
        FrontendInstance instance = load(instanceId);
        instance.transitionTo(FrontendInstanceStatus.GENERATING);
        log.info("Instance id={} infrastructure ready; generating", instanceId);
        return repository.save(instance);
    }

    /**
     * Branding pipeline completed successfully.
     * Transition: GENERATING|REGENERATING -> DEPLOYED (also bumps brandingVersion).
     */
    @Transactional
    public FrontendInstance markBrandingCompleted(Long instanceId, String frontendUrl) {
        FrontendInstance instance = load(instanceId);
        instance.transitionTo(FrontendInstanceStatus.DEPLOYED);
        if (frontendUrl != null) {
            instance.setFrontendUrl(frontendUrl);
        }
        log.info("Instance id={} deployed url={} version={}",
                instanceId, instance.getFrontendUrl(), instance.getBrandingVersion());
        return repository.save(instance);
    }

    /**
     * User/admin triggered rebrand — instance stays live with old branding until new one deploys.
     * Transition: DEPLOYED -> REGENERATING.
     */
    @Transactional
    public FrontendInstance rebrand(Long instanceId) {
        FrontendInstance instance = load(instanceId);
        instance.transitionTo(FrontendInstanceStatus.REGENERATING);
        log.info("Instance id={} rebrand triggered", instanceId);
        return repository.save(instance);
    }

    /**
     * Transition to FAILED from any pre-DEPLOYED / regenerating state.
     * Transition: INITIALIZING|GENERATING|REGENERATING -> FAILED (retryCount++).
     */
    @Transactional
    public FrontendInstance markFailed(Long instanceId, String reason) {
        FrontendInstance instance = load(instanceId);
        instance.transitionTo(FrontendInstanceStatus.FAILED);
        instance.setFailureReason(reason);
        log.warn("Instance id={} failed (retry={}): {}",
                instanceId, instance.getRetryCount(), reason);
        return repository.save(instance);
    }

    /**
     * Retry a FAILED instance (if under MAX_RETRIES).
     * Transition: FAILED -> INITIALIZING.
     */
    @Transactional
    public FrontendInstance retry(Long instanceId) {
        FrontendInstance instance = load(instanceId);
        if (instance.getRetryCount() >= MAX_RETRIES) {
            throw new IllegalStateException(
                    "Instance id=" + instanceId + " exceeded MAX_RETRIES=" + MAX_RETRIES
            );
        }
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        log.info("Instance id={} retry attempt {}", instanceId, instance.getRetryCount());
        return repository.save(instance);
    }

    private FrontendInstance load(Long instanceId) {
        return repository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "FrontendInstance not found: id=" + instanceId));
    }
}
