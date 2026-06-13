package com.kiteclass.core.module.course.service;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.mapper.CourseMapper;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.course.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CourseServiceImpl#unpublishCourse(Long)} (PUBLISHED → DRAFT).
 *
 * @since GAP-1113 LMS Phase0-BE
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService.unpublishCourse Tests")
class CourseUnpublishServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course publishedCourse() {
        Course course = Course.builder()
                .name("Test Course")
                .code("TEST-UNP-1")
                .teacherId(100L)
                .status(CourseStatus.PUBLISHED)
                .build();
        course.setId(1L);
        return course;
    }

    @Test
    @DisplayName("PUBLISHED course reverts to DRAFT and is saved")
    void unpublish_published_revertsToDraft() {
        Course course = publishedCourse();
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        courseService.unpublishCourse(1L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("DRAFT course cannot be unpublished -> ValidationException")
    void unpublish_draft_throws() {
        Course course = publishedCourse();
        course.setStatus(CourseStatus.DRAFT);
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.unpublishCourse(1L))
                .isInstanceOf(ValidationException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("ARCHIVED course cannot be unpublished -> ValidationException")
    void unpublish_archived_throws() {
        Course course = publishedCourse();
        course.setStatus(CourseStatus.ARCHIVED);
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.unpublishCourse(1L))
                .isInstanceOf(ValidationException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Missing course -> EntityNotFoundException")
    void unpublish_notFound_throws() {
        when(courseRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.unpublishCourse(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(courseRepository, never()).save(any());
    }
}
