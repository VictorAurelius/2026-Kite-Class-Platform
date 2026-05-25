package com.kitehub.admin.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ topology consumed by
 * {@link CrossServiceCacheInvalidationListener}.
 *
 * <p>Closes <strong>GAP-237</strong>. The kitehub-side per-module Outbox pattern
 * (ADR-021) writes events to the {@code subscription_outbox} table; a
 * (deferred) dispatcher publishes them to {@code kitehub.events.exchange}
 * (TopicExchange). This config declares the two consumer queues that admin
 * binds to so cache eviction triggers on cross-service writes.</p>
 *
 * <h3>Topology</h3>
 * <pre>
 *   exchange:  kitehub.events.exchange (topic, durable)
 *
 *   queue:     kitehub.admin.subscription-events  ← routing key: subscription.*
 *   queue:     kitehub.admin.instance-events      ← routing key: instance.*
 * </pre>
 *
 * <h3>Test profile</h3>
 * <p>The admin test profile excludes {@code RabbitAutoConfiguration} (see
 * {@code application-test.yml}); Spring will skip this config when the
 * connection factory bean is absent. The {@code @ConditionalOnClass} guard is
 * defensive — without {@code spring-boot-starter-amqp} on the classpath the
 * config is silently no-op, mirroring the subscription module pattern.</p>
 */
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(name = "kitehub.admin.cross-service-cache-invalidation.enabled",
        havingValue = "true", matchIfMissing = false)
public class RabbitListenerConfig {

    /** Topic exchange shared by all kitehub-side cross-service domain events. */
    public static final String EVENTS_EXCHANGE = "kitehub.events.exchange";

    /** Queue for {@code subscription.*} events bound to the events exchange. */
    public static final String ADMIN_SUBSCRIPTION_EVENTS_QUEUE = "kitehub.admin.subscription-events";

    /** Queue for {@code instance.*} events bound to the events exchange. */
    public static final String ADMIN_INSTANCE_EVENTS_QUEUE = "kitehub.admin.instance-events";

    /** Wildcard routing key matching every {@code subscription.*} event. */
    public static final String SUBSCRIPTION_ROUTING_KEY = "subscription.*";

    /** Wildcard routing key matching every {@code instance.*} event. */
    public static final String INSTANCE_ROUTING_KEY = "instance.*";

    /**
     * Topic exchange used for cross-service domain events on the kitehub side.
     * Declared idempotently — if the producer (kitehub-subscription dispatcher)
     * already declared it, this is a no-op.
     */
    @Bean
    public TopicExchange kitehubEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    /** Durable queue for subscription lifecycle events (admin-side cache eviction). */
    @Bean
    public Queue adminSubscriptionEventsQueue() {
        return QueueBuilder.durable(ADMIN_SUBSCRIPTION_EVENTS_QUEUE).build();
    }

    /** Durable queue for instance lifecycle events (admin-side cache eviction). */
    @Bean
    public Queue adminInstanceEventsQueue() {
        return QueueBuilder.durable(ADMIN_INSTANCE_EVENTS_QUEUE).build();
    }

    @Bean
    public Binding adminSubscriptionEventsBinding(Queue adminSubscriptionEventsQueue,
                                                  TopicExchange kitehubEventsExchange) {
        return BindingBuilder.bind(adminSubscriptionEventsQueue)
                .to(kitehubEventsExchange)
                .with(SUBSCRIPTION_ROUTING_KEY);
    }

    @Bean
    public Binding adminInstanceEventsBinding(Queue adminInstanceEventsQueue,
                                              TopicExchange kitehubEventsExchange) {
        return BindingBuilder.bind(adminInstanceEventsQueue)
                .to(kitehubEventsExchange)
                .with(INSTANCE_ROUTING_KEY);
    }

    // NOTE on infrastructure beans (RabbitTemplate, MessageConverter,
    // SimpleRabbitListenerContainerFactory): all three are already provided by
    // {@code com.kitehub.subscription.config.EmailQueueConfig} which the admin
    // module pulls in via the {@code kitehub-subscription} dependency. Adding a
    // duplicate {@code RabbitTemplate} bean here breaks tests that use
    // {@code @MockitoBean RabbitTemplate} with "expected a single matching bean"
    // (multiple primary candidates). The listener container factory configured
    // there uses {@code Jackson2JsonMessageConverter} so payloads arrive as
    // {@code Map<String,Object>} — exactly what
    // {@link CrossServiceCacheInvalidationListener} expects.
}
