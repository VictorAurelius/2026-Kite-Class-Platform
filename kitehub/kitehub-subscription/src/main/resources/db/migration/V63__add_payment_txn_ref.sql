-- V63: Add txn_ref column to payments for SePay webhook matching
-- Wave flow-kh3-2 (GAP-975) — dynamic VietQR + SePay reconciliation.
--
-- The SePay webhook extracts txn_ref (format KH3SUB<8 hex>) from the bank
-- transfer "description" field and locates the exact payment via an exact-match
-- lookup. Partial UNIQUE index allows multiple NULLs (legacy rows + non-VietQR
-- payments) while guaranteeing uniqueness among generated references.

ALTER TABLE payments ADD COLUMN txn_ref VARCHAR(32);

CREATE UNIQUE INDEX uq_payments_txn_ref ON payments (txn_ref) WHERE txn_ref IS NOT NULL;

COMMENT ON COLUMN payments.txn_ref IS
    'SePay matching reference KH3SUB<8 hex> derived from payment id (Wave flow-kh3-2, GAP-975)';
