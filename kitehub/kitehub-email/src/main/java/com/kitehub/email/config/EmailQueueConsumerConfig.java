package com.kitehub.email.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the standard transactional email queue ({@code email.send}).
 *
 * <p><strong>Why this config exists (GAP-787 / GAP-702):</strong> kitehub-subscription's
 * {@code EmailServiceClient.publishToQueue(...)} publishes every transactional
 * {@code EmailEvent} (welcome, trial warnings, beta-invite, staff-invite, DSAR, etc.)
 * to exchange {@code email.exchange} with routing key {@code email.send} when
 * {@code kitehub.email.use-queue=true} (the production default). Until now kitehub-email
 * declared NO consumer for that queue — only {@code branding.updated} and
 * {@code class.rescheduled} had listeners. In queue mode every transactional email was
 * therefore silently dropped (Wave meta-6 Bucket A walk Bug #14 surfaced this for the
 * staff-invite path; Wave 103 verify surfaced the same for beta-invite). This config
 * declares the consumer-side topology so {@link com.kitehub.email.listener.EmailEventListener}
 * can bind and deliver.</p>
 *
 * <p>The topology MUST byte-match the producer side
 * ({@code com.kitehub.subscription.config.EmailQueueConfig}) so producer + consumer
 * resolve the same durable queue:</p>
 * <ul>
 *   <li>exchange {@code email.exchange} (direct, durable)</li>
 *   <li>queue {@code email.send} (durable) with DLQ routing</li>
 *   <li>routing key {@code email.send}</li>
 *   <li>DLQ {@code email.dlq} via {@code email.exchange.dlq}</li>
 * </ul>
 *
 * <p><strong>Auto-declare mandate (per GAP-787 §Step 5 — avoid Bug #6 recurrence):</strong>
 * all queue/exchange/binding beans are declared via Spring AMQP so RabbitMQ creates them
 * on startup. No manual {@code rabbitmqadmin} step required. Idempotent re-declaration is
 * safe because the arguments match the producer side.</p>
 *
 * <p>Enabled by default; disable in tests / RabbitMQ-less environments via
 * {@code kitehub.email.use-queue=false}.</p>
 *
 * @since Wave phase2-beta (GAP-787)
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(
        name = "kitehub.email.use-queue",
        havingValue = "true",
        matchIfMissing = true)
public class EmailQueueConsumerConfig {

    /** MUST match {@code EmailQueueConfig.EMAIL_QUEUE} on the subscription producer side. */
    public static final String EMAIL_QUEUE = "email.send";
    /** MUST match {@code EmailQueueConfig.EMAIL_EXCHANGE}. */
    public static final String EMAIL_EXCHANGE = "email.exchange";
    /** MUST match {@code EmailQueueConfig.EMAIL_ROUTING_KEY}. */
    public static final String EMAIL_ROUTING_KEY = "email.send";

    public static final String EMAIL_DLQ = "email.dlq";
    public static final String EMAIL_DLQ_EXCHANGE = "email.exchange.dlq";
    public static final String EMAIL_DLQ_ROUTING_KEY = "email.dlq";

    /** JSON converter so {@code EmailEvent} payloads deserialize on consume. */
    @Bean
    public MessageConverter emailQueueJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate scoped to the email-send topology. Named distinctly from the
     * branding template ({@code brandingRabbitTemplate}) to avoid a primary-bean clash.
     */
    @Bean
    public RabbitTemplate emailQueueRabbitTemplate(ConnectionFactory connectionFactory,
                                                   MessageConverter emailQueueJsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(emailQueueJsonMessageConverter);
        return template;
    }

    /**
     * Main email send queue with DLQ routing on rejection — mirrors the producer-side
     * declaration so the durable queue arguments are consistent across services.
     */
    @Bean
    public Queue emailSendQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    /** Dead-letter queue for emails that exhaust retry on the consumer side. */
    @Bean
    public Queue emailSendDLQ() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public DirectExchange emailSendExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public DirectExchange emailSendDLQExchange() {
        return new DirectExchange(EMAIL_DLQ_EXCHANGE);
    }

    @Bean
    public Binding emailSendBinding(Queue emailSendQueue, DirectExchange emailSendExchange) {
        return BindingBuilder.bind(emailSendQueue)
                .to(emailSendExchange)
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding emailSendDLQBinding(Queue emailSendDLQ, DirectExchange emailSendDLQExchange) {
        return BindingBuilder.bind(emailSendDLQ)
                .to(emailSendDLQExchange)
                .with(EMAIL_DLQ_ROUTING_KEY);
    }
}
