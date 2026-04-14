package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.k12.entity.HomeroomClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.15.0 (GAP-054)
 */
@Repository
public interface HomeroomClassRepository extends JpaRepository<HomeroomClass, Long> {

    List<HomeroomClass> findByAcademicYearIdAndDeletedFalse(Long academicYearId);

    Optional<HomeroomClass> findByAcademicYearIdAndGradeAndSectionAndDeletedFalse(
            Long academicYearId, String grade, String section);

    boolean existsByAcademicYearIdAndGradeAndSectionAndDeletedFalse(
            Long academicYearId, String grade, String section);

    List<HomeroomClass> findByHomeroomTeacherIdAndDeletedFalse(Long teacherId);
}
