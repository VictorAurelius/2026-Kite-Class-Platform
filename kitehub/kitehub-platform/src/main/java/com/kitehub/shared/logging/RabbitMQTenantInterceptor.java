package com.kitehub.shared.logging;

import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

/**
 * Propagates SLF4J MDC across the RabbitMQ queue boundary.
 *
 * <p>Outbound: register as a {@link MessagePostProcessor} on {@code RabbitTemplate} so
 * outgoing messages carry the producer's MDC values as headers ({@code mdc-tenantId},
 * {@code mdc-traceId}, etc.).</p>
 *
 * <p>Inbound: consumer-side wiring is service-specific (a {@code @RabbitHandler}'s
 * pre-processing step or an advice on the listener container). This class exposes
 * {@link #applyToInbound(Message)} for that path.</p>
 *
 * <p>MDC keys propagated must match {@link TenantContextFilter}'s constants.</p>
 *
 * <p>Spec: {@code logs-format-standard.md} §2.2 ("RabbitMQ messages preserve MDC
 * qua headers" — GAP-114 AC).</p>
 */
public class RabbitMQTenantInterceptor implements MessagePostProcessor {

    static final String HEADER_PREFIX = "mdc-";

    private static final String[] PROPAGATED_KEYS = {
        TenantContextFilter.MDC_TENANT_ID,
        TenantContextFilter.MDC_USER_ID,
        TenantContextFilter.MDC_TRACE_ID,
        TenantContextFilter.MDC_SPAN_ID,
        TenantContextFilter.MDC_REQUEST_ID
    };

    @Override
    public Message postProcessMessage(Message message) {
        for (String key : PROPAGATED_KEYS) {
            String v = MDC.get(key);
            if (v != null && !v.isBlank()) {
                message.getMessageProperties().setHeader(HEADER_PREFIX + key, v);
            }
        }
        return message;
    }

    /**
     * Apply inbound MDC from a Rabbit message. Caller must clear MDC after handling
     * (suggest {@code try { applyToInbound(msg); ... } finally { clear(); }}).
     */
    public static void applyToInbound(Message message) {
        for (String key : PROPAGATED_KEYS) {
            Object v = message.getMessageProperties().getHeader(HEADER_PREFIX + key);
            if (v != null) {
                MDC.put(key, v.toString());
            }
        }
    }

    /** Clear all MDC keys this interceptor manages. */
    public static void clear() {
        for (String key : PROPAGATED_KEYS) {
            MDC.remove(key);
        }
    }
}
