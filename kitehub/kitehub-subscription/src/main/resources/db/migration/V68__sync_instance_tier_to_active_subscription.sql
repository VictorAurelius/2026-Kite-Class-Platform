-- GAP-1090 (SUB-21): backfill instances.tier to mirror its ACTIVE subscription tier.
--
-- Root cause: SubscriptionService.applyPendingUpgrade (create + upgrade flows) and
-- SubscriptionRenewalService.processRenewal (end-of-cycle downgrade apply) flipped
-- subscriptions.tier but never synced instances.tier, leaving instances.tier stuck at
-- FREE (create flow) or the pre-change tier (upgrade/downgrade). instances.tier is
-- load-bearing: connection-pool size (MultiTenantDataSourceConfig), custom-domain
-- eligibility (DomainService), and data-retention window (DataRetentionService).
--
-- subscriptions.tier is the source-of-truth; instances.tier is the synced denormalized
-- current-effective-tier. This migration converges existing rows that drifted before the
-- code fix landed. ACTIVE-subscription filter + deleted=false mirror SubscriptionRepository
-- .findActiveByInstanceId so the backfill uses the same "current effective subscription".
--
-- Idempotent: only rows where instances.tier diverges from its active subscription tier are
-- touched (i.tier <> s.tier); re-running after convergence is a no-op. At most one ACTIVE
-- non-deleted subscription exists per instance (enforced by SubscriptionService.createSubscription
-- single-active guard), so the join is single-valued in practice.

UPDATE instances i
SET tier = s.tier
FROM subscriptions s
WHERE s.instance_id = i.id
  AND s.status = 'ACTIVE'
  AND s.deleted = false
  AND i.deleted = false
  AND i.tier <> s.tier;
