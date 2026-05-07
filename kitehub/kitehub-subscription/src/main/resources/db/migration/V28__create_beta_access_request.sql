-- GAP-372 (Wave 33 Bucket C): Beta tenant invite mechanism — Phase 1 BETA invite-only model.
--
-- Phase 1 BETA replaces public signup form với 3-stage flow:
--   1. Request: user submits beta access request
--   2. Approve: coordinator manually reviews + approves
--   3. Invite: approved user receives signup token email; completes signup
--
-- Public surface: POST /api/v1/auth/request-beta-access (unauthenticated, rate-limited).
-- Coordinator endpoints behind admin auth.
--
-- invite_token: 24h TTL, single-use UUID emitted on approve, validated on signup completion.

CREATE TABLE beta_access_request (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    name VARCHAR(200) NOT NULL,
    org_name VARCHAR(200) NOT NULL,
    persona VARCHAR(50) NOT NULL,
    referral_source VARCHAR(500) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    invite_token UUID NULL,
    invite_token_expiry TIMESTAMP WITH TIME ZONE NULL,
    invite_sent_at TIMESTAMP WITH TIME ZONE NULL,
    approver_id VARCHAR(100) NULL,
    approved_at TIMESTAMP WITH TIME ZONE NULL,
    rejected_at TIMESTAMP WITH TIME ZONE NULL,
    rejection_reason TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_beta_access_request_status ON beta_access_request(status);
CREATE INDEX idx_beta_access_request_email ON beta_access_request(email);
CREATE UNIQUE INDEX idx_beta_access_request_token ON beta_access_request(invite_token)
    WHERE invite_token IS NOT NULL;

COMMENT ON TABLE beta_access_request IS
    'Beta tenant invite request queue (GAP-372 Wave 33 Phase 1 BETA). 3-stage flow: PENDING -> APPROVED (token issued, email sent) -> SIGNED_UP, or PENDING -> REJECTED.';

COMMENT ON COLUMN beta_access_request.persona IS
    'Tier persona enumeration: P1_SOLO_TEACHER / P2_CENTER_OWNER (Phase 1 scope per release-1-deploy-plan.md).';

COMMENT ON COLUMN beta_access_request.status IS
    'PENDING -> APPROVED|REJECTED. APPROVED -> SIGNED_UP after the invitee completes signup with the token.';

COMMENT ON COLUMN beta_access_request.invite_token IS
    'Single-use UUID issued on approve. NULL while PENDING/REJECTED. Cleared once SIGNED_UP. 24h TTL via invite_token_expiry.';

COMMENT ON COLUMN beta_access_request.referral_source IS
    'Optional free-text — how the requester heard about KiteClass. Used by coordinator review heuristics.';
