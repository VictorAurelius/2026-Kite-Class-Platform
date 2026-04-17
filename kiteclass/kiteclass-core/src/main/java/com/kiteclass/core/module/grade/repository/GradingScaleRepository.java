package com.kiteclass.core.module.grade.repository;

import com.kiteclass.core.module.grade.entity.GradingScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for GradingScale entity.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Repository
public interface GradingScaleRepository extends JpaRepository<GradingScale, Long> {

    /**
     * Find grading scale by ID (not deleted).
     *
     * @param id grading scale ID
     * @return grading scale if found
     */
    Optional<GradingScale> findByIdAndDeletedFalse(Long id);

    /**
     * Find all grading scales by instance ID (not deleted).
     * Used for tenant-specific grading configurations.
     *
     * @param instanceId instance ID
     * @return list of grading scales
     */
    List<GradingScale> findByInstanceIdAndDeletedFalseOrderByMinScoreDesc(UUID instanceId);

    /**
     * Find grading scale by instance ID and score range (not deleted).
     * Used for mapping final score to letter grade and GPA.
     *
     * @param instanceId instance ID
     * @param score final score (0-100)
     * @return grading scale if found
     */
    @Query("SELECT gs FROM GradingScale gs WHERE gs.instanceId = :instanceId " +
           "AND :score >= gs.minScore AND :score <= gs.maxScore " +
           "AND gs.deleted = false")
    Optional<GradingScale> findByInstanceIdAndScoreRange(
            @Param("instanceId") UUID instanceId,
            @Param("score") BigDecimal score);

    /**
     * Find grading scale by instance ID and letter grade (not deleted).
     *
     * @param instanceId instance ID
     * @param letterGrade letter grade
     * @return grading scale if found
     */
    Optional<GradingScale> findByInstanceIdAndLetterGradeAndDeletedFalse(
            UUID instanceId, String letterGrade);

    /**
     * Find grading scale by instance ID and GPA value (not deleted).
     *
     * @param instanceId instance ID
     * @param gpaValue GPA value
     * @return grading scale if found
     */
    Optional<GradingScale> findByInstanceIdAndGpaValueAndDeletedFalse(
            UUID instanceId, BigDecimal gpaValue);

    /**
     * Check if grading scale exists for instance (not deleted).
     * Used to determine if instance has custom scale or use default.
     *
     * @param instanceId instance ID
     * @return true if exists
     */
    boolean existsByInstanceIdAndDeletedFalse(UUID instanceId);

    /**
     * Count grading scales by instance ID (not deleted).
     *
     * @param instanceId instance ID
     * @return count
     */
    long countByInstanceIdAndDeletedFalse(UUID instanceId);

    /**
     * Find default grading scales (instance_id is null).
     * Used as fallback when instance doesn't have custom scale.
     *
     * @return list of default grading scales
     */
    @Query("SELECT gs FROM GradingScale gs WHERE gs.instanceId IS NULL " +
           "AND gs.deleted = false ORDER BY gs.minScore DESC")
    List<GradingScale> findDefaultGradingScales();
}
