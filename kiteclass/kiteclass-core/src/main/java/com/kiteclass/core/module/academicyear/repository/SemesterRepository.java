package com.kiteclass.core.module.academicyear.repository;

import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.entity.SemesterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Semester entity (child of AcademicYear aggregate).
 *
 * @since 3.15.0 (GAP-053)
 */
@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByAcademicYearIdAndDeletedFalse(Long academicYearId);

    Optional<Semester> findByAcademicYearIdAndTypeAndDeletedFalse(Long academicYearId, SemesterType type);

    boolean existsByAcademicYearIdAndTypeAndDeletedFalse(Long academicYearId, SemesterType type);
}
