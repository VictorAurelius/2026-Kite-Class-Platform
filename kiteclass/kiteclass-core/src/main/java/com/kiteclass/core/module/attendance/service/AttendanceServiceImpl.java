package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.attendance.dto.AttendanceResponse;
import com.kiteclass.core.module.attendance.dto.AttendanceStatsResponse;
import com.kiteclass.core.module.attendance.dto.BulkAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.UpdateAttendanceStatusRequest;
import com.kiteclass.core.module.attendance.entity.Attendance;
import com.kiteclass.core.module.attendance.mapper.AttendanceMapper;
import com.kiteclass.core.module.attendance.repository.AttendanceRepository;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.gamification.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link AttendanceService}.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceMapper attendanceMapper;
    private final PointService pointService;

    @Override
    @Transactional
    public AttendanceResponse markAttendance(CreateAttendanceRequest request) {
        log.info("Marking attendance for enrollment {} in session {}",
                request.getEnrollmentId(), request.getSessionId());

        // BR-ATTEND-001: Validate enrollment exists and is ACTIVE
        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(request.getEnrollmentId())
                .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND",
                        (Object) request.getEnrollmentId()));

        if (!enrollment.isActive()) {
            log.warn("Enrollment {} is not active (status: {})",
                    request.getEnrollmentId(), enrollment.getStatus());
            throw new ValidationException("ENROLLMENT_NOT_ACTIVE",
                    request.getEnrollmentId(), enrollment.getStatus());
        }

        // BR-ATTEND-003: Check for duplicate attendance
        if (attendanceRepository.existsByEnrollmentIdAndSessionIdAndDeletedFalse(
                request.getEnrollmentId(), request.getSessionId())) {
            log.warn("Attendance already marked for enrollment {} in session {}",
                    request.getEnrollmentId(), request.getSessionId());
            throw new ValidationException("ATTENDANCE_ALREADY_MARKED",
                    request.getEnrollmentId(), request.getSessionId());
        }

        // Create attendance entity
        Attendance attendance = attendanceMapper.toEntity(request);
        attendance.setInstanceId(enrollment.getInstanceId()); // Multi-tenant

        // BR-ATTEND-006: Points calculated automatically in @PrePersist
        Attendance savedAttendance = attendanceRepository.save(attendance);

        // Award/deduct points via gamification service
        String pointDescription = String.format("Attendance: %s for session %d",
                savedAttendance.getStatus().getDisplayNameVi(),
                savedAttendance.getSessionId());

        pointService.awardAttendancePoints(
                enrollment.getStudentId(),
                savedAttendance.getId(),
                savedAttendance.getPointsAwarded(),
                pointDescription
        );

        log.info("Successfully marked attendance {} for enrollment {} in session {}",
                savedAttendance.getId(), request.getEnrollmentId(), request.getSessionId());

        return enrichResponse(savedAttendance, enrollment);
    }

    @Override
    @Transactional
    public List<AttendanceResponse> markBulkAttendance(BulkAttendanceRequest request) {
        log.info("Bulk marking attendance for {} students in session {}",
                request.getRecords().size(), request.getSessionId());

        List<AttendanceResponse> responses = new ArrayList<>();

        for (BulkAttendanceRequest.AttendanceRecord record : request.getRecords()) {
            CreateAttendanceRequest singleRequest = CreateAttendanceRequest.builder()
                    .enrollmentId(record.getEnrollmentId())
                    .sessionId(request.getSessionId())
                    .status(record.getStatus())
                    .notes(record.getNotes())
                    .build();

            try {
                AttendanceResponse response = markAttendance(singleRequest);
                responses.add(response);
            } catch (Exception e) {
                log.error("Failed to mark attendance for enrollment {} in session {}: {}",
                        record.getEnrollmentId(), request.getSessionId(), e.getMessage());
                // In bulk operation, we continue even if one fails
                // Alternatively, could rollback entire transaction
                throw e; // For now, fail entire bulk operation on first error
            }
        }

        log.info("Successfully marked {} attendance records for session {}",
                responses.size(), request.getSessionId());

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(Long id) {
        log.debug("Fetching attendance with ID: {}", id);

        Attendance attendance = attendanceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("ATTENDANCE_NOT_FOUND", (Object) id));

        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(attendance.getEnrollmentId())
                .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND",
                        (Object) attendance.getEnrollmentId()));

        return enrichResponse(attendance, enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAttendanceByEnrollment(Long enrollmentId, Pageable pageable) {
        log.debug("Fetching attendance records for enrollment: {}", enrollmentId);

        // Validate enrollment exists
        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) enrollmentId));

        Page<Attendance> attendances = attendanceRepository.findByEnrollmentIdAndDeletedFalse(
                enrollmentId, pageable
        );

        return attendances.map(attendance -> enrichResponse(attendance, enrollment));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAttendanceBySession(Long sessionId, Pageable pageable) {
        log.debug("Fetching attendance records for session: {}", sessionId);

        Page<Attendance> attendances = attendanceRepository.findBySessionIdAndDeletedFalse(
                sessionId, pageable
        );

        return attendances.map(attendance -> {
            Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(attendance.getEnrollmentId())
                    .orElse(null);
            return enrichResponse(attendance, enrollment);
        });
    }

    @Override
    @Transactional
    public AttendanceResponse updateAttendanceStatus(Long id, UpdateAttendanceStatusRequest request) {
        log.info("Updating attendance {} to status {}", id, request.getStatus());

        Attendance attendance = attendanceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("ATTENDANCE_NOT_FOUND", (Object) id));

        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(attendance.getEnrollmentId())
                .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND",
                        (Object) attendance.getEnrollmentId()));

        // Update status and notes
        AttendanceStatus oldStatus = attendance.getStatus();
        attendance.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            attendance.setNotes(request.getNotes());
        }

        // Recalculate points (done in @PreUpdate)
        Attendance updatedAttendance = attendanceRepository.save(attendance);

        // Update points in gamification system if status changed
        if (oldStatus != request.getStatus()) {
            String pointDescription = String.format("Attendance updated: %s for session %d",
                    updatedAttendance.getStatus().getDisplayNameVi(),
                    updatedAttendance.getSessionId());

            pointService.updateAttendancePoints(
                    enrollment.getStudentId(),
                    updatedAttendance.getId(),
                    updatedAttendance.getPointsAwarded(),
                    pointDescription
            );

            log.info("Updated attendance {} points from {} ({} pts) to {} ({} pts)",
                    id, oldStatus, oldStatus.getPointsDeduction(),
                    request.getStatus(), request.getStatus().getPointsDeduction());
        }

        return enrichResponse(updatedAttendance, enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatsResponse getStudentAttendanceStats(Long studentId) {
        log.debug("Calculating attendance statistics for student: {}", studentId);

        // Get all enrollments for student
        // For simplicity, we'll calculate stats across all enrollments
        // In a more complex implementation, could filter by class or date range

        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdAndDeletedFalse(studentId, Pageable.unpaged())
                .getContent();

        long totalSessions = 0;
        long presentCount = 0;
        long absentCount = 0;
        long lateCount = 0;
        long excusedCount = 0;
        long makeupCount = 0;

        for (Enrollment enrollment : enrollments) {
            long enrollmentTotal = attendanceRepository.countByEnrollmentIdAndDeletedFalse(enrollment.getId());
            totalSessions += enrollmentTotal;

            presentCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.PRESENT);
            absentCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.ABSENT);
            lateCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.LATE);
            excusedCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.EXCUSED);
            makeupCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.MAKEUP);
        }

        Double attendanceRate = totalSessions > 0
                ? (presentCount * 100.0 / totalSessions)
                : null;

        return AttendanceStatsResponse.builder()
                .targetId(studentId)
                .targetType("STUDENT")
                .totalSessions(totalSessions)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .excusedCount(excusedCount)
                .makeupCount(makeupCount)
                .attendanceRate(attendanceRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatsResponse getClassAttendanceStats(Long classId) {
        log.debug("Calculating attendance statistics for class: {}", classId);

        // Get all active enrollments for class
        List<Enrollment> enrollments = enrollmentRepository
                .findByClassIdAndDeletedFalse(classId, Pageable.unpaged())
                .getContent();

        long totalSessions = 0;
        long presentCount = 0;
        long absentCount = 0;
        long lateCount = 0;
        long excusedCount = 0;
        long makeupCount = 0;

        for (Enrollment enrollment : enrollments) {
            long enrollmentTotal = attendanceRepository.countByEnrollmentIdAndDeletedFalse(enrollment.getId());
            totalSessions += enrollmentTotal;

            presentCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.PRESENT);
            absentCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.ABSENT);
            lateCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.LATE);
            excusedCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.EXCUSED);
            makeupCount += attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                    enrollment.getId(), AttendanceStatus.MAKEUP);
        }

        Double attendanceRate = totalSessions > 0
                ? (presentCount * 100.0 / totalSessions)
                : null;

        return AttendanceStatsResponse.builder()
                .targetId(classId)
                .targetType("CLASS")
                .totalSessions(totalSessions)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .lateCount(lateCount)
                .excusedCount(excusedCount)
                .makeupCount(makeupCount)
                .attendanceRate(attendanceRate)
                .build();
    }

    @Override
    @Transactional
    public void deleteAttendance(Long id) {
        log.info("Soft deleting attendance: {}", id);

        Attendance attendance = attendanceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("ATTENDANCE_NOT_FOUND", (Object) id));

        attendance.markAsDeleted();
        attendanceRepository.save(attendance);

        log.info("Successfully deleted attendance: {}", id);
    }

    /**
     * Enrich attendance response with related data.
     * Populates studentName, sessionNumber, and markedByName from related entities.
     *
     * @param attendance attendance entity
     * @param enrollment enrollment entity (can be null)
     * @return enriched response
     */
    private AttendanceResponse enrichResponse(Attendance attendance, Enrollment enrollment) {
        AttendanceResponse response = attendanceMapper.toResponse(attendance);

        // Populate studentName from enrollment if available
        // In a real implementation, would fetch Student entity
        if (enrollment != null) {
            response.setStudentName("Student-" + enrollment.getStudentId()); // Placeholder
        }

        // Populate sessionNumber from session if available
        // In a real implementation, would fetch ClassSession entity
        response.setSessionNumber(null); // Placeholder - would fetch from ClassSession

        // Populate markedByName from teacher if available
        // In a real implementation, would fetch Teacher entity
        if (attendance.getMarkedBy() != null) {
            response.setMarkedByName("Teacher-" + attendance.getMarkedBy()); // Placeholder
        }

        return response;
    }
}
