package com.kiteclass.core.common.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@Configuration
public class RabbitConfig {

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

    // Other exchanges, queues, and bindings are defined per-module as event-driven
    // features are implemented (e.g., branding topic exchange in BrandingEventsConfig).

    /**
     * Eagerly declare RabbitMQ queues on application startup (GAP-825).
     *
     * <p>Spring AMQP's auto-configured {@link RabbitAdmin} normally declares queue
     * beans via a {@code ConnectionListener} on first broker connection. With ONLY
     * Queue beans (no Exchange/Binding to anchor the topology), some startup
     * scenarios race: {@code @RabbitListener} containers begin {@code queueDeclarePassive}
     * BEFORE RabbitAdmin completes its eager declaration, and fresh brokers return
     * {@code NOT_FOUND} → consumer abort → Spring context refresh fails → kc-core
     * unhealthy on cold rebuild.</p>
     *
     * <p>Surfaced 2026-06-02 (Wave local-doable-5 Bucket A): fresh WSL cold rebuild
     * → kiteclass-core failed startup with
     * {@code Failed to declare queue [class.rescheduled.queue]} (404 NOT_FOUND). Manual
     * workaround: {@code rabbitmqadmin declare queue name=class.rescheduled.queue
     * durable=true} + restart container. This {@link ApplicationRunner} eliminates
     * the workaround by declaring eagerly via the auto-configured admin.</p>
     *
     * @param rabbitAdmin Spring Boot auto-configured admin
     * @param classRescheduledQueue queue bean for class-reschedule events
     * @param classRescheduledEmailQueue queue bean for email-dispatch sister path
     * @return runner that declares both queues at application-ready time
     */
    @Bean
    public ApplicationRunner declareRabbitQueuesEagerly(
            RabbitAdmin rabbitAdmin,
            Queue classRescheduledQueue,
            Queue classRescheduledEmailQueue) {
        return args -> {
            rabbitAdmin.declareQueue(classRescheduledQueue);
            rabbitAdmin.declareQueue(classRescheduledEmailQueue);
            log.info("Declared RabbitMQ queues eagerly: {}, {}",
                    classRescheduledQueue.getName(),
                    classRescheduledEmailQueue.getName());
        };
    }
}
