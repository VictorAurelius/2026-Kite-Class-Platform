package com.kitehub.subscription.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * RabbitMQ configuration for email send queue.
 * <p>
 * Configures:
 * - email.send queue (durable) with DLQ routing
 * - email.dlq for failed emails after retry exhaustion
 * - email.exchange (direct)
 * - JSON message converter for EmailEvent serialization
 * <p>
 * Retry: 3 attempts, initial 60s, multiplier 3x (configured in application.yml).
 * Activated only when kitehub.email.use-queue=true (default).
 *
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "kitehub.email.use-queue", havingValue = "true", matchIfMissing = true)
public class EmailQueueConfig {

    public static final String EMAIL_QUEUE = "email.send";
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.send";

    public static final String EMAIL_DLQ = "email.dlq";
    public static final String EMAIL_DLQ_EXCHANGE = "email.exchange.dlq";
    public static final String EMAIL_DLQ_ROUTING_KEY = "email.dlq";

    /**
     * JSON message converter for RabbitMQ messages.
     */
    @Bean
    public MessageConverter emailJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate configured with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(emailJsonMessageConverter());
        return template;
    }

    /**
     * Listener container factory with JSON converter + DLQ-on-failure semantics.
     *
     * <p>GAP-607 Wave 91 Bucket A — explicit RMQ DLQ wiring:
     * <ul>
     *   <li>{@code setDefaultRequeueRejected(false)} — rejected messages KHÔNG re-queue
     *       infinitely; instead go through advice chain → eventually rejected → DLX routes
     *       to {@code email.dlq}.</li>
     *   <li>RetryInterceptor: max 3 attempts với exponential backoff (1s → 2s → 4s, max 60s).
     *       After exhaustion, {@link RejectAndDontRequeueRecoverer} rejects message →
     *       broker routes to DLX.</li>
     * </ul>
     * Trước fix này, EmailConsumer poison messages (vd template missing GAP-606) retry
     * infinitely → log spam + CPU waste. Per Wave 90 evidence ~10×/sec cho >24h.</p>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(emailJsonMessageConverter());
        // Rejected messages MUST NOT re-queue forever — DLX routing kicks in instead.
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    /**
     * Retry advice cho RMQ listener: 3 attempts exponential backoff, then DLQ.
     *
     * <p>Caller stacking: Spring {@code listener.simple.retry} (in application.yml) đã
     * configure 3 attempts at higher level; this bean tightens behavior bằng cách wire
     * {@link RejectAndDontRequeueRecoverer} — ensures exhausted messages route via DLX
     * thay vì stay in-queue.</p>
     */
    @Bean
    public Advice retryInterceptor() {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(60_000L);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return RetryInterceptorBuilder.stateless()
            .retryOperations(retryTemplate)
            .recoverer(new RejectAndDontRequeueRecoverer())
            .build();
    }

    /**
     * Main email send queue with DLQ routing on rejection.
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Dead letter queue for emails that failed after all retries.
     */
    @Bean
    public Queue emailDLQ() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    /**
     * Main email exchange (direct).
     */
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    /**
     * Dead letter exchange for failed emails.
     */
    @Bean
    public DirectExchange emailDLQExchange() {
        return new DirectExchange(EMAIL_DLQ_EXCHANGE);
    }

    /**
     * Bind email queue to exchange with routing key.
     */
    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(emailExchange)
                .with(EMAIL_ROUTING_KEY);
    }

    /**
     * Bind DLQ to DLQ exchange.
     */
    @Bean
    public Binding emailDLQBinding(Queue emailDLQ, DirectExchange emailDLQExchange) {
        return BindingBuilder.bind(emailDLQ)
                .to(emailDLQExchange)
                .with(EMAIL_DLQ_ROUTING_KEY);
    }
}
