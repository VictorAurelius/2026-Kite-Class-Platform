package com.kiteclass.core.testutil;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.SessionStatus;
import com.kiteclass.core.module.clazz.dto.*;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.entity.ClassSession;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Test data builder for Class module tests.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public final class ClassTestDataBuilder {

    private ClassTestDataBuilder() {}

    /** Default tenant UUID for tests. */
    public static final UUID DEFAULT_TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");

    /** Default course ID for tests. */
    public static final Long DEFAULT_COURSE_ID = 1L;

    public static Class createDefaultClass() {
        Class clazz = new Class();
        clazz.setId(1L);
        clazz.setCourseId(DEFAULT_COURSE_ID);
        clazz.setName("English B1 - Evening Class");
        clazz.setDescription("Evening class for working professionals");
        clazz.setSchedule("Mon-Wed-Fri 18:00-20:00");
        clazz.setLocationType(Class.LocationType.IN_PERSON);
        clazz.setLocationDetail("Room 101");
        clazz.setStartDate(LocalDate.of(2026, 3, 1));
        clazz.setEndDate(LocalDate.of(2026, 5, 31));
        clazz.setMaxStudents(20);
        clazz.setCurrentEnrolled(0);
        clazz.setStatus(ClassStatus.SCHEDULED);
        clazz.setInstanceId(DEFAULT_TENANT);
        clazz.setDeleted(false);
        clazz.setVersion(0L);
        clazz.setCreatedAt(Instant.now());
        return clazz;
    }

    public static Class createClassWithStatus(ClassStatus status) {
        Class clazz = createDefaultClass();
        clazz.setStatus(status);
        if (status == ClassStatus.IN_PROGRESS) {
            clazz.setStartedAt(Instant.now());
        } else if (status == ClassStatus.COMPLETED) {
            clazz.setStartedAt(Instant.now().minusSeconds(86400));
            clazz.setCompletedAt(Instant.now());
        } else if (status == ClassStatus.CANCELLED) {
            clazz.setCancelledAt(Instant.now());
        }
        return clazz;
    }

    public static Class createClassWithEnrolledStudents(int enrolled) {
        Class clazz = createDefaultClass();
        clazz.setCurrentEnrolled(enrolled);
        return clazz;
    }

    public static CreateClassRequest createDefaultCreateRequest() {
        return new CreateClassRequest(
                "English B1 - Evening Class",
                "Evening class for working professionals",
                "Mon-Wed-Fri 18:00-20:00",
                Class.LocationType.IN_PERSON,
                "Room 101",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 5, 31),
                20
        );
    }

    public static CreateClassRequest createMinimalCreateRequest() {
        return new CreateClassRequest(
                "Basic Class",
                null,
                null,
                null,
                null,
                null,
                null,
                10
        );
    }

    public static UpdateClassRequest createDefaultUpdateRequest() {
        return new UpdateClassRequest(
                null,
                "Updated description",
                null,
                null,
                "Room 201 (updated)",
                null,
                null,
                null
        );
    }

    public static GenerateClassCodeRequest createCodeRequest() {
        return new GenerateClassCodeRequest(null, null);
    }

    public static GenerateClassCodeRequest createCustomCodeRequest(String code) {
        return new GenerateClassCodeRequest(code, null);
    }

    public static CancelClassRequest createCancelRequest() {
        return new CancelClassRequest("Không đủ học sinh đăng ký");
    }

    public static CreateScheduleRequest createScheduleRequest() {
        return new CreateScheduleRequest(
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0)
        );
    }

    public static ClassSession createDefaultSession(Long classId, int sessionNumber, LocalDate date) {
        ClassSession session = new ClassSession();
        session.setId((long) sessionNumber);
        session.setClassId(classId);
        session.setSessionNumber(sessionNumber);
        session.setSessionDate(date);
        session.setStartTime(LocalTime.of(18, 0));
        session.setEndTime(LocalTime.of(20, 0));
        session.setStatus(SessionStatus.SCHEDULED);
        session.setAttendanceTaken(false);
        session.setInstanceId(DEFAULT_TENANT);
        session.setDeleted(false);
        session.setCreatedAt(Instant.now());
        return session;
    }
}
