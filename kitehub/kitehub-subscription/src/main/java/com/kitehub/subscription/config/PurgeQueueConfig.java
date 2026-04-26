package com.kitehub.subscription.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for instance purge events.
 * <p>
 * Uses a fanout exchange so multiple services (branding, kiteclass, etc.)
 * can independently consume purge events for cross-service cleanup.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
public class PurgeQueueConfig {

    public static final String PURGE_EXCHANGE = "instance.purge.exchange";
    public static final String PURGE_QUEUE_SUBSCRIPTION = "instance.purge.subscription";
    public static final String PURGE_ROUTING_KEY = "instance.purge";

    /** Outbox event-type label for instance purge. Stable contract for outbox dispatcher + consumers. */
    public static final String EVENT_TYPE_PURGE_REQUESTED = "instance.purge.requested";

    /**
     * Fanout exchange for purge events — multiple consumers can bind.
     */
    @Bean
    public FanoutExchange purgeExchange() {
        return new FanoutExchange(PURGE_EXCHANGE, true, false);
    }

    /**
     * Queue for subscription service's own purge-related cleanup.
     */
    @Bean
    public Queue purgeSubscriptionQueue() {
        return QueueBuilder.durable(PURGE_QUEUE_SUBSCRIPTION).build();
    }

    /**
     * Bind subscription purge queue to the fanout exchange.
     */
    @Bean
    public Binding purgeSubscriptionBinding(Queue purgeSubscriptionQueue, FanoutExchange purgeExchange) {
        return BindingBuilder.bind(purgeSubscriptionQueue).to(purgeExchange);
    }
}
