package com.kiteclass.core.module.clazz.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.clazz.event.ClassRescheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Email-dispatching consumer for {@link ClassRescheduledEvent}.
 *
 * <p>Activated only when feature flag {@code kite.class.reschedule.notify.enabled=true}
 * (Phase 1.5+ scope per cross-bucket LOCKED decision §3.6 Wave beta-readiness-4).
 *
 * <p>Forwards the event to the {@code class.rescheduled.email.queue} queue
 * (consumed by kitehub-email service). Email service builds the Thymeleaf template
 * with persona-specific greeting (parent: "Kính gửi quý phụ huynh,") and dispatches
 * via the configured provider (Resend/SES per {@code email.provider}).
 *
 * <p>Notification classification = OPERATIONAL — bypasses {@code marketing_consented}
 * gate per cross-bucket decision (operational notifications are always sent).
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "kite.class.reschedule.notify.enabled",
        havingValue = "true"
)
public class ClassRescheduledEmailConsumer {

    /** Queue consumed by kitehub-email service to render + send Thymeleaf template. */
    public static final String EMAIL_DISPATCH_QUEUE = "class.rescheduled.email.queue";

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "class.rescheduled.queue")
    public void handle(String payloadJson) {
        try {
            ClassRescheduledEvent event = objectMapper.readValue(payloadJson, ClassRescheduledEvent.class);
            log.info("[EMAIL] Forwarding ClassRescheduledEvent to email dispatch: classId={}, className={}",
                    event.classId(), event.className());

            // Forward to kitehub-email queue. Operational classification — bypass marketing_consented.
            rabbitTemplate.convertAndSend(EMAIL_DISPATCH_QUEUE, payloadJson);
        } catch (JsonProcessingException ex) {
            log.error("[EMAIL] Failed to deserialize ClassRescheduledEvent payload", ex);
            // Swallow — we don't want broken payloads to clog the queue forever.
            // Production should use a DLQ on this queue (per ops-readiness-audit Cat 2.4).
        }
    }
}
