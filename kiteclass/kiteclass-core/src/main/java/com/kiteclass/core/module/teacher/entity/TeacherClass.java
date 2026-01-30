package com.kiteclass.core.module.teacher.entity;

import com.kiteclass.core.common.constant.TeacherClassRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * TeacherClass entity representing class-level assignment between teacher and class.
 *
 * <p>This entity controls class-level permissions:
 * <ul>
 *   <li>MAIN_TEACHER: Primary teacher with full control over the class</li>
 *   <li>ASSISTANT: Teaching assistant with limited permissions</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-TEACHER-003: Teacher can be assigned to multiple classes</li>
 *   <li>BR-TEACHER-004: Class must have at least 1 MAIN_TEACHER</li>
 *   <li>BR-TEACHER-008: Only MAIN_TEACHER can take attendance</li>
 *   <li>Unique constraint: One teacher cannot be assigned to same class multiple times</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Entity
@Table(name = "teacher_classes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_teacher_classes_teacher_class",
                        columnNames = {"teacher_id", "class_id"})
        },
        indexes = {
                @Index(name = "idx_teacher_classes_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_teacher_classes_class_id", columnList = "class_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClass {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to teachers table.
     * The teacher assigned to the class.
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * Foreign key to classes table.
     * The class that the teacher is assigned to.
     */
    @Column(name = "class_id", nullable = false)
    private Long classId;

    /**
     * Role of the teacher in the class.
     * Values: MAIN_TEACHER, ASSISTANT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private TeacherClassRole role;

    /**
     * Timestamp when the teacher was assigned to the class.
     */
    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    /**
     * ID of the user who assigned the teacher to the class.
     */
    @Column(name = "assigned_by")
    private Long assignedBy;

    // Relationships will be added when implementing Class module
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    // private Teacher teacher;
    //
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "class_id", insertable = false, updatable = false)
    // private Class class;
}
