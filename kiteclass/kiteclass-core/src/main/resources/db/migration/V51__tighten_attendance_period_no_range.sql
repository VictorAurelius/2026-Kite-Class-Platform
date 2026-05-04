-- GAP-323b Phase 1B.6: tighten attendance_period.period_no to realistic K-12 range.
--
-- Phase 1A (V50) allowed any period_no > 0 — schema-level laxity intended to
-- avoid coupling Phase 1A migration ordering with discriminator behaviour.
-- Phase 1B narrows the contract to BETWEEN 1 AND 10 per TT 22/2021/TT-BGDĐT
-- typical session schedule (5 morning + 5 afternoon tiết maximum).
--
-- Trade-off: a future regulation that legitimises an 11th period would require
-- another migration; that is preferable to silently accepting clearly-invalid
-- writes from misbehaving clients.
--
-- The discriminator-pairing originally suggested for 1B.6
-- (`vertical_type='K12_SCHOOL' IMPLIES period_no IS NOT NULL`) is moot at the
-- table level: `attendance_period` ONLY exists for K-12 tenants and `period_no`
-- is already NOT NULL via V50. Cross-database CHECK between
-- `instances.vertical_type` (kitehub-subscription) and `attendance_period`
-- (kiteclass-core) cannot be expressed in SQL; that gating stays in the
-- service layer.

ALTER TABLE attendance_period
    DROP CONSTRAINT IF EXISTS chk_att_period_no_positive;

ALTER TABLE attendance_period
    ADD CONSTRAINT chk_att_period_no_range CHECK (period_no BETWEEN 1 AND 10);

COMMENT ON COLUMN attendance_period.period_no IS
    'Tiết number (1..10). DB-level CHECK enforces K-12 contract per TT 22/2021/TT-BGDĐT (Phase 1B GAP-323b 1B.6 — tightened from > 0 in Phase 1A).';
