-- GAP-099 Phase 1: Structured class schedule foundation
-- Creates table for typed weekly schedule slots per SubjectSection.
-- Free-form `schedule` column on subject_sections retained for backward compat until Phase 2 migrates data.

CREATE TABLE class_schedule_slots (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    subject_section_id BIGINT NOT NULL REFERENCES subject_sections(id),
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    effective_from DATE NOT NULL,
    effective_until DATE,
    recurrence_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_schedule_slot_day CHECK (
        day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')
    ),
    CONSTRAINT chk_schedule_slot_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_schedule_slot_date_order CHECK (effective_until IS NULL OR effective_until >= effective_from)
);

CREATE INDEX idx_schedule_slot_section_day ON class_schedule_slots(subject_section_id, day_of_week, deleted);
CREATE INDEX idx_schedule_slot_instance ON class_schedule_slots(instance_id);
CREATE INDEX idx_schedule_slot_effective ON class_schedule_slots(effective_from, effective_until);

COMMENT ON TABLE class_schedule_slots IS 'GAP-099: Structured weekly schedule slots replacing free-form schedule text';
COMMENT ON COLUMN class_schedule_slots.day_of_week IS 'java.time.DayOfWeek name (MONDAY..SUNDAY)';
COMMENT ON COLUMN class_schedule_slots.effective_until IS 'NULL = indefinite; set to end date for mid-year schedule changes';
COMMENT ON COLUMN class_schedule_slots.recurrence_note IS 'Free-text exceptions (e.g., "Skip week 5"). Structured exceptions deferred to Phase 2.';
