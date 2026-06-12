package com.kiteclass.core.module.legal;

import com.kiteclass.core.common.audit.AuditLog;
import com.kiteclass.core.common.audit.AuditLogWriter;
import com.kiteclass.core.common.audit.AuditLogWriter.AuditLogEvent;
import com.kiteclass.core.module.legal.entity.DmcaStatus;
import com.kiteclass.core.module.legal.entity.DmcaTakedownRequest;
import com.kiteclass.core.module.legal.repository.DmcaTakedownRepository;
import com.kiteclass.core.module.legal.service.DmcaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmcaServiceTest {

    @Mock
    private DmcaTakedownRepository repository;

    @Mock
    private AuditLogWriter auditLog;

    @InjectMocks
    private DmcaService service;

    private DmcaTakedownRequest freshRequest() {
        return DmcaTakedownRequest.builder()
                .reporterEmail("claimant@example.com")
                .reporterName("Claimant Co")
                .allegedInfringingUrl("https://tenant.kitehub.me/logo.svg")
                .copyrightedWorkDescription("Our registered logo")
                .build();
    }

    private DmcaTakedownRequest persisted(long id, DmcaStatus status) {
        DmcaTakedownRequest r = freshRequest();
        r.setStatus(status);
        r.setId(id);
        return r;
    }

    @Test
    void receiveTakedown_persists_pending_and_audits() {
        DmcaTakedownRequest incoming = freshRequest();
        when(repository.save(any(DmcaTakedownRequest.class))).thenAnswer(inv -> {
            DmcaTakedownRequest saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());

        DmcaTakedownRequest result = service.receiveTakedown(incoming);

        assertThat(result.getStatus()).isEqualTo(DmcaStatus.PENDING);
        assertThat(result.getId()).isEqualTo(10L);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(auditLog).record(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo("dmca.takedown.received");
        assertThat(captor.getValue().getAggregateType()).isEqualTo("DmcaTakedownRequest");
        assertThat(captor.getValue().getAggregateId()).isEqualTo("10");
    }

    @Test
    void markReviewing_transitions_pending_to_reviewing() {
        DmcaTakedownRequest existing = persisted(10L, DmcaStatus.PENDING);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(DmcaTakedownRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());

        DmcaTakedownRequest result = service.markReviewing(10L, 99L);

        assertThat(result.getStatus()).isEqualTo(DmcaStatus.REVIEWING);
        assertThat(result.getReviewerUserId()).isEqualTo(99L);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(auditLog).record(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo("dmca.takedown.reviewing");
        assertThat(captor.getValue().getActorUserId()).isEqualTo(99L);
    }

    @Test
    void happy_path_review_valid_execute_with_audit_per_step() {
        DmcaTakedownRequest existing = persisted(10L, DmcaStatus.PENDING);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(DmcaTakedownRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());

        service.markReviewing(10L, 99L);
        service.markValid(10L, 99L);
        service.execute(10L);

        assertThat(existing.getStatus()).isEqualTo(DmcaStatus.EXECUTED);
        assertThat(existing.getExecutedAt()).isNotNull();

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(auditLog, org.mockito.Mockito.times(3)).record(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AuditLogEvent::getActionType)
                .containsExactly(
                        "dmca.takedown.reviewing",
                        "dmca.takedown.valid",
                        "dmca.takedown.executed");
    }

    @Test
    void markInvalid_records_rejection_reason() {
        DmcaTakedownRequest existing = persisted(10L, DmcaStatus.REVIEWING);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(DmcaTakedownRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());

        DmcaTakedownRequest result = service.markInvalid(10L, 99L, "Missing required fields");

        assertThat(result.getStatus()).isEqualTo(DmcaStatus.INVALID);
        assertThat(result.getRejectionReason()).isEqualTo("Missing required fields");

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(auditLog).record(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo("dmca.takedown.invalid");
        assertThat(captor.getValue().getReason()).isEqualTo("Missing required fields");
    }

    @Test
    void contest_transitions_valid_to_contested_and_stores_counter_email() {
        DmcaTakedownRequest existing = persisted(10L, DmcaStatus.VALID);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(DmcaTakedownRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());

        DmcaTakedownRequest result = service.contest(10L, "tenant@example.com");

        assertThat(result.getStatus()).isEqualTo(DmcaStatus.CONTESTED);
        assertThat(result.getCounterNoticeEmail()).isEqualTo("tenant@example.com");
        assertThat(result.getContestedAt()).isNotNull();

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(auditLog).record(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo("dmca.takedown.contested");
    }

    @Test
    void execute_from_non_valid_state_is_rejected_by_state_machine() {
        DmcaTakedownRequest existing = persisted(10L, DmcaStatus.REVIEWING);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.execute(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid DMCA takedown transition");
    }

    @Test
    void load_missing_id_throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markReviewing(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
