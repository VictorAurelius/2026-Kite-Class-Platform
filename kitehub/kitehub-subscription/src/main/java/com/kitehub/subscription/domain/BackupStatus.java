package com.kitehub.subscription.domain;

/**
 * Status of a database backup operation.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public enum BackupStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    DELETED
}
