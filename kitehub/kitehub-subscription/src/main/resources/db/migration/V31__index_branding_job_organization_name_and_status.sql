-- V31: Add covering indexes on branding_jobs for slug-availability lookup
-- and admin status-filter queue.
--
-- Reference: GAP-392 (N+1 in SlugAvailabilityService) + GAP-393 status index
--            portion (Wave 35 Bucket E).
--
-- Background: SlugAvailabilityService#isTaken previously called
-- BrandingJobRepository.findAll() and stream-filtered every row. With even a
-- few thousand rows this becomes both an N+1 hot-path on every wizard
-- key-stroke AND a full table scan. The replacement query
-- (BrandingJobRepository.existsByOrganizationNameLowercased) compiles to
--    SELECT (COUNT(j) > 0) FROM branding_jobs WHERE LOWER(organization_name) = ?
-- which without an index also scans. The functional index below makes the
-- lookup O(log n).
--
-- The status index supports the admin job-queue filtering predicates (e.g.
-- WHERE status = 'PROCESSING') used by KiteHub admin dashboards added in
-- Wave 33/34. Postgres can use it for both equality filters and ORDER BY
-- status when listing recent jobs per status.

-- Functional index for case-insensitive organization-name slug-availability
-- lookups (drives existsByOrganizationNameLowercased query).
CREATE INDEX IF NOT EXISTS idx_branding_job_org_name_lower
    ON branding_jobs (LOWER(organization_name));

-- Btree index for admin queue filtering by job status. Cardinality of
-- JobStatus is small (~5 values) but selectivity is high because most rows
-- live in COMPLETED while admin queries target QUEUED / PROCESSING.
CREATE INDEX IF NOT EXISTS idx_branding_job_status
    ON branding_jobs (status);
