-- ============================================================================
-- V69: Create payment_records table for manual payment recording (GAP-292b)
-- ============================================================================
-- Wave beta-readiness-4 Bucket C — Paired payment recording with PER_HOUR pricing model.
--
-- NOTE: Wave plan §3.6 reserved V67b for this migration. Renamed to V69 because:
--   - V67 = pricing_model + unit_price (this PR)
--   - V68 = class reschedule audit (Bucket D in flight)
--   - V69 = payment_records (this migration; V67b -> V69 sequential to avoid Flyway ordering ambiguity)
--
-- Records manual payment received by teacher/admin at trung tâm (cash, bank transfer, VietQR, MoMo).
-- Distinct from online gateway payments (Payment entity for VNPAY/MoMo redirect flows).
--
-- Business Rules:
--   BR-PAYMENT-METHOD-001: method ∈ {CASH, BANK_TRANSFER, VIETQR, MOMO} (PaymentRecordMethod enum)
--   BR-PAYMENT-METHOD-002: amount > 0 for non-FREE courses (CHECK constraint)
--   BR-PAYMENT-METHOD-003: instance_id matches invoice's tenant (RLS / OWASP A01 defense)
--   BR-PAYMENT-METHOD-004: Idempotency via shared idempotency_keys table (V66) scope=PAYMENT
--
-- Multi-tenant: instance_id UUID inherited from BaseEntity, enforced by Hibernate tenantFilter.
-- ============================================================================

CREATE TABLE payment_records (
    id BIGSERIAL PRIMARY KEY,

    -- BaseEntity inherited columns
    instance_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,

    -- Domain columns
    invoice_id BIGINT NOT NULL,
    method VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    paid_at TIMESTAMPTZ NOT NULL,
    note VARCHAR(500),
    recorded_by BIGINT NOT NULL,

    -- Constraints
    CONSTRAINT chk_payment_records_method
        CHECK (method IN ('CASH', 'BANK_TRANSFER', 'VIETQR', 'MOMO')),
    CONSTRAINT chk_payment_records_amount_positive
        CHECK (amount > 0),
    CONSTRAINT fk_payment_records_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

-- Indexes per PaymentRecord @Table indexes annotation
CREATE INDEX idx_payment_records_invoice_id ON payment_records(invoice_id);
CREATE INDEX idx_payment_records_instance_id ON payment_records(instance_id);
CREATE INDEX idx_payment_records_paid_at ON payment_records(paid_at);
CREATE INDEX idx_payment_records_method ON payment_records(method);

-- Composite index for tenant-scoped period queries (revenue dashboard Wave 92 Bucket B precedent)
CREATE INDEX idx_payment_records_tenant_period
    ON payment_records(instance_id, paid_at)
    WHERE deleted = FALSE;

COMMENT ON TABLE payment_records IS
    'Manual payment recordings by teachers/admins (cash, bank, VietQR, MoMo). Wave beta-readiness-4 Bucket C / GAP-292b';
COMMENT ON COLUMN payment_records.method IS
    'PaymentRecordMethod enum: CASH | BANK_TRANSFER | VIETQR | MOMO';
COMMENT ON COLUMN payment_records.amount IS
    'Amount received in VND. NUMERIC(19,2) handles up to 9.99 × 10^16 đ.';
COMMENT ON COLUMN payment_records.recorded_by IS
    'User ID (teacher/admin) who recorded this payment — audit trail for accountability.';
