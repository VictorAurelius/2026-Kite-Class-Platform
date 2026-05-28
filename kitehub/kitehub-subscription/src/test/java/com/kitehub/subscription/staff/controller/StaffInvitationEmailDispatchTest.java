package com.kitehub.subscription.staff.controller;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.staff.dto.CreateStaffInvitationRequest;
import com.kitehub.subscription.staff.entity.StaffInvitation;
import com.kitehub.subscription.staff.entity.StaffInvitationStatus;
import com.kitehub.subscription.staff.repository.StaffInvitationAuditRepository;
import com.kitehub.subscription.staff.service.StaffInvitationService;
import com.kitehub.subscription.staff.service.StaffInvitationService.InvitationIssued;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-787 regression coverage for the staff-invite email dispatch wiring.
 *
 * <p>Wave meta-6 Bucket A walk (Bug #14) discovered that creating a staff invitation
 * returned HTTP 201 + persisted the DB row, but NO email was ever sent — in queue mode
 * the {@code email.send} queue had no kitehub-email consumer, so the dispatch was a
 * silent no-op end-to-end. This test mirrors {@code BetaAccessServiceApprovalEmailTest}
 * (GAP-702): it asserts the controller fires
 * {@link EmailServiceClient#sendInviteStaffEmail} on {@code create()} with the raw token
 * carried into the accept-invite URL, preventing regression to the silent-skip behaviour.</p>
 *
 * <p>The consumer-side delivery (the actual missing piece) is restored by
 * {@code com.kitehub.email.listener.EmailEventListener} in kitehub-email; end-to-end
 * delivery is verified by the coordinator's MailHog runtime walk.</p>
 */
@DisplayName("StaffInvitationController → invite email dispatch (GAP-787)")
class StaffInvitationEmailDispatchTest {

    private StaffInvitationService service;
    private StaffInvitationAuditRepository auditRepository;
    private UserRepository userRepository;
    private EmailServiceClient emailServiceClient;
    private StaffInvitationController controller;

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        service = mock(StaffInvitationService.class);
        auditRepository = mock(StaffInvitationAuditRepository.class);
        userRepository = mock(UserRepository.class);
        emailServiceClient = mock(EmailServiceClient.class);
        controller = new StaffInvitationController(service, auditRepository, userRepository,
                emailServiceClient);
        // @Value-injected base URL — set deterministically (production default points to kitehub.me).
        ReflectionTestUtils.setField(controller, "inviteBaseUrl", "https://sky-edu-test.kitehub.me");

        // No stale PENDING invite → idempotency revoke path is a no-op.
        when(service.listByTenant(TENANT_ID)).thenReturn(Collections.emptyList());
        // Owner name resolution.
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(
                User.builder().id(OWNER_ID).name("Nguyễn Văn An").build()));
    }

    @Test
    @DisplayName("shouldSendInviteEmailOnCreate — create fires sendInviteStaffEmail with raw token in accept URL")
    void shouldSendInviteEmailOnCreate() {
        OffsetDateTime expiry = OffsetDateTime.now().plusDays(7);
        StaffInvitation inv = StaffInvitation.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .invitedBy(OWNER_ID)
                .email("staff.test1@test.vn")
                .fullName("Trần Thị Hồng")
                .tokenHash("hash-not-the-raw-token")
                .status(StaffInvitationStatus.PENDING)
                .expiresAt(expiry)
                .build();
        String rawToken = "RAW-TOKEN-abc123";
        when(service.create(TENANT_ID, OWNER_ID, "staff.test1@test.vn", "Trần Thị Hồng"))
                .thenReturn(new InvitationIssued(inv, rawToken));

        CreateStaffInvitationRequest request = CreateStaffInvitationRequest.builder()
                .email("staff.test1@test.vn")
                .fullName("Trần Thị Hồng")
                .build();

        ResponseEntity<?> response = controller.create(
                TENANT_ID.toString(), OWNER_ID.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Email dispatch fires (GAP-787 core assertion).
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> recipientNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ownerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tenantNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> roleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> expiresCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailServiceClient).sendInviteStaffEmail(
                toCaptor.capture(),
                recipientNameCaptor.capture(),
                ownerNameCaptor.capture(),
                tenantNameCaptor.capture(),
                roleCaptor.capture(),
                urlCaptor.capture(),
                expiresCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("staff.test1@test.vn");
        assertThat(recipientNameCaptor.getValue()).isEqualTo("Trần Thị Hồng");
        assertThat(ownerNameCaptor.getValue()).isEqualTo("Nguyễn Văn An");
        assertThat(roleCaptor.getValue()).isEqualTo("STAFF");
        // Raw token (NOT the hash) is carried into the accept-invite URL.
        assertThat(urlCaptor.getValue())
                .isEqualTo("https://sky-edu-test.kitehub.me/staff/accept-invite?token=" + rawToken)
                .doesNotContain("hash-not-the-raw-token");
        // VN-formatted expiry per vn-localization-audit-checklist.md §1 (e.g. "Thứ Hai, 22/05/2026").
        assertThat(expiresCaptor.getValue()).matches(".+, \\d{2}/\\d{2}/\\d{4}");
    }

    @Test
    @DisplayName("create still returns 201 even when email dispatch throws (best-effort isolation)")
    void createSucceedsWhenEmailDispatchThrows() {
        OffsetDateTime expiry = OffsetDateTime.now().plusDays(7);
        StaffInvitation inv = StaffInvitation.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .invitedBy(OWNER_ID)
                .email("staff.test2@test.vn")
                .fullName("Phạm Thị Mai")
                .tokenHash("hash")
                .status(StaffInvitationStatus.PENDING)
                .expiresAt(expiry)
                .build();
        when(service.create(any(), any(), anyString(), anyString()))
                .thenReturn(new InvitationIssued(inv, "RAW-TOKEN-xyz"));
        // sendInviteStaffEmail swallows its own exceptions internally (best-effort);
        // here we assert create() completes 201 regardless of dispatch outcome.

        CreateStaffInvitationRequest request = CreateStaffInvitationRequest.builder()
                .email("staff.test2@test.vn")
                .fullName("Phạm Thị Mai")
                .build();

        ResponseEntity<?> response = controller.create(
                TENANT_ID.toString(), OWNER_ID.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(emailServiceClient).sendInviteStaffEmail(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString());
    }
}
