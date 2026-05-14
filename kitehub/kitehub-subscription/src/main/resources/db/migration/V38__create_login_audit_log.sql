-- GAP-517: Login audit log + new-fingerprint alert
-- Persists every successful login with (user, ip, ua, fingerprint_hash). On a NEW
-- fingerprint for a PLATFORM_ADMIN user, a Spring ApplicationEvent triggers a
-- transactional alert email. Cooldown enforced via fingerprint_hash + last_alert
-- comparison within 24h.
--
-- Per `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.5 (admin login
-- alerts) + `logs-format-standard.md` §4 retention (security/audit logs = 7y).
--
-- Note: user_id is UUID to match the existing users.id type (kitehub-platform
-- shared User entity). Task spec referenced BIGINT but actual schema uses UUID.

CREATE TABLE IF NOT EXISTS login_audit_log (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             UUID         NOT NULL,
    login_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ip                  INET,
    user_agent          VARCHAR(512),
    geo_country         VARCHAR(8),
    fingerprint_hash    CHAR(64),
    alert_sent          BOOLEAN      NOT NULL DEFAULT FALSE,
    alert_sent_at       TIMESTAMPTZ,
    CONSTRAINT fk_login_audit_log_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_login_audit_user_time
    ON login_audit_log (user_id, login_at DESC);

CREATE INDEX IF NOT EXISTS idx_login_audit_user_fingerprint
    ON login_audit_log (user_id, fingerprint_hash);

COMMENT ON TABLE login_audit_log IS
    'OWASP A07 §2.5 — per-login audit trail with fingerprint-based new-IP detection for PLATFORM_ADMIN alerts. Retention: 7 years per logs-format-standard.md.';
