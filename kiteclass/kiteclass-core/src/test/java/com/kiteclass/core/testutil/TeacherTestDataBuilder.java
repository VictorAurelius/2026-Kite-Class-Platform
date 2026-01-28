package com.kiteclass.core.testutil;

import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
import com.kiteclass.core.module.teacher.entity.Teacher;

/**
 * Test data builder for Teacher-related objects.
 *
 * <p>Provides factory methods to create test data for:
 * <ul>
 *   <li>Teacher entities</li>
 *   <li>CreateTeacherRequest DTOs</li>
 *   <li>UpdateTeacherRequest DTOs</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
public class TeacherTestDataBuilder {

    /**
     * Creates a default Teacher entity for testing.
     *
     * @return Teacher with default test data
     */
    public static Teacher createDefaultTeacher() {
        Teacher teacher = Teacher.builder()
                .name("John Smith")
                .email("john.smith@example.com")
                .phoneNumber("0123456789")
                .specialization("English")
                .bio("Experienced English teacher with 10 years of teaching")
                .qualification("Bachelor of Education")
                .experienceYears(10)
                .avatarUrl("https://example.com/avatar.jpg")
                .status(TeacherStatus.ACTIVE)
                .build();
        teacher.setId(1L);
        teacher.setDeleted(false);
        return teacher;
    }

    /**
     * Creates a Teacher entity with custom name.
     *
     * @param name the teacher name
     * @return Teacher with specified name
     */
    public static Teacher createTeacherWithName(String name) {
        Teacher teacher = createDefaultTeacher();
        teacher.setName(name);
        return teacher;
    }

    /**
     * Creates a Teacher entity with custom email.
     *
     * @param email the teacher email
     * @return Teacher with specified email
     */
    public static Teacher createTeacherWithEmail(String email) {
        Teacher teacher = createDefaultTeacher();
        teacher.setEmail(email);
        return teacher;
    }

    /**
     * Creates a Teacher entity with custom status.
     *
     * @param status the teacher status
     * @return Teacher with specified status
     */
    public static Teacher createTeacherWithStatus(TeacherStatus status) {
        Teacher teacher = createDefaultTeacher();
        teacher.setStatus(status);
        return teacher;
    }

    /**
     * Creates a Teacher entity with custom specialization.
     *
     * @param specialization the teacher specialization
     * @return Teacher with specified specialization
     */
    public static Teacher createTeacherWithSpecialization(String specialization) {
        Teacher teacher = createDefaultTeacher();
        teacher.setSpecialization(specialization);
        return teacher;
    }

    /**
     * Creates a default CreateTeacherRequest for testing.
     *
     * @return CreateTeacherRequest with default test data
     */
    public static CreateTeacherRequest createDefaultCreateRequest() {
        return new CreateTeacherRequest(
                "Jane Doe",
                "jane.doe@example.com",
                "0987654321",
                "Mathematics",
                "Passionate math teacher",
                "Master of Science in Mathematics",
                8
        );
    }

    /**
     * Creates a CreateTeacherRequest with custom name.
     *
     * @param name the teacher name
     * @return CreateTeacherRequest with specified name
     */
    public static CreateTeacherRequest createRequestWithName(String name) {
        return new CreateTeacherRequest(
                name,
                "test@example.com",
                "0123456789",
                "English",
                "Test bio",
                "Test qualification",
                5
        );
    }

    /**
     * Creates a CreateTeacherRequest with custom email.
     *
     * @param email the teacher email
     * @return CreateTeacherRequest with specified email
     */
    public static CreateTeacherRequest createRequestWithEmail(String email) {
        return new CreateTeacherRequest(
                "Test Teacher",
                email,
                "0123456789",
                "English",
                "Test bio",
                "Test qualification",
                5
        );
    }

    /**
     * Creates a default UpdateTeacherRequest for testing.
     *
     * @return UpdateTeacherRequest with default test data
     */
    public static UpdateTeacherRequest createDefaultUpdateRequest() {
        return new UpdateTeacherRequest(
                "Updated Teacher",
                "updated@example.com",
                "0999999999",
                "Science",
                "Updated bio",
                "Updated qualification",
                12,
                TeacherStatus.ACTIVE
        );
    }

    /**
     * Creates an UpdateTeacherRequest with custom status.
     *
     * @param status the teacher status
     * @return UpdateTeacherRequest with specified status
     */
    public static UpdateTeacherRequest createUpdateRequestWithStatus(TeacherStatus status) {
        return new UpdateTeacherRequest(
                "Test Teacher",
                "test@example.com",
                "0123456789",
                "English",
                "Test bio",
                "Test qualification",
                5,
                status
        );
    }
}
