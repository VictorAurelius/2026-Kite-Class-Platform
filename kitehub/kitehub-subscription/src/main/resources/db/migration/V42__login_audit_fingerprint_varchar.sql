-- V42: Fix login_audit_log.fingerprint_hash column type CHAR(64) → VARCHAR(64)
--
-- V38 (PR #1298 Wave 72b Bucket C) declared CHAR(64). Entity LoginAuditLog
-- uses @Column(length=64) which Hibernate maps to VARCHAR(64). Schema-
-- validation strict mode fails at startup because column types mismatch.
--
-- Found via: kitehub-admin tests Spring context load failure (hotfix PR #1347
-- post-AuthService DI fix CI run).
--
-- Migration checksum-immutable per `gap-architecture-v2.md` Flyway rules —
-- cannot edit V38 in-place. ALTER applied as new migration V42.

ALTER TABLE login_audit_log
    ALTER COLUMN fingerprint_hash TYPE VARCHAR(64);

COMMENT ON COLUMN login_audit_log.fingerprint_hash
    IS 'SHA-256 hex digest of (ip + user_agent) fingerprint. VARCHAR(64) to match LoginAuditLog entity @Column(length=64).';
