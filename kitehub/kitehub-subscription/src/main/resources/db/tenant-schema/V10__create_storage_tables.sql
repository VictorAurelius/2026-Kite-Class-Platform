-- Migration: Create storage tables for file management
-- Version: V10
-- Description: Creates uploaded_files and storage_quotas tables with multi-tenant support
-- Author: KiteClass Team
-- Date: 2026-02-27

-- Create uploaded_files table
CREATE TABLE uploaded_files (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant support
    instance_id UUID NOT NULL,

    -- Uploader information (Gateway user ID, no FK constraint)
    uploader_id BIGINT NOT NULL,

    -- File metadata
    file_type VARCHAR(20) NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,

    -- Access control
    access_level VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Expiration for pending uploads (30 min TTL)
    expires_at TIMESTAMP,

    -- Audit fields (from BaseEntity pattern)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,

    -- Optimistic locking
    version BIGINT,

    -- Constraints
    CONSTRAINT chk_file_size_positive CHECK (file_size > 0)
);

-- Create storage_quotas table
CREATE TABLE storage_quotas (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant support (one quota per tenant)
    instance_id UUID NOT NULL UNIQUE,

    -- Quota tier and limits
    tier VARCHAR(20) NOT NULL DEFAULT 'FREE',
    used_bytes BIGINT NOT NULL DEFAULT 0,
    quota_bytes BIGINT NOT NULL,

    -- Tracking
    last_calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_used_bytes_non_negative CHECK (used_bytes >= 0),
    CONSTRAINT chk_quota_bytes_positive CHECK (quota_bytes > 0)
);

-- Create indexes for uploaded_files
CREATE INDEX IF NOT EXISTS idx_uploaded_files_instance_id ON uploaded_files(instance_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_uploaded_files_status ON uploaded_files(status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_uploaded_files_expires_at ON uploaded_files(expires_at) WHERE status = 'PENDING' AND deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_uploaded_files_uploader_id ON uploaded_files(uploader_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_uploaded_files_deleted ON uploaded_files(deleted);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_deleted_at ON uploaded_files(deleted_at) WHERE deleted = TRUE;
CREATE INDEX IF NOT EXISTS idx_uploaded_files_instance_status ON uploaded_files(instance_id, status) WHERE deleted = FALSE;

-- Create indexes for storage_quotas
CREATE INDEX IF NOT EXISTS idx_storage_quotas_instance_id ON storage_quotas(instance_id);
CREATE INDEX IF NOT EXISTS idx_storage_quotas_tier ON storage_quotas(tier);

-- Add trigger for updated_at on uploaded_files
CREATE OR REPLACE FUNCTION update_uploaded_files_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_uploaded_files_updated_at
    BEFORE UPDATE ON uploaded_files
    FOR EACH ROW
    EXECUTE FUNCTION update_uploaded_files_updated_at();

-- Add trigger for updated_at on storage_quotas
CREATE OR REPLACE FUNCTION update_storage_quotas_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_storage_quotas_updated_at
    BEFORE UPDATE ON storage_quotas
    FOR EACH ROW
    EXECUTE FUNCTION update_storage_quotas_updated_at();

-- Add comments for documentation
COMMENT ON TABLE uploaded_files IS 'Stores file metadata for uploaded files with multi-tenant support';
COMMENT ON COLUMN uploaded_files.id IS 'Unique identifier for uploaded file';
COMMENT ON COLUMN uploaded_files.instance_id IS 'Tenant identifier for multi-tenant isolation';
COMMENT ON COLUMN uploaded_files.uploader_id IS 'User ID from Gateway service (no FK constraint)';
COMMENT ON COLUMN uploaded_files.file_type IS 'File type: IMAGE, DOCUMENT, VIDEO, AUDIO, OTHER';
COMMENT ON COLUMN uploaded_files.original_name IS 'Original filename from upload';
COMMENT ON COLUMN uploaded_files.storage_path IS 'S3/MinIO storage path: {instanceId}/uploads/{year}/{month}/{uuid}.ext';
COMMENT ON COLUMN uploaded_files.file_size IS 'File size in bytes';
COMMENT ON COLUMN uploaded_files.mime_type IS 'MIME type (e.g., image/jpeg, application/pdf)';
COMMENT ON COLUMN uploaded_files.access_level IS 'Access control: PUBLIC, PRIVATE, TENANT';
COMMENT ON COLUMN uploaded_files.status IS 'Upload status: PENDING, CONFIRMED, EXPIRED, DELETED';
COMMENT ON COLUMN uploaded_files.expires_at IS 'Expiration time for PENDING uploads (30 min TTL)';
COMMENT ON COLUMN uploaded_files.deleted IS 'Soft delete flag (TRUE = deleted, file scheduled for S3 cleanup)';
COMMENT ON COLUMN uploaded_files.deleted_at IS 'Timestamp when file was soft deleted (30-day grace period)';
COMMENT ON COLUMN uploaded_files.version IS 'Version for optimistic locking';

COMMENT ON TABLE storage_quotas IS 'Stores storage quota usage per tenant';
COMMENT ON COLUMN storage_quotas.id IS 'Unique identifier for quota record';
COMMENT ON COLUMN storage_quotas.instance_id IS 'Tenant identifier (one quota per tenant)';
COMMENT ON COLUMN storage_quotas.tier IS 'Quota tier: FREE (1GB), BASIC (10GB), PRO (50GB), ENTERPRISE (100GB)';
COMMENT ON COLUMN storage_quotas.used_bytes IS 'Current storage usage in bytes';
COMMENT ON COLUMN storage_quotas.quota_bytes IS 'Maximum allowed storage in bytes';
COMMENT ON COLUMN storage_quotas.last_calculated_at IS 'Last time quota usage was recalculated';
