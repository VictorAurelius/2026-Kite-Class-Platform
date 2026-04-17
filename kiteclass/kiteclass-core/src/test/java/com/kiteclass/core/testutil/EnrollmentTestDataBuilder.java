package com.kiteclass.core.testutil;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.UpdateEnrollmentStatusRequest;
import com.kiteclass.core.module.enrollment.entity.Enrollment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test data builder for Enrollment-related objects.
 *
 * <p>Provides factory methods to create test data for:
 * <ul>
 *   <li>Enrollment entities</li>
 *   <li>CreateEnrollmentRequest DTOs</li>
 *   <li>UpdateEnrollmentStatusRequest DTOs</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
public class EnrollmentTestDataBuilder {

    public static final UUID DEFAULT_TENANT = ClassTestDataBuilder.DEFAULT_TENANT;

    /**
     * Creates a default Enrollment entity for testing.
     *
     * @return Enrollment with default test data
     */
    public static Enrollment createDefaultEnrollment() {
        Enrollment enrollment = Enrollment.builder()
                .studentId(1L)
                .classId(1L)
                .enrollmentDate(LocalDateTime.now())
                .status(EnrollmentStatus.PENDING_PAYMENT)
                .tuitionAmount(new BigDecimal("1000.00"))
                .discountPercent(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("1000.00"))
                .notes("Default test enrollment")
                .build();
        enrollment.setId(1L);
        enrollment.setInstanceId(DEFAULT_TENANT);
        enrollment.setDeleted(false);
        return enrollment;
    }

    /**
     * Creates an Enrollment entity with custom student and class IDs.
     *
     * @param studentId the student ID
     * @param classId the class ID
     * @return Enrollment with specified IDs
     */
    public static Enrollment createEnrollment(Long studentId, Long classId) {
        Enrollment enrollment = createDefaultEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setClassId(classId);
        return enrollment;
    }

    /**
     * Creates an Enrollment entity with discount applied.
     *
     * @param tuitionAmount the tuition amount
     * @param discountPercent the discount percentage (0-100)
     * @return Enrollment with discount
     */
    public static Enrollment createEnrollmentWithDiscount(BigDecimal tuitionAmount, BigDecimal discountPercent) {
        Enrollment enrollment = createDefaultEnrollment();
        enrollment.setTuitionAmount(tuitionAmount);
        enrollment.setDiscountPercent(discountPercent);
        // finalAmount will be calculated by @PrePersist
        return enrollment;
    }

    /**
     * Creates a default CreateEnrollmentRequest for testing.
     *
     * @return CreateEnrollmentRequest with default test data
     */
    public static CreateEnrollmentRequest createDefaultCreateRequest() {
        return CreateEnrollmentRequest.builder()
                .studentId(1L)
                .classId(1L)
                .tuitionAmount(new BigDecimal("1000.00"))
                .discountPercent(BigDecimal.ZERO)
                .notes("Test enrollment")
                .build();
    }

    /**
     * Creates a CreateEnrollmentRequest with custom student and class IDs.
     *
     * @param studentId the student ID
     * @param classId the class ID
     * @return CreateEnrollmentRequest with specified IDs
     */
    public static CreateEnrollmentRequest createRequestForStudentAndClass(Long studentId, Long classId) {
        return CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(new BigDecimal("1000.00"))
                .discountPercent(BigDecimal.ZERO)
                .notes("Test enrollment")
                .build();
    }

    /**
     * Creates a CreateEnrollmentRequest with discount.
     *
     * @param studentId the student ID
     * @param classId the class ID
     * @param tuitionAmount the tuition amount
     * @param discountPercent the discount percentage
     * @return CreateEnrollmentRequest with discount
     */
    public static CreateEnrollmentRequest createRequestWithDiscount(
            Long studentId,
            Long classId,
            BigDecimal tuitionAmount,
            BigDecimal discountPercent) {
        return CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(tuitionAmount)
                .discountPercent(discountPercent)
                .notes("Test enrollment with discount")
                .build();
    }

    /**
     * Creates a default UpdateEnrollmentStatusRequest for testing.
     *
     * @return UpdateEnrollmentStatusRequest with default test data
     */
    public static UpdateEnrollmentStatusRequest createDefaultUpdateStatusRequest() {
        return UpdateEnrollmentStatusRequest.builder()
                .status(EnrollmentStatus.ACTIVE)
                .notes("Status updated to ACTIVE")
                .build();
    }

    /**
     * Creates an UpdateEnrollmentStatusRequest with custom status.
     *
     * @param status the enrollment status
     * @return UpdateEnrollmentStatusRequest with specified status
     */
    public static UpdateEnrollmentStatusRequest createUpdateStatusRequest(EnrollmentStatus status) {
        return UpdateEnrollmentStatusRequest.builder()
                .status(status)
                .notes("Status updated to " + status)
                .build();
    }
}
