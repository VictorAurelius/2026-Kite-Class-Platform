package com.kiteclass.core.module.branding.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for tenant branding lifecycle events.
 *
 * <p>Exchange {@value #BRANDING_EVENTS_EXCHANGE} is a topic exchange carrying
 * fine-grained routing keys such as {@code branding.updated} and future
 * {@code branding.rolled-back}. Consumers (e.g. {@code kitehub-email}) bind
 * their own queues to the routing keys they care about.
 *
 * <p>GAP-1213: also carries the cross-service {@code branding.deployed} event
 * published by kitehub-branding (same exchange name + topic type — idempotent
 * re-declaration). {@link #brandingDeployedQueue()} is bound + consumed by
 * {@code BrandingDeployedEventConsumer} which applies the deployed theme to the
 * tenant landing page so the PUBLIC landing actually changes after wizard deploy.
 *
 * <p>Enabled by default; disable via {@code branding.events.enabled=false}
 * (tests, environments without broker).
 *
 * @since Wave 4 (GAP-021)
 */
@Configuration
@ConditionalOnProperty(name = "branding.events.enabled", havingValue = "true", matchIfMissing = true)
public class BrandingEventsConfig {

    public static final String BRANDING_EVENTS_EXCHANGE = "branding.events";
    public static final String ROUTING_KEY_UPDATED = "branding.updated";
    public static final String ROUTING_KEY_ROLLED_BACK = "branding.rolled-back";

    /** GAP-1213 — cross-service deploy event from kitehub-branding. */
    public static final String ROUTING_KEY_DEPLOYED = "branding.deployed";
    public static final String DEPLOYED_QUEUE = "branding.deployed.kiteclass.queue";

    @Bean
    public TopicExchange brandingEventsExchange() {
        return new TopicExchange(BRANDING_EVENTS_EXCHANGE, /*durable*/ true, /*autoDelete*/ false);
    }

    /** GAP-1213 — queue receiving {@code branding.deployed} for landing theme application. */
    @Bean
    public Queue brandingDeployedQueue() {
        return QueueBuilder.durable(DEPLOYED_QUEUE).build();
    }

    /** GAP-1213 — bind {@link #brandingDeployedQueue()} to {@code branding.events} (branding.deployed). */
    @Bean
    public Binding brandingDeployedBinding(Queue brandingDeployedQueue, TopicExchange brandingEventsExchange) {
        return BindingBuilder.bind(brandingDeployedQueue).to(brandingEventsExchange).with(ROUTING_KEY_DEPLOYED);
    }
}
