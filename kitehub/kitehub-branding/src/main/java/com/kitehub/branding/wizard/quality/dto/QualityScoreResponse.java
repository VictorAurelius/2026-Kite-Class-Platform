package com.kitehub.branding.wizard.quality.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Quality-score breakdown per {@code ai-branding-guidelines.md} §5.
 *
 * <p>Contract reference:
 * {@code documents/01-business/kitehub/ai-branding/api-contract.md}
 * GET {@code /api/v1/branding/jobs/{jobId}/quality-score} (sub-GAP-272c).</p>
 *
 * <p>v0 implementation: real WCAG / visual-regression / ML measurement is
 * deferred to GAP-226 / GAP-227 / GAP-228 (Wave 8+). The aggregator returns
 * deterministic placeholder sub-scores derived from job metadata so the
 * wizard preview step and FE polling code work end-to-end.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QualityScoreResponse(
        String jobId,
        int score,
        boolean passed,
        int threshold,
        Map<String, Integer> subscores,
        List<Issue> issues,
        Instant computedAt
) {

    public record Issue(String severity, String check, String message) {}
}
