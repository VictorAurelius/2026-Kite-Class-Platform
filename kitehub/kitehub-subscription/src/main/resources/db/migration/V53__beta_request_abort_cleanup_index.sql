-- V53: GAP-600 (Wave 92 Bucket C) — composite index for beta_access_request abort cleanup
--
-- Background: BetaRequestAbortCleanupScheduler chạy mỗi 6h (default cron 0 0 */6 * * *)
-- và sweep stale PENDING rows older than threshold (default 24h) → mark ABORTED.
--
-- Query pattern (per BetaAccessRequestRepository.markStaleAsAborted):
--   UPDATE beta_access_request
--   SET status = 'ABORTED', updated_at = :now
--   WHERE status = 'PENDING' AND created_at < :threshold;
--
-- V28 đã có single-column index idx_beta_access_request_status (status). Nhưng cleanup
-- query filter BOTH status='PENDING' AND created_at < threshold. Single-column index
-- chỉ hiệu quả nếu PENDING row chiếm < ~20% bảng (typical Postgres planner cutoff);
-- composite index (status, created_at) cho phép planner index-only scan với both predicates.
--
-- Phase 1 BETA scale: < 1000 rows expected nên perf chưa critical, nhưng:
--   1. Migration cost ~zero (small table)
--   2. Future-proof khi scale lên Phase 1.5 PAID (10k+ rows)
--   3. Cleanup job chạy thường xuyên (every 6h) — perf của ITS query matters
--
-- Pattern reference: composite index pattern dùng trong V35 onboarding_progress
-- (status, last_active_at) cho similar cleanup-style query.

CREATE INDEX IF NOT EXISTS idx_beta_access_request_status_created_at
    ON beta_access_request(status, created_at);

COMMENT ON INDEX idx_beta_access_request_status_created_at IS
    'GAP-600 (Wave 92): composite index for BetaRequestAbortCleanupScheduler stale-PENDING sweep query. status + created_at partial filter performance.';
