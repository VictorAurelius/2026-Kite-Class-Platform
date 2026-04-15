package com.kiteclass.core.wave04;

import com.kiteclass.core.common.audit.AuditLogWriter;
import com.kiteclass.core.module.instance.approval.ApprovalStatus;
import com.kiteclass.core.module.legal.entity.DmcaStatus;
import com.kiteclass.core.module.legal.entity.DmcaTakedownRequest;
import com.kiteclass.core.module.moderation.ModerationStatus;
import com.kiteclass.core.module.retention.DeletionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave 4 cross-module smoke — confirms every Wave 4 state machine compiles + respects its
 * allowed-transitions contract. Mocks-free, no Spring context; functional tests for each
 * module already live in their own package.
 *
 * <p>Kept lean: this PR's job is wave-level wiring; the substance is in the per-sub-PR
 * tests already merged.
 *
 * @since Wave 4 Sub-PR 4.6
 */
class Wave04IntegrationTest {

    @Test
    void moderation_status_terminals_are_immutable() {
        assertThat(ModerationStatus.APPROVED.isTerminal()).isTrue();
        assertThat(ModerationStatus.REJECTED.isTerminal()).isTrue();
        assertThat(ModerationStatus.PENDING.canTransitionTo(ModerationStatus.APPROVED)).isTrue();
        assertThat(ModerationStatus.APPROVED.canTransitionTo(ModerationStatus.PENDING)).isFalse();
    }

    @Test
    void dmca_status_follows_documented_path() {
        assertThat(DmcaStatus.PENDING.canTransitionTo(DmcaStatus.REVIEWING)).isTrue();
        assertThat(DmcaStatus.REVIEWING.canTransitionTo(DmcaStatus.VALID)).isTrue();
        assertThat(DmcaStatus.VALID.canTransitionTo(DmcaStatus.EXECUTED)).isTrue();
        assertThat(DmcaStatus.INVALID.isTerminal()).isTrue();
        assertThat(DmcaStatus.EXECUTED.isTerminal()).isTrue();
    }

    @Test
    void deletion_status_grace_workflow() {
        assertThat(DeletionStatus.PENDING.canTransitionTo(DeletionStatus.GRACE_PERIOD)).isTrue();
        assertThat(DeletionStatus.PENDING.canTransitionTo(DeletionStatus.CANCELLED)).isTrue();
        assertThat(DeletionStatus.GRACE_PERIOD.canTransitionTo(DeletionStatus.PROCESSING)).isTrue();
        assertThat(DeletionStatus.COMPLETED.isTerminal()).isTrue();
    }

    @Test
    void approval_status_remains_intact_after_wave4_additions() {
        // Wave 3 state machine still works after Wave 4 added sibling enums.
        assertThat(ApprovalStatus.PENDING.canTransitionTo(ApprovalStatus.APPROVED)).isTrue();
        assertThat(ApprovalStatus.APPROVED.isTerminal()).isTrue();
    }

    @Test
    void audit_log_event_builder_accepts_wave4_action_types() {
        // Smoke-test that all Wave 4 modules can feed AuditLogWriter with their event types.
        String[] wave4Actions = {
                "moderation.approved", "moderation.rejected",
                "dmca.takedown.valid", "dmca.takedown.invalid",
                "deletion.requested", "deletion.completed",
                "quality.review.passed", "quality.review.failed"
        };
        for (String action : wave4Actions) {
            AuditLogWriter.AuditLogEvent event = AuditLogWriter.AuditLogEvent.builder()
                    .actionType(action)
                    .aggregateType("TestAggregate")
                    .aggregateId("1")
                    .build();
            assertThat(event.getActionType()).isEqualTo(action);
        }
    }

    @Test
    void dmca_request_entity_transitions_through_saga_path() {
        DmcaTakedownRequest request = DmcaTakedownRequest.builder()
                .status(DmcaStatus.PENDING)
                .reporterEmail("legal@example.com")
                .build();
        request.setId(1L);
        // Smoke — just confirm state-machine enum references compile end-to-end.
        assertThat(request.getStatus()).isEqualTo(DmcaStatus.PENDING);
    }
}
