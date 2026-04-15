package com.kiteclass.core.module.ai.client;

/**
 * Transient failure from the AI layer — Circuit Breaker + Retry treat as retry-able.
 *
 * <p>Non-retryable failures (invalid prompt, unsupported resource type) should throw
 * {@link NonRetryableAIException} so resilience4j skips retry and goes to fallback.
 *
 * @since 3.18.0
 */
public class AIException extends RuntimeException {

    public AIException(String message) {
        super(message);
    }

    public AIException(String message, Throwable cause) {
        super(message, cause);
    }
}
