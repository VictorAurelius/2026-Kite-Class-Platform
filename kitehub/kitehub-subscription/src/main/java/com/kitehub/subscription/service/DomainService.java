package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.config.DomainVerificationConfig;
import com.kitehub.subscription.dto.DomainVerifyResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing custom domain setup and DNS verification.
 *
 * Business rules:
 * - Only PREMIUM and ENTERPRISE instances can use custom domains
 * - Flow: initiate → user adds TXT record → verify → active
 * - Token format: kitehub-verify={uuid}
 * - Timeout: configurable (default 48h)
 * - Backup URL (subdomain.kiteclass.com) always works in parallel
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DomainService {

    private final InstanceRepository instanceRepository;
    private final DomainVerificationConfig domainVerificationConfig;
    private final DnsTxtLookupService dnsTxtLookupService;

    /**
     * Initiate custom domain setup for an instance.
     * Generates a DNS TXT verification token and saves PENDING_VERIFY status.
     *
     * @param instanceId   the instance UUID
     * @param customDomain the custom domain to set up (e.g., "school.example.com")
     * @return DomainVerifyResponse containing the token and instructions
     * @throws EntityNotFoundException  if instance not found
     * @throws IllegalArgumentException if tier does not allow custom domain, or domain already in use
     */
    public DomainVerifyResponse initiateCustomDomain(UUID instanceId, String customDomain) {
        log.info("Initiating custom domain '{}' for instance '{}'", customDomain, instanceId);

        Instance instance = findInstanceOrThrow(instanceId);

        // Check tier permission
        if (!instance.canUseCustomDomain()) {
            throw new IllegalArgumentException(
                "Custom domain is only available for PREMIUM and ENTERPRISE tiers. " +
                "Current tier: " + instance.getTier()
            );
        }

        // Check domain uniqueness (another instance cannot use the same custom domain)
        // Allow re-initiation if this instance already owns the domain
        instanceRepository.findByCustomDomainAndDeletedFalse(customDomain)
            .ifPresent(existing -> {
                if (!existing.getId().equals(instanceId)) {
                    throw new IllegalArgumentException(
                        "Domain '" + customDomain + "' is already in use by another instance"
                    );
                }
            });

        // Generate verification token: kitehub-verify={uuid}
        String token = "kitehub-verify=" + UUID.randomUUID();

        // Update instance
        instance.setCustomDomain(customDomain);
        instance.setDomainVerifyToken(token);
        instance.setDomainStatus(Instance.DomainStatus.PENDING_VERIFY);
        instance.setDomainVerifiedAt(null);
        instanceRepository.save(instance);

        log.info("Domain verification initiated for instance '{}', domain '{}', token '{}'",
            instanceId, customDomain, token);

        return buildResponse(instance, customDomain, token);
    }

    /**
     * Verify custom domain by checking DNS TXT record.
     * In mock mode: if DNS is not resolvable, returns PENDING (does not fail hard).
     * In production: checks actual TXT record matches the token.
     *
     * @param instanceId the instance UUID
     * @return DomainVerifyResponse with updated status
     * @throws EntityNotFoundException  if instance not found
     * @throws IllegalArgumentException if no domain verification is pending
     */
    public DomainVerifyResponse verifyCustomDomain(UUID instanceId) {
        log.info("Verifying custom domain for instance '{}'", instanceId);

        Instance instance = findInstanceOrThrow(instanceId);

        if (instance.getDomainStatus() != Instance.DomainStatus.PENDING_VERIFY
            || instance.getCustomDomain() == null
            || instance.getDomainVerifyToken() == null) {
            throw new IllegalArgumentException(
                "No domain verification pending for instance: " + instanceId
            );
        }

        String domain = instance.getCustomDomain();
        String expectedToken = instance.getDomainVerifyToken();

        // Attempt DNS TXT lookup
        boolean verified = checkDnsTxtRecord(domain, expectedToken);

        if (verified) {
            instance.setDomainStatus(Instance.DomainStatus.VERIFIED);
            instance.setDomainVerifiedAt(LocalDateTime.now());
            log.info("Domain '{}' verified for instance '{}'", domain, instanceId);
        } else {
            // Mock mode: stay PENDING instead of marking FAILED
            // This prevents hard failures in environments where DNS is not accessible
            if (!domainVerificationConfig.isMockMode()) {
                // Check timeout: if past timeoutHours, mark FAILED
                // (Timeout logic would be implemented in a scheduled job)
                instance.setDomainStatus(Instance.DomainStatus.PENDING_VERIFY);
                log.info("Domain '{}' TXT record not found yet for instance '{}', staying PENDING",
                    domain, instanceId);
            } else {
                instance.setDomainStatus(Instance.DomainStatus.PENDING_VERIFY);
                log.info("Mock mode: domain '{}' DNS not resolvable, keeping PENDING for instance '{}'",
                    domain, instanceId);
            }
        }

        instanceRepository.save(instance);
        return buildResponse(instance, instance.getCustomDomain(), instance.getDomainVerifyToken());
    }

    /**
     * Remove custom domain from an instance. Clears all domain fields.
     *
     * @param instanceId the instance UUID
     * @throws EntityNotFoundException if instance not found
     */
    public void removeCustomDomain(UUID instanceId) {
        log.info("Removing custom domain for instance '{}'", instanceId);

        Instance instance = findInstanceOrThrow(instanceId);

        instance.setCustomDomain(null);
        instance.setDomainVerifyToken(null);
        instance.setDomainVerifiedAt(null);
        instance.setDomainStatus(Instance.DomainStatus.NONE);
        instanceRepository.save(instance);

        log.info("Custom domain removed for instance '{}'", instanceId);
    }

    /**
     * Get current domain status for an instance.
     *
     * @param instanceId the instance UUID
     * @return DomainVerifyResponse with current status
     * @throws EntityNotFoundException if instance not found
     */
    @Transactional(readOnly = true)
    public DomainVerifyResponse getDomainStatus(UUID instanceId) {
        Instance instance = findInstanceOrThrow(instanceId);
        return buildResponse(instance, instance.getCustomDomain(), instance.getDomainVerifyToken());
    }

    // =========================================================
    // Private helpers
    // =========================================================

    private Instance findInstanceOrThrow(UUID instanceId) {
        return instanceRepository.findById(instanceId)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + instanceId));
    }

    /**
     * Check DNS TXT record for the given domain via {@link DnsTxtLookupService} (JNDI).
     *
     * <p>Per GAP-812 §Phần A: replaces previous stub (returned {@code false} always).
     * Looks up TXT record at {@code _kitehub-verify.{domain}} (preferred) or apex.
     * Returns {@code false} (not throws) on lookup failure — state machine handles.</p>
     *
     * @param domain        the domain to check
     * @param expectedToken the expected TXT record value (e.g., "kitehub-verify=abc123")
     * @return true if TXT record found and matches, false otherwise
     */
    private boolean checkDnsTxtRecord(String domain, String expectedToken) {
        return dnsTxtLookupService.verifyTxtRecord(domain, expectedToken);
    }

    private DomainVerifyResponse buildResponse(Instance instance, String customDomain, String token) {
        String verifyRecord = null;
        if (token != null && customDomain != null) {
            verifyRecord = String.format(
                "Add TXT record to your DNS: @ %s  (or _kitehub-verify.%s)", token, customDomain
            );
        }

        String backupUrl = "https://" + instance.getSubdomain() + ".kiteclass.com";

        return DomainVerifyResponse.builder()
            .customDomain(customDomain)
            .verifyToken(token)
            .verifyRecord(verifyRecord)
            .status(instance.getDomainStatus() != null ? instance.getDomainStatus() : Instance.DomainStatus.NONE)
            .verifiedAt(instance.getDomainVerifiedAt())
            .backupUrl(backupUrl)
            .build();
    }
}
