-- =====================================================================
-- Wave 19 — GAP-321c Phase 1C v1: PDPL granular consent + complaint queue
-- =====================================================================
-- Two changes (additive, backward compatible):
--   1) ALTER TABLE parent_student_links — add JSONB column
--      `parental_consent` storing per-field visibility + consent version.
--      Default value `{"fields":{}, "version":1, "updatedAt":null}` so
--      existing rows from V42 migrate without manual fill. ConsentService
--      treats missing/empty `fields` map as "no consent granted yet" and
--      gates facet APIs accordingly.
--
--   2) CREATE TABLE parent_complaint_queue — minimal v1 write surface
--      satisfying Đ.83 K2 implicit communication right (Luật Giáo dục
--      2019). Full complaint workflow (4-level escalation, attachments,
--      resolution UI) lands in GAP-339; this v1 just persists the parent
--      input + scopes by parent/student so the audit trail exists.
--
-- See:
--   * BR-PARENT-PORTAL-011..013 in
--     documents/01-business/kiteclass/parent-portal/rules.md
--   * UC-PARENT-CONSENT-MANAGE + UC-PARENT-COMPLAINT-FILE in
--     documents/01-business/kiteclass/parent-portal/use-cases.md
-- =====================================================================

-- ---- 1) parent_student_links.parental_consent ----------------------
ALTER TABLE parent_student_links
    ADD COLUMN parental_consent JSONB NOT NULL
    DEFAULT '{"fields": {}, "version": 1, "updatedAt": null}'::jsonb;

COMMENT ON COLUMN parent_student_links.parental_consent IS
    'PDPL Decree 13/2023 Art 16 granular consent — JSONB shape '
    '{"fields": {"<field>": <bool>}, "version": <int>, "updatedAt": <iso>}. '
    'ConsentService gates facet reads on per-field flags. Wave 19 GAP-321c.';

-- ---- 2) parent_complaint_queue (v1 write surface) ------------------
CREATE TABLE parent_complaint_queue (
    id              BIGSERIAL PRIMARY KEY,
    instance_id     UUID         NOT NULL,

    parent_id       BIGINT       NOT NULL REFERENCES parents (id),
    student_id      BIGINT       NOT NULL REFERENCES students (id),
    complaint_text  TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    resolved_at     TIMESTAMP,

    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_parent_complaint_status
        CHECK (status IN ('PENDING', 'IN_REVIEW', 'RESOLVED', 'REJECTED'))
);

CREATE INDEX idx_parent_complaint_queue_parent
    ON parent_complaint_queue (parent_id);
CREATE INDEX idx_parent_complaint_queue_student
    ON parent_complaint_queue (student_id);
CREATE INDEX idx_parent_complaint_queue_instance
    ON parent_complaint_queue (instance_id);
CREATE INDEX idx_parent_complaint_queue_status
    ON parent_complaint_queue (status);

COMMENT ON TABLE parent_complaint_queue IS
    'Minimal v1 complaint queue (Wave 19 GAP-321c). Full workflow lands GAP-339.';
