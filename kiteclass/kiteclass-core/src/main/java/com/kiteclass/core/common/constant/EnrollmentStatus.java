package com.kiteclass.core.common.constant;

/**
 * Enrollment status enumeration.
 *
 * <p>Represents the lifecycle status of a student's enrollment in a class:
 * <ul>
 *   <li>{@link #ACTIVE} - Student is currently enrolled and attending</li>
 *   <li>{@link #PENDING_PAYMENT} - Enrollment pending payment confirmation</li>
 *   <li>{@link #COMPLETED} - Enrollment completed (class finished)</li>
 *   <li>{@link #WITHDRAWN} - Student withdrew from the class</li>
 *   <li>{@link #CANCELLED} - Enrollment cancelled by admin/system</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
public enum EnrollmentStatus {
    /**
     * Student is currently enrolled and actively participating in the class.
     */
    ACTIVE,

    /**
     * Enrollment created but pending payment confirmation.
     * Student cannot attend class until payment is confirmed.
     */
    PENDING_PAYMENT,

    /**
     * Enrollment completed successfully.
     * Class has finished and student has completed the course.
     */
    COMPLETED,

    /**
     * Student withdrew from the class.
     * May be eligible for partial refund based on withdrawal policy.
     */
    WITHDRAWN,

    /**
     * Enrollment cancelled by admin or system.
     * Full refund typically issued if applicable.
     */
    CANCELLED
}
