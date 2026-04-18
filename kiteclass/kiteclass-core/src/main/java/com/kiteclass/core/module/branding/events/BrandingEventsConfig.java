package com.kiteclass.core.module.branding.events;

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

    @Bean
    public TopicExchange brandingEventsExchange() {
        return new TopicExchange(BRANDING_EVENTS_EXCHANGE, /*durable*/ true, /*autoDelete*/ false);
    }
}
