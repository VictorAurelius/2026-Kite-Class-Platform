package com.kiteclass.core.module.course.entity;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Course entity representing a course in the system.
 *
 * <p>Tracks course information including:
 * <ul>
 *   <li>Basic information (name, code, description)</li>
 *   <li>Educational content (syllabus, objectives, prerequisites)</li>
 *   <li>Course details (duration, sessions, target audience)</li>
 *   <li>Teacher assignment (created_by/teacher_id as foreign key)</li>
 *   <li>Pricing and cover image</li>
 *   <li>Lifecycle status (DRAFT, PUBLISHED, ARCHIVED)</li>
 * </ul>
 *
 * <p>Extends {@link BaseEntity} for audit fields and soft delete support.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-COURSE-001: Course code must be unique</li>
 *   <li>BR-COURSE-002: Status transition rules apply (DRAFT can edit all, PUBLISHED limited edits, ARCHIVED read-only)</li>
 *   <li>BR-COURSE-003: Auto-create TeacherCourse with role=CREATOR when course created</li>
 *   <li>BR-COURSE-004: Cannot delete if has active classes</li>
 * </ul>
 *
 * <p>Relationships:
 * <ul>
 *   <li>Course → Teacher (many-to-one via teacher_id/created_by)</li>
 *   <li>Course → TeacherCourse (one-to-many, managed by Teacher module)</li>
 *   <li>Course → Class (one-to-many, will be added in Class module)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_courses_code", columnList = "code", unique = true),
        @Index(name = "idx_courses_status", columnList = "status"),
        @Index(name = "idx_courses_teacher_id", columnList = "teacher_id"),
        @Index(name = "idx_courses_name", columnList = "name"),
        @Index(name = "idx_courses_deleted", columnList = "deleted")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course extends BaseEntity {

    /**
     * Course name.
     * Required field, max 200 characters.
     * Example: "English for Business Communication"
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Course code (unique identifier).
     * Required field, unique, max 50 characters.
     * Example: "ENG-B1-001", "TOEIC-600"
     * Used for quick lookup and enrollment.
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Course description.
     * TEXT field for detailed course information.
     * Max 5000 characters recommended.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Course syllabus.
     * TEXT field containing week-by-week course content.
     * Example: "Week 1: Introduction\nWeek 2: Basic Grammar\n..."
     * Max 10000 characters recommended.
     */
    @Column(name = "syllabus", columnDefinition = "TEXT")
    private String syllabus;

    /**
     * Learning objectives.
     * TEXT field describing what students will learn.
     * Example: "Students will be able to:\n- Communicate in business contexts\n- Write professional emails\n..."
     * Max 5000 characters recommended.
     */
    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;

    /**
     * Prerequisites for the course.
     * TEXT field listing required knowledge or courses.
     * Example: "- Basic English (A2 level)\n- Completed Introduction to Business"
     * Max 2000 characters recommended.
     */
    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites;

    /**
     * Target audience description.
     * TEXT field describing who should take this course.
     * Example: "Working professionals who need to use English in business contexts"
     * Max 1000 characters recommended.
     */
    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;

    /**
     * Teacher ID who created the course.
     * Foreign key to teachers.id.
     * This is the course CREATOR who will be auto-assigned with role=CREATOR in teacher_courses.
     * Note: BaseEntity.createdBy also tracks this, but this field is explicit for course ownership.
     */
    @Column(name = "teacher_id")
    private Long teacherId;

    /**
     * Course duration in weeks.
     * Must be >= 1 if specified.
     * Example: 12 weeks, 8 weeks, etc.
     */
    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    /**
     * Total number of sessions in the course.
     * Must be >= 1 if specified.
     * Example: 36 sessions (3 sessions/week * 12 weeks)
     */
    @Column(name = "total_sessions")
    private Integer totalSessions;

    /**
     * Course price in VND (LEGACY — deprecated Wave br-4 per BR-COURSE-PRICING-001..003).
     * Must be >= 0. NULL or 0 means free course.
     * Precision: 15 digits total, 2 decimal places.
     * Example: 5000000.00 (5 million VND)
     *
     * Soft-deprecated Wave br-4: prefer {@link #pricingModel} + {@link #unitPrice} per ADR-035.
     * Field retained for backward compat (existing callers via CourseMapper + IT fixtures);
     * @Deprecated annotation omitted to avoid strict-warnings cascade.
     * Migration V67 backfills existing courses to PricingModel.COURSE_PACKAGE.
     */
    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    /**
     * Pricing model taxonomy per Wave br-4 ADR-035 (VN TT Anh ngữ market norm).
     * Values: PER_HOUR | MONTHLY | COURSE_PACKAGE | FREE.
     * Default PER_HOUR for NEW rows (ADR-035 mandate: VN TT Anh ngữ market bán theo giờ là chính).
     * V67 backfill set existing rows to COURSE_PACKAGE; V70 sets DB column default to PER_HOUR.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false, length = 32)
    @lombok.Builder.Default
    private PricingModel pricingModel = PricingModel.PER_HOUR;

    /**
     * Unit price in VND, semantics depends on {@link #pricingModel}:
     * <ul>
     *   <li>PER_HOUR: đồng/giờ (vd 200.000đ/giờ)</li>
     *   <li>MONTHLY: đồng/tháng (vd 1.500.000đ/tháng)</li>
     *   <li>COURSE_PACKAGE: đồng/khoá (vd 8.000.000đ/khoá, 1-shot)</li>
     *   <li>FREE: 0</li>
     * </ul>
     * Precision: 19 digits total, 2 decimal places (V67 migration).
     * Used by InvoiceGenerationService per BR-COURSE-PRICING-001..004.
     */
    @Column(name = "unit_price", precision = 19, scale = 2)
    @lombok.Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /**
     * Current lifecycle status of the course.
     * Values: DRAFT, PUBLISHED, ARCHIVED.
     * Default: DRAFT.
     * Controls what operations are allowed on the course.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    /**
     * URL to course cover image.
     * Max 500 characters.
     * Example: "https://cdn.kitehub.me/courses/eng-b1-001.jpg"
     */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /**
     * Courses that are prerequisites for this course.
     * Many-to-many self-referential relationship.
     * Example: "Algebra 2" has prerequisite course "Algebra 1".
     * Circular dependencies are prevented by application logic (DFS algorithm).
     */
    @ManyToMany
    @JoinTable(
        name = "course_prerequisites",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    @Builder.Default
    private Set<Course> prerequisiteCourses = new HashSet<>();

    /**
     * Courses that depend on this course (inverse relationship).
     * Many-to-many mappedBy relationship.
     * Example: If "Algebra 1" is prerequisite for "Algebra 2", then "Algebra 2" is in dependentCourses of "Algebra 1".
     * Not persisted, derived from prerequisiteCourses relationship.
     */
    @ManyToMany(mappedBy = "prerequisiteCourses")
    @Builder.Default
    private Set<Course> dependentCourses = new HashSet<>();

    /**
     * Course difficulty level.
     * Max 50 characters.
     * Examples: "Beginner", "Intermediate", "Advanced"
     * Used for filtering and student guidance.
     */
    @Column(name = "level", length = 50)
    private String level;

    /**
     * Course category/subject area.
     * Max 100 characters.
     * Examples: "Math", "Science", "Language", "Technology", "Business"
     * Used for course organization and filtering.
     */
    @Column(name = "category", length = 100)
    private String category;

    // Relationships will be added when implementing other modules
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    // private Teacher teacher;
    //
    // @OneToMany(mappedBy = "courseId")
    // private List<TeacherCourse> teacherCourses;
    //
    // @OneToMany(mappedBy = "courseId")
    // private List<Class> classes;

    /**
     * Adds a prerequisite to this course.
     * Updates bidirectional relationship (this.prerequisiteCourses + prerequisite.dependentCourses).
     *
     * @param prerequisite Course to add as prerequisite
     */
    public void addPrerequisite(Course prerequisite) {
        if (prerequisite != null) {
            this.prerequisiteCourses.add(prerequisite);
            prerequisite.getDependentCourses().add(this);
        }
    }

    /**
     * Removes a prerequisite from this course.
     * Updates bidirectional relationship (this.prerequisiteCourses + prerequisite.dependentCourses).
     *
     * @param prerequisite Course to remove as prerequisite
     */
    public void removePrerequisite(Course prerequisite) {
        if (prerequisite != null) {
            this.prerequisiteCourses.remove(prerequisite);
            prerequisite.getDependentCourses().remove(this);
        }
    }

    /**
     * Checks if course can be fully edited based on status.
     *
     * @return true if all fields can be edited (DRAFT status)
     */
    public boolean canFullyEdit() {
        return status != null && status.allowsFullEdit();
    }

    /**
     * Checks if course can be edited with limitations based on status.
     *
     * @return true if limited fields can be edited (PUBLISHED status)
     */
    public boolean canLimitedEdit() {
        return status != null && status.allowsLimitedEdit();
    }

    /**
     * Checks if course is read-only based on status.
     *
     * @return true if course cannot be edited (ARCHIVED status)
     */
    public boolean isReadOnly() {
        return status != null && status.isReadOnly();
    }

    /**
     * Checks if course can be deleted based on status.
     *
     * @return true if course can be deleted (DRAFT status only)
     */
    public boolean canBeDeleted() {
        return status != null && status.allowsDelete();
    }

    /**
     * Checks if course is in DRAFT status.
     *
     * @return true if status is DRAFT
     */
    public boolean isDraft() {
        return CourseStatus.DRAFT.equals(status);
    }

    /**
     * Checks if course is in PUBLISHED status.
     *
     * @return true if status is PUBLISHED
     */
    public boolean isPublished() {
        return CourseStatus.PUBLISHED.equals(status);
    }

    /**
     * Checks if course is in ARCHIVED status.
     *
     * @return true if status is ARCHIVED
     */
    public boolean isArchived() {
        return CourseStatus.ARCHIVED.equals(status);
    }
}
