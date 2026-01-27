package com.kiteclass.core.testutil;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.entity.Student;

import java.time.LocalDate;

/**
 * Test data builder for Student-related objects.
 *
 * <p>Provides factory methods to create test data for:
 * <ul>
 *   <li>Student entities</li>
 *   <li>CreateStudentRequest DTOs</li>
 *   <li>UpdateStudentRequest DTOs</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
public class StudentTestDataBuilder {

    /**
     * Creates a default Student entity for testing.
     *
     * @return Student with default test data
     */
    public static Student createDefaultStudent() {
        Student student = Student.builder()
                .name("Nguyen Van A")
                .email("nguyenvana@example.com")
                .phone("0123456789")
                .dateOfBirth(LocalDate.of(2010, 1, 1))
                .gender(Gender.MALE)
                .address("123 Test Street, Hanoi")
                .avatarUrl("https://example.com/avatar.jpg")
                .status(StudentStatus.ACTIVE)
                .note("Default test student")
                .build();
        student.setId(1L);
        student.setDeleted(false);
        return student;
    }

    /**
     * Creates a Student entity with custom name.
     *
     * @param name the student name
     * @return Student with specified name
     */
    public static Student createStudentWithName(String name) {
        Student student = createDefaultStudent();
        student.setName(name);
        return student;
    }

    /**
     * Creates a Student entity with custom email.
     *
     * @param email the student email
     * @return Student with specified email
     */
    public static Student createStudentWithEmail(String email) {
        Student student = createDefaultStudent();
        student.setEmail(email);
        return student;
    }

    /**
     * Creates a Student entity with custom status.
     *
     * @param status the student status
     * @return Student with specified status
     */
    public static Student createStudentWithStatus(StudentStatus status) {
        Student student = createDefaultStudent();
        student.setStatus(status);
        return student;
    }

    /**
     * Creates a default CreateStudentRequest for testing.
     *
     * @return CreateStudentRequest with default test data
     */
    public static CreateStudentRequest createDefaultCreateRequest() {
        return new CreateStudentRequest(
                "Tran Thi B",
                "tranthib@example.com",
                "0987654321",
                LocalDate.of(2012, 5, 15),
                Gender.FEMALE,
                "456 Test Avenue, HCMC",
                "Test note"
        );
    }

    /**
     * Creates a CreateStudentRequest with custom name.
     *
     * @param name the student name
     * @return CreateStudentRequest with specified name
     */
    public static CreateStudentRequest createRequestWithName(String name) {
        return new CreateStudentRequest(
                name,
                "test@example.com",
                "0123456789",
                LocalDate.of(2010, 1, 1),
                Gender.MALE,
                "Test address",
                "Test note"
        );
    }

    /**
     * Creates a default UpdateStudentRequest for testing.
     *
     * @return UpdateStudentRequest with default test data
     */
    public static UpdateStudentRequest createDefaultUpdateRequest() {
        return new UpdateStudentRequest(
                "Updated Name",
                "updated@example.com",
                "0999999999",
                LocalDate.of(2011, 6, 20),
                Gender.FEMALE,
                "Updated address",
                StudentStatus.ACTIVE,
                "Updated note"
        );
    }

    /**
     * Creates an UpdateStudentRequest with custom status.
     *
     * @param status the student status
     * @return UpdateStudentRequest with specified status
     */
    public static UpdateStudentRequest createUpdateRequestWithStatus(StudentStatus status) {
        return new UpdateStudentRequest(
                "Test Student",
                "test@example.com",
                "0123456789",
                LocalDate.of(2010, 1, 1),
                Gender.MALE,
                "Test address",
                status,
                "Test note"
        );
    }
}
