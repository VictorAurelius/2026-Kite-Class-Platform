package com.kiteclass.core.common.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogWriterTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditLogWriter writer;

    @Test
    void record_persists_basic_event() {
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog saved = writer.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("rebrand.rejected")
                .aggregateType("RebrandApproval")
                .aggregateId("100")
                .actorUserId(42L)
                .actorRole("ADMIN")
                .reason("off-brand colours")
                .build());

        assertThat(saved.getActionType()).isEqualTo("rebrand.rejected");
        assertThat(saved.getAggregateId()).isEqualTo("100");
        assertThat(saved.getActorUserId()).isEqualTo(42L);
    }

    @Test
    void record_truncates_payload_beyond_max() {
        String payload = "x".repeat(AuditLogWriter.MAX_PAYLOAD_CHARS + 500);
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        writer.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("big.payload").aggregateType("X").aggregateId("1")
                .payload(payload).build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPayload())
                .hasSize(AuditLogWriter.MAX_PAYLOAD_CHARS)
                .endsWith("...");
    }

    @Test
    void record_truncates_reason_beyond_max() {
        String reason = "y".repeat(AuditLogWriter.MAX_REASON_CHARS + 100);
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        writer.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("x").aggregateType("X").aggregateId("1")
                .reason(reason).build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReason())
                .hasSize(AuditLogWriter.MAX_REASON_CHARS)
                .endsWith("...");
    }

    @Test
    void record_passes_null_fields_through() {
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog saved = writer.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType("x").aggregateType("X").aggregateId("1").build());

        assertThat(saved.getPayload()).isNull();
        assertThat(saved.getReason()).isNull();
        assertThat(saved.getActorUserId()).isNull();
    }
}
