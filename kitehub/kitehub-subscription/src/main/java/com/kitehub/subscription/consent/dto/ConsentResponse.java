package com.kitehub.subscription.consent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kitehub.subscription.consent.entity.ConsentRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response payload for consent endpoints. Mirrors {@link ConsentRecord} but
 * intentionally excludes raw {@code ipAddress} / {@code userAgent} (PDPL Art 6
 * data-minimisation — clients don't need these back).
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsentResponse {

    private Long id;
    private UUID visitorId;
    private Long userId;
    private UUID tenantId;
    private Boolean essentialConsented;
    private Boolean analyticsConsented;
    private Boolean marketingConsented;
    private Integer consentVersion;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime revokedAt;
    private Boolean revoked;

    public static ConsentResponse from(ConsentRecord record) {
        return ConsentResponse.builder()
                .id(record.getId())
                .visitorId(record.getVisitorId())
                .userId(record.getUserId())
                .tenantId(record.getTenantId())
                .essentialConsented(record.getEssentialConsented())
                .analyticsConsented(record.getAnalyticsConsented())
                .marketingConsented(record.getMarketingConsented())
                .consentVersion(record.getConsentVersion())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .expiresAt(record.getExpiresAt())
                .revokedAt(record.getRevokedAt())
                .revoked(record.isRevoked())
                .build();
    }
}
