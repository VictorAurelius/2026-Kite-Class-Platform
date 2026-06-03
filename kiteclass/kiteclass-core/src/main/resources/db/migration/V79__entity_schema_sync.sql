-- Wave local-doable-14 Bucket B: entity/schema sync for drift surfaced by Wave 13.
--
-- Boundary calls:
--   * This migration is additive and structural only. Money precision, timestamp
--     normalization, and enum CHECK uppercase harmonization remain Bucket D scope.
--   * `leads` and `contact_messages` are real marketing entities with repository
--     and service flows, so DB is backfilled to match entity state.
--   * `class_sessions.instance_id` is derived from the parent `classes` row for
--     existing data, then made NOT NULL to satisfy BaseEntity.

-- ---------------------------------------------------------------------------
-- Marketing ghost entities: Lead + ContactMessage
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leads (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    source VARCHAR(50) NOT NULL DEFAULT 'LANDING_PAGE',
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    course_interest_id BIGINT REFERENCES courses(id),
    message TEXT,
    registration_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_contacted_at TIMESTAMPTZ,
    converted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_leads_instance_email UNIQUE (instance_id, email),
    CONSTRAINT chk_leads_source CHECK (
        source IN ('LANDING_PAGE', 'CONTACT_FORM', 'TRIAL_SIGNUP', 'REFERRAL', 'SOCIAL_MEDIA', 'OTHER')
    ),
    CONSTRAINT chk_leads_status CHECK (
        status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CONVERTED', 'LOST', 'INVALID')
    )
);

CREATE INDEX IF NOT EXISTS idx_leads_instance ON leads(instance_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_leads_status ON leads(status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_leads_course_interest ON leads(course_interest_id);
CREATE INDEX IF NOT EXISTS idx_leads_registration_date ON leads(registration_date DESC);

CREATE TABLE IF NOT EXISTS contact_messages (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    subject VARCHAR(300),
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    read_by VARCHAR(100),
    replied BOOLEAN NOT NULL DEFAULT FALSE,
    replied_at TIMESTAMPTZ,
    reply_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_contact_messages_instance ON contact_messages(instance_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_contact_messages_unread ON contact_messages(instance_id, is_read) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_contact_messages_created_at ON contact_messages(created_at DESC);

ALTER TABLE leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE leads FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON leads;
CREATE POLICY tenant_isolation ON leads
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

ALTER TABLE contact_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE contact_messages FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON contact_messages;
CREATE POLICY tenant_isolation ON contact_messages
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

-- ---------------------------------------------------------------------------
-- Invoice entity drift
-- ---------------------------------------------------------------------------
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS enrollment_id BIGINT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_invoices_enrollment
    ON invoices(enrollment_id)
    WHERE enrollment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_invoices_enrollment ON invoices(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_invoices_deleted ON invoices(deleted);

-- ---------------------------------------------------------------------------
-- Payment entity drift
-- ---------------------------------------------------------------------------
ALTER TABLE payments ADD COLUMN IF NOT EXISTS installment_id BIGINT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_transaction_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_url TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_response TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(50);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS initiated_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS failed_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS failure_reason TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE payments
SET payment_status = UPPER(COALESCE(status, 'pending'))
WHERE payment_status IS NULL;

UPDATE payments
SET initiated_at = created_at
WHERE initiated_at IS NULL;

UPDATE payments
SET transaction_id = 'legacy-' || id::text
WHERE transaction_id IS NULL;

ALTER TABLE payments ALTER COLUMN payment_status SET DEFAULT 'PENDING';
ALTER TABLE payments ALTER COLUMN payment_status SET NOT NULL;
ALTER TABLE payments ALTER COLUMN initiated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE payments ALTER COLUMN initiated_at SET NOT NULL;
ALTER TABLE payments ALTER COLUMN transaction_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_transaction_id ON payments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_status ON payments(payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_deleted ON payments(deleted);
CREATE INDEX IF NOT EXISTS idx_payments_installment ON payments(installment_id);

-- ---------------------------------------------------------------------------
-- ClassSession entity drift
-- ---------------------------------------------------------------------------
ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS instance_id UUID;
ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS location VARCHAR(200);
ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS attendance_taken BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE class_sessions ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE class_sessions cs
SET instance_id = c.instance_id
FROM classes c
WHERE cs.class_id = c.id
  AND cs.instance_id IS NULL;

DO $$
DECLARE
    missing_count bigint;
BEGIN
    SELECT COUNT(*) INTO missing_count
    FROM class_sessions
    WHERE instance_id IS NULL;

    IF missing_count > 0 THEN
        RAISE EXCEPTION 'class_sessions has % rows that cannot be backfilled to instance_id', missing_count;
    END IF;
END $$;

ALTER TABLE class_sessions ALTER COLUMN instance_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_class_sessions_instance_id ON class_sessions(instance_id);
CREATE INDEX IF NOT EXISTS idx_class_sessions_status ON class_sessions(status);

ALTER TABLE class_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_sessions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON class_sessions;
CREATE POLICY tenant_isolation ON class_sessions
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );
