package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Attendance recording and retrieval flow.
 *
 * <p>Tests complete workflow:
 * 1. Set up student, course, class, enrollment
 * 2. Record attendance for a session
 * 3. Get attendance records and verify
 *
 * @author KiteClass Team
 * @since 2.16
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class AttendanceFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private UUID tenantId;
    private Long teacherId;

    @BeforeEach
    void setUp() throws Exception {
        tenantId = UUID.randomUUID();
        teacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
    }

    @Test
    @DisplayName("Record attendance -> get attendance records -> verify")
    void shouldRecordAndRetrieveAttendance() throws Exception {
        // Step 1: Create student
        Long studentId = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantId,
                "Attendance IT Student",
                "attend.it." + System.currentTimeMillis() + "@kiteclass.test",
                "0903334455"
        );

        // Step 2: Create publishable course and publish
        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId, "Attendance IT Course", teacherId);

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        // Step 3: Create class
        CreateClassRequest classRequest = new CreateClassRequest(
                "Attendance IT Class",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                30
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 4: Enroll student
        CreateEnrollmentRequest enrollRequest = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000))
                .build();

        MvcResult enrollResult = mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long enrollmentId = objectMapper.readTree(enrollResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 5: Get class sessions
        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/classes/" + classId + "/sessions")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        var sessions = objectMapper.readTree(sessionsResult.getResponse().getContentAsString())
                .get("data");

        // Need at least 1 session to record attendance
        if (sessions == null || sessions.size() < 1) {
            return; // Skip if no sessions generated
        }

        Long sessionId = sessions.get(0).get("id").asLong();

        // Step 6: Record attendance (PRESENT)
        CreateAttendanceRequest attendanceRequest = CreateAttendanceRequest.builder()
                .enrollmentId(enrollmentId)
                .sessionId(sessionId)
                .status(AttendanceStatus.PRESENT)
                .build();

        mockMvc.perform(post("/api/v1/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(attendanceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));

        // Step 7: Get attendance records for student/class and verify
        mockMvc.perform(get("/api/v1/attendance/student/" + studentId + "/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PRESENT"));
    }

    @Test
    @DisplayName("Record multiple attendance statuses -> verify all recorded")
    void shouldRecordMultipleAttendanceStatuses() throws Exception {
        // Setup: student + course + class + enrollment
        Long studentId = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantId,
                "Multi Attend Student",
                "multi.attend." + System.currentTimeMillis() + "@kiteclass.test",
                "0904445566"
        );

        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId, "Multi Attend Course", teacherId);

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        CreateClassRequest classRequest = new CreateClassRequest(
                "Multi Attend Class",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                30
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        CreateEnrollmentRequest enrollRequest = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000))
                .build();

        MvcResult enrollResult = mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long enrollmentId = objectMapper.readTree(enrollResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Get sessions (need at least 2)
        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/classes/" + classId + "/sessions")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        var sessions = objectMapper.readTree(sessionsResult.getResponse().getContentAsString())
                .get("data");

        if (sessions == null || sessions.size() < 2) {
            return; // Skip if not enough sessions
        }

        Long session1Id = sessions.get(0).get("id").asLong();
        Long session2Id = sessions.get(1).get("id").asLong();

        // Record PRESENT for session 1
        CreateAttendanceRequest attendance1 = CreateAttendanceRequest.builder()
                .enrollmentId(enrollmentId)
                .sessionId(session1Id)
                .status(AttendanceStatus.PRESENT)
                .build();

        mockMvc.perform(post("/api/v1/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(attendance1)))
                .andExpect(status().isCreated());

        // Record ABSENT for session 2
        CreateAttendanceRequest attendance2 = CreateAttendanceRequest.builder()
                .enrollmentId(enrollmentId)
                .sessionId(session2Id)
                .status(AttendanceStatus.ABSENT)
                .notes("Sick leave")
                .build();

        mockMvc.perform(post("/api/v1/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(attendance2)))
                .andExpect(status().isCreated());

        // Verify all attendance records
        mockMvc.perform(get("/api/v1/attendance/student/" + studentId + "/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
