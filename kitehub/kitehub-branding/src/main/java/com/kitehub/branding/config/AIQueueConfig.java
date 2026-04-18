package com.kitehub.branding.config;

import com.kitehub.branding.queue.AIJobPriority;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for tier-based AI job priority queues (GAP-005a).
 *
 * <p>3 tier queues with dedicated DLQs:
 * <ul>
 *   <li>{@code ai.request.enterprise} — Enterprise tier (weight 3)</li>
 *   <li>{@code ai.request.pro} — PREMIUM/BASIC tier (weight 2)</li>
 *   <li>{@code ai.request.free} — Free/Trial tier (weight 1)</li>
 * </ul>
 *
 * <p>Routing: a single {@link DirectExchange} ({@code ai.request.exchange})
 * routes by tier name. Each queue has its own DLQ ({@code ai.request.{tier}.dlq}).</p>
 *
 * <p>Feature flag: this topology is only created when {@code ai.queue.fair-queue-enabled=true}
 * (default). When disabled, jobs fall back to the legacy {@code branding-jobs} queue
 * in {@link RabbitMQConfig}.</p>
 *
 * @since 1.0
 */
@Configuration
@ConditionalOnProperty(name = "ai.queue.fair-queue-enabled", havingValue = "true", matchIfMissing = true)
public class AIQueueConfig {

    // Exchanges
    public static final String AI_EXCHANGE = "ai.request.exchange";
    public static final String AI_DLQ_EXCHANGE = "ai.request.exchange.dlq";

    // Primary queues
    public static final String QUEUE_ENTERPRISE = "ai.request.enterprise";
    public static final String QUEUE_PRO = "ai.request.pro";
    public static final String QUEUE_FREE = "ai.request.free";

    // Routing keys (same as queue names for direct exchange)
    public static final String ROUTING_KEY_ENTERPRISE = "ai.request.enterprise";
    public static final String ROUTING_KEY_PRO = "ai.request.pro";
    public static final String ROUTING_KEY_FREE = "ai.request.free";

    // DLQs
    public static final String DLQ_ENTERPRISE = "ai.request.enterprise.dlq";
    public static final String DLQ_PRO = "ai.request.pro.dlq";
    public static final String DLQ_FREE = "ai.request.free.dlq";

    // DLQ routing keys
    public static final String DLQ_ROUTING_KEY_ENTERPRISE = "ai.request.enterprise.dlq";
    public static final String DLQ_ROUTING_KEY_PRO = "ai.request.pro.dlq";
    public static final String DLQ_ROUTING_KEY_FREE = "ai.request.free.dlq";

    /** Main direct exchange routing jobs by tier. */
    @Bean
    public DirectExchange aiRequestExchange() {
        return new DirectExchange(AI_EXCHANGE);
    }

    /** DLQ exchange receiving rejected / expired jobs. */
    @Bean
    public DirectExchange aiRequestDlqExchange() {
        return new DirectExchange(AI_DLQ_EXCHANGE);
    }

    // --- Enterprise tier ------------------------------------------------------

    @Bean
    public Queue aiEnterpriseQueue() {
        return QueueBuilder.durable(QUEUE_ENTERPRISE)
                .withArgument("x-dead-letter-exchange", AI_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY_ENTERPRISE)
                .build();
    }

    @Bean
    public Queue aiEnterpriseDlq() {
        return QueueBuilder.durable(DLQ_ENTERPRISE).build();
    }

    @Bean
    public Binding aiEnterpriseBinding(Queue aiEnterpriseQueue, DirectExchange aiRequestExchange) {
        return BindingBuilder.bind(aiEnterpriseQueue)
                .to(aiRequestExchange)
                .with(ROUTING_KEY_ENTERPRISE);
    }

    @Bean
    public Binding aiEnterpriseDlqBinding(Queue aiEnterpriseDlq, DirectExchange aiRequestDlqExchange) {
        return BindingBuilder.bind(aiEnterpriseDlq)
                .to(aiRequestDlqExchange)
                .with(DLQ_ROUTING_KEY_ENTERPRISE);
    }

    // --- Pro tier -------------------------------------------------------------

    @Bean
    public Queue aiProQueue() {
        return QueueBuilder.durable(QUEUE_PRO)
                .withArgument("x-dead-letter-exchange", AI_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY_PRO)
                .build();
    }

    @Bean
    public Queue aiProDlq() {
        return QueueBuilder.durable(DLQ_PRO).build();
    }

    @Bean
    public Binding aiProBinding(Queue aiProQueue, DirectExchange aiRequestExchange) {
        return BindingBuilder.bind(aiProQueue)
                .to(aiRequestExchange)
                .with(ROUTING_KEY_PRO);
    }

    @Bean
    public Binding aiProDlqBinding(Queue aiProDlq, DirectExchange aiRequestDlqExchange) {
        return BindingBuilder.bind(aiProDlq)
                .to(aiRequestDlqExchange)
                .with(DLQ_ROUTING_KEY_PRO);
    }

    // --- Free tier ------------------------------------------------------------

    @Bean
    public Queue aiFreeQueue() {
        return QueueBuilder.durable(QUEUE_FREE)
                .withArgument("x-dead-letter-exchange", AI_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY_FREE)
                .build();
    }

    @Bean
    public Queue aiFreeDlq() {
        return QueueBuilder.durable(DLQ_FREE).build();
    }

    @Bean
    public Binding aiFreeBinding(Queue aiFreeQueue, DirectExchange aiRequestExchange) {
        return BindingBuilder.bind(aiFreeQueue)
                .to(aiRequestExchange)
                .with(ROUTING_KEY_FREE);
    }

    @Bean
    public Binding aiFreeDlqBinding(Queue aiFreeDlq, DirectExchange aiRequestDlqExchange) {
        return BindingBuilder.bind(aiFreeDlq)
                .to(aiRequestDlqExchange)
                .with(DLQ_ROUTING_KEY_FREE);
    }

    /**
     * Map a priority to its primary queue name — used by consumers
     * ({@code @RabbitListener(queues = ...)}) and by the dispatcher.
     *
     * @param priority job priority
     * @return primary queue name (never null)
     */
    public static String queueFor(AIJobPriority priority) {
        return switch (priority) {
            case ENTERPRISE -> QUEUE_ENTERPRISE;
            case PRO -> QUEUE_PRO;
            case FREE -> QUEUE_FREE;
        };
    }

    /**
     * Map a priority to its routing key for direct exchange publishing.
     *
     * @param priority job priority
     * @return routing key (never null)
     */
    public static String routingKeyFor(AIJobPriority priority) {
        return switch (priority) {
            case ENTERPRISE -> ROUTING_KEY_ENTERPRISE;
            case PRO -> ROUTING_KEY_PRO;
            case FREE -> ROUTING_KEY_FREE;
        };
    }
}
