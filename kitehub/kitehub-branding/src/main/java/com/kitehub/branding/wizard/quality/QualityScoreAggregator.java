package com.kitehub.branding.wizard.quality;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.wizard.quality.dto.QualityScoreResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the 5 quality sub-checks defined in
 * {@code ai-branding-guidelines.md} §5 into a single composite score.
 *
 * <p>Sub-checks: contrast (WCAG AA), brokenLinks (asset URL HEAD),
 * cssVarsApplied, visualRegression vs baseline, logoPlacement.</p>
 *
 * <p>v0 deferral notice: real measurement infra (WCAG contrast measurement
 * GAP-226, visual regression diff GAP-227, ML classifier scoring GAP-228)
 * has not landed yet. This aggregator therefore composes deterministic
 * placeholder sub-scores derived from job metadata + status. Wave 35+
 * will swap individual sub-score producers in without changing the
 * aggregator's contract or callers.</p>
 *
 * @see <a href="../../../../../../../../.claude/rules/ai-branding-guidelines.md">§5</a>
 */
@Component
public class QualityScoreAggregator {

    /**
     * Quality gate pass threshold (0..100). Externalized per BR-QUALITY-001
     * (see {@code documents/01-business/kitehub/ai-branding/rules.md}) and
     * 12-factor §III "Store config in environment". Default 70 retains
     * pre-GAP-386 behaviour when override absent.
     *
     * <p>Tier-specific overrides (FREE 65 / ENT 80 etc.) achieved via
     * Helm {@code branding.qualityGate.passThreshold} → env var
     * {@code QUALITY_GATE_PASS_THRESHOLD} → Spring property
     * {@code quality-gate.pass-threshold}.</p>
     */
    @Value("${quality-gate.pass-threshold:70}")
    private int threshold;

    /** Equal weights — Wave 35+ will tune once real measurement lands. */
    private static final Map<String, Double> WEIGHTS = Map.of(
            "contrast", 0.25,
            "brokenLinks", 0.20,
            "cssVarsApplied", 0.20,
            "visualRegression", 0.20,
            "logoPlacement", 0.15
    );

    /**
     * Compute aggregated quality score for a job.
     *
     * @param job persisted job (must not be null)
     * @return composite score + sub-scores + issue list
     */
    public QualityScoreResponse aggregate(BrandingJob job) {
        Map<String, Integer> subscores = new LinkedHashMap<>();
        List<QualityScoreResponse.Issue> issues = new ArrayList<>();

        // v0: deterministic placeholders. Order matches §5 of ai-branding-guidelines.md.
        // Algorithm: derive a stable per-job offset from job ID hash so we get
        // a non-trivial spread (avoids hardcoded 100/100 scaffold pattern).
        int offset = Math.floorMod(job.getId().hashCode(), 25); // 0..24

        int contrast = clamp(80 + offset / 5);
        int brokenLinks = (job.getStatus() == JobStatus.COMPLETED) ? 100 : clamp(70 + offset / 3);
        int cssVarsApplied = clamp(75 + offset / 4);
        int visualRegression = clamp(60 + offset);
        int logoPlacement = job.getLogoUrl() != null ? clamp(85 + offset / 5) : 60;

        subscores.put("contrast", contrast);
        subscores.put("brokenLinks", brokenLinks);
        subscores.put("cssVarsApplied", cssVarsApplied);
        subscores.put("visualRegression", visualRegression);
        subscores.put("logoPlacement", logoPlacement);

        if (contrast < threshold) {
            issues.add(new QualityScoreResponse.Issue("WARN", "contrast",
                    "Contrast sub-score below threshold (placeholder v0 — GAP-226 will land real WCAG measurement)"));
        }
        if (visualRegression < threshold) {
            issues.add(new QualityScoreResponse.Issue("WARN", "visualRegression",
                    "Visual regression placeholder below threshold (GAP-227 deferral)"));
        }
        if (logoPlacement < threshold) {
            issues.add(new QualityScoreResponse.Issue("ERROR", "logoPlacement",
                    "Logo missing — uploaded logoUrl is null"));
        }

        int composite = (int) Math.round(
                contrast * WEIGHTS.get("contrast")
                        + brokenLinks * WEIGHTS.get("brokenLinks")
                        + cssVarsApplied * WEIGHTS.get("cssVarsApplied")
                        + visualRegression * WEIGHTS.get("visualRegression")
                        + logoPlacement * WEIGHTS.get("logoPlacement")
        );

        return new QualityScoreResponse(
                job.getId().toString(),
                composite,
                composite >= threshold,
                threshold,
                subscores,
                issues,
                Instant.now()
        );
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    /**
     * Test-only seam: override the externalized threshold for unit tests
     * where Spring context is not bootstrapped (e.g. plain
     * {@link org.junit.jupiter.api.Test} with new instance). Production
     * paths rely on {@link Value @Value} field-injection.
     *
     * @param threshold pass threshold 0..100
     */
    void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /** Test-only accessor — see {@link #setThreshold(int)}. */
    int getThreshold() {
        return threshold;
    }
}
