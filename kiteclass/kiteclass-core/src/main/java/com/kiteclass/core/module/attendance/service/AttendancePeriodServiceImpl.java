package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodBatchCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodUpdateRequest;
import com.kiteclass.core.module.attendance.dto.DailyAttendanceRollupResponse;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link AttendancePeriodService}.
 *
 * <p>Phase 1A (read-only) shipped Wave 18b1. Phase 1B adds idempotent batch
 * upsert, single-row update with {@code @Version} optimistic lock, and an
 * on-demand daily roll-up aggregation. Mapping stays inline; if the surface
 * grows further a dedicated mapper should be extracted alongside the existing
 * legacy {@code AttendanceMapper}.
 *
 * <p>Threshold for {@code allDayAbsent} = 7 (TT 22/2021/TT-BGDĐT) is centralised
 * in {@link #ALL_DAY_ABSENT_THRESHOLD}; LATE counts toward "missed instructional
 * time" together with ABSENT for the daily metric.
 *
 * @since GAP-323 Phase 1A (Wave 18b1); Phase 1B GAP-323b (Wave 18b2)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendancePeriodServiceImpl implements AttendancePeriodService {

    private static final long ALL_DAY_ABSENT_THRESHOLD = 7L;

    private final AttendancePeriodRepository repository;

    @Override
    public AttendancePeriodResponse findById(Long id) {
        AttendancePeriod entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ATTENDANCE_PERIOD_NOT_FOUND", id));
        return toResponse(entity);
    }

    @Override
    public Page<AttendancePeriodResponse> findByStudent(
            Long studentId, LocalDate from, LocalDate to, Pageable pageable) {
        return repository
                .findByStudentIdAndDateBetweenAndDeletedFalse(studentId, from, to, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<AttendancePeriodResponse> findByClassAndDate(Long classId, LocalDate date) {
        return repository
                .findByClassIdAndDateAndDeletedFalse(classId, date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Page<AttendancePeriodResponse> findBySubjectSection(
            Long subjectSectionId, LocalDate from, LocalDate to, Pageable pageable) {
        return repository
                .findBySubjectSectionIdAndDateBetweenAndDeletedFalse(
                        subjectSectionId, from, to, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public List<AttendancePeriodResponse> upsertBatch(
            AttendancePeriodBatchCreateRequest request, Long recordedBy) {

        UUID tenantId = TenantContext.getCurrentTenant();
        LocalDateTime now = LocalDateTime.now();
        List<AttendancePeriodResponse> out = new ArrayList<>(request.getEntries().size());

        for (AttendancePeriodCreateRequest entry : request.getEntries()) {
            AttendancePeriod row = repository
                    .findByStudentIdAndSubjectSectionIdAndDateAndPeriodNoAndDeletedFalse(
                            entry.getStudentId(),
                            entry.getSubjectSectionId(),
                            entry.getDate(),
                            entry.getPeriodNo())
                    .map(existing -> {
                        existing.setStatus(entry.getStatus());
                        existing.setNotes(entry.getNotes());
                        existing.setRecordedBy(recordedBy);
                        existing.setRecordedAt(now);
                        existing.setClassId(entry.getClassId());
                        return existing;
                    })
                    .orElseGet(() -> {
                        AttendancePeriod fresh = AttendancePeriod.builder()
                                .studentId(entry.getStudentId())
                                .classId(entry.getClassId())
                                .subjectSectionId(entry.getSubjectSectionId())
                                .periodNo(entry.getPeriodNo())
                                .date(entry.getDate())
                                .status(entry.getStatus())
                                .recordedBy(recordedBy)
                                .recordedAt(now)
                                .notes(entry.getNotes())
                                .build();
                        fresh.setInstanceId(tenantId);
                        return fresh;
                    });

            out.add(toResponse(repository.save(row)));
        }

        log.info("Upserted {} attendance_period rows for tenant {} by teacher {}",
                out.size(), tenantId, recordedBy);
        return out;
    }

    @Override
    @Transactional
    public AttendancePeriodResponse update(
            Long id, AttendancePeriodUpdateRequest request, Long recordedBy) {

        AttendancePeriod existing = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ATTENDANCE_PERIOD_NOT_FOUND", id));

        if (!request.getVersion().equals(existing.getVersion())) {
            throw new OptimisticLockingFailureException(
                    "Stale version for attendance_period id=" + id
                            + " (got=" + request.getVersion()
                            + ", current=" + existing.getVersion() + ")");
        }

        existing.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            existing.setNotes(request.getNotes());
        }
        existing.setRecordedBy(recordedBy);
        existing.setRecordedAt(LocalDateTime.now());

        return toResponse(repository.save(existing));
    }

    @Override
    public List<DailyAttendanceRollupResponse> dailyRollupForClass(
            Long classId, LocalDate from, LocalDate to) {

        List<Object[]> rows = repository.aggregateDailyRollupForClass(classId, from, to);
        List<DailyAttendanceRollupResponse> out = new ArrayList<>(rows.size());

        for (Object[] r : rows) {
            long absent = ((Number) r[5]).longValue();
            long late = ((Number) r[6]).longValue();
            boolean allDayAbsent = (absent + late) >= ALL_DAY_ABSENT_THRESHOLD;

            out.add(DailyAttendanceRollupResponse.builder()
                    .studentId(((Number) r[0]).longValue())
                    .classId(((Number) r[1]).longValue())
                    .date((LocalDate) r[2])
                    .periodCount(((Number) r[3]).longValue())
                    .presentCount(((Number) r[4]).longValue())
                    .absentCount(absent)
                    .lateCount(late)
                    .excusedCount(((Number) r[7]).longValue())
                    .makeupCount(((Number) r[8]).longValue())
                    .allDayAbsent(allDayAbsent)
                    .build());
        }
        return out;
    }

    private AttendancePeriodResponse toResponse(AttendancePeriod entity) {
        return AttendancePeriodResponse.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .classId(entity.getClassId())
                .subjectSectionId(entity.getSubjectSectionId())
                .periodNo(entity.getPeriodNo())
                .date(entity.getDate())
                .status(entity.getStatus())
                .recordedBy(entity.getRecordedBy())
                .recordedAt(entity.getRecordedAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
