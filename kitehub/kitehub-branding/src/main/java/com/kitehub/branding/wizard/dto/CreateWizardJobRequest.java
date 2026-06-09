package com.kitehub.branding.wizard.dto;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/branding/jobs} (Phase 1 wizard create —
 * GAP-1021). Carries the wizard selections captured across steps 1-5 so the
 * created {@link com.kitehub.branding.domain.entity.BrandingJob} reflects the
 * tenant the owner is provisioning.
 *
 * <p>All fields optional except {@code organizationName} (or {@code slug}
 * fallback) which drives the deterministic preview palette in
 * {@link com.kitehub.branding.wizard.quality.BrandColoursDeriver}.</p>
 *
 * @since GAP-1021 (Phase 1 deploy pipeline mock)
 */
public record CreateWizardJobRequest(
        String slug,
        String organizationName,
        String language,
        String audience,
        String tone,
        String templateId,
        String logoUrl,
        Boolean aiLogo,
        List<String> approvedResources
) {
}
