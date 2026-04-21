package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.k12.entity.HomeroomClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Finds a homeroom class by ID with its {@code academicYear} prefetched
     * (GAP-134 anti-N+1). The lazy {@code @ManyToOne} association otherwise
     * triggers a per-row SELECT whenever a caller formats the report heading
     * ("Grade 10A — 2025-2026").
     *
     * @param id the homeroom-class ID
     * @return Optional containing class with academicYear prefetched
     * @since 3.17.0 (GAP-134 expansion — Wave 9.5)
     */
    @EntityGraph(attributePaths = {"academicYear"})
    @Query("SELECT h FROM HomeroomClass h WHERE h.id = :id AND h.deleted = false")
    Optional<HomeroomClass> findByIdWithAcademicYear(@Param("id") Long id);
}
