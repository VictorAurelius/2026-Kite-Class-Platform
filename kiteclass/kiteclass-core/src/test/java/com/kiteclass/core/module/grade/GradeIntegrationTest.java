package com.kiteclass.core.module.grade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.GradeComponentType;
import com.kiteclass.core.common.constant.GradeStatus;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.grade.dto.request.CreateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.request.FinalizeGradeRequest;
import com.kiteclass.core.module.grade.entity.Grade;
import com.kiteclass.core.module.grade.entity.GradingScale;
import com.kiteclass.core.module.grade.repository.GradeRepository;
import com.kiteclass.core.module.grade.repository.GradingScaleRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Grade module.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("Grade Integration Tests")
class GradeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private GradingScaleRepository gradingScaleRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    private Course testCourse;
    private Class testClass;
    private Student testStudent;
    private Grade testGrade;
    private GradingScale testGradingScale;
    private final UUID tenantId = UUID.randomUUID();
    private final Long mainTeacherId = 100L;

    @BeforeEach
    void setUp() {
        // Set tenant context
        TenantContext.setCurrentTenant(tenantId);

        // Create test course
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
                .email("john.doe@example.com")
                .phone("0123456789")
                .dateOfBirth(LocalDate.of(2005, 1, 1))
                .build();
        testStudent.setInstanceId(tenantId);
        testStudent = studentRepository.save(testStudent);

        // Create test grade
        testGrade = Grade.builder()
                .studentId(testStudent.getId())
                .classId(testClass.getId())
                .status(GradeStatus.IN_PROGRESS)
                .passThreshold(BigDecimal.valueOf(50.0))
                .build();
        testGrade.setInstanceId(tenantId);
        testGrade = gradeRepository.save(testGrade);

        // Create grading scale
        testGradingScale = GradingScale.builder()
                .scaleName("Standard")
                .letterGrade("B+")
                .gpaValue(BigDecimal.valueOf(3.3))
                .minScore(BigDecimal.valueOf(87.00))
                .maxScore(BigDecimal.valueOf(89.99))
                .build();
        testGradingScale.setInstanceId(tenantId);
        gradingScaleRepository.save(testGradingScale);
    }

    // ==================== Initialize Grade Tests ====================

    @Test
    @DisplayName("Should initialize grade via API")
    void shouldInitializeGrade() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/grades/initialize")
                        .param("studentId", testStudent.getId().toString())
                        .param("classId", testClass.getId().toString())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.studentId", is(testStudent.getId().intValue())))
                .andExpect(jsonPath("$.data.classId", is(testClass.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));
    }

    // ==================== Get Grade Tests ====================

    @Test
    @DisplayName("Should get grade by ID")
    void shouldGetGradeById() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/grades/{id}", testGrade.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(testGrade.getId().intValue())))
                .andExpect(jsonPath("$.data.studentId", is(testStudent.getId().intValue())));
    }

    @Test
    @DisplayName("Should get grade by student and class")
    void shouldGetGradeByStudentAndClass() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/grades/student/{studentId}/class/{classId}",
                        testStudent.getId(), testClass.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.studentId", is(testStudent.getId().intValue())))
                .andExpect(jsonPath("$.data.classId", is(testClass.getId().intValue())));
    }

    // ==================== Add Component Tests ====================

    @Test
    @DisplayName("Should add component to grade")
    void shouldAddComponent() throws Exception {
        // Arrange
        CreateGradeComponentRequest request = CreateGradeComponentRequest.builder()
                .gradeId(testGrade.getId())
                .componentType(GradeComponentType.MIDTERM)
                .componentName("Midterm Exam")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(30))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/grades/components")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.componentName", is("Midterm Exam")))
                .andExpect(jsonPath("$.data.score", is(85)))
                .andExpect(jsonPath("$.data.weightPercent", is(30)))
                .andExpect(jsonPath("$.data.weightedScore", notNullValue()));
    }

    // ==================== Calculate Final Score Tests ====================

    @Test
    @DisplayName("Should calculate final score")
    void shouldCalculateFinalScore() throws Exception {
        // Arrange - Add components
        CreateGradeComponentRequest midterm = CreateGradeComponentRequest.builder()
                .gradeId(testGrade.getId())
                .componentType(GradeComponentType.MIDTERM)
                .componentName("Midterm")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(40))
                .build();

        CreateGradeComponentRequest finalExam = CreateGradeComponentRequest.builder()
                .gradeId(testGrade.getId())
                .componentType(GradeComponentType.FINAL)
                .componentName("Final")
                .score(BigDecimal.valueOf(90))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(60))
                .build();

        mockMvc.perform(post("/api/v1/grades/components")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(midterm)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/grades/components")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalExam)))
                .andExpect(status().isCreated());

        // Act & Assert - Calculate
        mockMvc.perform(post("/api/v1/grades/{id}/calculate", testGrade.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.finalScore", is(88.0))) // 85*0.4 + 90*0.6 = 34 + 54 = 88
                .andExpect(jsonPath("$.data.letterGrade", is("B+")))
                .andExpect(jsonPath("$.data.gpa", is(3.3)));
    }

    // ==================== Finalize Grade Tests ====================

    @Test
    @DisplayName("Should finalize grade when weights sum to 100")
    void shouldFinalizeGrade() throws Exception {
        // Arrange - Add components with weights = 100%
        CreateGradeComponentRequest midterm = CreateGradeComponentRequest.builder()
                .gradeId(testGrade.getId())
                .componentType(GradeComponentType.MIDTERM)
                .componentName("Midterm")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(50))
                .build();

        CreateGradeComponentRequest finalExam = CreateGradeComponentRequest.builder()
                .gradeId(testGrade.getId())
                .componentType(GradeComponentType.FINAL)
                .componentName("Final")
                .score(BigDecimal.valueOf(90))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(50))
                .build();

        mockMvc.perform(post("/api/v1/grades/components")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(midterm)));

        mockMvc.perform(post("/api/v1/grades/components")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finalExam)));

        FinalizeGradeRequest finalizeRequest = FinalizeGradeRequest.builder()
                .teacherId(mainTeacherId)
                .comments("Good work!")
                .build();

        // Act & Assert
        // GAP-1000: finalize derives the acting teacher from the authenticated principal
        // (X-User-Reference-Id), not request.teacherId. Send AS the seeded MAIN_TEACHER.
        mockMvc.perform(post("/api/v1/grades/{id}/finalize", testGrade.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", mainTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalizeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PASSED")))
                .andExpect(jsonPath("$.data.finalScore", is(87.5))) // 85*0.5 + 90*0.5
                .andExpect(jsonPath("$.data.finalizedBy", is(mainTeacherId.intValue())))
                .andExpect(jsonPath("$.data.comments", is("Good work!")))
                .andExpect(jsonPath("$.data.isFinalized", is(true)));
    }

    @Test
    @DisplayName("Should fail to finalize when weights don't sum to 100")
    void shouldFailToFinalizeWithInvalidWeights() throws Exception {
        // Arrange - Add component with only 30% weight
        CreateGradeComponentRequest midterm = CreateGradeComponentRequest.builder()
                .gradeId(testGrade.getId())
                .componentType(GradeComponentType.MIDTERM)
                .componentName("Midterm")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(30))
                .build();

        mockMvc.perform(post("/api/v1/grades/components")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(midterm)));

        FinalizeGradeRequest finalizeRequest = FinalizeGradeRequest.builder()
                .teacherId(mainTeacherId)
                .build();

        // Act & Assert
        // GAP-1000: send AS the seeded MAIN_TEACHER so the request reaches weight validation
        // (400) instead of failing the permission check (403) first.
        mockMvc.perform(post("/api/v1/grades/{id}/finalize", testGrade.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", mainTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalizeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==================== Get Grades by Student/Class Tests ====================

    @Test
    @DisplayName("Should get all grades by student")
    void shouldGetGradesByStudent() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/grades/student/{studentId}", testStudent.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].studentId", is(testStudent.getId().intValue())));
    }

    @Test
    @DisplayName("Should get all grades by class")
    void shouldGetGradesByClass() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/grades/class/{classId}", testClass.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].classId", is(testClass.getId().intValue())));
    }

    // ==================== Class Statistics Tests ====================

    @Test
    @DisplayName("Should calculate class statistics")
    void shouldCalculateClassStatistics() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/grades/class/{classId}/statistics", testClass.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalStudents", is(1)))
                .andExpect(jsonPath("$.data.finalizedGrades", is(0)))
                .andExpect(jsonPath("$.data.passedStudents", is(0)))
                .andExpect(jsonPath("$.data.failedStudents", is(0)));
    }
}
