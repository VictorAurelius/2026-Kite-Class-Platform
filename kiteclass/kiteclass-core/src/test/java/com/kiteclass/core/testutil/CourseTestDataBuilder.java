package com.kiteclass.core.testutil;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.entity.Course;

import java.math.BigDecimal;

/**
 * Test data builder for Course-related objects.
 *
 * <p>Provides factory methods to create test data for:
 * <ul>
 *   <li>Course entities</li>
 *   <li>CreateCourseRequest DTOs</li>
 *   <li>UpdateCourseRequest DTOs</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
public class CourseTestDataBuilder {

    /**
     * Creates a default Course entity for testing.
     *
     * @return Course with default test data
     */
    public static Course createDefaultCourse() {
        Course course = Course.builder()
                .name("English for Business Communication")
                .code("ENG-B1-001")
                .description("Learn professional English for business contexts")
                .syllabus("Week 1: Introduction\nWeek 2: Business Vocabulary\nWeek 3: Writing Emails")
                .objectives("Students will be able to:\n- Communicate effectively in business\n- Write professional emails")
                .prerequisites("Basic English (A2 level)")
                .targetAudience("Working professionals")
                .teacherId(1L)
                .durationWeeks(12)
                .totalSessions(36)
                .price(new BigDecimal("5000000.00"))
                .status(CourseStatus.DRAFT)
                .coverImageUrl("https://example.com/course-cover.jpg")
                .build();
        course.setId(1L);
        course.setDeleted(false);
        return course;
    }

    /**
     * Creates a Course entity with custom name.
     *
     * @param name the course name
     * @return Course with specified name
     */
    public static Course createCourseWithName(String name) {
        Course course = createDefaultCourse();
        course.setName(name);
        return course;
    }

    /**
     * Creates a Course entity with custom code.
     *
     * @param code the course code
     * @return Course with specified code
     */
    public static Course createCourseWithCode(String code) {
        Course course = createDefaultCourse();
        course.setCode(code);
        return course;
    }

    /**
     * Creates a Course entity with custom status.
     *
     * @param status the course status
     * @return Course with specified status
     */
    public static Course createCourseWithStatus(CourseStatus status) {
        Course course = createDefaultCourse();
        course.setStatus(status);
        return course;
    }

    /**
     * Creates a Course entity with custom teacher ID.
     *
     * @param teacherId the teacher ID
     * @return Course with specified teacher ID
     */
    public static Course createCourseWithTeacher(Long teacherId) {
        Course course = createDefaultCourse();
        course.setTeacherId(teacherId);
        return course;
    }

    /**
     * Creates a PUBLISHED Course entity.
     *
     * @return Course with PUBLISHED status
     */
    public static Course createPublishedCourse() {
        return createCourseWithStatus(CourseStatus.PUBLISHED);
    }

    /**
     * Creates an ARCHIVED Course entity.
     *
     * @return Course with ARCHIVED status
     */
    public static Course createArchivedCourse() {
        return createCourseWithStatus(CourseStatus.ARCHIVED);
    }

    /**
     * Creates a default CreateCourseRequest for testing.
     *
     * @return CreateCourseRequest with default test data
     */
    public static CreateCourseRequest createDefaultCreateRequest() {
        return new CreateCourseRequest(
                "TOEIC 600+ Preparation",
                "TOEIC-600",
                "Comprehensive TOEIC preparation course",
                "Week 1: Listening basics\nWeek 2: Reading comprehension",
                "Students will achieve TOEIC score of 600+",
                "Basic English understanding",
                "Students preparing for TOEIC exam",
                1L,
                8,
                24,
                new BigDecimal("3000000.00")
        );
    }

    /**
     * Creates a CreateCourseRequest with custom code.
     *
     * @param code the course code
     * @return CreateCourseRequest with specified code
     */
    public static CreateCourseRequest createRequestWithCode(String code) {
        return new CreateCourseRequest(
                "Test Course",
                code,
                "Test description",
                "Test syllabus",
                "Test objectives",
                "Test prerequisites",
                "Test audience",
                1L,
                10,
                30,
                new BigDecimal("4000000.00")
        );
    }

    /**
     * Creates a CreateCourseRequest with custom teacher ID.
     *
     * @param teacherId the teacher ID
     * @return CreateCourseRequest with specified teacher ID
     */
    public static CreateCourseRequest createRequestWithTeacher(Long teacherId) {
        return new CreateCourseRequest(
                "Test Course",
                "TEST-001",
                "Test description",
                "Test syllabus",
                "Test objectives",
                "Test prerequisites",
                "Test audience",
                teacherId,
                10,
                30,
                new BigDecimal("4000000.00")
        );
    }

    /**
     * Creates a CreateCourseRequest with minimal required fields.
     *
     * @return CreateCourseRequest with only required fields
     */
    public static CreateCourseRequest createMinimalRequest() {
        return new CreateCourseRequest(
                "Minimal Course",
                "MIN-001",
                null,
                null,
                null,
                null,
                null,
                1L,
                null,
                null,
                null
        );
    }

    /**
     * Creates a default UpdateCourseRequest for testing.
     *
     * @return UpdateCourseRequest with default test data
     */
    public static UpdateCourseRequest createDefaultUpdateRequest() {
        return new UpdateCourseRequest(
                "Updated Course Name",
                "Updated description",
                "Updated syllabus",
                "Updated objectives",
                "Updated prerequisites",
                "Updated target audience",
                15,
                45,
                new BigDecimal("6000000.00"),
                "https://example.com/updated-cover.jpg"
        );
    }

    /**
     * Creates an UpdateCourseRequest with limited fields (PUBLISHED course).
     *
     * @return UpdateCourseRequest with only fields allowed for PUBLISHED courses
     */
    public static UpdateCourseRequest createLimitedUpdateRequest() {
        return new UpdateCourseRequest(
                null, // name not allowed for PUBLISHED
                "Updated description only",
                "Updated syllabus only",
                "Updated objectives only",
                null, // prerequisites not allowed for PUBLISHED
                null, // targetAudience not allowed for PUBLISHED
                null, // durationWeeks not allowed for PUBLISHED
                null, // totalSessions not allowed for PUBLISHED
                new BigDecimal("5500000.00"),
                "https://example.com/new-cover.jpg"
        );
    }

    /**
     * Creates an UpdateCourseRequest with custom description.
     *
     * @param description the description
     * @return UpdateCourseRequest with specified description
     */
    public static UpdateCourseRequest createUpdateRequestWithDescription(String description) {
        return new UpdateCourseRequest(
                null,
                description,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
