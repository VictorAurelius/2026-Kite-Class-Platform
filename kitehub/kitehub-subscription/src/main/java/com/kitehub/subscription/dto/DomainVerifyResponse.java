package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.entity.Instance;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for custom domain verification status.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
public class DomainVerifyResponse {

    /**
     * The custom domain being set up.
     */
    private String customDomain;

    /**
     * The verification token (format: kitehub-verify={uuid}).
     * Customer must add this as a DNS TXT record.
     */
    private String verifyToken;

    /**
     * Human-readable instruction for the TXT DNS record to add.
     * Example: "Add TXT record: @ kitehub-verify=abc123"
     */
    private String verifyRecord;

    /**
     * Current status of domain verification.
     */
    private Instance.DomainStatus status;

    /**
     * When domain was verified (null if not yet verified).
     */
    private LocalDateTime verifiedAt;

    /**
     * Backup subdomain URL (always available).
     * Format: {subdomain}.kitehub.me
     */
    private String backupUrl;
}
