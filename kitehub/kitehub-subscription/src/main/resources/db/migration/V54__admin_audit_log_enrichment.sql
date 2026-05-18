-- GAP-521 Wave 92 Bucket A — Admin audit log enrichment (Phase 2)
--
-- Wave 72a Bucket B (V36) đã tạo baseline admin_audit_log: id / admin_user_id /
-- action / target_entity_type / target_entity_id / request_ip / user_agent /
-- payload_json / success / error_message / created_at.
--
-- Phase 2 (Wave 92) thêm 5 enrichment columns cho forensic richer + better
-- correlation across audit trail:
--
--   request_id      — UUID/string định danh request (correlation key với
--                     trace_id / X-Request-Id từ gateway header). Cho phép
--                     join admin_audit_log với access logs + APM traces khi
--                     forensic investigate.
--   target_resource_type — semantic resource type (vd "instance",
--                     "tenant_config", "user"). Tách biệt với existing
--                     target_entity_type (vd "beta_access_request") khi resource
--                     ≠ JPA entity (vd config key, RBAC role mapping).
--   target_resource_id   — fully-qualified resource id (vd "tenant/UUID",
--                     "config/kite.foo.bar"). Tách biệt với target_entity_id
--                     (chỉ chứa JPA entity PK).
--   before_state    — JSONB snapshot of resource state TRƯỚC action (nullable
--                     for CREATE actions). Cho phép diff reconstruction khi
--                     audit cần verify what changed.
--   after_state     — JSONB snapshot of resource state SAU action (nullable
--                     for DELETE actions). Pair với before_state cho complete
--                     forensic trail.
--
-- Index: composite (target_resource_type, target_resource_id) cho fast lookup
-- "tất cả actions trên resource X" — forensic query phổ biến.
--
-- Backward compat: all enrichment columns nullable; existing rows + callers
-- không ảnh hưởng. AdminAuditAspect đã thêm support populate fields qua
-- @Auditable extension trong cùng PR.

ALTER TABLE admin_audit_log
    ADD COLUMN IF NOT EXISTS request_id           VARCHAR(64),
    ADD COLUMN IF NOT EXISTS target_resource_type VARCHAR(64),
    ADD COLUMN IF NOT EXISTS target_resource_id   VARCHAR(256),
    ADD COLUMN IF NOT EXISTS before_state         JSONB,
    ADD COLUMN IF NOT EXISTS after_state          JSONB;

CREATE INDEX IF NOT EXISTS idx_admin_audit_log_resource
    ON admin_audit_log (target_resource_type, target_resource_id);

COMMENT ON COLUMN admin_audit_log.request_id IS
    'Correlation key với gateway X-Request-Id / trace_id (forensic join).';

COMMENT ON COLUMN admin_audit_log.target_resource_type IS
    'Semantic resource type (config key, RBAC role, etc.) — tách biệt với '
    'target_entity_type (JPA entity).';

COMMENT ON COLUMN admin_audit_log.target_resource_id IS
    'Fully-qualified resource id (vd "tenant/UUID", "config/kite.foo.bar").';

COMMENT ON COLUMN admin_audit_log.before_state IS
    'JSONB snapshot of resource state TRƯỚC action — nullable for CREATE.';

COMMENT ON COLUMN admin_audit_log.after_state IS
    'JSONB snapshot of resource state SAU action — nullable for DELETE.';
