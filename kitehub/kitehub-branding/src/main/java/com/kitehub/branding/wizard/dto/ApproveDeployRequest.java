package com.kitehub.branding.wizard.dto;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/branding/jobs/{jobId}/approve} (GAP-1021
 * pt1 — persist approved theme + trigger deploy). The owner approves the
 * generated resources and clicks "Triển khai trang web"; the backend persists
 * the theme and drives the MOCK provisioning lifecycle to DEPLOYED.
 *
 * <p>{@code slug} builds the placeholder {@code frontendUrl}
 * ({@code https://{slug}.kitehub.me}). All fields optional — the backend
 * falls back to job-derived values when absent.</p>
 *
 * @since GAP-1021 (Phase 1 deploy pipeline mock)
 */
public record ApproveDeployRequest(
        String slug,
        String templateId,
        List<String> approvedResources
) {
}
