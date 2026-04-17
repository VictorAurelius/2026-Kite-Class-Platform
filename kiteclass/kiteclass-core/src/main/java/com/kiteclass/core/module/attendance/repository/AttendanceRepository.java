package com.kiteclass.core.module.attendance.repository;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.module.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Attendance} entity.
 *
 * <p>Provides database access methods for attendance management with
 * support for multi-tenancy and soft deletes.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * Find attendance by ID excluding deleted.
     * Respects soft delete.
     *
     * @param id attendance ID
     * @return optional attendance
     */
    Optional<Attendance> findByIdAndDeletedFalse(Long id);

    /**
     * Check if attendance exists for enrollment and session (excluding deleted).
     * Used to prevent duplicate attendance records.
     *
     * @param enrollmentId enrollment ID
     * @param sessionId session ID
     * @return true if exists, false otherwise
     */
    boolean existsByEnrollmentIdAndSessionIdAndDeletedFalse(Long enrollmentId, Long sessionId);

    /**
     * Find all attendance records for an enrollment (excluding deleted).
     * Supports pagination and sorting.
     *
     * @param enrollmentId enrollment ID
     * @param pageable pagination info
     * @return page of attendance records
     */
    Page<Attendance> findByEnrollmentIdAndDeletedFalse(Long enrollmentId, Pageable pageable);

    /**
     * Find all attendance records for a session (excluding deleted).
     * Supports pagination and sorting.
     *
     * @param sessionId session ID
     * @param pageable pagination info
     * @return page of attendance records
     */
    Page<Attendance> findBySessionIdAndDeletedFalse(Long sessionId, Pageable pageable);

    /**
     * Find all attendance records for a session ordered by enrollment ID (excluding deleted).
     * Used for bulk operations and roster display.
     *
     * @param sessionId session ID
     * @return list of attendance records
     */
    List<Attendance> findBySessionIdAndDeletedFalseOrderByEnrollmentIdAsc(Long sessionId);

    /**
     * Count attendance records for a session with specific status (excluding deleted).
     * Used for statistics and reporting.
     *
     * @param sessionId session ID
     * @param status attendance status
     * @return count of records
     */
    long countBySessionIdAndStatusAndDeletedFalse(Long sessionId, AttendanceStatus status);

    /**
     * Count attendance records for an enrollment with specific status (excluding deleted).
     * Used for student statistics.
     *
     * @param enrollmentId enrollment ID
     * @param status attendance status
     * @return count of records
     */
    long countByEnrollmentIdAndStatusAndDeletedFalse(Long enrollmentId, AttendanceStatus status);

    /**
     * Count total attendance records for an enrollment (excluding deleted).
     * Used to calculate total sessions attended.
     *
     * @param enrollmentId enrollment ID
     * @return total count
     */
    long countByEnrollmentIdAndDeletedFalse(Long enrollmentId);

    /**
     * Find all attendance records for multiple enrollments in a specific session.
     * Used for class-level statistics.
     *
     * @param sessionId session ID
     * @param enrollmentIds list of enrollment IDs
     * @return list of attendance records
     */
    @Query("SELECT a FROM Attendance a " +
           "WHERE a.sessionId = :sessionId " +
           "AND a.enrollmentId IN :enrollmentIds " +
           "AND a.deleted = false")
    List<Attendance> findBySessionIdAndEnrollmentIdIn(
            @Param("sessionId") Long sessionId,
            @Param("enrollmentIds") List<Long> enrollmentIds
    );

    /**
     * Get attendance statistics for an enrollment.
     * Returns counts grouped by status.
     *
     * @param enrollmentId enrollment ID
     * @return list of [status, count] pairs
     */
    @Query("SELECT a.status, COUNT(a) FROM Attendance a " +
           "WHERE a.enrollmentId = :enrollmentId " +
           "AND a.deleted = false " +
           "GROUP BY a.status")
    List<Object[]> getAttendanceStatsByEnrollment(@Param("enrollmentId") Long enrollmentId);

    /**
     * Find attendance record by enrollment and session (excluding deleted).
     *
     * @param enrollmentId enrollment ID
     * @param sessionId session ID
     * @return optional attendance record
     */
    Optional<Attendance> findByEnrollmentIdAndSessionIdAndDeletedFalse(
            Long enrollmentId,
            Long sessionId
    );
}
