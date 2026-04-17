-- =========================================================================
-- V41: student_bulk_import_jobs (GAP-051 Wave 1 — Bulk Import Students MVP)
-- =========================================================================
-- Context: GAP-051 — schools need to onboard 100s-1000s of students via
-- xlsx upload rather than manual one-by-one creation.
--
-- This table tracks each bulk-import attempt so admins can:
--   1. Audit past imports (who, when, how many rows succeeded/failed)
--   2. Download the generated error-report xlsx for failed rows
--   3. (Future) rollback within a grace window
--
-- Columns align with BaseEntity (audit + multi-tenant + soft delete + version).
-- =========================================================================

CREATE TABLE student_bulk_import_jobs (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Job metadata
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_rows INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    error_report_url VARCHAR(500),
    completed_at TIMESTAMP,

    -- Audit (aligned with BaseEntity)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Soft delete + optimistic locking
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_bulk_import_jobs_tenant ON student_bulk_import_jobs(instance_id);
CREATE INDEX idx_bulk_import_jobs_status ON student_bulk_import_jobs(status);
