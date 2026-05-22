-- =====================================================================
-- Wave 105 Bucket D — Parent Payment Idempotency (GAP-705)
-- =====================================================================
-- Two changes:
--   1) CREATE TABLE payment_idempotency_keys — stores Idempotency-Key
--      header value + first-write payment_id so repeat clicks (pay 2x)
--      return the SAME payment row instead of creating a new one.
--
--      Per `pre-handoff-self-test-completeness.md` §2.6 Payment flow gap
--      (d) "Idempotency key honored — same key replayed → no double-charge".
--
--   2) CREATE TABLE zalo_oa_notification_outbox — Wave 105 stub recording
--      events that would be sent to Zalo OA channel (parent invite, payment
--      confirm, attendance alert). Wave 106 GAP-286 will wire actual ZNS API
--      call; Wave 105 ships only the log surface so observability is in place.
--
-- Per `audit-to-gap-pipeline.md` Step 2 state-check:
--   * V60 is current head; V61 = next sequential per Flyway convention.
--   * `payments` table exists from earlier wave; no FK enforced here yet
--     because Bucket E security cluster still needs to wire user_id from JWT
--     (B1/D1 — PaymentController hardcoded `userId=1L`). FK enforced once
--     PaymentService consumes real principal id.
--
-- VN-localization (per `vn-localization-audit-checklist.md` §1):
--   * Currency normalized to VND minor unit (no decimal) — amount stored
--     BIGINT để tránh floating-point drift (vd 1.500.000đ = BIGINT 1500000).
--   * Idempotency window 24h khớp VietQR partner banks expiry convention.
-- =====================================================================

-- ---- 1) payment_idempotency_keys -----------------------------------
CREATE TABLE payment_idempotency_keys (
    id                  BIGSERIAL PRIMARY KEY,
    instance_id         UUID         NOT NULL,

    -- Idempotency-Key header value as supplied by client (UUID, ksuid, ulid).
    -- Length 64 generous — supports UUIDv4 (36) + future ksuid (27) + headroom.
    idempotency_key     VARCHAR(64)  NOT NULL,

    -- Caller identity for scope: same key from different users is OK to be
    -- two distinct payments (multi-tenant: 2 parents can both choose same UUID).
    -- Once Bucket E lands real principal extraction, this binds to that user_id.
    user_id             BIGINT       NOT NULL,

    -- Target invoice the payment is for. Helps debug + supports scope check.
    invoice_id          BIGINT       NOT NULL,

    -- The payment row created on FIRST request with this key. Subsequent
    -- requests with same (instance_id, idempotency_key) return THIS payment.
    payment_id          BIGINT       NOT NULL,

    -- For VietQR specifically — the QR payload returned on first request,
    -- so replay returns the same QR string (no need to re-call VietQR API).
    qr_payload          TEXT,

    -- 24h expiry — after this, key is reusable (matches VietQR partner-bank
    -- transaction-expiry window). Background sweeper deletes rows past expiry.
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMP    NOT NULL DEFAULT NOW() + INTERVAL '24 hours',

    CONSTRAINT uk_payment_idempotency_scope
        UNIQUE (instance_id, idempotency_key)
);

CREATE INDEX idx_payment_idempotency_user ON payment_idempotency_keys (user_id);
CREATE INDEX idx_payment_idempotency_invoice ON payment_idempotency_keys (invoice_id);
CREATE INDEX idx_payment_idempotency_expires ON payment_idempotency_keys (expires_at);

COMMENT ON TABLE payment_idempotency_keys IS
    'Wave 105 Bucket D — Parent payment idempotency state. '
    'Idempotency-Key header → first-write payment_id mapping. '
    'See pre-handoff-self-test-completeness.md §2.6 (d).';

-- ---- 2) zalo_oa_notification_outbox -------------------------------
-- Wave 105 stub: logs events that WOULD be sent to Zalo OA channel.
-- Wave 106 GAP-286 will read this outbox + dispatch ZNS API.
-- Per `design-patterns.md` §3.5 Outbox pattern — same-txn write for reliability.

CREATE TABLE zalo_oa_notification_outbox (
    id              BIGSERIAL PRIMARY KEY,
    instance_id     UUID         NOT NULL,

    -- Event type — 3 supported in Wave 105 stub:
    --   PARENT_INVITE_SENT      — sister channel to email invite
    --   PAYMENT_CONFIRM         — payment success notification
    --   ATTENDANCE_ALERT        — daily attendance recap for parent
    event_type      VARCHAR(40)  NOT NULL,

    -- Recipient identity — parent_id linked to Zalo OA via future GAP-286 token.
    parent_id       BIGINT       NOT NULL,

    -- Optional context — child_id for attendance/grade events; invoice_id for
    -- payment events; invitation_id for invite events. NULL when N/A.
    context_id      BIGINT,

    -- JSONB payload — schema differs per event_type. Wave 106 dispatcher
    -- normalizes to ZNS template variables (zns_template_id + template_data).
    payload         JSONB        NOT NULL DEFAULT '{}'::jsonb,

    -- Dispatch state — Wave 105 stub stays at PENDING. Wave 106 introduces
    -- DISPATCHED + FAILED transitions via background worker.
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    -- For Wave 106 retry semantics — number of attempts so far.
    attempt_count   INT          NOT NULL DEFAULT 0,

    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    dispatched_at   TIMESTAMP,

    CONSTRAINT chk_zalo_oa_status
        CHECK (status IN ('PENDING', 'DISPATCHED', 'FAILED', 'SKIPPED')),
    CONSTRAINT chk_zalo_oa_event_type
        CHECK (event_type IN ('PARENT_INVITE_SENT', 'PAYMENT_CONFIRM', 'ATTENDANCE_ALERT'))
);

CREATE INDEX idx_zalo_oa_parent ON zalo_oa_notification_outbox (parent_id);
CREATE INDEX idx_zalo_oa_status_created ON zalo_oa_notification_outbox (status, created_at);

COMMENT ON TABLE zalo_oa_notification_outbox IS
    'Wave 105 Bucket D STUB — events that would be sent to Zalo OA channel. '
    'Wave 106 GAP-286 wires ZNS API dispatch. Per outbox pattern '
    'design-patterns.md §3.5.';
