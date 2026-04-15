package com.kiteclass.core.module.quality.check;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import lombok.Builder;
import lombok.Value;

/**
 * One automated quality check (Strategy pattern per Wave 4 plan).
 *
 * <p>Implementations run in {@code InstanceQualityReviewer}; each returns a {@link Result}
 * with an integer score 0–100 and optional issue detail. The reviewer aggregates results
 * (weighted average) into the overall {@code QualityReport.score}.
 *
 * <p>5 reference checks per ai-branding-guidelines.md §5:
 * <ol>
 *   <li>WCAG contrast ratio ≥ 4.5:1</li>
 *   <li>CSS variables applied (no default values remaining)</li>
 *   <li>No broken asset URLs</li>
 *   <li>Visual regression ≤ 20% diff</li>
 *   <li>Logo placement sanity (not cropped / appropriate size)</li>
 * </ol>
 *
 * @since 3.25.0 (Wave 4 Sub-PR 4.5, GAP-012)
 */
public interface QualityCheck {

    /** Stable identifier — used as map key in reports and as metric label. */
    String name();

    Result run(FrontendInstance instance);

    @Value
    @Builder
    class Result {
        String checkName;
        int score;
        boolean passed;
        String detail;

        public static Result pass(String name, int score) {
            return Result.builder()
                    .checkName(name).score(score).passed(true).build();
        }

        public static Result fail(String name, int score, String detail) {
            return Result.builder()
                    .checkName(name).score(score).passed(false).detail(detail).build();
        }
    }
}
