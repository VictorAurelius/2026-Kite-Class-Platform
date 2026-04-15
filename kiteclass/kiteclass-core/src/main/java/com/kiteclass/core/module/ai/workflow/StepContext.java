package com.kiteclass.core.module.ai.workflow;

import com.kiteclass.core.module.ai.dto.AnalysisResult;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable state bag threaded through a {@link Plan}'s Steps (Command pattern per ADR-006).
 *
 * <p>Deliberately kept small: per-plan identity (instanceId + tenantId) + analysis result +
 * a {@code Map<String, Object>} bag for ad-hoc step-to-step hand-off. Steps should read+write
 * the bag under explicit keys declared in their own javadoc to keep coupling traceable.
 *
 * <p>Not thread-safe — a single {@link PlanExecutor} thread drains one plan sequentially.
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, ADR-006)
 */
@Getter
@Setter
public class StepContext {

    private final Long instanceId;
    private final String tenantId;

    private AnalysisResult analysis;

    private final Map<String, Object> attributes = new HashMap<>();
    private final List<String> executedSteps = new java.util.ArrayList<>();

    public StepContext(Long instanceId, String tenantId) {
        this.instanceId = instanceId;
        this.tenantId = tenantId;
    }

    public void put(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) attributes.get(key);
    }

    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    void recordExecution(String stepName) {
        executedSteps.add(stepName);
    }
}
