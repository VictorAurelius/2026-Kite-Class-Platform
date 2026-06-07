-- =================================================================
-- V93: Batch monthly invoice generation — billing_month + idempotency (GAP-297)
-- =================================================================
-- Adds a billing_month column so an enrollment can be billed once PER MONTH
-- (recurring monthly invoices), while preserving the legacy "one auto-invoice
-- per enrollment" guarantee for the enrollment-time auto-invoice path.
--
-- Idempotency: re-running batch-confirm for the same (tenant, month) must NOT
-- create duplicate invoices. Enforced by the composite unique index below
-- (service layer also pre-checks + skips for a friendly created/skipped count).
-- =================================================================

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS billing_month DATE;

-- Replace the single-column enrollment uniqueness (V79 partial unique index on
-- enrollment_id) with a composite (instance_id, enrollment_id, billing_month).
--
-- NULLS NOT DISTINCT (Postgres 15+) keeps the legacy invariant: the auto-invoice
-- created at enrollment time has billing_month = NULL, and two such rows for the
-- same (instance, enrollment) are still treated as duplicates → one-per-enrollment
-- preserved. Recurring monthly invoices carry a non-null billing_month, so distinct
-- months coexist for the same enrollment.
DROP INDEX IF EXISTS uk_invoices_enrollment;

CREATE UNIQUE INDEX IF NOT EXISTS uk_invoices_enrollment_month
    ON invoices (instance_id, enrollment_id, billing_month) NULLS NOT DISTINCT
    WHERE enrollment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invoices_billing_month ON invoices (billing_month);

COMMENT ON COLUMN invoices.billing_month IS
    'First day of the billing month for recurring monthly invoices (GAP-297). '
    'NULL for one-off auto-invoices created at enrollment time.';
