package com.kiteclass.core.module.quality.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @since 3.25.0 (Wave 4 Sub-PR 4.5)
 */
@Repository
public interface QualityReportRepository extends JpaRepository<QualityReport, Long> {

    List<QualityReport> findByTargetInstanceIdOrderByCreatedAtDesc(Long targetInstanceId);

    long countByPassedAndDeletedFalse(Boolean passed);
}
