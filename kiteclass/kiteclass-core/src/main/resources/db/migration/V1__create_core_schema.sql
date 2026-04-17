-- ============================================================================
-- KITECLASS CORE DATABASE - V1 FOUNDATION SCHEMA
-- Version: 1.0.0
-- Created: 2026-02-27
-- Description: Complete foundation schema for Core service (business logic)
-- ============================================================================

-- =================================================================
-- SECTION 1: STUDENT MODULE
-- =================================================================

CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Profile
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),

    -- Address
    address TEXT,

    -- Avatar
    avatar_url VARCHAR(500),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- PENDING, ACTIVE, INACTIVE, GRADUATED, DROPPED

    -- Notes
    note TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_students_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED')
    )
);

CREATE INDEX idx_students_instance ON students(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_students_email ON students(email) WHERE deleted = FALSE;
CREATE INDEX idx_students_phone ON students(phone);
CREATE INDEX idx_students_status ON students(status) WHERE deleted = FALSE;

COMMENT ON TABLE students IS 'Student profiles (Core DB) - linked via Gateway.users.reference_id (V1)';
COMMENT ON COLUMN students.instance_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN students.status IS 'Student status: PENDING, ACTIVE, INACTIVE, GRADUATED, DROPPED';

-- =================================================================
-- SECTION 2: TEACHER MODULE
-- =================================================================

CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Profile
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),

    -- Professional info
    department VARCHAR(100),
    specialization VARCHAR(100),
    qualifications TEXT,
    bio TEXT,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_teachers_instance ON teachers(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_teachers_email ON teachers(email) WHERE deleted = FALSE;
CREATE INDEX idx_teachers_department ON teachers(department);

COMMENT ON TABLE teachers IS 'Teacher profiles (Core DB) - linked via Gateway.users.reference_id (V1)';
COMMENT ON COLUMN teachers.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 3: CLASS MODULE - Courses
-- =================================================================

CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Category
    category VARCHAR(100),
    -- math, english, physics, etc.

    -- Media
    thumbnail_url TEXT,

    -- Pricing
    suggested_tuition DECIMAL(12, 2),

    -- Settings
    default_sessions INTEGER, -- Số buổi mặc định

    -- Status
    status VARCHAR(50) DEFAULT 'active',

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uk_courses_instance_code UNIQUE (instance_id, code)
);

CREATE INDEX idx_courses_instance ON courses(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_courses_category ON courses(category);
CREATE INDEX idx_courses_status ON courses(status) WHERE deleted = FALSE;

COMMENT ON TABLE courses IS 'Course definitions - templates for classes (V1)';
COMMENT ON COLUMN courses.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 4: CLASS MODULE - Classes
-- =================================================================

CREATE TABLE classes (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    course_id BIGINT NOT NULL REFERENCES courses(id),

    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,

    -- Teacher (Core DB FK)
    teacher_id BIGINT REFERENCES teachers(id),

    -- Schedule
    start_date DATE NOT NULL,
    end_date DATE,

    -- Capacity
    max_students INTEGER DEFAULT 30,

    -- Tuition
    tuition_amount DECIMAL(12, 2) NOT NULL,
    tuition_type VARCHAR(20) DEFAULT 'fixed',
    -- fixed, per_session

    -- Status
    status VARCHAR(50) DEFAULT 'upcoming',
    -- upcoming, ongoing, completed, cancelled

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,  -- User ID from Gateway (no FK constraint across DBs)
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uk_classes_instance_code UNIQUE (instance_id, code),
    CONSTRAINT chk_classes_status CHECK (
        status IN ('upcoming', 'ongoing', 'completed', 'cancelled')
    )
);

CREATE INDEX idx_classes_instance ON classes(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_classes_course ON classes(course_id);
CREATE INDEX idx_classes_teacher ON classes(teacher_id);
CREATE INDEX idx_classes_status ON classes(status) WHERE deleted = FALSE;
CREATE INDEX idx_classes_start_date ON classes(start_date);

COMMENT ON TABLE classes IS 'Class instances (sections) of courses (V1)';
COMMENT ON COLUMN classes.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 5: CLASS MODULE - Class Schedules
-- =================================================================

CREATE TABLE class_schedules (
    id BIGSERIAL PRIMARY KEY,

    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,

    -- Recurring pattern
    day_of_week INTEGER NOT NULL, -- 0=Sunday, 1=Monday, etc.
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_schedules_day CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT chk_schedules_time CHECK (end_time > start_time)
);

CREATE INDEX idx_class_schedules_class ON class_schedules(class_id);
CREATE INDEX idx_class_schedules_day ON class_schedules(day_of_week);

COMMENT ON TABLE class_schedules IS 'Recurring class schedule (weekly) (V1)';

-- =================================================================
-- SECTION 6: CLASS MODULE - Class Sessions
-- =================================================================

CREATE TABLE class_sessions (
    id BIGSERIAL PRIMARY KEY,

    class_id BIGINT NOT NULL REFERENCES classes(id),

    -- Session info
    session_number INTEGER NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    -- Topic/content
    topic VARCHAR(255),
    notes TEXT,

    -- Status
    status VARCHAR(50) DEFAULT 'scheduled',
    -- scheduled, completed, cancelled

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_class_sessions UNIQUE (class_id, session_date)
);

CREATE INDEX idx_class_sessions_class ON class_sessions(class_id);
CREATE INDEX idx_class_sessions_date ON class_sessions(session_date);

COMMENT ON TABLE class_sessions IS 'Individual class sessions (instances) (V1)';

-- =================================================================
-- SECTION 7: CLASS MODULE - Enrollments
-- =================================================================

CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    class_id BIGINT NOT NULL REFERENCES classes(id),
    student_id BIGINT NOT NULL REFERENCES students(id),  -- Core DB FK

    -- Enrollment info
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Status
    status VARCHAR(50) DEFAULT 'active',
    -- active, completed, dropped, transferred

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,  -- User ID from Gateway (no FK constraint)

    CONSTRAINT uk_enrollments UNIQUE (class_id, student_id)
);

CREATE INDEX idx_enrollments_instance ON enrollments(instance_id);
CREATE INDEX idx_enrollments_class ON enrollments(class_id);
CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);

COMMENT ON TABLE enrollments IS 'Student enrollment in classes (V1)';
COMMENT ON COLUMN enrollments.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 8: LEARNING MODULE - Attendance
-- =================================================================

CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    session_id BIGINT NOT NULL REFERENCES class_sessions(id),
    student_id BIGINT NOT NULL REFERENCES students(id),  -- Core DB FK

    -- Attendance status
    status VARCHAR(20) NOT NULL,
    -- present, absent, late, excused

    -- Check-in time
    check_in_time TIMESTAMP WITH TIME ZONE,

    -- Notes
    notes TEXT,

    -- Marked by (User ID from Gateway - no FK constraint)
    marked_by BIGINT,  -- Teacher or Admin user ID
    marked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_attendance UNIQUE (session_id, student_id),
    CONSTRAINT chk_attendance_status CHECK (
        status IN ('present', 'absent', 'late', 'excused')
    )
);

CREATE INDEX idx_attendance_instance ON attendance(instance_id);
CREATE INDEX idx_attendance_session ON attendance(session_id);
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_attendance_status ON attendance(status);

COMMENT ON TABLE attendance IS 'Attendance records for class sessions (V1)';
COMMENT ON COLUMN attendance.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 9: LEARNING MODULE - Grades
-- =================================================================

CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    class_id BIGINT NOT NULL REFERENCES classes(id),
    student_id BIGINT NOT NULL REFERENCES students(id),

    -- Grade info
    grade_type VARCHAR(50) NOT NULL,
    -- quiz, midterm, final, assignment, participation

    title VARCHAR(255) NOT NULL,

    -- Score
    score DECIMAL(5, 2) NOT NULL,
    max_score DECIMAL(5, 2) DEFAULT 10,
    weight DECIMAL(3, 2) DEFAULT 1.0, -- For weighted average

    -- Feedback
    feedback TEXT,

    -- Date
    graded_date DATE NOT NULL,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    graded_by BIGINT,  -- User ID from Gateway (NO FK)

    CONSTRAINT chk_grades_score CHECK (score >= 0 AND score <= max_score)
);

CREATE INDEX idx_grades_instance ON grades(instance_id);
CREATE INDEX idx_grades_class ON grades(class_id);
CREATE INDEX idx_grades_student ON grades(student_id);
CREATE INDEX idx_grades_type ON grades(grade_type);
CREATE INDEX idx_grades_date ON grades(graded_date);

COMMENT ON TABLE grades IS 'Student grades for various assessments (V1)';
COMMENT ON COLUMN grades.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 10: LEARNING MODULE - Assignments
-- =================================================================

CREATE TABLE assignments (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    class_id BIGINT NOT NULL REFERENCES classes(id),

    -- Assignment info
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT,

    -- Attachments
    attachments JSONB DEFAULT '[]',
    -- [{"name": "homework.pdf", "url": "...", "size": 1024}]

    -- Dates
    assigned_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Grading
    max_score DECIMAL(5, 2) DEFAULT 10,

    -- Status
    status VARCHAR(50) DEFAULT 'active',
    -- draft, active, closed

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT  -- User ID from Gateway (NO FK)
);

CREATE INDEX idx_assignments_instance ON assignments(instance_id);
CREATE INDEX idx_assignments_class ON assignments(class_id);
CREATE INDEX idx_assignments_due ON assignments(due_date);
CREATE INDEX idx_assignments_status ON assignments(status);

COMMENT ON TABLE assignments IS 'Assignment definitions and due dates (V1)';
COMMENT ON COLUMN assignments.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 11: LEARNING MODULE - Submissions
-- =================================================================

CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    assignment_id BIGINT NOT NULL REFERENCES assignments(id),
    student_id BIGINT NOT NULL REFERENCES students(id),

    -- Submission content
    content TEXT,
    attachments JSONB DEFAULT '[]',

    -- Status
    status VARCHAR(50) DEFAULT 'submitted',
    -- draft, submitted, late, graded

    -- Grading
    score DECIMAL(5, 2),
    feedback TEXT,
    graded_at TIMESTAMP WITH TIME ZONE,
    graded_by BIGINT,  -- User ID from Gateway (NO FK)

    -- Timestamps
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_submissions UNIQUE (assignment_id, student_id)
);

CREATE INDEX idx_submissions_instance ON submissions(instance_id);
CREATE INDEX idx_submissions_assignment ON submissions(assignment_id);
CREATE INDEX idx_submissions_student ON submissions(student_id);
CREATE INDEX idx_submissions_status ON submissions(status);

COMMENT ON TABLE submissions IS 'Student assignment submissions (V1)';
COMMENT ON COLUMN submissions.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 12: BILLING MODULE - Invoices
-- =================================================================

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    invoice_number VARCHAR(50) NOT NULL,
    -- INV-2025-0001

    student_id BIGINT NOT NULL REFERENCES students(id),
    class_id BIGINT REFERENCES classes(id),

    -- Invoice period
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,

    -- Amount
    subtotal DECIMAL(12, 2) NOT NULL,
    discount DECIMAL(12, 2) DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL,
    amount_paid DECIMAL(12, 2) DEFAULT 0,
    balance_due DECIMAL(12, 2) GENERATED ALWAYS AS (total - amount_paid) STORED,

    -- Dates
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NOT NULL,

    -- Status
    status VARCHAR(50) DEFAULT 'pending',
    -- draft, pending, partially_paid, paid, overdue, cancelled

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,  -- User ID from Gateway (NO FK)

    CONSTRAINT uk_invoices_instance_number UNIQUE (instance_id, invoice_number),
    CONSTRAINT chk_invoices_amounts CHECK (
        subtotal >= 0 AND discount >= 0 AND total >= 0 AND amount_paid >= 0
    ),
    CONSTRAINT chk_invoices_status CHECK (
        status IN ('draft', 'pending', 'partially_paid', 'paid', 'overdue', 'cancelled')
    )
);

CREATE INDEX idx_invoices_instance ON invoices(instance_id);
CREATE INDEX idx_invoices_student ON invoices(student_id);
CREATE INDEX idx_invoices_class ON invoices(class_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE status IN ('pending', 'partially_paid');
CREATE INDEX idx_invoices_period ON invoices(period_start, period_end);

COMMENT ON TABLE invoices IS 'Student invoices for tuition and fees (V1)';
COMMENT ON COLUMN invoices.instance_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN invoices.balance_due IS 'Computed column: total - amount_paid';

-- =================================================================
-- SECTION 13: BILLING MODULE - Invoice Items
-- =================================================================

CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,

    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,

    -- Item details
    description VARCHAR(255) NOT NULL,
    quantity INTEGER DEFAULT 1,
    unit_price DECIMAL(12, 2) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,

    -- Reference
    item_type VARCHAR(50), -- tuition, material, other
    reference_id BIGINT, -- class_id, session_id, etc.

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);

COMMENT ON TABLE invoice_items IS 'Line items for invoices (tuition, materials, etc.) (V1)';

-- =================================================================
-- SECTION 14: BILLING MODULE - Payments
-- =================================================================

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    payment_number VARCHAR(50) NOT NULL,
    -- PAY-2025-0001

    invoice_id BIGINT NOT NULL REFERENCES invoices(id),

    -- Amount
    amount DECIMAL(12, 2) NOT NULL,

    -- Payment method
    payment_method VARCHAR(50) NOT NULL,
    -- cash, bank_transfer, momo, zalopay, qr

    -- Transaction info
    transaction_id VARCHAR(100),

    -- QR Payment
    qr_code_url TEXT,

    -- Payer info (for parent payments)
    payer_id BIGINT,  -- User ID from Gateway (NO FK)
    payer_name VARCHAR(255),

    -- Status
    status VARCHAR(50) DEFAULT 'pending',
    -- pending, completed, failed, refunded

    -- Notes
    notes TEXT,
    receipt_url TEXT,

    -- Timestamps
    paid_at TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    received_by BIGINT,  -- User ID from Gateway (NO FK)

    CONSTRAINT uk_payments_instance_number UNIQUE (instance_id, payment_number),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_status CHECK (
        status IN ('pending', 'completed', 'failed', 'refunded')
    )
);

CREATE INDEX idx_payments_instance ON payments(instance_id);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_payer ON payments(payer_id);
CREATE INDEX idx_payments_date ON payments(paid_at);

COMMENT ON TABLE payments IS 'Payment records for invoices (V1)';
COMMENT ON COLUMN payments.instance_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN payments.payment_method IS 'Method: cash, bank_transfer, momo, zalopay, qr';

-- =================================================================
-- SECTION 15: GAMIFICATION MODULE - Point Rules
-- =================================================================

CREATE TABLE point_rules (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    -- ATTENDANCE, GRADE_A, ASSIGNMENT_SUBMIT, etc.

    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Points
    points INTEGER NOT NULL,

    -- Event trigger
    event_type VARCHAR(50) NOT NULL,
    -- attendance_present, grade_submitted, assignment_submitted

    -- Conditions (JSONB for flexibility)
    conditions JSONB DEFAULT '{}',
    -- {"min_score": 8, "on_time": true}

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_point_rules_instance_code UNIQUE (instance_id, code)
);

CREATE INDEX idx_point_rules_instance ON point_rules(instance_id);
CREATE INDEX idx_point_rules_code ON point_rules(code);
CREATE INDEX idx_point_rules_event_type ON point_rules(event_type);

COMMENT ON TABLE point_rules IS 'Gamification rules: point allocation per event (V1)';
COMMENT ON COLUMN point_rules.instance_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN point_rules.conditions IS 'JSONB conditions: min_score, on_time, etc.';

-- =================================================================
-- SECTION 16: GAMIFICATION MODULE - Student Points
-- =================================================================

CREATE TABLE student_points (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    student_id BIGINT NOT NULL REFERENCES students(id),
    rule_id BIGINT REFERENCES point_rules(id),

    -- Points
    points INTEGER NOT NULL,

    -- Reference
    reference_type VARCHAR(50), -- attendance, grade, assignment
    reference_id BIGINT,

    -- Description
    description VARCHAR(255),

    -- Timestamp
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_student_points_instance ON student_points(instance_id);
CREATE INDEX idx_student_points_student ON student_points(student_id);
CREATE INDEX idx_student_points_earned ON student_points(earned_at);
CREATE INDEX idx_student_points_rule ON student_points(rule_id);

COMMENT ON TABLE student_points IS 'Point transactions per student (V1)';
COMMENT ON COLUMN student_points.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 17: GAMIFICATION MODULE - Badges
-- =================================================================

CREATE TABLE badges (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Visual
    icon_url TEXT,
    color VARCHAR(20),

    -- Requirements
    requirement_type VARCHAR(50) NOT NULL,
    -- points, streak, special

    requirement_value INTEGER,
    -- e.g., 1000 points, 10 day streak

    requirement_conditions JSONB DEFAULT '{}',

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_badges_instance_code UNIQUE (instance_id, code)
);

CREATE INDEX idx_badges_instance ON badges(instance_id);
CREATE INDEX idx_badges_code ON badges(code);
CREATE INDEX idx_badges_requirement_type ON badges(requirement_type);

COMMENT ON TABLE badges IS 'Badge definitions for gamification rewards (V1)';
COMMENT ON COLUMN badges.instance_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN badges.requirement_type IS 'Type: points, streak, special';

-- =================================================================
-- SECTION 18: GAMIFICATION MODULE - Student Badges
-- =================================================================

CREATE TABLE student_badges (
    id BIGSERIAL PRIMARY KEY,

    student_id BIGINT NOT NULL REFERENCES students(id),
    badge_id BIGINT NOT NULL REFERENCES badges(id),

    -- Earned info
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_student_badges UNIQUE (student_id, badge_id)
);

CREATE INDEX idx_student_badges_student ON student_badges(student_id);
CREATE INDEX idx_student_badges_badge ON student_badges(badge_id);

COMMENT ON TABLE student_badges IS 'Badges earned by students (achievement tracking) (V1)';

-- =================================================================
-- SECTION 19: GAMIFICATION MODULE - Rewards
-- =================================================================

CREATE TABLE rewards (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Visual
    image_url TEXT,

    -- Cost
    points_required INTEGER NOT NULL,

    -- Inventory
    quantity_available INTEGER, -- NULL = unlimited
    quantity_redeemed INTEGER DEFAULT 0,

    -- Validity
    valid_from DATE,
    valid_until DATE,

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_rewards_instance_code UNIQUE (instance_id, code)
);

CREATE INDEX idx_rewards_instance ON rewards(instance_id);
CREATE INDEX idx_rewards_code ON rewards(code);
CREATE INDEX idx_rewards_points_required ON rewards(points_required);

COMMENT ON TABLE rewards IS 'Reward catalog redeemable with student points (V1)';
COMMENT ON COLUMN rewards.instance_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN rewards.quantity_available IS 'NULL = unlimited inventory';

-- =================================================================
-- SECTION 20: GAMIFICATION MODULE - Reward Redemptions
-- =================================================================

CREATE TABLE reward_redemptions (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    student_id BIGINT NOT NULL REFERENCES students(id),
    reward_id BIGINT NOT NULL REFERENCES rewards(id),

    -- Points spent
    points_spent INTEGER NOT NULL,

    -- Status
    status VARCHAR(50) DEFAULT 'pending',
    -- pending, approved, delivered, cancelled

    -- Approval
    approved_by BIGINT,  -- User ID from Gateway (NO FK)
    approved_at TIMESTAMP WITH TIME ZONE,

    -- Delivery
    delivered_at TIMESTAMP WITH TIME ZONE,

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_redemptions_instance ON reward_redemptions(instance_id);
CREATE INDEX idx_redemptions_student ON reward_redemptions(student_id);
CREATE INDEX idx_redemptions_reward ON reward_redemptions(reward_id);
CREATE INDEX idx_redemptions_status ON reward_redemptions(status);

COMMENT ON TABLE reward_redemptions IS 'Reward redemption requests and fulfillment tracking (V1)';
COMMENT ON COLUMN reward_redemptions.instance_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN reward_redemptions.status IS 'Workflow: pending → approved → delivered';

-- =================================================================
-- SECTION 21: GAMIFICATION MODULE - Grading Scales
-- =================================================================

CREATE TABLE grading_scales (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Grade info
    grade VARCHAR(5) NOT NULL,     -- A+, A, B+, B, C+, C, D+, D, F
    min_percentage DECIMAL(5, 2) NOT NULL,  -- 95.00
    max_percentage DECIMAL(5, 2) NOT NULL,  -- 100.00
    gpa DECIMAL(3, 2) NOT NULL,    -- 4.0

    -- Description
    description VARCHAR(255),

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_grading_scales_instance_grade UNIQUE (instance_id, grade)
);

CREATE INDEX idx_grading_scales_instance ON grading_scales(instance_id);

COMMENT ON TABLE grading_scales IS 'Grading scales for GPA calculation (V1)';
COMMENT ON COLUMN grading_scales.instance_id IS 'Tenant ID for multi-tenant isolation';

-- =================================================================
-- SECTION 22: Triggers for updated_at
-- =================================================================

CREATE OR REPLACE FUNCTION update_core_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_students_updated_at
    BEFORE UPDATE ON students
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_teachers_updated_at
    BEFORE UPDATE ON teachers
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_courses_updated_at
    BEFORE UPDATE ON courses
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_classes_updated_at
    BEFORE UPDATE ON classes
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_class_sessions_updated_at
    BEFORE UPDATE ON class_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_enrollments_updated_at
    BEFORE UPDATE ON enrollments
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_attendance_updated_at
    BEFORE UPDATE ON attendance
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_grades_updated_at
    BEFORE UPDATE ON grades
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_assignments_updated_at
    BEFORE UPDATE ON assignments
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_submissions_updated_at
    BEFORE UPDATE ON submissions
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_point_rules_updated_at
    BEFORE UPDATE ON point_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_rewards_updated_at
    BEFORE UPDATE ON rewards
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

CREATE TRIGGER trg_reward_redemptions_updated_at
    BEFORE UPDATE ON reward_redemptions
    FOR EACH ROW
    EXECUTE FUNCTION update_core_updated_at();

-- =================================================================
-- SECTION 23: Views
-- =================================================================

CREATE OR REPLACE VIEW student_total_points AS
SELECT
    instance_id,
    student_id,
    SUM(points) as total_points,
    COUNT(*) as transaction_count
FROM student_points
GROUP BY instance_id, student_id;

COMMENT ON VIEW student_total_points IS 'Aggregated points per student (V1)';

-- =================================================================
-- END OF V1 CORE SCHEMA
-- ============================================================================
