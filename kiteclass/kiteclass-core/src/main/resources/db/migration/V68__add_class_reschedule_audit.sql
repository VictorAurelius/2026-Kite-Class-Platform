-- ============================================================================
-- V68: Add reschedule audit columns to classes table (GAP-291)
-- ============================================================================
-- Wave beta-readiness-4 Bucket D — Reschedule + email fallback
--
-- Per cross-bucket LOCKED decision §3.6:
--   - NO new ClassStatus.RESCHEDULED enum (preserves backward compatibility)
--   - Audit log columns only on existing classes row
--   - reason_category MANDATORY for FE form; reason_notes optional
--
-- Backfill: existing rows get NULL for all new columns (never rescheduled).
-- Industry benchmark: Cal.com `Booking.rescheduledFromId` audit pattern.
-- ============================================================================

ALTER TABLE classes
    ADD COLUMN rescheduled_by_user_id BIGINT NULL;

ALTER TABLE classes
    ADD COLUMN rescheduled_at TIMESTAMPTZ NULL;

ALTER TABLE classes
    ADD COLUMN previous_start_date DATE NULL;

ALTER TABLE classes
    ADD COLUMN previous_end_date DATE NULL;

ALTER TABLE classes
    ADD COLUMN reschedule_reason_category VARCHAR(64) NULL;

ALTER TABLE classes
    ADD COLUMN reschedule_reason_notes TEXT NULL;

COMMENT ON COLUMN classes.reschedule_reason_category
    IS 'RescheduleReasonCategory enum: GV_OM_BAN_DOT_XUAT, PHONG_HOC_KHONG_KHA_DUNG, '
       'MAT_DIEN_INTERNET, LE_TET_NGHI_CHINH_THUC, HOC_SINH_XIN_NGHI_TAP_THE, LY_DO_KHAC '
       '(Wave beta-readiness-4 Bucket D — GAP-291)';

COMMENT ON COLUMN classes.previous_start_date
    IS 'Capture of startDate BEFORE reschedule (audit trail). Null for never-rescheduled classes.';

COMMENT ON COLUMN classes.previous_end_date
    IS 'Capture of endDate BEFORE reschedule (audit trail). Null for never-rescheduled classes.';

COMMENT ON COLUMN classes.rescheduled_by_user_id
    IS 'User ID who triggered the most recent reschedule. PDPL Art 9 audit retention ≥5 years.';

COMMENT ON COLUMN classes.rescheduled_at
    IS 'Timestamp of the most recent reschedule operation (TIMESTAMPTZ, UTC).';

CREATE INDEX idx_classes_rescheduled_at ON classes (rescheduled_at) WHERE rescheduled_at IS NOT NULL;
