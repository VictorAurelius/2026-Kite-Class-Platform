package com.kiteclass.core.module.course.service;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.CourseSearchCriteria;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.service.TeacherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OWASP Top 10 security tests for Course module.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>SQL Injection prevention (5 tests)</li>
 *   <li>XSS prevention (3 tests)</li>
 *   <li>Input validation (3 tests)</li>
 *   <li>Business rule enforcement (4 tests)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@Rollback(true)
class CourseSecurityTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private CourseRepository courseRepository;

    private UUID tenantId;
    private Long defaultTeacherId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        // Create default teacher for tests
        CreateTeacherRequest teacherRequest = new CreateTeacherRequest(
            "Dr. Test", "test@example.com", "0901234567",
            "Computer Science", null, null, 10
        );
        TeacherResponse teacher = teacherService.createTeacher(teacherRequest);
        defaultTeacherId = teacher.id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========================================================================
    // SQL Injection Prevention Tests (5 tests)
    // ========================================================================

    @Test
    @DisplayName("Should prevent SQL injection via search parameter")
    void shouldPreventSqlInjection_viaSearch() {
        // Given: Existing course
        createCourse("JAVA101", "Java Programming", defaultTeacherId);

        // When: Search with SQL injection payload
        String maliciousInput = "'; DROP TABLE courses; --";
        CourseSearchCriteria criteria = new CourseSearchCriteria(
            maliciousInput, null, null, 0, 20, null
        );
        PageResponse<CourseResponse> result = courseService.getCourses(criteria);

        // Then: Should return empty (no match), not execute SQL
        assertThat(result.getContent()).isEmpty();

        // Verify: Table still exists with data
        assertThat(courseRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should use parameterized queries for create")
    void shouldUseParameterizedQueries_create() {
        // Given: Input with SQL special characters
        String code = "SQLCOURSE";
        String name = "SQL Programming with Special Chars";
        String description = "Test & Learn SQL safely";

        CreateCourseRequest request = new CreateCourseRequest(
            name, code, description,
            null, null, null, null,
            defaultTeacherId, 12, 24, BigDecimal.valueOf(1000000),
            null, null
        );

        // When: Create course
        CourseResponse response = courseService.createCourse(request);

        // Then: Should be saved safely with special characters preserved
        assertThat(response.code()).isEqualTo("SQLCOURSE");
        assertThat(response.name()).isEqualTo("SQL Programming with Special Chars");
        assertThat(response.description()).contains("&");

        // Verify: No SQL injection occurred
        assertThat(courseRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should use parameterized queries for update")
    void shouldUseParameterizedQueries_update() {
        // Given: Existing course
        CourseResponse course = createCourse("PYTHON101", "Python Basics", defaultTeacherId);

        // When: Update with SQL injection attempt
        String maliciousName = "'; UPDATE courses SET deleted=true WHERE '1'='1";
        UpdateCourseRequest request = new UpdateCourseRequest(
            maliciousName, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        CourseResponse updated = courseService.updateCourse(course.id(), request);

        // Then: Name updated safely, no SQL injection
        assertThat(updated.name()).isEqualTo(maliciousName);

        // Verify: Other courses not affected
        assertThat(courseRepository.count()).isEqualTo(1);
        assertThat(courseRepository.findById(course.id())).isPresent();
    }

    @Test
    @DisplayName("Should prevent SQL injection via code search")
    void shouldPreventSqlInjection_viaCode() {
        // Given: Course with normal code
        createCourse("WEB101", "Web Development", defaultTeacherId);

        // When: Search with SQL injection in code
        String maliciousSearch = "WEB101' OR '1'='1";
        CourseSearchCriteria criteria = new CourseSearchCriteria(
            maliciousSearch, null, null, 0, 10, null
        );
        PageResponse<CourseResponse> result = courseService.getCourses(criteria);

        // Then: Should not return all records (SQL injection blocked)
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should prevent SQL injection via status filter")
    void shouldPreventSqlInjection_viaStatus() {
        // Given: Course with DRAFT status
        createCourse("MOBILE101", "Mobile Development", defaultTeacherId);

        // When: Filter with SQL injection in status
        String maliciousStatus = "DRAFT'; DROP TABLE courses; --";
        CourseSearchCriteria criteria = new CourseSearchCriteria(
            null, maliciousStatus, null, 0, 20, null
        );

        // Then: Should handle invalid status gracefully (no SQL injection)
        assertThatThrownBy(() -> courseService.getCourses(criteria))
            .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("status"));

        // Verify: Table still exists
        assertThat(courseRepository.count()).isEqualTo(1);
    }

    // ========================================================================
    // XSS Prevention Tests (3 tests)
    // ========================================================================

    @Test
    @DisplayName("Should handle XSS payload in name field")
    void shouldHandleXssPayload_inName() {
        // Given: XSS payload in name
        String xssPayload = "<script>alert('XSS')</script>";

        CreateCourseRequest request = new CreateCourseRequest(
            xssPayload, "XSS101", "Security Testing",
            null, null, null, null,
            defaultTeacherId, 8, 16, BigDecimal.valueOf(500000),
            null, null  // level, category
        );

        // When: Create course
        CourseResponse response = courseService.createCourse(request);

        // Then: Name stored (will be escaped by frontend/API layer)
        assertThat(response.name()).isNotNull();
        // Backend stores raw value, XSS prevention is frontend responsibility
        assertThat(response.name()).contains("script");
    }

    @Test
    @DisplayName("Should handle XSS payload in description field")
    void shouldHandleXssPayload_inDescription() {
        // Given: Course with XSS in description
        String xssDescription = "<img src=x onerror='alert(1)'>";

        CreateCourseRequest request = new CreateCourseRequest(
            "HTML Basics", "HTML101", xssDescription,
            null, null, null, null,
            defaultTeacherId, 6, 12, BigDecimal.valueOf(300000),
            null, null  // level, category
        );

        // When: Create course
        CourseResponse response = courseService.createCourse(request);

        // Then: Description stored safely
        assertThat(response.description()).isNotNull();
        assertThat(response.description()).contains("<img");
    }

    @Test
    @DisplayName("Should handle XSS payload in syllabus field")
    void shouldHandleXssPayload_inSyllabus() {
        // Given: Existing course
        CourseResponse course = createCourse("CSS101", "CSS Styling", defaultTeacherId);

        // When: Update with XSS in syllabus
        String xssSyllabus = "<iframe src='javascript:alert(1)'></iframe>";
        UpdateCourseRequest request = new UpdateCourseRequest(
            null, null, null, null, xssSyllabus, null, null, null, null, null, null, null,
            null, null  // level, category
        );

        CourseResponse updated = courseService.updateCourse(course.id(), request);

        // Then: Syllabus stored (XSS handled by output encoding)
        assertThat(updated.syllabus()).isNotNull();
        assertThat(updated.syllabus()).contains("iframe");
    }

    // ========================================================================
    // Input Validation Tests (3 tests)
    // ========================================================================

    @Test
    @DisplayName("Should enforce code uniqueness")
    void shouldEnforceCodeUniqueness() {
        // Given: Existing course
        createCourse("UNIQUE101", "Unique Course", defaultTeacherId);

        // When: Try to create another course with same code
        CreateCourseRequest request = new CreateCourseRequest(
            "Another Course", "UNIQUE101", "Description",
            null, null, null, null,
            defaultTeacherId, 10, 20, BigDecimal.valueOf(800000),
            null, null  // level, category
        );

        // Then: Should throw DuplicateResourceException
        assertThatThrownBy(() -> courseService.createCourse(request))
            .isInstanceOf(DuplicateResourceException.class)
            .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("code"));
    }

    @Test
    @DisplayName("Should validate duration constraints")
    void shouldValidateDurationConstraints() {
        // Given: Invalid duration (0 weeks)
        CreateCourseRequest request = new CreateCourseRequest(
            "Invalid Course", "INVALID101", "Description",
            null, null, null, null,
            defaultTeacherId, 0, 0, BigDecimal.valueOf(500000),
            null, null  // level, category
        );

        // When/Then: Should throw validation exception
        assertThatThrownBy(() -> courseService.createCourse(request))
            .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("duration"));
    }

    @Test
    @DisplayName("Should validate price is non-negative")
    void shouldValidatePrice() {
        // Given: Negative price
        CreateCourseRequest request = new CreateCourseRequest(
            "Price Test", "PRICE101", "Description",
            null, null, null, null,
            defaultTeacherId, 8, 16, BigDecimal.valueOf(-1000),
            null, null  // level, category
        );

        // When/Then: Should throw validation exception
        assertThatThrownBy(() -> courseService.createCourse(request))
            .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("price"));
    }

    // ========================================================================
    // Business Rule Enforcement Tests (4 tests)
    // ========================================================================

    @Test
    @DisplayName("Should prevent update of ARCHIVED course")
    void shouldPreventUpdateOfArchivedCourse() {
        // Given: Course in ARCHIVED status
        CourseResponse course = createCourse("ARCH101", "Archived Course", defaultTeacherId);
        // Manually set to ARCHIVED (in real scenario, would use publishCourse then archiveCourse)
        courseRepository.findById(course.id()).ifPresent(c -> {
            c.setStatus(CourseStatus.ARCHIVED);
            courseRepository.save(c);
        });

        // When: Try to update archived course
        UpdateCourseRequest request = new UpdateCourseRequest(
            "Updated Name", null, null, null, null, null, null, null, null, null, null, null,
            null, null  // level, category
        );

        // Then: Should throw ValidationException
        assertThatThrownBy(() -> courseService.updateCourse(course.id(), request))
            .isInstanceOf(ValidationException.class)
            .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("ARCHIVED"));
    }

    @Test
    @DisplayName("Should restrict updates for PUBLISHED course")
    void shouldRestrictUpdatesForPublishedCourse() {
        // Given: Course in PUBLISHED status
        CourseResponse course = createCourse("PUB101", "Published Course", defaultTeacherId);
        courseRepository.findById(course.id()).ifPresent(c -> {
            c.setStatus(CourseStatus.PUBLISHED);
            courseRepository.save(c);
        });

        // When: Try to update restricted field (durationWeeks) on published course
        UpdateCourseRequest request = new UpdateCourseRequest(
            null, null, null, null, null, null, null, null, 20, null, null, null,
            null, null  // level, category
        );

        // Then: Should throw ValidationException
        assertThatThrownBy(() -> courseService.updateCourse(course.id(), request))
            .isInstanceOf(ValidationException.class)
            .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("PUBLISHED"));
    }

    @Test
    @DisplayName("Should allow description update for PUBLISHED course")
    void shouldAllowDescriptionUpdateForPublishedCourse() {
        // Given: Course in PUBLISHED status
        CourseResponse course = createCourse("PUBDESC101", "Published Course", defaultTeacherId);
        courseRepository.findById(course.id()).ifPresent(c -> {
            c.setStatus(CourseStatus.PUBLISHED);
            courseRepository.save(c);
        });

        // When: Update allowed field (description) on published course
        UpdateCourseRequest request = new UpdateCourseRequest(
            null, null, null, "Updated description for published course", null, null, null, null, null, null, null, null,
            null, null  // level, category
        );

        CourseResponse updated = courseService.updateCourse(course.id(), request);

        // Then: Should succeed
        assertThat(updated.description()).isEqualTo("Updated description for published course");
    }

    @Test
    @DisplayName("Should prevent deletion of PUBLISHED course")
    void shouldPreventDeletionOfPublishedCourse() {
        // Given: Course in PUBLISHED status
        CourseResponse course = createCourse("PUBDEL101", "Published Course", defaultTeacherId);
        courseRepository.findById(course.id()).ifPresent(c -> {
            c.setStatus(CourseStatus.PUBLISHED);
            courseRepository.save(c);
        });

        // When: Try to delete published course
        // Then: Should throw ValidationException
        assertThatThrownBy(() -> courseService.deleteCourse(course.id()))
            .isInstanceOf(ValidationException.class)
            .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("COURSE_CANNOT_DELETE_STATUS"));
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Helper method to create a course for testing.
     */
    private CourseResponse createCourse(String code, String name, Long teacherId) {
        CreateCourseRequest request = new CreateCourseRequest(
            name, code, "Test course description",
            null, null, null, null,
            teacherId, 10, 20, BigDecimal.valueOf(1000000),
            null, null  // level, category
        );
        return courseService.createCourse(request);
    }
}
