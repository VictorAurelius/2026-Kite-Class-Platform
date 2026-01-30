package com.kiteclass.core.module.teacher.service;

import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
import com.kiteclass.core.module.teacher.entity.Teacher;
import com.kiteclass.core.module.teacher.mapper.TeacherMapper;
import com.kiteclass.core.module.teacher.repository.TeacherRepository;
import com.kiteclass.core.module.teacher.service.impl.TeacherServiceImpl;
import com.kiteclass.core.testutil.TeacherTestDataBuilder;
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
 * Unit tests for {@link TeacherServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherMapper teacherMapper;

    @InjectMocks
    private TeacherServiceImpl teacherService;

    private Teacher teacher;
    private TeacherResponse teacherResponse;
    private CreateTeacherRequest createRequest;
    private UpdateTeacherRequest updateRequest;

    @BeforeEach
    void setUp() {
        teacher = TeacherTestDataBuilder.createDefaultTeacher();
        teacherResponse = new TeacherResponse(
                teacher.getId(),
                teacher.getName(),
                teacher.getEmail(),
                teacher.getPhoneNumber(),
                teacher.getSpecialization(),
                teacher.getBio(),
                teacher.getQualification(),
                teacher.getExperienceYears(),
                teacher.getAvatarUrl(),
                teacher.getStatus().name()
        );
        createRequest = TeacherTestDataBuilder.createDefaultCreateRequest();
        updateRequest = TeacherTestDataBuilder.createDefaultUpdateRequest();
    }

    @Test
    void createTeacher_shouldCreateSuccessfully() {
        // Given
        when(teacherRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(false);
        when(teacherMapper.toEntity(any(CreateTeacherRequest.class))).thenReturn(teacher);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);
        when(teacherMapper.toResponse(any(Teacher.class))).thenReturn(teacherResponse);

        // When
        TeacherResponse result = teacherService.createTeacher(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(teacher.getName());
        verify(teacherRepository).existsByEmailAndDeletedFalse(createRequest.email());
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void createTeacher_shouldThrowDuplicateResourceException_whenEmailExists() {
        // Given
        when(teacherRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> teacherService.createTeacher(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasFieldOrPropertyWithValue("code", "TEACHER_EMAIL_EXISTS");

        verify(teacherRepository).existsByEmailAndDeletedFalse(createRequest.email());
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void getTeacherById_shouldReturnTeacher_whenExists() {
        // Given
        when(teacherRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(teacher));
        when(teacherMapper.toResponse(any(Teacher.class))).thenReturn(teacherResponse);

        // When
        TeacherResponse result = teacherService.getTeacherById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(teacherRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    void getTeacherById_shouldThrowEntityNotFoundException_whenNotExists() {
        // Given
        when(teacherRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> teacherService.getTeacherById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "TEACHER_NOT_FOUND");

        verify(teacherRepository).findByIdAndDeletedFalse(999L);
    }

    @Test
    void getTeachers_shouldReturnPageOfTeachers() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Teacher> teacherPage = new PageImpl<>(List.of(teacher), pageable, 1);

        when(teacherRepository.findBySearchCriteria(anyString(), any(), any(Pageable.class)))
                .thenReturn(teacherPage);
        when(teacherMapper.toResponse(any(Teacher.class))).thenReturn(teacherResponse);

        // When
        PageResponse<TeacherResponse> result = teacherService.getTeachers("test", "ACTIVE", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(teacherRepository).findBySearchCriteria("test", TeacherStatus.ACTIVE, pageable);
    }

    @Test
    void updateTeacher_shouldUpdateSuccessfully() {
        // Given
        when(teacherRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);
        when(teacherMapper.toResponse(any(Teacher.class))).thenReturn(teacherResponse);

        // When
        TeacherResponse result = teacherService.updateTeacher(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(teacherRepository).findByIdAndDeletedFalse(1L);
        verify(teacherMapper).updateEntity(any(Teacher.class), any(UpdateTeacherRequest.class));
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void updateTeacher_shouldThrowEntityNotFoundException_whenNotExists() {
        // Given
        when(teacherRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> teacherService.updateTeacher(999L, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "TEACHER_NOT_FOUND");

        verify(teacherRepository).findByIdAndDeletedFalse(999L);
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void deleteTeacher_shouldDeleteSuccessfully() {
        // Given
        when(teacherRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);

        // When
        teacherService.deleteTeacher(1L);

        // Then
        verify(teacherRepository).findByIdAndDeletedFalse(1L);
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void deleteTeacher_shouldThrowEntityNotFoundException_whenNotExists() {
        // Given
        when(teacherRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> teacherService.deleteTeacher(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "TEACHER_NOT_FOUND");

        verify(teacherRepository).findByIdAndDeletedFalse(999L);
        verify(teacherRepository, never()).save(any(Teacher.class));
    }
}
