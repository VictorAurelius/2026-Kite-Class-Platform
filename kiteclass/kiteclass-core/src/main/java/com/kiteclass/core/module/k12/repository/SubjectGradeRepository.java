package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.enums.SubjectGradeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.15.0 (GAP-054); Phase 1C extension 5.x (Wave 19 Bucket B — GAP-323c)
 */
@Repository
public interface SubjectGradeRepository extends JpaRepository<SubjectGrade, Long> {

    List<SubjectGrade> findByStudentIdAndSemesterIdAndDeletedFalse(Long studentId, Long semesterId);

    List<SubjectGrade> findBySubjectSectionIdAndSemesterIdAndDeletedFalse(
            Long subjectSectionId, Long semesterId);

    Optional<SubjectGrade> findByStudentIdAndSubjectSectionIdAndSemesterIdAndDeletedFalse(
            Long studentId, Long subjectSectionId, Long semesterId);

    /**
     * Phase 1C — query by Tổ trưởng approval status.
     *
     * @since Wave 19 Bucket B — GAP-323c
     */
    List<SubjectGrade> findBySubjectSectionIdAndStatusAndDeletedFalse(
            Long subjectSectionId, SubjectGradeStatus status);

    /**
     * Phase 1C — query all records by status across instance (for Tổ trưởng /
     * Hiệu trưởng review queue dashboards).
     *
     * @since Wave 19 Bucket B — GAP-323c
     */
    List<SubjectGrade> findByStatusAndDeletedFalse(SubjectGradeStatus status);

    /**
     * Phase 1C — query by (student, section, semester, type) for formula
     * computation aggregations (e.g., all TX assessments for a student in a
     * semester).
     *
     * @since Wave 19 Bucket B — GAP-323c (used by GradeFormulaService)
     */
    List<SubjectGrade> findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
            Long studentId, Long subjectSectionId, Long semesterId, SubjectGradeType type);

    /**
     * Phase 1C remainder — load a single record honoring soft-delete.
     *
     * @since Wave 24 Bucket B — GAP-360 §360.1
     */
    Optional<SubjectGrade> findByIdAndDeletedFalse(Long id);

    /**
     * Phase 1C remainder — count grades for (student, academicYear) whose status
     * is NOT the supplied value. Used by the học bạ trigger listener
     * (§360.5): when the count of "not-yet-PUBLISHED" rows reaches zero, every
     * subject for that student in that academic year is published, and the
     * học bạ generation Outbox event fires.
     *
     * <p>Joins via {@code Semester.academicYear.id} so a single query spans
     * HK1 + HK2 without needing the listener to resolve semester ids. Uses
     * an explicit JPQL query rather than the Spring Data {@code _} traversal
     * naming convention so the method name stays Checkstyle-compliant.
     *
     * @since Wave 24 Bucket B — GAP-360 §360.5
     */
    @Query("select count(g) from SubjectGrade g "
            + "where g.studentId = :studentId "
            + "and g.semester.academicYear.id = :academicYearId "
            + "and g.status <> :status "
            + "and g.deleted = false")
    long countNotInStatusForStudentAndAcademicYear(
            @Param("studentId") Long studentId,
            @Param("academicYearId") Long academicYearId,
            @Param("status") SubjectGradeStatus status);
}
