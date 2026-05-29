package com.kitehub.subscription.beta.service;

import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-702 Wave 104 Bucket B1 regression coverage for the
 * {@link BetaAccessService#approveRequest} → {@link EmailServiceClient#sendBetaInviteEmail}
 * wiring.
 *
 * <p>Wave 103 Bucket D live verify discovered that approve returned HTTP 200
 * + flipped status PENDING→APPROVED, but the downstream email service
 * received ZERO send requests — the custom {@code beta.invite.sent} outbox
 * event had no consumer in {@code kitehub-email}. This test asserts the new
 * direct dispatch path fires whenever approve succeeds, preventing
 * regression to the silent-skip behaviour.</p>
 */
@DisplayName("BetaAccessService → invite email dispatch (GAP-702)")
class BetaAccessServiceApprovalEmailTest {

    private BetaAccessRequestRepository repository;
    private SubscriptionEventEmitter eventEmitter;
    private EmailServiceClient emailServiceClient;
    private BetaAccessService service;

    @BeforeEach
    void setUp() {
        repository = mock(BetaAccessRequestRepository.class);
        SubscriptionOutboxRepository outboxRepo = mock(SubscriptionOutboxRepository.class);
        eventEmitter = new SubscriptionEventEmitter(outboxRepo);
        emailServiceClient = mock(EmailServiceClient.class);
        service = new BetaAccessService(repository, eventEmitter, new SimpleMeterRegistry(),
                emailServiceClient);
        // Inject deterministic signup base URL (production default points to kitehub.me).
        ReflectionTestUtils.setField(service, "betaSignupBaseUrl", "https://kitehub.me");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("shouldSendInviteEmailOnApprove — approve PENDING fires sendBetaInviteEmail with claim code + signup URL")
    void shouldSendInviteEmailOnApprove() {
        BetaAccessRequest pending = BetaAccessRequest.builder()
                .id(42L)
                .email("hong.tran@skyedu.vn")
                .name("Trần Thị Hồng")
                .orgName("Trung tâm Anh ngữ Sky Education")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        when(repository.findById(42L)).thenReturn(Optional.of(pending));

        BetaApproveCommand cmd = new BetaApproveCommand(UUID.randomUUID().toString());
        BetaAccessRequest approved = service.approveRequest(42L, cmd);

        // Status flip + token + claim code issued
        assertThat(approved.getStatus()).isEqualTo(BetaAccessRequestStatus.APPROVED);
        assertThat(approved.getClaimCode()).isNotBlank();
        assertThat(approved.getInviteToken()).isNotNull();
        assertThat(approved.getInviteTokenExpiry()).isAfter(OffsetDateTime.now());

        // Email dispatch fires (GAP-702 core assertion)
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> orgCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> expiresCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailServiceClient).sendBetaInviteEmail(
                toCaptor.capture(),
                nameCaptor.capture(),
                orgCaptor.capture(),
                codeCaptor.capture(),
                urlCaptor.capture(),
                expiresCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("hong.tran@skyedu.vn");
        assertThat(nameCaptor.getValue()).isEqualTo("Trần Thị Hồng");
        assertThat(orgCaptor.getValue()).isEqualTo("Trung tâm Anh ngữ Sky Education");
        assertThat(codeCaptor.getValue()).isEqualTo(approved.getClaimCode());
        assertThat(urlCaptor.getValue())
                .startsWith("https://kitehub.me/beta-signup/code?code=")
                .endsWith(approved.getClaimCode());
        assertThat(expiresCaptor.getValue()).matches("\\d{2}/\\d{2}/\\d{4} lúc \\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("approve still succeeds when EmailServiceClient is absent (unit-test context)")
    void approveSucceedsWhenEmailClientNull() {
        BetaAccessService serviceNoEmail = new BetaAccessService(repository, eventEmitter,
                new SimpleMeterRegistry(), null);
        ReflectionTestUtils.setField(serviceNoEmail, "betaSignupBaseUrl", "https://kitehub.me");

        BetaAccessRequest pending = BetaAccessRequest.builder()
                .id(43L)
                .email("test@kitehub.me")
                .name("Test")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        when(repository.findById(43L)).thenReturn(Optional.of(pending));

        // Must NOT throw — approve continues even when email client absent.
        BetaAccessRequest approved = serviceNoEmail.approveRequest(43L, new BetaApproveCommand(UUID.randomUUID().toString()));
        assertThat(approved.getStatus()).isEqualTo(BetaAccessRequestStatus.APPROVED);
    }
}
