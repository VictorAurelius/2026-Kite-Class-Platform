package com.kiteclass.core.module.moderation;

import com.kiteclass.core.common.audit.AuditLog;
import com.kiteclass.core.common.audit.AuditLogWriter;
import com.kiteclass.core.common.audit.AuditLogWriter.AuditLogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock
    private ModerationQueueRepository queueRepository;

    @Mock
    private AuditLogWriter auditLog;

    @InjectMocks
    private ContentModerationService service;

    @BeforeEach
    void wireConfig() {
        // @Value fields are not auto-set by Mockito — inject defaults matching application.yml.
        ReflectionTestUtils.setField(service, "stage1Enabled", true);
        ReflectionTestUtils.setField(service, "nsfwThreshold", 0.7);
        ReflectionTestUtils.setField(service, "autoFallbackToTemplate", true);
    }

    @Test
    void clean_content_is_approved_without_queue_row() {
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());

        ModerationResult result = service.check("branding.banner", "b-1",
                "Friendly education center welcoming new students", null);

        assertThat(result.isApproved()).isTrue();
        assertThat(result.getScore()).isLessThan(0.55);
        assertThat(result.getFlaggedKeywords()).isEmpty();
        verify(queueRepository, never()).save(any(ModerationQueue.class));
    }

    @Test
    void flagged_keyword_is_rejected_and_persists_queue_row() {
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());
        when(queueRepository.save(any(ModerationQueue.class))).thenAnswer(inv -> {
            ModerationQueue saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        ModerationResult result = service.check("branding.banner", "b-2",
                "explicit nsfw violence content here", null);

        assertThat(result.isRejected()).isTrue();
        assertThat(result.getFlaggedKeywords()).contains("nsfw", "violence");
        assertThat(result.getScore()).isGreaterThanOrEqualTo(0.7);

        ArgumentCaptor<ModerationQueue> captor = ArgumentCaptor.forClass(ModerationQueue.class);
        verify(queueRepository).save(captor.capture());
        ModerationQueue saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(saved.getDecidedAt()).isNotNull();
        assertThat(saved.getFlaggedKeywords()).contains("nsfw").contains("violence");
    }

    @Test
    void borderline_score_escalates_to_human_review() {
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());
        when(queueRepository.save(any(ModerationQueue.class))).thenAnswer(inv -> {
            ModerationQueue saved = inv.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        // Single hit → score 0.55, below 0.7 threshold but in 0.5..0.7 review band.
        ModerationResult result = service.check("branding.logo", "l-9",
                "premium drug-free academy", null);

        assertThat(result.needsHumanReview()).isTrue();
        assertThat(result.getFlaggedKeywords()).containsExactly("drug");
        assertThat(result.getReason()).isEqualTo("borderline-score");

        ArgumentCaptor<ModerationQueue> captor = ArgumentCaptor.forClass(ModerationQueue.class);
        verify(queueRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ModerationStatus.NEEDS_HUMAN_REVIEW);
        assertThat(captor.getValue().getDecidedAt()).isNull();
    }

    @Test
    void every_outcome_writes_audit_log_entry() {
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());
        when(queueRepository.save(any(ModerationQueue.class))).thenAnswer(inv -> {
            ModerationQueue saved = inv.getArgument(0);
            saved.setId(300L);
            return saved;
        });

        service.check("branding.banner", "t-1", "all clean tone here", null);
        service.check("branding.banner", "t-2", "nsfw porn gore hate", null);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(auditLog, org.mockito.Mockito.times(2)).record(captor.capture());
        assertThat(captor.getAllValues().get(0).getActionType()).isEqualTo("moderation.approved");
        assertThat(captor.getAllValues().get(0).getAggregateType()).isEqualTo("ModerationQueue");
        assertThat(captor.getAllValues().get(1).getActionType()).isEqualTo("moderation.rejected");
    }

    @Test
    void stage1_disabled_approves_everything_and_skips_queue() {
        ReflectionTestUtils.setField(service, "stage1Enabled", false);
        when(auditLog.record(any(AuditLogEvent.class))).thenReturn(new AuditLog());

        ModerationResult result = service.check("branding.banner", "b-3",
                "violence porn nsfw hate", null);

        assertThat(result.isApproved()).isTrue();
        assertThat(result.getReason()).isEqualTo("stage1.disabled");
        verify(queueRepository, never()).save(any(ModerationQueue.class));

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(auditLog).record(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo("moderation.approved");
    }

    @Test
    void state_machine_invalid_transition_throws() {
        ModerationQueue row = ModerationQueue.builder()
                .targetType("branding.banner").targetId("x")
                .status(ModerationStatus.PENDING).score(0.0).build();
        row.transitionTo(ModerationStatus.APPROVED);

        assertThatThrownBy(() -> row.transitionTo(ModerationStatus.REJECTED))
                .isInstanceOf(IllegalStateException.class);
    }
}
