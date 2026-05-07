package com.kitehub.branding.wizard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kitehub.branding.domain.entity.BrandingJob;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * v1 jobs/{id} response DTO — Wave 34 Bucket B.
 *
 * <p>Matches {@code documents/01-business/kitehub/ai-branding/api-contract.md}
 * GET {@code /api/v1/branding/jobs/{jobId}} schema. Replaces direct entity
 * exposure used by the legacy {@code /api/platform/branding/jobs/{id}}
 * controller and adds the {@code brandColors} field (sub-GAP-272k).</p>
 *
 * <p>Builder-style static factory keeps the controller free of entity
 * mapping logic. Where data is not yet persisted (regenerateCount,
 * brandingVersion, templateId, audience, tone), the DTO exposes
 * deterministic v0 defaults sourced from job metadata; real values
 * land as the lifecycle service (Bucket C) and saga steps populate
 * the underlying tables.</p>
 *
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrandingJobResponse(
        UUID jobId,
        UUID instanceId,
        String tenantId,
        String status,
        Integer regenerateCount,
        Integer brandingVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BrandColours brandColors,
        String templateId,
        String audience,
        String tone,
        String previewUrl
) {

    /**
     * Map a {@link BrandingJob} entity to the v1 response DTO.
     *
     * @param job persisted job (must not be null)
     * @param brandColors derived colours (sub-GAP-272k); may not be null
     * @return populated response DTO
     */
    public static BrandingJobResponse from(BrandingJob job, BrandColours brandColors) {
        return new BrandingJobResponse(
                job.getId(),
                job.getInstanceId(),
                null, // tenantId not yet available on BrandingJob entity — populated post-Bucket C lifecycle wiring
                mapStatus(job),
                job.getRetryCount(), // proxy until BrandingRegenerateUsage entity (Bucket A) is wired
                1, // v0 default; brandingVersion comes from FrontendInstance after Bucket C
                job.getCreatedAt(),
                job.getUpdatedAt(),
                brandColors,
                null,
                null,
                null,
                "/api/v1/branding/jobs/" + job.getId() + "/preview"
        );
    }

    /**
     * Map the queue-side {@code JobStatus} to the lifecycle vocabulary used
     * in the api-contract: {@code INITIALIZING | GENERATING | REVIEWING |
     * DEPLOYED | REGENERATING | FAILED}.
     *
     * <p>Bucket C will replace this with the authoritative
     * {@code InstanceLifecycleService} state once integrated. Until then,
     * we approximate so FE consumers see contract-shaped strings.</p>
     */
    private static String mapStatus(BrandingJob job) {
        return switch (job.getStatus()) {
            case QUEUED -> "INITIALIZING";
            case PROCESSING -> "GENERATING";
            case COMPLETED -> "DEPLOYED";
            case FAILED -> "FAILED";
            case CANCELLED -> "FAILED";
        };
    }
}
