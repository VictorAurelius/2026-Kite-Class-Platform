package com.kiteclass.core.module.course.mapper;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CourseMapper}.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@SpringBootTest
@ActiveProfiles("test")
class CourseMapperTest {

    @Autowired
    private CourseMapper courseMapper;

    @Test
    void toResponse_shouldMapCorrectly() {
        // Given
        Course course = CourseTestDataBuilder.createDefaultCourse();

        // When
        CourseResponse response = courseMapper.toResponse(course);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(course.getId());
        assertThat(response.name()).isEqualTo(course.getName());
        assertThat(response.code()).isEqualTo(course.getCode());
        assertThat(response.description()).isEqualTo(course.getDescription());
        assertThat(response.syllabus()).isEqualTo(course.getSyllabus());
        assertThat(response.objectives()).isEqualTo(course.getObjectives());
        assertThat(response.prerequisites()).isEqualTo(course.getPrerequisites());
        assertThat(response.targetAudience()).isEqualTo(course.getTargetAudience());
        assertThat(response.teacherId()).isEqualTo(course.getTeacherId());
        assertThat(response.durationWeeks()).isEqualTo(course.getDurationWeeks());
        assertThat(response.totalSessions()).isEqualTo(course.getTotalSessions());
        assertThat(response.price()).isEqualTo(course.getPrice());
        assertThat(response.status()).isEqualTo(course.getStatus().name());
        assertThat(response.coverImageUrl()).isEqualTo(course.getCoverImageUrl());
    }

    @Test
    void toEntity_shouldMapCorrectly() {
        // Given
        CreateCourseRequest request = CourseTestDataBuilder.createDefaultCreateRequest();

        // When
        Course course = courseMapper.toEntity(request);

        // Then
        assertThat(course).isNotNull();
        assertThat(course.getName()).isEqualTo(request.name());
        assertThat(course.getCode()).isEqualTo(request.code());
        assertThat(course.getDescription()).isEqualTo(request.description());
        assertThat(course.getSyllabus()).isEqualTo(request.syllabus());
        assertThat(course.getObjectives()).isEqualTo(request.objectives());
        assertThat(course.getPrerequisites()).isEqualTo(request.prerequisites());
        assertThat(course.getTargetAudience()).isEqualTo(request.targetAudience());
        assertThat(course.getTeacherId()).isEqualTo(request.teacherId());
        assertThat(course.getDurationWeeks()).isEqualTo(request.durationWeeks());
        assertThat(course.getTotalSessions()).isEqualTo(request.totalSessions());
        assertThat(course.getPrice()).isEqualTo(request.price());
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT); // Default status
        assertThat(course.getCoverImageUrl()).isNull(); // Not set during creation
    }

    @Test
    void updateEntity_shouldUpdateOnlyNonNullFields() {
        // Given
        Course course = CourseTestDataBuilder.createDefaultCourse();
        String originalCode = course.getCode();
        Long originalTeacherId = course.getTeacherId();
        CourseStatus originalStatus = course.getStatus();

        UpdateCourseRequest request = new UpdateCourseRequest(
                "Updated Name",
                "Updated description",
                null,  // Should not update syllabus
                "Updated objectives",
                null,  // Should not update prerequisites
                null,  // Should not update target audience
                15,    // Update duration
                null,  // Should not update total sessions
                new BigDecimal("6000000.00"),
                "https://example.com/new-cover.jpg"
        );

        // When
        courseMapper.updateEntity(course, request);

        // Then
        assertThat(course.getName()).isEqualTo("Updated Name");
        assertThat(course.getDescription()).isEqualTo("Updated description");
        assertThat(course.getSyllabus()).isNotEqualTo("Updated objectives"); // Syllabus unchanged
        assertThat(course.getObjectives()).isEqualTo("Updated objectives");
        assertThat(course.getDurationWeeks()).isEqualTo(15);
        assertThat(course.getPrice()).isEqualByComparingTo(new BigDecimal("6000000.00"));
        assertThat(course.getCoverImageUrl()).isEqualTo("https://example.com/new-cover.jpg");

        // These should not change
        assertThat(course.getCode()).isEqualTo(originalCode);
        assertThat(course.getTeacherId()).isEqualTo(originalTeacherId);
        assertThat(course.getStatus()).isEqualTo(originalStatus);
    }

    @Test
    void updateEntity_shouldNotUpdateCodeTeacherIdOrStatus() {
        // Given
        Course course = CourseTestDataBuilder.createDefaultCourse();
        String originalCode = course.getCode();
        Long originalTeacherId = course.getTeacherId();
        CourseStatus originalStatus = course.getStatus();

        UpdateCourseRequest request = new UpdateCourseRequest(
                "New Name",
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

        // When
        courseMapper.updateEntity(course, request);

        // Then
        assertThat(course.getName()).isEqualTo("New Name");
        // These protected fields should remain unchanged
        assertThat(course.getCode()).isEqualTo(originalCode);
        assertThat(course.getTeacherId()).isEqualTo(originalTeacherId);
        assertThat(course.getStatus()).isEqualTo(originalStatus);
    }
}
