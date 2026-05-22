-- V55 — Beta access request: partial unique index on (email) WHERE status='PENDING'
--
-- Wave 105 Bucket A (Anonymous persona walk) — closes failure-mode matrix A1.
--
-- Hardens A1 (double-submit race): existing service-layer idempotency
-- (BetaAccessService.submitRequest returns existing PENDING row when found)
-- has a TOCTOU race window between SELECT and INSERT. Concurrent submissions
-- from same email + same IP within ~50ms can both pass the existence check
-- and both insert → duplicate PENDING rows + duplicate coordinator workload
-- + duplicate invite emails.
--
-- This partial unique index makes the DB itself the source of truth for
-- "one open PENDING request per email", so any race-loser INSERT fails with
-- SQLState 23505 (duplicate key) → service catches + returns the existing row.
--
-- Why PARTIAL: (email) only matters while status=PENDING. Once a request
-- transitions to APPROVED / REJECTED / SIGNED_UP / EXPIRED, the same email
-- may submit again (e.g. tenant reapplies after rejection, or token expired).
--
-- Backward compat: idx_beta_access_request_email (V28, non-unique, all rows)
-- retained for status-agnostic lookup queries. No backfill needed — existing
-- data has no enforced uniqueness yet but per BetaAccessService flow the
-- in-flight invariant should already hold; if duplicates exist this migration
-- will fail loudly with the conflicting (email) value, alerting coordinator
-- to manual cleanup before retry.

CREATE UNIQUE INDEX IF NOT EXISTS uq_beta_access_request_email_pending
    ON beta_access_request (email)
    WHERE status = 'PENDING';

COMMENT ON INDEX uq_beta_access_request_email_pending IS
    'Wave 105 Bucket A (failure-mode A1): DB-level idempotency for beta request submission. One open PENDING per email; transitions away from PENDING release the slot.';
