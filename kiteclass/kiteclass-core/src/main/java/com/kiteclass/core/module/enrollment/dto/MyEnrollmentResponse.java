package com.kiteclass.core.module.enrollment.dto;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Response DTO for the student-self enrollment list ({@code GET /api/v1/enrollments/me}).
 *
 * <p>Extends the plain {@link EnrollmentResponse} fields with class + course
 * enrichment ({@code classId} + {@code className} + {@code courseId} +
 * {@code courseName}) so the kc-student frontend can render "Khóa học của tôi"
 * (my courses) and "Lớp của tôi" (my classes) without a second round-trip per
 * enrollment. The endpoint is inherently self-scoped — the caller resolves to
 * their own {@code students.id} via {@code X-User-Reference-Id} — so no
 * studentId path variable is exposed (GAP-1285).
 *
 * @author KiteClass Team
 * @since GAP-1285 (Wave rbac-lms-gap-1285)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyEnrollmentResponse {

    /** Enrollment ID. */
    private Long id;

    /** Student ID (the calling actor's {@code students.id}). */
    private Long studentId;

    /** Class ID the enrollment belongs to. */
    private Long classId;

    /** Class display name (enriched from {@code classes.name}); null if class soft-deleted. */
    private String className;

    /** Course ID the class belongs to (enriched via class → course); null if class missing. */
    private Long courseId;

    /** Course display name (enriched from {@code courses.name}); null if course missing. */
    private String courseName;

    /** Enrollment date. */
    private LocalDateTime enrollmentDate;

    /** Current enrollment status. */
    private EnrollmentStatus status;

    /** Original tuition amount. */
    private BigDecimal tuitionAmount;

    /** Discount percentage applied. */
    private BigDecimal discountPercent;

    /** Final amount after discount. */
    private BigDecimal finalAmount;

    /** Additional notes. */
    private String notes;

    /** Creation timestamp. */
    private Instant createdAt;

    /** Last update timestamp. */
    private Instant updatedAt;
}
