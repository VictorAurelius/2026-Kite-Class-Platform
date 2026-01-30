package com.kiteclass.core.module.student.entity;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Student entity representing a student in the system.
 *
 * <p>Tracks student information including:
 * <ul>
 *   <li>Personal details (name, email, phone, date of birth, gender)</li>
 *   <li>Contact information (address)</li>
 *   <li>Avatar URL for profile picture</li>
 *   <li>Current status (ACTIVE, INACTIVE, GRADUATED, etc.)</li>
 *   <li>Additional notes</li>
 * </ul>
 *
 * <p>Extends {@link BaseEntity} for audit fields and soft delete support.
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_students_email", columnList = "email"),
        @Index(name = "idx_students_phone", columnList = "phone"),
        @Index(name = "idx_students_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseEntity {

    /**
     * Student's full name.
     * Required field, max 100 characters.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Student's email address.
     * Unique per student, max 255 characters.
     * Used for communication and login (if applicable).
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Student's phone number.
     * Format: Vietnamese phone (10 digits starting with 0).
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Student's date of birth.
     * Used for age calculation and birthday notifications.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Student's gender.
     * Values: MALE, FEMALE, OTHER.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    /**
     * Student's address.
     * Stored as TEXT for detailed address information.
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * URL to student's avatar/profile picture.
     * Max 500 characters.
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Current status of the student.
     * Values: PENDING, ACTIVE, INACTIVE, GRADUATED, DROPPED.
     * Default: ACTIVE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    /**
     * Additional notes about the student.
     * Stored as TEXT for unlimited content.
     */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Relationships will be added when implementing other modules
    // @OneToMany(mappedBy = "student")
    // private List<Enrollment> enrollments;
    //
    // @OneToMany(mappedBy = "student")
    // private List<Attendance> attendances;
    //
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "parent_id")
    // private Parent parent;
}
