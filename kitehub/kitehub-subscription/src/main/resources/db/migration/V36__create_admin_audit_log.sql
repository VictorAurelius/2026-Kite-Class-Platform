-- GAP-521: Admin action audit log
-- Persists every PLATFORM_ADMIN privileged action with full context for
-- OWASP A07 + PDPL audit trail (admin user id, action, target, ip, ua, payload).
-- Populated by AdminAuditAspect (@Auditable) on admin controller methods.

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id                  BIGSERIAL    PRIMARY KEY,
    admin_user_id       UUID         NOT NULL,
    action              VARCHAR(64)  NOT NULL,
    target_entity_type  VARCHAR(64),
    target_entity_id    VARCHAR(128),
    request_ip          VARCHAR(64),
    user_agent          VARCHAR(512),
    payload_json        JSONB,
    success             BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message       VARCHAR(1024),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_admin_audit_log_user FOREIGN KEY (admin_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_log_user_time
    ON admin_audit_log (admin_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_log_action_time
    ON admin_audit_log (action, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_log_target
    ON admin_audit_log (target_entity_type, target_entity_id);

COMMENT ON TABLE admin_audit_log IS
    'OWASP A07 + PDPL audit trail for privileged admin actions. Retention: 7 years per logs-format-standard.md.';
