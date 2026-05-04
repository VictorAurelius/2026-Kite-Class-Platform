package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import com.kiteclass.core.module.childprotection.repository.VettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VettingServiceImpl} — Phase 1B foundation state
 * machine + CRUD with AES-256-encrypted fields (encryption itself is
 * exercised by the converter tests).
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VettingServiceImpl Phase 1B foundation — state machine + CRUD")
class VettingServiceTest {

    @Mock
    private VettingRepository vettingRepository;

    @InjectMocks
    private VettingServiceImpl vettingService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates record with PENDING status + encrypted fields")
        void shouldCreatePending() {
            ArgumentCaptor<Vetting> captor = ArgumentCaptor.forClass(Vetting.class);
            when(vettingRepository.save(any(Vetting.class))).thenAnswer(inv -> {
                Vetting v = inv.getArgument(0);
                v.setId(7L);
                return v;
            });

            Vetting result = vettingService.create(
                    100L,
                    "LLTP-12345",
                    "Police check passed without remarks",
                    Instant.parse("2027-05-04T00:00:00Z")
            );

            verify(vettingRepository).save(captor.capture());
            Vetting persisted = captor.getValue();
            assertThat(persisted.getTeacherId()).isEqualTo(100L);
            assertThat(persisted.getStatus()).isEqualTo(VettingStatus.PENDING);
            assertThat(persisted.getLltpNumber()).isEqualTo("LLTP-12345");
            assertThat(persisted.getPoliceCheckDetails())
                    .isEqualTo("Police check passed without remarks");
            assertThat(persisted.getExpiresAt()).isEqualTo(Instant.parse("2027-05-04T00:00:00Z"));
            assertThat(result.getId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("optional sensitive fields may be null")
        void shouldAllowNullSensitiveFields() {
            when(vettingRepository.save(any(Vetting.class))).thenAnswer(inv -> inv.getArgument(0));

            Vetting result = vettingService.create(100L, null, null, null);

            assertThat(result.getLltpNumber()).isNull();
            assertThat(result.getPoliceCheckDetails()).isNull();
            assertThat(result.getExpiresAt()).isNull();
        }

        @Test
        @DisplayName("null teacherId rejected")
        void shouldRejectNullTeacher() {
            assertThatThrownBy(() -> vettingService.create(null, "x", "y", null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("VETTING_TEACHER_ID_REQUIRED");
            verify(vettingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById / findLatestForTeacher")
    class Find {

        @Test
        @DisplayName("findById returns existing non-deleted vetting")
        void shouldFindById() {
            Vetting v = sample(5L, VettingStatus.PENDING);
            when(vettingRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(v));

            assertThat(vettingService.findById(5L)).isSameAs(v);
        }

        @Test
        @DisplayName("findById missing → EntityNotFoundException")
        void shouldThrowWhenMissing() {
            when(vettingRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vettingService.findById(404L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("VETTING_NOT_FOUND");
        }

        @Test
        @DisplayName("findLatestForTeacher returns latest non-deleted record")
        void shouldFindLatest() {
            Vetting v = sample(9L, VettingStatus.APPROVED);
            when(vettingRepository.findFirstByTeacherIdAndDeletedFalseOrderByIdDesc(100L))
                    .thenReturn(Optional.of(v));

            assertThat(vettingService.findLatestForTeacher(100L)).isSameAs(v);
        }

        @Test
        @DisplayName("findLatestForTeacher missing → EntityNotFoundException")
        void shouldThrowWhenNoTeacherRecord() {
            when(vettingRepository.findFirstByTeacherIdAndDeletedFalseOrderByIdDesc(100L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vettingService.findLatestForTeacher(100L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("VETTING_NOT_FOUND_FOR_TEACHER");
        }

        @Test
        @DisplayName("findLatestForTeacher null teacherId → ValidationException")
        void shouldRejectNullTeacherId() {
            assertThatThrownBy(() -> vettingService.findLatestForTeacher(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("VETTING_TEACHER_ID_REQUIRED");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("delegates filter to repository")
        void shouldDelegateFilter() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vetting> page = new PageImpl<>(List.of(sample(1L, VettingStatus.PENDING)));
            when(vettingRepository.findByFilters(eq(VettingStatus.PENDING), eq(pageable)))
                    .thenReturn(page);

            Page<Vetting> result = vettingService.findAll(VettingStatus.PENDING, pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("null status passes through (no filter)")
        void shouldAllowNullStatus() {
            Pageable pageable = PageRequest.of(0, 5);
            when(vettingRepository.findByFilters(eq(null), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(vettingService.findAll(null, pageable)).isEmpty();
        }
    }

    @Nested
    @DisplayName("transition — state machine")
    class Transition {

        @Test
        @DisplayName("PENDING → SUBMITTED stamps submittedAt")
        void shouldTransitionPendingToSubmitted() {
            Vetting v = sample(1L, VettingStatus.PENDING);
            when(vettingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(v));
            when(vettingRepository.save(any(Vetting.class))).thenAnswer(inv -> inv.getArgument(0));

            Vetting result = vettingService.transition(1L, VettingStatus.SUBMITTED, 50L);

            assertThat(result.getStatus()).isEqualTo(VettingStatus.SUBMITTED);
            assertThat(result.getSubmittedAt()).isNotNull();
        }

        @Test
        @DisplayName("SUBMITTED → INTERVIEW_DONE stamps interviewedAt")
        void shouldTransitionSubmittedToInterview() {
            Vetting v = sample(2L, VettingStatus.SUBMITTED);
            when(vettingRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(v));
            when(vettingRepository.save(any(Vetting.class))).thenAnswer(inv -> inv.getArgument(0));

            Vetting result = vettingService.transition(2L, VettingStatus.INTERVIEW_DONE, 50L);

            assertThat(result.getStatus()).isEqualTo(VettingStatus.INTERVIEW_DONE);
            assertThat(result.getInterviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("INTERVIEW_DONE → APPROVED records officer + decidedAt")
        void shouldTransitionToApproved() {
            Vetting v = sample(3L, VettingStatus.INTERVIEW_DONE);
            when(vettingRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(v));
            when(vettingRepository.save(any(Vetting.class))).thenAnswer(inv -> inv.getArgument(0));

            Vetting result = vettingService.transition(3L, VettingStatus.APPROVED, 555L);

            assertThat(result.getStatus()).isEqualTo(VettingStatus.APPROVED);
            assertThat(result.getDecidedByUserId()).isEqualTo(555L);
            assertThat(result.getDecidedAt()).isNotNull();
        }

        @Test
        @DisplayName("INTERVIEW_DONE → REJECTED records officer + decidedAt")
        void shouldTransitionToRejected() {
            Vetting v = sample(3L, VettingStatus.INTERVIEW_DONE);
            when(vettingRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(v));
            when(vettingRepository.save(any(Vetting.class))).thenAnswer(inv -> inv.getArgument(0));

            Vetting result = vettingService.transition(3L, VettingStatus.REJECTED, 555L);

            assertThat(result.getStatus()).isEqualTo(VettingStatus.REJECTED);
            assertThat(result.getDecidedByUserId()).isEqualTo(555L);
        }

        @Test
        @DisplayName("APPROVED → EXPIRED allowed")
        void shouldTransitionApprovedToExpired() {
            Vetting v = sample(4L, VettingStatus.APPROVED);
            when(vettingRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(v));
            when(vettingRepository.save(any(Vetting.class))).thenAnswer(inv -> inv.getArgument(0));

            Vetting result = vettingService.transition(4L, VettingStatus.EXPIRED, null);

            assertThat(result.getStatus()).isEqualTo(VettingStatus.EXPIRED);
        }

        @Test
        @DisplayName("PENDING → APPROVED rejected — illegal jump")
        void shouldRejectIllegalJump() {
            Vetting v = sample(1L, VettingStatus.PENDING);
            when(vettingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(v));

            assertThatThrownBy(() -> vettingService.transition(1L, VettingStatus.APPROVED, 50L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("VETTING_INVALID_TRANSITION");
            verify(vettingRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECTED → anything rejected — terminal")
        void shouldRejectFromTerminal() {
            Vetting v = sample(5L, VettingStatus.REJECTED);
            when(vettingRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(v));

            assertThatThrownBy(() ->
                    vettingService.transition(5L, VettingStatus.APPROVED, 50L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("VETTING_INVALID_TRANSITION");
        }

        @Test
        @DisplayName("EXPIRED → anything rejected — terminal")
        void shouldRejectFromExpired() {
            Vetting v = sample(6L, VettingStatus.EXPIRED);
            when(vettingRepository.findByIdAndDeletedFalse(6L)).thenReturn(Optional.of(v));

            assertThatThrownBy(() ->
                    vettingService.transition(6L, VettingStatus.APPROVED, 50L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("VETTING_INVALID_TRANSITION");
        }

        @Test
        @DisplayName("null target rejected")
        void shouldRejectNullTarget() {
            assertThatThrownBy(() -> vettingService.transition(1L, null, 50L))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("VETTING_TARGET_STATUS_REQUIRED");
        }

        @Test
        @DisplayName("EntityNotFoundException is a BusinessException — propagates 404")
        void shouldThrowEntityNotFoundForMissingId() {
            when(vettingRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

            // Sanity check that the not-found path uses BusinessException family.
            assertThatThrownBy(() -> vettingService.transition(404L, VettingStatus.SUBMITTED, 50L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("VETTING_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("softDelete")
    class SoftDelete {

        @Test
        @DisplayName("marks vetting as deleted and saves")
        void shouldSoftDelete() {
            Vetting v = sample(10L, VettingStatus.PENDING);
            assertThat(v.isDeleted()).isFalse();
            when(vettingRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(v));
            when(vettingRepository.save(any(Vetting.class))).thenAnswer(inv -> inv.getArgument(0));

            vettingService.softDelete(10L);

            assertThat(v.isDeleted()).isTrue();
            verify(vettingRepository).save(v);
        }
    }

    private static Vetting sample(Long id, VettingStatus status) {
        Vetting v = Vetting.builder()
                .teacherId(100L)
                .status(status)
                .build();
        v.setId(id);
        return v;
    }
}
