package com.kiteclass.core.module.gamification.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.gamification.entity.StudentPoint;
import com.kiteclass.core.module.gamification.repository.StudentPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementation of PointService for managing student points.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointServiceImpl implements PointService {

    private static final String ATTENDANCE_REFERENCE_TYPE = "ATTENDANCE";

    private final StudentPointRepository studentPointRepository;

    @Override
    @Transactional
    public void awardAttendancePoints(Long studentId, Long attendanceId, Integer points, String description) {
        log.debug("Awarding {} points to student {} for attendance {}", points, studentId, attendanceId);

        StudentPoint studentPoint = StudentPoint.builder()
                .instanceId(TenantContext.getCurrentTenant())
                .studentId(studentId)
                .points(points)
                .referenceType(ATTENDANCE_REFERENCE_TYPE)
                .referenceId(attendanceId)
                .description(description)
                .earnedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        studentPointRepository.save(studentPoint);

        log.info("Awarded {} points to student {} for attendance {}", points, studentId, attendanceId);
    }

    @Override
    @Transactional
    public void updateAttendancePoints(Long studentId, Long attendanceId, Integer newPoints, String description) {
        log.debug("Updating attendance points for student {} attendance {} to {}", studentId, attendanceId, newPoints);

        // Find and delete old point record
        studentPointRepository.findByReferenceTypeAndReferenceId(ATTENDANCE_REFERENCE_TYPE, attendanceId)
                .ifPresent(oldPoint -> {
                    log.debug("Deleting old point record: {} points", oldPoint.getPoints());
                    studentPointRepository.delete(oldPoint);
                });

        // Create new point record
        awardAttendancePoints(studentId, attendanceId, newPoints, description);

        log.info("Updated attendance points for student {} attendance {} to {}", studentId, attendanceId, newPoints);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPoints(Long studentId) {
        return studentPointRepository.getTotalPointsByStudentId(studentId);
    }
}
