package com.kiteclass.core.module.ai.client;

/**
 * Permanent failure — don't retry. Resilience4j config lists this as a non-retry class.
 *
 * <p>Use for:
 * <ul>
 *   <li>Invalid prompt (schema violation)</li>
 *   <li>Unsupported resource type</li>
 *   <li>Content-safety rejections</li>
 *   <li>Authentication failures (no key / expired token)</li>
 * </ul>
 *
 * @since 3.18.0
 */
public class NonRetryableAIException extends AIException {

    public NonRetryableAIException(String message) {
        super(message);
    }

    public NonRetryableAIException(String message, Throwable cause) {
        super(message, cause);
    }
}
