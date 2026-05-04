-- GAP-323 Phase 1A (Wave 18b1 Bucket F): tenant vertical-type discriminator.
--
-- Adds `vertical_type` to instances so KiteClass can distinguish two
-- fundamentally different operating models:
--   - CENTER:      private trung tâm (legacy, default for backward compat)
--   - K12_SCHOOL:  trường công lập per TT 22/2021 + TT 32/2018 / GDPT 2018
--
-- Phase 1A behaviour: column added, default 'CENTER' for all existing rows.
-- Service layer in kiteclass-core uses this discriminator to decide which
-- attendance + grading model applies. Phase 1B (GAP-323b) will add a CHECK
-- constraint pairing `vertical_type = 'K12_SCHOOL'` with the
-- `attendance_period` write path; Phase 1A keeps enforcement in service layer
-- to avoid coupling migration ordering between repos.
--
-- Backward compat: every existing row defaults to 'CENTER'; existing tenants
-- behave exactly as before. The DEFAULT clause is preserved so legacy bulk
-- inserts from tooling continue to succeed without specifying the column.

ALTER TABLE instances
    ADD COLUMN vertical_type VARCHAR(20) NOT NULL DEFAULT 'CENTER';

ALTER TABLE instances
    ADD CONSTRAINT chk_instances_vertical_type
    CHECK (vertical_type IN ('CENTER', 'K12_SCHOOL'));

CREATE INDEX idx_instances_vertical_type
    ON instances(vertical_type);

COMMENT ON COLUMN instances.vertical_type IS
    'Tenant operating model. CENTER (legacy private trung tâm, default) or K12_SCHOOL (trường công lập per TT 22/2021 + TT 32/2018 GDPT 2018). GAP-323 Phase 1A — service layer enforces K-12 contract; Phase 1B will add per-table CHECK constraints.';
