package com.kiteclass.core.module.teacher.repository;

import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TeacherClass entity.
 *
 * <p>Provides data access methods for class-level teacher assignments including:
 * <ul>
 *   <li>Find assignments by teacher or class</li>
 *   <li>Check existence and permissions</li>
 *   <li>Query by role for permission checks</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Repository
public interface TeacherClassRepository extends JpaRepository<TeacherClass, Long> {

    /**
     * Finds all class assignments for a teacher.
     *
     * @param teacherId the teacher ID
     * @return list of class assignments
     */
    List<TeacherClass> findByTeacherId(Long teacherId);

    /**
     * Finds all teacher assignments for a class.
     *
     * @param classId the class ID
     * @return list of teacher assignments
     */
    List<TeacherClass> findByClassId(Long classId);

    /**
     * Finds a specific teacher-class assignment.
     *
     * @param teacherId the teacher ID
     * @param classId the class ID
     * @return Optional containing the assignment if exists
     */
    Optional<TeacherClass> findByTeacherIdAndClassId(Long teacherId, Long classId);

    /**
     * Checks if a teacher is assigned to a class.
     *
     * @param teacherId the teacher ID
     * @param classId the class ID
     * @return true if assignment exists
     */
    boolean existsByTeacherIdAndClassId(Long teacherId, Long classId);

    /**
     * Checks if a teacher has a specific role in a class.
     *
     * @param teacherId the teacher ID
     * @param classId the class ID
     * @param role the class role
     * @return true if teacher has the role
     */
    boolean existsByTeacherIdAndClassIdAndRole(Long teacherId, Long classId, TeacherClassRole role);

    /**
     * Finds all classes where teacher has a specific role.
     *
     * @param teacherId the teacher ID
     * @param role the class role
     * @return list of class assignments with the role
     */
    List<TeacherClass> findByTeacherIdAndRole(Long teacherId, TeacherClassRole role);

    /**
     * Counts teachers assigned to a class.
     *
     * @param classId the class ID
     * @return count of teachers
     */
    long countByClassId(Long classId);

    /**
     * Counts teachers with a specific role in a class.
     * Used for BR-TEACHER-004: Class must have at least 1 MAIN_TEACHER.
     *
     * @param classId the class ID
     * @param role the class role
     * @return count of teachers with the role
     */
    long countByClassIdAndRole(Long classId, TeacherClassRole role);
}
