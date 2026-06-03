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
-- Assignment entity drift
-- ---------------------------------------------------------------------------
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS weight_percent NUMERIC(5, 2) NOT NULL DEFAULT 0;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS allow_late_submission BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS late_penalty_percent NUMERIC(5, 2);
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_assignments_deleted ON assignments(deleted);

-- ---------------------------------------------------------------------------
-- Attendance entity drift
-- ---------------------------------------------------------------------------
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS enrollment_id BIGINT;
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS marked_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS points_awarded INTEGER DEFAULT 0;
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_attendance_enrollment ON attendance(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_attendance_deleted ON attendance(deleted);

-- ---------------------------------------------------------------------------
-- Submission entity drift
-- ---------------------------------------------------------------------------
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS submission_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS content_url VARCHAR(500);
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS adjusted_score NUMERIC(5, 2);
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_submissions_deleted ON submissions(deleted);

-- ---------------------------------------------------------------------------
-- GradingScale entity drift
-- ---------------------------------------------------------------------------
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS scale_name VARCHAR(100) NOT NULL DEFAULT 'Default';
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS letter_grade VARCHAR(5) NOT NULL DEFAULT 'F';
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS min_score NUMERIC(5, 2) NOT NULL DEFAULT 0;
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS max_score NUMERIC(5, 2) NOT NULL DEFAULT 100;
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS gpa_value NUMERIC(3, 2) NOT NULL DEFAULT 0;
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS is_passing BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
ALTER TABLE grading_scales ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_grading_scales_deleted ON grading_scales(deleted);

-- ---------------------------------------------------------------------------
-- Grade component + transcript entity drift
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grade_components (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    grade_id BIGINT NOT NULL REFERENCES grades(id),
    component_type VARCHAR(50) NOT NULL,
    component_name VARCHAR(255) NOT NULL,
    component_ref_id BIGINT,
    score NUMERIC(5, 2) NOT NULL,
    max_score NUMERIC(5, 2) NOT NULL,
    weight_percent NUMERIC(5, 2) NOT NULL,
    weighted_score NUMERIC(5, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_grade_components_ref UNIQUE (grade_id, component_type, component_ref_id)
);

CREATE INDEX IF NOT EXISTS idx_grade_components_instance ON grade_components(instance_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_grade_components_grade ON grade_components(grade_id);

CREATE TABLE IF NOT EXISTS transcripts (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    student_id BIGINT NOT NULL REFERENCES students(id),
    semester VARCHAR(50),
    academic_year INTEGER,
    total_credits NUMERIC(5, 2) NOT NULL DEFAULT 0,
    semester_gpa NUMERIC(3, 2),
    cumulative_gpa NUMERIC(3, 2),
    total_courses INTEGER NOT NULL DEFAULT 0,
    passed_courses INTEGER NOT NULL DEFAULT 0,
    failed_courses INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_transcripts_student_semester UNIQUE (student_id, semester, academic_year)
);

CREATE INDEX IF NOT EXISTS idx_transcripts_instance ON transcripts(instance_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transcripts_student ON transcripts(student_id);

-- ---------------------------------------------------------------------------
-- Storage + user preferences entity drift
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS uploaded_files (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    uploader_id BIGINT NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    access_level VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_uploaded_files_instance_id ON uploaded_files(instance_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_status ON uploaded_files(status);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_expires_at ON uploaded_files(expires_at);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_uploader_id ON uploaded_files(uploader_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_deleted ON uploaded_files(deleted);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_deleted_at ON uploaded_files(deleted_at);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_instance_status ON uploaded_files(instance_id, status);

CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    language VARCHAR(5) NOT NULL DEFAULT 'VI',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    theme VARCHAR(10) NOT NULL DEFAULT 'LIGHT',
    notification_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_user_preferences_instance ON user_preferences(instance_id) WHERE deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_preferences_instance_user ON user_preferences(instance_id, user_id) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------------
-- Landing page + subject grade entity drift
-- ---------------------------------------------------------------------------
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS teachers JSONB;
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS programs JSONB;
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS pricing_tiers JSONB;
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS testimonials JSONB;
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS faqs JSONB;
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS stats JSONB;

ALTER TABLE subject_grades ADD COLUMN IF NOT EXISTS weight NUMERIC(4, 2);
ALTER TABLE subject_grades ADD COLUMN IF NOT EXISTS status VARCHAR(16);
ALTER TABLE subject_grades ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;
ALTER TABLE subject_grades ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;

-- ---------------------------------------------------------------------------
-- Teacher assignment + storage quota + invoice adjustment entity drift
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS teacher_classes (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    class_id BIGINT NOT NULL REFERENCES classes(id),
    role VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    CONSTRAINT uk_teacher_classes_teacher_class UNIQUE (teacher_id, class_id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_classes_teacher_id ON teacher_classes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_classes_class_id ON teacher_classes(class_id);

CREATE TABLE IF NOT EXISTS storage_quotas (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL UNIQUE,
    tier VARCHAR(20) NOT NULL DEFAULT 'FREE',
    used_bytes BIGINT NOT NULL DEFAULT 0,
    quota_bytes BIGINT NOT NULL,
    last_calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_storage_quotas_instance_id ON storage_quotas(instance_id);
CREATE INDEX IF NOT EXISTS idx_storage_quotas_tier ON storage_quotas(tier);

CREATE TABLE IF NOT EXISTS invoice_adjustments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    type VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_adjustments_invoice ON invoice_adjustments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_adjustments_type ON invoice_adjustments(type);

-- ---------------------------------------------------------------------------
-- Installment/refund/payment webhook entity drift
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS installment_plans (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    number_of_installments INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMPTZ,
    approved_by BIGINT,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_plans_invoice UNIQUE (invoice_id)
);

CREATE INDEX IF NOT EXISTS idx_plans_invoice ON installment_plans(invoice_id);
CREATE INDEX IF NOT EXISTS idx_plans_instance ON installment_plans(instance_id);
CREATE INDEX IF NOT EXISTS idx_plans_status ON installment_plans(status);

CREATE TABLE IF NOT EXISTS installments (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES installment_plans(id),
    installment_number INTEGER NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_amount NUMERIC(10, 2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_installments_plan_number UNIQUE (plan_id, installment_number)
);

CREATE INDEX IF NOT EXISTS idx_installments_plan ON installments(plan_id);
CREATE INDEX IF NOT EXISTS idx_installments_status ON installments(status);
CREATE INDEX IF NOT EXISTS idx_installments_due_date ON installments(due_date);

CREATE TABLE IF NOT EXISTS refund_requests (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    refund_amount NUMERIC(10, 2) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    requested_by BIGINT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMPTZ,
    rejected_by BIGINT,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_refunds_invoice ON refund_requests(invoice_id);
CREATE INDEX IF NOT EXISTS idx_refunds_instance ON refund_requests(instance_id);
CREATE INDEX IF NOT EXISTS idx_refunds_status ON refund_requests(status);

CREATE TABLE IF NOT EXISTS payment_webhook_logs (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    payment_id BIGINT,
    gateway VARCHAR(50) NOT NULL,
    request_payload TEXT NOT NULL,
    signature VARCHAR(512),
    signature_valid BOOLEAN,
    processed BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_instance ON payment_webhook_logs(instance_id);
CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_payment ON payment_webhook_logs(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_gateway ON payment_webhook_logs(gateway);

-- ---------------------------------------------------------------------------
-- LMS entity drift
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS course_modules (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    course_id BIGINT NOT NULL REFERENCES courses(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    order_number INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_course_modules_course_order UNIQUE (course_id, order_number, instance_id)
);

CREATE INDEX IF NOT EXISTS idx_course_modules_course_id ON course_modules(course_id);
CREATE INDEX IF NOT EXISTS idx_course_modules_instance_id ON course_modules(instance_id);

CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    module_id BIGINT NOT NULL REFERENCES course_modules(id),
    title VARCHAR(200) NOT NULL,
    content TEXT,
    video_url VARCHAR(500),
    is_trial BOOLEAN NOT NULL DEFAULT FALSE,
    order_number INTEGER NOT NULL,
    estimated_duration INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_lessons_module_order UNIQUE (module_id, order_number, instance_id)
);

CREATE INDEX IF NOT EXISTS idx_lessons_module_id ON lessons(module_id);
CREATE INDEX IF NOT EXISTS idx_lessons_is_trial ON lessons(is_trial);
CREATE INDEX IF NOT EXISTS idx_lessons_instance_id ON lessons(instance_id);

CREATE TABLE IF NOT EXISTS learning_resources (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id),
    type VARCHAR(20) NOT NULL,
    url VARCHAR(500) NOT NULL,
    title VARCHAR(200) NOT NULL,
    file_size BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_learning_resources_lesson_id ON learning_resources(lesson_id);
CREATE INDEX IF NOT EXISTS idx_learning_resources_type ON learning_resources(type);
CREATE INDEX IF NOT EXISTS idx_learning_resources_instance_id ON learning_resources(instance_id);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_lesson_progress_user_lesson UNIQUE (user_id, lesson_id, instance_id)
);

CREATE INDEX IF NOT EXISTS idx_lesson_progress_user_id ON lesson_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_lesson_progress_lesson_id ON lesson_progress(lesson_id);
CREATE INDEX IF NOT EXISTS idx_lesson_progress_completed ON lesson_progress(completed);
CREATE INDEX IF NOT EXISTS idx_lesson_progress_instance_id ON lesson_progress(instance_id);

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

-- ---------------------------------------------------------------------------
-- RLS for V79 tenant-scoped tables
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    table_name text;
    instance_id_tables text[] := ARRAY[
        'grade_components',
        'transcripts',
        'uploaded_files',
        'user_preferences',
        'storage_quotas',
        'installment_plans',
        'refund_requests',
        'payment_webhook_logs',
        'course_modules',
        'lessons',
        'learning_resources',
        'lesson_progress'
    ];
BEGIN
    FOREACH table_name IN ARRAY instance_id_tables
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', table_name);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            'USING ('
            '    COALESCE(current_setting(''app.is_platform_admin'', true)::boolean, false) '
            '    OR instance_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid'
            ') '
            'WITH CHECK ('
            '    COALESCE(current_setting(''app.is_platform_admin'', true)::boolean, false) '
            '    OR instance_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid'
            ')',
            table_name
        );
    END LOOP;
END $$;

ALTER TABLE teacher_classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_classes FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON teacher_classes;
CREATE POLICY tenant_isolation ON teacher_classes
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR EXISTS (
            SELECT 1
            FROM teachers
            WHERE teachers.id = teacher_classes.teacher_id
              AND teachers.instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        )
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR EXISTS (
            SELECT 1
            FROM teachers
            WHERE teachers.id = teacher_classes.teacher_id
              AND teachers.instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        )
    );

ALTER TABLE invoice_adjustments ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_adjustments FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON invoice_adjustments;
CREATE POLICY tenant_isolation ON invoice_adjustments
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR EXISTS (
            SELECT 1
            FROM invoices
            WHERE invoices.id = invoice_adjustments.invoice_id
              AND invoices.instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        )
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR EXISTS (
            SELECT 1
            FROM invoices
            WHERE invoices.id = invoice_adjustments.invoice_id
              AND invoices.instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        )
    );

ALTER TABLE installments ENABLE ROW LEVEL SECURITY;
ALTER TABLE installments FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON installments;
CREATE POLICY tenant_isolation ON installments
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR EXISTS (
            SELECT 1
            FROM installment_plans
            WHERE installment_plans.id = installments.plan_id
              AND installment_plans.instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        )
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR EXISTS (
            SELECT 1
            FROM installment_plans
            WHERE installment_plans.id = installments.plan_id
              AND installment_plans.instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        )
    );
