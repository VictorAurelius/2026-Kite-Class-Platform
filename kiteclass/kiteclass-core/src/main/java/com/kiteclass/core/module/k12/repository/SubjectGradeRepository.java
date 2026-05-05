package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.enums.SubjectGradeType;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
