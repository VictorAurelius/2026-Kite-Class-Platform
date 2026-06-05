-- V64: Enforce uniqueness on payments.transaction_id for SePay webhook idempotency
-- Wave flow-kh3-2 (GAP-976).
--
-- The SePay webhook stamps payments.transaction_id with the SePay transaction id
-- on completion. A partial UNIQUE index (NULLs excluded — pending payments carry
-- a NULL transaction_id) guarantees a replayed webhook can never complete a second
-- payment under the same SePay id. Complements the service-level idempotency
-- early-return (findByTransactionId).

CREATE UNIQUE INDEX uq_payments_transaction_id
    ON payments (transaction_id) WHERE transaction_id IS NOT NULL;
