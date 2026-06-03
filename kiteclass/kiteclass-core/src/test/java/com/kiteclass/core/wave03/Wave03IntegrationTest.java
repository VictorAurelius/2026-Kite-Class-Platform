package com.kiteclass.core.wave03;

import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.ai.client.MockAIClient;
import com.kiteclass.core.module.ai.workflow.AnalyzerService;
import com.kiteclass.core.module.ai.workflow.Plan;
import com.kiteclass.core.module.ai.workflow.PlanExecutor;
import com.kiteclass.core.module.ai.workflow.PlannerService;
import com.kiteclass.core.module.ai.workflow.StepContext;
import com.kiteclass.core.module.ai.workflow.step.ExtractPaletteStep;
import com.kiteclass.core.module.ai.workflow.step.PickTemplateStep;
import com.kiteclass.core.module.ai.workflow.step.PublishPackageStep;
import com.kiteclass.core.module.ai.workflow.step.QualityReviewStep;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.service.InstanceQualityReviewer;
import com.kiteclass.core.module.branding.service.CachingBrandingPackageProxy;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Wave 3 cross-module smoke test — exercises the full agent-workflow wiring with
 * mocks (no Spring context): AnalyzerService → PlannerService → PlanExecutor running
 * the 3 scaffold Steps, emitting outbox events, final Step transitioning DEPLOYED +
 * evicting the package cache.
 *
 * <p>Proves Sub-PRs 3.1 (outbox), 3.2 (AI adapter), 3.3 (handlers), 3.4 (package cache),
 * 3.5 (agent workflow) compose together.
 *
 * @since Wave 3 Sub-PR 3.8
 */
class Wave03IntegrationTest {

    @Test
    void full_plan_exercises_outbox_lifecycle_and_cache() {
        InstanceLifecycleService lifecycle = mock(InstanceLifecycleService.class);
        CachingBrandingPackageProxy cache = mock(CachingBrandingPackageProxy.class);
        OutboxEventWriter outbox = mock(OutboxEventWriter.class);

        AnalyzerService analyzer = new AnalyzerService(new MockAIClient());
        ExtractPaletteStep extractPalette = new ExtractPaletteStep();
        PickTemplateStep pickTemplate = new PickTemplateStep();
        InstanceQualityReviewer reviewer = mock(InstanceQualityReviewer.class);
        when(reviewer.review(anyLong())).thenReturn(
                QualityReport.builder().targetInstanceId(42L).brandingVersion(1)
                        .score(90).passed(true).build());
        QualityReviewStep qualityReview = new QualityReviewStep(reviewer);
        PublishPackageStep publishPackage = new PublishPackageStep(lifecycle, cache);
        PlannerService planner = new PlannerService(
                extractPalette, pickTemplate, qualityReview, publishPackage);
        PlanExecutor executor = new PlanExecutor(outbox);

        FrontendInstance instance = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        instance.setId(42L);
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        instance.transitionTo(FrontendInstanceStatus.GENERATING);

        when(lifecycle.markBrandingCompleted(anyLong(), any())).thenReturn(instance);

        // 1) Analyzer
        var analysis = analyzer.analyze(
                com.kiteclass.core.module.ai.dto.AnalysisRequest.builder()
                        .audience("K-12").tone("friendly").build());

        // 2) Planner — Wave 4 inserted QualityReviewStep between pick and publish
        Plan plan = planner.plan(analysis);
        assertThat(plan.size()).isEqualTo(4);

        // 3) Executor drains the plan
        StepContext ctx = new StepContext(instance.getId(), instance.getTenantSlug());
        ctx.setAnalysis(analysis);
        executor.execute(plan, ctx);

        // PublishPackageStep side effects proved end-to-end wiring
        verify(lifecycle).markBrandingCompleted(42L, null);
        verify(cache).evict(42L);

        // Outbox saw both plan.started and plan.completed
        verify(outbox, atLeastOnce()).enqueue(eq("ai.plan.started"), anyString(), anyString(), anyString());
        verify(outbox).enqueue(eq("ai.plan.completed"), anyString(), anyString(), anyString());

        assertThat(ctx.getExecutedSteps()).containsExactly(
                "extract-palette", "pick-template", "quality-review", "publish-package");
    }
}
