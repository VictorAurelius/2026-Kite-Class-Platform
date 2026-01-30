package com.kiteclass.core.module.teacher.entity;

import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Teacher entity representing a teacher in the system.
 *
 * <p>Tracks teacher information including:
 * <ul>
 *   <li>Personal details (name, email, phone)</li>
 *   <li>Professional details (specialization, qualification, experience)</li>
 *   <li>Bio and avatar for profile</li>
 *   <li>Current status (ACTIVE, INACTIVE, ON_LEAVE)</li>
 * </ul>
 *
 * <p>Extends {@link BaseEntity} for audit fields and soft delete support.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-TEACHER-001: Email must be unique</li>
 *   <li>BR-TEACHER-005: Only ACTIVE teachers can be assigned to new classes/courses</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Entity
@Table(name = "teachers", indexes = {
        @Index(name = "idx_teachers_email", columnList = "email"),
        @Index(name = "idx_teachers_status", columnList = "status"),
        @Index(name = "idx_teachers_specialization", columnList = "specialization")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Teacher extends BaseEntity {

    /**
     * Teacher's full name.
     * Required field, max 100 characters.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Teacher's email address.
     * Unique per teacher, max 255 characters.
     * Used for communication and linked to Gateway User.
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Teacher's phone number.
     * Format: Vietnamese phone (10 digits starting with 0).
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Teacher's specialization/subject area.
     * Examples: "English", "Math", "Science", "TOEIC Preparation".
     * Max 100 characters.
     */
    @Column(name = "specialization", length = 100)
    private String specialization;

    /**
     * Teacher's biography/introduction.
     * Stored as TEXT for detailed information.
     * Max 2000 characters.
     */
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /**
     * Teacher's qualification/education background.
     * Examples: "Bachelor in Education", "Master in English Literature".
     * Max 200 characters.
     */
    @Column(name = "qualification", length = 200)
    private String qualification;

    /**
     * Years of teaching experience.
     * Must be >= 0.
     */
    @Column(name = "experience_years")
    private Integer experienceYears;

    /**
     * URL to teacher's avatar/profile picture.
     * Max 500 characters.
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Current status of the teacher.
     * Values: ACTIVE, INACTIVE, ON_LEAVE.
     * Default: ACTIVE.
     * Only ACTIVE teachers can be assigned to new classes/courses.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TeacherStatus status = TeacherStatus.ACTIVE;

    // Relationships will be added when implementing other modules
    // @OneToMany(mappedBy = "teacher")
    // private List<TeacherCourse> teacherCourses;
    //
    // @OneToMany(mappedBy = "teacher")
    // private List<TeacherClass> teacherClasses;
}
