-- GAP-388 (Wave 36 Bucket A): Token 2FA via short-lived claim code.
--
-- Email contains a 6-digit numeric "claim_code" instead of the raw UUID
-- invite_token. Signup form exchanges claim_code -> UUID via a server-side
-- endpoint, providing a 2FA gate (must possess email AND know claim code).
--
-- claim_code is plaintext (24h TTL, single-use) — over the same TTL window
-- as invite_token. Lifecycle: nullable until APPROVE, set with token, cleared
-- on SIGNED_UP or token expiry.

ALTER TABLE beta_access_request
    ADD COLUMN claim_code VARCHAR(6) NULL;

CREATE UNIQUE INDEX idx_beta_access_request_claim_code
    ON beta_access_request(claim_code)
    WHERE claim_code IS NOT NULL;

COMMENT ON COLUMN beta_access_request.claim_code IS
    'Short-lived 6-digit numeric code emailed to invitee. Exchanged for invite_token at signup time (GAP-388 Wave 36 Bucket A 2FA — defends against email forwarding/screenshot of full UUID).';
