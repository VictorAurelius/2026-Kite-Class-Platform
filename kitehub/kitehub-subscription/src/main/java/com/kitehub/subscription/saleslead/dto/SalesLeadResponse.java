package com.kitehub.subscription.saleslead.dto;

import com.kitehub.subscription.saleslead.entity.SalesLead;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response body for {@code POST /api/platform/sales-leads} (GAP-1101).
 *
 * <p>Minimal receipt — intentionally does NOT echo back PII
 * ({@code email}/{@code phone}/{@code message}) to reduce payload + avoid leak
 * via browser cache. Mirrors {@code FeedbackSubmissionResponse} precedent.</p>
 *
 * @since GAP-1101
 */
public record SalesLeadResponse(
        UUID id,
        String fullName,
        String organizationName,
        String planInterest,
        String status,
        OffsetDateTime createdAt
) {
    public static SalesLeadResponse from(SalesLead entity) {
        return new SalesLeadResponse(
                entity.getPublicId(),
                entity.getFullName(),
                entity.getOrganizationName(),
                entity.getPlanInterest(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
