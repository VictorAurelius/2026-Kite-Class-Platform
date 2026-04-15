package com.kiteclass.core.common.audit;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thin wrapper around {@link AuditLogRepository} enforcing foundation rules:
 * <ul>
 *   <li>Caller must be inside an existing transaction ({@link Propagation#MANDATORY}) —
 *       audit rows commit with the domain change they describe</li>
 *   <li>Payload silently truncated to {@value #MAX_PAYLOAD_CHARS} chars to protect DB</li>
 *   <li>Reason silently truncated to {@value #MAX_REASON_CHARS} chars</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 *   @Transactional
 *   public void reject(Long id, String reason, Long actor) {
 *       var row = repo.save(...);
 *       auditLog.record(AuditLogEvent.builder()
 *           .actionType("rebrand.rejected")
 *           .aggregateType("RebrandApproval")
 *           .aggregateId(String.valueOf(id))
 *           .actorUserId(actor)
 *           .reason(reason)
 *           .build());
 *   }
 * }</pre>
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.0)
 */
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    public static final int MAX_PAYLOAD_CHARS = 8_000;
    public static final int MAX_REASON_CHARS = 500;

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog record(AuditLogEvent event) {
        AuditLog row = AuditLog.builder()
                .actionType(event.getActionType())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .actorUserId(event.getActorUserId())
                .actorRole(event.getActorRole())
                .payload(truncate(event.getPayload(), MAX_PAYLOAD_CHARS))
                .reason(truncate(event.getReason(), MAX_REASON_CHARS))
                .build();
        return repository.save(row);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    @Value
    @Builder
    public static class AuditLogEvent {
        String actionType;
        String aggregateType;
        String aggregateId;
        Long actorUserId;
        String actorRole;
        String payload;
        String reason;
    }
}
