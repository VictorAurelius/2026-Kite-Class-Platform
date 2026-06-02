package com.kiteclass.core.module.clazz.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.idempotency.EmailIdempotencyGuard;
import com.kiteclass.core.module.clazz.event.ClassRescheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
 * <p><strong>GAP-840 (Wave local-doable-6 Bucket H) — sister-path idempotency:</strong>
 * RabbitMQ {@code class.rescheduled.queue} is at-least-once. Without dedup, a crash
 * AFTER {@code rabbitTemplate.convertAndSend(...)} but BEFORE the inbound ack causes
 * the broker to redeliver the {@code ClassRescheduledEvent}, producing a duplicate
 * forward to {@code class.rescheduled.email.queue} → duplicate email at the recipient.
 * {@link EmailIdempotencyGuard} (Redis SETNX + Caffeine fallback, key prefix
 * {@code class-reschedule:idempotency:*}) suppresses the duplicate forward.
 *
 * <p>Note: this guard is INDEPENDENT of the GAP-580 guard at the {@code email.send}
 * queue (kitehub-email side) — different queue, different payload shape, different
 * Redis key namespace. Both layers together cover the full pipeline.
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291); GAP-840 dedup Wave local-doable-6 Bucket H
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
    private final EmailIdempotencyGuard idempotencyGuard;

    @RabbitListener(queues = "class.rescheduled.queue")
    public void handle(String payloadJson) {
        ClassRescheduledEvent event;
        try {
            event = objectMapper.readValue(payloadJson, ClassRescheduledEvent.class);
        } catch (JsonProcessingException ex) {
            log.error("[EMAIL] Failed to deserialize ClassRescheduledEvent payload", ex);
            // Swallow — broken payloads must not clog the queue forever (DLQ per
            // ops-readiness-audit Cat 2.4).
            return;
        }

        // GAP-840 producer-side dedup: at-least-once inbound redelivery would otherwise
        // produce a duplicate forward to class.rescheduled.email.queue. Key derived
        // from classId + rescheduledAt + sorted recipient set so identical reschedule
        // events always collapse to the same dedup key.
        String idempotencyKey = idempotencyGuard.computeKey(
                event.classId(),
                event.rescheduledAt(),
                recipientListKey(event.parentUserIds(), event.enrolledStudentIds()));
        if (!idempotencyGuard.markIfFirstSeen(idempotencyKey)) {
            log.info("[EMAIL] Skipping duplicate ClassRescheduled forward (idempotent): classId={}",
                    event.classId());
            return;
        }

        log.info("[EMAIL] Forwarding ClassRescheduledEvent to email dispatch: classId={}, className={}",
                event.classId(), event.className());

        // Forward to kitehub-email queue. Operational classification — bypass marketing_consented.
        rabbitTemplate.convertAndSend(EMAIL_DISPATCH_QUEUE, payloadJson);
    }

    /**
     * Build a stable string capturing the recipient set so identical broadcasts
     * collapse to the same dedup key regardless of input list order.
     */
    static String recipientListKey(List<Long> parentUserIds, List<Long> enrolledStudentIds) {
        return "parents=" + sortedJoin(parentUserIds)
                + ";students=" + sortedJoin(enrolledStudentIds);
    }

    private static String sortedJoin(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
