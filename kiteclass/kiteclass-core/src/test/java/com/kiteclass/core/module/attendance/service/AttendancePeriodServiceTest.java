package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttendancePeriodService} (Phase 1A read-only).
 *
 * <p>Tests cover the read-only query surface for K-12 per-period attendance
 * (TT 22/2021/TT-BGDĐT). Write API + GVCN mobile UI deferred to GAP-323b.
 *
 * @since GAP-323 Phase 1A (Wave 18b1)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendancePeriodService (Phase 1A read-only) Tests")
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
}
