package com.kiteclass.core.common.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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

    // Exchanges, queues, and bindings are defined per-module as event-driven features are implemented.
    // See individual module configs (e.g., notification, enrollment) for specific queue definitions.
}
