package com.kitehub.subscription.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Phase 1 BETA stub for {@link CertProvisioningService}.
 *
 * <p>Auto-issues a certificate synchronously so the custom-domain state machine can reach
 * {@code VERIFIED} in local/dev/Phase-1 environments without a real certificate authority.
 * No real ACM / Cloudflare Custom Hostname call is made here.</p>
 *
 * <p><strong>Deferred (Phase 1.5+):</strong> the real adapter (AWS ACM DNS-validated cert
 * OR Cloudflare-for-SaaS Custom Hostname, per BR-DOMAIN-008 + ADR-018) replaces this bean.
 * It will return {@link CertProvisioningResult.CertStatus#PENDING} while issuance is in flight
 * and rely on a poll/webhook to flip the instance CERT_PROVISIONING → VERIFIED out of band.
 * This stub keeps the seam exercised + the happy path reachable until then.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class StubCertProvisioningService implements CertProvisioningService {

    @Override
    public CertProvisioningResult requestCertificate(String domain) {
        // Phase 1 stub: pretend the cert authority issued immediately. Real ACM/Cloudflare
        // issuance is a Phase 1.5+ vendor integration — see CertProvisioningService javadoc.
        log.info("[domain] STUB cert provisioning for '{}' — auto-issuing (real ACM/Cloudflare deferred Phase 1.5+)",
            domain);
        return CertProvisioningResult.issued("stub-cert-auto-issued");
    }
}
