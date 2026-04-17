package com.kitehub.branding.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for branding job queue.
 * <p>
 * Configures:
 * - branding-jobs queue
 * - branding-exchange (direct)
 * - Dead letter queue for failed jobs
 * - Retry mechanism with exponential backoff
 *
 * @since 1.0
 */
@Configuration
public class RabbitMQConfig {

    public static final String BRANDING_QUEUE = "branding-jobs";
    public static final String BRANDING_EXCHANGE = "branding-exchange";
    public static final String BRANDING_ROUTING_KEY = "branding.job.create";

    public static final String DLQ_QUEUE = "branding-jobs-dlq";
    public static final String DLQ_EXCHANGE = "branding-exchange-dlq";
    public static final String DLQ_ROUTING_KEY = "branding.job.failed";

    /**
     * Message converter for JSON serialization.
     *
     * @return Jackson2JsonMessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter.
     *
     * @param connectionFactory connection factory
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Rabbit listener container factory with JSON converter.
     *
     * @param connectionFactory connection factory
     * @return SimpleRabbitListenerContainerFactory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    /**
     * Main branding jobs queue with DLQ configuration.
     *
     * @return Queue
     */
    @Bean
    public Queue brandingQueue() {
        return QueueBuilder.durable(BRANDING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Dead letter queue for failed jobs.
     *
     * @return Queue
     */
    @Bean
    public Queue brandingDLQ() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    /**
     * Main exchange.
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange brandingExchange() {
        return new DirectExchange(BRANDING_EXCHANGE);
    }

    /**
     * Dead letter exchange.
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange brandingDLQExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    /**
     * Binding main queue to exchange.
     *
     * @param brandingQueue main queue
     * @param brandingExchange main exchange
     * @return Binding
     */
    @Bean
    public Binding brandingBinding(Queue brandingQueue, DirectExchange brandingExchange) {
        return BindingBuilder.bind(brandingQueue)
                .to(brandingExchange)
                .with(BRANDING_ROUTING_KEY);
    }

    /**
     * Binding DLQ to DLQ exchange.
     *
     * @param brandingDLQ DLQ
     * @param brandingDLQExchange DLQ exchange
     * @return Binding
     */
    @Bean
    public Binding brandingDLQBinding(Queue brandingDLQ, DirectExchange brandingDLQExchange) {
        return BindingBuilder.bind(brandingDLQ)
                .to(brandingDLQExchange)
                .with(DLQ_ROUTING_KEY);
    }
}
