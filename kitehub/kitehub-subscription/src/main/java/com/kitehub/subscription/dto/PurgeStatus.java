package com.kitehub.subscription.dto;

/**
 * Status of a purge operation result.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public enum PurgeStatus {
    SUCCESS,
    SKIPPED_NO_BACKUP,
    FAILED
}
