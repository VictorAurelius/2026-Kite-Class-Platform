package com.kiteclass.core.module.ai.workflow;

import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.workflow.step.ExtractPaletteStep;
import com.kiteclass.core.module.ai.workflow.step.PickTemplateStep;
import com.kiteclass.core.module.ai.workflow.step.PublishPackageStep;
import com.kiteclass.core.module.ai.workflow.step.QualityReviewStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Layer 2 of the agent orchestration (ADR-006): produces a {@link Plan} from an
 * {@link AnalysisResult}.
 *
 * <p>Scaffold planner — returns a fixed 3-step sequence covering: palette extraction,
 * template selection, package publication. Heavy image-generation steps (logo/banner/hero)
 * will be slotted by a follow-up (Sub-PR 3.5b) which adds async queue integration.
 *
 * <p>When {@code analysis.templateOnly} is true, planner still yields the same plan —
 * Steps themselves handle the template-only path (no FULL_AI enqueue).
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, ADR-006)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlannerService {

    private final ExtractPaletteStep extractPalette;
    private final PickTemplateStep pickTemplate;
    private final QualityReviewStep qualityReview;
    private final PublishPackageStep publishPackage;

    public Plan plan(AnalysisResult analysis) {
        log.debug("[planner] templateOnly={} palette={} mood={}",
                analysis.isTemplateOnly(),
                analysis.getPalette(),
                analysis.getMoodTags());
        // Quality gate (ai-branding-guidelines.md §5) runs between pick-template and
        // publish-package — must pass before DEPLOY.
        List<Step> steps = List.of(extractPalette, pickTemplate, qualityReview, publishPackage);
        String description = analysis.isTemplateOnly()
                ? "template-only plan (AI fallback path)"
                : "standard plan (template-first with AI assist available)";
        return new Plan(description, steps);
    }
}
