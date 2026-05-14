-- V43: Wave 78 Bucket B (GAP-538) — Onboarding Progress table.
--
-- Per-tenant checklist state for first-time onboarding. One row per tenant;
-- step state stored as JSONB array of {stepId, completed, completedAt}
-- where stepId is whitelisted by OnboardingStepId enum on the BE side.
--
-- Schema source-of-truth: documents/01-business/kitehub/onboarding/api-contract.md
-- (Wave 78 Bucket 0 Foundation contract — committed 2026-05-14).

CREATE TABLE IF NOT EXISTS onboarding_progress (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           UUID         NOT NULL,
    steps_json          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    completion_percent  INT          NOT NULL DEFAULT 0,
    last_updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_onboarding_progress_tenant UNIQUE (tenant_id),
    CONSTRAINT ck_onboarding_completion_pct CHECK (completion_percent BETWEEN 0 AND 100)
);

CREATE INDEX IF NOT EXISTS idx_onboarding_progress_tenant
    ON onboarding_progress (tenant_id);

COMMENT ON TABLE onboarding_progress IS
    'Wave 78 GAP-538 — Per-tenant Day-1 onboarding checklist state. Lazy-init on first GET.';

COMMENT ON COLUMN onboarding_progress.steps_json IS
    'JSONB array of {stepId, completed, completedAt}. stepId whitelisted by OnboardingStepId enum.';
