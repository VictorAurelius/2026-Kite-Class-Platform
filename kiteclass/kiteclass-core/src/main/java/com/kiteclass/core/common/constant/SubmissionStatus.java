package com.kiteclass.core.common.constant;

/**
 * Submission status enumeration.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
public enum SubmissionStatus {
    /**
     * Pending - Submitted but not graded yet.
     */
    PENDING,

    /**
     * Graded - Score assigned by teacher.
     */
    GRADED,

    /**
     * Returned - Feedback sent to student.
     */
    RETURNED
}
