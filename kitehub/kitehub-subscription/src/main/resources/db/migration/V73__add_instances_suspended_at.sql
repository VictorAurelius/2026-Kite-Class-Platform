-- V73: Add suspended_at to instances table (GAP-1264, SUB-25).
--
-- Wave kitehub-biz-100 (BE-3 dunning/retention/suspend-lifecycle).
--
-- suspended_at is the DETERMINISTIC anchor for the data-retention clock.
-- Before this column, DataRetentionService computed the retention window from
-- instances.updated_at (RET-12) — but updated_at is bumped by ANY row update
-- (tier sync, email-prefs change, contact-email edit, …), so an unrelated write
-- during the suspension window silently RESET the retention clock. PDPL Art 16
-- (data minimisation) requires a deterministic delete-by date independent of
-- incidental updates.
--
-- The Instance entity stamps this column on every transition INTO SUSPENDED
-- (trial-expiry / involuntary-churn / cancel suspend) and clears it on every
-- reactivation (ACTIVE / TRIAL). DataRetentionService reads suspended_at first
-- and only falls back to updated_at for legacy rows (NULL) suspended before this
-- migration shipped.

ALTER TABLE instances ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP NULL;

COMMENT ON COLUMN instances.suspended_at IS
    'Timestamp of the most recent transition INTO SUSPENDED status (SUB-25). '
    'Deterministic anchor for the data-retention clock — NOT updated_at, which '
    'is bumped by unrelated row updates. NULL = never suspended OR legacy row '
    'suspended before V73 (DataRetentionService falls back to updated_at).';
