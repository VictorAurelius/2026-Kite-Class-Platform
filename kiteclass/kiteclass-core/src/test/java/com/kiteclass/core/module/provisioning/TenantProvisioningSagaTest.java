package com.kiteclass.core.module.provisioning;

import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.workflow.AnalyzerService;
import com.kiteclass.core.module.ai.workflow.Plan;
import com.kiteclass.core.module.ai.workflow.PlanExecutor;
import com.kiteclass.core.module.ai.workflow.PlannerService;
import com.kiteclass.core.module.ai.workflow.StepContext;
import com.kiteclass.core.module.ai.workflow.StepException;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningSagaTest {

    @Mock
    private InstanceLifecycleService lifecycle;

    @Mock
    private AnalyzerService analyzer;

    @Mock
    private PlannerService planner;

    @Mock
    private PlanExecutor executor;

    @InjectMocks
    private TenantProvisioningSaga saga;

    private TenantCreatedEvent event() {
        return TenantCreatedEvent.builder()
                .tenantId("t-1").slug("acme")
                .audience("K-12").tone("friendly")
                .build();
    }

    private FrontendInstance initialized(long id) {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.INITIALIZING)
                .retryCount(0).brandingVersion(0).build();
        i.setId(id);
        return i;
    }

    @Test
    void happy_path_runs_initiate_infra_analyzer_planner_executor() {
        when(lifecycle.initiate(anyString(), anyString())).thenReturn(initialized(42L));
        when(analyzer.analyze(any())).thenReturn(AnalysisResult.builder()
                .palette(List.of("#1F2937")).build());
        when(planner.plan(any())).thenReturn(new Plan("desc", List.of()));

        Long id = saga.provision(event());

        assertThat(id).isEqualTo(42L);
        verify(lifecycle).initiate("t-1", "acme");
        verify(lifecycle).markInfrastructureReady(42L);
        verify(analyzer).analyze(any());
        verify(planner).plan(any());
        verify(executor).execute(any(Plan.class), any(StepContext.class));
        verify(lifecycle, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void initiate_failure_short_circuits_and_does_not_compensate() {
        when(lifecycle.initiate(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("slug in use"));

        assertThatThrownBy(() -> saga.provision(event()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slug in use");

        verify(lifecycle, never()).markInfrastructureReady(anyLong());
        verify(lifecycle, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void plan_failure_triggers_compensation_markFailed() {
        when(lifecycle.initiate(anyString(), anyString())).thenReturn(initialized(42L));
        when(analyzer.analyze(any())).thenReturn(AnalysisResult.templateOnly());
        when(planner.plan(any())).thenReturn(new Plan("desc", List.of()));
        doThrow(new StepException("plan blew up")).when(executor).execute(any(), any());

        assertThatThrownBy(() -> saga.provision(event()))
                .isInstanceOf(StepException.class)
                .hasMessageContaining("plan blew up");

        verify(lifecycle).markFailed(eq(42L), eq("plan blew up"));
    }

    @Test
    void unexpected_runtime_inside_saga_also_compensates() {
        when(lifecycle.initiate(anyString(), anyString())).thenReturn(initialized(42L));
        when(analyzer.analyze(any()))
                .thenThrow(new RuntimeException("analyzer exploded"));

        assertThatThrownBy(() -> saga.provision(event()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("analyzer exploded");

        verify(lifecycle).markFailed(42L, "analyzer exploded");
    }

    @Test
    void analyzer_fallback_to_templateOnly_still_runs_plan() {
        when(lifecycle.initiate(anyString(), anyString())).thenReturn(initialized(42L));
        when(analyzer.analyze(any())).thenReturn(AnalysisResult.templateOnly());
        when(planner.plan(any())).thenReturn(new Plan("template-only", List.of()));

        saga.provision(event());

        verify(executor).execute(any(), any(StepContext.class));
    }
}
