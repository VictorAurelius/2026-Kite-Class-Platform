package com.kiteclass.core.module.student.mapper;

import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StudentMapper}.
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
class StudentMapperTest {

    private final StudentMapper mapper = Mappers.getMapper(StudentMapper.class);

    @Test
    void toResponse_shouldMapCorrectly() {
        // Given
        Student student = StudentTestDataBuilder.createDefaultStudent();

        // When
        StudentResponse response = mapper.toResponse(student);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(student.getId());
        assertThat(response.name()).isEqualTo(student.getName());
        assertThat(response.email()).isEqualTo(student.getEmail());
    }

    @Test
    void toEntity_shouldMapCorrectly() {
        // Given
        CreateStudentRequest request = StudentTestDataBuilder.createDefaultCreateRequest();

        // When
        Student entity = mapper.toEntity(request);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo(request.name());
        assertThat(entity.getEmail()).isEqualTo(request.email());
        assertThat(entity.getId()).isNull();
    }

    @Test
    void updateEntity_shouldUpdateOnlyProvidedFields() {
        // Given
        Student student = StudentTestDataBuilder.createDefaultStudent();
        String originalEmail = student.getEmail();
        UpdateStudentRequest request = new UpdateStudentRequest(
                "Updated Name", null, null, null, null, null, null, null);

        // When
        mapper.updateEntity(student, request);

        // Then
        assertThat(student.getName()).isEqualTo("Updated Name");
        assertThat(student.getEmail()).isEqualTo(originalEmail); // Should not change
    }
}
