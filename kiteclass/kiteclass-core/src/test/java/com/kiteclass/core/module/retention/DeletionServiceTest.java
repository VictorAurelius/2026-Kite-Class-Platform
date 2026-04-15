package com.kiteclass.core.module.retention;

import com.kiteclass.core.common.audit.AuditLogWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletionServiceTest {

    @Mock
    private DeletionRequestRepository repository;

    @Mock
    private AuditLogWriter auditLog;

    @InjectMocks
    private DeletionService service;

    @BeforeEach
    void setGracePeriodDays() {
        ReflectionTestUtils.setField(service, "gracePeriodDays", 7L);
    }

    @Test
    void request_deletion_creates_pending_with_grace_window_and_audits() {
        UUID tenantId = UUID.randomUUID();
        when(repository.findFirstByUserIdAndStatusAndDeletedFalse(42L, DeletionStatus.PENDING))
                .thenReturn(Optional.empty());
        when(repository.findFirstByUserIdAndStatusAndDeletedFalse(42L, DeletionStatus.GRACE_PERIOD))
                .thenReturn(Optional.empty());
        when(repository.save(any(DeletionRequest.class))).thenAnswer(inv -> {
            DeletionRequest saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(100L);
            }
            return saved;
        });

        DeletionRequest request = service.requestDeletion(42L, tenantId);

        assertThat(request.getStatus()).isEqualTo(DeletionStatus.PENDING);
        assertThat(request.getUserId()).isEqualTo(42L);
        assertThat(request.getTenantId()).isEqualTo(tenantId);
        assertThat(request.getGraceEndsAt()).isAfter(Instant.now().plusSeconds(6 * 86_400));
        verify(auditLog).record(matchAction("deletion.requested"));
    }

    @Test
    void request_deletion_rejects_duplicate_pending_row() {
        DeletionRequest existing = DeletionRequest.builder()
                .userId(42L).tenantId(UUID.randomUUID())
                .status(DeletionStatus.PENDING).build();
        existing.setId(77L);
        when(repository.findFirstByUserIdAndStatusAndDeletedFalse(42L, DeletionStatus.PENDING))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.requestDeletion(42L, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already pending");
    }

    @Test
    void cancel_deletion_transitions_and_stores_reason() {
        DeletionRequest pending = DeletionRequest.builder()
                .userId(42L).tenantId(UUID.randomUUID())
                .status(DeletionStatus.PENDING).build();
        pending.setId(100L);
        when(repository.findById(100L)).thenReturn(Optional.of(pending));
        when(repository.save(any(DeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DeletionRequest result = service.cancelDeletion(100L, "changed my mind");

        assertThat(result.getStatus()).isEqualTo(DeletionStatus.CANCELLED);
        assertThat(result.getCancellationReason()).isEqualTo("changed my mind");
        assertThat(result.getCancelledAt()).isNotNull();
        verify(auditLog).record(matchAction("deletion.cancelled"));
    }

    @Test
    void cancel_rejects_terminal_state() {
        DeletionRequest completed = DeletionRequest.builder()
                .userId(42L).tenantId(UUID.randomUUID())
                .status(DeletionStatus.COMPLETED).build();
        completed.setId(100L);
        when(repository.findById(100L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.cancelDeletion(100L, "too late"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void start_processing_transitions_pending_to_processing() {
        DeletionRequest pending = DeletionRequest.builder()
                .userId(42L).tenantId(UUID.randomUUID())
                .status(DeletionStatus.PENDING).build();
        pending.setId(100L);
        when(repository.findById(100L)).thenReturn(Optional.of(pending));
        when(repository.save(any(DeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DeletionRequest result = service.startProcessing(100L);

        assertThat(result.getStatus()).isEqualTo(DeletionStatus.PROCESSING);
        assertThat(result.getProcessingStartedAt()).isNotNull();
        verify(auditLog).record(matchAction("deletion.processing_started"));
    }

    @Test
    void mark_completed_transitions_processing_to_completed() {
        DeletionRequest processing = DeletionRequest.builder()
                .userId(42L).tenantId(UUID.randomUUID())
                .status(DeletionStatus.PROCESSING).build();
        processing.setId(100L);
        when(repository.findById(100L)).thenReturn(Optional.of(processing));
        when(repository.save(any(DeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DeletionRequest result = service.markCompleted(100L);

        assertThat(result.getStatus()).isEqualTo(DeletionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(auditLog).record(matchAction("deletion.completed"));
    }

    @Test
    void expire_past_grace_promotes_all_due_pending_rows() {
        DeletionRequest due1 = DeletionRequest.builder()
                .userId(1L).tenantId(UUID.randomUUID())
                .status(DeletionStatus.PENDING).build();
        due1.setId(201L);
        DeletionRequest due2 = DeletionRequest.builder()
                .userId(2L).tenantId(UUID.randomUUID())
                .status(DeletionStatus.PENDING).build();
        due2.setId(202L);

        when(repository.findByStatusAndGraceEndsAtBeforeAndDeletedFalse(
                any(DeletionStatus.class), any(Instant.class)))
                .thenReturn(List.of(due1, due2));
        when(repository.save(any(DeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.expirePastGrace();

        assertThat(count).isEqualTo(2);
        assertThat(due1.getStatus()).isEqualTo(DeletionStatus.PROCESSING);
        assertThat(due2.getStatus()).isEqualTo(DeletionStatus.PROCESSING);
        verify(auditLog, org.mockito.Mockito.times(2))
                .record(matchAction("deletion.processing_started"));
    }

    @Test
    void expire_past_grace_returns_zero_when_no_due_rows() {
        when(repository.findByStatusAndGraceEndsAtBeforeAndDeletedFalse(
                any(DeletionStatus.class), any(Instant.class)))
                .thenReturn(List.of());

        assertThat(service.expirePastGrace()).isZero();
    }

    // --- helpers -----------------------------------------------------------

    private static AuditLogWriter.AuditLogEvent matchAction(String actionType) {
        return org.mockito.ArgumentMatchers.argThat(ev ->
                ev != null && actionType.equals(ev.getActionType()));
    }
}
