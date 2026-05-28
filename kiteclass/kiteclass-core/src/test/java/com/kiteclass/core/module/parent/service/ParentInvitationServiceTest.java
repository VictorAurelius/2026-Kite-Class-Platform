package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.common.constant.ParentInvitationStatus;
import com.kiteclass.core.common.constant.ParentRelationship;
import com.kiteclass.core.common.constant.ParentStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.parent.config.ParentPortalProperties;
import com.kiteclass.core.module.parent.dto.ParentInvitationResponse;
import com.kiteclass.core.module.parent.dto.RedeemInvitationRequest;
import com.kiteclass.core.module.parent.dto.RedeemInvitationResult;
import com.kiteclass.core.module.parent.entity.Parent;
import com.kiteclass.core.module.parent.entity.ParentInvitation;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.parent.repository.ParentInvitationRepository;
import com.kiteclass.core.module.parent.repository.ParentRepository;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.impl.ParentInvitationServiceImpl;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ParentInvitationServiceImpl}.
 *
 * <p>Scope is limited to business-rule enforcement: tenant scoping, token
 * generation, duplicate detection, expiry handling, and the email-publish
 * best-effort contract. Database behaviours (unique constraints, tenant
 * filter) are covered by {@code Wave02MigrationsTest} + integration tests.
 *
 * @since 2.14.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentInvitationService")
class ParentInvitationServiceTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Long STUDENT_ID = 42L;
    private static final UUID INVITER_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final String PARENT_EMAIL = "phuhuynh@example.com";
    private static final String STRONG_PASSWORD = "Str0ng!Pass";

    @Mock private ParentRepository parentRepository;
    @Mock private ParentInvitationRepository invitationRepository;
    @Mock private ParentStudentLinkRepository linkRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private OutboxEventWriter outbox;

    // Match Spring Boot default — registers JavaTimeModule so Instant serializes
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ParentInvitationServiceImpl service;

    private Student student;

    @BeforeEach
    void setUp() {
        ParentPortalProperties props = new ParentPortalProperties(
                /* enabled */ true,
                /* invitationTtlHours */ 24,
                /* redeemBaseUrl */ "http://t/"
        );
        service = new ParentInvitationServiceImpl(
                parentRepository,
                invitationRepository,
                linkRepository,
                studentRepository,
                rabbitTemplate,
                outbox,
                objectMapper,
                props
        );

        student = Student.builder().name("Nguyễn Văn A").build();
        student.setId(STUDENT_ID);
        student.setInstanceId(TENANT);
    }

    // ——— invite ————————————————————————————————————————————————

    @Nested
    @DisplayName("invite")
    class Invite {

        @Test
        @DisplayName("creates PENDING invitation, mints 128-bit token, publishes email")
        void happyPath() {
            when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID))
                    .thenReturn(Optional.of(student));
            when(parentRepository.existsByEmailAndInstanceIdAndDeletedFalse(PARENT_EMAIL, TENANT))
                    .thenReturn(false);
            when(invitationRepository.save(any(ParentInvitation.class)))
                    .thenAnswer(inv -> {
                        ParentInvitation saved = inv.getArgument(0);
                        saved.setId(100L);
                        return saved;
                    });

            ParentInvitationResponse response = service.invite(
                    TENANT, STUDENT_ID, PARENT_EMAIL, INVITER_ID);

            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.email()).isEqualTo(PARENT_EMAIL);
            assertThat(response.studentId()).isEqualTo(STUDENT_ID);
            assertThat(response.token()).hasSize(36); // UUID canonical form

            // Capture the persisted invitation so we can assert the exact state
            // the service wrote to the repository.
            ArgumentCaptor<ParentInvitation> captor = ArgumentCaptor.forClass(ParentInvitation.class);
            verify(invitationRepository).save(captor.capture());
            ParentInvitation saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ParentInvitationStatus.PENDING);
            assertThat(saved.getInvitedByUserId()).isEqualTo(INVITER_ID);
            assertThat(saved.getInstanceId()).isEqualTo(TENANT);
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());
            assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(25, ChronoUnit.HOURS));

            verify(rabbitTemplate).convertAndSend(eq("email.exchange"), eq("email.send"), any(Object.class));
            // Per design-patterns.md §3.5.1 Exception A: outbox row is the reliability net.
            verify(outbox).enqueue(eq("email.send"), eq("ParentInvitation"),
                    eq(saved.getId().toString()), anyString());
        }

        @Test
        @DisplayName("rejects when student does not exist in tenant")
        void missingStudent() {
            when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.invite(TENANT, STUDENT_ID, PARENT_EMAIL, INVITER_ID))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(invitationRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("rejects when a Parent already owns the email in this tenant")
        void duplicateParentEmail() {
            when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID))
                    .thenReturn(Optional.of(student));
            when(parentRepository.existsByEmailAndInstanceIdAndDeletedFalse(PARENT_EMAIL, TENANT))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.invite(TENANT, STUDENT_ID, PARENT_EMAIL, INVITER_ID))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(invitationRepository, never()).save(any());
        }

        @Test
        @DisplayName("survives RabbitMQ failure — invitation row still written")
        void emailPublishBestEffort() {
            when(studentRepository.findByIdAndDeletedFalse(STUDENT_ID))
                    .thenReturn(Optional.of(student));
            when(parentRepository.existsByEmailAndInstanceIdAndDeletedFalse(PARENT_EMAIL, TENANT))
                    .thenReturn(false);
            when(invitationRepository.save(any(ParentInvitation.class)))
                    .thenAnswer(inv -> {
                        ParentInvitation saved = inv.getArgument(0);
                        saved.setId(101L);
                        return saved;
                    });
            // Simulate broker outage.
            org.mockito.Mockito.doThrow(new RuntimeException("broker down"))
                    .when(rabbitTemplate)
                    .convertAndSend(anyString(), anyString(), any(Object.class));

            ParentInvitationResponse response = service.invite(
                    TENANT, STUDENT_ID, PARENT_EMAIL, INVITER_ID);

            assertThat(response.status()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("short-circuits when parent-portal feature flag is disabled")
        void featureFlagOff() {
            ParentPortalProperties off = new ParentPortalProperties(
                    false, 24, "http://t/");
            ParentInvitationServiceImpl disabled = new ParentInvitationServiceImpl(
                    parentRepository, invitationRepository, linkRepository,
                    studentRepository, rabbitTemplate, outbox, objectMapper, off);

            assertThatThrownBy(() -> disabled.invite(TENANT, STUDENT_ID, PARENT_EMAIL, INVITER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PARENT_PORTAL_DISABLED");

            verify(studentRepository, never()).findByIdAndDeletedFalse(anyLong());
        }
    }

    // ——— redeem ————————————————————————————————————————————————

    @Nested
    @DisplayName("redeem")
    class Redeem {

        @Test
        @DisplayName("creates Parent + link and marks invitation REDEEMED")
        void happyPath() {
            ParentInvitation invitation = pendingInvitation();
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(invitation));
            when(parentRepository.findByEmailAndInstanceIdAndDeletedFalse(PARENT_EMAIL, TENANT))
                    .thenReturn(Optional.empty());
            when(parentRepository.save(any(Parent.class)))
                    .thenAnswer(inv -> {
                        Parent p = inv.getArgument(0);
                        p.setId(500L);
                        return p;
                    });
            when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(500L, STUDENT_ID))
                    .thenReturn(false);
            when(linkRepository.findStudentIdsByParentId(500L))
                    .thenReturn(List.of(STUDENT_ID));

            RedeemInvitationRequest req = new RedeemInvitationRequest(
                    STRONG_PASSWORD, "Nguyễn Thị B", "0912345678", "MOTHER");

            RedeemInvitationResult result = service.redeem(TENANT, "tok", req);

            assertThat(result.parentId()).isEqualTo(500L);
            assertThat(result.email()).isEqualTo(PARENT_EMAIL);
            assertThat(result.linkedStudentIds()).containsExactly(STUDENT_ID);

            ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);
            verify(parentRepository).save(parentCaptor.capture());
            Parent saved = parentCaptor.getValue();
            assertThat(saved.getRelationship()).isEqualTo(ParentRelationship.MOTHER);
            assertThat(saved.getStatus()).isEqualTo(ParentStatus.ACTIVE);
            assertThat(saved.getInstanceId()).isEqualTo(TENANT);

            ArgumentCaptor<ParentStudentLink> linkCaptor = ArgumentCaptor.forClass(ParentStudentLink.class);
            verify(linkRepository).save(linkCaptor.capture());
            assertThat(linkCaptor.getValue().getParent().getId()).isEqualTo(500L);
            assertThat(linkCaptor.getValue().getStudent().getId()).isEqualTo(STUDENT_ID);

            assertThat(invitation.getStatus()).isEqualTo(ParentInvitationStatus.REDEEMED);
            assertThat(invitation.getRedeemedAt()).isNotNull();
            assertThat(invitation.getRedeemedParentId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("reuses existing Parent when email already has one (sibling redemption)")
        void reusesExistingParent() {
            ParentInvitation invitation = pendingInvitation();
            Parent existing = Parent.builder()
                    .email(PARENT_EMAIL)
                    .fullName("Existing Parent")
                    .status(ParentStatus.ACTIVE)
                    .relationship(ParentRelationship.FATHER)
                    .build();
            existing.setId(999L);
            existing.setInstanceId(TENANT);

            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(invitation));
            when(parentRepository.findByEmailAndInstanceIdAndDeletedFalse(PARENT_EMAIL, TENANT))
                    .thenReturn(Optional.of(existing));
            when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(999L, STUDENT_ID))
                    .thenReturn(false);
            when(linkRepository.findStudentIdsByParentId(999L))
                    .thenReturn(List.of(STUDENT_ID, 77L));

            RedeemInvitationRequest req = new RedeemInvitationRequest(
                    STRONG_PASSWORD, "Existing Parent", null, "FATHER");
            RedeemInvitationResult result = service.redeem(TENANT, "tok", req);

            // Existing ACTIVE parent — service should not re-save profile.
            verify(parentRepository, never()).save(any());
            assertThat(result.parentId()).isEqualTo(999L);
            assertThat(result.linkedStudentIds()).hasSize(2);
        }

        @Test
        @DisplayName("throws and auto-transitions to EXPIRED when token is past expiresAt")
        void expiredToken() {
            ParentInvitation expired = pendingInvitation();
            expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(expired));

            RedeemInvitationRequest req = new RedeemInvitationRequest(
                    STRONG_PASSWORD, "X", null, "GUARDIAN");

            assertThatThrownBy(() -> service.redeem(TENANT, "tok", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PARENT_INVITATION_EXPIRED");

            assertThat(expired.getStatus()).isEqualTo(ParentInvitationStatus.EXPIRED);
        }

        @Test
        @DisplayName("rejects already-redeemed token")
        void alreadyRedeemed() {
            ParentInvitation used = pendingInvitation();
            used.setStatus(ParentInvitationStatus.REDEEMED);
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(used));

            RedeemInvitationRequest req = new RedeemInvitationRequest(
                    STRONG_PASSWORD, "X", null, "GUARDIAN");

            assertThatThrownBy(() -> service.redeem(TENANT, "tok", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PARENT_INVITATION_ALREADY_USED");
        }

        @Test
        @DisplayName("rejects cross-tenant redemption attempts")
        void crossTenantBlocked() {
            ParentInvitation invitation = pendingInvitation(); // tenant = TENANT
            when(invitationRepository.findByTokenAndDeletedFalse("tok"))
                    .thenReturn(Optional.of(invitation));

            UUID otherTenant = UUID.fromString("22222222-2222-2222-2222-222222222222");
            RedeemInvitationRequest req = new RedeemInvitationRequest(
                    STRONG_PASSWORD, "X", null, "GUARDIAN");

            assertThatThrownBy(() -> service.redeem(otherTenant, "tok", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("returns 404 when token is unknown")
        void unknownToken() {
            when(invitationRepository.findByTokenAndDeletedFalse("nope"))
                    .thenReturn(Optional.empty());

            RedeemInvitationRequest req = new RedeemInvitationRequest(
                    STRONG_PASSWORD, "X", null, "GUARDIAN");

            assertThatThrownBy(() -> service.redeem(TENANT, "nope", req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PARENT_INVITATION_NOT_FOUND");
        }
    }

    // ——— expireStale ————————————————————————————————————————————

    @Nested
    @DisplayName("expireStale")
    class ExpireStale {

        @Test
        @DisplayName("transitions PENDING invitations past expiry to EXPIRED and returns count")
        void sweepsPendingRows() {
            ParentInvitation a = pendingInvitation();
            ParentInvitation b = pendingInvitation();
            when(invitationRepository.findByStatusAndExpiresAtBeforeAndDeletedFalse(
                    eq(ParentInvitationStatus.PENDING), any(Instant.class)))
                    .thenReturn(List.of(a, b));

            int count = service.expireStale();

            assertThat(count).isEqualTo(2);
            assertThat(a.getStatus()).isEqualTo(ParentInvitationStatus.EXPIRED);
            assertThat(b.getStatus()).isEqualTo(ParentInvitationStatus.EXPIRED);
            verify(invitationRepository).saveAll(List.of(a, b));
        }

        @Test
        @DisplayName("no-ops when there are no stale rows")
        void emptySweep() {
            when(invitationRepository.findByStatusAndExpiresAtBeforeAndDeletedFalse(
                    eq(ParentInvitationStatus.PENDING), any(Instant.class)))
                    .thenReturn(List.of());

            int count = service.expireStale();

            assertThat(count).isZero();
        }
    }

    // ——— fixtures ————————————————————————————————————————————————

    private ParentInvitation pendingInvitation() {
        ParentInvitation inv = ParentInvitation.builder()
                .email(PARENT_EMAIL)
                .student(student)
                .token("tok")
                .status(ParentInvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .invitedByUserId(INVITER_ID)
                .build();
        inv.setId(10L);
        inv.setInstanceId(TENANT);
        return inv;
    }
}
