package com.kiteclass.core.module.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.AssignmentStatus;
import com.kiteclass.core.common.constant.SubmissionStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.assignment.dto.request.CreateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.request.GradeSubmissionRequest;
import com.kiteclass.core.module.assignment.dto.request.SubmitAssignmentRequest;
import com.kiteclass.core.module.assignment.entity.Assignment;
import com.kiteclass.core.module.assignment.entity.Submission;
import com.kiteclass.core.module.assignment.repository.AssignmentRepository;
import com.kiteclass.core.module.assignment.repository.SubmissionRepository;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import com.kiteclass.core.common.constant.TeacherClassRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Assignment module.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("Assignment Integration Tests")
class AssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    /**
     * Used to force flush of pending UPDATEs (e.g., testAssignment.setStatus / setDueDate)
     * before mockMvc.perform triggers {@code TestTenantContextFilter.clear()} which would
     * otherwise discard them under the GAP-746 cross-tenant detach path. Without this,
     * controller reads stale entity state from DB (status=DRAFT or original future dueDate).
     */
    @PersistenceContext
    private EntityManager entityManager;

    private Course testCourse;
    private Class testClass;
    private Student testStudent;
    private Assignment testAssignment;
    private final UUID tenantId = UUID.randomUUID();
    private final Long mainTeacherId = 100L;

    @BeforeEach
    void setUp() {
        // Set tenant context
        TenantContext.setCurrentTenant(tenantId);

        // Create test course first
        testCourse = Course.builder()
                .name("Math Course")
                .code("MATH-COURSE-001")
                .description("Mathematics")
                .price(BigDecimal.valueOf(1000))
                .teacherId(mainTeacherId)
                .build();
        testCourse.setInstanceId(tenantId);
        testCourse = courseRepository.save(testCourse);

        // Create test class
        testClass = Class.builder()
                .courseId(testCourse.getId())
                .name("Math 101")
                .classCode("MATH-101")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build();
        testClass.setInstanceId(tenantId);
        testClass = classRepository.save(testClass);

        // Create teacher-class relationship
        TeacherClass teacherClass = TeacherClass.builder()
                .teacherId(mainTeacherId)
                .classId(testClass.getId())
                .role(TeacherClassRole.MAIN_TEACHER)
                .build();
        teacherClassRepository.save(teacherClass);

        // Create test student
        testStudent = Student.builder()
                .name("John Doe")
                .dateOfBirth(LocalDate.of(2005, 1, 1))
                .email("john.doe@example.com")
                .phone("0123456789")
                .build();
        testStudent.setInstanceId(tenantId);
        testStudent = studentRepository.save(testStudent);

        // Create test assignment
        testAssignment = Assignment.builder()
                .classId(testClass.getId())
                .title("Homework 1")
                .description("Math homework")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(20))
                .allowLateSubmission(true)
                .latePenaltyPercent(BigDecimal.valueOf(10))
                .status(AssignmentStatus.DRAFT)
                .build();
        testAssignment.setInstanceId(tenantId);
        testAssignment = assignmentRepository.save(testAssignment);
    }

    // ==================== Teacher - Create Assignment ====================

    @Test
    @DisplayName("Teacher - Create assignment should succeed")
    void createAssignment_shouldSucceed() throws Exception {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .classId(testClass.getId())
                .title("Homework 2")
                .description("Chapter 2 exercises")
                .dueDate(LocalDateTime.now().plusDays(14))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(15))
                .allowLateSubmission(true)
                .latePenaltyPercent(BigDecimal.valueOf(10))
                .build();

        mockMvc.perform(post("/api/v1/assignments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", mainTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Homework 2"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("Teacher - Create assignment should return 403 for non-main teacher")
    void createAssignment_shouldReturn403_forNonMainTeacher() throws Exception {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .classId(testClass.getId())
                .title("Homework 2")
                .dueDate(LocalDateTime.now().plusDays(14))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(15))
                .allowLateSubmission(true)
                .build();

        mockMvc.perform(post("/api/v1/assignments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== Teacher - Publish Assignment ====================

    @Test
    @DisplayName("Teacher - Publish assignment should succeed")
    void publishAssignment_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/assignments/{id}/publish", testAssignment.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", mainTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    // ==================== Student - Submit Assignment ====================

    @Test
    @DisplayName("Student - Submit assignment should succeed when published")
    void submitAssignment_shouldSucceed_whenPublished() throws Exception {
        // Publish assignment first
        testAssignment.setStatus(AssignmentStatus.PUBLISHED);
        assignmentRepository.save(testAssignment);
        entityManager.flush(); // GAP-746 fix side-effect: TestTenantContextFilter.clear() on cross-tenant would discard this UPDATE; flush before mockMvc

        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .assignmentId(testAssignment.getId())
                .contentUrl("https://s3.amazonaws.com/submission.pdf")
                .notes("My submission")
                .build();

        mockMvc.perform(post("/api/v1/assignments/submit")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", testStudent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignmentId").value(testAssignment.getId()))
                .andExpect(jsonPath("$.data.studentId").value(testStudent.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("Student - Submit assignment should return 400 when already submitted")
    void submitAssignment_shouldReturn400_whenAlreadySubmitted() throws Exception {
        // Publish assignment and create existing submission
        testAssignment.setStatus(AssignmentStatus.PUBLISHED);
        assignmentRepository.save(testAssignment);

        Submission existingSubmission = Submission.builder()
                .assignmentId(testAssignment.getId())
                .studentId(testStudent.getId())
                .submissionDate(LocalDateTime.now())
                .status(SubmissionStatus.PENDING)
                .build();
        existingSubmission.setInstanceId(tenantId);
        submissionRepository.save(existingSubmission);

        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .assignmentId(testAssignment.getId())
                .contentUrl("https://s3.amazonaws.com/submission2.pdf")
                .build();

        mockMvc.perform(post("/api/v1/assignments/submit")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", testStudent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== Teacher - Grade Submission ====================

    @Test
    @DisplayName("Teacher - Grade submission should succeed")
    void gradeSubmission_shouldSucceed() throws Exception {
        // Create submission
        Submission submission = Submission.builder()
                .assignmentId(testAssignment.getId())
                .studentId(testStudent.getId())
                .submissionDate(LocalDateTime.now())
                .status(SubmissionStatus.PENDING)
                .build();
        submission.setInstanceId(tenantId);
        submission = submissionRepository.save(submission);

        GradeSubmissionRequest request = GradeSubmissionRequest.builder()
                .score(BigDecimal.valueOf(85))
                .feedback("Good work!")
                .build();

        mockMvc.perform(post("/api/v1/assignments/submissions/{id}/grade", submission.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", mainTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("GRADED"))
                .andExpect(jsonPath("$.data.score").value(85))
                .andExpect(jsonPath("$.data.feedback").value("Good work!"));
    }

    @Test
    @DisplayName("Teacher - Grade submission with late penalty should apply penalty")
    void gradeSubmission_shouldApplyLatePenalty() throws Exception {
        // Set assignment overdue
        testAssignment.setDueDate(LocalDateTime.now().minusDays(2));
        assignmentRepository.save(testAssignment);

        // Create late submission
        Submission submission = Submission.builder()
                .assignmentId(testAssignment.getId())
                .studentId(testStudent.getId())
                .submissionDate(LocalDateTime.now()) // 2 days late
                .status(SubmissionStatus.PENDING)
                .build();
        submission.setInstanceId(tenantId);
        submission = submissionRepository.save(submission);
        entityManager.flush(); // GAP-746 fix side-effect: flush testAssignment.dueDate UPDATE + submission INSERT before mockMvc clear()

        GradeSubmissionRequest request = GradeSubmissionRequest.builder()
                .score(BigDecimal.valueOf(100))
                .feedback("Good but late")
                .build();

        mockMvc.perform(post("/api/v1/assignments/submissions/{id}/grade", submission.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", mainTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.adjustedScore").value(lessThan(100.0))); // Should have penalty
    }

    // ==================== Get Methods ====================

    @Test
    @DisplayName("Get assignments by class should return all assignments")
    void getAssignmentsByClass_shouldReturnAllAssignments() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/class/{classId}", testClass.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[*].title", hasItem("Homework 1")));
    }

    @Test
    @DisplayName("Get published assignments should return only published")
    void getPublishedAssignments_shouldReturnOnlyPublished() throws Exception {
        // Create another published assignment
        Assignment publishedAssignment = Assignment.builder()
                .classId(testClass.getId())
                .title("Homework 2")
                .dueDate(LocalDateTime.now().plusDays(14))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(15))
                .allowLateSubmission(true)
                .latePenaltyPercent(BigDecimal.valueOf(10))
                .status(AssignmentStatus.PUBLISHED)
                .build();
        publishedAssignment.setInstanceId(tenantId);
        assignmentRepository.save(publishedAssignment);

        mockMvc.perform(get("/api/v1/assignments/class/{classId}/published", testClass.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1))) // Only published one
                .andExpect(jsonPath("$.data[0].title").value("Homework 2"))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"));
    }

    // ==================== Delete Assignment ====================

    @Test
    @DisplayName("Teacher - Delete assignment should return 400 when has submissions")
    void deleteAssignment_shouldReturn400_whenHasSubmissions() throws Exception {
        // Create submission
        Submission submission = Submission.builder()
                .assignmentId(testAssignment.getId())
                .studentId(testStudent.getId())
                .submissionDate(LocalDateTime.now())
                .status(SubmissionStatus.PENDING)
                .build();
        submission.setInstanceId(tenantId);
        submissionRepository.save(submission);

        mockMvc.perform(delete("/api/v1/assignments/{id}", testAssignment.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", mainTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Teacher - Delete assignment should succeed when no submissions")
    void deleteAssignment_shouldSucceed_whenNoSubmissions() throws Exception {
        mockMvc.perform(delete("/api/v1/assignments/{id}", testAssignment.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", mainTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true));
    }
}
