-- =====================================================================
-- Wave A Bucket B (GAP-786): Deprecate kiteclass-core staff_invitations
-- =====================================================================
-- Wave meta-6 Bucket A (V71) shipped a staff invitation MVP in kiteclass-core
-- that had Bug #17 (accept doesn't create user). Wave A Bucket B Day 1
-- investigation surfaced:
--   * kiteclass-core (port 5432, kiteclass_dev DB) cannot share JPA
--     UserRepository with kitehub-subscription (port 5433, kitehub DB).
--   * kitehub-subscription already has a working staff_invitations table +
--     controller that creates user on accept (production-proven pre-Wave
--     meta-6 via Wave 80 implementation).
--
-- Resolution per user-confirmed reversal 2026-05-28: kitehub-subscription
-- is the canonical staff invitation owner. Gateway routing reverted in
-- kitehub-gateway/application.yml same PR; kiteclass-core staff module
-- source code deleted same PR.
--
-- This migration adds a deprecation comment to the table; data NOT
-- dropped (preserves any beta tenant test data + supports rollback if
-- needed). Future cleanup: drop table after 1 release cycle confirms no
-- regressions.
--
-- Reference: GAP-786, Wave A plan §5.2 Risk 1, kitehub-gateway
-- application.yml staff-invitations route.
-- =====================================================================

COMMENT ON TABLE staff_invitations IS
  'DEPRECATED 2026-05-28 (Wave A Bucket B / GAP-786). Canonical staff '
  'invitation flow moved to kitehub.staff_invitations in kitehub-subscription DB. '
  'This table retained for rollback safety; drop in next major release after '
  '1 cycle of stable routing.';
