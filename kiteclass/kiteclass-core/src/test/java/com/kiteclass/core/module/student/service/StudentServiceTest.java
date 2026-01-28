package com.kiteclass.core.module.student.service;

import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.mapper.StudentMapper;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.student.service.impl.StudentServiceImpl;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StudentServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student student;
    private StudentResponse studentResponse;
    private CreateStudentRequest createRequest;
    private UpdateStudentRequest updateRequest;

    @BeforeEach
    void setUp() {
        student = StudentTestDataBuilder.createDefaultStudent();
        studentResponse = new StudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getDateOfBirth(),
                student.getGender(),
                student.getAddress(),
                student.getAvatarUrl(),
                student.getStatus(),
                student.getNote()
        );
        createRequest = StudentTestDataBuilder.createDefaultCreateRequest();
        updateRequest = StudentTestDataBuilder.createDefaultUpdateRequest();
    }

    @Test
    void createStudent_shouldCreateSuccessfully() {
        // Given
        when(studentRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(studentRepository.existsByPhoneAndDeletedFalse(anyString())).thenReturn(false);
        when(studentMapper.toEntity(any(CreateStudentRequest.class))).thenReturn(student);
        when(studentRepository.save(any(Student.class))).thenReturn(student);
        when(studentMapper.toResponse(any(Student.class))).thenReturn(studentResponse);

        // When
        StudentResponse result = studentService.createStudent(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(student.getName());
        verify(studentRepository).existsByEmailAndDeletedFalse(createRequest.email());
        verify(studentRepository).existsByPhoneAndDeletedFalse(createRequest.phone());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void createStudent_shouldThrowDuplicateResourceException_whenEmailExists() {
        // Given
        when(studentRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> studentService.createStudent(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "STUDENT_EMAIL_EXISTS");

        verify(studentRepository).existsByEmailAndDeletedFalse(createRequest.email());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createStudent_shouldThrowDuplicateResourceException_whenPhoneExists() {
        // Given
        when(studentRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(studentRepository.existsByPhoneAndDeletedFalse(anyString())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> studentService.createStudent(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "STUDENT_PHONE_EXISTS");

        verify(studentRepository).existsByPhoneAndDeletedFalse(createRequest.phone());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void getStudentById_shouldReturnStudent_whenExists() {
        // Given
        when(studentRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(student));
        when(studentMapper.toResponse(any(Student.class))).thenReturn(studentResponse);

        // When
        StudentResponse result = studentService.getStudentById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(student.getId());
        verify(studentRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    void getStudentById_shouldThrowEntityNotFoundException_whenNotExists() {
        // Given
        when(studentRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> studentService.getStudentById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "STUDENT_NOT_FOUND");

        verify(studentRepository).findByIdAndDeletedFalse(999L);
    }

    @Test
    void getStudents_shouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Student> studentPage = new PageImpl<>(List.of(student), pageable, 1);

        when(studentRepository.findBySearchCriteria(anyString(), any(), any(Pageable.class)))
                .thenReturn(studentPage);
        when(studentMapper.toResponse(any(Student.class))).thenReturn(studentResponse);

        // When
        PageResponse<StudentResponse> result = studentService.getStudents("test", "ACTIVE", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(studentRepository).findBySearchCriteria("test", StudentStatus.ACTIVE, pageable);
    }

    @Test
    void updateStudent_shouldUpdateSuccessfully() {
        // Given
        when(studentRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(student);
        when(studentMapper.toResponse(any(Student.class))).thenReturn(studentResponse);

        // When
        StudentResponse result = studentService.updateStudent(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(studentRepository).findByIdAndDeletedFalse(1L);
        verify(studentMapper).updateEntity(any(Student.class), any(UpdateStudentRequest.class));
        verify(studentRepository).save(student);
    }

    @Test
    void updateStudent_shouldThrowEntityNotFoundException_whenNotExists() {
        // Given
        when(studentRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> studentService.updateStudent(999L, updateRequest))
                .isInstanceOf(EntityNotFoundException.class);

        verify(studentRepository).findByIdAndDeletedFalse(999L);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void deleteStudent_shouldSoftDeleteSuccessfully() {
        // Given
        when(studentRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        // When
        studentService.deleteStudent(1L);

        // Then
        verify(studentRepository).findByIdAndDeletedFalse(1L);
        verify(studentRepository).save(student);
    }

    @Test
    void deleteStudent_shouldThrowEntityNotFoundException_whenNotExists() {
        // Given
        when(studentRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> studentService.deleteStudent(999L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(studentRepository).findByIdAndDeletedFalse(999L);
        verify(studentRepository, never()).save(any());
    }
}
