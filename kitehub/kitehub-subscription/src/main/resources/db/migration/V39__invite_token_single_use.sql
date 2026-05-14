-- GAP-534 (Wave 77 Bucket D): Invite token single-use enforcement + audit log.
--
-- Wave 77 outside-in failure-mode matrix F1 (P0): invite email forwarded →
-- 2nd recipient validates token → beta slot hijack. Pre-existing flow validates
-- token successfully on every call until SIGNED_UP — letting a leaked link
-- consume the slot regardless of which recipient acted first.
--
-- This migration adds explicit single-use metadata to beta_access_request so
-- the service layer can enforce atomic UPDATE ... WHERE used_at IS NULL
-- semantics + audit log every reuse attempt.

ALTER TABLE beta_access_request
    ADD COLUMN used_at TIMESTAMP WITH TIME ZONE NULL;

ALTER TABLE beta_access_request
    ADD COLUMN consumed_ip VARCHAR(45) NULL;

ALTER TABLE beta_access_request
    ADD COLUMN consumed_user_agent VARCHAR(512) NULL;

CREATE INDEX idx_beta_access_request_used_at
    ON beta_access_request(used_at)
    WHERE used_at IS NOT NULL;

COMMENT ON COLUMN beta_access_request.used_at IS
    'GAP-534 Wave 77 — invite-token consumption timestamp. NULL = unused. Atomic UPDATE WHERE used_at IS NULL enforces single-use; 2nd consume attempt → row-count==0 → 409 Conflict.';

COMMENT ON COLUMN beta_access_request.consumed_ip IS
    'GAP-534 Wave 77 — IP that consumed the invite (forensic audit on reuse-attempt). 45 chars accommodates IPv6.';

COMMENT ON COLUMN beta_access_request.consumed_user_agent IS
    'GAP-534 Wave 77 — UA string at consume time (forensic audit on reuse-attempt). Truncated to 512 chars.';
