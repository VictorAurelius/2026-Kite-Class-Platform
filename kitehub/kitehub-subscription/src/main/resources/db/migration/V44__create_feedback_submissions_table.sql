-- GAP-542 (Wave 78 Bucket F): in-app feedback widget submission storage.
--
-- Schema source: documents/01-business/kitehub/feedback/api-contract.md
-- Endpoint: POST /api/v1/feedback (public, rate-limited at gateway 10 req/min/IP)
--
-- Bucket F + Bucket B both add migrations in Wave 78. Bucket B takes V43
-- (onboarding_progress); this migration uses V44 per
-- concurrent-production-mutation-ops.md coordination — pick V[highest+2].
--
-- Columns (matching api-contract.md FeedbackSubmissionRequest):
--   id           — internal BIGSERIAL primary key
--   public_id    — UUID exposed to client (FeedbackSubmissionResponse.id)
--   rating       — SMALLINT 1..5 (NOT NULL, CHECK constraint)
--   comment      — TEXT 5..2000 chars (validated server-side)
--   email        — VARCHAR(320) nullable (anonymous submit allowed)
--   page_url     — VARCHAR(2000) nullable (FE auto-populates)
--   category     — VARCHAR(50) enum (BUG/USABILITY/FEATURE_REQUEST/GENERAL)
--   tenant_id    — VARCHAR(100) nullable (auto-attached if JWT present)
--   user_id      — VARCHAR(100) nullable (auto-attached if JWT present)
--   client_ip    — VARCHAR(45) nullable (IPv6-safe)
--   status       — VARCHAR(50) (RECEIVED/REVIEWED/ARCHIVED)
--   created_at   — TIMESTAMPTZ NOT NULL
--   updated_at   — TIMESTAMPTZ NOT NULL
--
-- Indexes:
--   - idx_feedback_submissions_status_created (status, created_at DESC) for admin queue
--   - idx_feedback_submissions_tenant_created (tenant_id, created_at DESC) for per-tenant view
--   - idx_feedback_submissions_email_created (email, created_at DESC) for email survey lookup

CREATE TABLE IF NOT EXISTS feedback_submissions (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID NOT NULL UNIQUE,
    rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT NOT NULL CHECK (char_length(trim(comment)) BETWEEN 5 AND 2000),
    email           VARCHAR(320),
    page_url        VARCHAR(2000),
    category        VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    tenant_id       VARCHAR(100),
    user_id         VARCHAR(100),
    client_ip       VARCHAR(45),
    status          VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feedback_submissions_status_created
    ON feedback_submissions (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_feedback_submissions_tenant_created
    ON feedback_submissions (tenant_id, created_at DESC)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_feedback_submissions_email_created
    ON feedback_submissions (email, created_at DESC)
    WHERE email IS NOT NULL;

COMMENT ON TABLE feedback_submissions IS
    'In-app feedback widget submissions (GAP-542 Wave 78). Schema: documents/01-business/kitehub/feedback/api-contract.md. Public POST /api/v1/feedback endpoint persists rows; email-survey scheduler reads created_at + email for day-7/14 reminders.';

COMMENT ON COLUMN feedback_submissions.public_id IS
    'UUID exposed to clients (FeedbackSubmissionResponse.id); id column is internal.';

COMMENT ON COLUMN feedback_submissions.rating IS
    '1-5 rating per BR-FEEDBACK-001 (1=very poor, 5=excellent).';

COMMENT ON COLUMN feedback_submissions.category IS
    'Enum: BUG | USABILITY | FEATURE_REQUEST | GENERAL. Default GENERAL.';

COMMENT ON COLUMN feedback_submissions.status IS
    'Workflow status: RECEIVED (new) | REVIEWED (admin triaged) | ARCHIVED. Default RECEIVED.';
