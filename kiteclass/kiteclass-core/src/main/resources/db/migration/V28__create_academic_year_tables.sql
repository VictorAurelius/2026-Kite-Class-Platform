-- =========================================================================
-- V28: Academic Year + Semester + Holiday tables
-- =========================================================================
-- Context: GAP-053, ADR-002
-- Purpose: Top-level organizing structure for K-12 + university tenants
-- Breaking change: NO (additive only, existing classes continue to work)
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. academic_years table
-- -------------------------------------------------------------------------
CREATE TABLE academic_years (
    id             BIGSERIAL PRIMARY KEY,
    instance_id    UUID         NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    start_date     DATE         NOT NULL,
    end_date       DATE         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING',

    -- Audit fields (per BaseEntity)
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_academic_year_dates CHECK (end_date > start_date),
    CONSTRAINT chk_academic_year_status CHECK (status IN ('UPCOMING', 'CURRENT', 'COMPLETED'))
);

CREATE UNIQUE INDEX idx_academic_years_tenant_name
    ON academic_years(instance_id, name) WHERE deleted = FALSE;
CREATE INDEX idx_academic_years_status ON academic_years(status) WHERE deleted = FALSE;
CREATE INDEX idx_academic_years_instance_id ON academic_years(instance_id);
CREATE INDEX idx_academic_years_deleted ON academic_years(deleted);

-- -------------------------------------------------------------------------
-- 2. semesters table
-- -------------------------------------------------------------------------
CREATE TABLE semesters (
    id                 BIGSERIAL PRIMARY KEY,
    instance_id        UUID         NOT NULL,
    academic_year_id   BIGINT       NOT NULL REFERENCES academic_years(id),
    type               VARCHAR(20)  NOT NULL,
    name               VARCHAR(100),
    start_date         DATE         NOT NULL,
    end_date           DATE         NOT NULL,
    exam_start_date    DATE,
    exam_end_date      DATE,

    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    version            BIGINT       NOT NULL DEFAULT 0,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_semester_dates CHECK (end_date > start_date),
    CONSTRAINT chk_semester_exam_dates CHECK (
        (exam_start_date IS NULL AND exam_end_date IS NULL) OR
        (exam_start_date IS NOT NULL AND exam_end_date IS NOT NULL AND exam_end_date >= exam_start_date)
    ),
    CONSTRAINT chk_semester_type CHECK (type IN ('HK1', 'HK2', 'SUMMER'))
);

CREATE UNIQUE INDEX idx_semesters_year_type
    ON semesters(academic_year_id, type) WHERE deleted = FALSE;
CREATE INDEX idx_semesters_instance_id ON semesters(instance_id);
CREATE INDEX idx_semesters_deleted ON semesters(deleted);

-- -------------------------------------------------------------------------
-- 3. holidays table
-- -------------------------------------------------------------------------
CREATE TABLE holidays (
    id                 BIGSERIAL PRIMARY KEY,
    instance_id        UUID         NOT NULL,
    academic_year_id   BIGINT       NOT NULL REFERENCES academic_years(id),
    name               VARCHAR(100) NOT NULL,
    start_date         DATE         NOT NULL,
    end_date           DATE         NOT NULL,
    type               VARCHAR(20)  NOT NULL DEFAULT 'NATIONAL',
    description        VARCHAR(500),

    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    version            BIGINT       NOT NULL DEFAULT 0,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_holiday_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_holiday_type CHECK (type IN ('NATIONAL', 'SCHOOL', 'RELIGIOUS'))
);

CREATE INDEX idx_holidays_year ON holidays(academic_year_id) WHERE deleted = FALSE;
CREATE INDEX idx_holidays_dates ON holidays(start_date, end_date) WHERE deleted = FALSE;
CREATE INDEX idx_holidays_instance_id ON holidays(instance_id);
CREATE INDEX idx_holidays_deleted ON holidays(deleted);

-- -------------------------------------------------------------------------
-- Note: VN national holiday seed data is handled at application level
-- via HolidayService.seedVnNationalHolidays() when tenant creates academic year.
-- This avoids migrating tenant-specific data here.
-- -------------------------------------------------------------------------
