-- =========================================================================
-- V39: quality_reports (Wave 4 Sub-PR 4.5 — Quality Gate GAP-012)
-- =========================================================================
-- Context: ai-branding-guidelines.md §5 "Quality Gate MANDATORY before DEPLOY"
-- Pipeline gate: InstanceQualityReviewer.review(instanceId) → QualityReport.
-- If report.score < PASS_THRESHOLD (default 70), PublishPackageStep blocks
-- the DEPLOY transition; lifecycle caller marks FAILED with report issues.
-- =========================================================================

CREATE TABLE quality_reports (
    id                BIGSERIAL PRIMARY KEY,
    instance_id       UUID         NOT NULL,

    target_instance_id BIGINT      NOT NULL,
    branding_version  INT          NOT NULL,
    score             INT          NOT NULL,
    passed            BOOLEAN      NOT NULL,
    issues            JSONB,

    -- Per-check results (materialized so admins can query without parsing JSON)
    contrast_score    INT,
    css_vars_score    INT,
    asset_urls_score  INT,
    visual_regression_score INT,
    logo_placement_score    INT,

    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       NOT NULL DEFAULT 0,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_quality_report_score_range
        CHECK (score >= 0 AND score <= 100)
);

CREATE INDEX idx_quality_report_target ON quality_reports(target_instance_id);
CREATE INDEX idx_quality_report_passed ON quality_reports(passed);
CREATE INDEX idx_quality_report_created_at ON quality_reports(created_at DESC);
CREATE INDEX idx_quality_report_deleted ON quality_reports(deleted);
