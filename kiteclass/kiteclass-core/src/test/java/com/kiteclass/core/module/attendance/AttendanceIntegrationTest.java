package com.kiteclass.core.module.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.constant.SessionStatus;
import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.UpdateAttendanceStatusRequest;
import com.kiteclass.core.module.attendance.repository.AttendanceRepository;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.teacher.entity.Teacher;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import com.kiteclass.core.module.teacher.repository.TeacherRepository;
import com.kiteclass.core.testutil.AttendanceTestDataBuilder;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Attendance module.
 *
 * <p>Tests the full stack: Controller → Service → Repository → Database.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class AttendanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private TeacherClassRepository teacherClassRepository;
    @Autowired
    private ClassSessionRepository classSessionRepository;

    private Student savedStudent;
    private Class savedClass;
    private Enrollment savedEnrollment;
    private Teacher savedTeacher;
    private final UUID tenantId = AttendanceTestDataBuilder.DEFAULT_TENANT;
    // GAP-992: single-mark now loads the session via classSessionRepository — the
    // session row MUST exist and be SCHEDULED. Capture the real generated ids of
    // two SCHEDULED sessions created in @BeforeEach (no longer a hardcoded 1L/2L).
    private Long sessionId;  // first SCHEDULED session id
    private Long sessionId2; // second SCHEDULED session id (for multi-session stats)

    @BeforeEach
    void setUp() {
        // Set TenantContext for EntityPersistenceListener
        TenantContext.setCurrentTenant(tenantId);
        try {
            // Create course
            Course course = CourseTestDataBuilder.createDefaultCourse();
            course.setId(null);
            course.setTeacherId(null);
            course.setStatus(CourseStatus.PUBLISHED);
            Course savedCourse = courseRepository.save(course);

            // Create class
            Class clazz = Class.builder()
                    .courseId(savedCourse.getId())
                    .name("Test Class")
                    .classCode("TC-001")
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusDays(90))
                    .maxStudents(10)
                    .currentEnrolled(0)
                    .status(ClassStatus.SCHEDULED)
                    .build();
            savedClass = classRepository.save(clazz);

            // Create student
            Student student = StudentTestDataBuilder.createDefaultStudent();
            student.setId(null);
            student.setEmail("student1@example.com");
            student.setStatus(StudentStatus.ACTIVE);
            savedStudent = studentRepository.save(student);

            // Create teacher
            Teacher teacher = Teacher.builder()
                    .name("Test Teacher")
                    .email("teacher@example.com")
                    .phoneNumber("1234567890")
                    .specialization("Test")
                    .status(TeacherStatus.ACTIVE)
                    .build();
            savedTeacher = teacherRepository.save(teacher);

            // Create teacher-class assignment (MAIN_TEACHER)
            TeacherClass teacherClass = TeacherClass.builder()
                    .teacherId(savedTeacher.getId())
                    .classId(savedClass.getId())
                    .role(TeacherClassRole.MAIN_TEACHER)
                    .build();
            teacherClassRepository.save(teacherClass);

            // GAP-992: single-mark attendance now requires the ClassSession row to
            // EXIST and be SCHEDULED. Create two SCHEDULED sessions and capture their
            // real generated ids (sequence-assigned, not necessarily 1L/2L).
            ClassSession session1 = ClassSession.builder()
                    .classId(savedClass.getId())
                    .sessionNumber(1)
                    .sessionDate(LocalDate.now())
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(11, 0))
                    .status(SessionStatus.SCHEDULED)
                    .attendanceTaken(false)
                    .build();
            sessionId = classSessionRepository.save(session1).getId();

            ClassSession session2 = ClassSession.builder()
                    .classId(savedClass.getId())
                    .sessionNumber(2)
                    .sessionDate(LocalDate.now().plusDays(1))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(11, 0))
                    .status(SessionStatus.SCHEDULED)
                    .attendanceTaken(false)
                    .build();
            sessionId2 = classSessionRepository.save(session2).getId();

            // Create enrollment
            Enrollment enrollment = Enrollment.builder()
                    .studentId(savedStudent.getId())
                    .classId(savedClass.getId())
                    .status(EnrollmentStatus.ACTIVE)
                    .tuitionAmount(new BigDecimal("1000.00"))
                    .discountPercent(BigDecimal.ZERO)
                    .finalAmount(new BigDecimal("1000.00"))
                    .build();
            savedEnrollment = enrollmentRepository.save(enrollment);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // Mark attendance - Success cases
    // =========================================================================

    @Test
    void markAttendance_shouldReturn201_whenValidRequest() throws Exception {
        CreateAttendanceRequest request = AttendanceTestDataBuilder.createRequestForEnrollmentAndSession(
                savedEnrollment.getId(),
                sessionId
        );

        mockMvc.perform(post("/api/v1/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").value(savedEnrollment.getId()))
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.status").value("PRESENT"))
                .andExpect(jsonPath("$.pointsAwarded").value(0));

        // Verify in database
        boolean exists = attendanceRepository.existsByEnrollmentIdAndSessionIdAndDeletedFalse(
                savedEnrollment.getId(), sessionId
        );
        assertThat(exists).isTrue();
    }

    @Test
    void markAttendance_shouldDeductPoints_whenAbsent() throws Exception {
        CreateAttendanceRequest request = AttendanceTestDataBuilder.createRequestWithStatus(
                savedEnrollment.getId(),
                sessionId,
                AttendanceStatus.ABSENT
        );

        mockMvc.perform(post("/api/v1/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABSENT"))
                .andExpect(jsonPath("$.pointsAwarded").value(-10));
    }

    // =========================================================================
    // Mark attendance - Error cases
    // =========================================================================

    @Test
    void markAttendance_shouldReturn404_whenEnrollmentNotFound() throws Exception {
        CreateAttendanceRequest request = AttendanceTestDataBuilder.createRequestForEnrollmentAndSession(
                99999L, // Non-existent enrollment
                sessionId
        );

        mockMvc.perform(post("/api/v1/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAttendance_shouldReturn400_whenDuplicateAttendance() throws Exception {
        // First attendance
        CreateAttendanceRequest request = AttendanceTestDataBuilder.createRequestForEnrollmentAndSession(
                savedEnrollment.getId(),
                sessionId
        );

        mockMvc.perform(post("/api/v1/attendance")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate attendance
        mockMvc.perform(post("/api/v1/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Get attendance
    // =========================================================================

    @Test
    void getAttendanceByEnrollment_shouldReturnPage() throws Exception {
        // Create attendance first
        CreateAttendanceRequest request = AttendanceTestDataBuilder.createRequestForEnrollmentAndSession(
                savedEnrollment.getId(),
                sessionId
        );
        mockMvc.perform(post("/api/v1/attendance")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get attendance
        mockMvc.perform(get("/api/v1/attendance/enrollment/" + savedEnrollment.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].enrollmentId").value(savedEnrollment.getId()));
    }

    // =========================================================================
    // Update attendance
    // =========================================================================

    @Test
    void updateAttendanceStatus_shouldUpdateSuccessfully() throws Exception {
        // Create attendance
        CreateAttendanceRequest createRequest = AttendanceTestDataBuilder.createRequestForEnrollmentAndSession(
                savedEnrollment.getId(),
                sessionId
        );
        String createResponse = mockMvc.perform(post("/api/v1/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long attendanceId = objectMapper.readTree(createResponse).get("id").asLong();

        // Update status
        UpdateAttendanceStatusRequest updateRequest =
                AttendanceTestDataBuilder.createUpdateStatusRequest(AttendanceStatus.EXCUSED);

        mockMvc.perform(patch("/api/v1/attendance/" + attendanceId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-Teacher-Id", savedTeacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCUSED"))
                .andExpect(jsonPath("$.pointsAwarded").value(0));
    }

    // =========================================================================
    // Delete attendance
    // =========================================================================

    @Test
    void deleteAttendance_shouldSoftDelete() throws Exception {
        // Create attendance
        CreateAttendanceRequest createRequest = AttendanceTestDataBuilder.createRequestForEnrollmentAndSession(
                savedEnrollment.getId(),
                sessionId
        );
        String createResponse = mockMvc.perform(post("/api/v1/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long attendanceId = objectMapper.readTree(createResponse).get("id").asLong();

        // Delete attendance
        mockMvc.perform(delete("/api/v1/attendance/" + attendanceId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());

        // Verify soft delete
        boolean exists = attendanceRepository.existsByEnrollmentIdAndSessionIdAndDeletedFalse(
                savedEnrollment.getId(), sessionId
        );
        assertThat(exists).isFalse();
    }

    // =========================================================================
    // Statistics
    // =========================================================================

    @Test
    void getStudentStats_shouldCalculateCorrectly() throws Exception {
        // Create multiple attendance records
        CreateAttendanceRequest present = AttendanceTestDataBuilder.createRequestWithStatus(
                savedEnrollment.getId(), sessionId, AttendanceStatus.PRESENT
        );
        CreateAttendanceRequest absent = AttendanceTestDataBuilder.createRequestWithStatus(
                savedEnrollment.getId(), sessionId2, AttendanceStatus.ABSENT
        );

        mockMvc.perform(post("/api/v1/attendance")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(present)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(absent)))
                .andExpect(status().isCreated());

        // Get statistics
        mockMvc.perform(get("/api/v1/attendance/stats/student/" + savedStudent.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetType").value("STUDENT"))
                .andExpect(jsonPath("$.totalSessions").value(2))
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.absentCount").value(1))
                .andExpect(jsonPath("$.attendanceRate").value(50.0));
    }
}
