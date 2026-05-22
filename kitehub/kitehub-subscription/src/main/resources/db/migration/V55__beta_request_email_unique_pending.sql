-- Wave 105 Bucket E0 Bug 3 — failure-mode matrix A1 idempotency closure.
--
-- Without this partial unique index, two concurrent POSTs to
-- /api/v1/auth/request-beta-access with the same email both pass the
-- "find existing PENDING" service check (TOCTOU race) and INSERT 2 rows
-- → admin sees duplicate requests + audit-trail confusion. The app-level
-- guard in BetaAccessService.submitRequest (findFirstByEmailAndStatus...)
-- is NOT sufficient — race window is real.
--
-- Partial unique index on (email) WHERE status='PENDING' enforces "at most
-- one PENDING per email" at DB level. APPROVED/REJECTED/SIGNED_UP rows
-- coexist (history retained); only the PENDING slot is unique.
--
-- The 2nd concurrent INSERT will hit Postgres unique violation
-- (SQLSTATE 23505); JPA surfaces as DataIntegrityViolationException →
-- mapped to HTTP 409 by GlobalExceptionHandler (existing pattern).
--
-- FE button-debounce 1s (Bucket A scope — handoff) covers the trivial
-- double-click; this index is the correctness guard for true concurrency.

CREATE UNIQUE INDEX IF NOT EXISTS idx_beta_request_email_unique_pending
    ON beta_access_request (email)
    WHERE status = 'PENDING';

COMMENT ON INDEX idx_beta_request_email_unique_pending
    IS 'Wave 105 Bucket E0 — at most one PENDING beta request per email '
       '(idempotency vs double-click + concurrent POST race).';
