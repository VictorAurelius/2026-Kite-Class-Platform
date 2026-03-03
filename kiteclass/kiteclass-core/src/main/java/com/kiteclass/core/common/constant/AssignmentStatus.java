package com.kiteclass.core.common.constant;

/**
 * Assignment status enumeration.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
public enum AssignmentStatus {
    /**
     * Draft - Not visible to students, teacher still editing.
     */
    DRAFT,

    /**
     * Published - Visible to students, submissions allowed.
     */
    PUBLISHED,

    /**
     * Closed - No more submissions accepted, grading may continue.
     */
    CLOSED
}
