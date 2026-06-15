-- ============================================================================
-- V100: deterministic FK link learning_resources -> uploaded_files (GAP-1307)
-- ============================================================================
-- GAP-1307 (storage download-url enrollment paywall bypass).
--
-- StorageController.generateDownloadUrl previously enforced only the visibility model
-- (PUBLIC/PRIVATE/TENANT) and never the LMS enrollment paywall. A material attached to a
-- PAID (non-trial) lesson but stored at TENANT scope was therefore downloadable by any
-- same-tenant student, including students who never enrolled in the course — bypassing the
-- paywall that LessonAccessGuard already enforces on the LMS read/write paths.
--
-- The storage download paywall needs a reliable file<->lesson link. The earlier attempt
-- (#2416, reverted) matched uploaded_files.storage_path as a SUBSTRING of the free-text
-- learning_resources.url column — fragile (non-key URLs miss; substring is not a contract).
-- This migration replaces that heuristic with a real, deterministic FK:
--
--   learning_resources.uploaded_file_id BIGINT NULL  -> uploaded_files(id) ON DELETE SET NULL
--
-- Nullable: external links / YouTube / legacy rows have no backing uploaded file and must
-- stay un-paywalled (behaviour unchanged). ON DELETE SET NULL so deleting the stored file
-- never orphans/blocks the resource row.
--
-- Forward-only, idempotent (column/constraint/index existence guards in DO blocks; defensive
-- table-existence guard mirrors V58/V84/V99). Best-effort backfill links existing rows whose
-- url embeds the file's storage key (UUID-bearing => collisions effectively impossible),
-- scoped to the same tenant for safety.
-- ============================================================================

-- 1. Add the nullable FK column + constraint + index (idempotent).
DO $$
BEGIN
    -- Defensive: both tables must exist (created in V79).
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'learning_resources'
    ) THEN
        RAISE NOTICE 'Skipping V100 (learning_resources does not exist)';
        RETURN;
    END IF;

    -- Add nullable FK column if missing.
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'learning_resources'
          AND column_name = 'uploaded_file_id'
    ) THEN
        ALTER TABLE learning_resources ADD COLUMN uploaded_file_id BIGINT;
        RAISE NOTICE 'Added learning_resources.uploaded_file_id';
    END IF;

    -- Add FK constraint if missing AND the referenced table exists.
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'uploaded_files'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'learning_resources'
          AND constraint_name = 'fk_learning_resources_uploaded_file'
    ) THEN
        ALTER TABLE learning_resources
            ADD CONSTRAINT fk_learning_resources_uploaded_file
            FOREIGN KEY (uploaded_file_id) REFERENCES uploaded_files(id) ON DELETE SET NULL;
        RAISE NOTICE 'Added FK fk_learning_resources_uploaded_file (ON DELETE SET NULL)';
    END IF;
END $$;

-- Index for the paywall lookup (findByUploadedFileIdAndDeletedFalse).
CREATE INDEX IF NOT EXISTS idx_learning_resources_uploaded_file_id
    ON learning_resources(uploaded_file_id);

-- 2. Best-effort backfill of existing rows (idempotent: only NULL FKs touched).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'uploaded_files'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'learning_resources'
          AND column_name = 'uploaded_file_id'
    ) THEN
        -- Match the file's storage key (UUID-bearing => unique) embedded in the resource url,
        -- restricted to the same tenant. Only fills rows not already linked.
        UPDATE learning_resources lr
        SET uploaded_file_id = uf.id
        FROM uploaded_files uf
        WHERE lr.uploaded_file_id IS NULL
          AND lr.deleted = FALSE
          AND uf.deleted = FALSE
          AND lr.instance_id = uf.instance_id
          AND lr.url LIKE '%' || uf.storage_path || '%';
        RAISE NOTICE 'V100 backfill of learning_resources.uploaded_file_id complete';
    END IF;
END $$;
