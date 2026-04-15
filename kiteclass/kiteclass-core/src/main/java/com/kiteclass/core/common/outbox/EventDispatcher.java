package com.kiteclass.core.common.outbox;

/**
 * Ships an outbox event to the messaging infrastructure (RabbitMQ, Kafka, log).
 *
 * <p>Adapter pattern — keeps broker-specific types out of domain code
 * (design-patterns.md §3.10). Implementations:
 *
 * <ul>
 *   <li>{@code LoggingEventDispatcher} — default, logs payload (dev + tests)</li>
 *   <li>{@code RabbitMQEventDispatcher} — real broker (profile {@code rabbitmq-live})</li>
 * </ul>
 *
 * <p>Throw any exception on transient failure — publisher bumps retryCount and
 * schedules the next attempt with exponential backoff.
 *
 * @since 3.17.0 (Wave 3 Sub-PR 3.1, ADR-007)
 */
public interface EventDispatcher {

    void dispatch(OutboxEvent event) throws DispatchException;
}
