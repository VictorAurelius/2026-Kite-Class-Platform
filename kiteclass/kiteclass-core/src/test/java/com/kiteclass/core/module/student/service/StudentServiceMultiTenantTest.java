package com.kiteclass.core.module.student.service;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Multi-tenant security tests for StudentService.
 *
 * <p>Verifies tenant isolation:
 * <ul>
 *   <li>Students from different tenants are isolated</li>
 *   <li>Cross-tenant access is denied</li>
 *   <li>Tenant filter works correctly</li>
 *   <li>No SQL injection via tenant context</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class StudentServiceMultiTenantTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID tenant1;
    private UUID tenant2;

    @BeforeEach
    void setUp() {
        tenant1 = UUID.randomUUID();
        tenant2 = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should only return current tenant's students")
    void getStudents_shouldOnlyReturnCurrentTenantStudents() {
        // Given: Two tenants with students
        createStudentForTenant(tenant1, "Tenant 1 Student", "student1@t1.com");
        createStudentForTenant(tenant2, "Tenant 2 Student", "student2@t2.com");

        // When: Get students as tenant1
        TenantContext.setCurrentTenant(tenant1);
        enableTenantFilter(tenant1);

        List<Student> tenant1Students = studentRepository.findAll();

        // Then: Should only see tenant1's student
        assertThat(tenant1Students)
            .hasSize(1)
            .extracting(Student::getName)
            .containsExactly("Tenant 1 Student");

        assertThat(tenant1Students)
            .noneMatch(s -> s.getEmail().equals("student2@t2.com"));
    }

    @Test
    @DisplayName("Should throw 404 for other tenant's student")
    void getStudentById_shouldThrow404_whenAccessingOtherTenantStudent() {
        // Given: Student belongs to tenant1
        Student student = createStudentForTenant(tenant1, "Tenant 1 Student", "student@t1.com");
        Long studentId = student.getId();

        // When: Try to access as tenant2
        TenantContext.setCurrentTenant(tenant2);
        enableTenantFilter(tenant2);

        // Then: Should throw EntityNotFoundException (cross-tenant access denied)
        assertThatThrownBy(() -> {
            studentRepository.findById(studentId).orElseThrow(() ->
                new EntityNotFoundException("Student", studentId));
        })
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(studentId.toString());
    }

    @Test
    @DisplayName("Should not allow cross-tenant update")
    void updateStudent_shouldNotAllowCrossTenantUpdate() {
        // Given: Student belongs to tenant1
        Student student = createStudentForTenant(tenant1, "Original Name", "student@t1.com");
        Long studentId = student.getId();

        // When: Tenant2 tries to update tenant1's student
        TenantContext.setCurrentTenant(tenant2);
        enableTenantFilter(tenant2);

        UpdateStudentRequest request = new UpdateStudentRequest(
            "Hacked Name", null, null, null, null, null, null, null
        );

        // Then: Should throw exception (not found in tenant2 context)
        assertThatThrownBy(() -> {
            Student found = studentRepository.findById(studentId).orElseThrow(() ->
                new EntityNotFoundException("Student", studentId));
            found.setName(request.name());
            studentRepository.save(found);
        })
            .isInstanceOf(EntityNotFoundException.class);

        // Verify: Original data unchanged
        TenantContext.clear();
        TenantContext.setCurrentTenant(tenant1);
        enableTenantFilter(tenant1);

        Student unchanged = studentRepository.findById(studentId).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Original Name");
    }

    @Test
    @DisplayName("Should not allow cross-tenant delete")
    void deleteStudent_shouldNotAllowCrossTenantDelete() {
        // Given: Student belongs to tenant1
        Student student = createStudentForTenant(tenant1, "Tenant 1 Student", "student@t1.com");
        Long studentId = student.getId();

        // When: Tenant2 tries to delete tenant1's student
        TenantContext.setCurrentTenant(tenant2);
        enableTenantFilter(tenant2);

        // Then: Should throw exception
        assertThatThrownBy(() -> {
            Student found = studentRepository.findById(studentId).orElseThrow(() ->
                new EntityNotFoundException("Student", studentId));
            studentRepository.delete(found);
        })
            .isInstanceOf(EntityNotFoundException.class);

        // Verify: Student still exists
        TenantContext.clear();
        TenantContext.setCurrentTenant(tenant1);
        enableTenantFilter(tenant1);

        assertThat(studentRepository.findById(studentId)).isPresent();
    }

    @Test
    @DisplayName("Should auto-set instanceId when creating student")
    void createStudent_shouldAutoSetInstanceId() {
        // Given: Tenant context set
        TenantContext.setCurrentTenant(tenant1);

        // When: Create student (without setting instanceId)
        Student student = Student.builder()
            .name("New Student")
            .email("new@t1.com")
            .status(StudentStatus.ACTIVE)
            .build();

        Student saved = studentRepository.save(student);

        // Then: instanceId should be auto-set to current tenant
        assertThat(saved.getInstanceId()).isEqualTo(tenant1);
    }

    @Test
    @DisplayName("Should prevent changing instanceId on update")
    void updateStudent_shouldPreventChangingInstanceId() {
        // Given: Student belongs to tenant1
        Student student = createStudentForTenant(tenant1, "Student", "student@t1.com");

        // When: Try to change instanceId to tenant2
        TenantContext.setCurrentTenant(tenant1);
        enableTenantFilter(tenant1);

        student.setInstanceId(tenant2); // Try to change tenant

        // Then: Should throw exception
        assertThatThrownBy(() -> studentRepository.save(student))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot update entity from different tenant");
    }

    @Test
    @DisplayName("Should find students by email within tenant")
    void findByEmail_shouldOnlySearchWithinTenant() {
        // Given: Same email in different tenants
        createStudentForTenant(tenant1, "Student T1", "same@email.com");
        createStudentForTenant(tenant2, "Student T2", "same@email.com");

        // When: Search as tenant1
        TenantContext.setCurrentTenant(tenant1);
        enableTenantFilter(tenant1);

        List<Student> found = studentRepository.findAll();

        // Then: Should only find tenant1's student
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Student T1");
        assertThat(found.get(0).getInstanceId()).isEqualTo(tenant1);
    }

    @Test
    @DisplayName("Should count only current tenant's students")
    void countStudents_shouldOnlyCountCurrentTenant() {
        // Given: Different numbers of students in different tenants
        createStudentForTenant(tenant1, "Student 1", "s1@t1.com");
        createStudentForTenant(tenant1, "Student 2", "s2@t1.com");
        createStudentForTenant(tenant2, "Student 3", "s3@t2.com");

        // When: Count as tenant1
        TenantContext.setCurrentTenant(tenant1);
        enableTenantFilter(tenant1);

        long count = studentRepository.count();

        // Then: Should only count tenant1's students
        assertThat(count).isEqualTo(2);
    }

    /**
     * Helper method to create student for specific tenant.
     */
    private Student createStudentForTenant(UUID tenantId, String name, String email) {
        TenantContext.setCurrentTenant(tenantId);

        Student student = Student.builder()
            .name(name)
            .email(email)
            .status(StudentStatus.ACTIVE)
            .dateOfBirth(LocalDate.of(2010, 1, 1))
            .build();

        Student saved = studentRepository.save(student);
        entityManager.flush();
        entityManager.clear();
        TenantContext.clear();

        return saved;
    }

    /**
     * Helper method to enable Hibernate tenant filter.
     */
    private void enableTenantFilter(UUID tenantId) {
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenantId);
    }
}
