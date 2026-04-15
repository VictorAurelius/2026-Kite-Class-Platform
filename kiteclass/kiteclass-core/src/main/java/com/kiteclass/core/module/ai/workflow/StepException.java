package com.kiteclass.core.module.ai.workflow;

/**
 * Signals a Step failure. {@link PlanExecutor} catches this and either invokes the Step's
 * fallback (if available) or aborts the plan (Saga compensation per ADR-006).
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5)
 */
public class StepException extends RuntimeException {

    public StepException(String message) {
        super(message);
    }

    public StepException(String message, Throwable cause) {
        super(message, cause);
    }
}
