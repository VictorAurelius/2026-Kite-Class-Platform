package com.kiteclass.core.module.course.repository;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import com.kiteclass.core.testutil.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

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

    private Course course1;
    private Course course2;
    private Course course3;

    @BeforeEach
    void setUp() {
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
    void existsByCodeAndDeletedFalse_shouldReturnTrue_whenExists() {
        // When
        boolean exists = courseRepository.existsByCodeAndDeletedFalse("ENG-001");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByCodeAndDeletedFalse_shouldReturnFalse_whenNotExists() {
        // When
        boolean exists = courseRepository.existsByCodeAndDeletedFalse("NONEXISTENT");

        // Then
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
        Page<Course> result = courseRepository.findByStatusAndDeletedFalse(CourseStatus.PUBLISHED, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void findBySearchCriteria_shouldFindByName() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria("Mathematics", null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("Mathematics");
    }

    @Test
    void findBySearchCriteria_shouldFindByCode() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria("ENG", null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).contains("ENG");
    }

    @Test
    void findBySearchCriteria_shouldFilterByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria(null, CourseStatus.DRAFT, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void findBySearchCriteria_shouldFilterByTeacherId() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Course> result = courseRepository.findBySearchCriteria(null, null, 1L, pageable);

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
                "Physics", CourseStatus.ARCHIVED, 1L, pageable
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("PHY-001");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.ARCHIVED);
        assertThat(result.getContent().get(0).getTeacherId()).isEqualTo(1L);
    }

    @Test
    void countByStatusAndDeletedFalse_shouldReturnCorrectCount() {
        // When
        long count = courseRepository.countByStatusAndDeletedFalse(CourseStatus.DRAFT);

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
