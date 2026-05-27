package com.kiteclass.core.module.staff.service;

import com.kiteclass.core.common.constant.StaffInvitationStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteRequest;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteResult;
import com.kiteclass.core.module.staff.dto.StaffInvitationResponse;
import com.kiteclass.core.module.staff.entity.StaffInvitation;
import com.kiteclass.core.module.staff.repository.StaffInvitationRepository;
import com.kiteclass.core.module.staff.service.impl.StaffInvitationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StaffInvitationServiceImpl}.
 *
 * <p>Scope mirrors {@code ParentInvitationServiceTest} adapted for staff:
 * tenant scoping, token generation, status transitions (PENDING / ACCEPTED /
 * EXPIRED / REVOKED), cross-tenant isolation, idempotency on re-revoke + double
 * accept. The DB-level uniqueness on the token + multi-tenant Hibernate filter
 * remain covered by repository integration tests (sister module pattern).
 *
 * <p>Wave meta-6 follow-up — GAP-782 Bucket A item 2 test coverage for the
 * module shipped PR #1904 (Wave meta-6 Bucket A — GAP-772).
 *
 * @since 2026-05-28 (Wave meta-6 follow-up — GAP-782)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StaffInvitationService")
class StaffInvitationServiceImplTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TENANT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Long INVITER_ID = 7L;
    private static final String STAFF_EMAIL = "hong.tran@skyedu.vn";
    private static final String ROLE_STAFF = "STAFF";
    private static final String FULL_NAME = "Trần Thị Hồng";
    private static final String STRONG_PASSWORD = "Str0ng!Pass";

    @Mock private StaffInvitationRepository invitationRepository;

    private StaffInvitationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StaffInvitationServiceImpl(invitationRepository);
        // @Value field injected via reflection (no @ConfigurationProperties wrapper
        // exists for this MVP — service reads the TTL directly).
        ReflectionTestUtils.setField(service, "ttlHours", 168L);
    }

    // ——— invite ————————————————————————————————————————————————

    @Nested
    @DisplayName("invite")
    class Invite {

        @Test
        @DisplayName("creates PENDING invitation with 36-char UUID token + Vietnamese-friendly email")
        void happyPath() {
            when(invitationRepository.save(any(StaffInvitation.class)))
                    .thenAnswer(inv -> {
                        StaffInvitation saved = inv.getArgument(0);
                        saved.setId(100L);
                        return saved;
                    });

            StaffInvitationResponse response = service.invite(
                    TENANT, STAFF_EMAIL, ROLE_STAFF, INVITER_ID);

            assertThat(response.status()).isEqualTo(StaffInvitationStatus.PENDING);
            assertThat(response.email()).isEqualTo(STAFF_EMAIL);
            assertThat(response.role()).isEqualTo(ROLE_STAFF);
            // Owner-issuing response MUST include token (FE renders redemption link).
            assertThat(response.token()).hasSize(36);
            assertThat(response.invitedByUserId()).isEqualTo(INVITER_ID);

            ArgumentCaptor<StaffInvitation> captor = ArgumentCaptor.forClass(StaffInvitation.class);
            verify(invitationRepository).save(captor.capture());
            StaffInvitation saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(StaffInvitationStatus.PENDING);
            assertThat(saved.getInvitedByUserId()).isEqualTo(INVITER_ID);
            assertThat(saved.getInstanceId()).isEqualTo(TENANT);
            // TTL window: should be ~168h from now (1h tolerance for test wall-clock).
            assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(167, ChronoUnit.HOURS));
            assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(169, ChronoUnit.HOURS));
        }

        @Test
        @DisplayName("normalizes email to lowercase + trims whitespace")
        void normalizesEmail() {
            when(invitationRepository.save(any(StaffInvitation.class)))
                    .thenAnswer(inv -> {
                        StaffInvitation saved = inv.getArgument(0);
                        saved.setId(101L);
                        return saved;
                    });

            StaffInvitationResponse response = service.invite(
                    TENANT, "  HONG.TRAN@SKYEDU.VN  ", ROLE_STAFF, INVITER_ID);

            assertThat(response.email()).isEqualTo("hong.tran@skyedu.vn");

            ArgumentCaptor<StaffInvitation> captor = ArgumentCaptor.forClass(StaffInvitation.class);
            verify(invitationRepository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("hong.tran@skyedu.vn");
        }

        @Test
        @DisplayName("each invite mints a fresh UUID token (no collision on consecutive issues)")
        void freshTokenPerInvite() {
            when(invitationRepository.save(any(StaffInvitation.class)))
                    .thenAnswer(inv -> {
                        StaffInvitation saved = inv.getArgument(0);
                        saved.setId(102L);
                        return saved;
                    });

            String token1 = service.invite(TENANT, STAFF_EMAIL, ROLE_STAFF, INVITER_ID).token();
            String token2 = service.invite(TENANT, "nguyen.an@quangminh.edu.vn", "TEACHER", INVITER_ID).token();

            assertThat(token1).isNotEqualTo(token2);
            assertThat(token1).hasSize(36);
            assertThat(token2).hasSize(36);
        }
    }

    // ——— listForTenant ————————————————————————————————————————————

    @Nested
    @DisplayName("listForTenant")
    class ListForTenant {

        @Test
        @DisplayName("returns PENDING invitations for caller tenant; omits token on list responses")
        void listsForTenant() {
            StaffInvitation row = pendingInvitation();
            when(invitationRepository
                    .findByStatusAndDeletedFalseOrderByCreatedAtDesc(StaffInvitationStatus.PENDING))
                    .thenReturn(List.of(row));

            List<StaffInvitationResponse> rows = service.listForTenant(TENANT);

            assertThat(rows).hasSize(1);
            // Token MUST be omitted on list payload to reduce leak surface.
            assertThat(rows.get(0).token()).isNull();
            assertThat(rows.get(0).email()).isEqualTo(STAFF_EMAIL);
        }

        @Test
        @DisplayName("filters out rows that belong to a different tenant")
        void filtersCrossTenant() {
            StaffInvitation mine = pendingInvitation();
            StaffInvitation theirs = pendingInvitation();
            theirs.setInstanceId(OTHER_TENANT);

            when(invitationRepository
                    .findByStatusAndDeletedFalseOrderByCreatedAtDesc(StaffInvitationStatus.PENDING))
                    .thenReturn(List.of(mine, theirs));

            List<StaffInvitationResponse> rows = service.listForTenant(TENANT);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).email()).isEqualTo(STAFF_EMAIL);
        }

        @Test
        @DisplayName("empty list when tenant has no PENDING invitations")
        void emptyList() {
            when(invitationRepository
                    .findByStatusAndDeletedFalseOrderByCreatedAtDesc(StaffInvitationStatus.PENDING))
                    .thenReturn(List.of());

            assertThat(service.listForTenant(TENANT)).isEmpty();
        }
    }

    // ——— revoke ————————————————————————————————————————————————

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("transitions PENDING → REVOKED for own tenant")
        void happyPath() {
            StaffInvitation row = pendingInvitation();
            when(invitationRepository.findById(10L)).thenReturn(Optional.of(row));

            service.revoke(TENANT, 10L);

            assertThat(row.getStatus()).isEqualTo(StaffInvitationStatus.REVOKED);
            verify(invitationRepository).save(row);
        }

        @Test
        @DisplayName("throws NOT_FOUND when row is missing")
        void notFound() {
            when(invitationRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revoke(TENANT, 404L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_NOT_FOUND");
            verify(invitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("blocks cross-tenant revoke — surfaces NOT_FOUND for caller from other tenant")
        void crossTenantBlocked() {
            StaffInvitation theirs = pendingInvitation(); // tenant = TENANT
            when(invitationRepository.findById(10L)).thenReturn(Optional.of(theirs));

            assertThatThrownBy(() -> service.revoke(OTHER_TENANT, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_NOT_FOUND");
            verify(invitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns CONFLICT when invitation is already ACCEPTED (idempotent surface)")
        void alreadyAccepted() {
            StaffInvitation accepted = pendingInvitation();
            accepted.setStatus(StaffInvitationStatus.ACCEPTED);
            when(invitationRepository.findById(10L)).thenReturn(Optional.of(accepted));

            assertThatThrownBy(() -> service.revoke(TENANT, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_NOT_PENDING");
            verify(invitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns CONFLICT when row is soft-deleted")
        void softDeletedRejected() {
            StaffInvitation deleted = pendingInvitation();
            deleted.setDeleted(true);
            when(invitationRepository.findById(10L)).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> service.revoke(TENANT, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_NOT_FOUND");
        }
    }

    // ——— accept ————————————————————————————————————————————————

    @Nested
    @DisplayName("accept")
    class Accept {

        @Test
        @DisplayName("transitions PENDING → ACCEPTED and returns credential payload for gateway")
        void happyPath() {
            StaffInvitation invitation = pendingInvitation();
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(invitation));

            AcceptStaffInviteRequest req = new AcceptStaffInviteRequest(FULL_NAME, STRONG_PASSWORD);

            AcceptStaffInviteResult result = service.accept(TENANT, "tok", req);

            assertThat(invitation.getStatus()).isEqualTo(StaffInvitationStatus.ACCEPTED);
            assertThat(invitation.getAcceptedAt()).isNotNull();
            assertThat(result.invitationId()).isEqualTo(10L);
            assertThat(result.tenantId()).isEqualTo(TENANT);
            assertThat(result.email()).isEqualTo(STAFF_EMAIL);
            assertThat(result.role()).isEqualTo(ROLE_STAFF);
            // VN diacritic round-trip per vn-localization-audit-checklist.md §5.
            assertThat(result.fullName()).isEqualTo("Trần Thị Hồng");
            verify(invitationRepository).save(invitation);
        }

        @Test
        @DisplayName("rejects expired token + flips PENDING → EXPIRED on the row")
        void expiredToken() {
            StaffInvitation expired = pendingInvitation();
            expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(expired));

            AcceptStaffInviteRequest req = new AcceptStaffInviteRequest(FULL_NAME, STRONG_PASSWORD);

            assertThatThrownBy(() -> service.accept(TENANT, "tok", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_EXPIRED");

            assertThat(expired.getStatus()).isEqualTo(StaffInvitationStatus.EXPIRED);
            verify(invitationRepository).save(expired);
        }

        @Test
        @DisplayName("rejects double-accept on already-ACCEPTED token")
        void alreadyAccepted() {
            StaffInvitation used = pendingInvitation();
            used.setStatus(StaffInvitationStatus.ACCEPTED);
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(used));

            AcceptStaffInviteRequest req = new AcceptStaffInviteRequest(FULL_NAME, STRONG_PASSWORD);

            assertThatThrownBy(() -> service.accept(TENANT, "tok", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_ALREADY_ACCEPTED");
        }

        @Test
        @DisplayName("rejects accept on REVOKED token")
        void revokedRejected() {
            StaffInvitation revoked = pendingInvitation();
            revoked.setStatus(StaffInvitationStatus.REVOKED);
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(revoked));

            AcceptStaffInviteRequest req = new AcceptStaffInviteRequest(FULL_NAME, STRONG_PASSWORD);

            assertThatThrownBy(() -> service.accept(TENANT, "tok", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_REVOKED");
        }

        @Test
        @DisplayName("returns 404 when token is unknown (no row leak)")
        void unknownToken() {
            when(invitationRepository.findByTokenAndDeletedFalse("nope"))
                    .thenReturn(Optional.empty());

            AcceptStaffInviteRequest req = new AcceptStaffInviteRequest(FULL_NAME, STRONG_PASSWORD);

            assertThatThrownBy(() -> service.accept(TENANT, "nope", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_NOT_FOUND");
        }

        @Test
        @DisplayName("blocks cross-tenant accept — surfaces NOT_FOUND for stolen token replayed in other tenant")
        void crossTenantBlocked() {
            StaffInvitation invitation = pendingInvitation(); // tenant = TENANT
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(invitation));

            AcceptStaffInviteRequest req = new AcceptStaffInviteRequest(FULL_NAME, STRONG_PASSWORD);

            assertThatThrownBy(() -> service.accept(OTHER_TENANT, "tok", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("STAFF_INVITATION_NOT_FOUND");

            // Status must NOT flip on cross-tenant attempt.
            assertThat(invitation.getStatus()).isEqualTo(StaffInvitationStatus.PENDING);
        }
    }

    // ——— fixtures ————————————————————————————————————————————————

    private StaffInvitation pendingInvitation() {
        StaffInvitation inv = StaffInvitation.builder()
                .email(STAFF_EMAIL)
                .role(ROLE_STAFF)
                .token("tok")
                .status(StaffInvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .invitedByUserId(INVITER_ID)
                .build();
        inv.setId(10L);
        inv.setInstanceId(TENANT);
        return inv;
    }
}
