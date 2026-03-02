-- ============================================================================
-- V12: Invoice Module Extended Tables
-- Created: 2026-03-02
-- Description: Adds invoice module support with adjustments, installment plans,
--              installments, and refund requests. Alters existing invoices table
--              to add missing columns for Invoice Module implementation.
-- ============================================================================

-- =================================================================
-- SECTION 1: Alter Existing invoices Table
-- =================================================================

-- Add missing columns to existing invoices table
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS enrollment_id BIGINT,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

-- Add foreign key constraint for enrollment_id (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoices_enrollment'
    ) THEN
        ALTER TABLE invoices
            ADD CONSTRAINT fk_invoices_enrollment
            FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE RESTRICT;
    END IF;
END $$;

-- Add unique constraint for enrollment_id (one invoice per enrollment)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_invoices_enrollment'
    ) THEN
        ALTER TABLE invoices
            ADD CONSTRAINT uk_invoices_enrollment UNIQUE (enrollment_id);
    END IF;
END $$;

-- Add soft delete index
CREATE INDEX IF NOT EXISTS idx_invoices_deleted ON invoices(deleted) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_invoices_enrollment ON invoices(enrollment_id) WHERE deleted = FALSE;

-- Update status constraint to match InvoiceStatus enum
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS chk_invoices_status;
ALTER TABLE invoices ADD CONSTRAINT chk_invoices_status CHECK (
    status IN ('DRAFT', 'SENT', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED', 'REFUNDED')
);

COMMENT ON COLUMN invoices.enrollment_id IS 'FK to enrollments - one invoice per enrollment';
COMMENT ON COLUMN invoices.paid_at IS 'Timestamp when invoice was fully paid';
COMMENT ON COLUMN invoices.deleted IS 'Soft delete flag for multi-tenant isolation';

-- =================================================================
-- SECTION 2: Invoice Adjustments Table
-- =================================================================

CREATE TABLE invoice_adjustments (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    invoice_id BIGINT NOT NULL,

    -- Adjustment Details
    type VARCHAR(50) NOT NULL,
    -- DISCOUNT, LATE_FEE, ADDITIONAL_CHARGE, REFUND
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    -- Positive for charges/fees, negative for discounts
    reason TEXT,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_adjustments_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_adjustments_type CHECK (
        type IN ('DISCOUNT', 'LATE_FEE', 'ADDITIONAL_CHARGE', 'REFUND')
    )
);

CREATE INDEX idx_adjustments_invoice ON invoice_adjustments(invoice_id);
CREATE INDEX idx_adjustments_type ON invoice_adjustments(type);

COMMENT ON TABLE invoice_adjustments IS 'Invoice adjustments (discounts, fees, charges, refunds)';
COMMENT ON COLUMN invoice_adjustments.type IS 'DISCOUNT (negative), LATE_FEE, ADDITIONAL_CHARGE, REFUND';
COMMENT ON COLUMN invoice_adjustments.amount IS 'Positive for charges, negative for discounts';

-- =================================================================
-- SECTION 3: Installment Plans Table
-- =================================================================

CREATE TABLE installment_plans (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Foreign Keys
    invoice_id BIGINT NOT NULL,

    -- Plan Details
    number_of_installments INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    -- PENDING, APPROVED, REJECTED, ACTIVE, COMPLETED, CANCELLED

    -- Approval Workflow
    requested_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by BIGINT,
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,

    -- Soft Delete
    deleted BOOLEAN DEFAULT FALSE,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_plans_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT uk_plans_invoice UNIQUE (invoice_id),
    CONSTRAINT chk_plan_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'ACTIVE', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_plan_installments CHECK (number_of_installments BETWEEN 2 AND 12)
);

CREATE INDEX idx_plans_invoice ON installment_plans(invoice_id);
CREATE INDEX idx_plans_instance ON installment_plans(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_plans_status ON installment_plans(status) WHERE deleted = FALSE;

-- Trigger for updated_at
CREATE TRIGGER trg_installment_plans_updated_at
    BEFORE UPDATE ON installment_plans
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

COMMENT ON TABLE installment_plans IS 'Installment plan requests for invoices (2-12 installments)';
COMMENT ON COLUMN installment_plans.number_of_installments IS 'Number of installments (2-12)';
COMMENT ON COLUMN installment_plans.status IS 'PENDING → APPROVED → ACTIVE → COMPLETED';

-- =================================================================
-- SECTION 4: Installments Table
-- =================================================================

CREATE TABLE installments (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    plan_id BIGINT NOT NULL,

    -- Installment Details
    installment_number INTEGER NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_amount DECIMAL(10, 2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    -- PENDING, PAID, OVERDUE, CANCELLED

    -- Payment Timestamp
    paid_at TIMESTAMP WITH TIME ZONE,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_installments_plan FOREIGN KEY (plan_id)
        REFERENCES installment_plans(id) ON DELETE CASCADE,
    CONSTRAINT uk_installments_plan_number UNIQUE (plan_id, installment_number),
    CONSTRAINT chk_installment_status CHECK (
        status IN ('PENDING', 'PAID', 'OVERDUE', 'CANCELLED')
    ),
    CONSTRAINT chk_installment_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_installment_paid_amount CHECK (paid_amount >= 0 AND paid_amount <= amount)
);

CREATE INDEX idx_installments_plan ON installments(plan_id);
CREATE INDEX idx_installments_status ON installments(status);
CREATE INDEX idx_installments_due_date ON installments(due_date);

COMMENT ON TABLE installments IS 'Individual installment payments within a plan';
COMMENT ON COLUMN installments.installment_number IS 'Installment sequence number (1, 2, 3, ...)';
COMMENT ON COLUMN installments.paid_amount IS 'Amount paid so far (for partial payments)';

-- =================================================================
-- SECTION 5: Refund Requests Table
-- =================================================================

CREATE TABLE refund_requests (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Foreign Keys
    invoice_id BIGINT NOT NULL,

    -- Refund Details
    refund_amount DECIMAL(10, 2) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    -- PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED

    -- Request Info
    requested_by BIGINT,
    requested_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Approval Workflow
    approved_by BIGINT,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_by BIGINT,
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,

    -- Processing
    processed_at TIMESTAMP WITH TIME ZONE,

    -- Soft Delete
    deleted BOOLEAN DEFAULT FALSE,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT fk_refunds_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE RESTRICT,
    CONSTRAINT chk_refund_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_refund_amount CHECK (refund_amount > 0)
);

CREATE INDEX idx_refunds_invoice ON refund_requests(invoice_id);
CREATE INDEX idx_refunds_instance ON refund_requests(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_refunds_status ON refund_requests(status) WHERE deleted = FALSE;

-- Trigger for updated_at
CREATE TRIGGER trg_refund_requests_updated_at
    BEFORE UPDATE ON refund_requests
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

COMMENT ON TABLE refund_requests IS 'Refund request workflow with approval';
COMMENT ON COLUMN refund_requests.status IS 'PENDING → APPROVED → COMPLETED';
COMMENT ON COLUMN refund_requests.refund_amount IS 'Amount to refund (must be <= amount_paid)';

-- =================================================================
-- END OF V12 MIGRATION
-- ============================================================================
