package com.kitehub.subscription.service;

/**
 * Outcome of a {@link CertProvisioningService#requestCertificate(String)} call.
 *
 * <p>Maps to the custom-domain state machine downstream of DNS verification:
 * <ul>
 *   <li>{@link CertStatus#ISSUED} → instance moves CERT_PROVISIONING → VERIFIED</li>
 *   <li>{@link CertStatus#PENDING} → instance stays CERT_PROVISIONING (real async issuance in flight)</li>
 *   <li>{@link CertStatus#FAILED} → instance moves to FAILED (cert authority rejected)</li>
 * </ul>
 *
 * @param status the provisioning status
 * @param detail optional human-readable detail (cert ARN, error message, etc.)
 */
public record CertProvisioningResult(CertStatus status, String detail) {

    public enum CertStatus {
        ISSUED, PENDING, FAILED
    }

    public static CertProvisioningResult issued(String detail) {
        return new CertProvisioningResult(CertStatus.ISSUED, detail);
    }

    public static CertProvisioningResult pending(String detail) {
        return new CertProvisioningResult(CertStatus.PENDING, detail);
    }

    public static CertProvisioningResult failed(String detail) {
        return new CertProvisioningResult(CertStatus.FAILED, detail);
    }

    public boolean isIssued() {
        return status == CertStatus.ISSUED;
    }

    public boolean isFailed() {
        return status == CertStatus.FAILED;
    }
}
