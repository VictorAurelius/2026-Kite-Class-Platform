package com.kiteclass.core.module.course.repository;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import com.kiteclass.core.testutil.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CourseRepository}.
 *
 * <p>These tests require a real database connection and are only run when
 * {@code INTEGRATION_TEST=true} environment variable is set.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TEST", matches = "true")
class CourseRepositoryTest extends IntegrationTestBase {

    @Autowired
    private CourseRepository courseRepository;

    /** Fixed test tenant — set in TenantContext so EntityPersistenceListener stamps instance_id
     * on saved courses, and passed to findBySearchCriteria (GAP-791 tenant-scoped native query). */
    private static final UUID TEST_TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Course course1;
    private Course course2;
    private Course course3;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TEST_TENANT);
        courseRepository.deleteAll();

        course1 = CourseTestDataBuilder.createCourseWithCode("ENG-001");
        course1.setId(null);
        course1.setTeacherId(1L);
        course1 = courseRepository.save(course1);

        course2 = CourseTestDataBuilder.createCourseWithCode("MATH-001");
        course2.setId(null);
        course2.setName("Mathematics Fundamentals");
        course2.setTeacherId(2L);
        course2.setStatus(CourseStatus.PUBLISHED);
        course2 = courseRepository.save(course2);

        course3 = CourseTestDataBuilder.createCourseWithCode("PHY-001");
        course3.setId(null);
        course3.setName("Physics for Beginners");
        course3.setTeacherId(1L);
        course3.setStatus(CourseStatus.ARCHIVED);
        course3 = courseRepository.save(course3);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findByIdAndDeletedFalse_shouldReturnCourse_whenExists() {
        // When
        Optional<Course> result = courseRepository.findByIdAndDeletedFalse(course1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("ENG-001");
    }

    @Test
    void findByIdAndDeletedFalse_shouldReturnEmpty_whenDeleted() {
        // Given
        course1.markAsDeleted();
        courseRepository.save(course1);

        // When
        Optional<Course> result = courseRepository.findByIdAndDeletedFalse(course1.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByCodeAndDeletedFalse_shouldReturnCourse_whenExists() {
        // When
        Optional<Course> result = courseRepository.findByCodeAndDeletedFalse("ENG-001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(course1.getId());
    }

    @Test
    void existsByCodeAndInstanceId_shouldReturnFalse_whenCodeNotExists() {
        // When
        boolean exists = courseRepository.existsByCodeAndInstanceIdAndDeletedFalse("NONEXISTENT", TEST_TENANT);

        // Then
        assertThat(exists).isFalse();
    }

    /** GAP-799 — tenant-scoped uniqueness: same code in current tenant → exists. */
    @Test
    void existsByCodeAndInstanceId_shouldReturnTrue_whenCodeExistsInSameTenant() {
        // When
        boolean exists = courseRepository.existsByCodeAndInstanceIdAndDeletedFalse("ENG-001", TEST_TENANT);

        // Then
        assertThat(exists).isTrue();
    }

    /**
     * GAP-799 regression guard — a code used by tenant A must NOT collide for tenant B.
     * Before the fix, the global {@code existsByCodeAndDeletedFalse} saw all tenants'
     * rows in the shared kiteclass DB → cross-tenant collision (409 COURSE_CODE_EXISTS).
     */
    @Test
    void existsByCodeAndInstanceId_shouldReturnFalse_forDifferentTenant() {
        // Given — ENG-001 exists in TEST_TENANT (seeded in setUp)
        UUID otherTenant = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // When
        boolean exists = courseRepository.existsByCodeAndInstanceIdAndDeletedFalse("ENG-001", otherTenant);

        // Then — tenant B can legitimately reuse the same code
        assertThat(exists).isFalse();
    }

    @Test
    void findByTeacherIdAndDeletedFalse_shouldReturnCoursesForTeacher() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findByTeacherIdAndDeletedFalse(1L, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2); // course1 and course3
        assertThat(result.getContent()).extracting(Course::getTeacherId).containsOnly(1L);
    }

    @Test
    void findByStatusAndDeletedFalse_shouldReturnCoursesWithStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findByStatusAndDeletedFalse("PUBLISHED", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void findBySearchCriteria_shouldFindByName() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria(TEST_TENANT, "Mathematics", null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("Mathematics");
    }

    @Test
    void findBySearchCriteria_shouldFindByCode() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria(TEST_TENANT, "ENG", null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).contains("ENG");
    }

    @Test
    void findBySearchCriteria_shouldFilterByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria(TEST_TENANT, null, "DRAFT", null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void findBySearchCriteria_shouldFilterByTeacherId() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria(TEST_TENANT, null, null, 1L, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2); // course1 and course3
        assertThat(result.getContent()).extracting(Course::getTeacherId).containsOnly(1L);
    }

    @Test
    void findBySearchCriteria_shouldCombineMultipleFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria(
                TEST_TENANT, "Physics", "ARCHIVED", 1L, pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("PHY-001");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("ARCHIVED");
        assertThat(result.getContent().get(0).getTeacherId()).isEqualTo(1L);
    }

    @Test
    void countByStatusAndDeletedFalse_shouldReturnCorrectCount() {
        // When
        long count = courseRepository.countByStatusAndDeletedFalse("DRAFT");

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void countByTeacherIdAndDeletedFalse_shouldReturnCorrectCount() {
        // When
        long count = courseRepository.countByTeacherIdAndDeletedFalse(1L);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void findPublishedCourses_shouldReturnOnlyPublishedCourses() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findPublishedCourses(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void findDraftCoursesByTeacher_shouldReturnOnlyDraftCoursesForTeacher() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findDraftCoursesByTeacher(1L, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.getContent().get(0).getTeacherId()).isEqualTo(1L);
    }
}
