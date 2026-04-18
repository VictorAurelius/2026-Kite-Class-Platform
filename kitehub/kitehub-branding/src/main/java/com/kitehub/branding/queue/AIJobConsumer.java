package com.kitehub.branding.queue;

import com.kitehub.branding.config.AIQueueConfig;
import com.kitehub.branding.config.AIQueueProperties;
import com.kitehub.branding.dto.AIJobPayload;
import com.kitehub.branding.service.DistributedRateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Tier-aware RabbitMQ consumer for the fair-queue AI pipeline (GAP-005a).
 *
 * <p>One listener per tier queue:
 * <ul>
 *   <li>{@code ai.request.enterprise} → {@link #consumeEnterprise}</li>
 *   <li>{@code ai.request.pro} → {@link #consumePro}</li>
 *   <li>{@code ai.request.free} → {@link #consumeFree}</li>
 * </ul>
 *
 * <p>For every job:
 * <ol>
 *   <li>Acquire a Redis concurrency slot (tier-specific cap).
 *   If cap reached, message is rejected and redelivered later.</li>
 *   <li>Apply backpressure: if enterprise backlog exceeds threshold AND the
 *   current job is FREE tier, it's rejected with a template-fallback hint.</li>
 *   <li>Delegate to the processing pipeline (currently logs only — real
 *   AI work will be wired in the next wave; Phase 1 focuses on the topology).</li>
 *   <li>Record wait + duration timers; counter per outcome.</li>
 * </ol>
 *
 * <p>Only active when {@code ai.queue.fair-queue-enabled=true} (default).</p>
 *
 * @since 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.queue.fair-queue-enabled", havingValue = "true", matchIfMissing = true)
public class AIJobConsumer {

    private final DistributedRateLimiter rateLimiter;
    private final AIQueueProperties properties;
    private final MeterRegistry meterRegistry;
    private final BacklogInspector backlogInspector;

    public AIJobConsumer(DistributedRateLimiter rateLimiter,
                         AIQueueProperties properties,
                         MeterRegistry meterRegistry,
                         BacklogInspector backlogInspector) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.backlogInspector = backlogInspector;
    }

    // --- Tier listeners -------------------------------------------------------

    @RabbitListener(queues = AIQueueConfig.QUEUE_ENTERPRISE)
    public void consumeEnterprise(AIJobPayload payload) {
        handle(AIJobPriority.ENTERPRISE, payload);
    }

    @RabbitListener(queues = AIQueueConfig.QUEUE_PRO)
    public void consumePro(AIJobPayload payload) {
        handle(AIJobPriority.PRO, payload);
    }

    @RabbitListener(queues = AIQueueConfig.QUEUE_FREE)
    public void consumeFree(AIJobPayload payload) {
        handle(AIJobPriority.FREE, payload);
    }

    // --- Shared handler -------------------------------------------------------

    private void handle(AIJobPriority priority, AIJobPayload payload) {
        String tierTag = priority.name().toLowerCase();

        // Backpressure check — free tier degrades when enterprise is saturated.
        if (priority == AIJobPriority.FREE
                && backlogInspector.enterpriseBacklog() > properties.getBackpressure().getEnterpriseBacklogThreshold()) {
            log.warn("Free tier degraded to template fallback — enterprise backlog exceeds {}",
                    properties.getBackpressure().getEnterpriseBacklogThreshold());
            Counter.builder("ai.job.outcome")
                    .tag("tier", tierTag)
                    .tag("outcome", "degraded")
                    .register(meterRegistry)
                    .increment();
            // Degraded path = drop AI processing; caller's controller layer
            // will have already delivered a template response.
            return;
        }

        // Concurrency gate (Redis semaphore).
        int cap = concurrencyCapFor(priority);
        boolean acquired = rateLimiter.tryAcquireConcurrencySlot(payload.getInstanceId(), cap);
        if (!acquired) {
            log.info("Concurrency cap {} reached for instance {} (tier={}) — rejecting to redeliver",
                    cap, payload.getInstanceId(), tierTag);
            Counter.builder("ai.job.outcome")
                    .tag("tier", tierTag)
                    .tag("outcome", "concurrency_limited")
                    .register(meterRegistry)
                    .increment();
            // Throw — Rabbit will NACK and (with retry config) redeliver.
            throw new ConcurrencyLimitedException("concurrency cap " + cap + " reached");
        }

        // Wait-time timer (enqueue → process start).
        if (payload.getEnqueuedAt() != null) {
            Duration wait = Duration.between(payload.getEnqueuedAt(), Instant.now());
            Timer.builder("ai.job.wait.time")
                    .tag("tier", tierTag)
                    .register(meterRegistry)
                    .record(wait);
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            process(priority, payload);
            Counter.builder("ai.job.outcome")
                    .tag("tier", tierTag)
                    .tag("outcome", "success")
                    .register(meterRegistry)
                    .increment();
        } catch (RuntimeException ex) {
            log.error("Job {} failed (tier={}): {}", payload.getJobId(), tierTag, ex.getMessage(), ex);
            Counter.builder("ai.job.outcome")
                    .tag("tier", tierTag)
                    .tag("outcome", "failure")
                    .register(meterRegistry)
                    .increment();
            throw ex;
        } finally {
            sample.stop(Timer.builder("ai.job.duration")
                    .tag("tier", tierTag)
                    .register(meterRegistry));
            rateLimiter.releaseConcurrencySlot(payload.getInstanceId());
        }
    }

    /**
     * Resolve the concurrency cap for the given tier.
     *
     * <p>Overridable via {@link AIQueueProperties#getConcurrency()}.</p>
     */
    private int concurrencyCapFor(AIJobPriority priority) {
        return switch (priority) {
            case ENTERPRISE -> properties.getConcurrency().getEnterprise();
            case PRO -> properties.getConcurrency().getPro();
            case FREE -> properties.getConcurrency().getFree();
        };
    }

    /**
     * Extension point for the real AI pipeline. Phase 1 only logs — the
     * {@code AIBrandingProcessor} / reactive {@code ContentGenerationService}
     * will be plugged in here in a follow-up once the dispatcher is used by
     * controllers. Keeping the skeleton means Phase 1 can ship without
     * breaking legacy consumers.
     */
    protected void process(AIJobPriority priority, AIJobPayload payload) {
        log.info("Processing AI job {} (tier={}, type={}, instance={})",
                payload.getJobId(), priority, payload.getJobType(), payload.getInstanceId());
    }

    /**
     * Raised when the per-tenant concurrency cap is hit — signals the broker
     * to NACK the message and redeliver.
     */
    public static class ConcurrencyLimitedException extends RuntimeException {
        public ConcurrencyLimitedException(String message) {
            super(message);
        }
    }
}
