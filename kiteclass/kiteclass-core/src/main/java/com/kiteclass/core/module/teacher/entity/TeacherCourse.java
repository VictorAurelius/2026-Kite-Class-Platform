package com.kiteclass.core.module.teacher.entity;

import com.kiteclass.core.common.constant.TeacherCourseRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * TeacherCourse entity representing course-level assignment between teacher and course.
 *
 * <p>This entity controls course-level permissions:
 * <ul>
 *   <li>CREATOR: Teacher who created the course, full control over course and all classes</li>
 *   <li>INSTRUCTOR: Teacher assigned to teach the course, can manage classes and students</li>
 *   <li>ASSISTANT: Teaching assistant with view-only permissions</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-TEACHER-006: Course creator (CREATOR role) has full control</li>
 *   <li>BR-TEACHER-007: Teacher can have different roles in different courses</li>
 *   <li>Unique constraint: One teacher cannot have multiple roles in same course</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Entity
@Table(name = "teacher_courses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_teacher_courses_teacher_course",
                        columnNames = {"teacher_id", "course_id"})
        },
        indexes = {
                @Index(name = "idx_teacher_courses_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_teacher_courses_course_id", columnList = "course_id"),
                @Index(name = "idx_teacher_courses_role", columnList = "role")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourse {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to teachers table.
     * The teacher assigned to the course.
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * Foreign key to courses table.
     * The course that the teacher is assigned to.
     */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /**
     * Role of the teacher in the course.
     * Values: CREATOR, INSTRUCTOR, ASSISTANT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private TeacherCourseRole role;

    /**
     * Timestamp when the teacher was assigned to the course.
     */
    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    /**
     * ID of the user who assigned the teacher to the course.
     * NULL if self-created (CREATOR role).
     */
    @Column(name = "assigned_by")
    private Long assignedBy;

    // Relationships will be added when implementing Course module
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    // private Teacher teacher;
    //
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "course_id", insertable = false, updatable = false)
    // private Course course;
}
