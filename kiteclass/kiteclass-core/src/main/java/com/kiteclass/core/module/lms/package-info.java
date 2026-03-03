/**
 * LMS (Learning Management System) Module - PR 2.9.
 *
 * <p>Provides 3-tier course structure management:
 * <ul>
 *   <li>Course (existing entity from PR 2.4) → CourseModule → Lesson</li>
 *   <li>Trial lesson access for guest users (no authentication required)</li>
 *   <li>Student progress tracking (lesson completion, course progress calculation)</li>
 *   <li>Comprehensive CRUD operations for teachers (course owners only)</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Multi-Tenant Public Access:</strong> Public endpoints work without X-Tenant-Id header (manual tenant context)</li>
 *   <li><strong>Trial Lesson Model:</strong> Lesson.isTrial flag controls guest access (BR-LMS-001)</li>
 *   <li><strong>Enrollment Check:</strong> Paid lessons require active enrollment (BR-LMS-002)</li>
 *   <li><strong>Progress Tracking:</strong> Idempotent lesson completion, auto-calculated course progress (BR-LMS-004, BR-LMS-010)</li>
 *   <li><strong>Permission Model:</strong> Only course owner can CRUD modules/lessons</li>
 * </ul>
 *
 * <h2>Access Control Matrix</h2>
 * <table border="1">
 *   <tr>
 *     <th>User Type</th>
 *     <th>Course Structure</th>
 *     <th>Trial Lessons</th>
 *     <th>Paid Lessons</th>
 *     <th>Progress Tracking</th>
 *   </tr>
 *   <tr>
 *     <td>Guest (unauthenticated)</td>
 *     <td>✅ View</td>
 *     <td>✅ View</td>
 *     <td>❌ Forbidden</td>
 *     <td>❌ N/A</td>
 *   </tr>
 *   <tr>
 *     <td>Student (enrolled)</td>
 *     <td>✅ View</td>
 *     <td>✅ View</td>
 *     <td>✅ View</td>
 *     <td>✅ Track</td>
 *   </tr>
 *   <tr>
 *     <td>Teacher (course owner)</td>
 *     <td>✅ CRUD</td>
 *     <td>✅ CRUD</td>
 *     <td>✅ CRUD</td>
 *     <td>❌ N/A</td>
 *   </tr>
 * </table>
 *
 * <h2>Business Rules</h2>
 * <ul>
 *   <li>BR-LMS-001: Guest can only access lessons where isTrial=true</li>
 *   <li>BR-LMS-002: Student must have active enrollment to access paid lessons</li>
 *   <li>BR-LMS-003: Lesson progress auto-saves on completion</li>
 *   <li>BR-LMS-004: Course progress = (completed lessons / total lessons) * 100</li>
 *   <li>BR-LMS-005: Modules belong to a course (foreign key)</li>
 *   <li>BR-LMS-006: Order number must be unique within course/module</li>
 *   <li>BR-LMS-007: Cannot delete module if it has lessons</li>
 *   <li>BR-LMS-008: Order number must be unique within module</li>
 *   <li>BR-LMS-009: One progress record per user per lesson</li>
 *   <li>BR-LMS-010: Completing a lesson is idempotent</li>
 * </ul>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li><strong>controller</strong> - REST endpoints (LmsController, LessonProgressController)</li>
 *   <li><strong>service</strong> - Business logic (LmsService, LessonProgressService)</li>
 *   <li><strong>repository</strong> - Data access (CourseModuleRepository, LessonRepository, etc.)</li>
 *   <li><strong>entity</strong> - JPA entities (CourseModule, Lesson, LearningResource, LessonProgress)</li>
 *   <li><strong>dto.request</strong> - Request DTOs for API endpoints</li>
 *   <li><strong>dto.response</strong> - Response DTOs for API endpoints</li>
 *   <li><strong>mapper</strong> - MapStruct mapper (LmsMapper)</li>
 *   <li><strong>event</strong> - Domain events (LessonCompletedEvent)</li>
 * </ul>
 *
 * <h2>Database Schema (V14 Migration)</h2>
 * <ul>
 *   <li><strong>course_modules:</strong> 2nd tier (modules within a course)</li>
 *   <li><strong>lessons:</strong> 3rd tier (lessons within a module, isTrial flag)</li>
 *   <li><strong>learning_resources:</strong> Supplemental materials (PDFs, videos, slides, etc.)</li>
 *   <li><strong>lesson_progress:</strong> Student progress tracking</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>PR 2.4 - Course Module (Course entity)</li>
 *   <li>PR 2.6 - Enrollment Module (enrollment check)</li>
 *   <li>PR 2.10.1 - File Storage Module (future integration for resource uploads)</li>
 * </ul>
 *
 * <h2>Phase 1 Scope (MVP)</h2>
 * <ul>
 *   <li>✅ All lessons accessible to enrolled students (no sequential unlocking)</li>
 *   <li>✅ Video URLs stored as text (no Media Service integration yet)</li>
 *   <li>✅ LearningResource as basic entity (optional nice-to-have)</li>
 *   <li>❌ Quiz engine (Phase 2)</li>
 *   <li>❌ Video progress tracking (Phase 2)</li>
 *   <li>❌ Sequential lesson unlocking (Phase 2)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @version 2.9.0
 * @since 2.9.0
 */
package com.kiteclass.core.module.lms;
