-- Wave local-doable-14 Bucket D — type harmonize for money, time, and enum values.
--
-- Risk note: this migration rewrites numeric precision, timestamp storage, and
-- invoice/payment enum constraints. Run during a short write-freeze window:
-- (1) freeze writes for 5 minutes, (2) run migration, (3) run boot-validate and
-- type-consistency checks, (4) unfreeze writes.
--
-- Boundary call: Bucket D ships after A/B/C in the wave plan, but this worktree
-- currently only has Bucket A migrations. Keep the planned V81 name to avoid
-- renumber churn when B/C land.
--
-- Boundary call: columns ending `_date` that are business calendar dates
-- (`LocalDate`) remain DATE. This migration only converts existing
-- `timestamp without time zone` columns to TIMESTAMPTZ.

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS chk_invoices_status;
DROP INDEX IF EXISTS idx_invoices_due_date;

UPDATE invoices
SET status = CASE status
    WHEN 'draft' THEN 'DRAFT'
    WHEN 'pending' THEN 'SENT'
    WHEN 'partially_paid' THEN 'PARTIAL'
    WHEN 'paid' THEN 'PAID'
    WHEN 'overdue' THEN 'OVERDUE'
    WHEN 'cancelled' THEN 'CANCELLED'
    WHEN 'refunded' THEN 'REFUNDED'
    ELSE UPPER(status)
END
WHERE status IS NOT NULL;

ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payments_status;

UPDATE payments
SET status = CASE status
    WHEN 'pending' THEN 'PENDING'
    WHEN 'processing' THEN 'PROCESSING'
    WHEN 'completed' THEN 'COMPLETED'
    WHEN 'failed' THEN 'FAILED'
    WHEN 'cancelled' THEN 'FAILED'
    WHEN 'refunded' THEN 'REFUNDED'
    ELSE UPPER(status)
END
WHERE status IS NOT NULL;

ALTER TABLE invoices DROP COLUMN IF EXISTS balance_due;

ALTER TABLE invoices
    ALTER COLUMN subtotal TYPE NUMERIC(19, 2),
    ALTER COLUMN discount TYPE NUMERIC(19, 2),
    ALTER COLUMN total TYPE NUMERIC(19, 2),
    ALTER COLUMN amount_paid TYPE NUMERIC(19, 2);

ALTER TABLE invoices
    ADD COLUMN balance_due NUMERIC(19, 2)
    GENERATED ALWAYS AS (total - amount_paid) STORED;

ALTER TABLE invoice_items
    ALTER COLUMN unit_price TYPE NUMERIC(19, 2),
    ALTER COLUMN amount TYPE NUMERIC(19, 2);

ALTER TABLE payments
    ALTER COLUMN amount TYPE NUMERIC(19, 2);

ALTER TABLE courses
    ALTER COLUMN price TYPE NUMERIC(19, 2);

ALTER TABLE classes
    ALTER COLUMN tuition_amount TYPE NUMERIC(19, 2);

ALTER TABLE enrollments
    ALTER COLUMN tuition_amount TYPE NUMERIC(19, 2),
    ALTER COLUMN final_amount TYPE NUMERIC(19, 2);

ALTER TABLE payroll_periods
    ALTER COLUMN gross_amount TYPE NUMERIC(19, 2),
    ALTER COLUMN net_amount TYPE NUMERIC(19, 2);

DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT table_schema, table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND data_type = 'timestamp without time zone'
          AND (
              column_name LIKE '%\_at' ESCAPE '\'
              OR column_name LIKE '%\_time' ESCAPE '\'
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN %I TYPE TIMESTAMPTZ USING %I AT TIME ZONE ''UTC''',
            r.table_schema,
            r.table_name,
            r.column_name,
            r.column_name
        );
    END LOOP;
END $$;

ALTER TABLE invoices
    ALTER COLUMN status SET DEFAULT 'DRAFT';

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoices_status
    CHECK (status IN ('DRAFT', 'SENT', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED', 'REFUNDED'));

CREATE INDEX idx_invoices_due_date
    ON invoices(due_date)
    WHERE status IN ('SENT', 'PARTIAL');

ALTER TABLE payments
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED'));

COMMENT ON COLUMN invoices.discount IS
    'Discount amount in VND. NUMERIC(19,2) standardized by Wave 14 Bucket D.';
COMMENT ON COLUMN payments.status IS
    'UPPERCASE enum value aligned with PaymentStatus. Legacy cancelled rows map to FAILED.';
