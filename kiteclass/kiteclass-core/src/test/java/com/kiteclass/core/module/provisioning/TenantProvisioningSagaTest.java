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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private TenantProvisioningSaga saga;

    private double counter(String result) {
        var c = meterRegistry.find(TenantProvisioningSaga.METRIC_COMPENSATION)
                .tag("result", result).counter();
        return c == null ? 0d : c.count();
    }

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

    private FrontendInstance withStatus(long id, FrontendInstanceStatus... transitions) {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        for (FrontendInstanceStatus s : transitions) {
            i.transitionTo(s);
        }
        i.setId(id);
        return i;
    }

    private FrontendInstance failed(long id) {
        return withStatus(id, FrontendInstanceStatus.INITIALIZING, FrontendInstanceStatus.FAILED);
    }

    private FrontendInstance deployed(long id) {
        return withStatus(id, FrontendInstanceStatus.INITIALIZING,
                FrontendInstanceStatus.GENERATING, FrontendInstanceStatus.DEPLOYED);
    }

    private FrontendInstance generating(long id) {
        return withStatus(id, FrontendInstanceStatus.INITIALIZING, FrontendInstanceStatus.GENERATING);
    }

    @Test
    void happy_path_runs_initiate_infra_analyzer_planner_executor() {
        when(lifecycle.findActiveBySlug("acme")).thenReturn(Optional.empty());
        when(lifecycle.initiate(anyString(), anyString())).thenReturn(initialized(42L));
        when(analyzer.analyze(any())).thenReturn(AnalysisResult.builder()
                .palette(List.of("#1F2937")).build());
        when(planner.plan(any())).thenReturn(new Plan("desc", List.of()));

        Long id = saga.provision(event());

        assertThat(id).isEqualTo(42L);
        verify(lifecycle).initiate("t-1", "acme");
        verify(lifecycle, never()).retry(anyLong());
        verify(lifecycle).markInfrastructureReady(42L);
        verify(analyzer).analyze(any());
        verify(planner).plan(any());
        verify(executor).execute(any(Plan.class), any(StepContext.class));
        verify(lifecycle, never()).markFailed(anyLong(), anyString());
    }

    // ---- GAP-953 admin force-retry: idempotent + retry-aware provision ----

    @Test
    void no_existing_instance_initiates_and_does_not_retry() {
        when(lifecycle.findActiveBySlug("acme")).thenReturn(Optional.empty());
        when(lifecycle.initiate(anyString(), anyString())).thenReturn(initialized(42L));
        when(analyzer.analyze(any())).thenReturn(AnalysisResult.templateOnly());
        when(planner.plan(any())).thenReturn(new Plan("desc", List.of()));

        Long id = saga.provision(event());

        assertThat(id).isEqualTo(42L);
        verify(lifecycle).initiate("t-1", "acme");
        verify(lifecycle, never()).retry(anyLong());
        verify(lifecycle).markInfrastructureReady(42L);
        verify(executor).execute(any(Plan.class), any(StepContext.class));
    }

    @Test
    void existing_failed_instance_routes_through_retry_not_initiate() {
        when(lifecycle.findActiveBySlug("acme")).thenReturn(Optional.of(failed(42L)));
        when(lifecycle.retry(42L)).thenReturn(initialized(42L));
        when(analyzer.analyze(any())).thenReturn(AnalysisResult.templateOnly());
        when(planner.plan(any())).thenReturn(new Plan("desc", List.of()));

        Long id = saga.provision(event());

        assertThat(id).isEqualTo(42L);
        verify(lifecycle).retry(42L);
        verify(lifecycle, never()).initiate(anyString(), anyString());
        verify(lifecycle).markInfrastructureReady(42L);
        verify(executor).execute(any(Plan.class), any(StepContext.class));
    }

    @Test
    void existing_deployed_instance_is_idempotent_no_op() {
        when(lifecycle.findActiveBySlug("acme")).thenReturn(Optional.of(deployed(77L)));

        Long id = saga.provision(event());

        assertThat(id).isEqualTo(77L);
        verify(lifecycle, never()).initiate(anyString(), anyString());
        verify(lifecycle, never()).retry(anyLong());
        verify(lifecycle, never()).markInfrastructureReady(anyLong());
        verify(executor, never()).execute(any(), any());
    }

    @Test
    void existing_in_flight_instance_is_idempotent_no_op() {
        when(lifecycle.findActiveBySlug("acme")).thenReturn(Optional.of(generating(88L)));

        Long id = saga.provision(event());

        assertThat(id).isEqualTo(88L);
        verify(lifecycle, never()).initiate(anyString(), anyString());
        verify(lifecycle, never()).retry(anyLong());
        verify(lifecycle, never()).markInfrastructureReady(anyLong());
        verify(executor, never()).execute(any(), any());
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
    void compensation_success_increments_success_counter() {
        when(lifecycle.initiate(anyString(), anyString())).thenReturn(initialized(42L));
        when(analyzer.analyze(any())).thenReturn(AnalysisResult.templateOnly());
        when(planner.plan(any())).thenReturn(new Plan("desc", List.of()));
        doThrow(new StepException("plan blew up")).when(executor).execute(any(), any());

        assertThatThrownBy(() -> saga.provision(event()))
                .isInstanceOf(StepException.class);

        verify(lifecycle).markFailed(eq(42L), eq("plan blew up"));
        assertThat(counter("success")).isEqualTo(1d);
        assertThat(counter("failed")).isEqualTo(0d);
    }

    @Test
    void compensation_markFailed_failure_increments_failed_counter_and_does_not_mask_original() {
        when(lifecycle.initiate(anyString(), anyString())).thenReturn(initialized(42L));
        when(analyzer.analyze(any())).thenReturn(AnalysisResult.templateOnly());
        when(planner.plan(any())).thenReturn(new Plan("desc", List.of()));
        doThrow(new StepException("plan blew up")).when(executor).execute(any(), any());
        doThrow(new RuntimeException("db connection lost"))
                .when(lifecycle).markFailed(anyLong(), anyString());

        // The original saga failure (StepException) must surface — NOT the secondary markFailed error.
        assertThatThrownBy(() -> saga.provision(event()))
                .isInstanceOf(StepException.class)
                .hasMessageContaining("plan blew up");

        assertThat(counter("failed")).isEqualTo(1d);
        assertThat(counter("success")).isEqualTo(0d);
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
