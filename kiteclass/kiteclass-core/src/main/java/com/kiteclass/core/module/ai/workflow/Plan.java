package com.kiteclass.core.module.ai.workflow;

import lombok.Value;

import java.util.List;

/**
 * Ordered sequence of {@link Step}s (Composite pattern per ADR-006).
 *
 * <p>Produced by {@code PlannerService} from an {@link com.kiteclass.core.module.ai.dto.AnalysisResult};
 * consumed by {@link PlanExecutor}.
 *
 * <p>Steps execute in declared order. Saga compensation on failure is handled per-step
 * (via {@link Step#fallback}) rather than globally — a failed step either recovers via
 * fallback or aborts the whole plan.
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, ADR-006)
 */
@Value
public class Plan {

    String description;
    List<Step> steps;

    public int size() {
        return steps.size();
    }
}
