package com.kiteclass.core.module.grade.repository;

import com.kiteclass.core.common.constant.GradeComponentType;
import com.kiteclass.core.module.grade.entity.GradeComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GradeComponent entity.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Repository
public interface GradeComponentRepository extends JpaRepository<GradeComponent, Long> {

    /**
     * Find component by ID (not deleted).
     *
     * @param id component ID
     * @return component if found
     */
    Optional<GradeComponent> findByIdAndDeletedFalse(Long id);

    /**
     * Find all components by grade ID (not deleted).
     *
     * @param gradeId grade ID
     * @return list of components
     */
    @Query("SELECT gc FROM GradeComponent gc WHERE gc.grade.id = :gradeId " +
           "AND gc.deleted = false ORDER BY gc.componentType, gc.componentName")
    List<GradeComponent> findByGradeIdAndDeletedFalse(@Param("gradeId") Long gradeId);

    /**
     * Find component by grade ID and type (not deleted).
     * Multiple components of same type allowed (e.g., multiple assignments).
     *
     * @param gradeId grade ID
     * @param componentType component type
     * @return list of components
     */
    @Query("SELECT gc FROM GradeComponent gc WHERE gc.grade.id = :gradeId " +
           "AND gc.componentType = :componentType AND gc.deleted = false " +
           "ORDER BY gc.componentName")
    List<GradeComponent> findByGradeIdAndComponentType(
            @Param("gradeId") Long gradeId,
            @Param("componentType") GradeComponentType componentType);

    /**
     * Find component by grade ID, type, and reference ID (not deleted).
     * Used for auto-update from events.
     *
     * @param gradeId grade ID
     * @param componentType component type
     * @param componentRefId reference ID
     * @return component if found
     */
    @Query("SELECT gc FROM GradeComponent gc WHERE gc.grade.id = :gradeId " +
           "AND gc.componentType = :componentType " +
           "AND gc.componentRefId = :componentRefId AND gc.deleted = false")
    Optional<GradeComponent> findByGradeIdAndComponentTypeAndComponentRefId(
            @Param("gradeId") Long gradeId,
            @Param("componentType") GradeComponentType componentType,
            @Param("componentRefId") Long componentRefId);

    /**
     * Find attendance component by grade ID (not deleted).
     * Only one attendance component per grade.
     *
     * @param gradeId grade ID
     * @return attendance component if found
     */
    @Query("SELECT gc FROM GradeComponent gc WHERE gc.grade.id = :gradeId " +
           "AND gc.componentType = 'ATTENDANCE' AND gc.deleted = false")
    Optional<GradeComponent> findAttendanceComponentByGradeId(@Param("gradeId") Long gradeId);

    /**
     * Calculate sum of weight percentages for grade (not deleted).
     * Used for validation before finalization.
     *
     * @param gradeId grade ID
     * @return sum of weights
     */
    @Query("SELECT COALESCE(SUM(gc.weightPercent), 0) FROM GradeComponent gc " +
           "WHERE gc.grade.id = :gradeId AND gc.deleted = false")
    Double calculateTotalWeightByGradeId(@Param("gradeId") Long gradeId);

    /**
     * Count components by grade ID (not deleted).
     *
     * @param gradeId grade ID
     * @return count
     */
    @Query("SELECT COUNT(gc) FROM GradeComponent gc WHERE gc.grade.id = :gradeId " +
           "AND gc.deleted = false")
    long countByGradeIdAndDeletedFalse(@Param("gradeId") Long gradeId);

    /**
     * Delete all components by grade ID.
     * Used when grade is deleted.
     *
     * @param gradeId grade ID
     */
    @Query("UPDATE GradeComponent gc SET gc.deleted = true WHERE gc.grade.id = :gradeId")
    void softDeleteByGradeId(@Param("gradeId") Long gradeId);
}
