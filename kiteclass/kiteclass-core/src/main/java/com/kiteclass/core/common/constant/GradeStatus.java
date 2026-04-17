package com.kiteclass.core.common.constant;

/**
 * Grade status enumeration.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>IN_PROGRESS: Grade is being calculated (default status)</li>
 *   <li>FINALIZED: Grade is locked and cannot be changed</li>
 *   <li>PASSED: Student passed the class (final_score >= pass_threshold)</li>
 *   <li>FAILED: Student failed the class (final_score < pass_threshold)</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-GRADE-001: Only FINALIZED grades can be PASSED/FAILED</li>
 *   <li>BR-GRADE-002: IN_PROGRESS → FINALIZED transition requires all components present</li>
 *   <li>BR-GRADE-003: FINALIZED → IN_PROGRESS allowed only by admin (unfinalizing)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
public enum GradeStatus {
    /**
     * Grade is being calculated, not final yet.
     */
    IN_PROGRESS,

    /**
     * Grade is finalized and locked.
     */
    FINALIZED,

    /**
     * Student passed the class.
     */
    PASSED,

    /**
     * Student failed the class.
     */
    FAILED
}
