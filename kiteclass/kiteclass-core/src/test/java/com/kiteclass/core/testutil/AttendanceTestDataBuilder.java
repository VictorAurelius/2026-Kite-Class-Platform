package com.kiteclass.core.testutil;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.module.attendance.dto.BulkAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.UpdateAttendanceStatusRequest;
import com.kiteclass.core.module.attendance.entity.Attendance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test data builder for Attendance-related objects.
 *
 * <p>Provides factory methods to create test data for:
 * <ul>
 *   <li>Attendance entities</li>
 *   <li>CreateAttendanceRequest DTOs</li>
 *   <li>BulkAttendanceRequest DTOs</li>
 *   <li>UpdateAttendanceStatusRequest DTOs</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
public class AttendanceTestDataBuilder {

    public static final UUID DEFAULT_TENANT = ClassTestDataBuilder.DEFAULT_TENANT;

    /**
     * Creates a default Attendance entity for testing.
     *
     * @return Attendance with default test data
     */
    public static Attendance createDefaultAttendance() {
        Attendance attendance = Attendance.builder()
                .enrollmentId(1L)
                .sessionId(1L)
                .status(AttendanceStatus.PRESENT)
                .markedDate(LocalDateTime.now())
                .markedBy(1L)
                .notes("Default test attendance")
                .pointsAwarded(0)
                .build();
        attendance.setId(1L);
        attendance.setInstanceId(DEFAULT_TENANT);
        attendance.setDeleted(false);
        return attendance;
    }

    /**
     * Creates an Attendance entity with custom enrollment and session IDs.
     *
     * @param enrollmentId the enrollment ID
     * @param sessionId the session ID
     * @return Attendance with specified IDs
     */
    public static Attendance createAttendance(Long enrollmentId, Long sessionId) {
        Attendance attendance = createDefaultAttendance();
        attendance.setEnrollmentId(enrollmentId);
        attendance.setSessionId(sessionId);
        return attendance;
    }

    /**
     * Creates an Attendance entity with custom status.
     *
     * @param enrollmentId the enrollment ID
     * @param sessionId the session ID
     * @param status the attendance status
     * @return Attendance with specified status
     */
    public static Attendance createAttendanceWithStatus(
            Long enrollmentId,
            Long sessionId,
            AttendanceStatus status) {
        Attendance attendance = createAttendance(enrollmentId, sessionId);
        attendance.setStatus(status);
        attendance.setPointsAwarded(status.getPointsDeduction());
        return attendance;
    }

    /**
     * Creates an ABSENT attendance entity.
     *
     * @param enrollmentId the enrollment ID
     * @param sessionId the session ID
     * @return Attendance with ABSENT status
     */
    public static Attendance createAbsentAttendance(Long enrollmentId, Long sessionId) {
        return createAttendanceWithStatus(enrollmentId, sessionId, AttendanceStatus.ABSENT);
    }

    /**
     * Creates a LATE attendance entity.
     *
     * @param enrollmentId the enrollment ID
     * @param sessionId the session ID
     * @return Attendance with LATE status
     */
    public static Attendance createLateAttendance(Long enrollmentId, Long sessionId) {
        return createAttendanceWithStatus(enrollmentId, sessionId, AttendanceStatus.LATE);
    }

    /**
     * Creates a default CreateAttendanceRequest for testing.
     *
     * @return CreateAttendanceRequest with default test data
     */
    public static CreateAttendanceRequest createDefaultCreateRequest() {
        return CreateAttendanceRequest.builder()
                .enrollmentId(1L)
                .sessionId(1L)
                .status(AttendanceStatus.PRESENT)
                .notes("Test attendance")
                .build();
    }

    /**
     * Creates a CreateAttendanceRequest with custom enrollment and session IDs.
     *
     * @param enrollmentId the enrollment ID
     * @param sessionId the session ID
     * @return CreateAttendanceRequest with specified IDs
     */
    public static CreateAttendanceRequest createRequestForEnrollmentAndSession(
            Long enrollmentId,
            Long sessionId) {
        return CreateAttendanceRequest.builder()
                .enrollmentId(enrollmentId)
                .sessionId(sessionId)
                .status(AttendanceStatus.PRESENT)
                .notes("Test attendance")
                .build();
    }

    /**
     * Creates a CreateAttendanceRequest with custom status.
     *
     * @param enrollmentId the enrollment ID
     * @param sessionId the session ID
     * @param status the attendance status
     * @return CreateAttendanceRequest with specified status
     */
    public static CreateAttendanceRequest createRequestWithStatus(
            Long enrollmentId,
            Long sessionId,
            AttendanceStatus status) {
        return CreateAttendanceRequest.builder()
                .enrollmentId(enrollmentId)
                .sessionId(sessionId)
                .status(status)
                .notes("Test " + status + " attendance")
                .build();
    }

    /**
     * Creates a default BulkAttendanceRequest for testing.
     *
     * @param sessionId the session ID
     * @param enrollmentIds list of enrollment IDs
     * @return BulkAttendanceRequest with default test data
     */
    public static BulkAttendanceRequest createBulkRequest(Long sessionId, List<Long> enrollmentIds) {
        List<BulkAttendanceRequest.AttendanceRecord> records = new ArrayList<>();
        for (Long enrollmentId : enrollmentIds) {
            records.add(BulkAttendanceRequest.AttendanceRecord.builder()
                    .enrollmentId(enrollmentId)
                    .status(AttendanceStatus.PRESENT)
                    .notes("Bulk attendance test")
                    .build());
        }

        return BulkAttendanceRequest.builder()
                .sessionId(sessionId)
                .records(records)
                .build();
    }

    /**
     * Creates a BulkAttendanceRequest with mixed statuses.
     *
     * @param sessionId the session ID
     * @param enrollmentIds list of enrollment IDs
     * @param statuses list of statuses (must match enrollmentIds length)
     * @return BulkAttendanceRequest with mixed statuses
     */
    public static BulkAttendanceRequest createBulkRequestWithStatuses(
            Long sessionId,
            List<Long> enrollmentIds,
            List<AttendanceStatus> statuses) {
        if (enrollmentIds.size() != statuses.size()) {
            throw new IllegalArgumentException("enrollmentIds and statuses must have same length");
        }

        List<BulkAttendanceRequest.AttendanceRecord> records = new ArrayList<>();
        for (int i = 0; i < enrollmentIds.size(); i++) {
            records.add(BulkAttendanceRequest.AttendanceRecord.builder()
                    .enrollmentId(enrollmentIds.get(i))
                    .status(statuses.get(i))
                    .notes("Bulk test " + statuses.get(i))
                    .build());
        }

        return BulkAttendanceRequest.builder()
                .sessionId(sessionId)
                .records(records)
                .build();
    }

    /**
     * Creates a default UpdateAttendanceStatusRequest for testing.
     *
     * @return UpdateAttendanceStatusRequest with default test data
     */
    public static UpdateAttendanceStatusRequest createDefaultUpdateStatusRequest() {
        return UpdateAttendanceStatusRequest.builder()
                .status(AttendanceStatus.EXCUSED)
                .notes("Status updated to EXCUSED")
                .build();
    }

    /**
     * Creates an UpdateAttendanceStatusRequest with custom status.
     *
     * @param status the attendance status
     * @return UpdateAttendanceStatusRequest with specified status
     */
    public static UpdateAttendanceStatusRequest createUpdateStatusRequest(AttendanceStatus status) {
        return UpdateAttendanceStatusRequest.builder()
                .status(status)
                .notes("Status updated to " + status)
                .build();
    }
}
