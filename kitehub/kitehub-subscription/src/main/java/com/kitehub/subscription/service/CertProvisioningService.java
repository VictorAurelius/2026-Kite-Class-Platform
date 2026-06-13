package com.kitehub.subscription.service;

/**
 * Seam for TLS certificate provisioning during the custom-domain verification flow
 * (GAP-1024 — state machine completion).
 *
 * <p>Strategy / Adapter pattern (per {@code design-patterns.md} §2 — multiple
 * implementations, swap via config). The custom-domain state machine transitions
 * {@code PENDING_VERIFY → CERT_PROVISIONING → VERIFIED}: once DNS ownership is proven,
 * a certificate must actually be issued before the domain is "really serving". This
 * interface is the async boundary behind which the real ACM / Cloudflare Custom
 * Hostname call lives.</p>
 *
 * <p><strong>Phase 1 BETA:</strong> only {@link StubCertProvisioningService} exists —
 * it auto-issues synchronously so the state machine reaches VERIFIED locally without a
 * real cert authority. The real AWS ACM / Cloudflare-for-SaaS integration is deferred to
 * Phase 1.5+ (vendor dependency per BR-DOMAIN-008 + ADR-018). When the real adapter lands,
 * {@link #requestCertificate(String)} returns {@link CertProvisioningResult.CertStatus#PENDING}
 * and a poll/webhook flips CERT_PROVISIONING → VERIFIED out of band.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public interface CertProvisioningService {

    /**
     * Request a TLS certificate for the (already DNS-verified) custom domain.
     *
     * <p>Implementations MUST be idempotent — the verification flow may re-invoke this
     * for an instance already in {@code CERT_PROVISIONING} (poll / retry).</p>
     *
     * @param domain the custom domain to issue a certificate for (e.g., "lop.skyedu.vn")
     * @return result describing whether the cert is ISSUED (→ VERIFIED), still PENDING
     *         (→ stay CERT_PROVISIONING), or FAILED (→ FAILED)
     */
    CertProvisioningResult requestCertificate(String domain);
}
