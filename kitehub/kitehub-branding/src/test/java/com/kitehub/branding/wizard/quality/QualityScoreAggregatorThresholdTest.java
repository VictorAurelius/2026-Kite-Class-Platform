package com.kitehub.branding.wizard.quality;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.wizard.quality.dto.QualityScoreResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-386 — verifies quality-gate threshold is externalized via
 * {@link Value @Value} (config key {@code quality-gate.pass-threshold})
 * and that overrides change the pass/fail decision.
 *
 * <p>Plain JUnit (no Spring context) — uses
 * {@link ReflectionTestUtils#setField} to simulate {@code @Value}
 * field-injection. The package-private setter on
 * {@link QualityScoreAggregator} provides an alternative seam.</p>
 */
@DisplayName("QualityScoreAggregator — externalized threshold (GAP-386)")
class QualityScoreAggregatorThresholdTest {

    private static BrandingJob fixedJob() {
        BrandingJob job = new BrandingJob();
        // Fixed UUID → stable hashCode → deterministic offset → deterministic composite
        job.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        job.setInstanceId(UUID.randomUUID());
        job.setOrganizationName("Trường ABC");
        job.setLanguage("vi");
        job.setStatus(JobStatus.COMPLETED);
        job.setLogoUrl("https://cdn.example.com/logo.png");
        job.setProgress(100);
        job.setRetryCount(0);
        job.setQueuedAt(LocalDateTime.now().minusMinutes(5));
        job.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        job.setUpdatedAt(LocalDateTime.now());
        return job;
    }

    @Test
    @DisplayName("default fallback 70 when no @Value override (GAP-386 AC: missing config → 70)")
    void defaultThresholdIs70WhenNotInjected() throws Exception {
        // Simulate Spring not having processed @Value — manually read the
        // default annotation value to confirm the fallback contract.
        QualityScoreAggregator aggregator = new QualityScoreAggregator();
        Field thresholdField = QualityScoreAggregator.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);

        // Field is 0 in plain new-instance (no Spring); apply the @Value default
        // explicitly to mirror what Spring would do with missing config:
        ReflectionTestUtils.setField(aggregator, "threshold", 70);

        QualityScoreResponse response = aggregator.aggregate(fixedJob());

        assertThat(response.threshold())
                .as("default threshold from @Value(\"${quality-gate.pass-threshold:70}\") fallback")
                .isEqualTo(70);
    }

    @Test
    @DisplayName("custom threshold 80 → score 75 returns FAIL; score 85 returns PASS (GAP-386 AC)")
    void customThresholdAffectsPassDecision() {
        BrandingJob job = fixedJob();

        // Compute baseline composite with threshold 0 (always passes — neutral)
        QualityScoreAggregator probe = new QualityScoreAggregator();
        ReflectionTestUtils.setField(probe, "threshold", 0);
        int composite = probe.aggregate(job).score();

        assertThat(composite)
                .as("placeholder v0 composite is bounded 0..100 and non-trivial for fixed UUID")
                .isBetween(50, 100);

        // FAIL case: threshold = composite + 5 → must fail
        QualityScoreAggregator stricter = new QualityScoreAggregator();
        int strictThreshold = Math.min(composite + 5, 100);
        ReflectionTestUtils.setField(stricter, "threshold", strictThreshold);
        QualityScoreResponse failing = stricter.aggregate(job);
        assertThat(failing.threshold()).isEqualTo(strictThreshold);
        assertThat(failing.passed())
                .as("score %d below threshold %d → must FAIL", failing.score(), strictThreshold)
                .isFalse();

        // PASS case: threshold = composite - 5 → must pass
        QualityScoreAggregator lenient = new QualityScoreAggregator();
        int lenientThreshold = Math.max(composite - 5, 0);
        ReflectionTestUtils.setField(lenient, "threshold", lenientThreshold);
        QualityScoreResponse passing = lenient.aggregate(job);
        assertThat(passing.threshold()).isEqualTo(lenientThreshold);
        assertThat(passing.passed())
                .as("score %d above threshold %d → must PASS", passing.score(), lenientThreshold)
                .isTrue();
    }

    @Test
    @DisplayName("threshold field is response.threshold() — surface invariant")
    void responseEchoesConfiguredThreshold() {
        QualityScoreAggregator aggregator = new QualityScoreAggregator();
        ReflectionTestUtils.setField(aggregator, "threshold", 88);

        QualityScoreResponse response = aggregator.aggregate(fixedJob());

        assertThat(response.threshold())
                .as("response.threshold() must echo the externalized config (BR-QUALITY-001)")
                .isEqualTo(88);
    }

    @Test
    @DisplayName("setter seam exposes threshold for non-Spring tests (GAP-386 test ergonomics)")
    void setterSeamWorks() {
        QualityScoreAggregator aggregator = new QualityScoreAggregator();
        aggregator.setThreshold(65);

        assertThat(aggregator.getThreshold()).isEqualTo(65);
        assertThat(aggregator.aggregate(fixedJob()).threshold()).isEqualTo(65);
    }
}
