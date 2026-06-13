-- GAP-1265 (wave-kitehub-biz-100): persistent in-app notification (persistent-banner) store.
-- Durable fallback channel for the NotificationChannel seam — written for every dispatched owner
-- notification so the owner always has a record even when email delivery is disabled/fails.
--
-- NOTE (multi-session coordination): version V74 (kitehub-subscription Flyway sequence; prior max
-- was V72) reserved by the BE-4 notification bucket of wave-kitehub-biz-100. If a sibling bucket
-- also claimed V74, renumber to the next free version at wave merge (Flyway orders by version;
-- gaps such as the unused V73 slot are allowed).
--
-- Tenant isolation is enforced at the app layer (TenantOwnershipGuard + instanceId filter); RLS
-- is intentionally NOT enabled here because the dispatch path runs in admin context where the
-- per-request tenant GUC is not the owner's tenant. RLS hardening is a documented follow-up.

CREATE TABLE IF NOT EXISTS in_app_notifications (
    id                UUID PRIMARY KEY,
    instance_id       UUID NOT NULL,
    notification_type VARCHAR(50)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              VARCHAR(1000) NOT NULL,
    action_url        VARCHAR(500),
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    read_at           TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_in_app_notifications_instance_unread
    ON in_app_notifications (instance_id, is_read)
    WHERE deleted = FALSE;

COMMENT ON TABLE in_app_notifications IS
    'GAP-1265 persistent-banner fallback channel for owner notifications (payment-confirmed, win-back, ...).';
