-- V52: Fix login_audit_log.ip column type INET → VARCHAR(45)
--
-- V38 declared `ip INET`. Entity LoginAuditLog maps the field as Java String
-- with @Column(columnDefinition="inet"). Hibernate binds via
-- PreparedStatement.setString (VARCHAR), and PostgreSQL refuses the implicit
-- cast from varchar to inet with SQLState 42804:
--
--   ERROR: column "ip" is of type inet but expression is of type character varying
--   Hint: You will need to rewrite or cast the expression.
--
-- This caused every successful login to throw inside LoginAuditService.recordLogin
-- (Wave 87 hotfix 2026-05-16, audit: documents/04-quality/audits/aws-verification/
-- 2026-05-16-admin-login-500-rca.md). The companion code fix moves recordLogin
-- to Propagation.REQUIRES_NEW so audit failure no longer poisons the parent
-- login transaction; this migration restores correct INSERT behavior so the
-- audit row is actually written.
--
-- VARCHAR(45) is the canonical max length for IPv6 textual representation
-- (8 groups × 4 hex digits + 7 colons + IPv4-mapped suffix `:255.255.255.255`).
-- We accept the loss of Postgres-native inet validation; LoginAuditService
-- already validates IP shape via extractClientIp (X-Forwarded-For parsing).
--
-- Pattern precedent: V42 ALTER fingerprint_hash CHAR(64) → VARCHAR(64)
-- (same root cause class — entity ↔ DDL type mismatch surfaced only on
-- production Postgres, not H2 unit tests).

ALTER TABLE login_audit_log
    ALTER COLUMN ip TYPE VARCHAR(45) USING ip::text;

COMMENT ON COLUMN login_audit_log.ip
    IS 'Client IP (IPv4 or IPv6 textual form, max 45 chars). VARCHAR for Hibernate String compatibility; validation happens in LoginAuditService.extractClientIp.';
