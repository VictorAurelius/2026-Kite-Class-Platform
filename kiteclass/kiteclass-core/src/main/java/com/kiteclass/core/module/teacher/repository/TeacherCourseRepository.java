package com.kiteclass.core.module.teacher.repository;

import com.kiteclass.core.common.constant.TeacherCourseRole;
import com.kiteclass.core.module.teacher.entity.TeacherCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TeacherCourse entity.
 *
 * <p>Provides data access methods for course-level teacher assignments including:
 * <ul>
 *   <li>Find assignments by teacher or course</li>
 *   <li>Check existence and permissions</li>
 *   <li>Query by role for permission checks</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Repository
public interface TeacherCourseRepository extends JpaRepository<TeacherCourse, Long> {

    /**
     * Finds all course assignments for a teacher.
     *
     * @param teacherId the teacher ID
     * @return list of course assignments
     */
    List<TeacherCourse> findByTeacherId(Long teacherId);

    /**
     * Finds all teacher assignments for a course.
     *
     * @param courseId the course ID
     * @return list of teacher assignments
     */
    List<TeacherCourse> findByCourseId(Long courseId);

    /**
     * Finds a specific teacher-course assignment.
     *
     * @param teacherId the teacher ID
     * @param courseId the course ID
     * @return Optional containing the assignment if exists
     */
    Optional<TeacherCourse> findByTeacherIdAndCourseId(Long teacherId, Long courseId);

    /**
     * Checks if a teacher is assigned to a course.
     *
     * @param teacherId the teacher ID
     * @param courseId the course ID
     * @return true if assignment exists
     */
    boolean existsByTeacherIdAndCourseId(Long teacherId, Long courseId);

    /**
     * Checks if a teacher has a specific role in a course.
     *
     * @param teacherId the teacher ID
     * @param courseId the course ID
     * @param role the course role
     * @return true if teacher has the role
     */
    boolean existsByTeacherIdAndCourseIdAndRole(Long teacherId, Long courseId, TeacherCourseRole role);

    /**
     * Checks if a teacher has any of the specified roles in a course.
     * Used for permission checks (e.g., CREATOR or INSTRUCTOR).
     *
     * @param teacherId the teacher ID
     * @param courseId the course ID
     * @param roles the list of roles to check
     * @return true if teacher has any of the roles
     */
    boolean existsByTeacherIdAndCourseIdAndRoleIn(Long teacherId, Long courseId, List<TeacherCourseRole> roles);

    /**
     * Finds all courses where teacher has a specific role.
     *
     * @param teacherId the teacher ID
     * @param role the course role
     * @return list of course assignments with the role
     */
    List<TeacherCourse> findByTeacherIdAndRole(Long teacherId, TeacherCourseRole role);

    /**
     * Counts teachers assigned to a course.
     *
     * @param courseId the course ID
     * @return count of teachers
     */
    long countByCourseId(Long courseId);

    /**
     * Counts teachers with a specific role in a course.
     *
     * @param courseId the course ID
     * @param role the course role
     * @return count of teachers with the role
     */
    long countByCourseIdAndRole(Long courseId, TeacherCourseRole role);
}
