package com.kiteclass.core.module.teacher.mapper;

import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
import com.kiteclass.core.module.teacher.entity.Teacher;
import com.kiteclass.core.testutil.TeacherTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TeacherMapper}.
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@SpringBootTest
@ActiveProfiles("test")
class TeacherMapperTest {

    @Autowired
    private TeacherMapper teacherMapper;

    @Test
    void toResponse_shouldMapCorrectly() {
        // Given
        Teacher teacher = TeacherTestDataBuilder.createDefaultTeacher();

        // When
        TeacherResponse response = teacherMapper.toResponse(teacher);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(teacher.getId());
        assertThat(response.name()).isEqualTo(teacher.getName());
        assertThat(response.email()).isEqualTo(teacher.getEmail());
        assertThat(response.phoneNumber()).isEqualTo(teacher.getPhoneNumber());
        assertThat(response.specialization()).isEqualTo(teacher.getSpecialization());
        assertThat(response.status()).isEqualTo(teacher.getStatus().name());
    }

    @Test
    void toEntity_shouldMapCorrectly() {
        // Given
        CreateTeacherRequest request = TeacherTestDataBuilder.createDefaultCreateRequest();

        // When
        Teacher teacher = teacherMapper.toEntity(request);

        // Then
        assertThat(teacher).isNotNull();
        assertThat(teacher.getName()).isEqualTo(request.name());
        assertThat(teacher.getEmail()).isEqualTo(request.email());
        assertThat(teacher.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(teacher.getSpecialization()).isEqualTo(request.specialization());
        assertThat(teacher.getStatus()).isEqualTo(TeacherStatus.ACTIVE); // Default status
    }

    @Test
    void updateEntity_shouldUpdateOnlyNonNullFields() {
        // Given
        Teacher teacher = TeacherTestDataBuilder.createDefaultTeacher();
        String originalEmail = teacher.getEmail();
        UpdateTeacherRequest request = new UpdateTeacherRequest(
                "New Name",
                null,  // Should not update email
                "0999999999",
                null,  // Should not update specialization
                "New bio",
                null,
                null,
                TeacherStatus.INACTIVE
        );

        // When
        teacherMapper.updateEntity(teacher, request);

        // Then
        assertThat(teacher.getName()).isEqualTo("New Name");
        assertThat(teacher.getEmail()).isEqualTo(originalEmail); // Email should not change
        assertThat(teacher.getPhoneNumber()).isEqualTo("0999999999");
        assertThat(teacher.getBio()).isEqualTo("New bio");
        assertThat(teacher.getStatus()).isEqualTo(TeacherStatus.INACTIVE);
    }
}
