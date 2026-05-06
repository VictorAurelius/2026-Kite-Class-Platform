-- GAP-353c (Wave 26 Bucket A): self-service DSAR (Data Subject Access Request) ticket queue.
--
-- Backs `BR-PDPL-DSAR-001..005` from `documents/01-business/kitehub/marketing/rules.md`.
-- PDPL 2023 Article 14 grants 6 data-subject rights (access / rectification / erasure /
-- portability / restrict-processing / object); manual email-based DSAR was acceptable for
-- MVP per WG13/2023 reading, but Phase 2 ships a structured intake form so the SLA timer,
-- DPO notification flow, and per-ticket audit trail can be enforced programmatically
-- instead of relying on a shared inbox.
--
-- Public surface: BOTH endpoints (POST /api/v1/dsar/request + GET /api/v1/dsar/{ticketId})
-- are unauthenticated by design — DSAR submitters are NOT logged-in users by definition
-- (right-to-access often invoked by ex-users or never-signed-up data subjects). Identity
-- verification happens out-of-band via national_id_last4 + email + DPO callback.

CREATE TABLE dsar_ticket (
    id BIGSERIAL PRIMARY KEY,
    ticket_uuid UUID NOT NULL UNIQUE,
    requester_email VARCHAR(320) NOT NULL,
    requester_name VARCHAR(200) NOT NULL,
    national_id_last4 VARCHAR(4) NOT NULL,
    right_type VARCHAR(50) NOT NULL,
    scope TEXT NULL,
    reason TEXT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sla_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    resolution TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE UNIQUE INDEX idx_dsar_ticket_uuid ON dsar_ticket(ticket_uuid);
CREATE INDEX idx_dsar_ticket_status ON dsar_ticket(status);
CREATE INDEX idx_dsar_ticket_sla_deadline ON dsar_ticket(sla_deadline) WHERE status IN ('PENDING', 'IN_REVIEW');

COMMENT ON TABLE dsar_ticket IS
    'Self-service DSAR ticket queue (GAP-353c Wave 26). PDPL Art 14 6 rights; SLA 20 days per BR-PDPL-DSAR-002; identity verification via national_id_last4 + DPO callback (BR-PDPL-DSAR-003). Both endpoints public — DSAR submitters are not logged-in by definition.';

COMMENT ON COLUMN dsar_ticket.ticket_uuid IS
    'Public reference exposed to requester (avoids exposing BIGSERIAL row id externally).';
COMMENT ON COLUMN dsar_ticket.right_type IS
    'PDPL Art 14 right enumeration: ACCESS / RECTIFICATION / ERASURE / PORTABILITY / RESTRICT / OBJECT (BR-PDPL-DSAR-001).';
COMMENT ON COLUMN dsar_ticket.status IS
    'PENDING -> IN_REVIEW -> COMPLETED|REJECTED. PENDING = freshly submitted, awaiting DPO triage. IN_REVIEW = DPO actively investigating. COMPLETED = response delivered. REJECTED = identity verification failed or out-of-scope.';
COMMENT ON COLUMN dsar_ticket.sla_deadline IS
    'PDPL Art 14 + Decree 13/2023 Art 19 — controller must respond within 20 days of request (BR-PDPL-DSAR-002). Set at submit time = created_at + INTERVAL ''20 days''. SlaTimerCron flags overdue rows daily 04:00.';
COMMENT ON COLUMN dsar_ticket.national_id_last4 IS
    'Last 4 digits of CMND/CCCD for identity verification (BR-PDPL-DSAR-003). Full ID never collected — DPO callback verifies remaining digits out-of-band per data-minimization principle.';
COMMENT ON COLUMN dsar_ticket.resolution IS
    'Free-text DPO notes after COMPLETED/REJECTED. Stored for 36 months per DR-03 retention then purged (BR-PDPL-DSAR-004).';
