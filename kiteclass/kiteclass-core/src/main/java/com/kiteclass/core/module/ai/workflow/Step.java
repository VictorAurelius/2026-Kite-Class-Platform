package com.kiteclass.core.module.ai.workflow;

/**
 * A single operation in an AI branding {@link Plan} (Command pattern per ADR-006).
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Declare a unique {@link #name()} (used in logs, metrics, outbox events)</li>
 *   <li>Read/write {@link StepContext} through explicit keys documented in javadoc</li>
 *   <li>Throw {@link StepException} on any failure — never swallow errors silently</li>
 *   <li>Be idempotent when reasonably possible (plans may be replayed on retry)</li>
 * </ul>
 *
 * <p>Fallback support is opt-in:
 * <ul>
 *   <li>{@link #hasFallback()} returns {@code true} if a degraded-but-acceptable path exists</li>
 *   <li>{@link #fallback(StepContext)} runs when {@link #execute} throws</li>
 * </ul>
 *
 * <p>Heavy steps (e.g. image generation) should enqueue async work inside
 * {@code execute} and return promptly — blocking the executor thread is banned
 * (ai-branding-guidelines.md §3.3).
 *
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, ADR-006)
 */
public interface Step {

    String name();

    void execute(StepContext context);

    default boolean hasFallback() {
        return false;
    }

    default void fallback(StepContext context) {
        throw new UnsupportedOperationException(
                name() + " declared hasFallback=false or forgot to override fallback()");
    }
}
