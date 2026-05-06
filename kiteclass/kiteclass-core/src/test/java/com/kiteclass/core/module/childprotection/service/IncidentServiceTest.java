package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.repository.IncidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
 * Unit tests for {@link IncidentService} — Phase 1A skeleton CRUD with
 * encryption persisted transparently via {@code AesGcmAttributeConverter}
 * (the converter itself is tested in
 * {@code AesGcmAttributeConverterTest}).
 *
 * <p>This test mocks the repository to focus on service-layer validation +
 * orchestration; the encryption roundtrip is exercised end-to-end at the
 * converter unit-test level.
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IncidentService Phase 1A — CRUD with encrypted fields")
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private IncidentService incidentService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happy path persists incident with REPORTED status + plaintext title + sensitive description")
        void shouldCreateIncident() {
            ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
            when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
                Incident i = inv.getArgument(0);
                i.setId(42L);
                return i;
            });

            Incident result = incidentService.create(
                    "Bullying observed in class 7A",
                    "Detailed sensitive narrative here",
                    "minio/evidence-1.jpg\nminio/evidence-2.png",
                    IncidentSeverity.HIGH,
                    IncidentCategory.BULLYING,
                    100L,
                    200L
            );

            verify(incidentRepository).save(captor.capture());
            Incident persisted = captor.getValue();
            assertThat(persisted.getTitle()).isEqualTo("Bullying observed in class 7A");
            assertThat(persisted.getDescription()).isEqualTo("Detailed sensitive narrative here");
            assertThat(persisted.getEvidencePaths())
                    .isEqualTo("minio/evidence-1.jpg\nminio/evidence-2.png");
            assertThat(persisted.getSeverity()).isEqualTo(IncidentSeverity.HIGH);
            assertThat(persisted.getCategory()).isEqualTo(IncidentCategory.BULLYING);
            assertThat(persisted.getStatus()).isEqualTo(IncidentStatus.REPORTED);
            assertThat(persisted.getReporterUserId()).isEqualTo(100L);
            assertThat(persisted.getSubjectStudentId()).isEqualTo(200L);
            assertThat(result.getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("optional sensitive fields may be null")
        void shouldAllowNullDescriptionAndEvidence() {
            when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

            Incident result = incidentService.create(
                    "Concern reported",
                    null,
                    null,
                    IncidentSeverity.LOW,
                    IncidentCategory.OTHER,
                    100L,
                    null
            );

            assertThat(result.getDescription()).isNull();
            assertThat(result.getEvidencePaths()).isNull();
            assertThat(result.getSubjectStudentId()).isNull();
        }

        @Test
        @DisplayName("blank title rejected")
        void shouldRejectBlankTitle() {
            assertThatThrownBy(() -> incidentService.create(
                    "  ",
                    null, null,
                    IncidentSeverity.LOW, IncidentCategory.OTHER,
                    100L, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("INCIDENT_TITLE_REQUIRED");

            verify(incidentRepository, never()).save(any());
        }

        @Test
        @DisplayName("title >200 chars rejected")
        void shouldRejectTooLongTitle() {
            String tooLong = "x".repeat(201);
            assertThatThrownBy(() -> incidentService.create(
                    tooLong,
                    null, null,
                    IncidentSeverity.LOW, IncidentCategory.OTHER,
                    100L, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("INCIDENT_TITLE_TOO_LONG");
        }

        @Test
        @DisplayName("null severity rejected")
        void shouldRejectNullSeverity() {
            assertThatThrownBy(() -> incidentService.create(
                    "Title",
                    null, null,
                    null, IncidentCategory.OTHER,
                    100L, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("INCIDENT_SEVERITY_REQUIRED");
        }

        @Test
        @DisplayName("null category rejected")
        void shouldRejectNullCategory() {
            assertThatThrownBy(() -> incidentService.create(
                    "Title",
                    null, null,
                    IncidentSeverity.LOW, null,
                    100L, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("INCIDENT_CATEGORY_REQUIRED");
        }

        @Test
        @DisplayName("null reporterUserId rejected")
        void shouldRejectNullReporter() {
            assertThatThrownBy(() -> incidentService.create(
                    "Title",
                    null, null,
                    IncidentSeverity.LOW, IncidentCategory.OTHER,
                    null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("INCIDENT_REPORTER_REQUIRED");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns incident when present + not deleted")
        void shouldFindIncident() {
            Incident incident = sample(7L);
            when(incidentRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(incident));

            Incident result = incidentService.findById(7L);

            assertThat(result).isSameAs(incident);
        }

        @Test
        @DisplayName("missing → EntityNotFoundException")
        void shouldThrowWhenMissing() {
            when(incidentRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("INCIDENT_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("delegates filters to repository")
        void shouldDelegateFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Incident> page = new PageImpl<>(List.of(sample(1L), sample(2L)));
            when(incidentRepository.findByFilters(
                    IncidentSeverity.CRITICAL,
                    IncidentCategory.ABUSE,
                    IncidentStatus.REPORTED,
                    pageable
            )).thenReturn(page);

            Page<Incident> result = incidentService.findAll(
                    IncidentSeverity.CRITICAL,
                    IncidentCategory.ABUSE,
                    IncidentStatus.REPORTED,
                    pageable
            );

            assertThat(result.getContent()).hasSize(2);
            verify(incidentRepository).findByFilters(
                    eq(IncidentSeverity.CRITICAL),
                    eq(IncidentCategory.ABUSE),
                    eq(IncidentStatus.REPORTED),
                    eq(pageable)
            );
        }

        @Test
        @DisplayName("null filters pass through (returns all)")
        void shouldAllowNullFilters() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<Incident> empty = new PageImpl<>(List.of());
            when(incidentRepository.findByFilters(null, null, null, pageable)).thenReturn(empty);

            Page<Incident> result = incidentService.findAll(null, null, null, pageable);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("updates status from REPORTED → INVESTIGATING")
        void shouldUpdateStatus() {
            Incident incident = sample(5L);
            incident.setStatus(IncidentStatus.REPORTED);
            when(incidentRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

            Incident result = incidentService.updateStatus(5L, IncidentStatus.INVESTIGATING);

            assertThat(result.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);
        }

        @Test
        @DisplayName("null status rejected")
        void shouldRejectNullStatus() {
            assertThatThrownBy(() -> incidentService.updateStatus(5L, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("INCIDENT_STATUS_REQUIRED");
            verify(incidentRepository, never()).save(any());
        }

        @Test
        @DisplayName("missing incident → EntityNotFoundException")
        void shouldThrowWhenMissing() {
            when(incidentRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.updateStatus(404L, IncidentStatus.RESOLVED))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("assignOfficer")
    class AssignOfficer {

        @Test
        @DisplayName("happy path sets officer user id")
        void shouldAssignOfficer() {
            Incident incident = sample(8L);
            when(incidentRepository.findByIdAndDeletedFalse(8L)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

            Incident result = incidentService.assignOfficer(8L, 555L);

            assertThat(result.getAssignedOfficerUserId()).isEqualTo(555L);
        }

        @Test
        @DisplayName("null officer rejected")
        void shouldRejectNullOfficer() {
            assertThatThrownBy(() -> incidentService.assignOfficer(8L, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("INCIDENT_OFFICER_REQUIRED");
        }
    }

    @Nested
    @DisplayName("softDelete")
    class SoftDelete {

        @Test
        @DisplayName("marks incident as deleted")
        void shouldSoftDelete() {
            Incident incident = sample(10L);
            assertThat(incident.isDeleted()).isFalse();
            when(incidentRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

            incidentService.softDelete(10L);

            assertThat(incident.isDeleted()).isTrue();
            verify(incidentRepository).save(incident);
        }
    }

    private static Incident sample(Long id) {
        Incident i = Incident.builder()
                .title("Sample title")
                .description("Sample description")
                .severity(IncidentSeverity.MEDIUM)
                .category(IncidentCategory.BULLYING)
                .status(IncidentStatus.REPORTED)
                .reporterUserId(1L)
                .build();
        i.setId(id);
        return i;
    }
}
