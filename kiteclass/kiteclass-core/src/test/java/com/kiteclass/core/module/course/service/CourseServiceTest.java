package com.kiteclass.core.module.course.service;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.constant.TeacherCourseRole;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.CourseSearchCriteria;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.mapper.CourseMapper;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.course.service.impl.CourseServiceImpl;
import com.kiteclass.core.module.teacher.entity.Teacher;
import com.kiteclass.core.module.teacher.entity.TeacherCourse;
import com.kiteclass.core.module.teacher.repository.TeacherCourseRepository;
import com.kiteclass.core.module.teacher.repository.TeacherRepository;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import com.kiteclass.core.testutil.TeacherTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CourseServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherCourseRepository teacherCourseRepository;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course course;
    private CourseResponse courseResponse;
    private CreateCourseRequest createRequest;
    private UpdateCourseRequest updateRequest;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        course = CourseTestDataBuilder.createDefaultCourse();
        courseResponse = new CourseResponse(
                course.getId(),
                course.getName(),
                course.getCode(),
                course.getDescription(),
                course.getSyllabus(),
                course.getObjectives(),
                course.getPrerequisites(),
                course.getTargetAudience(),
                course.getTeacherId(),
                course.getDurationWeeks(),
                course.getTotalSessions(),
                course.getPrice(),
                course.getStatus().name(),
                course.getCoverImageUrl(),
                null,
                null
        );
        createRequest = CourseTestDataBuilder.createDefaultCreateRequest();
        updateRequest = CourseTestDataBuilder.createDefaultUpdateRequest();
        teacher = TeacherTestDataBuilder.createDefaultTeacher();
    }

    @Test
    void createCourse_shouldCreateSuccessfully() {
        // Given
        when(courseRepository.existsByCodeAndDeletedFalse(anyString())).thenReturn(false);
        when(teacherRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(teacher));
        when(courseMapper.toEntity(any(CreateCourseRequest.class))).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(teacherCourseRepository.save(any(TeacherCourse.class))).thenReturn(new TeacherCourse());
        when(courseMapper.toResponse(any(Course.class))).thenReturn(courseResponse);

        // When
        CourseResponse result = courseService.createCourse(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(course.getName());
        verify(courseRepository).existsByCodeAndDeletedFalse(createRequest.code());
        verify(teacherRepository).findByIdAndDeletedFalse(createRequest.teacherId());
        verify(courseRepository).save(any(Course.class));
        verify(teacherCourseRepository).save(any(TeacherCourse.class));
    }

    @Test
    void createCourse_shouldThrowDuplicateResourceException_whenCodeExists() {
        // Given
        when(courseRepository.existsByCodeAndDeletedFalse(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> courseService.createCourse(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("code");

        verify(courseRepository).existsByCodeAndDeletedFalse(createRequest.code());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void createCourse_shouldCreateTeacherCourseWithCreatorRole() {
        // Given
        when(courseRepository.existsByCodeAndDeletedFalse(anyString())).thenReturn(false);
        when(teacherRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(teacher));
        when(courseMapper.toEntity(any(CreateCourseRequest.class))).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toResponse(any(Course.class))).thenReturn(courseResponse);

        // When
        courseService.createCourse(createRequest);

        // Then
        verify(teacherCourseRepository).save(argThat(tc ->
                tc.getTeacherId().equals(createRequest.teacherId()) &&
                tc.getCourseId().equals(course.getId()) &&
                tc.getRole() == TeacherCourseRole.CREATOR &&
                tc.getAssignedBy() == null
        ));
    }

    @Test
    void createCourse_shouldThrowEntityNotFoundException_whenTeacherNotExists() {
        // Given
        when(courseRepository.existsByCodeAndDeletedFalse(anyString())).thenReturn(false);
        when(teacherRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> courseService.createCourse(createRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Teacher");

        verify(teacherRepository).findByIdAndDeletedFalse(createRequest.teacherId());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void getCourseById_shouldReturnCourse_whenExists() {
        // Given
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(courseMapper.toResponse(any(Course.class))).thenReturn(courseResponse);

        // When
        CourseResponse result = courseService.getCourseById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(courseRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    void getCourseById_shouldThrowEntityNotFoundException_whenNotExists() {
        // Given
        when(courseRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> courseService.getCourseById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Course");

        verify(courseRepository).findByIdAndDeletedFalse(999L);
    }

    @Test
    void getCourses_shouldReturnPageOfCourses() {
        // Given
        CourseSearchCriteria criteria = new CourseSearchCriteria("test", "DRAFT", 1L, 0, 20, "createdAt,desc");
        Page<Course> coursePage = new PageImpl<>(List.of(course));

        when(courseRepository.findBySearchCriteria(anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(coursePage);
        when(courseMapper.toResponse(any(Course.class))).thenReturn(courseResponse);

        // When
        PageResponse<CourseResponse> result = courseService.getCourses(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(courseRepository).findBySearchCriteria(
                eq("test"),
                eq(CourseStatus.DRAFT),
                eq(1L),
                any(Pageable.class)
        );
    }

    @Test
    void updateCourse_shouldUpdateSuccessfully() {
        // Given
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toResponse(any(Course.class))).thenReturn(courseResponse);

        // When
        CourseResponse result = courseService.updateCourse(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseMapper).updateEntity(eq(course), eq(updateRequest));
        verify(courseRepository).save(course);
    }

    @Test
    void updateCourse_shouldThrowEntityNotFoundException_whenNotExists() {
        // Given
        when(courseRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> courseService.updateCourse(999L, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Course");

        verify(courseRepository).findByIdAndDeletedFalse(999L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateCourse_shouldThrowValidationException_whenCourseIsArchived() {
        // Given
        Course archivedCourse = CourseTestDataBuilder.createArchivedCourse();
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(archivedCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.updateCourse(1L, updateRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ARCHIVED");

        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateCourse_shouldThrowValidationException_whenUpdatingRestrictedFieldsOnPublishedCourse() {
        // Given
        Course publishedCourse = CourseTestDataBuilder.createPublishedCourse();
        UpdateCourseRequest restrictedUpdate = new UpdateCourseRequest(
                "New Name", // name is restricted for PUBLISHED
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(publishedCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.updateCourse(1L, restrictedUpdate))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PUBLISHED");

        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void deleteCourse_shouldDeleteSuccessfully() {
        // Given
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        // When
        courseService.deleteCourse(1L);

        // Then
        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository).save(argThat(c -> c.isDeleted()));
    }

    @Test
    void deleteCourse_shouldThrowValidationException_whenCourseIsPublished() {
        // Given
        Course publishedCourse = CourseTestDataBuilder.createPublishedCourse();
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(publishedCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.deleteCourse(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DRAFT");

        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void publishCourse_shouldChangeStatusToPublished() {
        // Given
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toResponse(any(Course.class))).thenReturn(courseResponse);

        // When
        CourseResponse result = courseService.publishCourse(1L);

        // Then
        assertThat(result).isNotNull();
        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository).save(argThat(c -> c.getStatus() == CourseStatus.PUBLISHED));
    }

    @Test
    void publishCourse_shouldThrowValidationException_whenCourseIsNotDraft() {
        // Given
        Course publishedCourse = CourseTestDataBuilder.createPublishedCourse();
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(publishedCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.publishCourse(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DRAFT");

        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void publishCourse_shouldThrowValidationException_whenRequiredFieldsMissing() {
        // Given
        Course incompleteCourse = CourseTestDataBuilder.createDefaultCourse();
        incompleteCourse.setSyllabus(null); // Missing required field
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(incompleteCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.publishCourse(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("syllabus");

        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void archiveCourse_shouldChangeStatusToArchived() {
        // Given
        Course publishedCourse = CourseTestDataBuilder.createPublishedCourse();
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(publishedCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(publishedCourse);
        when(courseMapper.toResponse(any(Course.class))).thenReturn(courseResponse);

        // When
        CourseResponse result = courseService.archiveCourse(1L);

        // Then
        assertThat(result).isNotNull();
        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository).save(argThat(c -> c.getStatus() == CourseStatus.ARCHIVED));
    }

    @Test
    void archiveCourse_shouldThrowValidationException_whenCourseIsNotPublished() {
        // Given
        Course draftCourse = CourseTestDataBuilder.createDefaultCourse();
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(draftCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.archiveCourse(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PUBLISHED");

        verify(courseRepository).findByIdAndDeletedFalse(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }
}
