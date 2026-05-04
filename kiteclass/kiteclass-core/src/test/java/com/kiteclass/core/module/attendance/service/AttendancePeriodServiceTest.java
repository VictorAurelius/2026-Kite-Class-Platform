package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodBatchCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodUpdateRequest;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttendancePeriodService}.
 *
 * <p>Phase 1A read-only tests + Phase 1B write tests (idempotent upsert,
 * optimistic-lock update). Daily roll-up has DB-level aggregation; covered by
 * the integration test, not here.
 *
 * @since GAP-323 Phase 1A (Wave 18b1); Phase 1B GAP-323b (Wave 18b2)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendancePeriodService Tests (Phase 1A read + Phase 1B write)")
class AttendancePeriodServiceTest {

    @Mock
    private AttendancePeriodRepository repository;

    @InjectMocks
    private AttendancePeriodServiceImpl service;

    private AttendancePeriod sampleRecord;
    private final LocalDate sampleDate = LocalDate.of(2026, 9, 5);
    private final UUID instanceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sampleRecord = AttendancePeriod.builder()
                .studentId(101L)
                .classId(202L)
                .subjectSectionId(303L)
                .periodNo(1)
                .date(sampleDate)
                .status(AttendanceStatus.PRESENT)
                .recordedBy(404L)
                .recordedAt(LocalDateTime.of(2026, 9, 5, 7, 5))
                .build();
        sampleRecord.setId(1L);
        sampleRecord.setInstanceId(instanceId);
    }

    @Test
    @DisplayName("findById returns response when record exists")
    void findById_existing_returnsResponse() {
        when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(sampleRecord));

        AttendancePeriodResponse result = service.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStudentId()).isEqualTo(101L);
        assertThat(result.getPeriodNo()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("findByStudentAndDateRange returns paginated records")
    void findByStudentAndDateRange_returnsPage() {
        Page<AttendancePeriod> page = new PageImpl<>(List.of(sampleRecord));
        when(repository.findByStudentIdAndDateBetweenAndDeletedFalse(
                eq(101L),
                eq(sampleDate),
                eq(sampleDate.plusDays(7)),
                any(Pageable.class)))
                .thenReturn(page);

        Page<AttendancePeriodResponse> result = service.findByStudent(
                101L, sampleDate, sampleDate.plusDays(7), Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStudentId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("findByClassAndDate returns daily roster")
    void findByClassAndDate_returnsRoster() {
        when(repository.findByClassIdAndDateAndDeletedFalse(202L, sampleDate))
                .thenReturn(List.of(sampleRecord));

        List<AttendancePeriodResponse> result = service.findByClassAndDate(202L, sampleDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassId()).isEqualTo(202L);
        assertThat(result.get(0).getDate()).isEqualTo(sampleDate);
    }

    @Test
    @DisplayName("findBySubjectSection filters by subject")
    void findBySubjectSection_filtersBySubject() {
        Page<AttendancePeriod> page = new PageImpl<>(List.of(sampleRecord));
        when(repository.findBySubjectSectionIdAndDateBetweenAndDeletedFalse(
                eq(303L),
                eq(sampleDate),
                eq(sampleDate.plusDays(30)),
                any(Pageable.class)))
                .thenReturn(page);

        Page<AttendancePeriodResponse> result = service.findBySubjectSection(
                303L, sampleDate, sampleDate.plusDays(30), Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSubjectSectionId()).isEqualTo(303L);
    }

    // ----- Phase 1B (GAP-323b) write API tests ---------------------------------

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("upsertBatch inserts when no existing row matches the unique tuple")
    void upsertBatch_insertsWhenAbsent() {
        TenantContext.setCurrentTenant(instanceId);
        AttendancePeriodCreateRequest entry = AttendancePeriodCreateRequest.builder()
                .studentId(101L)
                .classId(202L)
                .subjectSectionId(303L)
                .periodNo(2)
                .date(sampleDate)
                .status(AttendanceStatus.ABSENT)
                .notes("phụ huynh xin phép")
                .build();
        AttendancePeriodBatchCreateRequest batch = AttendancePeriodBatchCreateRequest.builder()
                .entries(List.of(entry))
                .build();

        when(repository.findByStudentIdAndSubjectSectionIdAndDateAndPeriodNoAndDeletedFalse(
                101L, 303L, sampleDate, 2)).thenReturn(Optional.empty());
        when(repository.save(any(AttendancePeriod.class))).thenAnswer(inv -> {
            AttendancePeriod saved = inv.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        List<AttendancePeriodResponse> result = service.upsertBatch(batch, 909L);

        assertThat(result).hasSize(1);
        AttendancePeriodResponse out = result.get(0);
        assertThat(out.getId()).isEqualTo(42L);
        assertThat(out.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(out.getRecordedBy()).isEqualTo(909L);
        verify(repository, times(1)).save(any(AttendancePeriod.class));
    }

    @Test
    @DisplayName("upsertBatch updates the existing row when the unique tuple matches")
    void upsertBatch_updatesWhenPresent() {
        TenantContext.setCurrentTenant(instanceId);
        AttendancePeriodCreateRequest entry = AttendancePeriodCreateRequest.builder()
                .studentId(101L)
                .classId(202L)
                .subjectSectionId(303L)
                .periodNo(1)
                .date(sampleDate)
                .status(AttendanceStatus.LATE)
                .notes("đi muộn 5 phút")
                .build();
        AttendancePeriodBatchCreateRequest batch = AttendancePeriodBatchCreateRequest.builder()
                .entries(List.of(entry))
                .build();

        when(repository.findByStudentIdAndSubjectSectionIdAndDateAndPeriodNoAndDeletedFalse(
                101L, 303L, sampleDate, 1)).thenReturn(Optional.of(sampleRecord));
        when(repository.save(any(AttendancePeriod.class))).thenAnswer(inv -> inv.getArgument(0));

        List<AttendancePeriodResponse> result = service.upsertBatch(batch, 909L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(result.get(0).getNotes()).isEqualTo("đi muộn 5 phút");
        assertThat(result.get(0).getRecordedBy()).isEqualTo(909L);
        // Same row reused — id preserved
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("update succeeds when version matches the current row")
    void update_succeedsOnFreshVersion() {
        sampleRecord.setVersion(3L);
        when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(sampleRecord));
        when(repository.save(any(AttendancePeriod.class))).thenAnswer(inv -> inv.getArgument(0));

        AttendancePeriodUpdateRequest req = AttendancePeriodUpdateRequest.builder()
                .status(AttendanceStatus.EXCUSED)
                .notes("ốm")
                .version(3L)
                .build();

        AttendancePeriodResponse out = service.update(1L, req, 808L);

        assertThat(out.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
        assertThat(out.getNotes()).isEqualTo("ốm");
        assertThat(out.getRecordedBy()).isEqualTo(808L);
    }

    @Test
    @DisplayName("update throws optimistic-lock when version is stale")
    void update_throwsOnStaleVersion() {
        sampleRecord.setVersion(5L);
        when(repository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(sampleRecord));

        AttendancePeriodUpdateRequest req = AttendancePeriodUpdateRequest.builder()
                .status(AttendanceStatus.EXCUSED)
                .version(3L) // stale
                .build();

        assertThatThrownBy(() -> service.update(1L, req, 808L))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("update throws not-found when row is missing or soft-deleted")
    void update_throwsWhenMissing() {
        when(repository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        AttendancePeriodUpdateRequest req = AttendancePeriodUpdateRequest.builder()
                .status(AttendanceStatus.PRESENT)
                .version(0L)
                .build();

        assertThatThrownBy(() -> service.update(999L, req, 808L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
