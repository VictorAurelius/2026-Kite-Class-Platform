package com.kiteclass.core.module.instance.service;

import com.kiteclass.core.common.outbox.OutboxEventWriter;
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

    private static final String AGGREGATE_TYPE = "FrontendInstance";

    private final FrontendInstanceRepository repository;
    private final OutboxEventWriter outbox;

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
                .tenantSlug(tenantId)
                .slug(slug)
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0)
                .brandingVersion(0)
                .build();
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        FrontendInstance saved = repository.save(instance);
        emit("instance.initializing", saved);
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
        FrontendInstance saved = repository.save(instance);
        emit("instance.generating", saved);
        log.info("Instance id={} infrastructure ready; generating", instanceId);
        return saved;
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
        FrontendInstance saved = repository.save(instance);
        emit("instance.deployed", saved);
        log.info("Instance id={} deployed url={} version={}",
                instanceId, saved.getFrontendUrl(), saved.getBrandingVersion());
        return saved;
    }

    /**
     * User/admin triggered rebrand — instance stays live with old branding until new one deploys.
     * Transition: DEPLOYED -> REGENERATING.
     */
    @Transactional
    public FrontendInstance rebrand(Long instanceId) {
        FrontendInstance instance = load(instanceId);
        instance.transitionTo(FrontendInstanceStatus.REGENERATING);
        FrontendInstance saved = repository.save(instance);
        emit("instance.regenerating", saved);
        log.info("Instance id={} rebrand triggered", instanceId);
        return saved;
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
        FrontendInstance saved = repository.save(instance);
        emit("instance.failed", saved);
        log.warn("Instance id={} failed (retry={}): {}",
                instanceId, saved.getRetryCount(), reason);
        return saved;
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
        FrontendInstance saved = repository.save(instance);
        emit("instance.initializing", saved);
        log.info("Instance id={} retry attempt {}", instanceId, saved.getRetryCount());
        return saved;
    }

    /**
     * Suspend a live tenant (subscription expired / payment failed). GAP-954.
     * Transition: DEPLOYED -> SUSPENDED. Reversible via {@link #reactivate(Long)}.
     */
    @Transactional
    public FrontendInstance suspend(Long instanceId) {
        FrontendInstance instance = load(instanceId);
        instance.transitionTo(FrontendInstanceStatus.SUSPENDED);
        FrontendInstance saved = repository.save(instance);
        emit("instance.suspended", saved);
        log.info("Instance id={} suspended", instanceId);
        return saved;
    }

    /**
     * Reactivate a suspended tenant back to live. GAP-954.
     * Transition: SUSPENDED -> DEPLOYED (does NOT bump brandingVersion — same branding restored).
     */
    @Transactional
    public FrontendInstance reactivate(Long instanceId) {
        FrontendInstance instance = load(instanceId);
        instance.transitionTo(FrontendInstanceStatus.DEPLOYED);
        FrontendInstance saved = repository.save(instance);
        emit("instance.deployed", saved);
        log.info("Instance id={} reactivated from suspension", instanceId);
        return saved;
    }

    /**
     * Soft-delete a suspended tenant — starts the 30-day PDPL Art 23 retention grace before the
     * cross-service hard purge runs in kitehub-subscription InstancePurgeService. GAP-954.
     * Transition: SUSPENDED -> DELETED (terminal). One-way — file a new instance for re-onboarding.
     */
    @Transactional
    public FrontendInstance softDelete(Long instanceId) {
        FrontendInstance instance = load(instanceId);
        instance.transitionTo(FrontendInstanceStatus.DELETED);
        FrontendInstance saved = repository.save(instance);
        emit("instance.deleted", saved);
        log.info("Instance id={} soft-deleted (30d PDPL Art 23 retention grace started)", instanceId);
        return saved;
    }

    private void emit(String eventType, FrontendInstance instance) {
        String payload = String.format(
                "{\"instanceId\":%d,\"tenantId\":\"%s\",\"slug\":\"%s\",\"status\":\"%s\","
                        + "\"brandingVersion\":%d,\"retryCount\":%d}",
                instance.getId(),
                escape(instance.getTenantSlug()),
                escape(instance.getSlug()),
                instance.getStatus().name(),
                instance.getBrandingVersion(),
                instance.getRetryCount()
        );
        outbox.enqueue(eventType, AGGREGATE_TYPE, String.valueOf(instance.getId()), payload);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private FrontendInstance load(Long instanceId) {
        return repository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "FrontendInstance not found: id=" + instanceId));
    }
}
