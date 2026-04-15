package com.kiteclass.core.module.quality;

import com.kiteclass.core.module.ai.workflow.StepContext;
import com.kiteclass.core.module.ai.workflow.StepException;
import com.kiteclass.core.module.ai.workflow.step.QualityReviewStep;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.service.InstanceQualityReviewer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QualityReviewStepTest {

    private QualityReport report(int score, boolean passed) {
        return QualityReport.builder()
                .targetInstanceId(42L).brandingVersion(1)
                .score(score).passed(passed).issues("[]").build();
    }

    @Test
    void execute_passes_when_report_ok_and_writes_context() {
        InstanceQualityReviewer reviewer = mock(InstanceQualityReviewer.class);
        QualityReport rep = report(88, true);
        rep.setId(100L);
        when(reviewer.review(anyLong())).thenReturn(rep);

        QualityReviewStep step = new QualityReviewStep(reviewer);
        StepContext ctx = new StepContext(42L, "t-1");

        step.execute(ctx);

        assertThat((Long) ctx.get(QualityReviewStep.KEY_REPORT_ID)).isEqualTo(100L);
        assertThat((Integer) ctx.get(QualityReviewStep.KEY_SCORE)).isEqualTo(88);
    }

    @Test
    void execute_throws_StepException_when_report_fails() {
        InstanceQualityReviewer reviewer = mock(InstanceQualityReviewer.class);
        QualityReport rep = report(40, false);
        rep.setId(200L);
        when(reviewer.review(anyLong())).thenReturn(rep);

        QualityReviewStep step = new QualityReviewStep(reviewer);
        StepContext ctx = new StepContext(42L, "t-1");

        assertThatThrownBy(() -> step.execute(ctx))
                .isInstanceOf(StepException.class)
                .hasMessageContaining("quality gate failed")
                .hasMessageContaining("200");
    }

    @Test
    void step_has_no_fallback() {
        QualityReviewStep step = new QualityReviewStep(mock(InstanceQualityReviewer.class));
        assertThat(step.hasFallback()).isFalse();
        assertThat(step.name()).isEqualTo("quality-review");
    }
}
