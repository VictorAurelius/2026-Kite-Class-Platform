package com.kiteclass.core.module.clazz.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.clazz.event.ClassRescheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default no-op consumer for {@link ClassRescheduledEvent}.
 *
 * <p>Active by default (feature flag {@code kite.class.reschedule.notify.enabled=false})
 * per cross-bucket LOCKED decision §3.6 (Wave beta-readiness-4). When email/Zalo
 * notifications are enabled in Phase 1.5+, this bean is replaced by
 * {@link ClassRescheduledEmailConsumer}.
 *
 * <p>Logs only — provides observability that the event flowed end-to-end through
 * the Outbox dispatcher without triggering any user-visible side effect.
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "kite.class.reschedule.notify.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class ClassRescheduledNoOpConsumer {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "class.rescheduled.queue")
    public void handle(String payloadJson) {
        try {
            ClassRescheduledEvent event = objectMapper.readValue(payloadJson, ClassRescheduledEvent.class);
            log.info("[NO-OP] ClassRescheduledEvent received: classId={}, className={}, "
                            + "previousStartDate={}, newStartDate={}, reason={}, notification disabled",
                    event.classId(), event.className(),
                    event.previousStartDate(), event.newStartDate(), event.reasonCategory());
        } catch (JsonProcessingException ex) {
            log.warn("[NO-OP] Failed to deserialize ClassRescheduledEvent payload: {}", ex.getMessage());
        }
    }
}
