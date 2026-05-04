-- GAP-057 Phase 1 (Wave 18a Bucket C): Teacher payroll tables.
--
-- Phase 1 ships HOURLY type only. Schema columns for SALARY/COMMISSION/HYBRID
-- are present so admin UI can prefill / migrate without ALTER in Phase 2
-- (GAP-057b), but the Phase 1 PayrollService throws UnsupportedOperationException
-- for non-HOURLY types.
--
-- Phase 2 (GAP-057b) deferred items: VN tax (TNCN) progressive deductions, BHXH
-- + BHYT mandatory percentages, payslip PDF (depends on GAP-047), bank export,
-- admin run/approve UI workflow, audit log expansion.

CREATE TABLE payroll_configs (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    teacher_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    hourly_rate DECIMAL(15, 2),
    base_salary DECIMAL(15, 2),
    commission_percent DECIMAL(5, 2),
    gvcn_allowance DECIMAL(15, 2),
    bonuses TEXT,

    -- BaseEntity audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_payroll_config_type CHECK (
        type IN ('SALARY', 'HOURLY', 'COMMISSION', 'HYBRID')
    ),
    CONSTRAINT chk_payroll_config_hourly_rate_positive CHECK (
        hourly_rate IS NULL OR hourly_rate > 0
    ),
    CONSTRAINT chk_payroll_config_commission_range CHECK (
        commission_percent IS NULL OR (commission_percent >= 0 AND commission_percent <= 100)
    )
);

-- BR-PAYROLL-001: One config per teacher per tenant (excluding soft-deleted)
CREATE UNIQUE INDEX uk_payroll_configs_teacher_tenant
    ON payroll_configs(teacher_id, instance_id)
    WHERE deleted = FALSE;

CREATE INDEX idx_payroll_configs_teacher_id ON payroll_configs(teacher_id);
CREATE INDEX idx_payroll_configs_instance_id ON payroll_configs(instance_id);
CREATE INDEX idx_payroll_configs_type ON payroll_configs(type);

COMMENT ON TABLE payroll_configs IS
    'GAP-057 Phase 1: per-teacher payroll configuration. Phase 1 calc engine only consults type+hourly_rate; baseSalary/commissionPercent/gvcnAllowance/bonuses persisted but inert until GAP-057b.';
COMMENT ON COLUMN payroll_configs.type IS 'PayrollType enum; Phase 1 supports HOURLY only.';
COMMENT ON COLUMN payroll_configs.hourly_rate IS 'VND/hour. Required when type=HOURLY (BR-PAYROLL-002).';
COMMENT ON COLUMN payroll_configs.bonuses IS 'JSON map (Phase 2 GAP-057b will pair with @JdbcTypeCode(SqlTypes.JSON)).';

CREATE TABLE payroll_periods (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    teacher_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    hours_worked DECIMAL(7, 2),
    gross_amount DECIMAL(15, 2) NOT NULL,
    deductions DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- BaseEntity audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_payroll_period_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_payroll_period_status CHECK (
        status IN ('DRAFT', 'APPROVED', 'PAID')
    ),
    CONSTRAINT chk_payroll_period_amounts_nonneg CHECK (
        gross_amount >= 0 AND deductions >= 0 AND net_amount >= 0
    )
);

CREATE INDEX idx_payroll_periods_teacher_id ON payroll_periods(teacher_id);
CREATE INDEX idx_payroll_periods_instance_id ON payroll_periods(instance_id);
CREATE INDEX idx_payroll_periods_dates ON payroll_periods(start_date, end_date);
CREATE INDEX idx_payroll_periods_status ON payroll_periods(status);

COMMENT ON TABLE payroll_periods IS
    'GAP-057 Phase 1: one row per teacher per pay period. Phase 1 ships DRAFT only with deductions=0; APPROVED/PAID transitions + TNCN/BHXH/BHYT in Phase 2 (GAP-057b).';
COMMENT ON COLUMN payroll_periods.hours_worked IS 'Sum of ClassSession durations in [start_date, end_date]. Phase 1 derives at calc time.';
COMMENT ON COLUMN payroll_periods.deductions IS 'Phase 1: always 0 (BR-PAYROLL-006). Phase 2: TNCN + BHXH + BHYT progressive computation.';
