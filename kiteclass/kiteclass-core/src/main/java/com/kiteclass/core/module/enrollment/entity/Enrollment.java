package com.kiteclass.core.module.enrollment.entity;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Enrollment entity representing a student's enrollment in a class.
 *
 * <p>Manages the relationship between students and classes, including:
 * <ul>
 *   <li>Enrollment status and dates</li>
 *   <li>Tuition and discount calculations</li>
 *   <li>Payment tracking</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-ENROLL-001: Class must not be at capacity (checked in service)</li>
 *   <li>BR-ENROLL-002: Student cannot enroll in same class twice</li>
 *   <li>BR-ENROLL-003: final_amount = tuition_amount * (1 - discount_percent/100)</li>
 *   <li>BR-ENROLL-004: discount_percent must be 0-100</li>
 *   <li>BR-ENROLL-005: Cannot enroll in ARCHIVED course classes</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enrollments_student_class_instance",
                        columnNames = {"student_id", "class_id", "instance_id", "deleted"}
                )
        },
        indexes = {
                @Index(name = "idx_enrollments_student_id", columnList = "student_id"),
                @Index(name = "idx_enrollments_class_id", columnList = "class_id"),
                @Index(name = "idx_enrollments_status", columnList = "status"),
                @Index(name = "idx_enrollments_instance_id", columnList = "instance_id"),
                @Index(name = "idx_enrollments_deleted", columnList = "deleted"),
                @Index(name = "idx_enrollments_enrollment_date", columnList = "enrollment_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment extends BaseEntity {

    /**
     * Foreign key to student.
     * Required, must reference an existing active student.
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Foreign key to class.
     * Required, must reference an existing non-cancelled class.
     */
    @Column(name = "class_id", nullable = false)
    private Long classId;

    /**
     * Enrollment date and time.
     * Defaults to current timestamp when enrollment is created.
     */
    @Column(name = "enrollment_date", nullable = false)
    @Builder.Default
    private LocalDateTime enrollmentDate = LocalDateTime.now();

    /**
     * Enrollment status.
     * Defaults to PENDING_PAYMENT, transitions to ACTIVE when payment confirmed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.PENDING_PAYMENT;

    /**
     * Original tuition amount for the class.
     * Retrieved from class/course pricing at enrollment time.
     * Must be non-negative.
     */
    @Column(name = "tuition_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal tuitionAmount;

    /**
     * Discount percentage applied to tuition.
     * Range: 0.00 to 100.00.
     * Defaults to 0 (no discount).
     */
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /**
     * Final amount after discount.
     * Auto-calculated: tuition_amount * (1 - discount_percent/100)
     * Rounded to 2 decimal places.
     */
    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    /**
     * Additional notes about the enrollment.
     * Optional, max 2000 characters.
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Lifecycle callback to calculate final_amount before persisting.
     * Ensures final_amount is always consistent with tuition and discount.
     */
    @PrePersist
    @PreUpdate
    public void calculateFinalAmount() {
        if (tuitionAmount != null && discountPercent != null) {
            // final_amount = tuition_amount * (1 - discount_percent/100)
            BigDecimal discountMultiplier = BigDecimal.ONE
                    .subtract(discountPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            this.finalAmount = tuitionAmount
                    .multiply(discountMultiplier)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Check if enrollment is active.
     *
     * @return true if status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return this.status == EnrollmentStatus.ACTIVE;
    }

    /**
     * Check if enrollment is pending payment.
     *
     * @return true if status is PENDING_PAYMENT, false otherwise
     */
    public boolean isPendingPayment() {
        return this.status == EnrollmentStatus.PENDING_PAYMENT;
    }

    /**
     * Check if enrollment is completed.
     *
     * @return true if status is COMPLETED, false otherwise
     */
    public boolean isCompleted() {
        return this.status == EnrollmentStatus.COMPLETED;
    }
}
