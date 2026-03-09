package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.UpdateInstanceRequest;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing instances.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InstanceService {

    private final InstanceRepository instanceRepository;

    /**
     * Create a new trial instance.
     *
     * @param request create instance request
     * @return created instance response
     */
    public InstanceResponse createTrialInstance(CreateInstanceRequest request) {
        log.info("Creating trial instance for subdomain: {}", request.getSubdomain());

        // Validate subdomain uniqueness
        if (instanceRepository.existsBySubdomainAndDeletedFalse(request.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists: " + request.getSubdomain());
        }

        // Validate custom domain (only PREMIUM/ENTERPRISE)
        if (request.getCustomDomain() != null && !request.getTier().allowsCustomDomain()) {
            throw new IllegalArgumentException("Custom domain is only available for PREMIUM and ENTERPRISE tiers");
        }

        // Create instance entity
        Instance instance = new Instance();
        instance.setSubdomain(request.getSubdomain());
        instance.setOrganizationName(request.getOrganizationName());
        instance.setOwnerId(request.getOwnerId());
        instance.setTier(request.getTier());
        instance.setCustomDomain(request.getCustomDomain());

        // Set placeholder database credentials (will be provisioned in PR 4.2)
        instance.setDatabaseUrl("jdbc:postgresql://kitehub-postgres:5432/kiteclass_" + UUID.randomUUID().toString().substring(0, 8));
        instance.setDatabaseUsername("kiteclass_user");
        instance.setDatabasePassword("encrypted_password_placeholder");

        // Start trial
        instance.startTrial();

        // Save
        Instance saved = instanceRepository.save(instance);

        log.info("Created trial instance: {} (expires: {})", saved.getId(), saved.getTrialExpiresAt());

        return toResponse(saved);
    }

    /**
     * Get instance by ID.
     *
     * @param id instance UUID
     * @return instance response
     */
    @Transactional(readOnly = true)
    public InstanceResponse getInstanceById(UUID id) {
        Instance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + id));

        if (instance.isDeleted()) {
            throw new IllegalArgumentException("Instance has been deleted: " + id);
        }

        return toResponse(instance);
    }

    /**
     * Get instance by subdomain.
     *
     * @param subdomain subdomain
     * @return instance response
     */
    @Transactional(readOnly = true)
    public InstanceResponse getInstanceBySubdomain(String subdomain) {
        Instance instance = instanceRepository.findBySubdomainAndDeletedFalse(subdomain)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + subdomain));

        return toResponse(instance);
    }

    /**
     * Get all instances for owner.
     *
     * @param ownerId owner UUID
     * @return list of instance responses
     */
    @Transactional(readOnly = true)
    public List<InstanceResponse> getInstancesByOwner(UUID ownerId) {
        return instanceRepository.findByOwnerIdAndDeletedFalse(ownerId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Update instance.
     *
     * @param id instance UUID
     * @param request update request
     * @return updated instance response
     */
    public InstanceResponse updateInstance(UUID id, UpdateInstanceRequest request) {
        log.info("Updating instance: {}", id);

        Instance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + id));

        if (instance.isDeleted()) {
            throw new IllegalArgumentException("Cannot update deleted instance: " + id);
        }

        // Update organization name
        if (request.getOrganizationName() != null) {
            instance.setOrganizationName(request.getOrganizationName());
        }

        // Update tier
        if (request.getTier() != null) {
            instance.setTier(request.getTier());
        }

        // Update custom domain (validate tier)
        if (request.getCustomDomain() != null) {
            if (!instance.canUseCustomDomain()) {
                throw new IllegalArgumentException("Custom domain requires PREMIUM or ENTERPRISE tier");
            }
            instance.setCustomDomain(request.getCustomDomain());
        }

        Instance updated = instanceRepository.save(instance);

        log.info("Updated instance: {}", id);

        return toResponse(updated);
    }

    /**
     * Delete instance (soft delete).
     *
     * @param id instance UUID
     */
    public void deleteInstance(UUID id) {
        log.info("Deleting instance: {}", id);

        Instance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + id));

        if (instance.isDeleted()) {
            throw new IllegalArgumentException("Instance already deleted: " + id);
        }

        instance.softDelete();
        instance.setStatus(InstanceStatus.DELETED);
        instanceRepository.save(instance);

        log.info("Deleted instance: {}", id);
    }

    /**
     * Suspend expired trials (scheduled job).
     */
    public void suspendExpiredTrials() {
        List<Instance> expiredTrials = instanceRepository.findExpiredTrials(LocalDateTime.now());

        log.info("Found {} expired trials to suspend", expiredTrials.size());

        for (Instance instance : expiredTrials) {
            instance.suspend();
            instanceRepository.save(instance);
            log.info("Suspended expired trial: {} (subdomain: {})", instance.getId(), instance.getSubdomain());
        }
    }

    /**
     * Suspend expired subscriptions (scheduled job).
     */
    public void suspendExpiredSubscriptions() {
        List<Instance> expiredSubscriptions = instanceRepository.findExpiredSubscriptions(LocalDateTime.now());

        log.info("Found {} expired subscriptions to suspend", expiredSubscriptions.size());

        for (Instance instance : expiredSubscriptions) {
            instance.suspend();
            instanceRepository.save(instance);
            log.info("Suspended expired subscription: {} (subdomain: {})", instance.getId(), instance.getSubdomain());
        }
    }

    /**
     * Convert entity to response DTO.
     *
     * @param instance instance entity
     * @return instance response
     */
    private InstanceResponse toResponse(Instance instance) {
        return InstanceResponse.builder()
            .id(instance.getId())
            .subdomain(instance.getSubdomain())
            .customDomain(instance.getCustomDomain())
            .organizationName(instance.getOrganizationName())
            .ownerId(instance.getOwnerId())
            .tier(instance.getTier())
            .status(instance.getStatus())
            .trialStartedAt(instance.getTrialStartedAt())
            .trialExpiresAt(instance.getTrialExpiresAt())
            .trialDaysLeft(instance.getTrialDaysLeft())
            .subscriptionId(instance.getSubscriptionId())
            .subscriptionExpiresAt(instance.getSubscriptionExpiresAt())
            .isActive(instance.isActive())
            .isOnTrial(instance.isOnTrial())
            .createdAt(instance.getCreatedAt())
            .updatedAt(instance.getUpdatedAt())
            .build();
    }
}
