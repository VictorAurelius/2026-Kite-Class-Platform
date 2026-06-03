package com.kiteclass.core.module.instance.approval;

import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RebrandApprovalServiceTest {

    @Mock
    private RebrandApprovalRepository repository;

    @Mock
    private FrontendInstanceRepository instanceRepository;

    @Mock
    private OutboxEventWriter outbox;

    @InjectMocks
    private RebrandApprovalService service;

    private FrontendInstance deployedInstance(long id, long version) {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.DEPLOYED)
                .retryCount(0).brandingVersion(1).build();
        i.setId(id);
        i.setVersion(version);
        return i;
    }

    @Test
    void request_creates_pending_approval_on_matching_version() {
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(deployedInstance(1L, 5L)));
        when(repository.findFirstByTargetInstanceIdAndStatusAndDeletedFalse(1L, ApprovalStatus.PENDING))
                .thenReturn(Optional.empty());
        when(repository.save(any(RebrandApproval.class))).thenAnswer(inv -> {
            RebrandApproval saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        RebrandApproval approval = service.request(1L, 99L, 5L, "theme refresh");

        assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(approval.getInitiatorUserId()).isEqualTo(99L);
        verify(outbox).enqueue(eq("rebrand.requested"), anyString(), anyString(), anyString());
    }

    @Test
    void request_rejects_stale_version() {
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(deployedInstance(1L, 7L)));

        assertThatThrownBy(() -> service.request(1L, 99L, 5L, "stale click"))
                .isInstanceOf(ConcurrentRebrandException.class)
                .hasMessageContaining("version changed");
    }

    @Test
    void request_rejects_if_pending_approval_already_exists() {
        when(instanceRepository.findById(1L)).thenReturn(Optional.of(deployedInstance(1L, 5L)));
        RebrandApproval pending = RebrandApproval.builder().build();
        pending.setId(77L);
        when(repository.findFirstByTargetInstanceIdAndStatusAndDeletedFalse(1L, ApprovalStatus.PENDING))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.request(1L, 99L, 5L, "duplicate"))
                .isInstanceOf(ConcurrentRebrandException.class)
                .hasMessageContaining("already pending");
    }

    @Test
    void approve_transitions_and_emits_event() {
        RebrandApproval pending = RebrandApproval.builder()
                .targetInstanceId(1L).initiatorUserId(99L)
                .status(ApprovalStatus.PENDING).build();
        pending.setId(100L);
        when(repository.findById(100L)).thenReturn(Optional.of(pending));
        when(repository.save(any(RebrandApproval.class))).thenAnswer(inv -> inv.getArgument(0));

        RebrandApproval result = service.approve(100L, 42L);

        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(result.getApproverUserId()).isEqualTo(42L);
        verify(outbox).enqueue(eq("rebrand.approved"), anyString(), anyString(), anyString());
    }

    @Test
    void approve_rejects_when_approver_same_as_initiator() {
        RebrandApproval pending = RebrandApproval.builder()
                .targetInstanceId(1L).initiatorUserId(99L)
                .status(ApprovalStatus.PENDING).build();
        pending.setId(100L);
        when(repository.findById(100L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.approve(100L, 99L))
                .isInstanceOf(ConcurrentRebrandException.class)
                .hasMessageContaining("different from initiator");
    }

    @Test
    void reject_records_reason() {
        RebrandApproval pending = RebrandApproval.builder()
                .targetInstanceId(1L).initiatorUserId(99L)
                .status(ApprovalStatus.PENDING).build();
        pending.setId(100L);
        when(repository.findById(100L)).thenReturn(Optional.of(pending));
        when(repository.save(any(RebrandApproval.class))).thenAnswer(inv -> inv.getArgument(0));

        RebrandApproval result = service.reject(100L, 42L, "off-brand");

        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("off-brand");
    }

    @Test
    void expire_due_approvals_transitions_pending_past_deadline() {
        RebrandApproval expired = RebrandApproval.builder()
                .targetInstanceId(1L).initiatorUserId(99L)
                .status(ApprovalStatus.PENDING).build();
        expired.setId(200L);
        when(repository.findByStatusAndExpiresAtBeforeAndDeletedFalse(
                eq(ApprovalStatus.PENDING), any()))
                .thenReturn(List.of(expired));
        when(repository.save(any(RebrandApproval.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.expireDueApprovals();

        assertThat(count).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(ApprovalStatus.EXPIRED);
        verify(outbox).enqueue(eq("rebrand.expired"), anyString(), anyString(), anyString());
    }
}
