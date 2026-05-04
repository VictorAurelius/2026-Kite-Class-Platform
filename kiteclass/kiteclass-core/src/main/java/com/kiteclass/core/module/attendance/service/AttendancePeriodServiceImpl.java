package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Phase 1A read-only implementation of {@link AttendancePeriodService}.
 *
 * <p>Mapping is performed inline (no MapStruct) to keep the Phase 1A surface
 * minimal. When the write API lands (GAP-323b), a dedicated mapper should be
 * extracted in line with the existing Attendance module pattern.
 *
 * @since GAP-323 Phase 1A (Wave 18b1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendancePeriodServiceImpl implements AttendancePeriodService {

    private final AttendancePeriodRepository repository;

    @Override
    public AttendancePeriodResponse findById(Long id) {
        AttendancePeriod entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "AttendancePeriod not found: " + id));
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
