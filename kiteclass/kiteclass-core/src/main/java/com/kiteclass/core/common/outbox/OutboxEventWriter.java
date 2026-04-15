package com.kiteclass.core.common.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Services call this helper inside their own {@code @Transactional} block so the outbox
 * row commits atomically with the domain change.
 *
 * <p>The helper itself uses {@code Propagation.MANDATORY} to enforce "caller must have an
 * open transaction" — no accidental auto-commit creating lost-update windows.
 *
 * <p>Usage:
 * <pre>{@code
 *   @Transactional
 *   public void markBrandingCompleted(long id, String url) {
 *       var i = load(id);
 *       i.transitionTo(DEPLOYED);
 *       repository.save(i);
 *       outbox.enqueue("instance.deployed", "FrontendInstance", String.valueOf(id),
 *                       objectMapper.writeValueAsString(payload));
 *   }
 * }</pre>
 *
 * @since 3.17.0 (Wave 3 Sub-PR 3.1, ADR-007)
 */
@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

    private final OutboxEventRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent enqueue(String eventType, String aggregateType, String aggregateId, String payloadJson) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payloadJson)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        return repository.save(event);
    }
}
