-- GAP-290 Wave 18a: Add recurrence_rule JSONB column to classes table.
--
-- Stores RFC 5545 RRULE subset (WEEKLY only Phase 1) for structured recurring
-- class schedules. Free-form `schedule` VARCHAR(200) column is retained for
-- backward compatibility with classes created before Phase 1.
--
-- Schema (v1):
--   {
--     "freq": "WEEKLY",
--     "by_day": ["TU", "TH"],
--     "start_time": "19:00",
--     "end_time": "20:30",
--     "until": "2026-08-01",
--     "exclude_dates": ["2026-06-15"]
--   }
--
-- BR-CLASS-009 (see documents/01-business/kiteclass/clazz/rules.md):
--  - freq=WEEKLY only in Phase 1
--  - by_day must contain >=1 valid 2-letter iCal day code
--  - end_time strictly after start_time
--  - until required (no infinite recurrence)
--  - exclude_dates optional, may be null/empty
--
-- Existing rows: column allowed NULL — classes without RRULE keep using the
-- legacy free-form `schedule` text field.

ALTER TABLE classes
    ADD COLUMN recurrence_rule JSONB;

COMMENT ON COLUMN classes.recurrence_rule IS
    'GAP-290: Structured RRULE subset (WEEKLY only). NULL = no recurrence; sessions created manually or via legacy /schedule endpoint.';

-- GIN index for occasional admin queries by RRULE field (e.g., find all classes
-- recurring on Tuesdays). Cheap on insert; column is small JSON. Conditional on
-- non-null to skip 99% of rows that won't have a rule yet.
CREATE INDEX idx_classes_recurrence_rule_gin
    ON classes USING GIN (recurrence_rule)
    WHERE recurrence_rule IS NOT NULL;
