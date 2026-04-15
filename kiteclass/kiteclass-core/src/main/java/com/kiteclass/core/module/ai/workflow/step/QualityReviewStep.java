package com.kiteclass.core.module.ai.workflow.step;

import com.kiteclass.core.module.ai.workflow.Step;
import com.kiteclass.core.module.ai.workflow.StepContext;
import com.kiteclass.core.module.ai.workflow.StepException;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.service.InstanceQualityReviewer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Quality-gate step — runs {@link InstanceQualityReviewer} and blocks DEPLOY when the
 * report doesn't pass (ai-branding-guidelines.md §5).
 *
 * <p>Inserted between {@code PickTemplateStep} and {@code PublishPackageStep} in the
 * {@code PlannerService} output. On failure throws {@link StepException} with the
 * report id and score so the saga compensation ({@code markFailed}) captures the reason.
 *
 * <p>Context writes: {@code quality-report-id}, {@code quality-score}.
 *
 * @since 3.25.0 (Wave 4 Sub-PR 4.5)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QualityReviewStep implements Step {

    public static final String KEY_REPORT_ID = "quality-report-id";
    public static final String KEY_SCORE = "quality-score";

    private final InstanceQualityReviewer reviewer;

    @Override
    public String name() {
        return "quality-review";
    }

    @Override
    public void execute(StepContext context) {
        QualityReport report = reviewer.review(context.getInstanceId());
        context.put(KEY_REPORT_ID, report.getId());
        context.put(KEY_SCORE, report.getScore());

        if (!Boolean.TRUE.equals(report.getPassed())) {
            log.warn("[quality-review] id={} blocked: score={} issues={}",
                    context.getInstanceId(), report.getScore(), report.getIssues());
            throw new StepException(
                    "quality gate failed — score " + report.getScore()
                            + " (report id=" + report.getId() + ")");
        }
        log.debug("[quality-review] id={} passed score={}",
                context.getInstanceId(), report.getScore());
    }
}
