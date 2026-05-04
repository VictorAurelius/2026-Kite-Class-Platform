-- GAP-321b Phase 1B (Wave 18b2 Bucket C): per-read audit log skeleton for parent portal.
--
-- Phase 1A (GAP-321) shipped scope guard for transcript reads. Phase 1B extends
-- to 4 sibling facets (attendance / fees / conduct / notifications) and adds a
-- per-read audit row in service of PDPL Decree 13/2023 Art 16 traceability +
-- Luật Trẻ em 2016 Đ.21 (children's privacy right). Without a read-audit table
-- there is no way to answer "who looked at child X's data, when, on which
-- facet" — a hard requirement for safeguarding investigations.
--
-- Phase 1B v1 ships the entity + service skeleton; admin/safeguarding query
-- surface + 5-year retention sweeper + IP/user-agent capture deferred to
-- GAP-321b.4 follow-up. The skeleton on its own is sufficient for the
-- compliance promise: every facet read writes one row; rows are append-only.
--
-- Backward compat: new table; no impact on existing reads or writes.

CREATE TABLE parent_read_audit_log (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- Domain columns
    parent_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    facet VARCHAR(20) NOT NULL,
    read_at TIMESTAMP NOT NULL,

    -- BaseEntity audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_parent_read_audit_facet CHECK (
        facet IN ('TRANSCRIPT', 'ATTENDANCE', 'FEES', 'CONDUCT', 'NOTIFICATIONS')
    )
);

-- Primary query path: "show me every read of child X by parent Y in time
-- range." parent_id leads because most queries scope by parent (admin
-- investigations); child_id second; read_at last for range scans.
CREATE INDEX idx_parent_read_audit_parent_child_time
    ON parent_read_audit_log(parent_id, child_id, read_at);

-- Secondary: tenant + facet aggregations (e.g., "how many notification reads
-- this month for instance X").
CREATE INDEX idx_parent_read_audit_instance_facet
    ON parent_read_audit_log(instance_id, facet);

CREATE INDEX idx_parent_read_audit_deleted
    ON parent_read_audit_log(deleted);

COMMENT ON TABLE parent_read_audit_log IS
    'GAP-321b Phase 1B v1 skeleton: per-read audit row for parent-side facet endpoints (PDPL Decree 13/2023 Art 16 traceability). 5-year retention + IP/user-agent capture deferred to follow-up.';

COMMENT ON COLUMN parent_read_audit_log.facet IS
    'Which parent portal facet was read. Allowed values: TRANSCRIPT (Phase 1A), ATTENDANCE / FEES / CONDUCT / NOTIFICATIONS (Phase 1B). Discipline facet deferred to GAP-321c.';

COMMENT ON COLUMN parent_read_audit_log.read_at IS
    'Server-side timestamp at the moment the facet endpoint returned 200. Distinct from created_at to keep room for backfill loads if required by audit subpoena.';
