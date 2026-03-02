package com.kiteclass.core.module.gamification.repository;

import com.kiteclass.core.module.gamification.entity.StudentPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link StudentPoint} entity.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Repository
public interface StudentPointRepository extends JpaRepository<StudentPoint, Long> {

    /**
     * Find student point record by reference type and reference ID.
     *
     * @param referenceType reference type (e.g., "ATTENDANCE")
     * @param referenceId reference ID
     * @return optional student point record
     */
    Optional<StudentPoint> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    /**
     * Calculate total points for a student.
     *
     * @param studentId student ID
     * @return total points (sum of all point transactions)
     */
    @Query("SELECT COALESCE(SUM(sp.points), 0) FROM StudentPoint sp WHERE sp.studentId = :studentId")
    Integer getTotalPointsByStudentId(@Param("studentId") Long studentId);
}
