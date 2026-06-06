package com.kiteclass.core.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for message queue integration.
 *
 * <p>Configures:
 * <ul>
 *   <li>JSON message converter for DTOs</li>
 *   <li>RabbitTemplate for sending messages</li>
 *   <li>Listener container factory for consuming messages</li>
 *   <li>Exchanges, queues, and bindings (to be added)</li>
 * </ul>
 *
 * <p>Usage examples:
 * <pre>
 * // Sending messages
 * {@code @Autowired}
 * private RabbitTemplate rabbitTemplate;
 *
 * public void sendNotification(NotificationEvent event) {
 *     rabbitTemplate.convertAndSend("notifications.exchange", "notification.created", event);
 * }
 *
 * // Receiving messages
 * {@code @RabbitListener(queues = "notification.queue")}
 * public void handleNotification(NotificationEvent event) {
 *     // Process notification
 * }
 * </pre>
 *
 * <p>Exchanges, queues, and bindings will be added per-module as event-driven features are implemented.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Configuration
public class RabbitConfig {

    /**
     * Cross-service exchange owned by kitehub-subscription
     * ({@code EmailQueueConfig.EMAIL_EXCHANGE}). Declared here too so kiteclass-core can
     * bind {@link #tenantCreatedQueue()} to it. MUST stay a {@link DirectExchange} (exact
     * routing-key match) to match the producer-side declaration — a type mismatch would make
     * RabbitMQ reject the declaration at startup.
     */
    public static final String EMAIL_EXCHANGE = "email.exchange";

    /** Queue carrying {@code TenantCreatedEvent} from kitehub-subscription (GAP-945). */
    public static final String TENANT_CREATED_QUEUE = "tenant.created.queue";

    /** Routing key the producer emits as the event {@code topic} (GAP-945). */
    public static final String TENANT_CREATED_ROUTING_KEY = "tenant.created";

    /**
     * Configures JSON message converter for serializing/deserializing message payloads.
     *
     * @return Jackson2JsonMessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Explicit {@link RabbitAdmin} bean for runtime queue/exchange/binding declaration.
     *
     * <p>Spring AMQP {@link Queue} {@code @Bean} declarations (see {@link #classRescheduledQueue()}
     * and {@link #classRescheduledEmailQueue()} below) require a {@code RabbitAdmin} to actually
     * declare those queues against the broker at startup. Spring Boot's
     * {@code RabbitAutoConfiguration} normally provides one automatically, but explicit declaration:
     *
     * <ul>
     *   <li>Guarantees autowire availability regardless of autoconfig conditions
     *       (e.g., {@code spring.rabbitmq.dynamic=false} flag, conditional bean ordering)</li>
     *   <li>Eliminates ambiguity when Spring needs to satisfy {@code AmqpAdmin} dependencies
     *       in downstream beans (e.g., the implicit eager queue declarer)</li>
     *   <li>Matches the explicit-config style used by sister kitehub services
     *       (see {@code BacklogInspector} which autowires {@code AmqpAdmin})</li>
     * </ul>
     *
     * <p>Closes GAP-866 — kc-core startup crashloop caused by missing {@code RabbitAdmin}
     * autowire for the implicit eager queue declarer that Spring Boot creates when
     * {@code @Bean Queue} declarations are present.
     *
     * @param connectionFactory RabbitMQ connection factory (auto-injected by Spring Boot)
     * @return configured {@link RabbitAdmin}
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Configures RabbitTemplate with JSON message converter.
     *
     * @param connectionFactory RabbitMQ connection factory
     * @return configured RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Configures listener container factory with JSON message converter.
     *
     * @param connectionFactory RabbitMQ connection factory
     * @return configured SimpleRabbitListenerContainerFactory
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
     * Class-reschedule event queue.
     *
     * <p>Producers publish {@code ClassRescheduledEvent} payloads through the
     * Outbox dispatcher with routing key {@code class.rescheduled} via the
     * default (nameless) exchange — RabbitMQ routes by queue name. Consumed by
     * {@code ClassRescheduledNoOpConsumer} (notifications disabled, default) OR
     * {@code ClassRescheduledEmailConsumer} (notifications enabled, Phase 1.5+).
     *
     * <p>Declaring this durable queue bean fixes the IaC gap where the queue was
     * only runtime-declared by the {@code @RabbitListener}, leaving the broker
     * topology unmanaged and the core service unhealthy if the listener bean was
     * inactive.
     *
     * @return durable queue {@code class.rescheduled.queue}
     */
    @Bean
    public Queue classRescheduledQueue() {
        return QueueBuilder.durable("class.rescheduled.queue").build();
    }

    /**
     * Email-dispatch queue forwarded to by {@code ClassRescheduledEmailConsumer}.
     *
     * <p>Receives the serialized {@code ClassRescheduledEvent} via
     * {@code convertAndSend("class.rescheduled.email.queue", ...)} (default
     * exchange, routing key = queue name) and is consumed by the
     * {@code kitehub-email} service to render + send the Thymeleaf template.
     *
     * @return durable queue {@code class.rescheduled.email.queue}
     */
    @Bean
    public Queue classRescheduledEmailQueue() {
        return QueueBuilder.durable("class.rescheduled.email.queue").build();
    }

    /**
     * Cross-service {@code email.exchange} DirectExchange (GAP-945).
     *
     * <p>Owned by kitehub-subscription; re-declared here (idempotent — same name, type, durable)
     * so this service can bind {@link #tenantCreatedQueue()}. The subscription
     * {@code SubscriptionEventEmitter} / {@code SubscriptionOutboxDispatcher} publish every
     * cross-service event to this exchange routed by the event {@code topic}.
     *
     * @return durable DirectExchange {@code email.exchange}
     */
    @Bean
    public DirectExchange emailExchange() {
        return ExchangeBuilder.directExchange(EMAIL_EXCHANGE).durable(true).build();
    }

    /**
     * Queue receiving {@code TenantCreatedEvent} payloads — consumed by
     * {@code TenantCreatedEventConsumer} → {@code TenantProvisioningSaga.provision(...)} (GAP-945).
     *
     * @return durable queue {@code tenant.created.queue}
     */
    @Bean
    public Queue tenantCreatedQueue() {
        return QueueBuilder.durable(TENANT_CREATED_QUEUE).build();
    }

    /**
     * Binds {@link #tenantCreatedQueue()} to {@link #emailExchange()} with routing key
     * {@code tenant.created} — the exact key the producer emits as the event topic (GAP-945).
     *
     * @return binding tenant.created.queue → email.exchange (tenant.created)
     */
    @Bean
    public Binding tenantCreatedBinding(Queue tenantCreatedQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(tenantCreatedQueue).to(emailExchange).with(TENANT_CREATED_ROUTING_KEY);
    }

    // Other exchanges, queues, and bindings are defined per-module as event-driven
    // features are implemented (e.g., branding topic exchange in BrandingEventsConfig).
}
