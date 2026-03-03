/**
 * Assignment Module - Assignment lifecycle, late penalties, grading workflow.
 *
 * <h2>Core Features</h2>
 * <ul>
 *   <li>Assignment CRUD với permission checks (MAIN_TEACHER only)</li>
 *   <li>Late submission penalty calculation (default 10% per day)</li>
 *   <li>Student submission workflow</li>
 *   <li>Grading workflow với ASSIGNMENT_GRADED event</li>
 *   <li>Multi-tenant support with instance_id filtering</li>
 * </ul>
 *
 * <h2>Key Business Rules</h2>
 * <ul>
 *   <li><strong>BR-ASSIGN-001</strong>: Only MAIN_TEACHER can create assignments</li>
 *   <li><strong>BR-ASSIGN-002</strong>: Students can only submit to PUBLISHED assignments</li>
 *   <li><strong>BR-ASSIGN-003</strong>: One submission per student per assignment</li>
 *   <li><strong>BR-ASSIGN-004</strong>: Late submissions get penalty (default 10% per day)</li>
 *   <li><strong>BR-ASSIGN-005</strong>: Only assigned grader or MAIN_TEACHER can grade</li>
 *   <li><strong>BR-ASSIGN-006</strong>: Late penalty calculation: adjusted_score = score * (1 - penalty%)</li>
 *   <li><strong>BR-ASSIGN-007</strong>: Assignment weight_percent affects final grade</li>
 * </ul>
 *
 * <h2>Assignment Status Flow</h2>
 * <pre>
 * DRAFT → PUBLISHED → CLOSED
 * </pre>
 *
 * <h2>Submission Status Flow</h2>
 * <pre>
 * PENDING → GRADED → RETURNED
 * </pre>
 *
 * <h2>Late Penalty Calculation</h2>
 * <pre>
 * daysLate = ceil((submissionDate - dueDate) / 24 hours)
 * totalPenalty = daysLate * latePenaltyPercent
 * multiplier = 1 - (totalPenalty / 100)
 * adjustedScore = score * multiplier
 * </pre>
 *
 * <h2>Events Published</h2>
 * <ul>
 *   <li><strong>AssignmentGradedEvent</strong>: When submission is graded (triggers Grade Module update)</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>Class Module</strong>: Assignment.classId FK</li>
 *   <li><strong>Student Module</strong>: Submission.studentId FK</li>
 *   <li><strong>Teacher Module</strong>: Permission checks, created_by, graded_by</li>
 *   <li><strong>Storage Module</strong>: File uploads (content_url)</li>
 *   <li><strong>Grade Module</strong>: ASSIGNMENT_GRADED event updates grade components</li>
 * </ul>
 *
 * @since 2.7.1
 * @author KiteClass Team
 */
package com.kiteclass.core.module.assignment;
