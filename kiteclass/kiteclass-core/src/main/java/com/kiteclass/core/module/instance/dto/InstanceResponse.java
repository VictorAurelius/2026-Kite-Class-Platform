package com.kiteclass.core.module.instance.dto;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;

import java.time.Instant;

/**
 * Serialisable view of a {@link FrontendInstance} for REST responses.
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4)
 */
public record InstanceResponse(
        Long id,
        String tenantId,
        String slug,
        String frontendUrl,
        FrontendInstanceStatus status,
        Integer retryCount,
        String failureReason,
        Integer brandingVersion,
        Instant initializingAt,
        Instant generatingAt,
        Instant deployedAt,
        Instant lastRegenerateAt,
        Instant failedAt) {

    public static InstanceResponse from(FrontendInstance instance) {
        return new InstanceResponse(
                instance.getId(),
                instance.getTenantSlug(),
                instance.getSlug(),
                instance.getFrontendUrl(),
                instance.getStatus(),
                instance.getRetryCount(),
                instance.getFailureReason(),
                instance.getBrandingVersion(),
                instance.getInitializingAt(),
                instance.getGeneratingAt(),
                instance.getDeployedAt(),
                instance.getLastRegenerateAt(),
                instance.getFailedAt()
        );
    }
}
