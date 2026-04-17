package com.kiteclass.core.common.constant;

/**
 * Grade component type enumeration.
 *
 * <p>Component Types:
 * <ul>
 *   <li>ATTENDANCE: Attendance-based score (from attendance records)</li>
 *   <li>ASSIGNMENT: Assignment score (from assignment submissions)</li>
 *   <li>MIDTERM: Midterm exam score</li>
 *   <li>FINAL: Final exam score</li>
 *   <li>QUIZ: Quiz score</li>
 *   <li>PROJECT: Project score</li>
 *   <li>PARTICIPATION: Class participation score</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-GRADE-COMP-001: ATTENDANCE component auto-updated from attendance module</li>
 *   <li>BR-GRADE-COMP-002: ASSIGNMENT component auto-updated from assignment module</li>
 *   <li>BR-GRADE-COMP-003: Other components manually entered by teacher</li>
 *   <li>BR-GRADE-COMP-004: Total weights must sum to 100% before finalization</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
public enum GradeComponentType {
    /**
     * Attendance-based component.
     * Auto-calculated from attendance records.
     */
    ATTENDANCE,

    /**
     * Assignment component.
     * Auto-updated when assignments are graded.
     */
    ASSIGNMENT,

    /**
     * Midterm exam component.
     * Manually entered by teacher.
     */
    MIDTERM,

    /**
     * Final exam component.
     * Manually entered by teacher.
     */
    FINAL,

    /**
     * Quiz component.
     * Manually entered by teacher.
     */
    QUIZ,

    /**
     * Project component.
     * Manually entered by teacher.
     */
    PROJECT,

    /**
     * Class participation component.
     * Manually entered by teacher.
     */
    PARTICIPATION
}
