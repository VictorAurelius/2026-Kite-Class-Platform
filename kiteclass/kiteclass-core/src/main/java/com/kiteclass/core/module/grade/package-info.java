/**
 * Grade Module - Final Grade Calculation and Transcript Generation.
 *
 * <h2>Module Overview</h2>
 * <p>The Grade module manages final grade calculation, GPA mapping, and transcript generation.
 * It integrates with Attendance and Assignment modules to calculate weighted final scores.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Weighted grade calculation from multiple components</li>
 *   <li>Automatic component updates via event-driven architecture</li>
 *   <li>Letter grade and GPA mapping using configurable grading scales</li>
 *   <li>Pass/Fail determination based on threshold</li>
 *   <li>Transcript generation for academic records</li>
 *   <li>Grade finalization workflow with teacher approval</li>
 * </ul>
 *
 * <h2>Entities</h2>
 * <ul>
 *   <li>{@link com.kiteclass.core.module.grade.entity.Grade} - Final grade per student per class</li>
 *   <li>{@link com.kiteclass.core.module.grade.entity.GradeComponent} - Individual component scores (attendance, assignments, exams)</li>
 *   <li>{@link com.kiteclass.core.module.grade.entity.GradingScale} - Letter grade and GPA mapping configuration</li>
 *   <li>{@link com.kiteclass.core.module.grade.entity.Transcript} - Student academic transcript per semester</li>
 * </ul>
 *
 * <h2>Grade Calculation Formula</h2>
 * <pre>
 * final_score = Σ(component.weighted_score)
 * where weighted_score = (score/max_score * 100) * (weight_percent/100)
 *
 * Example:
 * Attendance: 94.4% * 10% weight = 9.44 points
 * Assignment: 87.67/100 * 30% weight = 26.30 points
 * Midterm: 82/100 * 25% weight = 20.50 points
 * Final: 88/100 * 35% weight = 30.80 points
 * ───────────────────────────────────────────────
 * final_score = 87.04/100 → B+ (GPA 3.3)
 * </pre>
 *
 * <h2>Business Rules</h2>
 * <ul>
 *   <li>BR-GRADE-001: Unique (student_id, class_id) - one grade per student per class</li>
 *   <li>BR-GRADE-002: Component weights must sum to 100% before finalization</li>
 *   <li>BR-GRADE-003: Finalized grades are immutable (require unfinalizing)</li>
 *   <li>BR-GRADE-004: ATTENDANCE/ASSIGNMENT components auto-updated via events</li>
 *   <li>BR-GRADE-005: Pass/Fail determined by final_score >= pass_threshold</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>Attendance Module:</strong> Listen to ATTENDANCE_MARKED events to update attendance component</li>
 *   <li><strong>Assignment Module:</strong> Listen to ASSIGNMENT_GRADED events to update assignment components</li>
 *   <li><strong>Enrollment Module:</strong> Initialize grade when student enrolls in class</li>
 *   <li><strong>Class Module:</strong> Link grades to class records</li>
 * </ul>
 *
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>GET /api/v1/grades/{id} - Get grade by ID</li>
 *   <li>GET /api/v1/grades/student/{studentId}/class/{classId} - Get student's grade in class</li>
 *   <li>POST /api/v1/grades/calculate/{gradeId} - Calculate final score</li>
 *   <li>POST /api/v1/grades/finalize/{gradeId} - Finalize grade (teacher only)</li>
 *   <li>POST /api/v1/grades/unfinalize/{gradeId} - Unfinalize grade (admin only)</li>
 *   <li>POST /api/v1/grades/components - Add/update grade component</li>
 *   <li>GET /api/v1/transcripts/student/{studentId} - Get student transcript</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>PR 2.3: Student Module (student records)</li>
 *   <li>PR 2.5: Class Module (class records)</li>
 *   <li>PR 2.7: Attendance Module (attendance component)</li>
 *   <li>PR 2.7.1: Assignment Module (assignment component)</li>
 * </ul>
 *
 * @since 2.7.2
 * @author KiteClass Team
 */
package com.kiteclass.core.module.grade;
