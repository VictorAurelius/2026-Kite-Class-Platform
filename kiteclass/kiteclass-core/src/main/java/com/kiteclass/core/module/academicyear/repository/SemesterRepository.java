package com.kiteclass.core.module.academicyear.repository;

import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.entity.SemesterType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Finds a semester by ID with its {@code academicYear} parent prefetched
     * (GAP-134 anti-N+1). Grade + transcript services typically read the
     * semester to render "HK1 / 2025-2026"; without this method Hibernate
     * emits an extra SELECT for the lazy {@code @ManyToOne} on every access.
     *
     * @param id the semester ID
     * @return Optional containing the semester with academicYear prefetched
     * @since 3.17.0 (GAP-134 expansion — Wave 9.5)
     */
    @EntityGraph(attributePaths = {"academicYear"})
    @Query("SELECT s FROM Semester s WHERE s.id = :id AND s.deleted = false")
    Optional<Semester> findByIdWithAcademicYear(@Param("id") Long id);
}
