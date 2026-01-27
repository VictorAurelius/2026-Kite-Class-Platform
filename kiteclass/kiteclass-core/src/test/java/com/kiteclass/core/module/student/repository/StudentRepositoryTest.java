package com.kiteclass.core.module.student.repository;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.testutil.IntegrationTestBase;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link StudentRepository}.
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
class StudentRepositoryTest extends IntegrationTestBase {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void findByIdAndDeletedFalse_shouldReturnStudent_whenExists() {
        // Given
        Student student = StudentTestDataBuilder.createDefaultStudent();
        student.setId(null);
        Student saved = studentRepository.save(student);

        // When
        var result = studentRepository.findByIdAndDeletedFalse(saved.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(student.getName());
    }

    @Test
    void existsByEmailAndDeletedFalse_shouldReturnTrue_whenEmailExists() {
        // Given
        Student student = StudentTestDataBuilder.createDefaultStudent();
        student.setId(null);
        student.setEmail("unique@example.com");
        studentRepository.save(student);

        // When
        boolean exists = studentRepository.existsByEmailAndDeletedFalse("unique@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void findBySearchCriteria_shouldReturnMatchingStudents() {
        // Given
        Student student1 = StudentTestDataBuilder.createStudentWithName("John Doe");
        student1.setId(null);
        studentRepository.save(student1);

        // When
        Page<Student> result = studentRepository.findBySearchCriteria(
                "John", null, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().get(0).getName()).contains("John");
    }

    @Test
    void countByStatusAndDeletedFalse_shouldReturnCorrectCount() {
        // Given
        Student student = StudentTestDataBuilder.createStudentWithStatus(StudentStatus.ACTIVE);
        student.setId(null);
        studentRepository.save(student);

        // When
        long count = studentRepository.countByStatusAndDeletedFalse(StudentStatus.ACTIVE);

        // Then
        assertThat(count).isGreaterThan(0);
    }
}
