package com.kiteclass.core.module.student.repository;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.testutil.IntegrationTestBase;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link StudentRepository}.
 *
 * <p><strong>IMPORTANT:</strong> These tests require Docker to be running
 * as they use Testcontainers with PostgreSQL.
 *
 * <p>To run these tests:
 * <ul>
 *   <li>Start Docker Desktop (or Docker daemon)</li>
 *   <li>Set environment variable: ENABLE_INTEGRATION_TESTS=true</li>
 *   <li>Run: mvn test -DENABLE_INTEGRATION_TESTS=true</li>
 * </ul>
 *
 * <p>These tests are disabled by default to allow running unit tests
 * without Docker dependency.
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
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
    void existsByEmailAndInstanceIdAndDeletedFalse_shouldReturnTrue_whenEmailExists() {
        // Given
        Student student = StudentTestDataBuilder.createDefaultStudent();
        student.setId(null);
        student.setEmail("unique@example.com");
        Student saved = studentRepository.save(student);

        // When
        boolean exists = studentRepository.existsByEmailAndInstanceIdAndDeletedFalse(
                "unique@example.com",
                saved.getInstanceId()
        );

        // Then
        assertThat(exists).isTrue();
    }

    /** GAP-799 — tenant-scoped phone uniqueness: same phone in current tenant → exists. */
    @Test
    void existsByPhoneAndInstanceId_shouldReturnTrue_whenPhoneExistsInSameTenant() {
        // Given
        Student student = StudentTestDataBuilder.createDefaultStudent();
        student.setId(null);
        student.setPhone("0901234567");
        Student saved = studentRepository.save(student);

        // When
        boolean exists = studentRepository.existsByPhoneAndInstanceIdAndDeletedFalse(
                "0901234567", saved.getInstanceId());

        // Then
        assertThat(exists).isTrue();
    }

    /**
     * GAP-799 regression guard — a phone used by tenant A must NOT collide for tenant B.
     * Before the fix, the global {@code existsByPhoneAndDeletedFalse} saw all tenants'
     * rows in the shared kiteclass DB → blocked legitimate reuse (shared parent phone
     * across centers) + cross-tenant enumeration leak.
     */
    @Test
    void existsByPhoneAndInstanceId_shouldReturnFalse_forDifferentTenant() {
        // Given
        Student student = StudentTestDataBuilder.createDefaultStudent();
        student.setId(null);
        student.setPhone("0907654321");
        studentRepository.save(student);
        java.util.UUID otherTenant = java.util.UUID.fromString("33333333-3333-3333-3333-333333333333");

        // When
        boolean exists = studentRepository.existsByPhoneAndInstanceIdAndDeletedFalse(
                "0907654321", otherTenant);

        // Then — tenant B can legitimately reuse the same phone
        assertThat(exists).isFalse();
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
        long count = studentRepository.countByStatusAndDeletedFalse("ACTIVE");

        // Then
        assertThat(count).isGreaterThan(0);
    }
}
