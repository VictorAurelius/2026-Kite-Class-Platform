package com.kiteclass.core.common.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Default {@link EventDispatcher} — logs payload and treats every dispatch as successful.
 *
 * <p>Used in tests, dev, and production until {@code RabbitMQEventDispatcher} is wired in a
 * later sub-PR under the {@code rabbitmq-live} profile.
 *
 * @since 3.17.0
 */
@Component
@Primary
@ConditionalOnMissingBean(name = "rabbitMQEventDispatcher")
@Slf4j
public class LoggingEventDispatcher implements EventDispatcher {

    @Override
    public void dispatch(OutboxEvent event) {
        log.info("[outbox] dispatch type={} aggregate={}:{} payload={}",
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getPayload());
    }
}
