package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.config.DomainVerificationConfig;
import com.kitehub.subscription.dto.DomainVerifyResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing custom domain setup and DNS verification.
 *
 * Business rules:
 * - Only PREMIUM and ENTERPRISE instances can use custom domains
 * - State machine (BR-DOMAIN-002): NONE → PENDING_VERIFY → CERT_PROVISIONING → VERIFIED;
 *   FAILED reachable from any non-terminal state; re-initiate resets FAILED → PENDING_VERIFY
 * - Token format: kitehub-verify={uuid}
 * - Timeout: configurable (default 48h, BR-DOMAIN-003) — PENDING_VERIFY → FAILED via
 *   {@code DomainVerificationTimeoutScheduler}
 * - Backup URL (subdomain.kitehub.me) always works in parallel (BR-DOMAIN-007)
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
    private final CertProvisioningService certProvisioningService;

    /**
     * GAP-1414: canonical public app base-url. The tenant backup URL (BR-DOMAIN-007) is the
     * subdomain landing on the platform domain; host is derived from this config instead of a
     * hardcoded {@code kitehub.me} literal so it stays in sync with email/notification links.
     */
    @Value("${kitehub.app.base-url:https://kitehub.me}")
    private String appBaseUrl;

    // KH-7 FM-5: a tenant must not be able to claim the platform's own domains as their
    // custom domain (no denylist previously — `kitehub.me` was accepted). Block the
    // platform apex domains and any subdomain of them.
    // Platform-domain claim denylist (protective — CANONICAL-BANNED values a tenant
    // must not claim as a custom domain, NOT tenant landing URLs).
    private static final Set<String> RESERVED_DOMAINS = Set.of(
        "kitehub.me", "kitehub.vn", "kitehub.com", "kiteclass.com", "kiteclass.me", "kiteclass.vn" // stale-domain-ok: reserved-claim denylist, not a tenant URL
    );

    private boolean isReservedDomain(String domain) {
        String d = domain.toLowerCase();
        return RESERVED_DOMAINS.stream().anyMatch(r -> d.equals(r) || d.endsWith("." + r));
    }

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

        // KH-7 FM-5: reject platform-reserved domains (a tenant can't claim kitehub.me etc.)
        if (isReservedDomain(customDomain)) {
            throw new IllegalArgumentException(
                "Domain '" + customDomain + "' is reserved by the platform and cannot be used as a custom domain"
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

        // Update instance — (re)enters PENDING_VERIFY (BR-DOMAIN-004: also the FAILED → retry path)
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
     * Verify custom domain by advancing the verification state machine
     * (BR-DOMAIN-002: PENDING_VERIFY → CERT_PROVISIONING → VERIFIED).
     *
     * <p>Idempotent (GAP-1024): re-verifying an already-VERIFIED domain returns the current
     * state (HTTP 200 no-op) instead of throwing. Re-verifying a CERT_PROVISIONING domain
     * re-polls certificate issuance. PENDING_VERIFY does a DNS TXT lookup; on success it
     * enters CERT_PROVISIONING then requests a cert (stub auto-issues → VERIFIED). NONE /
     * FAILED (or no domain set) → IllegalArgumentException (use {@link #initiateCustomDomain}
     * to (re)start).</p>
     *
     * <p>In mock mode, an unresolved DNS lookup keeps the instance PENDING_VERIFY (does not
     * fail hard). The {@code DomainVerificationTimeoutScheduler} flips PENDING_VERIFY → FAILED
     * after the configured timeout (BR-DOMAIN-003).</p>
     *
     * @param instanceId the instance UUID
     * @return DomainVerifyResponse with updated status
     * @throws EntityNotFoundException  if instance not found
     * @throws IllegalArgumentException if no domain verification is pending
     */
    public DomainVerifyResponse verifyCustomDomain(UUID instanceId) {
        log.info("Verifying custom domain for instance '{}'", instanceId);

        Instance instance = findInstanceOrThrow(instanceId);
        Instance.DomainStatus status = instance.getDomainStatus();

        // Idempotent: already VERIFIED → no-op (GAP-1024 — previously threw 400)
        if (status == Instance.DomainStatus.VERIFIED) {
            log.info("Domain '{}' already VERIFIED for instance '{}' — idempotent no-op",
                instance.getCustomDomain(), instanceId);
            return buildResponse(instance, instance.getCustomDomain(), instance.getDomainVerifyToken());
        }

        // Idempotent: certificate provisioning in flight → re-poll issuance (no DNS re-check)
        if (status == Instance.DomainStatus.CERT_PROVISIONING) {
            provisionCertAndAdvance(instance);
            instanceRepository.save(instance);
            return buildResponse(instance, instance.getCustomDomain(), instance.getDomainVerifyToken());
        }

        // Only PENDING_VERIFY is DNS-verifiable. NONE / FAILED / no-domain → nothing pending
        // (FAILED must re-initiate to regenerate token per BR-DOMAIN-004).
        if (status != Instance.DomainStatus.PENDING_VERIFY
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
            // DNS ownership proven → enter CERT_PROVISIONING, then request the cert.
            instance.setDomainStatus(Instance.DomainStatus.CERT_PROVISIONING);
            log.info("Domain '{}' DNS TXT verified for instance '{}' → CERT_PROVISIONING", domain, instanceId);
            provisionCertAndAdvance(instance);
        } else {
            // No TXT yet — stay PENDING_VERIFY; timeout scheduler flips FAILED after timeout-hours.
            instance.setDomainStatus(Instance.DomainStatus.PENDING_VERIFY);
            if (domainVerificationConfig.isMockMode()) {
                log.info("Mock mode: domain '{}' DNS not resolvable, keeping PENDING for instance '{}'",
                    domain, instanceId);
            } else {
                log.info("Domain '{}' TXT record not found yet for instance '{}', staying PENDING "
                        + "(timeout job will FAIL after {}h)",
                    domain, instanceId, domainVerificationConfig.getTimeoutHours());
            }
        }

        instanceRepository.save(instance);
        return buildResponse(instance, instance.getCustomDomain(), instance.getDomainVerifyToken());
    }

    /**
     * Request a TLS certificate for the (DNS-verified) domain and advance the state machine:
     * ISSUED → VERIFIED (+ domainVerifiedAt); PENDING → stay CERT_PROVISIONING (real async
     * issuance in flight); FAILED → FAILED. The instance is mutated in place; caller persists.
     *
     * @param instance the instance currently entering/in CERT_PROVISIONING
     */
    private void provisionCertAndAdvance(Instance instance) {
        String domain = instance.getCustomDomain();
        CertProvisioningResult cert = certProvisioningService.requestCertificate(domain);

        if (cert.isIssued()) {
            instance.setDomainStatus(Instance.DomainStatus.VERIFIED);
            instance.setDomainVerifiedAt(LocalDateTime.now());
            log.info("Domain '{}' certificate issued ({}) for instance '{}' → VERIFIED",
                domain, cert.detail(), instance.getId());
        } else if (cert.isFailed()) {
            instance.setDomainStatus(Instance.DomainStatus.FAILED);
            log.warn("Domain '{}' certificate provisioning FAILED ({}) for instance '{}' → FAILED",
                domain, cert.detail(), instance.getId());
        } else {
            // PENDING — real async issuance in flight; stay CERT_PROVISIONING (re-poll on next verify).
            instance.setDomainStatus(Instance.DomainStatus.CERT_PROVISIONING);
            log.info("Domain '{}' certificate provisioning PENDING ({}) for instance '{}' — staying CERT_PROVISIONING",
                domain, cert.detail(), instance.getId());
        }
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

        // GAP-1414: derive host from configured base-url; interpolate tenant subdomain.
        // e.g. https://kitehub.me → https://{subdomain}.kitehub.me
        String backupUrl = appBaseUrl.replaceFirst("://", "://" + instance.getSubdomain() + ".");

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
