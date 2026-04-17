package com.kiteclass.core.module.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * StudentPoint entity representing point transactions for students.
 *
 * <p>Tracks points earned/deducted for various activities:
 * <ul>
 *   <li>Attendance (PRESENT: 0, LATE: -5, ABSENT: -10)</li>
 *   <li>Grades and assignments</li>
 *   <li>Other gamification events</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Entity
@Table(name = "student_points")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Instance ID for multi-tenant isolation.
     */
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    /**
     * Student ID who earned/lost the points.
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Point rule ID (optional, can be null for direct point awards).
     */
    @Column(name = "rule_id")
    private Long ruleId;

    /**
     * Points earned (positive) or deducted (negative).
     */
    @Column(name = "points", nullable = false)
    private Integer points;

    /**
     * Reference type (e.g., "ATTENDANCE", "GRADE", "ASSIGNMENT").
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * Reference ID pointing to the specific record.
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * Description of why points were awarded/deducted.
     */
    @Column(name = "description")
    private String description;

    /**
     * Timestamp when points were earned.
     */
    @Column(name = "earned_at", nullable = false)
    @Builder.Default
    private Instant earnedAt = Instant.now();

    /**
     * Creation timestamp.
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
