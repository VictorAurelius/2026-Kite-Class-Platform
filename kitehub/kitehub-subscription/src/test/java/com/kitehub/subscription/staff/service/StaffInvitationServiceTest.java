package com.kitehub.subscription.staff.service;

import com.kitehub.subscription.staff.entity.StaffInvitation;
import com.kitehub.subscription.staff.entity.StaffInvitationStatus;
import com.kitehub.subscription.staff.repository.StaffInvitationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StaffInvitationService} skeleton (Wave 79 GAP-561).
 *
 * <p>Covers token hashing, idempotency guard, staff-limit cap, lifecycle
 * transitions. Email dispatch + user account creation deferred to GAP-561b
 * Wave 80 — not tested here.</p>
 *
 * @since Wave 79
 */
@ExtendWith(MockitoExtension.class)
class StaffInvitationServiceTest {

    @Mock
    private StaffInvitationRepository repository;

    @InjectMocks
    private StaffInvitationService service;

    private final UUID tenantId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID ownerId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void hashTokenIsDeterministicAndSha256() {
        String hashA = StaffInvitationService.hashToken("token-abc");
        String hashB = StaffInvitationService.hashToken("token-abc");
        String hashC = StaffInvitationService.hashToken("token-def");

        assertThat(hashA).isEqualTo(hashB);
        assertThat(hashA).isNotEqualTo(hashC);
        // SHA-256 hex digest length
        assertThat(hashA).hasSize(64);
        assertThat(hashA).matches("^[0-9a-f]{64}$");
    }

    @Test
    void createPersistsInvitationWithHashedTokenAndDefaultTtl() {
        when(repository.findPendingByTenantAndEmail(any(), any())).thenReturn(Optional.empty());
        when(repository.countByTenantIdAndStatus(any(), any())).thenReturn(0L);
        when(repository.save(any(StaffInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffInvitationService.InvitationIssued result =
                service.create(tenantId, ownerId, "staff.new@example.edu.vn", "Nguyễn Văn Mẫu");

        assertThat(result.rawToken()).isNotBlank().hasSizeGreaterThan(30);
        ArgumentCaptor<StaffInvitation> captor = ArgumentCaptor.forClass(StaffInvitation.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        StaffInvitation saved = captor.getValue();

        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getInvitedBy()).isEqualTo(ownerId);
        assertThat(saved.getEmail()).isEqualTo("staff.new@example.edu.vn");
        assertThat(saved.getFullName()).isEqualTo("Nguyễn Văn Mẫu");
        assertThat(saved.getStatus()).isEqualTo(StaffInvitationStatus.PENDING);
        // BR-ROLE-INVITE-TTL = 7 days
        assertThat(saved.getExpiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(saved.getExpiresAt()).isBefore(OffsetDateTime.now().plusDays(8));
        // Raw token must NOT equal stored hash (security guarantee).
        assertThat(saved.getTokenHash()).isNotEqualTo(result.rawToken());
        assertThat(saved.getTokenHash())
                .isEqualTo(StaffInvitationService.hashToken(result.rawToken()));
    }

    @Test
    void createNormalizesEmailToLowercase() {
        when(repository.findPendingByTenantAndEmail(any(), any())).thenReturn(Optional.empty());
        when(repository.countByTenantIdAndStatus(any(), any())).thenReturn(0L);
        when(repository.save(any(StaffInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(tenantId, ownerId, " Staff.New@Example.EDU.VN ", "Test User");

        ArgumentCaptor<StaffInvitation> captor = ArgumentCaptor.forClass(StaffInvitation.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("staff.new@example.edu.vn");
    }

    @Test
    void createRejectsWhenSameTenantEmailHasPendingInvite() {
        StaffInvitation existing = StaffInvitation.builder()
                .tenantId(tenantId)
                .email("dup@example.com")
                .status(StaffInvitationStatus.PENDING)
                .build();
        when(repository.findPendingByTenantAndEmail(tenantId, "dup@example.com"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                service.create(tenantId, ownerId, "dup@example.com", "Dup User"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INVITATION_ALREADY_PENDING");
    }

    @Test
    void createRejectsWhenTenantAtStaffCap() {
        when(repository.findPendingByTenantAndEmail(any(), any())).thenReturn(Optional.empty());
        when(repository.countByTenantIdAndStatus(tenantId, StaffInvitationStatus.PENDING))
                .thenReturn(30L);
        when(repository.countByTenantIdAndStatus(tenantId, StaffInvitationStatus.ACCEPTED))
                .thenReturn(20L);
        // Combined 50 == STAFF_MAX_PER_TENANT.

        assertThatThrownBy(() ->
                service.create(tenantId, ownerId, "n51@example.com", "N51"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("STAFF_LIMIT_REACHED");
    }

    @Test
    void revokeIsIdempotentForAlreadyRevoked() {
        UUID invId = UUID.randomUUID();
        StaffInvitation already = StaffInvitation.builder()
                .id(invId)
                .status(StaffInvitationStatus.REVOKED)
                .build();
        when(repository.findById(invId)).thenReturn(Optional.of(already));

        StaffInvitation result = service.revoke(invId, ownerId);

        assertThat(result.getStatus()).isEqualTo(StaffInvitationStatus.REVOKED);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .save(any(StaffInvitation.class));
    }

    @Test
    void revokeRejectsAcceptedInvitation() {
        UUID invId = UUID.randomUUID();
        StaffInvitation accepted = StaffInvitation.builder()
                .id(invId)
                .status(StaffInvitationStatus.ACCEPTED)
                .build();
        when(repository.findById(invId)).thenReturn(Optional.of(accepted));

        assertThatThrownBy(() -> service.revoke(invId, ownerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INVITATION_NOT_REVOCABLE");
    }

    @Test
    void findActiveByTokenReturnsEmptyForExpiredInvite() {
        String raw = "expired-token";
        StaffInvitation expired = StaffInvitation.builder()
                .status(StaffInvitationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .build();
        when(repository.findByTokenHash(StaffInvitationService.hashToken(raw)))
                .thenReturn(Optional.of(expired));

        Optional<StaffInvitation> result = service.findActiveByToken(raw);
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByTokenReturnsPresentForPendingValidInvite() {
        String raw = "valid-token";
        StaffInvitation valid = StaffInvitation.builder()
                .status(StaffInvitationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusDays(3))
                .build();
        when(repository.findByTokenHash(StaffInvitationService.hashToken(raw)))
                .thenReturn(Optional.of(valid));

        Optional<StaffInvitation> result = service.findActiveByToken(raw);
        assertThat(result).isPresent();
    }
}
