package com.kiteclass.core.module.enrollment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Concurrent integration tests verifying enrollment capacity enforcement.
 *
 * <p>Tests the {@code PESSIMISTIC_WRITE} (SELECT FOR UPDATE) approach on the
 * Class row: concurrent enrollers serialize at the DB level via an exclusive
 * row lock. The first {@code maxStudents} succeed; the rest get CLASS_FULL (400).
 * No optimistic retry needed — the serialized read-check-increment is atomic.
 *
 * <p>NOT annotated with {@code @Transactional} — concurrent HTTP requests
 * must commit independently so each transaction can observe the updated
 * {@code currentEnrolled} counter.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
class EnrollmentCapacityConcurrentTest {

    private static final int MAX_STUDENTS = 10;
    private static final int TOTAL_REQUESTS = 20;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private CourseRepository courseRepository;

    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private Class savedClass;
    private List<Student> savedStudents;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        try {
            // Create course
            Course course = CourseTestDataBuilder.createDefaultCourse();
            course.setId(null);
            course.setTeacherId(null);
            course.setStatus(CourseStatus.PUBLISHED);
            Course savedCourse = courseRepository.save(course);

            // Create class with capacity = MAX_STUDENTS
            Class clazz = Class.builder()
                    .courseId(savedCourse.getId())
                    .name("Capacity Test Class")
                    .classCode("CAPTEST-001")
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusDays(90))
                    .maxStudents(MAX_STUDENTS)
                    .currentEnrolled(0)
                    .status(ClassStatus.SCHEDULED)
                    .build();
            savedClass = classRepository.save(clazz);

            // Create TOTAL_REQUESTS distinct students (one per concurrent thread)
            savedStudents = new ArrayList<>();
            for (int i = 0; i < TOTAL_REQUESTS; i++) {
                Student student = StudentTestDataBuilder.createDefaultStudent();
                student.setId(null);
                student.setEmail("concurrent-student-" + i + "@example.com");
                student.setStatus(StudentStatus.ACTIVE);
                savedStudents.add(studentRepository.save(student));
            }
        } finally {
            TenantContext.clear();
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up in reverse FK order. No tenant context needed — deleteById bypasses Hibernate filter.
        if (savedClass != null) {
            enrollmentRepository.findAll().stream()
                    .filter(e -> savedClass.getId().equals(e.getClassId()))
                    .forEach(e -> enrollmentRepository.deleteById(e.getId()));
            classRepository.deleteById(savedClass.getId());
        }
        for (Student s : savedStudents) {
            studentRepository.deleteById(s.getId());
        }
    }

    /**
     * BR-ENROLL-001 (concurrent enforcement): 20 simultaneous enrollment requests
     * for a class with maxStudents=10 must result in exactly 10 successes (HTTP 201)
     * and 10 rejections (HTTP 400 CLASS_FULL). With PESSIMISTIC_WRITE serialization,
     * threads queue at the DB row lock; first 10 enroll, next 10 see a full class.
     *
     * <p>After the race, {@code currentEnrolled} counter on the Class row MUST equal
     * {@code MAX_STUDENTS} (= 10).
     */
    @Test
    @DisplayName("20 concurrent enroll requests to a 10-seat class → exactly 10 succeed, 10 get CLASS_FULL")
    void concurrentEnrollment_shouldEnforceCapacity() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_REQUESTS);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final Long studentId = savedStudents.get(i).getId();
            final Long classId = savedClass.getId();
            futures.add(executor.submit(() -> {
                try {
                    startGate.await(); // all threads start simultaneously
                    CreateEnrollmentRequest request = CreateEnrollmentRequest.builder()
                            .studentId(studentId)
                            .classId(classId)
                            .tuitionAmount(new BigDecimal("1000.00"))
                            .discountPercent(BigDecimal.ZERO)
                            .notes("Concurrent capacity test")
                            .build();

                    MvcResult result = mockMvc.perform(
                            post("/api/v1/enrollments")
                                    .header("X-Tenant-Id", tenantId.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    ).andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 201) {
                        successCount.incrementAndGet();
                    } else if (status == 400 || status == 409) {
                        // 400 = CLASS_FULL (ValidationException returns BAD_REQUEST in this codebase)
                        // 409 = OPTIMISTIC_LOCK_CONFLICT (OptimisticLockingFailureException)
                        rejectedCount.incrementAndGet();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // Unexpected — will show up as missing success/rejection counts
                } finally {
                    doneLatch.countDown();
                }
                return null;
            }));
        }

        // Release all threads simultaneously
        startGate.countDown();
        // Wait for all threads to finish (30s timeout)
        doneLatch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // Assert: exactly MAX_STUDENTS enrolled, the rest rejected
        assertThat(successCount.get())
                .as("Exactly MAX_STUDENTS=%d enrollments should succeed", MAX_STUDENTS)
                .isEqualTo(MAX_STUDENTS);
        assertThat(rejectedCount.get())
                .as("Remaining %d requests should be rejected (CLASS_FULL/400 or OPTIMISTIC_LOCK_CONFLICT/409)",
                        TOTAL_REQUESTS - MAX_STUDENTS)
                .isEqualTo(TOTAL_REQUESTS - MAX_STUDENTS);
        assertThat(successCount.get() + rejectedCount.get())
                .as("All %d requests must receive a response", TOTAL_REQUESTS)
                .isEqualTo(TOTAL_REQUESTS);

        // Assert: denormalized counter equals MAX_STUDENTS after race
        TenantContext.setCurrentTenant(tenantId);
        try {
            Class finalClass = classRepository.findByIdAndDeletedFalse(savedClass.getId()).orElseThrow();
            assertThat(finalClass.getCurrentEnrolled())
                    .as("currentEnrolled counter must equal MAX_STUDENTS after %d concurrent enrolments",
                            MAX_STUDENTS)
                    .isEqualTo(MAX_STUDENTS);
        } finally {
            TenantContext.clear();
        }
    }
}
