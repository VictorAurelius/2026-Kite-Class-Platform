-- =========================================================================
-- V45: Composite index for tenant-scoped BrandingResource lookup (GAP-129)
-- =========================================================================
-- Context: GAP-129 (perf + multi-tenancy fix), audit performance-2026-04-19 §1
--
-- Background:
-- BrandingPackageServiceImpl.getByInstanceId(instanceId) previously called
-- resourceRepository.findAll() and filtered in memory by deleted=false.
-- That:
--   (a) loaded ALL branding resources across ALL tenants (multi-tenancy leak),
--   (b) was an unbounded full-table scan on every cache miss.
--
-- Fix:
-- Service now uses BrandingResourceRepository.findByInstanceIdAndDeletedFalse(...)
-- which generates `WHERE instance_id = ? AND deleted = false`. This composite
-- index makes that lookup O(log n) on the prefix and avoids loading deleted rows.
--
-- Existing V32 indexes:
--   - idx_branding_resource_type    (instance_id, type)   -- still used for type lookups
--   - idx_branding_resource_category(category)
--   - idx_branding_resource_deleted (deleted)             -- single-column, low selectivity
--
-- This migration adds a more selective composite tailored to the hot read path.
--
-- Breaking change: NO (additive index only)
-- Rollback:        DROP INDEX IF EXISTS idx_branding_resources_instance_deleted;
-- =========================================================================

CREATE INDEX IF NOT EXISTS idx_branding_resources_instance_deleted
    ON branding_resources (instance_id, deleted);
