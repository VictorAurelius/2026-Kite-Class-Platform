-- GAP-942: Allow subscriptions.started_at + expires_at to be NULL for PENDING state
--
-- Wave flow-kh3 G1 walk 2026-06-04 surfaced contract drift between
-- PR #2151 SUB-20 fix (createSubscription sets PENDING with started_at=null,
-- expires_at=null until applyPendingUpgrade activates) and pre-existing
-- V2 schema constraint (started_at NOT NULL, expires_at NOT NULL — designed
-- when only ACTIVE state existed).
--
-- Insertion fails with SQLState 23502 → GlobalExceptionHandler maps to
-- HTTP 409 RESOURCE_CONFLICT (misleading; actual cause = constraint violation).
--
-- Fix: drop NOT NULL on both columns. Activation path (applyPendingUpgrade)
-- still sets both to non-null values when flipping PENDING → ACTIVE.

ALTER TABLE subscriptions
  ALTER COLUMN started_at DROP NOT NULL,
  ALTER COLUMN expires_at DROP NOT NULL;
