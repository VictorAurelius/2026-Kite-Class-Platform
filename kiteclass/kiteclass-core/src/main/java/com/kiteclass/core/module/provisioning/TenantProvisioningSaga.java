package com.kiteclass.core.module.provisioning;

import com.kiteclass.core.module.ai.dto.AnalysisRequest;
import com.kiteclass.core.module.ai.dto.AnalysisResult;
import com.kiteclass.core.module.ai.workflow.AnalyzerService;
import com.kiteclass.core.module.ai.workflow.Plan;
import com.kiteclass.core.module.ai.workflow.PlanExecutor;
import com.kiteclass.core.module.ai.workflow.PlannerService;
import com.kiteclass.core.module.ai.workflow.StepContext;
import com.kiteclass.core.module.ai.workflow.StepException;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Saga orchestrating the full tenant onboarding → branded-instance lifecycle.
 *
 * <p>Sequence (Saga pattern per ADR-006 + ADR-004):
 * <ol>
 *   <li>{@code initiate} — NOT_STARTED → INITIALIZING</li>
 *   <li>{@code provisionInfrastructure} — placeholder (DB schema, MinIO bucket, DNS);
 *       real steps belong to separate infra service — here we just log + proceed</li>
 *   <li>{@code markInfrastructureReady} — INITIALIZING → GENERATING</li>
 *   <li>Analyzer → Planner → PlanExecutor — last Step transitions DEPLOYED</li>
 *   <li>On any failure: compensation = {@code markFailed(reason)}</li>
 * </ol>
 *
 * <p>Kept as a plain service (no @Transactional) so each lifecycle step is an independent
 * transaction — a failed plan doesn't roll back the successful INITIALIZING / GENERATING
 * transitions (we need them persisted to drive retry logic).
 *
 * <p>The returned id is the {@link FrontendInstance} id — upstream uses it to
 * correlate subsequent events (outbox, webhooks).
 *
 * @since 3.22.0 (Wave 3 Sub-PR 3.6)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningSaga {

    private final InstanceLifecycleService lifecycle;
    private final AnalyzerService analyzer;
    private final PlannerService planner;
    private final PlanExecutor executor;

    public Long provision(TenantCreatedEvent event) {
        FrontendInstance instance;
        try {
            instance = lifecycle.initiate(event.getTenantId(), event.getSlug());
            log.info("[saga] initiated tenant={} slug={} id={}",
                    event.getTenantId(), event.getSlug(), instance.getId());
        } catch (RuntimeException startupFailure) {
            log.error("[saga] initiate failed tenant={} slug={}: {}",
                    event.getTenantId(), event.getSlug(), startupFailure.getMessage());
            throw startupFailure;
        }

        Long instanceId = instance.getId();

        try {
            provisionInfrastructure(event, instance);
            lifecycle.markInfrastructureReady(instanceId);
            runBrandingPlan(event, instance);
        } catch (StepException planFailure) {
            compensate(instanceId, planFailure.getMessage());
            throw planFailure;
        } catch (RuntimeException unexpected) {
            compensate(instanceId, unexpected.getMessage());
            throw unexpected;
        }
        return instanceId;
    }

    /**
     * Placeholder — the real infra provisioning (DB schema, MinIO bucket, DNS record,
     * subdomain certificate) is the responsibility of a separate ops service. For the
     * saga-integration scaffold we just log; the follow-up PR will replace this with
     * a call to {@code InfrastructureProvisioningService}.
     */
    protected void provisionInfrastructure(TenantCreatedEvent event, FrontendInstance instance) {
        log.info("[saga] infrastructure provisioning stub tenant={} slug={} id={}",
                event.getTenantId(), event.getSlug(), instance.getId());
    }

    private void runBrandingPlan(TenantCreatedEvent event, FrontendInstance instance) {
        AnalysisRequest request = AnalysisRequest.builder()
                .audience(event.getAudience())
                .tone(event.getTone())
                .build();
        AnalysisResult analysis = analyzer.analyze(request);

        StepContext context = new StepContext(instance.getId(), instance.getTenantSlug());
        context.setAnalysis(analysis);

        Plan plan = planner.plan(analysis);
        executor.execute(plan, context);
    }

    private void compensate(Long instanceId, String reason) {
        try {
            lifecycle.markFailed(instanceId, reason == null ? "unknown" : reason);
        } catch (RuntimeException secondary) {
            log.error("[saga] compensation markFailed itself threw id={}: {}",
                    instanceId, secondary.getMessage(), secondary);
        }
    }
}
