package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.k12.entity.SubjectGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.15.0 (GAP-054)
 */
@Repository
public interface SubjectGradeRepository extends JpaRepository<SubjectGrade, Long> {

    List<SubjectGrade> findByStudentIdAndSemesterIdAndDeletedFalse(Long studentId, Long semesterId);

    List<SubjectGrade> findBySubjectSectionIdAndSemesterIdAndDeletedFalse(
            Long subjectSectionId, Long semesterId);

    Optional<SubjectGrade> findByStudentIdAndSubjectSectionIdAndSemesterIdAndDeletedFalse(
            Long studentId, Long subjectSectionId, Long semesterId);
}
