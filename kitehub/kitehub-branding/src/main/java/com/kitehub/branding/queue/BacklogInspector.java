package com.kitehub.branding.queue;

import com.kitehub.branding.config.AIQueueConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Observes tier-queue depths via {@link AmqpAdmin} and exposes them as
 * Micrometer gauges + a {@link #enterpriseBacklog()} accessor used by
 * {@link AIJobConsumer} for backpressure decisions.
 *
 * <p>Gauges (per tier):
 * <ul>
 *   <li>{@code ai.queue.depth{tier=...}} — current ready messages</li>
 * </ul>
 *
 * <p>Depth is refreshed lazily on each read — acceptable because the consumer
 * only consults it when deciding whether to degrade free-tier jobs.</p>
 *
 * @since 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.queue.fair-queue-enabled", havingValue = "true", matchIfMissing = true)
public class BacklogInspector implements InitializingBean {

    private final AmqpAdmin amqpAdmin;
    private final MeterRegistry meterRegistry;

    private final AtomicLong enterpriseDepth = new AtomicLong(0);
    private final AtomicLong proDepth = new AtomicLong(0);
    private final AtomicLong freeDepth = new AtomicLong(0);

    public BacklogInspector(AmqpAdmin amqpAdmin, MeterRegistry meterRegistry) {
        this.amqpAdmin = amqpAdmin;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void afterPropertiesSet() {
        Gauge.builder("ai.queue.depth", enterpriseDepth, AtomicLong::doubleValue)
                .tag("tier", "enterprise")
                .register(meterRegistry);
        Gauge.builder("ai.queue.depth", proDepth, AtomicLong::doubleValue)
                .tag("tier", "pro")
                .register(meterRegistry);
        Gauge.builder("ai.queue.depth", freeDepth, AtomicLong::doubleValue)
                .tag("tier", "free")
                .register(meterRegistry);
    }

    /**
     * @return current depth of the enterprise queue (ready messages), 0 on error
     */
    public long enterpriseBacklog() {
        long depth = fetchDepth(AIQueueConfig.QUEUE_ENTERPRISE);
        enterpriseDepth.set(depth);
        return depth;
    }

    /**
     * @return current depth of the pro queue
     */
    public long proBacklog() {
        long depth = fetchDepth(AIQueueConfig.QUEUE_PRO);
        proDepth.set(depth);
        return depth;
    }

    /**
     * @return current depth of the free queue
     */
    public long freeBacklog() {
        long depth = fetchDepth(AIQueueConfig.QUEUE_FREE);
        freeDepth.set(depth);
        return depth;
    }

    /** Spring AMQP exposes message counts under this key in queue properties. */
    static final String QUEUE_MESSAGE_COUNT = "QUEUE_MESSAGE_COUNT";

    private long fetchDepth(String queueName) {
        try {
            Properties props = amqpAdmin.getQueueProperties(queueName);
            if (props == null) {
                return 0L;
            }
            Object count = props.get(QUEUE_MESSAGE_COUNT);
            if (count instanceof Number n) {
                return n.longValue();
            }
            return 0L;
        } catch (RuntimeException ex) {
            log.debug("Failed to read backlog for {}: {}", queueName, ex.getMessage());
            return 0L;
        }
    }
}
