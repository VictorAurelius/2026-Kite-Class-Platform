-- Wave 18a Bucket B — GAP-063 Phase 1 — per-User × per-NotificationType × Set<Channel>
-- preferences. Richer than the V18 (GAP-098) instance-level booleans on `instances`
-- which remain as legacy fallback per BR-NOTIF-006 in
-- documents/01-business/kitehub/notification/rules.md.

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    notification_type VARCHAR(32) NOT NULL,
    enabled_channels VARCHAR(64) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notification_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_notification_preferences_user_type
        UNIQUE (user_id, notification_type)
);

CREATE INDEX idx_notification_preferences_user_id
    ON notification_preferences (user_id);

COMMENT ON TABLE notification_preferences IS
    'Per-User × NotificationType preference rows (GAP-063 Phase 1, Wave 18a Bucket B). enabled_channels is a comma-separated set of NotificationChannelType enum names — Phase 1 only EMAIL is wired.';
COMMENT ON COLUMN notification_preferences.enabled_channels IS
    'Comma-separated NotificationChannelType set (EMAIL,SMS,ZALO,PUSH). Empty string = all channels disabled.';
