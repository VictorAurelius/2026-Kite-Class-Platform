package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.security.AuthorizationBean;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.attendance.dto.AttendanceResponse;
import com.kiteclass.core.module.attendance.dto.AttendanceStatsResponse;
import com.kiteclass.core.module.attendance.dto.BulkAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.UpdateAttendanceStatusRequest;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.module.attendance.entity.Attendance;
import com.kiteclass.core.module.attendance.event.AttendanceMarkedEvent;
import com.kiteclass.core.module.attendance.mapper.AttendanceMapper;
import com.kiteclass.core.module.attendance.repository.AttendanceRepository;
import com.kiteclass.core.common.constant.SessionStatus;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.gamification.service.PointService;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ClassSessionRepository classSessionRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final AttendanceMapper attendanceMapper;
    private final PointService pointService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthorizationBean authz;

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

        // BR-ATTEND-002 (GAP-992): Session must exist and not be COMPLETED/CANCELLED
        ClassSession session = classSessionRepository.findByIdAndDeletedFalse(request.getSessionId())
                .orElseThrow(() -> new EntityNotFoundException("SESSION_NOT_FOUND",
                        (Object) request.getSessionId()));
        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED) {
            log.warn("Cannot mark attendance for session {} with status {}",
                    request.getSessionId(), session.getStatus());
            throw new ValidationException("SESSION_NOT_MARKABLE",
                    request.getSessionId(), session.getStatus());
        }

        // BR-ATT-005 (GAP-993): EXCUSED status requires an excuse note
        if (request.getStatus() == AttendanceStatus.EXCUSED
                && (request.getNotes() == null || request.getNotes().isBlank())) {
            throw new ValidationException("EXCUSED_REQUIRES_NOTE", request.getEnrollmentId());
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
    public List<AttendanceResponse> markBulkAttendance(
            Long classId,
            Long sessionId,
            BulkAttendanceRequest request,
            Long teacherId
    ) {
        log.info("Bulk marking attendance for {} students in session {} by teacher {}",
                request.getRecords().size(), sessionId, teacherId);

        // 1. Permission Check: Verify teacher is MAIN_TEACHER in this class.
        // GAP-1301/GAP-1299: ADMIN/OWNER (tenant-admin) carry no numeric reference id
        // (teacherId == null) and have no TeacherClass row, so they bypass the per-class
        // MAIN_TEACHER check — mirrors AuthorizationBean.isAdmin() used across the per-resource
        // authz beans. A real teacher uses the token-derived reference id (NOT a client header).
        if (!authz.isAdmin()) {
            TeacherClass teacherClass = teacherClassRepository
                    .findByTeacherIdAndClassId(teacherId, classId)
                    .orElseThrow(() -> new PermissionDeniedException("TEACHER_NOT_IN_CLASS"));

            if (teacherClass.getRole() != TeacherClassRole.MAIN_TEACHER) {
                log.warn("Teacher {} is not MAIN_TEACHER in class {} (role: {})",
                        teacherId, classId, teacherClass.getRole());
                throw new PermissionDeniedException("ONLY_MAIN_TEACHER_CAN_MARK_ATTENDANCE");
            }
        }

        // 2. Validate session exists and belongs to class
        ClassSession session = classSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("SESSION_NOT_FOUND", (Object) sessionId));

        if (!session.getClassId().equals(classId)) {
            log.warn("Session {} does not belong to class {}", sessionId, classId);
            throw new ValidationException("SESSION_NOT_IN_CLASS", new Object[0]);
        }

        // BR-ATTEND-002 (GAP-992): Session must not be COMPLETED/CANCELLED — fail fast for whole batch
        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED) {
            log.warn("Cannot bulk-mark attendance for session {} with status {}",
                    sessionId, session.getStatus());
            throw new ValidationException("SESSION_NOT_MARKABLE", sessionId, session.getStatus());
        }

        // 3. Validate all enrollments belong to this class
        Set<Long> enrollmentIds = request.getRecords().stream()
                .map(BulkAttendanceRequest.AttendanceRecord::getEnrollmentId)
                .collect(Collectors.toSet());

        List<Enrollment> enrollments = enrollmentRepository.findAllById(enrollmentIds);

        if (enrollments.size() != enrollmentIds.size()) {
            log.warn("Not all enrollment IDs found: expected {}, found {}",
                    enrollmentIds.size(), enrollments.size());
            throw new EntityNotFoundException("ENROLLMENT_NOT_FOUND", new Object[0]);
        }

        // Verify all enrollments are for this class
        boolean allInClass = enrollments.stream()
                .allMatch(e -> e.getClassId().equals(classId) && !e.isDeleted());

        if (!allInClass) {
            log.warn("Not all enrollments belong to class {}", classId);
            throw new ValidationException("ENROLLMENT_NOT_IN_CLASS", new Object[0]);
        }

        // 4. Create or update attendance records
        List<Attendance> attendanceList = new ArrayList<>();
        List<AttendanceResponse> responses = new ArrayList<>();

        for (BulkAttendanceRequest.AttendanceRecord record : request.getRecords()) {
            CreateAttendanceRequest singleRequest = CreateAttendanceRequest.builder()
                    .enrollmentId(record.getEnrollmentId())
                    .sessionId(sessionId)
                    .status(record.getStatus())
                    .notes(record.getNotes())
                    .markedBy(teacherId)
                    .build();

            // Mark attendance (reuse existing method)
            AttendanceResponse response = markAttendance(singleRequest);
            responses.add(response);

            // Collect attendance for event
            Attendance attendance = attendanceRepository.findByIdAndDeletedFalse(response.getId())
                    .orElseThrow(() -> new EntityNotFoundException("ATTENDANCE_NOT_FOUND", (Object) response.getId()));
            attendanceList.add(attendance);
        }

        // 5. Update ClassSession.attendanceTaken flag
        session.setAttendanceTaken(true);
        classSessionRepository.save(session);

        // 6. Publish AttendanceMarkedEvent (AFTER save, WITHIN transaction)
        eventPublisher.publishEvent(new AttendanceMarkedEvent(
                this,
                attendanceList,
                sessionId,
                teacherId
        ));

        log.info("Successfully marked {} attendance records for session {} by teacher {}",
                attendanceList.size(), sessionId, teacherId);

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
    public AttendanceResponse updateAttendanceStatus(
            Long id,
            UpdateAttendanceStatusRequest request,
            Long teacherId
    ) {
        log.info("Updating attendance {} to status {} by teacher {}", id, request.getStatus(), teacherId);

        // 1. Find attendance record
        Attendance attendance = attendanceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("ATTENDANCE_NOT_FOUND", (Object) id));

        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(attendance.getEnrollmentId())
                .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND",
                        (Object) attendance.getEnrollmentId()));

        // 2. Get session to find classId
        ClassSession session = classSessionRepository.findByIdAndDeletedFalse(attendance.getSessionId())
                .orElseThrow(() -> new EntityNotFoundException("SESSION_NOT_FOUND", (Object) attendance.getSessionId()));

        // 3. Permission check: Verify teacher is MAIN_TEACHER.
        // GAP-1301/GAP-1299: ADMIN/OWNER bypass the per-class MAIN_TEACHER check (no numeric
        // reference id, no TeacherClass row) — mirrors AuthorizationBean.isAdmin(). The teacher
        // id is the token-derived reference id (X-User-Reference-Id), NOT a client header.
        if (!authz.isAdmin()) {
            TeacherClass teacherClass = teacherClassRepository
                    .findByTeacherIdAndClassId(teacherId, session.getClassId())
                    .orElseThrow(() -> new PermissionDeniedException("TEACHER_NOT_IN_CLASS"));

            if (teacherClass.getRole() != TeacherClassRole.MAIN_TEACHER) {
                log.warn("Teacher {} is not MAIN_TEACHER in class {} (role: {})",
                        teacherId, session.getClassId(), teacherClass.getRole());
                throw new PermissionDeniedException("ONLY_MAIN_TEACHER_CAN_UPDATE_ATTENDANCE");
            }
        }

        // 4. Update status and notes
        AttendanceStatus oldStatus = attendance.getStatus();
        attendance.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            attendance.setNotes(request.getNotes());
        }
        attendance.setMarkedBy(teacherId);

        // Recalculate points (done in @PreUpdate)
        Attendance updatedAttendance = attendanceRepository.save(attendance);

        // Update points in gamification system if status changed
        if (oldStatus != request.getStatus()) {
            String pointDescription = String.format("Attendance updated: %s for session %d",
                    updatedAttendance.getStatus(),
                    updatedAttendance.getSessionId());

            pointService.updateAttendancePoints(
                    enrollment.getStudentId(),
                    updatedAttendance.getId(),
                    updatedAttendance.getPointsAwarded(),
                    pointDescription
            );

            log.info("Updated attendance {} from status {} to {}",
                    id, oldStatus, request.getStatus());
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
                ? ((presentCount + lateCount) * 100.0 / totalSessions)  // BR-ATT-008 (GAP-994): rate = (PRESENT + LATE) / total
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
                ? ((presentCount + lateCount) * 100.0 / totalSessions)  // BR-ATT-008 (GAP-994): rate = (PRESENT + LATE) / total
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
