package com.kiteclass.core.module.quality.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-run quality review result for a FrontendInstance (GAP-012).
 *
 * <p>Written by {@code InstanceQualityReviewer.review()} before the pipeline transitions
 * an instance to DEPLOYED. If {@code score < PASS_THRESHOLD} (default 70), the
 * downstream {@code QualityReviewStep} blocks publication.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-QG-001: score ∈ [0, 100] (enforced by CHECK constraint)</li>
 *   <li>BR-QG-002: passed = (score ≥ threshold)</li>
 *   <li>BR-QG-003: issues JSONB is an array of {check, severity, detail} triples</li>
 * </ul>
 *
 * @since 3.25.0 (Wave 4 Sub-PR 4.5)
 */
@Entity
@Table(
        name = "quality_reports",
        indexes = {
                @Index(name = "idx_quality_report_target", columnList = "target_instance_id"),
                @Index(name = "idx_quality_report_passed", columnList = "passed"),
                @Index(name = "idx_quality_report_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityReport extends BaseEntity {

    @Column(name = "target_instance_id", nullable = false)
    private Long targetInstanceId;

    @Column(name = "branding_version", nullable = false)
    private Integer brandingVersion;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Column(name = "issues", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)  // GAP-220
    private String issues;

    @Column(name = "contrast_score")
    private Integer contrastScore;

    @Column(name = "css_vars_score")
    private Integer cssVarsScore;

    @Column(name = "asset_urls_score")
    private Integer assetUrlsScore;

    @Column(name = "visual_regression_score")
    private Integer visualRegressionScore;

    @Column(name = "logo_placement_score")
    private Integer logoPlacementScore;
}
