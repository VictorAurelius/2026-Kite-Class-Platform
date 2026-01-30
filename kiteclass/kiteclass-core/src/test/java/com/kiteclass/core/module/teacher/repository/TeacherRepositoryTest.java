package com.kiteclass.core.module.teacher.repository;

import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.module.teacher.entity.Teacher;
import com.kiteclass.core.testutil.IntegrationTestBase;
import com.kiteclass.core.testutil.TeacherTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TeacherRepository}.
 *
 * <p>These tests require a real database connection and are only run when
 * {@code INTEGRATION_TEST=true} environment variable is set.
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TEST", matches = "true")
class TeacherRepositoryTest extends IntegrationTestBase {

    @Autowired
    private TeacherRepository teacherRepository;

    private Teacher teacher1;
    private Teacher teacher2;

    @BeforeEach
    void setUp() {
        teacherRepository.deleteAll();

        teacher1 = TeacherTestDataBuilder.createTeacherWithEmail("teacher1@example.com");
        teacher1.setId(null);
        teacher1 = teacherRepository.save(teacher1);

        teacher2 = TeacherTestDataBuilder.createTeacherWithEmail("teacher2@example.com");
        teacher2.setId(null);
        teacher2.setSpecialization("Mathematics");
        teacher2 = teacherRepository.save(teacher2);
    }

    @Test
    void findByIdAndDeletedFalse_shouldReturnTeacher_whenExists() {
        // When
        Optional<Teacher> result = teacherRepository.findByIdAndDeletedFalse(teacher1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("teacher1@example.com");
    }

    @Test
    void findByIdAndDeletedFalse_shouldReturnEmpty_whenDeleted() {
        // Given
        teacher1.markAsDeleted();
        teacherRepository.save(teacher1);

        // When
        Optional<Teacher> result = teacherRepository.findByIdAndDeletedFalse(teacher1.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmailAndDeletedFalse_shouldReturnTrue_whenExists() {
        // When
        boolean exists = teacherRepository.existsByEmailAndDeletedFalse("teacher1@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailAndDeletedFalse_shouldReturnFalse_whenNotExists() {
        // When
        boolean exists = teacherRepository.existsByEmailAndDeletedFalse("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findBySearchCriteria_shouldFindByName() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Teacher> result = teacherRepository.findBySearchCriteria("John", null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findBySearchCriteria_shouldFindByEmail() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Teacher> result = teacherRepository.findBySearchCriteria("teacher1", null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).contains("teacher1");
    }

    @Test
    void findBySearchCriteria_shouldFindBySpecialization() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Teacher> result = teacherRepository.findBySearchCriteria("Mathematics", null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSpecialization()).isEqualTo("Mathematics");
    }

    @Test
    void findBySearchCriteria_shouldFilterByStatus() {
        // Given
        teacher2.setStatus(TeacherStatus.INACTIVE);
        teacherRepository.save(teacher2);
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<Teacher> result = teacherRepository.findBySearchCriteria(null, TeacherStatus.ACTIVE, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TeacherStatus.ACTIVE);
    }

    @Test
    void findByStatusAndDeletedFalse_shouldReturnActiveTeachers() {
        // When
        List<Teacher> result = teacherRepository.findByStatusAndDeletedFalse(TeacherStatus.ACTIVE);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void countByStatusAndDeletedFalse_shouldReturnCorrectCount() {
        // When
        long count = teacherRepository.countByStatusAndDeletedFalse(TeacherStatus.ACTIVE);

        // Then
        assertThat(count).isEqualTo(2);
    }
}
