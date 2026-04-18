package com.kitehub.email.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for tenant {@code branding.updated} events.
 *
 * <p>kiteclass-core publishes to exchange {@code branding.events} with routing key
 * {@code branding.updated}; this service binds a durable queue
 * {@code email.branding.updated} so cache invalidation survives restarts.
 *
 * <p>Enabled by default ({@code kitehub.email.branding.rabbit-enabled=true}).
 * Disable in tests or environments without RabbitMQ.
 *
 * @since Wave 4 (GAP-021)
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(
        name = "kitehub.email.branding.rabbit-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class BrandingEventsConfig {

    public static final String BRANDING_EVENTS_EXCHANGE = "branding.events";
    public static final String BRANDING_UPDATED_ROUTING_KEY = "branding.updated";
    public static final String EMAIL_BRANDING_UPDATED_QUEUE = "email.branding.updated";

    @Bean
    public TopicExchange brandingEventsExchange() {
        return new TopicExchange(BRANDING_EVENTS_EXCHANGE, /*durable*/ true, /*autoDelete*/ false);
    }

    @Bean
    public Queue emailBrandingUpdatedQueue() {
        return QueueBuilder.durable(EMAIL_BRANDING_UPDATED_QUEUE).build();
    }

    @Bean
    public Binding emailBrandingUpdatedBinding(Queue emailBrandingUpdatedQueue,
                                               TopicExchange brandingEventsExchange) {
        return BindingBuilder.bind(emailBrandingUpdatedQueue)
                .to(brandingEventsExchange)
                .with(BRANDING_UPDATED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter brandingJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate brandingRabbitTemplate(ConnectionFactory connectionFactory,
                                                 MessageConverter brandingJsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(brandingJsonMessageConverter);
        return template;
    }
}
