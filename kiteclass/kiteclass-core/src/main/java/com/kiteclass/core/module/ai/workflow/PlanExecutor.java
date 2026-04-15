package com.kiteclass.core.module.ai.workflow;

import com.kiteclass.core.common.outbox.OutboxEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drains a {@link Plan} by invoking each {@link Step} in order.
 *
 * <p>Behaviour per Step (Saga pattern per ADR-006):
 * <ol>
 *   <li>{@code step.execute(ctx)} happy path → record + continue</li>
 *   <li>If {@link StepException} AND {@link Step#hasFallback()} → {@code fallback(ctx)} →
 *       record + continue</li>
 *   <li>If exception propagates (no fallback or fallback also threw) → emit outbox event
 *       {@code ai.plan.failed} and rethrow to caller (saga abort)</li>
 * </ol>
 *
 * <p>Outbox events emitted during a plan run:
 * <ul>
 *   <li>{@code ai.plan.started} — entering the executor</li>
 *   <li>{@code ai.step.completed} — each successful step</li>
 *   <li>{@code ai.step.fallback} — step failed but fallback succeeded</li>
 *   <li>{@code ai.plan.completed} — all steps done</li>
 *   <li>{@code ai.plan.failed} — aborted mid-plan</li>
 * </ul>
 *
 * <p>Publishing goes through {@link OutboxEventWriter} — {@code @Transactional} here means
 * all events from one plan run commit with whatever DB writes the steps did. Consumers are
 * assumed idempotent (BR-OBX-004).
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, ADR-006 + ADR-007)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanExecutor {

    private static final String AGGREGATE_TYPE = "BrandingPlan";

    private final OutboxEventWriter outbox;

    @Transactional
    public void execute(Plan plan, StepContext context) {
        emit("ai.plan.started", context, "{\"steps\":" + plan.getSteps().size() + "}");
        log.info("[plan] start id={} steps={}", context.getInstanceId(), plan.getSteps().size());

        for (Step step : plan.getSteps()) {
            runStep(step, context);
        }

        emit("ai.plan.completed", context, "{}");
        log.info("[plan] completed id={} steps-run={}",
                context.getInstanceId(), context.getExecutedSteps().size());
    }

    private void runStep(Step step, StepContext context) {
        try {
            step.execute(context);
            context.recordExecution(step.name());
            emit("ai.step.completed", context, "{\"step\":\"" + step.name() + "\"}");
            log.debug("[plan] step.completed id={} step={}", context.getInstanceId(), step.name());
        } catch (StepException primary) {
            if (!step.hasFallback()) {
                emit("ai.plan.failed", context,
                        "{\"step\":\"" + step.name() + "\",\"reason\":\""
                                + escape(primary.getMessage()) + "\"}");
                log.error("[plan] step.failed id={} step={} no-fallback: {}",
                        context.getInstanceId(), step.name(), primary.getMessage());
                throw primary;
            }
            try {
                step.fallback(context);
                context.recordExecution(step.name() + "[fallback]");
                emit("ai.step.fallback", context,
                        "{\"step\":\"" + step.name() + "\",\"reason\":\""
                                + escape(primary.getMessage()) + "\"}");
                log.warn("[plan] step.fallback id={} step={}: {}",
                        context.getInstanceId(), step.name(), primary.getMessage());
            } catch (RuntimeException fallbackFailure) {
                emit("ai.plan.failed", context,
                        "{\"step\":\"" + step.name()
                                + "\",\"fallbackFailure\":\""
                                + escape(fallbackFailure.getMessage()) + "\"}");
                log.error("[plan] step.fallback.failed id={} step={}",
                        context.getInstanceId(), step.name(), fallbackFailure);
                throw new StepException(
                        step.name() + " failed AND fallback failed", fallbackFailure);
            }
        }
    }

    private void emit(String eventType, StepContext context, String extraPayload) {
        String payload = "{\"instanceId\":" + context.getInstanceId()
                + ",\"tenantId\":\"" + escape(context.getTenantId()) + "\""
                + ",\"detail\":" + extraPayload + "}";
        outbox.enqueue(eventType, AGGREGATE_TYPE, String.valueOf(context.getInstanceId()), payload);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
